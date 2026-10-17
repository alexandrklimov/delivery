package microarch.delivery.core.application.query;

import java.util.UUID;

/**
 *
 * @param id
 * @param name
 * @param x
 * @param y
 */
public record CourierDTO(UUID id, String name, int x, int y) {
}
