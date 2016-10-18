package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.logging.Audit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

import static no.difi.meldingsutveksling.receipt.MessageReceiptMarker.markerFrom;

/**
 * Periodically checks non final receipts, and their respective services for updates.
 */
@Component
public class ReceiptPolling {


    private static final Logger log = LoggerFactory.getLogger(ReceiptPolling.class);

    @Autowired
    private IntegrasjonspunktProperties props;

    @Autowired
    private MessageReceiptRepository messageReceiptRepository;

    @PostConstruct
    private void addTestData() {
        // TODO: fjernes etter test!
        messageReceiptRepository.save(MessageReceipt.of("bb8323b9-1023-4046-b620-63c4f9120b62", ServiceIdentifier.DPV));
    }

    @Scheduled(fixedRate = 30000)
    public void checkReceiptStatus() {
        if (!props.getFeature().isEnableReceipts()) {
            return;
        }

        List<MessageReceipt> receipts = messageReceiptRepository.findByReceived(false);

        receipts.forEach(r -> {
            log.info(markerFrom(r), "Checking status");
            ReceiptStrategy strategy = ReceiptStrategyFactory.getFactory(r);
            if (strategy.checkReceived(r)) {
                Audit.info("Changed status to \"received\"", markerFrom(r));
                r.setReceived(true);
                messageReceiptRepository.save(r);
            }
        });

    }
}
