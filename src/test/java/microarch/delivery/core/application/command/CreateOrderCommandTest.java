package microarch.delivery.core.application.command;

import lombok.val;
import microarch.delivery.core.domain.model.Volume;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateOrderCommandTest {

    private static CreateOrderCommand command(final UUID orderId, final String country, final String city,
            final String street, final String house, final String apartment) {
        return new CreateOrderCommand(orderId, country, city, street, house, apartment, Volume.create(3).getValue());
    }

    @Test
    void testNullOrderIdRejected() {
        assertThatThrownBy(() -> command(null, "Russia", "Moscow", "Tverskaya", "1", "2"))
                .isInstanceOf(CommandCreateValidationException.class);
    }

    @Test
    void testBlankCountryRejected() {
        assertThatThrownBy(() -> command(UUID.randomUUID(), " ", "Moscow", "Tverskaya", "1", "2"))
                .isInstanceOf(CommandCreateValidationException.class);
    }

    @Test
    void testBlankCityRejected() {
        assertThatThrownBy(() -> command(UUID.randomUUID(), "Russia", "", "Tverskaya", "1", "2"))
                .isInstanceOf(CommandCreateValidationException.class);
    }

    @Test
    void testBlankStreetRejected() {
        assertThatThrownBy(() -> command(UUID.randomUUID(), "Russia", "Moscow", null, "1", "2"))
                .isInstanceOf(CommandCreateValidationException.class);
    }

    @Test
    void testBlankHouseRejected() {
        assertThatThrownBy(() -> command(UUID.randomUUID(), "Russia", "Moscow", "Tverskaya", " ", "2"))
                .isInstanceOf(CommandCreateValidationException.class);
    }

    @Test
    void testBlankApartmentAllowed() {
        val command = command(UUID.randomUUID(), "Russia", "Moscow", "Tverskaya", "1", null);

        assertThat(command.apartment()).isNull();
    }

    @Test
    void testNullVolumeRejected() {
        assertThatThrownBy(
                () -> new CreateOrderCommand(UUID.randomUUID(), "Russia", "Moscow", "Tverskaya", "1", "2", null))
                        .isInstanceOf(CommandCreateValidationException.class);
    }

    @Test
    void testValidCommandAccepted() {
        val orderId = UUID.randomUUID();
        val volume = Volume.create(3).getValue();
        val command = new CreateOrderCommand(orderId, "Russia", "Moscow", "Tverskaya", "1", "2", volume);

        assertThat(command.orderId()).isEqualTo(orderId);
        assertThat(command.country()).isEqualTo("Russia");
        assertThat(command.volume()).isEqualTo(volume);
    }
}
