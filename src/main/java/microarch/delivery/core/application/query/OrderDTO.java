package microarch.delivery.core.application.query;

import microarch.delivery.core.domain.model.Location;

import java.util.UUID;

/**
 *
 * @param id
 * @param location
 */
public record OrderDTO(UUID id, Location location) {
}
