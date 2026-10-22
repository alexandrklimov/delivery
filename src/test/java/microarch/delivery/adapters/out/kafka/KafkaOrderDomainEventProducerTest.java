package microarch.delivery.adapters.out.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.val;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Volume;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.domain.model.order.events.OrderAssignedDomainEvent;
import microarch.delivery.core.domain.model.order.events.OrderCompletedDomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import queues.order.events.OrderEventsProto;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaOrderDomainEventProducerTest {

    private static final String TOPIC = "order.events";

    @Mock
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    private KafkaOrderDomainEventProducer producer;

    @BeforeEach
    void setUp() {
        producer = new KafkaOrderDomainEventProducer(kafkaTemplate);
        ReflectionTestUtils.setField(producer, "topic", TOPIC);
    }

    private static Order newOrder() {
        return Order.create(UUID.randomUUID(), Location.create(5, 5).getValue(), Volume.create(3).getValue())
                .getValue();
    }

    /**
     * Verifies that the "order assigned" domain event is published to the {@code order.events} topic keyed by the order
     * id and mapped into the protobuf integration contract.
     */
    @Test
    void testProduceAssignedEvent() throws InvalidProtocolBufferException {
        val order = newOrder();
        val event = new OrderAssignedDomainEvent(order);
        when(kafkaTemplate.send(anyString(), anyString(), any(byte[].class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        producer.produce(event);

        val captor = ArgumentCaptor.forClass(byte[].class);
        verify(kafkaTemplate).send(eq(TOPIC), eq(order.getId().toString()), captor.capture());
        val integrationEvent = OrderEventsProto.OrderAssignedIntegrationEvent.parseFrom(captor.getValue());
        assertThat(integrationEvent.getOrderId()).isEqualTo(order.getId().toString());
    }

    /**
     * Verifies that the "order completed" domain event is published to the {@code order.events} topic keyed by the
     * order id and mapped into the protobuf integration contract.
     */
    @Test
    void testProduceCompletedEvent() throws InvalidProtocolBufferException {
        val order = newOrder();
        val event = new OrderCompletedDomainEvent(order);
        when(kafkaTemplate.send(anyString(), anyString(), any(byte[].class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        producer.produce(event);

        val captor = ArgumentCaptor.forClass(byte[].class);
        verify(kafkaTemplate).send(eq(TOPIC), eq(order.getId().toString()), captor.capture());
        val integrationEvent = OrderEventsProto.OrderCompletedIntegrationEvent.parseFrom(captor.getValue());
        assertThat(integrationEvent.getOrderId()).isEqualTo(order.getId().toString());
    }

    /**
     * Verifies that a failed Kafka send is re-thrown as a runtime exception instead of being swallowed.
     */
    @Test
    void testSendFailureThrows() {
        val order = newOrder();
        when(kafkaTemplate.send(anyString(), anyString(), any(byte[].class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        assertThatThrownBy(() -> producer.produce(new OrderAssignedDomainEvent(order)))
                .isInstanceOf(RuntimeException.class).hasMessage("Kafka publish failed");
    }
}
