package microarch.delivery.core.domain.model.order.events;

import libs.ddd.DomainEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import microarch.delivery.core.domain.model.order.Order;

import java.util.UUID;

/**
 * Domain event stating the occurred fact "the order has been assigned to a courier": raised by the {@link Order}
 * aggregate after a successful transition into the {@code Assigned} status.
 */
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
public final class OrderAssignedDomainEvent extends DomainEvent {

    private final UUID orderId;

    /**
     * Captures the fact for the given aggregate state.
     *
     * @param order
     *            the order that has just been assigned
     */
    public OrderAssignedDomainEvent(final Order order) {
        super(order);
        this.orderId = order.getId();
    }
}
