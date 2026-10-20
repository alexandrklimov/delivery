package microarch.delivery.adapters.in.http;

import lombok.RequiredArgsConstructor;
import lombok.val;
import microarch.delivery.adapters.in.http.api.CompleteOrderApi;
import microarch.delivery.adapters.in.http.api.CreateOrderApi;
import microarch.delivery.adapters.in.http.api.GetOrdersApi;
import microarch.delivery.adapters.in.http.model.CreateOrderResponse;
import microarch.delivery.adapters.in.http.model.Location;
import microarch.delivery.adapters.in.http.model.NewOrder;
import microarch.delivery.adapters.in.http.model.Order;
import microarch.delivery.core.application.command.*;
import microarch.delivery.core.application.query.GetNotCompletedOrdersHandler;
import microarch.delivery.core.application.query.GetNotCompletedOrdersQuery;
import microarch.delivery.core.application.query.OrderDTO;
import microarch.delivery.core.domain.model.Volume;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Inbound HTTP adapter for order endpoints: implements the generated OpenAPI interfaces and delegates each request to
 * the corresponding application-layer use case (CQRS-style handlers).
 */
@RestController
@RequiredArgsConstructor
public class OrderController implements CreateOrderApi, GetOrdersApi, CompleteOrderApi {

    private final CreateOrderHandler createOrderHandler;
    private final GetNotCompletedOrdersHandler getNotCompletedOrdersHandler;
    private final CompleteOrderHandler completeOrderHandler;

    /**
     * Creates a new order from the incoming request body.
     *
     * @param newOrder
     *            new order payload (id, address, volume)
     *
     * @return {@code 201} with the created order id; an error status mapped from the use case failure
     */
    @Override
    public ResponseEntity<CreateOrderResponse> createOrder(final NewOrder newOrder) {
        val command = toCommand(newOrder);
        val result = createOrderHandler.handle(command);
        if (result.isFailure())
            throw ApiException.of(result.getError());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateOrderResponse().orderId(result.getValue().getId()));
    }

    /**
     * Returns all not completed orders.
     *
     * @return {@code 200} with the list of orders; an error status mapped from the query failure
     */
    @Override
    public ResponseEntity<List<Order>> getOrders() {
        val result = getNotCompletedOrdersHandler.handle(new GetNotCompletedOrdersQuery());
        if (result.isFailure())
            throw ApiException.of(result.getError());

        val orders = result.getValue().stream().map(this::toModel).toList();
        return ResponseEntity.ok(orders);
    }

    /**
     * Completes the order assigned to the given courier.
     *
     * @param courierId
     *            id of the courier holding the order
     * @param orderId
     *            id of the order to complete
     *
     * @return {@code 200} on success; an error status mapped from the use case failure
     */
    @Override
    public ResponseEntity<Void> completeOrder(final UUID courierId, final UUID orderId) {
        val result = completeOrderHandler.handle(new CompleteOrderCommand(courierId, orderId));
        if (result.isFailure())
            throw ApiException.of(result.getError());

        return ResponseEntity.ok().build();
    }

    /**
     * Converts the HTTP payload into a {@link CreateOrderCommand}, validating the parts the command constructor cannot
     * check by itself (null address, null volume, volume value).
     *
     * @param newOrder
     *            incoming payload
     *
     * @return the validated command
     */
    private CreateOrderCommand toCommand(final NewOrder newOrder) {
        val address = newOrder.getAddress();
        if (address == null)
            throw new CommandCreateValidationException("address must not be null");
        if (newOrder.getVolume() == null)
            throw new CommandCreateValidationException("volume must not be null");

        val volumeRes = Volume.create(newOrder.getVolume());
        if (volumeRes.isFailure())
            throw new CommandCreateValidationException(volumeRes.getError().getMessage());

        return new CreateOrderCommand(newOrder.getId(), address.getCountry(), address.getCity(), address.getStreet(),
                address.getHouse(), address.getApartment(), volumeRes.getValue());
    }

    /**
     * Maps the read-model {@link OrderDTO} to the HTTP response model.
     *
     * @param dto
     *            query result record
     *
     * @return the HTTP order model
     */
    private Order toModel(final OrderDTO dto) {
        return new Order().id(dto.id()).location(new Location().x(dto.x()).y(dto.y()));
    }
}
