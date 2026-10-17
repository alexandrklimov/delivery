package microarch.delivery.core.application.command;

import libs.errs.Error;
import libs.errs.Result;
import microarch.delivery.core.domain.model.courier.Courier;

public interface CreateCourierHandler {
    Result<Courier, Error> handle(CreateCourierCommand command);
}
