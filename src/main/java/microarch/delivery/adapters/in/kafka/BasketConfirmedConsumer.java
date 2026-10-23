package microarch.delivery.adapters.in.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import microarch.delivery.core.application.command.CreateOrderCommand;
import microarch.delivery.core.application.command.CreateOrderHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import queues.basket.events.BasketEventsProto;

import java.util.Base64;
import java.util.UUID;

/**
 * Inbound Kafka adapter for the {@code basket.events} topic: consumes {@code BasketConfirmedIntegrationEvent} messages
 * and drives the "Create Order" use case.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BasketConfirmedConsumer {

    private final CreateOrderHandler createOrderHandler;

    /**
     * Consumes one raw {@code BasketConfirmedIntegrationEvent} payload.
     * <p>
     * The routine follows these steps:
     * <ul>
     * <li>logs the raw payload (base64) before deserialization, as required by the Kafka rules;</li>
     * <li>parses the protobuf message, re-throwing wrapped deserialization failures;</li>
     * <li>maps the event into a {@link CreateOrderCommand} (basket id becomes the order id) and invokes
     * {@link CreateOrderHandler};</li>
     * <li>on a use case failure logs the error and acknowledges the message — a business rejection is not recoverable
     * by retrying.</li>
     * </ul>
     *
     * @param payload
     *            raw message bytes from the {@code basket.events} topic
     */
    @KafkaListener(topics = "${app.kafka.basket-events-topic}")
    public void listen(final byte[] payload) {
        log.debug("Received raw Kafka payload from 'basket.events': {}", Base64.getEncoder().encodeToString(payload));

        val event = parse(payload);
        createOrderHandler.handle(toCommand(event))
                .onFailure(error -> log.error("Failed to create order from basket event basketId='{}': {}",
                        event.getBasketId(), error))
                .onSuccess(order -> log.info("Order created from basket event basketId='{}': orderId={}",
                        event.getBasketId(), order.getId()));
    }

    /**
     * Parses the raw payload into a {@code BasketConfirmedIntegrationEvent}.
     *
     * @param payload
     *            raw message bytes
     *
     * @return the parsed integration event
     *
     * @throws IllegalStateException
     *             when the payload is not a valid protobuf message (wrapped deserialization failure)
     */
    private BasketEventsProto.BasketConfirmedIntegrationEvent parse(final byte[] payload) {
        try {
            return BasketEventsProto.BasketConfirmedIntegrationEvent.parseFrom(payload);
        } catch (InvalidProtocolBufferException ex) {
            log.error("Failed to deserialize basket event. Raw payload: {}",
                    Base64.getEncoder().encodeToString(payload), ex);
            throw new IllegalStateException("Failed to deserialize basket event", ex);
        }
    }

    /**
     * Maps the integration event into a {@link CreateOrderCommand}: the basket id becomes the order id, the event
     * address and volume are passed through. Event fields not used by the use case ({@code items},
     * {@code delivery_period}) are ignored.
     *
     * @param event
     *            parsed {@code BasketConfirmedIntegrationEvent}
     *
     * @return the command for the "Create Order" use case
     */
    private CreateOrderCommand toCommand(final BasketEventsProto.BasketConfirmedIntegrationEvent event) {
        val address = event.getAddress();
        return new CreateOrderCommand(UUID.fromString(event.getBasketId()), address.getCountry(), address.getCity(),
                address.getStreet(), address.getHouse(), address.getApartment(), event.getVolume());
    }
}
