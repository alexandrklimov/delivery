package microarch.delivery.core.application.command;

import libs.errs.Error;
import libs.errs.Result;
import lombok.RequiredArgsConstructor;
import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.domain.service.OrderAssignService;
import microarch.delivery.core.ports.CourierRepository;
import microarch.delivery.core.ports.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssignOrderHandlerImpl implements AssignOrderHandler {

    private final OrderRepository orderRepository;
    private final CourierRepository courierRepository;
    private final OrderAssignService orderAssignService;

    @Override
    @Transactional
    public Result<Courier, Error> handle(final AssignOrderCommand command) {
        if (command == null)
            return Result
                    .failure(Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "AssignOrderCommand command must not be null!"));

        val orderOpt = orderRepository.getAnyInCreatedStatus();
        if (orderOpt.isEmpty())
            return Result.failure(Error.of("order.not.found", "There is no any order in Created status to assign"));

        val order = orderOpt.get();
        val couriers = courierRepository.getAll();

        val assignRes = orderAssignService.assign(order, couriers);
        if (assignRes.isFailure())
            return Result.failure(assignRes.getError());

        orderRepository.update(order);
        return Result.success(courierRepository.update(assignRes.getValue()));
    }
}
