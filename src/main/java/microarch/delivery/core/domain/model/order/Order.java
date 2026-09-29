package microarch.delivery.core.domain.model.order;

import jakarta.validation.constraints.NotNull;
import libs.ddd.Aggregate;
import libs.errs.Error;
import libs.errs.Guard;
import libs.errs.Result;
import lombok.Getter;
import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Volume;

import java.util.Map;
import java.util.UUID;

@Getter
public class Order extends Aggregate<UUID> {
    private static final Map<OrderStatus, OrderStatus> ALLOWED_TARGET_FROM_SRC_STATUS_MAP = Map.of(OrderStatus.Assigned,
            OrderStatus.Created, OrderStatus.Completed, OrderStatus.Assigned);

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
     * Only allowed status transitions are listed below:
     * <ul>
     * <li>{@link OrderStatus#Created} -&gt; {@link OrderStatus#Assigned}</li>
     * <li>{@link OrderStatus#Assigned} -&gt; {@link OrderStatus#Completed}</li>
     * </ul>
     *
     * @param targetStatus
     *            must not be null
     *
     * @return if requested transition is allowed, successful result contains modified Order <i>(this obj.)</i>;
     *         otherwise - result contains err description
     */
    public Result<Order, Error> changeStatus(@NotNull final OrderStatus targetStatus) {
        if (targetStatus == null)
            return Result.failure(Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "targetStatus must not be null"));

        if (status == ALLOWED_TARGET_FROM_SRC_STATUS_MAP.get(targetStatus)) {
            status = targetStatus;
            return Result.success(this);
        }
        return Result.failure(Error.of("order.transition.not.allowed",
                "Status transition is not allowed: %s -> %s".formatted(status, targetStatus)));
    }
}