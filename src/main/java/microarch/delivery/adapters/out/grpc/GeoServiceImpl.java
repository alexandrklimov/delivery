package microarch.delivery.adapters.out.grpc;

import clients.geo.GeoGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import libs.errs.Error;
import libs.errs.Result;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import microarch.delivery.ApplicationProperties;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.ports.GeoService;
import org.springframework.stereotype.Service;

/**
 * gRPC adapter for the external Geo service: exchanges street addresses for geographic {@link Location}s via the
 * generated {@link GeoGrpc} blocking stub.
 */
@Service
@Slf4j
public class GeoServiceImpl implements GeoService {

    private final ManagedChannel channel;
    private final GeoGrpc.GeoBlockingStub blockingStub;

    /**
     * Builds the channel and the blocking stub from the {@code app.grpc.geo-service} configuration.
     *
     * @param applicationProperties
     *            application configuration carrying the Geo service host and port
     */
    public GeoServiceImpl(final ApplicationProperties applicationProperties) {
        val geoService = applicationProperties.getGrpc().getGeoService();
        this.channel = ManagedChannelBuilder.forAddress(geoService.getHost(), geoService.getPort()).usePlaintext()
                .build();
        this.blockingStub = GeoGrpc.newBlockingStub(channel);
    }

    /**
     * Resolves a street address into a geographic {@link Location} by calling the Geo service.
     * <p>
     * The routine follows these steps:
     * <ul>
     * <li>sends {@code GetGeolocationRequest} to the Geo service;</li>
     * <li>maps the returned coordinates into the domain {@link Location} (range-checked by
     * {@code Location#create});</li>
     * <li>translates transport failures into a use case {@link Error} instead of throwing.</li>
     * </ul>
     *
     * @param street
     *            street part of the delivery address
     *
     * @return success with the resolved {@link Location}, or failure when the Geo service is unreachable, responds with
     *         a gRPC error, omits the location, or returns out-of-range coordinates
     */
    @Override
    public Result<Location, Error> getLocation(final String street) {
        val request = clients.geo.GeoProto.GetGeolocationRequest.newBuilder().setStreet(street).build();

        final clients.geo.GeoProto.GetGeolocationReply reply;
        try {
            reply = blockingStub.getGeolocation(request);
        } catch (Exception ex) {
            log.error("Geo service call failed for street='{}'", street, ex);
            return Result.failure(Error.of(Constants.ERR_CODE_GEO_SERVICE_UNAVAILABLE, "Geo service is unavailable"));
        }

        if (!reply.hasLocation()) {
            log.error("Geo service returned no location for street='{}'", street);
            return Result
                    .failure(Error.of(Constants.ERR_CODE_GEO_SERVICE_UNAVAILABLE, "Geo service returned no location"));
        }

        val protoLocation = reply.getLocation();
        return Location.create(protoLocation.getX(), protoLocation.getY());
    }

    /**
     * Shuts down the gRPC channel on bean disposal.
     */
    @PreDestroy
    void shutdown() {
        channel.shutdown();
    }
}
