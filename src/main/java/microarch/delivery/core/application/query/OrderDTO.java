package microarch.delivery.core.application.query;

import java.util.UUID;

/**
 *
 * @param id
 * @param x
 * @param y
 */
public record OrderDTO(UUID id, int x, int y) {
}
