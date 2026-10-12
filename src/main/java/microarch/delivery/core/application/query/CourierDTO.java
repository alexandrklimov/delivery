package microarch.delivery.core.application.query;

import microarch.delivery.core.domain.model.Location;

import java.util.UUID;

/**
 *
 * @param id
 * @param name
 * @param location
 */
public record CourierDTO(UUID id, String name, Location location) {
}
