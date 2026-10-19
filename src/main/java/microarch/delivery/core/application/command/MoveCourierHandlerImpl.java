package microarch.delivery.core.application.command;

import libs.errs.Error;
import libs.errs.Result;
import lombok.RequiredArgsConstructor;
import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.ports.CourierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MoveCourierHandlerImpl implements MoveCourierHandler {

    private final CourierRepository courierRepository;

    @Override
    @Transactional
    public Result<Courier, Error> handle(final MoveCourierCommand command) {
        if (command == null)
            return Result
                    .failure(Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "MoveCourierCommand command must not be null!"));

        val courierOpt = courierRepository.findById(command.courierId());
        if (courierOpt.isEmpty())
            return Result.failure(
                    Error.of("courier.not.found", "Courier with id=%s not found".formatted(command.courierId())));

        val courier = courierOpt.get();
        val moveRes = courier.moveTo(command.location());
        if (moveRes.isFailure())
            return Result.failure(moveRes.getError());

        return Result.success(courierRepository.update(moveRes.getValue()));
    }
}
