package microarch.delivery.core.application.command;

import libs.errs.Error;
import libs.errs.Result;
import microarch.delivery.core.domain.model.order.Order;

public interface CompleteOrderHandler {
    Result<Order, Error> handle(CompleteOrderCommand command);
}
