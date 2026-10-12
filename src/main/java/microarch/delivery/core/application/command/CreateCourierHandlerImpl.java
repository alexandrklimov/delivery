package microarch.delivery.core.application.command;

import libs.errs.Error;
import libs.errs.Result;
import lombok.RequiredArgsConstructor;
import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.ports.CourierRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateCourierHandlerImpl implements CreateCourierHandler {

    private final CourierRepository courierRepository;

    @Override
    public Result<Courier, Error> handler(CreateCourierCommand command) {
        if(command == null)
            return Result.failure(Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "command must not be null"));

        val newCourierRes = Courier.create(command.name(), Location.create(1,1).getValue());
        if(newCourierRes.isFailure())
            return Result.failure(newCourierRes.getError());

        val persisted = courierRepository.create(newCourierRes.getValue());
        return Result.success(persisted);
    }
}
