package microarch.delivery.core.domain.model;

import libs.ddd.BaseEntity;
import libs.errs.Error;
import libs.errs.Guard;
import libs.errs.Result;
import lombok.Getter;
import lombok.val;
import microarch.delivery.Constants;

import java.util.UUID;

@Getter
public class Assignment extends BaseEntity<UUID> {
    private final UUID orderId;
    private final Volume volume;
    private final Location location;
    private Status status;

    private Assignment(final UUID orderId, final Volume volume, final Location location, final Status status) {
        super(UUID.randomUUID());
        this.orderId = orderId;
        this.volume = volume;
        this.location = location;
        this.status = status;
    }

    public static Result<Assignment, Error> create(final UUID orderId, final Volume volume, final Location location) {
        val err = Guard.combine(
            (orderId == null) ? Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "orderId must not be null") : null,
            (volume == null) ? Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "volume must not be null") : null,
            (location == null) ? Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "location must not be null") : null
        );

        return (err == null) ? Result.success(new Assignment(orderId, volume, location, Status.Assigned))
            : Result.failure(err);
    }

    /**
     *
     * @param courierLocation
     * @return if success - a Result contains modifies instance of the Assignment; otherwise - a Result contains an
     * Error
     */
    public Result<Assignment, Error> complete(final Location courierLocation) {
        val err = Guard.combine(
            (courierLocation == null) ? Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "courierLocation must not be null")
                : null,
            (status == Status.Completed)
                ? Error.of("assignment.wrong.transition", "Assignment " + id + " is already completed.")
                : null);
        if (err == null) {
            val distance = courierLocation.computeDistance(location).getValueOrThrow();

            if (distance.getValue() > 1) {
                return Result.failure(Error.of("out.of.completion.zone", "Out of completion zone"));
            } else {
                status = Status.Completed;
                return Result.success(this);
            }

        } else {
            return Result.failure(err);
        }
    }
}
