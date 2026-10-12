package microarch.delivery.core.application.command;

import libs.errs.Error;
import libs.errs.Result;
import microarch.delivery.core.domain.model.order.Order;

public interface CreateOrderHandler {
    Result<Order, Error> handle(CreateOrderCommand command);
}
