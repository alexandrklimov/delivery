package microarch.delivery.core.application.command;

import libs.errs.Error;
import libs.errs.Result;
import lombok.RequiredArgsConstructor;
import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Volume;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.ports.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class CreateOrderHandlerImpl implements CreateOrderHandler {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public Result<Order, Error> handle(CreateOrderCommand command) {
        if (command == null)
            return Result
                    .failure(Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "CreateOrderCommand command must not be null!"));

        val locRes = resolveLocation(command.country(), command.city(), command.street(), command.house(),
                command.apartment());
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

    /**
     * Location is random temporary
     *
     * @param country
     * @param city
     * @param street
     * @param house
     * @param apartment
     *
     * @return
     */
    private Result<Location, Error> resolveLocation(final String country, final String city, final String street,
            final String house, final String apartment) {
        val rnd = new Random();
        return Location.create(rnd.nextInt(1, 11), rnd.nextInt(1, 11));
    }
}
