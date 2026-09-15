package microarch.delivery.core.domain.model;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LocationTest {

    @ParameterizedTest
    @MethodSource
    void testValidateX(final int x) {
        val y = Location.MIN_Y;

        val result = Location.create(x, y);

        if (x < Location.MIN_X || x > Location.MAX_X) {
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().getCode()).isEqualTo("value.is.out.of.range");
        } else {
            assertThat(result.isFailure()).isFalse();
            assertThat(result.getValue().getX()).isEqualTo(x);
            assertThat(result.getValue().getY()).isEqualTo(y);
        }
    }

    static Stream<Arguments> testValidateX() {
        return Stream.of(
            Arguments.of(Location.MIN_X),
            Arguments.of(Location.MAX_X),
            Arguments.of((Location.MAX_X - Location.MIN_X) / 2),
            Arguments.of(Location.MIN_X - 1),
            Arguments.of(Location.MAX_X + 1)
        );
    }

    @Test
    void testComputeDistance() {
        val from = Location.create(2, 6).getValue();
        val to = Location.create(4, 9).getValue();
        val expectedDistance = 5;

        val result = Location.computeDistance(from, to);
        val resultReverse = Location.computeDistance(to, from);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getValue()).isEqualTo(expectedDistance);
        assertThat(resultReverse.isSuccess()).isTrue();
        assertThat(resultReverse.getValue().getValue()).isEqualTo(expectedDistance);
    }
}