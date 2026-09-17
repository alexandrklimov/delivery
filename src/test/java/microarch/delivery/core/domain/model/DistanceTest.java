package microarch.delivery.core.domain.model;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class DistanceTest {

    private final Random rnd = new Random();

    @Test
    void testEquals() {
        val value = rnd.nextInt(0, Integer.MAX_VALUE);
        val d1 = Distance.create(value).getValue();
        val d2 = Distance.create(value).getValue();

        assertThat(d1).isEqualTo(d2);
    }

    @Test
    void testInEquals() {
        val d1 = Distance.create(5).getValue();
        val d2 = Distance.create(6).getValue();

        assertThat(d1).isNotEqualTo(d2);
    }

    @Test
    void testCompareTo() {
        val d1 = Distance.create(5).getValue();
        val d2 = Distance.create(6).getValue();

        val res1 = d1.compareTo(d2);
        assertThat(res1).isLessThan(0);

        val res2 = d2.compareTo(d1);
        assertThat(res2).isGreaterThan(0);
    }

    @Test
    void testCompareToForEquals() {
        val value = rnd.nextInt(0, Integer.MAX_VALUE);
        val d1 = Distance.create(value).getValue();
        val d2 = Distance.create(value).getValue();

        val res1 = d1.compareTo(d2);
        assertThat(res1).isZero();
        val res2 = d2.compareTo(d1);
        assertThat(res2).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {-2, 0, 100500})
    void testValueValidation(final int value) {
        val result = Distance.create(value);

        if (value < 0) {
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().getCode()).isEqualTo("value.must.be.greater.than");
        } else {
            assertThat(result.isFailure()).isFalse();
            assertThat(result.getValue().getValue()).isEqualTo(value);
        }

    }

}