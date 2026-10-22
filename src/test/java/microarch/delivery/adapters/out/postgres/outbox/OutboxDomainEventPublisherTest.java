package microarch.delivery.adapters.out.postgres.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.val;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Volume;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.domain.model.order.events.OrderAssignedDomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxDomainEventPublisherTest {

    @Mock
    private OutboxJpaRepository jpa;

    private OutboxDomainEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxDomainEventPublisher(jpa, new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    /**
     * Verifies that each accumulated domain event is persisted as an unprocessed {@link OutboxMessage} carrying the
     * event id, event type, aggregate reference, and JSON payload, and that the aggregate is cleared afterwards.
     */
    @Test
    void testPublishPersistsOutboxMessageAndClears() {
        val order = Order.create(UUID.randomUUID(), Location.create(5, 5).getValue(), Volume.create(3).getValue())
                .getValue();
        order.markAsAssigned();
        val event = (OrderAssignedDomainEvent) order.getDomainEvents().getFirst();

        publisher.publish(List.of(order));

        val captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(jpa).save(captor.capture());
        val saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(event.getEventId());
        assertThat(saved.getEventType()).isEqualTo(OrderAssignedDomainEvent.class.getName());
        assertThat(saved.getAggregateId()).isEqualTo(order.getId().toString());
        assertThat(saved.getAggregateType()).isEqualTo("Order");
        assertThat(saved.getPayload()).contains(order.getId().toString());
        assertThat(saved.getProcessedOnUtc()).isNull();
        assertThat(order.getDomainEvents()).isEmpty();
    }
}
