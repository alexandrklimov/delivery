package microarch.delivery.adapters.out.grpc;

import clients.geo.GeoGrpc;
import clients.geo.GeoProto;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.val;
import microarch.delivery.ApplicationProperties;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link GeoServiceImpl} against an in-memory gRPC server so that request mapping, reply mapping, and error
 * translation are verified without the real Geo service.
 */
class GeoServiceImplTest {

    private Server server;
    private GeoServiceImpl geoService;

    /** Reply coordinates the fake Geo service returns; {@code null} disables the location field. */
    private Integer replyX;
    private Integer replyY;

    /** gRPC error the fake Geo service responds with; {@code null} means a successful reply. */
    private StatusRuntimeException replyError;

    /**
     * Fake Geo service answering with the coordinates configured by the test.
     */
    private final GeoGrpc.GeoImplBase fakeGeo = new GeoGrpc.GeoImplBase() {
        @Override
        public void getGeolocation(final GeoProto.GetGeolocationRequest request,
                final StreamObserver<GeoProto.GetGeolocationReply> responseObserver) {
            if (replyError != null) {
                responseObserver.onError(replyError);
                return;
            }
            val replyBuilder = GeoProto.GetGeolocationReply.newBuilder();
            if (replyX != null && replyY != null) {
                replyBuilder.setLocation(GeoProto.Location.newBuilder().setX(replyX).setY(replyY));
            }
            responseObserver.onNext(replyBuilder.build());
            responseObserver.onCompleted();
        }
    };

    @BeforeEach
    void setUp() throws IOException {
        server = ServerBuilder.forPort(0).addService(fakeGeo).build().start();

        val properties = new ApplicationProperties();
        properties.getGrpc().getGeoService().setHost("localhost");
        properties.getGrpc().getGeoService().setPort(server.getPort());
        geoService = new GeoServiceImpl(properties);
    }

    @AfterEach
    void tearDown() {
        geoService.shutdown();
        server.shutdownNow();
    }

    /**
     * Verifies that a successful Geo reply is mapped into the domain {@link Location}.
     */
    @Test
    void testSuccessReturnsLocation() {
        replyX = 5;
        replyY = 6;

        val result = geoService.getLocation("Tverskaya");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(Location.create(5, 6).getValue());
    }

    /**
     * Verifies that coordinates outside the delivery field are rejected by {@code Location#create} range checks.
     */
    @Test
    void testOutOfFieldCoordinatesFail() {
        replyX = 100;
        replyY = 6;

        val result = geoService.getLocation("Tverskaya");

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).startsWith("value.");
    }

    /**
     * Verifies that a gRPC error from the Geo service is translated into a failure with the
     * {@code geo.service.unavailable} code instead of a thrown exception.
     */
    @Test
    void testGrpcErrorFails() {
        replyError = new StatusRuntimeException(Status.INTERNAL);

        val result = geoService.getLocation("Tverskaya");

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_GEO_SERVICE_UNAVAILABLE);
    }

    /**
     * Verifies that a reply without the location field is rejected as a Geo service failure.
     */
    @Test
    void testMissingLocationFails() {
        val result = geoService.getLocation("Tverskaya");

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo(Constants.ERR_CODE_GEO_SERVICE_UNAVAILABLE);
    }
}
