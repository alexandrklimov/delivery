package microarch.delivery.core.application.query;

import libs.errs.Error;
import libs.errs.Result;

import java.util.List;

public interface GetNotCompletedOrdersHandler {
    Result<List<OrderDTO>, Error> handle(GetNotCompletedOrdersQuery query);
}
