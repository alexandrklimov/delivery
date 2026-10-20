package microarch.delivery.adapters.in.http;

import lombok.RequiredArgsConstructor;
import lombok.val;
import microarch.delivery.adapters.in.http.api.CreateCourierApi;
import microarch.delivery.adapters.in.http.api.GetCouriersApi;
import microarch.delivery.adapters.in.http.api.MoveCourierApi;
import microarch.delivery.adapters.in.http.model.Courier;
import microarch.delivery.adapters.in.http.model.CreateCourierResponse;
import microarch.delivery.adapters.in.http.model.Location;
import microarch.delivery.adapters.in.http.model.NewCourier;
import microarch.delivery.core.application.command.*;
import microarch.delivery.core.application.query.CourierDTO;
import microarch.delivery.core.application.query.GetAllCouriersHandler;
import microarch.delivery.core.application.query.GetAllCouriersQuery;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Inbound HTTP adapter for courier endpoints: implements the generated OpenAPI interfaces and delegates each request to
 * the corresponding application-layer use case (CQRS-style handlers).
 */
@RestController
@RequiredArgsConstructor
public class CourierController implements CreateCourierApi, GetCouriersApi, MoveCourierApi {

    private final CreateCourierHandler createCourierHandler;
    private final GetAllCouriersHandler getAllCouriersHandler;
    private final MoveCourierHandler moveCourierHandler;

    /**
     * Creates a new courier.
     *
     * @param newCourier
     *            new courier payload (name)
     *
     * @return {@code 201} with the created courier id; an error status mapped from the use case failure
     */
    @Override
    public ResponseEntity<CreateCourierResponse> createCourier(final NewCourier newCourier) {
        val result = createCourierHandler.handle(new CreateCourierCommand(newCourier.getName()));
        if (result.isFailure())
            throw ApiException.of(result.getError());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateCourierResponse().courierId(result.getValue().getId()));
    }

    /**
     * Returns all couriers.
     *
     * @return {@code 200} with the list of couriers; an error status mapped from the query failure
     */
    @Override
    public ResponseEntity<List<Courier>> getCouriers() {
        val result = getAllCouriersHandler.handle(new GetAllCouriersQuery());
        if (result.isFailure())
            throw ApiException.of(result.getError());

        val couriers = result.getValue().stream().map(this::toModel).toList();
        return ResponseEntity.ok(couriers);
    }

    /**
     * Moves the courier to the reported location.
     *
     * @param courierId
     *            id of the courier to move
     * @param location
     *            target location (x, y within the bounded field)
     *
     * @return {@code 200} on success; an error status mapped from the use case failure
     */
    @Override
    public ResponseEntity<Void> moveCourier(final UUID courierId, final Location location) {
        if (location == null)
            throw new CommandCreateValidationException("location must not be null");
        if (location.getX() == null || location.getY() == null)
            throw new CommandCreateValidationException("x and y must not be null");

        // Fully qualified: name clash with the generated HTTP model Location imported above.
        val locationRes = microarch.delivery.core.domain.model.Location.create(location.getX(), location.getY());
        if (locationRes.isFailure())
            throw new CommandCreateValidationException(locationRes.getError().getMessage());

        val result = moveCourierHandler.handle(new MoveCourierCommand(courierId, locationRes.getValue()));
        if (result.isFailure())
            throw ApiException.of(result.getError());

        return ResponseEntity.ok().build();
    }

    /**
     * Maps the read-model {@link CourierDTO} to the HTTP response model.
     *
     * @param dto
     *            query result record
     *
     * @return the HTTP courier model
     */
    private Courier toModel(final CourierDTO dto) {
        return new Courier().id(dto.id()).name(dto.name()).location(new Location().x(dto.x()).y(dto.y()));
    }
}
