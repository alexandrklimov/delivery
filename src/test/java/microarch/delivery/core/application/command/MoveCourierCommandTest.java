package microarch.delivery.core.application.command;

import lombok.val;
import microarch.delivery.core.domain.model.Location;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoveCourierCommandTest {

    @Test
    void testNullCourierIdRejected() {
        assertThatThrownBy(() -> new MoveCourierCommand(null, Location.create(5, 5).getValueOrThrow()))
                .isInstanceOf(CommandCreateValidationException.class);
    }

    @Test
    void testNullLocationRejected() {
        assertThatThrownBy(() -> new MoveCourierCommand(UUID.randomUUID(), null))
                .isInstanceOf(CommandCreateValidationException.class);
    }

    @Test
    void testValidCommandAccepted() {
        val courierId = UUID.randomUUID();
        val location = Location.create(5, 5).getValueOrThrow();

        val command = new MoveCourierCommand(courierId, location);

        assertThat(command.courierId()).isEqualTo(courierId);
        assertThat(command.location()).isEqualTo(location);
    }
}
