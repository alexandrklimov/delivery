package microarch.delivery.core.application.command;

import libs.errs.Error;
import libs.errs.Result;
import microarch.delivery.core.domain.model.courier.Courier;

public interface AssignOrderHandler {
    Result<Courier, Error> handle(AssignOrderCommand command);
}
