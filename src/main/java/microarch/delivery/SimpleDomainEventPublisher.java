package microarch.delivery;

import libs.ddd.Aggregate;
import libs.ddd.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.val;
import microarch.delivery.core.domain.model.order.events.OrderAssignedDomainEvent;
import microarch.delivery.core.domain.model.order.events.OrderCompletedDomainEvent;
import microarch.delivery.core.ports.OrderDomainEventProducer;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Domain Event Handler: publishes domain events accumulated by aggregates to the matching per-aggregate producer port
 * and clears the aggregates afterwards; aggregates stay isolated from the transport infrastructure.
 */
@Component
@Primary
@RequiredArgsConstructor
public class SimpleDomainEventPublisher implements DomainEventPublisher {

    private final OrderDomainEventProducer orderDomainEventProducer;

    /**
     * Routes every accumulated domain event of each aggregate to its aggregate-dedicated producer port and then clears
     * the aggregates' event lists.
     *
     * @param aggregates aggregates whose accumulated domain events should be published
     * @throws IllegalArgumentException when an event of an unsupported aggregate is encountered
     */
    @Override
    public void publish(final Iterable<? extends Aggregate<?>> aggregates) {
        for (val aggregate : aggregates) {
            for (val event : aggregate.getDomainEvents()) {
                switch (event) {
                    case OrderAssignedDomainEvent e -> orderDomainEventProducer.produce(e);
                    case OrderCompletedDomainEvent e -> orderDomainEventProducer.produce(e);
                    default -> throw new IllegalArgumentException(
                            "Unsupported domain event: " + event.getClass().getName());
                }
            }
            aggregate.clearDomainEvents();
        }
    }
}
