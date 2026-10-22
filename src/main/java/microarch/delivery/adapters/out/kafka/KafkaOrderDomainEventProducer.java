package microarch.delivery.adapters.out.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import microarch.delivery.core.domain.model.order.events.OrderAssignedDomainEvent;
import microarch.delivery.core.domain.model.order.events.OrderCompletedDomainEvent;
import microarch.delivery.core.ports.OrderDomainEventProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import queues.order.events.OrderEventsProto;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Kafka adapter for the {@code Order} aggregate: maps order domain events into the {@code order.events} protobuf
 * integration contract and publishes them to the {@code order.events} topic keyed by the order id.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaOrderDomainEventProducer implements OrderDomainEventProducer {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${app.kafka.order-events-topic}")
    private String topic;

    /**
     * Publishes the "order assigned" integration event to Kafka.
     *
     * @param event the domain event to publish
     */
    @Override
    public void produce(final OrderAssignedDomainEvent event) {
        val integrationEvent = OrderEventsProto.OrderAssignedIntegrationEvent.newBuilder()
                .setOrderId(event.getOrderId().toString()).build();
        send(event.getOrderId(), integrationEvent.toByteArray());
    }

    /**
     * Publishes the "order completed" integration event to Kafka.
     *
     * @param event the domain event to publish
     */
    @Override
    public void produce(final OrderCompletedDomainEvent event) {
        val integrationEvent = OrderEventsProto.OrderCompletedIntegrationEvent.newBuilder()
                .setOrderId(event.getOrderId().toString()).build();
        send(event.getOrderId(), integrationEvent.toByteArray());
    }

    /**
     * Synchronously sends the serialized integration event keyed by the order id to preserve per-order ordering.
     *
     * @param orderId order id used as the message key
     * @param payload serialized integration event bytes
     */
    private void send(final UUID orderId, final byte[] payload) {
        try {
            kafkaTemplate.send(topic, orderId.toString(), payload).get();
            log.info("Published integration event for orderId={} to topic '{}'", orderId, topic);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Kafka publish interrupted", ex);
        } catch (ExecutionException ex) {
            throw new RuntimeException("Kafka publish failed", ex);
        }
    }
}
