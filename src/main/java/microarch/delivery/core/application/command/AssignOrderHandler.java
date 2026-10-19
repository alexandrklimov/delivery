package microarch.delivery.core.application.command;

import libs.errs.Error;
import libs.errs.Result;
import microarch.delivery.core.domain.model.courier.Courier;

public interface AssignOrderHandler {
    /**
     * Assigns the first not assigned order to the most suitable courier.
     *
     * @param command
     *            must not be null
     *
     * @return if success - a Result contains the Courier the order has been assigned to; otherwise - a Result contains
     *         an Error
     */
    Result<Courier, Error> handle(AssignOrderCommand command);
}
