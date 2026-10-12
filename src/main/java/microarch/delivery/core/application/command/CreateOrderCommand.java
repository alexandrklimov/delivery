package microarch.delivery.core.application.command;

import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 *
 * @param orderId
 * @param country
 * @param city
 * @param street
 * @param house
 * @param apartment may be NULL or blank
 * @param volume
 */
public record CreateOrderCommand (
    UUID orderId, 
    String country,
    String city,
    String street,
    String house,
    String apartment,
    int volume
) {
    public CreateOrderCommand {
        if(orderId == null){
            throw new CommandCreateValidationException("orderId is must not be null");
        }
        if(!StringUtils.hasText(country)) {
            throw new CommandCreateValidationException("country must not be empty");
        }
        if(!StringUtils.hasText(city)) {
            throw new CommandCreateValidationException("city must not be empty");
        }
        if(!StringUtils.hasText(street)) {
            throw new CommandCreateValidationException("street must not be empty");
        }
        if(!StringUtils.hasText(house)) {
            throw new CommandCreateValidationException("house must not be empty");
        }
    }
}
