package microarch.delivery.core.application.command;

import libs.ddd.DomainEventPublisher;
import libs.errs.Error;
import libs.errs.Result;
import lombok.RequiredArgsConstructor;
import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.ports.CourierRepository;
import microarch.delivery.core.ports.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case "Complete Order": completes the courier's assignment, moves the order into the {@code Completed} status,
 * persists both aggregates, and publishes the order domain events.
 */
@Service
@RequiredArgsConstructor
public class CompleteOrderHandlerImpl implements CompleteOrderHandler {

    private final CourierRepository courierRepository;
    private final OrderRepository orderRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    @Transactional
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
            return Result.failure(Error.of(Constants.ERR_CODE_ORDER_NOT_FOUND,
                    "Order with id=%s not found".formatted(command.orderId())));

        val courier = courierOpt.get();
        val order = orderOpt.get();

        val completeRes = courier.completeAssignment(order.getId());
        if (completeRes.isFailure())
            return Result.failure(completeRes.getError());

        val markRes = order.markAsCompleted();
        if (markRes.isFailure())
            return Result.failure(markRes.getError());

        courierRepository.update(completeRes.getValue());
        val completedOrder = orderRepository.update(markRes.getValue());
        domainEventPublisher.publish(List.of(order));
        return Result.success(completedOrder);
    }
}
