package microarch.delivery.core.application.command;

import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Volume;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.domain.model.order.OrderStatus;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateOrderHandlerImplTest {

    @Mock
    private OrderRepository orderRepository;

    private CreateOrderHandlerImpl handler;

    @BeforeEach
    void setUp() {
        handler = new CreateOrderHandlerImpl(orderRepository);
    }

    /* ---------- argument validation ---------- */

    @Test
    void testNullCommandFails() {
        val result = handler.handle(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_OBJ_IS_NULL);
        verifyNoInteractions(orderRepository);
    }

    /* ---------- happy path ---------- */

    @Test
    void testSuccessCreatesOrderInCreatedStatus() {
        val orderId = UUID.randomUUID();
        val persisted = Order.create(orderId, Location.create(5, 5).getValue(), Volume.create(3).getValue()).getValue();
        when(orderRepository.create(any(Order.class))).thenReturn(persisted);

        val result = handler.handle(new CreateOrderCommand(orderId, "Russia", "Moscow", "Tverskaya", "1", "2",
                Volume.create(3).getValue()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getId()).isEqualTo(orderId);

        val captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).create(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(orderId);
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.Created);
        assertThat(captor.getValue().getVolume()).isEqualTo(Volume.create(3).getValue());
    }

    @Test
    void testSuccessWithBlankApartment() {
        val orderId = UUID.randomUUID();
        val persisted = Order.create(orderId, Location.create(5, 5).getValue(), Volume.create(1).getValue()).getValue();
        when(orderRepository.create(any(Order.class))).thenReturn(persisted);

        val result = handler.handle(new CreateOrderCommand(orderId, "Russia", "Moscow", "Tverskaya", "1", null,
                Volume.create(1).getValue()));

        assertThat(result.isSuccess()).isTrue();
        verify(orderRepository).create(any(Order.class));
    }
}
