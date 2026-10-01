package microarch.delivery.core.domain.service;

import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Volume;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.domain.model.order.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class OrderAssignServiceImplTest {

    private final OrderAssignService service = new OrderAssignServiceImpl();

    private static Courier createCourier(final int x, final int y) {
        return Courier.create("Ivan", Location.create(x, y).getValue()).getValue();
    }

    private static Order createOrder(final int volume, final int x, final int y) {
        return Order.create(UUID.randomUUID(), Location.create(x, y).getValue(), Volume.create(volume).getValue())
                .getValue();
    }

    /* ---------- argument validation ---------- */

    @Test
    void testAssignNullOrder() {
        val result = service.assign(null, List.of(createCourier(5, 5)));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_OBJ_IS_NULL);
    }

    @Test
    void testAssignNullCouriers() {
        val result = service.assign(createOrder(5, 5, 5), null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_OBJ_IS_NULL);
    }

    /* ---------- order status guard ---------- */

    @Test
    void testAssignCreatedOrderSucceeds() {
        val order = createOrder(5, 5, 5);

        val result = service.assign(order, List.of(createCourier(5, 6)));

        assertThat(result.isSuccess()).isTrue();
    }

    @ParameterizedTest
    @MethodSource
    void testAssignNonCreatedOrderFails(final OrderStatus status) {
        val order = createOrder(5, 5, 5);
        order.markAsAssigned();
        if (status == OrderStatus.Completed) {
            order.markAsCompleted();
        }

        val result = service.assign(order, List.of(createCourier(5, 6)));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("assignsrvc.order.wrong.status");
    }

    static Stream<Arguments> testAssignNonCreatedOrderFails() {
        return Stream.of(Arguments.of(OrderStatus.Assigned), Arguments.of(OrderStatus.Completed));
    }

    /* ---------- no courier available ---------- */

    @Test
    void testAssignWithEmptyCouriersFails() {
        val result = service.assign(createOrder(5, 5, 5), List.of());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("assignsrvc.no.available.courier");
    }

    @Test
    void testAssignWhenAllCouriersOverloadedFails() {
        val courier1 = createCourier(1, 1);
        val courier2 = createCourier(9, 9);
        courier1.assignOrder(createOrder(20, 5, 5));
        courier2.assignOrder(createOrder(20, 5, 5));
        val order = createOrder(1, 5, 5);

        val result = service.assign(order, List.of(courier1, courier2));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("assignsrvc.no.available.courier");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.Created);
    }

    /* ---------- nearest courier selection ---------- */

    @Test
    void testNearestCourierWins() {
        val far = createCourier(10, 10);
        val near = createCourier(5, 6);
        val middle = createCourier(7, 7);
        val order = createOrder(5, 5, 5);

        val result = service.assign(order, List.of(far, middle, near));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getId()).isEqualTo(near.getId());
    }

    @Test
    void testOverloadedCloserCourierIsSkipped() {
        val closerButOverloaded = createCourier(5, 6);
        closerButOverloaded.assignOrder(createOrder(20, 9, 9));
        val fartherAvailable = createCourier(8, 8);
        val order = createOrder(1, 5, 5);

        val result = service.assign(order, List.of(fartherAvailable, closerButOverloaded));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getId()).isEqualTo(fartherAvailable.getId());
    }

    @Test
    void testManhattanDistanceUsedForSelection() {
        val diagonal = createCourier(7, 7);
        val manhattanCloser = createCourier(3, 5);
        val order = createOrder(1, 5, 5);

        val result = service.assign(order, List.of(diagonal, manhattanCloser));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getId()).isEqualTo(manhattanCloser.getId());
    }

    @Test
    void testTieDistanceReturnsAnyCourierWithoutError() {
        val first = createCourier(4, 5);
        val second = createCourier(6, 5);
        val order = createOrder(1, 5, 5);

        val result = service.assign(order, List.of(first, second));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getId()).isIn(first.getId(), second.getId());
    }

    @Test
    void testPartiallyLoadedCourierStillEligible() {
        val courier = createCourier(5, 5);
        courier.assignOrder(createOrder(15, 1, 1));
        val order = createOrder(5, 5, 5);

        val result = service.assign(order, List.of(courier));

        assertThat(result.isSuccess()).isTrue();
        assertThat(courier.getAssignments()).hasSize(2);
    }

    /* ---------- consistent state after assignment ---------- */

    @Test
    void testSuccessCreatesAssignmentOnCourier() {
        val courier = createCourier(5, 5);
        val order = createOrder(7, 3, 4);

        val result = service.assign(order, List.of(courier));

        assertThat(result.isSuccess()).isTrue();
        val assignments = courier.getAssignments();
        assertThat(assignments).hasSize(1);
        val assignment = assignments.iterator().next();
        assertThat(assignment.getOrderId()).isEqualTo(order.getId());
        assertThat(assignment.getVolume()).isEqualTo(order.getVolume());
        assertThat(assignment.getLocation()).isEqualTo(order.getLocation());
    }

    @Test
    void testSuccessTransitionsOrderToAssigned() {
        val order = createOrder(5, 5, 5);

        val result = service.assign(order, List.of(createCourier(1, 1)));

        assertThat(result.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.Assigned);
    }

    @Test
    void testWinnerInternalFailurePropagatesAndOrderStaysCreated() {
        val courier = createCourier(5, 5);
        val order = createOrder(5, 5, 5);
        courier.assignOrder(order);

        val result = service.assign(order, List.of(courier));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("courier.already.has.order");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.Created);
    }
}
