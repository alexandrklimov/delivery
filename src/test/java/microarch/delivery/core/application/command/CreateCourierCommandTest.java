package microarch.delivery.core.application.command;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateCourierCommandTest {

    @Test
    void testBlankNameRejected() {
        assertThatThrownBy(() -> new CreateCourierCommand(" ")).isInstanceOf(CommandCreateValidationException.class);
    }

    @Test
    void testNullNameRejected() {
        assertThatThrownBy(() -> new CreateCourierCommand(null)).isInstanceOf(CommandCreateValidationException.class);
    }

    @Test
    void testValidNameAccepted() {
        assertThat(new CreateCourierCommand("Ivan").name()).isEqualTo("Ivan");
    }
}
