package microarch.delivery.core.application.command;

import microarch.delivery.core.domain.model.Volume;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 *
 * @param orderId
 * @param country
 * @param city
 * @param street
 * @param house
 * @param apartment
 *            may be NULL or blank
 * @param volume
 *            must not be null; create via {@link Volume#create(int)} — the value object validates the value itself
 */
public record CreateOrderCommand(UUID orderId, String country, String city, String street, String house,
        String apartment, Volume volume) {
    public CreateOrderCommand {
        if (orderId == null) {
            throw new CommandCreateValidationException("orderId is must not be null");
        }
        if (!StringUtils.hasText(country)) {
            throw new CommandCreateValidationException("country must not be empty");
        }
        if (!StringUtils.hasText(city)) {
            throw new CommandCreateValidationException("city must not be empty");
        }
        if (!StringUtils.hasText(street)) {
            throw new CommandCreateValidationException("street must not be empty");
        }
        if (!StringUtils.hasText(house)) {
            throw new CommandCreateValidationException("house must not be empty");
        }
        if (volume == null) {
            throw new CommandCreateValidationException("volume is must not be null");
        }
    }
}
