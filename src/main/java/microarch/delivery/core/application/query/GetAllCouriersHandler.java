package microarch.delivery.core.application.query;

import libs.errs.Error;
import libs.errs.Result;

import java.util.List;

public interface GetAllCouriersHandler {
    Result<List<CourierDTO>, Error> handle(GetAllCouriersQuery query);
}
