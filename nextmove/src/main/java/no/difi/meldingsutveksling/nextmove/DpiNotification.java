package no.difi.meldingsutveksling.nextmove;

import lombok.Data;

import javax.persistence.Embeddable;

@Data
@Embeddable
public class DpiNotification {

    String emailText;
    String smsText;
}
