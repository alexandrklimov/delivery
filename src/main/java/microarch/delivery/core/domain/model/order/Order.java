package microarch.delivery.core.domain.model.order;

import libs.ddd.Aggregate;
import libs.errs.Error;
import libs.errs.Guard;
import libs.errs.Result;
import lombok.Getter;
import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Volume;

import java.util.UUID;

@Getter
public class Order extends Aggregate<UUID> {
    private final Location location;
    private final Volume volume;
    private OrderStatus status;

    private Order(UUID id, Location location, Volume volume, OrderStatus status) {
        super(id);
        this.location = location;
        this.volume = volume;
        this.status = status;
    }

    public static Result<Order, Error> create(final UUID id, final Location location, final Volume volume) {
        val err = Guard.combine((id == null) ? Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "id must not be null") : null,
                (location == null) ? Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "location must not be null") : null,
                (volume == null) ? Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "volume must not be null") : null);

        return (err == null) ? Result.success(new Order(id, location, volume, OrderStatus.Created))
                : Result.failure(err);
    }

    /**
     * Allowed transition: {@link OrderStatus#Created} -&gt; {@link OrderStatus#Assigned}.
     *
     * @return if requested transition is allowed, successful result contains modified Order <i>(this obj.)</i>;
     *         otherwise - result contains err description
     */
    public Result<Order, Error> markAsAssigned() {
        return transition(OrderStatus.Created, OrderStatus.Assigned);
    }

    /**
     * Allowed transition: {@link OrderStatus#Assigned} -&gt; {@link OrderStatus#Completed}.
     *
     * @return if requested transition is allowed, successful result contains modified Order <i>(this obj.)</i>;
     *         otherwise - result contains err description
     */
    public Result<Order, Error> markAsCompleted() {
        return transition(OrderStatus.Assigned, OrderStatus.Completed);
    }

    private Result<Order, Error> transition(final OrderStatus expectedStatus, final OrderStatus targetStatus) {
        if (status != expectedStatus)
            return Result.failure(Error.of("order.transition.not.allowed",
                    "Status transition is not allowed: %s -> %s".formatted(status, targetStatus)));

        status = targetStatus;
        return Result.success(this);
    }
}