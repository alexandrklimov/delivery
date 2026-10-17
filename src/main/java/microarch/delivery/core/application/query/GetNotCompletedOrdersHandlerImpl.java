package microarch.delivery.core.application.query;

import libs.errs.Error;
import libs.errs.Result;
import lombok.RequiredArgsConstructor;
import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.order.OrderStatus;
import microarch.delivery.core.ports.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetNotCompletedOrdersHandlerImpl implements GetNotCompletedOrdersHandler {

    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public Result<List<OrderDTO>, Error> handle(final GetNotCompletedOrdersQuery query) {
        if (query == null)
            return Result.failure(
                    Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "GetNotCompletedOrdersQuery query must not be null!"));

        val orders = orderRepository.getAllByStatuses(List.of(OrderStatus.Created, OrderStatus.Assigned)).stream()
                .map(o -> new OrderDTO(o.getId(), o.getLocation())).toList();
        return Result.success(orders);
    }
}
