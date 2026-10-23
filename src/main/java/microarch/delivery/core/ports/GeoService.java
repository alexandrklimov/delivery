package microarch.delivery.core.ports;

import libs.errs.Error;
import libs.errs.Result;
import microarch.delivery.core.domain.model.Location;

/**
 * Output port for the external Geo service: resolves a delivery address into a geographic {@link Location} used for
 * courier dispatching.
 */
public interface GeoService {

    /**
     * Resolves the given street address into a geographic {@link Location}.
     *
     * @param street
     *            street part of the delivery address; the Geo contract accepts a single street string
     *
     * @return success with the resolved {@link Location}, or failure if the Geo service is unreachable, responds with
     *         an error, or returns coordinates outside the allowed field
     */
    Result<Location, Error> getLocation(final String street);
}
