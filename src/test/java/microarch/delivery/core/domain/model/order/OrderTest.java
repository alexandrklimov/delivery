package microarch.delivery.core.domain.model.order;

import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Volume;
import microarch.delivery.core.domain.model.order.events.OrderAssignedDomainEvent;
import microarch.delivery.core.domain.model.order.events.OrderCompletedDomainEvent;
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
            order.markAsAssigned();
        if (status == OrderStatus.Completed)
            order.markAsCompleted();
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
    void testMarkAsAssignedSuccess() {
        val order = createOrder();

        val assignResult = order.markAsAssigned();

        assertThat(assignResult.isSuccess()).isTrue();
        assertThat(assignResult.getValue()).isSameAs(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.Assigned);
    }

    @Test
    void testMarkAsCompletedSuccess() {
        val order = createOrder(OrderStatus.Assigned);

        val completeResult = order.markAsCompleted();

        assertThat(completeResult.isSuccess()).isTrue();
        assertThat(completeResult.getValue()).isSameAs(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.Completed);
    }

    @ParameterizedTest
    @MethodSource
    void testMarkAsAssignedNotAllowed(final OrderStatus fromStatus) {
        val order = createOrder(fromStatus);

        val result = order.markAsAssigned();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("order.transition.not.allowed");
        assertThat(order.getStatus()).isEqualTo(fromStatus);
    }

    static Stream<Arguments> testMarkAsAssignedNotAllowed() {
        return Stream.of(Arguments.of(OrderStatus.Assigned), Arguments.of(OrderStatus.Completed));
    }

    @ParameterizedTest
    @MethodSource
    void testMarkAsCompletedNotAllowed(final OrderStatus fromStatus) {
        val order = createOrder(fromStatus);

        val result = order.markAsCompleted();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("order.transition.not.allowed");
        assertThat(order.getStatus()).isEqualTo(fromStatus);
    }

    static Stream<Arguments> testMarkAsCompletedNotAllowed() {
        return Stream.of(Arguments.of(OrderStatus.Created), Arguments.of(OrderStatus.Completed));
    }

    /* ---------- domain events ---------- */

    /**
     * Verifies that a successful transition into {@code Assigned} raises exactly one {@code OrderAssignedDomainEvent}
     * carrying the order id.
     */
    @Test
    void testMarkAsAssignedRaisesDomainEvent() {
        val order = createOrder();

        order.markAsAssigned();

        assertThat(order.getDomainEvents()).hasSize(1);
        val event = (OrderAssignedDomainEvent) order.getDomainEvents().getFirst();
        assertThat(event.getOrderId()).isEqualTo(order.getId());
    }

    /**
     * Verifies that a successful transition into {@code Completed} raises exactly one {@code OrderCompletedDomainEvent}
     * carrying the order id.
     */
    @Test
    void testMarkAsCompletedRaisesDomainEvent() {
        val order = createOrder(OrderStatus.Assigned);
        order.clearDomainEvents();

        order.markAsCompleted();

        assertThat(order.getDomainEvents()).hasSize(1);
        val event = (OrderCompletedDomainEvent) order.getDomainEvents().getFirst();
        assertThat(event.getOrderId()).isEqualTo(order.getId());
    }

    /**
     * Verifies that a rejected transition does not raise any domain event.
     */
    @Test
    void testNotAllowedTransitionRaisesNoDomainEvent() {
        val order = createOrder(OrderStatus.Assigned);
        order.clearDomainEvents();

        val result = order.markAsAssigned();

        assertThat(result.isFailure()).isTrue();
        assertThat(order.getDomainEvents()).isEmpty();
    }
}
