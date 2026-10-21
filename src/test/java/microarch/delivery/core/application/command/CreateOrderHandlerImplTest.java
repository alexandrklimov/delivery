package microarch.delivery.core.application.command;

import libs.errs.Error;
import libs.errs.Result;
import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Volume;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.domain.model.order.OrderStatus;
import microarch.delivery.core.ports.GeoService;
import microarch.delivery.core.ports.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrderHandlerImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private GeoService geoService;

    private CreateOrderHandlerImpl handler;

    @BeforeEach
    void setUp() {
        handler = new CreateOrderHandlerImpl(orderRepository, geoService);
    }

    /* ---------- argument validation ---------- */

    /**
     * Verifies that a {@code null} command is rejected with the {@code object.is.null} error code.
     */
    @Test
    void testNullCommandFails() {
        val result = handler.handle(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_OBJ_IS_NULL);
        verifyNoInteractions(orderRepository);
    }

    /* ---------- geo integration ---------- */

    /**
     * Verifies that the handler propagates the {@link GeoService} failure and skips order creation when the
     * address cannot be resolved into a {@code Location}.
     */
    @Test
    void testGeoServiceFailureFails() {
        when(geoService.getLocation("Tverskaya"))
                .thenReturn(Result.failure(Error.of(Constants.ERR_CODE_GEO_SERVICE_UNAVAILABLE, "Geo is down")));

        val result = handler
                .handle(new CreateOrderCommand(UUID.randomUUID(), "Russia", "Moscow", "Tverskaya", "1", "2", 3));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_GEO_SERVICE_UNAVAILABLE);
        verifyNoInteractions(orderRepository);
    }

    /* ---------- value-object validation (owned by the handler) ---------- */

    /**
     * Verifies that a non-positive volume is rejected by the handler via {@code Volume#create} — the command carries a
     * primitive, so the boundary check lives in the use case.
     */
    @Test
    void testNonPositiveVolumeFails() {
        when(geoService.getLocation("Tverskaya")).thenReturn(Result.success(Location.create(5, 5).getValue()));

        val result = handler
                .handle(new CreateOrderCommand(UUID.randomUUID(), "Russia", "Moscow", "Tverskaya", "1", "2", 0));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("value.must.be.greater.or.equal");
        verifyNoInteractions(orderRepository);
    }

    /* ---------- happy path ---------- */

    /**
     * Verifies the happy path: the order is created in the {@code Created} status and persisted with the
     * {@code Location} resolved by the {@link GeoService}, not with a random one.
     */
    @Test
    void testSuccessCreatesOrderInCreatedStatus() {
        val orderId = UUID.randomUUID();
        val geoLocation = Location.create(5, 5).getValue();
        when(geoService.getLocation("Tverskaya")).thenReturn(Result.success(geoLocation));
        val persisted = Order.create(orderId, geoLocation, Volume.create(3).getValue()).getValue();
        when(orderRepository.create(any(Order.class))).thenReturn(persisted);

        val result = handler.handle(new CreateOrderCommand(orderId, "Russia", "Moscow", "Tverskaya", "1", "2", 3));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getId()).isEqualTo(orderId);

        val captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).create(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(orderId);
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.Created);
        assertThat(captor.getValue().getLocation()).isEqualTo(geoLocation);
        assertThat(captor.getValue().getVolume()).isEqualTo(Volume.create(3).getValue());
    }

    /**
     * Verifies that a blank apartment is accepted and does not block order creation.
     */
    @Test
    void testSuccessWithBlankApartment() {
        val orderId = UUID.randomUUID();
        when(geoService.getLocation("Tverskaya")).thenReturn(Result.success(Location.create(5, 5).getValue()));
        val persisted = Order.create(orderId, Location.create(5, 5).getValue(), Volume.create(1).getValue()).getValue();
        when(orderRepository.create(any(Order.class))).thenReturn(persisted);

        val result = handler.handle(new CreateOrderCommand(orderId, "Russia", "Moscow", "Tverskaya", "1", null, 1));

        assertThat(result.isSuccess()).isTrue();
        verify(orderRepository).create(any(Order.class));
    }
}
