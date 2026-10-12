package microarch.delivery.core.application.command;

import microarch.delivery.core.domain.model.Location;

import java.util.UUID;

/**
 *
 * @param courierId
 * @param location
 */
public record MoveCourierCommand(UUID courierId, Location location) {
    public MoveCourierCommand {
        if (courierId == null) {
            throw new CommandCreateValidationException("courierId is must not be null");
        }
        if (location == null) {
            throw new CommandCreateValidationException("location is must not be null");
        }
    }
}
