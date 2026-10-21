package microarch.delivery.core.application.command;

import java.util.UUID;

/**
 *
 * @param courierId
 * @param x
 *            horizontal coordinate of the target location
 * @param y
 *            vertical coordinate of the target location; the {@code Location} value object is created and validated by
 *            the use case handler, so out-of-range coordinates are reported as a use case failure, not a command
 *            rejection
 */
public record MoveCourierCommand(UUID courierId, int x, int y) {
    public MoveCourierCommand {
        if (courierId == null) {
            throw new CommandCreateValidationException("courierId is must not be null");
        }
    }
}
