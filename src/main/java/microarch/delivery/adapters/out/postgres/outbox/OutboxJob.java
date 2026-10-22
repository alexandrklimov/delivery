package microarch.delivery.adapters.out.postgres.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import libs.ddd.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import microarch.delivery.core.domain.model.order.events.OrderAssignedDomainEvent;
import microarch.delivery.core.domain.model.order.events.OrderCompletedDomainEvent;
import microarch.delivery.core.ports.OrderDomainEventProducer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polling publisher: periodically reads unprocessed outbox messages, restores the domain events from their JSON
 * payloads, and routes them to the aggregate-dedicated producer port. Failed dispatches stay unprocessed and are
 * retried on the next run, giving at-least-once delivery.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxJob {

    private final OrderDomainEventProducer orderDomainEventProducer;
    private final OutboxJpaRepository jpa;
    private final ObjectMapper objectMapper;

    /**
     * Dispatches all currently unprocessed outbox messages.
     */
    @Scheduled(fixedDelay = 1_000)
    public void run() {
        val outboxMessages = jpa.findUnprocessedMessages();
        for (val outboxMessage : outboxMessages) {
            try {
                produce(deserialize(outboxMessage));
                outboxMessage.markAsProcessed();
                jpa.save(outboxMessage);
            } catch (Exception ex) {
                log.error("Failed to publish outbox message {}", outboxMessage.getId(), ex);
            }
        }
    }

    /**
     * Restores the domain event from the outbox record payload.
     *
     * @param outboxMessage
     *            the outbox record to decode
     *
     * @return the deserialized domain event
     *
     * @throws Exception
     *             when the event class cannot be loaded or the payload is not valid JSON of that type
     */
    private DomainEvent deserialize(final OutboxMessage outboxMessage) throws Exception {
        val eventClass = Class.forName(outboxMessage.getEventType());
        val eventObject = objectMapper.readValue(outboxMessage.getPayload(), eventClass);

        if (!(eventObject instanceof DomainEvent domainEvent))
            throw new IllegalStateException("Invalid outbox message type: " + eventClass);

        return domainEvent;
    }

    /**
     * Routes the domain event to its aggregate-dedicated producer port.
     *
     * @param domainEvent
     *            the restored domain event
     */
    private void produce(final DomainEvent domainEvent) {
        switch (domainEvent) {
        case OrderAssignedDomainEvent e -> orderDomainEventProducer.produce(e);
        case OrderCompletedDomainEvent e -> orderDomainEventProducer.produce(e);
        default -> throw new IllegalArgumentException("Unsupported domain event: " + domainEvent.getClass().getName());
        }
    }
}
