package microarch.delivery.core.domain.model.courier;

import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Status;
import microarch.delivery.core.domain.model.Volume;
import microarch.delivery.core.domain.model.order.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourierTest {

    private static Courier createCourier(final int x, final int y) {
        return Courier.create("Ivan", Location.create(x, y).getValue()).getValue();
    }

    private static Order createOrder(final int volume, final int x, final int y) {
        return Order.create(UUID.randomUUID(), Location.create(x, y).getValue(), Volume.create(volume).getValue())
                .getValue();
    }

    /* ---------- create ---------- */

    @Test
    void testCreateSuccess() {
        val location = Location.create(5, 5).getValue();

        val result = Courier.create("Ivan", location);

        assertThat(result.isSuccess()).isTrue();
        val courier = result.getValue();
        assertThat(courier.getId()).isNotNull();
        assertThat(courier.getName()).isEqualTo("Ivan");
        assertThat(courier.getLocation()).isEqualTo(location);
        assertThat(courier.getMaxVolume().getValue()).isEqualTo(Courier.MAX_VOLUME);
        assertThat(courier.getAssignments()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   " })
    @NullSource
    void testCreateWithBlankName(final String name) {
        val result = Courier.create(name, Location.create(5, 5).getValue());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("courier.name.is.blank");
    }

    @Test
    void testCreateWithNullLocation() {
        val result = Courier.create("Ivan", null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_OBJ_IS_NULL);
    }

    /* ---------- checkCanAssignOneMore ---------- */

    @Test
    void testCheckCanAssignOneMoreNullVolume() {
        val courier = createCourier(5, 5);

        val result = courier.checkCanAssignOneMore(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_OBJ_IS_NULL);
    }

    @Test
    void testCheckCanAssignOneMoreExactMaxVolume() {
        val courier = createCourier(5, 5);

        val result = courier.checkCanAssignOneMore(Volume.create(Courier.MAX_VOLUME).getValue());

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void testCheckCanAssignOneMoreOverMaxVolume() {
        val courier = createCourier(5, 5);

        val result = courier.checkCanAssignOneMore(Volume.create(Courier.MAX_VOLUME + 1).getValue());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("courier.maxvol.restriction");
    }

    @Test
    void testCheckCanAssignOneMorePartiallyLoadedCourier() {
        val courier = createCourier(5, 5);
        courier.assignOrder(createOrder(10, 5, 5));

        val okResult = courier.checkCanAssignOneMore(Volume.create(10).getValue());
        val failResult = courier.checkCanAssignOneMore(Volume.create(11).getValue());

        assertThat(okResult.isSuccess()).isTrue();
        assertThat(failResult.isFailure()).isTrue();
        assertThat(failResult.getError().getCode()).isEqualTo("courier.maxvol.restriction");
    }

    /* ---------- assignOrder ---------- */

    @Test
    void testAssignOrderNull() {
        val courier = createCourier(5, 5);

        val result = courier.assignOrder(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_OBJ_IS_NULL);
        assertThat(courier.getAssignments()).isEmpty();
    }

    @Test
    void testAssignOrderSuccess() {
        val courier = createCourier(5, 5);
        val order = createOrder(5, 1, 1);

        val result = courier.assignOrder(order);

        assertThat(result.isSuccess()).isTrue();
        val assignments = courier.getAssignments();
        assertThat(assignments).hasSize(1);
        val assignment = assignments.iterator().next();
        assertThat(assignment.getOrderId()).isEqualTo(order.getId());
        assertThat(assignment.getVolume()).isEqualTo(order.getVolume());
        assertThat(assignment.getLocation()).isEqualTo(order.getLocation());
        assertThat(assignment.getStatus()).isEqualTo(Status.Assigned);
    }

    @Test
    void testAssignSameOrderTwice() {
        val courier = createCourier(5, 5);
        val order = createOrder(5, 1, 1);
        courier.assignOrder(order);

        val result = courier.assignOrder(order);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("courier.already.has.order");
        assertThat(courier.getAssignments()).hasSize(1);
    }

    @Test
    void testAssignOrderVolumeOverflow() {
        val courier = createCourier(5, 5);
        courier.assignOrder(createOrder(15, 1, 1));

        val overflowResult = courier.assignOrder(createOrder(6, 1, 2));

        assertThat(overflowResult.isFailure()).isTrue();
        assertThat(overflowResult.getError().getCode()).isEqualTo("courier.maxvol.restriction");
        assertThat(courier.getAssignments()).hasSize(1);

        val exactResult = courier.assignOrder(createOrder(5, 1, 3));

        assertThat(exactResult.isSuccess()).isTrue();
        assertThat(courier.getAssignments()).hasSize(2);
    }

    /* ---------- getAssignments ---------- */

    @Test
    void testGetAssignmentsIsUnmodifiable() {
        val courier = createCourier(5, 5);
        courier.assignOrder(createOrder(5, 1, 1));
        val assignments = courier.getAssignments();

        val assignment = assignments.iterator().next();
        assertThatThrownBy(() -> assignments.add(assignment)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> assignments.remove(assignment)).isInstanceOf(UnsupportedOperationException.class);
    }

    /* ---------- completeAssignment ---------- */

    @Test
    void testCompleteAssignmentNullOrderId() {
        val courier = createCourier(5, 5);
        courier.assignOrder(createOrder(5, 5, 5));

        val result = courier.completeAssignment(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_OBJ_IS_NULL);
    }

    @Test
    void testCompleteAssignmentUnknownOrderId() {
        val courier = createCourier(5, 5);
        courier.assignOrder(createOrder(5, 5, 5));

        val result = courier.completeAssignment(UUID.randomUUID());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("courier.order.not.found");
        assertThat(courier.getAssignments()).hasSize(1);
    }

    @ParameterizedTest
    @MethodSource
    void testCompleteAssignmentInZone(final int orderX, final int orderY) {
        val courier = createCourier(5, 5);
        val order = createOrder(5, orderX, orderY);
        courier.assignOrder(order);

        val result = courier.completeAssignment(order.getId());

        assertThat(result.isSuccess()).isTrue();
        assertThat(courier.getAssignments()).isEmpty();
    }

    static Stream<Arguments> testCompleteAssignmentInZone() {
        return Stream.of(Arguments.of(5, 5), Arguments.of(5, 6), Arguments.of(5, 4), Arguments.of(6, 5),
                Arguments.of(4, 5));
    }

    @Test
    void testCompleteAssignmentOutOfZone() {
        val courier = createCourier(5, 5);
        val order = createOrder(5, 5, 7);
        courier.assignOrder(order);

        val result = courier.completeAssignment(order.getId());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("courier.out.of.completion.zone");
        val assignment = courier.getAssignments().iterator().next();
        assertThat(assignment.getStatus()).isEqualTo(Status.Assigned);
    }

    @Test
    void testCompleteAssignmentTwiceFailsWithOrderNotFound() {
        val courier = createCourier(5, 5);
        val order = createOrder(5, 5, 5);
        courier.assignOrder(order);
        courier.completeAssignment(order.getId());

        val result = courier.completeAssignment(order.getId());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("courier.order.not.found");
    }

    @Test
    void testCompletedAssignmentFreesVolume() {
        val courier = createCourier(5, 5);
        val order = createOrder(Courier.MAX_VOLUME, 5, 5);
        courier.assignOrder(order);
        courier.completeAssignment(order.getId());

        val canTakeResult = courier.checkCanAssignOneMore(Volume.create(1).getValue());
        val assignResult = courier.assignOrder(createOrder(Courier.MAX_VOLUME, 5, 4));

        assertThat(canTakeResult.isSuccess()).isTrue();
        assertThat(assignResult.isSuccess()).isTrue();
        assertThat(courier.getAssignments()).hasSize(1);
    }

    /* ---------- moveTo ---------- */

    /**
     * Verifies that the courier can be moved to any valid field location in a single call — the target is reported by
     * the front end, so there is no step-by-step walking and no one-step restriction.
     */
    @ParameterizedTest
    @MethodSource
    void testMoveToTargetLocation(final int fromX, final int fromY, final int toX, final int toY) {
        val courier = createCourier(fromX, fromY);

        val result = courier.moveTo(Location.create(toX, toY).getValue());

        assertThat(result.isSuccess()).isTrue();
        assertThat(courier.getLocation()).isEqualTo(Location.create(toX, toY).getValue());
    }

    static Stream<Arguments> testMoveToTargetLocation() {
        // one step in each direction, multi-step, L-path, corner to corner
        return Stream.of(Arguments.of(5, 5, 6, 5), Arguments.of(5, 5, 4, 5), Arguments.of(5, 5, 5, 6),
                Arguments.of(5, 5, 5, 4), Arguments.of(5, 5, 8, 5), Arguments.of(2, 2, 6, 9),
                Arguments.of(1, 1, 10, 10));
    }

    /**
     * Verifies that moving to the current location succeeds and changes nothing.
     */
    @Test
    void testMoveToCurrentLocationSucceeds() {
        val courier = createCourier(5, 5);

        val result = courier.moveTo(Location.create(5, 5).getValue());

        assertThat(result.isSuccess()).isTrue();
        assertThat(courier.getLocation()).isEqualTo(Location.create(5, 5).getValue());
    }

    /**
     * Verifies that a null target location is rejected and the courier stays where it was.
     */
    @Test
    void testMoveToNullFails() {
        val courier = createCourier(5, 5);

        val result = courier.moveTo(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_OBJ_IS_NULL);
        assertThat(courier.getLocation()).isEqualTo(Location.create(5, 5).getValue());
    }
}
