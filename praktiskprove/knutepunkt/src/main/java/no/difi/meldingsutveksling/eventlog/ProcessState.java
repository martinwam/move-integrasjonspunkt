package no.difi.meldingsutveksling.eventlog;

/**
 * @author Kubilay Karayilan
 *         Kubilay.Karayilan@inmeta.no
 *         created on 10.11.2014.
 */
public enum ProcessState {
    WRONG_TYPE,
    DECRYPTION_ERROR,
    DECRYPTION_SUCCESS,
    SIGNATURE_VALIDATION_ERROR,
    SIGNATURE_VALIDATED,
    NO_ARKIVE_UNAVAILABLE,
    ARKIVE_RESPONSE_NULL,
    SBD_PACKAGING_FAIL,
    SBD_PACKED,
    SBD_SENT,
    BEST_EDU_SENT,
    MESSAGE_SENT,
    MESSAGE_SEND_FAIL,
    MESSAGE_RECIEVED,
    LEVERINGS_KVITTERING_SENT,
    LEVERINGS_KVITTERING_SENT_FAILED,
    BESTEDU_EXTRACTED,
    PAYLOAD_EXTRACTED,
    SBD_RECIEVED,
    BEST_EDU_RECIEVED,
    KVITTERING_MOTTATT,
    ERROR_INVALID_OR_MISSING_SENDER,  AAPNINGS_KVITTERING_SENT, SOME_OTHER_EXCEPTION, CAN_RECEIVE_INVOKED

}
