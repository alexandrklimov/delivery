package microarch.delivery.core.domain.model;

import libs.errs.Error;
import libs.errs.Result;
import lombok.val;
import microarch.delivery.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AssignmentTest {

    private static Result<Assignment, Error> createAssignment() {
        return Assignment.create(UUID.randomUUID(), Volume.create(10).getValue(), Location.create(5, 5).getValue());
    }

    @Test
    void testCreateSuccess() {
        val orderId = UUID.randomUUID();
        val volume = Volume.create(10).getValue();
        val location = Location.create(5, 5).getValue();

        val result = Assignment.create(orderId, volume, location);

        assertThat(result.isSuccess()).isTrue();
        val assignment = result.getValue();
        assertThat(assignment.getId()).isNotNull();
        assertThat(assignment.getOrderId()).isEqualTo(orderId);
        assertThat(assignment.getVolume()).isEqualTo(volume);
        assertThat(assignment.getLocation()).isEqualTo(location);
        assertThat(assignment.getStatus()).isEqualTo(Status.Assigned);
    }

    @ParameterizedTest
    @MethodSource
    void testCreateWithNullArgument(final UUID orderId, final Volume volume, final Location location) {
        val result = Assignment.create(orderId, volume, location);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_OBJ_IS_NULL);
    }

    static Stream<Arguments> testCreateWithNullArgument() {
        return Stream.of(Arguments.of(null, Volume.create(10).getValue(), Location.create(5, 5).getValue()),
                Arguments.of(UUID.randomUUID(), null, Location.create(5, 5).getValue()),
                Arguments.of(UUID.randomUUID(), Volume.create(10).getValue(), null));
    }

    @Test
    void testCompleteNullCourierLocation() {
        val assignment = createAssignment().getValue();

        val result = assignment.complete(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_OBJ_IS_NULL);
        assertThat(assignment.getStatus()).isEqualTo(Status.Assigned);
    }

    @Test
    void testCompleteSameCell() {
        val assignment = createAssignment().getValue();

        val result = assignment.complete(Location.create(5, 5).getValue());

        assertThat(result.isSuccess()).isTrue();
        assertThat(assignment.getStatus()).isEqualTo(Status.Completed);
    }

    @Test
    void testCompleteAdjacentCell() {
        val assignment = createAssignment().getValue();

        val result = assignment.complete(Location.create(5, 6).getValue());

        assertThat(result.isSuccess()).isTrue();
        assertThat(assignment.getStatus()).isEqualTo(Status.Completed);
    }

    @Test
    void testCompleteOutOfZone() {
        val assignment = createAssignment().getValue();

        val result = assignment.complete(Location.create(5, 7).getValue());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("out.of.completion.zone");
        assertThat(assignment.getStatus()).isEqualTo(Status.Assigned);
    }

    @Test
    void testCompleteAlreadyCompleted() {
        val assignment = createAssignment().getValue();
        assignment.complete(Location.create(5, 5).getValue());

        val result = assignment.complete(Location.create(5, 5).getValue());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("assignment.wrong.transition");
        assertThat(assignment.getStatus()).isEqualTo(Status.Completed);
    }
}
