package microarch.delivery.core.application.query;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetNotCompletedOrdersHandlerImplTest {

    @Mock
    private OrderRepository orderRepository;

    private GetNotCompletedOrdersHandlerImpl handler;

    @BeforeEach
    void setUp() {
        handler = new GetNotCompletedOrdersHandlerImpl(orderRepository);
    }

    private static Order newOrder(final OrderStatus status, final int x, final int y) {
        val order = Order.create(UUID.randomUUID(), Location.create(x, y).getValue(), Volume.create(3).getValue())
                .getValue();
        if (status == OrderStatus.Assigned || status == OrderStatus.Completed)
            order.markAsAssigned();
        if (status == OrderStatus.Completed)
            order.markAsCompleted();
        return order;
    }

    /* ---------- argument validation ---------- */

    @Test
    void testNullQueryFails() {
        val result = handler.handle(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_OBJ_IS_NULL);
        verifyNoInteractions(orderRepository);
    }

    /* ---------- happy path ---------- */

    @Test
    void testEmptyListSuccess() {
        when(orderRepository.getAllByStatuses(List.of(OrderStatus.Created, OrderStatus.Assigned)))
                .thenReturn(List.of());

        val result = handler.handle(new GetNotCompletedOrdersQuery());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEmpty();
    }

    @Test
    void testMapsNotCompletedOrdersToDto() {
        val created = newOrder(OrderStatus.Created, 1, 2);
        val assigned = newOrder(OrderStatus.Assigned, 3, 4);
        when(orderRepository.getAllByStatuses(List.of(OrderStatus.Created, OrderStatus.Assigned)))
                .thenReturn(List.of(created, assigned));

        val result = handler.handle(new GetNotCompletedOrdersQuery());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).containsExactly(new OrderDTO(created.getId(), Location.create(1, 2).getValue()),
                new OrderDTO(assigned.getId(), Location.create(3, 4).getValue()));
        verify(orderRepository).getAllByStatuses(List.of(OrderStatus.Created, OrderStatus.Assigned));
    }
}
