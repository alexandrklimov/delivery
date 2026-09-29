package microarch.delivery.core.domain.model.order;

import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Volume;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    private static Order createOrder() {
        return createOrder(OrderStatus.Created);
    }

    private static Order createOrder(final OrderStatus status) {
        val order = Order.create(UUID.randomUUID(), Location.create(5, 5).getValue(), Volume.create(10).getValue())
                .getValue();
        if (status != OrderStatus.Created)
            order.changeStatus(OrderStatus.Assigned);
        if (status == OrderStatus.Completed)
            order.changeStatus(OrderStatus.Completed);
        return order;
    }

    @Test
    void testCreateSuccess() {
        val id = UUID.randomUUID();
        val location = Location.create(5, 5).getValue();
        val volume = Volume.create(10).getValue();

        val result = Order.create(id, location, volume);

        assertThat(result.isSuccess()).isTrue();
        val order = result.getValue();
        assertThat(order.getId()).isEqualTo(id);
        assertThat(order.getLocation()).isEqualTo(location);
        assertThat(order.getVolume()).isEqualTo(volume);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.Created);
    }

    @ParameterizedTest
    @MethodSource
    void testCreateWithNullArgument(final UUID id, final Location location, final Volume volume) {
        val result = Order.create(id, location, volume);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_OBJ_IS_NULL);
    }

    static Stream<Arguments> testCreateWithNullArgument() {
        return Stream.of(Arguments.of(null, Location.create(5, 5).getValue(), Volume.create(10).getValue()),
                Arguments.of(UUID.randomUUID(), null, Volume.create(10).getValue()),
                Arguments.of(UUID.randomUUID(), Location.create(5, 5).getValue(), null));
    }

    @Test
    void testChangeStatusByHappyPath() {
        val order = createOrder();

        val assignResult = order.changeStatus(OrderStatus.Assigned);
        assertThat(assignResult.isSuccess()).isTrue();
        assertThat(assignResult.getValue()).isSameAs(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.Assigned);

        val completeResult = order.changeStatus(OrderStatus.Completed);
        assertThat(completeResult.isSuccess()).isTrue();
        assertThat(completeResult.getValue()).isSameAs(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.Completed);
    }

    @ParameterizedTest
    @MethodSource
    void testChangeStatusNotAllowed(final OrderStatus fromStatus, final OrderStatus targetStatus) {
        val order = createOrder(fromStatus);

        val result = order.changeStatus(targetStatus);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("order.transition.not.allowed");
        assertThat(order.getStatus()).isEqualTo(fromStatus);
    }

    static Stream<Arguments> testChangeStatusNotAllowed() {
        return Stream.of(Arguments.of(OrderStatus.Created, OrderStatus.Completed),
                Arguments.of(OrderStatus.Created, OrderStatus.Created),
                Arguments.of(OrderStatus.Assigned, OrderStatus.Assigned),
                Arguments.of(OrderStatus.Assigned, OrderStatus.Created),
                Arguments.of(OrderStatus.Completed, OrderStatus.Created),
                Arguments.of(OrderStatus.Completed, OrderStatus.Assigned),
                Arguments.of(OrderStatus.Completed, OrderStatus.Completed));
    }

    @Test
    void testChangeStatusNullTarget() {
        val order = createOrder();

        val result = order.changeStatus(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_OBJ_IS_NULL);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.Created);
    }
}
