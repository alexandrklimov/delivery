package microarch.delivery.adapters.in.kafka;

import libs.errs.Error;
import libs.errs.Result;
import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.application.command.CreateOrderCommand;
import microarch.delivery.core.application.command.CreateOrderHandler;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Volume;
import microarch.delivery.core.domain.model.order.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import queues.basket.events.BasketEventsProto;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketConfirmedConsumerTest {

    @Mock
    private CreateOrderHandler createOrderHandler;

    private BasketConfirmedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new BasketConfirmedConsumer(createOrderHandler);
    }

    /**
     * Builds a serialized {@code BasketConfirmedIntegrationEvent} with the given basket id.
     *
     * @param basketId
     *            basket id to embed into the event
     *
     * @return raw event bytes as they arrive from Kafka
     */
    private byte[] newEvent(final String basketId) {
        return BasketEventsProto.BasketConfirmedIntegrationEvent
                .newBuilder().setBasketId(basketId).setAddress(BasketEventsProto.Address.newBuilder()
                        .setCountry("Russia").setCity("Moscow").setStreet("Tverskaya").setHouse("1").setApartment("2"))
                .setVolume(3).build().toByteArray();
    }

    /**
     * Verifies the happy path: the consumed event is mapped into a {@link CreateOrderCommand} (basket id becomes the
     * order id, address and volume are passed through) and delegated to {@link CreateOrderHandler}.
     */
    @Test
    void testSuccessCreatesOrder() {
        val basketId = UUID.randomUUID();
        val order = Order.create(basketId, Location.create(5, 5).getValue(), Volume.create(3).getValue()).getValue();
        when(createOrderHandler.handle(any(CreateOrderCommand.class))).thenReturn(Result.success(order));

        consumer.listen(newEvent(basketId.toString()));

        verify(createOrderHandler)
                .handle(new CreateOrderCommand(basketId, "Russia", "Moscow", "Tverskaya", "1", "2", 3));
    }

    /**
     * Verifies that a malformed protobuf payload is re-thrown as a wrapped exception instead of being swallowed.
     */
    @Test
    void testMalformedPayloadThrows() {
        // field 1 (string), declared length 5, but only 1 byte follows -> truncated message
        val payload = new byte[] { 0x0A, 0x05, 'a' };

        assertThatThrownBy(() -> consumer.listen(payload)).isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to deserialize basket event");
    }

    /**
     * Verifies the acknowledged-failure decision: a use case {@code Result} failure is logged and the listener
     * completes normally, so the message is not re-delivered.
     */
    @Test
    void testHandlerFailureIsAcknowledged() {
        val basketId = UUID.randomUUID();
        when(createOrderHandler.handle(any(CreateOrderCommand.class)))
                .thenReturn(Result.failure(Error.of(Constants.ERR_CODE_GEO_SERVICE_UNAVAILABLE, "Geo is down")));

        assertThatCode(() -> consumer.listen(newEvent(basketId.toString()))).doesNotThrowAnyException();

        verify(createOrderHandler).handle(any(CreateOrderCommand.class));
    }
}
