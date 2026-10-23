package microarch.delivery.adapters.out.postgres.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import libs.ddd.Aggregate;
import libs.ddd.DomainEvent;
import libs.ddd.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

/**
 * Outbox implementation of {@link DomainEventPublisher}: serializes accumulated domain events into the outbox table
 * within the caller's transaction instead of publishing to the transport directly, eliminating the risk of losing
 * events on a crash between the aggregate save and the publish.
 */
@Repository
@Primary
@RequiredArgsConstructor
public class OutboxDomainEventPublisher implements DomainEventPublisher {

    private final OutboxJpaRepository jpa;
    private final ObjectMapper objectMapper;

    /**
     * Persists every accumulated domain event of each aggregate as an {@link OutboxMessage} and clears the aggregates'
     * event lists; runs inside the same transaction as the aggregate save, so either both the aggregate and the events
     * are committed or nothing is.
     *
     * @param aggregates
     *            aggregates whose accumulated domain events should be persisted
     */
    @Override
    public void publish(final Iterable<? extends Aggregate<?>> aggregates) {
        for (val aggregate : aggregates) {
            for (val domainEvent : aggregate.getDomainEvents()) {
                jpa.save(toOutboxMessage(aggregate, domainEvent));
            }
            aggregate.clearDomainEvents();
        }
    }

    /**
     * Serializes the event into a JSON payload and wraps it into an {@link OutboxMessage}.
     *
     * @param aggregate
     *            the aggregate that raised the event
     * @param domainEvent
     *            the raised domain event
     *
     * @return the outbox record ready to persist
     */
    private OutboxMessage toOutboxMessage(final Aggregate<?> aggregate, final DomainEvent domainEvent) {
        try {
            val payload = objectMapper.writeValueAsString(domainEvent);
            return new OutboxMessage(domainEvent.getEventId(), domainEvent.getClass().getName(),
                    aggregate.getId().toString(), aggregate.getClass().getSimpleName(), payload,
                    domainEvent.getOccurredOnUtc());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to serialize domainEvent for Outbox", ex);
        }
    }
}
