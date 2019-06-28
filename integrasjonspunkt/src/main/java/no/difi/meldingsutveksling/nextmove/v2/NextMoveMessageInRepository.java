package no.difi.meldingsutveksling.nextmove.v2;

import com.querydsl.core.BooleanBuilder;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.NextMoveInMessage;
import no.difi.meldingsutveksling.nextmove.QNextMoveInMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface NextMoveMessageInRepository extends PagingAndSortingRepository<NextMoveInMessage, Long>,
        QuerydslPredicateExecutor<NextMoveInMessage>,
        QuerydslBinderCustomizer<QNextMoveInMessage> {

    @Override
    default void customize(QuerydslBindings bindings, QNextMoveInMessage root) {
        bindings.excluding(root.sbd);
    }

    List<NextMoveInMessage> findByLockTimeoutLessThanEqual(OffsetDateTime now);

    Optional<NextMoveInMessage> findByConversationId(String conversationId);

    default Page<StandardBusinessDocument> find(NextMoveInMessageQueryInput input, Pageable pageable) {
        return findAll(createQuery(input).getValue()
                , pageable)
                .map(NextMoveInMessage::getSbd);
    }

    default Optional<NextMoveInMessage> peek(NextMoveInMessageQueryInput input) {
        return findAll(
                createQuery(input)
                        .and(QNextMoveInMessage.nextMoveInMessage.lockTimeout.isNull())
                        .getValue(),
                PageRequests.FIRST_BY_LAST_UPDATED_ASC)
                .getContent()
                .stream()
                .findFirst();
    }

    default BooleanBuilder createQuery(NextMoveInMessageQueryInput input) {
        BooleanBuilder builder = new BooleanBuilder();

        QNextMoveInMessage nextMoveInMessage = QNextMoveInMessage.nextMoveInMessage;

        if (input.getConversationId() != null) {
            builder.and(nextMoveInMessage.conversationId.eq(input.getConversationId()));
        }

        if (input.getReceiverIdentifier() != null) {
            builder.and(nextMoveInMessage.receiverIdentifier.eq(input.getReceiverIdentifier()));
        }

        if (input.getSenderIdentifier() != null) {
            builder.and(nextMoveInMessage.senderIdentifier.eq(input.getSenderIdentifier()));
        }

        if (input.getServiceIdentifier() != null) {
            builder.and(nextMoveInMessage.serviceIdentifier.eq(ServiceIdentifier.valueOf(input.getServiceIdentifier())));
        }

        return builder;
    }
}
