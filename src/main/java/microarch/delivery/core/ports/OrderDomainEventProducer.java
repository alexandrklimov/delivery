package microarch.delivery.core.ports;

import microarch.delivery.core.domain.model.order.events.OrderAssignedDomainEvent;
import microarch.delivery.core.domain.model.order.events.OrderCompletedDomainEvent;

/**
 * Output port for publishing {@code Order} aggregate domain events outside the service boundary; one aggregate — one
 * producer — one queue, so events of other aggregates must get their own dedicated ports.
 */
public interface OrderDomainEventProducer {

    /**
     * Publishes the "order assigned" domain event to the external transport.
     *
     * @param event
     *            the domain event to publish
     */
    void produce(final OrderAssignedDomainEvent event);

    /**
     * Publishes the "order completed" domain event to the external transport.
     *
     * @param event
     *            the domain event to publish
     */
    void produce(final OrderCompletedDomainEvent event);
}
