package no.difi.meldingsutveksling.receipt;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class MessageStatusFactory {

    private final Clock clock;

    public MessageStatus getMessageStatus(ReceiptStatus status) {
        return MessageStatus.of(status, OffsetDateTime.now(clock), null);
    }

    public MessageStatus getMessageStatus(ReceiptStatus status, OffsetDateTime lastUpdate) {
        return MessageStatus.of(status, lastUpdate, null);
    }

    public MessageStatus getMessageStatus(ReceiptStatus status, String description) {
        return MessageStatus.of(status, OffsetDateTime.now(clock), description);
    }
}
