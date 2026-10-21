package microarch.delivery.core.application.command;

import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoveCourierCommandTest {

    @Test
    void testNullCourierIdRejected() {
        assertThatThrownBy(() -> new MoveCourierCommand(null, 5, 5))
                .isInstanceOf(CommandCreateValidationException.class);
    }

    @Test
    void testValidCommandAccepted() {
        val courierId = UUID.randomUUID();

        val command = new MoveCourierCommand(courierId, 5, 5);

        assertThat(command.courierId()).isEqualTo(courierId);
        assertThat(command.x()).isEqualTo(5);
        assertThat(command.y()).isEqualTo(5);
    }
}
