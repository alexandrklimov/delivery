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
        val moveRes = moveTo(courier, command.location());
        if (moveRes.isFailure())
            return Result.failure(moveRes.getError());

        return Result.success(courierRepository.update(moveRes.getValue()));
    }

    /**
     * Moves the courier to the target location by performing as many one-step moves (see Courier.STEP_SIZE) as needed,
     * stepping towards the target along the X axis first and then along the Y axis.
     *
     * <p>
     * The target location is always within the field bounds (it is a valid Location), and every step reduces the
     * Manhattan distance to the target by exactly one, so the loop terminates after at most |dx| + |dy| steps.
     *
     * @param courier
     *            must not be null
     * @param target
     *            must not be null
     *
     * @return if success - a Result contains modified Courier; otherwise - a Result contains an Error
     */
    private Result<Courier, Error> moveTo(final Courier courier, final Location target) {
        while (!courier.getLocation().equals(target)) {
            val dx = target.getX() - courier.getLocation().getX();
            val dy = target.getY() - courier.getLocation().getY();

            val stepRes = (dx > 0) ? courier.moveRight()
                    : (dx < 0) ? courier.moveLeft() : (dy > 0) ? courier.moveUp() : courier.moveDown();
            if (stepRes.isFailure())
                return Result.failure(stepRes.getError());
        }
        return Result.success(courier);
    }
}
