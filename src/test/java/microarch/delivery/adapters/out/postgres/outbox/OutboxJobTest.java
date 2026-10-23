package microarch.delivery.adapters.out.postgres.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.val;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Volume;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.domain.model.order.events.OrderAssignedDomainEvent;
import microarch.delivery.core.ports.OrderDomainEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxJobTest {

    @Mock
    private OrderDomainEventProducer orderDomainEventProducer;

    @Mock
    private OutboxJpaRepository jpa;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private OutboxJob job;

    @BeforeEach
    void setUp() {
        job = new OutboxJob(orderDomainEventProducer, jpa, objectMapper);
    }

    /**
     * Builds an unprocessed outbox record for the given event, as the publisher would persist it.
     */
    private OutboxMessage toMessage(final OrderAssignedDomainEvent event) throws Exception {
        return new OutboxMessage(event.getEventId(), event.getClass().getName(), event.getOrderId().toString(), "Order",
                objectMapper.writeValueAsString(event), event.getOccurredOnUtc());
    }

    /**
     * Verifies the happy path: the event is restored from the payload, routed to the producer port, and the message is
     * marked as processed and saved.
     */
    @Test
    void testRunProducesAndMarksProcessed() throws Exception {
        val order = Order.create(UUID.randomUUID(), Location.create(5, 5).getValue(), Volume.create(3).getValue())
                .getValue();
        val event = new OrderAssignedDomainEvent(order);
        val message = toMessage(event);
        when(jpa.findUnprocessedMessages()).thenReturn(List.of(message));

        job.run();

        val captor = ArgumentCaptor.forClass(OrderAssignedDomainEvent.class);
        verify(orderDomainEventProducer).produce(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(order.getId());
        verify(jpa).save(message);
        assertThat(message.getProcessedOnUtc()).isNotNull();
    }

    /**
     * Verifies the retry semantics: when the producer fails, the message stays unprocessed and is not saved, so the
     * next job run picks it up again.
     */
    @Test
    void testRunProducerFailureLeavesUnprocessed() throws Exception {
        val order = Order.create(UUID.randomUUID(), Location.create(5, 5).getValue(), Volume.create(3).getValue())
                .getValue();
        val message = toMessage(new OrderAssignedDomainEvent(order));
        when(jpa.findUnprocessedMessages()).thenReturn(List.of(message));
        doThrow(new RuntimeException("kafka down")).when(orderDomainEventProducer)
                .produce(any(OrderAssignedDomainEvent.class));

        job.run();

        verify(jpa, never()).save(any(OutboxMessage.class));
        assertThat(message.getProcessedOnUtc()).isNull();
    }

    /**
     * Verifies that a corrupted payload is skipped without marking the message as processed.
     */
    @Test
    void testRunCorruptedPayloadLeavesUnprocessed() {
        val message = new OutboxMessage(UUID.randomUUID(), OrderAssignedDomainEvent.class.getName(),
                UUID.randomUUID().toString(), "Order", "not-a-json", Instant.now());
        when(jpa.findUnprocessedMessages()).thenReturn(List.of(message));

        job.run();

        verify(orderDomainEventProducer, never()).produce(any(OrderAssignedDomainEvent.class));
        verify(jpa, never()).save(any(OutboxMessage.class));
        assertThat(message.getProcessedOnUtc()).isNull();
    }
}
