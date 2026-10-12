package microarch.delivery.core.application.query;

import libs.errs.Error;
import libs.errs.Result;
import lombok.RequiredArgsConstructor;
import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.ports.CourierRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAllCouriersHandlerImpl implements GetAllCouriersHandler {

    private final CourierRepository courierRepository;

    @Override
    public Result<List<CourierDTO>, Error> handle(final GetAllCouriersQuery query) {
        if (query == null)
            return Result
                    .failure(Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "GetAllCouriersQuery query must not be null!"));

        val couriers = courierRepository.getAll().stream()
                .map(c -> new CourierDTO(c.getId(), c.getName(), c.getLocation())).toList();
        return Result.success(couriers);
    }
}
