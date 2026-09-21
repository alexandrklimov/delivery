package microarch.delivery.core.domain.model;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class VolumeTest {

    @ParameterizedTest
    @ValueSource(ints = { -1, 0, 1, 100500 })
    void testValueValidation(final int value) {
        val result = Volume.create(value);

        if (value <= 0) {
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().getCode()).isEqualTo("value.must.be.greater.or.equal");
        } else {
            assertThat(result.isFailure()).isFalse();
            assertThat(result.getValue().getValue()).isEqualTo(value);
        }
    }

    @Test
    void testEquals() {
        val v1 = Volume.create(5).getValue();
        val v2 = Volume.create(5).getValue();

        assertThat(v1).isEqualTo(v2);
    }

    @Test
    void testNotEquals() {
        val v1 = Volume.create(5).getValue();
        val v2 = Volume.create(6).getValue();

        assertThat(v1).isNotEqualTo(v2);
    }

    @Test
    void testCompareTo() {
        val v1 = Volume.create(5).getValue();
        val v2 = Volume.create(6).getValue();

        val res1 = v1.compareTo(v2);
        assertThat(res1).isLessThan(0);

        val res2 = v2.compareTo(v1);
        assertThat(res2).isGreaterThan(0);
    }
}
