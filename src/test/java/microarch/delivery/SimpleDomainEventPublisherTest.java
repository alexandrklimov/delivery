package microarch.delivery;

import libs.ddd.DomainEvent;
import lombok.val;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Volume;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.domain.model.order.events.OrderAssignedDomainEvent;
import microarch.delivery.core.ports.OrderDomainEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SimpleDomainEventPublisherTest {

    @Mock
    private OrderDomainEventProducer orderDomainEventProducer;

    private SimpleDomainEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new SimpleDomainEventPublisher(orderDomainEventProducer);
    }

    /**
     * Verifies that events accumulated by the aggregate are routed to the aggregate-dedicated producer port and the
     * aggregate's event list is cleared afterwards.
     */
    @Test
    void testPublishRoutesOrderEventsAndClears() {
        val order = Order.create(UUID.randomUUID(), Location.create(5, 5).getValue(), Volume.create(3).getValue())
                .getValue();
        order.markAsAssigned();
        val event = (OrderAssignedDomainEvent) order.getDomainEvents().getFirst();

        publisher.publish(List.of(order));

        verify(orderDomainEventProducer).produce(event);
        assertThat(order.getDomainEvents()).isEmpty();
    }

    /**
     * Verifies that an event of an unsupported aggregate is rejected instead of being silently dropped.
     */
    @Test
    void testPublishUnsupportedEventThrows() {
        val order = Order.create(UUID.randomUUID(), Location.create(5, 5).getValue(), Volume.create(3).getValue())
                .getValue();
        order.raiseDomainEvent(new DomainEvent(order) {
        });

        assertThatThrownBy(() -> publisher.publish(List.of(order))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported domain event");
    }
}
