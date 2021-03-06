package no.difi.meldingsutveksling.mail;

import com.sun.mail.smtp.SMTPTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.BestEduConverter;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.noarkexchange.PayloadUtil;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.springframework.util.StringUtils;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class EduMailSender {

    private static final String CHARSET = StandardCharsets.ISO_8859_1.name();

    private final IntegrasjonspunktProperties properties;
    private final Set<String> conversationIds = ConcurrentHashMap.newKeySet();

    public void send(PutMessageRequestType request, String title) {
        Properties props = new Properties();
        props.put("mail.smtp.host", properties.getMail().getSmtpHost());
        props.put("mail.smtp.port", properties.getMail().getSmtpPort());
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", properties.getMail().getEnableAuth());

        String trust = properties.getMail().getTrust();
        if (StringUtils.hasText(trust)) {
            props.put("mail.smtp.ssl.trust", trust);
        }

        Session session = Session.getDefaultInstance(props,
                new javax.mail.Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                properties.getMail().getUsername(),
                                properties.getMail().getPassword());
                    }
                }
        );

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(properties.getMail().getSenderAddress()));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(properties.getMail().getReceiverAddress()));
            message.setSubject(title, CHARSET);

            MimeMultipart mimeMultipart = new MimeMultipart();
            MimeBodyPart mimeBodyPart = new MimeBodyPart();

            if (PayloadUtil.isAppReceipt(request.getPayload())) {
                mimeBodyPart.setText("Kvittering (AppReceipt) mottatt.", CHARSET);
            } else {
                mimeBodyPart.setText("Du har fått en BestEdu melding. Se vedlegg for etadata og dokumenter.", CHARSET);

                MeldingType meldingType = BestEduConverter.payloadAsMeldingType(request.getPayload());
                List<DokumentType> docs = meldingType.getJournpost().getDokument();
                for (DokumentType doc : docs) {
                    ByteArrayDataSource ds = new ByteArrayDataSource(doc.getFil().getBase64(), doc.getVeMimeType());
                    MimeBodyPart attachementPart = new MimeBodyPart();
                    attachementPart.setDataHandler(new DataHandler(ds));
                    attachementPart.setFileName(doc.getVeFilnavn());
                    mimeMultipart.addBodyPart(attachementPart);
                    // Set file content to null since we want the payload xml later as attachement
                    doc.getFil().setBase64(null);
                }
                String payload = BestEduConverter.meldingTypeAsString(meldingType);
                MimeBodyPart payloadAttachement = new MimeBodyPart();
                ByteArrayDataSource ds = new ByteArrayDataSource(payload.getBytes(), "application/xml;charset=" + CHARSET);
                payloadAttachement.setDataHandler(new DataHandler(ds));
                payloadAttachement.setFileName("payload.xml");
                mimeMultipart.addBodyPart(payloadAttachement);
            }

            mimeMultipart.addBodyPart(mimeBodyPart);
            message.setContent(mimeMultipart);

            Transport transport = session.getTransport("smtps");

            Long maxMessageSize = getMaxMessageSize(transport);

            if (maxMessageSize != null) {
                long messageSize = getMessageSize(message);

                if (messageSize > maxMessageSize) {
                    String conversationId = request.getEnvelope().getConversationId();

                    if (conversationIds.add(conversationId)) {
                        message.setText("Du har mottatt en BestEdu melding. Denne er for stor for å kunne sendes over e-post. Vennligst logg deg inn på FIKS portalen for å laste den ned.",
                                CHARSET);
                    } else {
                        log.info("Notification email was already sent for conversation ID = {}", conversationId);
                        return;
                    }
                }
            }

            Transport.send(message);
        } catch (
                MessagingException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    private Long getMaxMessageSize(Transport transport) {
        return getMaxSizeFromMailServer(transport)
                .orElseGet(() -> properties.getMail().getMaxSize());
    }

    private Optional<Long> getMaxSizeFromMailServer(Transport transport) {
        if (transport instanceof SMTPTransport) {
            SMTPTransport smtpTransport = (SMTPTransport) transport;
            return Optional.ofNullable(smtpTransport.getExtensionParameter("SIZE"))
                    .map(Long::valueOf);
        }

        return Optional.empty();
    }

    private long getMessageSize(MimeMessage m) {
        try (CountingOutputStream out = new CountingOutputStream(new NullOutputStream())) {
            m.writeTo(out);
            return out.getByteCount() + 100L;
        } catch (IOException | MessagingException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }
}
