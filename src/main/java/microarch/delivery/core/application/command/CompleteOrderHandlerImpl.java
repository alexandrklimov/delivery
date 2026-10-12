package microarch.delivery.core.application.command;

import libs.errs.Error;
import libs.errs.Result;
import lombok.RequiredArgsConstructor;
import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.ports.CourierRepository;
import microarch.delivery.core.ports.OrderRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompleteOrderHandlerImpl implements CompleteOrderHandler {

    private final CourierRepository courierRepository;
    private final OrderRepository orderRepository;

    @Override
    public Result<Order, Error> handle(final CompleteOrderCommand command) {
        if (command == null)
            return Result.failure(
                    Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "CompleteOrderCommand command must not be null!"));

        val courierOpt = courierRepository.findById(command.courierId());
        if (courierOpt.isEmpty())
            return Result.failure(
                    Error.of("courier.not.found", "Courier with id=%s not found".formatted(command.courierId())));

        val orderOpt = orderRepository.findById(command.orderId());
        if (orderOpt.isEmpty())
            return Result
                    .failure(Error.of("order.not.found", "Order with id=%s not found".formatted(command.orderId())));

        val courier = courierOpt.get();
        val order = orderOpt.get();

        val completeRes = courier.completeAssignment(order.getId());
        if (completeRes.isFailure())
            return Result.failure(completeRes.getError());

        val markRes = order.markAsCompleted();
        if (markRes.isFailure())
            return Result.failure(markRes.getError());

        courierRepository.update(completeRes.getValue());
        return Result.success(orderRepository.update(markRes.getValue()));
    }
}
