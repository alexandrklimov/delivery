package microarch.delivery.core.application.command;

import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompleteOrderCommandTest {

    @Test
    void testNullCourierIdRejected() {
        assertThatThrownBy(() -> new CompleteOrderCommand(null, UUID.randomUUID()))
                .isInstanceOf(CommandCreateValidationException.class);
    }

    @Test
    void testNullOrderIdRejected() {
        assertThatThrownBy(() -> new CompleteOrderCommand(UUID.randomUUID(), null))
                .isInstanceOf(CommandCreateValidationException.class);
    }

    @Test
    void testValidCommandAccepted() {
        val courierId = UUID.randomUUID();
        val orderId = UUID.randomUUID();

        val command = new CompleteOrderCommand(courierId, orderId);

        assertThat(command.courierId()).isEqualTo(courierId);
        assertThat(command.orderId()).isEqualTo(orderId);
    }
}
