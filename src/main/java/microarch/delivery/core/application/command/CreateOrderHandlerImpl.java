package microarch.delivery.core.application.command;

import libs.errs.Error;
import libs.errs.Result;
import lombok.RequiredArgsConstructor;
import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.Volume;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.ports.GeoService;
import microarch.delivery.core.ports.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case "Create Order": resolves the delivery address into a {@code Location} via the Geo service port and persists
 * a new {@link Order} in the {@code Created} status.
 */
@Service
@RequiredArgsConstructor
public class CreateOrderHandlerImpl implements CreateOrderHandler {

    private final OrderRepository orderRepository;
    private final GeoService geoService;

    @Override
    @Transactional
    public Result<Order, Error> handle(CreateOrderCommand command) {
        if (command == null)
            return Result
                    .failure(Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "CreateOrderCommand command must not be null!"));

        val locRes = geoService.getLocation(command.street());
        if (locRes.isFailure())
            return Result.failure(locRes.getError());

        val volumeRes = Volume.create(command.volume());
        if (volumeRes.isFailure())
            return Result.failure(volumeRes.getError());

        val newOrderRes = Order.create(command.orderId(), locRes.getValue(), volumeRes.getValue());
        if (newOrderRes.isFailure())
            return Result.failure(newOrderRes.getError());

        return Result.success(orderRepository.create(newOrderRes.getValueOrThrow()));
    }
}
