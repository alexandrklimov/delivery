package microarch.delivery.core.application.command;

import libs.ddd.DomainEventPublisher;
import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Volume;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.domain.model.order.OrderStatus;
import microarch.delivery.core.domain.service.OrderAssignServiceImpl;
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
class AssignOrderHandlerImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CourierRepository courierRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private AssignOrderHandlerImpl handler;

    @BeforeEach
    void setUp() {
        handler = new AssignOrderHandlerImpl(orderRepository, courierRepository, new OrderAssignServiceImpl(),
                domainEventPublisher);
    }

    private static Order newOrder() {
        return Order.create(UUID.randomUUID(), Location.create(5, 5).getValue(), Volume.create(3).getValue())
                .getValue();
    }

    private static Courier newCourier(final String name, final int x, final int y) {
        return Courier.create(name, Location.create(x, y).getValue()).getValue();
    }

    /* ---------- argument validation ---------- */

    @Test
    void testNullCommandFails() {
        val result = handler.handle(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_OBJ_IS_NULL);
        verify(orderRepository, never()).update(any(Order.class));
        verify(courierRepository, never()).update(any(Courier.class));
        verifyNoInteractions(domainEventPublisher);
    }

    /* ---------- no order to assign ---------- */

    @Test
    void testNoCreatedOrderFails() {
        when(orderRepository.getAnyInCreatedStatus()).thenReturn(Optional.empty());

        val result = handler.handle(new AssignOrderCommand());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("order.not.found");
        verify(courierRepository, never()).update(any(Courier.class));
        verify(orderRepository, never()).update(any(Order.class));
    }

    /* ---------- no courier available ---------- */

    @Test
    void testNoAvailableCourierFails() {
        val order = newOrder();
        when(orderRepository.getAnyInCreatedStatus()).thenReturn(Optional.of(order));
        when(courierRepository.getAll()).thenReturn(List.of());

        val result = handler.handle(new AssignOrderCommand());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("assignsrvc.no.available.courier");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.Created);
        verify(orderRepository, never()).update(any(Order.class));
        verify(courierRepository, never()).update(any(Courier.class));
    }

    /* ---------- happy path ---------- */

    @Test
    void testSuccessAssignsNearestCourier() {
        val order = newOrder();
        val nearest = newCourier("Near", 5, 6);
        val farthest = newCourier("Far", 9, 9);
        when(orderRepository.getAnyInCreatedStatus()).thenReturn(Optional.of(order));
        when(courierRepository.getAll()).thenReturn(List.of(farthest, nearest));
        when(courierRepository.update(any(Courier.class))).thenReturn(nearest);

        val result = handler.handle(new AssignOrderCommand());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getId()).isEqualTo(nearest.getId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.Assigned);
        assertThat(nearest.getAssignments()).hasSize(1);
        assertThat(farthest.getAssignments()).isEmpty();

        verify(orderRepository).update(order);
        verify(courierRepository).update(nearest);
        verify(courierRepository, never()).update(farthest);
        verify(domainEventPublisher).publish(List.of(order));
    }
}
