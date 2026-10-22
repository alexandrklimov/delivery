package microarch.delivery.core.application.command;

import libs.ddd.DomainEventPublisher;
import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Volume;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.domain.model.order.OrderStatus;
import microarch.delivery.core.ports.CourierRepository;
import microarch.delivery.core.ports.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompleteOrderHandlerImplTest {

    @Mock
    private CourierRepository courierRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private CompleteOrderHandlerImpl handler;

    @BeforeEach
    void setUp() {
        handler = new CompleteOrderHandlerImpl(courierRepository, orderRepository, domainEventPublisher);
    }

    private static Courier newCourier(final int x, final int y) {
        return Courier.create("Ivan", Location.create(x, y).getValue()).getValue();
    }

    private static Order newAssignedOrder(final int x, final int y) {
        val order = Order.create(UUID.randomUUID(), Location.create(x, y).getValue(), Volume.create(3).getValue())
                .getValue();
        order.markAsAssigned();
        return order;
    }

    /* ---------- argument validation ---------- */

    @Test
    void testNullCommandFails() {
        val result = handler.handle(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_OBJ_IS_NULL);
        verifyNoInteractions(courierRepository);
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(domainEventPublisher);
    }

    /* ---------- not found guards ---------- */

    @Test
    void testCourierNotFoundFails() {
        val courierId = UUID.randomUUID();
        when(courierRepository.findById(courierId)).thenReturn(Optional.empty());

        val result = handler.handle(new CompleteOrderCommand(courierId, UUID.randomUUID()));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("courier.not.found");
        verifyNoInteractions(orderRepository);
        verify(courierRepository, never()).update(any(Courier.class));
    }

    @Test
    void testOrderNotFoundFails() {
        val courier = newCourier(5, 5);
        val orderId = UUID.randomUUID();
        when(courierRepository.findById(courier.getId())).thenReturn(Optional.of(courier));
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        val result = handler.handle(new CompleteOrderCommand(courier.getId(), orderId));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("order.not.found");
        verify(orderRepository, never()).update(any(Order.class));
        verify(courierRepository, never()).update(any(Courier.class));
    }

    /* ---------- domain guards ---------- */

    @Test
    void testCourierDoesNotHaveAssignmentFails() {
        val courier = newCourier(5, 5);
        val order = newAssignedOrder(6, 5);
        when(courierRepository.findById(courier.getId())).thenReturn(Optional.of(courier));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        val result = handler.handle(new CompleteOrderCommand(courier.getId(), order.getId()));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("courier.order.not.found");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.Assigned);
        verify(orderRepository, never()).update(any(Order.class));
        verify(courierRepository, never()).update(any(Courier.class));
    }

    @Test
    void testCourierOutOfCompletionZoneFails() {
        val courier = newCourier(1, 1);
        val order = newAssignedOrder(9, 9);
        courier.assignOrder(order);
        when(courierRepository.findById(courier.getId())).thenReturn(Optional.of(courier));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        val result = handler.handle(new CompleteOrderCommand(courier.getId(), order.getId()));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("courier.out.of.completion.zone");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.Assigned);
        assertThat(courier.getAssignments()).hasSize(1);
        verify(orderRepository, never()).update(any(Order.class));
        verify(courierRepository, never()).update(any(Courier.class));
    }

    /* ---------- happy path ---------- */

    @Test
    void testSuccessCompletesOrder() {
        val courier = newCourier(5, 5);
        val order = newAssignedOrder(6, 5);
        courier.assignOrder(order);
        when(courierRepository.findById(courier.getId())).thenReturn(Optional.of(courier));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenReturn(order);

        val result = handler.handle(new CompleteOrderCommand(courier.getId(), order.getId()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getStatus()).isEqualTo(OrderStatus.Completed);
        assertThat(courier.getAssignments()).isEmpty();
        verify(courierRepository).update(courier);
        verify(orderRepository).update(order);
        verify(domainEventPublisher).publish(List.of(order));
    }
}
