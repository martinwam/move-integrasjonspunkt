package no.difi.meldingsutveksling.mail;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.xml.transform.StringSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EduMailSenderTest {

    @Mock private IntegrasjonspunktProperties props;

    @InjectMocks private EduMailSender mailSender;

    private Unmarshaller unmarshaller;

    private static SimpleSmtpServer simpleSmtpServer;

    @BeforeClass
    public static void beforeClass() throws IOException {
        simpleSmtpServer = SimpleSmtpServer.start(SimpleSmtpServer.AUTO_SMTP_PORT);
    }

    @AfterClass
    public static void afterClass() {
        simpleSmtpServer.stop();
    }

    @Before
    public void init() throws JAXBException {
        simpleSmtpServer.reset();

        IntegrasjonspunktProperties.Mail mail = new IntegrasjonspunktProperties.Mail();
        mail.setSmtpHost("localhost");
        mail.setSenderAddress("doofenshmirtz@evil.inc");
        mail.setReceiverAddress("stuntman@difi.no");
        mail.setSmtpPort(String.valueOf(simpleSmtpServer.getPort()));
        mail.setEnableAuth("false");
        mail.setUsername("");
        mail.setPassword("");

        when(props.getMail()).thenReturn(mail);

        mailSender = new EduMailSender(props);

        JAXBContext putMessageJaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
        this.unmarshaller = putMessageJaxbContext.createUnmarshaller();
    }

    @Test
    public void testSend() throws JAXBException, IOException {
        send();
        assertThat(simpleSmtpServer.getReceivedEmails()).hasSize(1);

        SmtpMessage smtpMessage = simpleSmtpServer.getReceivedEmails().get(0);

        assertThat(smtpMessage.getHeaderValue("From")).isEqualTo("doofenshmirtz@evil.inc");
        assertThat(smtpMessage.getHeaderValue("To")).isEqualTo("stuntman@difi.no");
        assertThat(smtpMessage.getHeaderValue("Subject")).isEqualTo("foo");

        assertThat(smtpMessage.getBody()).hasLineCount(4);
    }

    @Test
    public void testSendTooLarge() throws JAXBException, IOException {
        props.getMail().setMaxSize(40000L);
        send();
        assertThat(simpleSmtpServer.getReceivedEmails()).hasSize(1);

        SmtpMessage smtpMessage = simpleSmtpServer.getReceivedEmails().get(0);

        assertThat(smtpMessage.getHeaderValue("From")).isEqualTo("doofenshmirtz@evil.inc");
        assertThat(smtpMessage.getHeaderValue("To")).isEqualTo("stuntman@difi.no");
        assertThat(smtpMessage.getHeaderValue("Subject")).isEqualTo("foo");

        assertThat(smtpMessage.getBody()).isEqualTo("Du har mottatt en BestEdu melding. Denne er for stor for =E5 kunne sendes o=ver e-post. Vennligst logg deg inn p=E5 FIKS portalen for =E5 laste den ned=..");
    }

    @Test
    public void testSendTooLargeTwice() throws JAXBException, IOException {
        props.getMail().setMaxSize(40000L);
        send();
        send();
        assertThat(simpleSmtpServer.getReceivedEmails()).hasSize(1);

        SmtpMessage smtpMessage = simpleSmtpServer.getReceivedEmails().get(0);

        assertThat(smtpMessage.getHeaderValue("From")).isEqualTo("doofenshmirtz@evil.inc");
        assertThat(smtpMessage.getHeaderValue("To")).isEqualTo("stuntman@difi.no");
        assertThat(smtpMessage.getHeaderValue("Subject")).isEqualTo("foo");

        assertThat(smtpMessage.getBody()).isEqualTo("Du har mottatt en BestEdu melding. Denne er for stor for =E5 kunne sendes o=ver e-post. Vennligst logg deg inn p=E5 FIKS portalen for =E5 laste den ned=..");
    }

    private void send() throws JAXBException, IOException {
        PutMessageRequestType putMessage = createPutMessageCdataXml(FileUtils.readFileToString(new File
                ("src/test/resources/putmessage_test.xml"), StandardCharsets.UTF_8));
        mailSender.send(putMessage, "foo");
    }

    private PutMessageRequestType createPutMessageCdataXml(String payload) throws JAXBException {
        return unmarshaller.unmarshal(new StringSource((payload)), PutMessageRequestType.class).getValue();
    }
}
