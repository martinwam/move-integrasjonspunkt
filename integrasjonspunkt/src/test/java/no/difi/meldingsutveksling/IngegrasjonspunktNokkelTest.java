package no.difi.meldingsutveksling;

import java.security.PrivateKey;
import static junit.framework.Assert.assertNotNull;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Temporary ignored. Functionality is to be moved, reason queue handling.")
public class IngegrasjonspunktNokkelTest {

    private IntegrasjonspunktNokkel nokkel;

    @Before
    public void init() {
        IntegrasjonspunktProperties properties = new IntegrasjonspunktProperties();

        properties.setCert(new IntegrasjonspunktProperties.Certificate());
        properties.getCert().setAlias("974720760");
        properties.getCert().setPassword("changeit");
        properties.getCert().setPath("src/main/resources/test-certificates.jks");

        nokkel = new IntegrasjonspunktNokkel(properties);

    }

    @Test
    public void testLastingavprivatnokkelfraTestressurser() {

        PrivateKey key = nokkel.loadPrivateKey();
        assertNotNull(key.getFormat());
    }

}
