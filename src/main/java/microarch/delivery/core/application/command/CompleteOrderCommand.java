package microarch.delivery.core.application.command;

import java.util.UUID;

/**
 *
 * @param courierId
 * @param orderId
 */
public record CompleteOrderCommand(UUID courierId, UUID orderId) {
    public CompleteOrderCommand {
        if (courierId == null) {
            throw new CommandCreateValidationException("courierId is must not be null");
        }
        if (orderId == null) {
            throw new CommandCreateValidationException("orderId is must not be null");
        }
    }
}
