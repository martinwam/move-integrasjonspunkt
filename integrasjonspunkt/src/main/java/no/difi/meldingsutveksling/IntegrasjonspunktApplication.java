package no.difi.meldingsutveksling;

import com.sun.xml.ws.transport.http.servlet.WSSpringServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import javax.crypto.Cipher;
import java.security.NoSuchAlgorithmException;

@SpringBootApplication(exclude = {SolrAutoConfiguration.class})
public class IntegrasjonspunktApplication extends SpringBootServletInitializer {
    private static final Logger log = LoggerFactory.getLogger(IntegrasjonspunktApplication.class);

    @Bean
    public ServletRegistrationBean servletNoArk() {
        WSSpringServlet servlet = new WSSpringServlet();
        ServletRegistrationBean reg = new ServletRegistrationBean(servlet, "/noarkExchange", "/receive");
        reg.setLoadOnStartup(1);
        return reg;
    }

    public static void main(String[] args) {
        try {
            if(!validateJCE()){
                logMissingJCE(null);
                return;
            }
            ConfigurableApplicationContext context = SpringApplication.run(IntegrasjonspunktApplication.class, args);
        } catch (SecurityException se) {
            logMissingJCE(se);
        }
    }

    private static void logMissingJCE(Exception e)
    {
        String message =
                "Failed startup. Possibly unlimited security policy files that is not updated." +
                        "/r/nTo fix this, download and replace policy files for the apropriate java version (found in ${java.home}/jre/lib/security/)" +
                        "/r/n- Java7: http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html" +
                        "/r/n- Java8: http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html";

        System.out.println(message);
        log.error(message);
        if(e != null){
            log.error(e.getMessage());
        }
    }

    private static boolean validateJCE()
    {
        try {
            int maxKeyLen = Cipher.getMaxAllowedKeyLength("AES");
            if(maxKeyLen > 128)
            {
                return true;
            }
        }
        catch(NoSuchAlgorithmException ex) {
        }
        return false;

    }
}
