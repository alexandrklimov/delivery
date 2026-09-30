package microarch.delivery.core.domain.model.courier;

import jakarta.validation.constraints.NotNull;
import libs.ddd.Aggregate;
import libs.errs.Error;
import libs.errs.Guard;
import libs.errs.Result;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import microarch.delivery.Constants;
import microarch.delivery.core.domain.model.Assignment;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Volume;
import microarch.delivery.core.domain.model.order.Order;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class Courier extends Aggregate<UUID> {
    static final int MAX_VOLUME = 20;
    static final int STEP_SIZE = 1;

    private final String name;
    private Location location;
    private final Volume maxVolume = Volume.create(MAX_VOLUME).getValueOrThrow();
    @Getter(AccessLevel.NONE)
    private final Map<UUID, Assignment> orderIdToAssignMap = new ConcurrentHashMap<>();

    private Courier(final String name, final Location location) {
        super(UUID.randomUUID());
        this.name = name;
        this.location = location;
    }

    public static Result<Courier, Error> create(final String name, final Location location) {
        val err = Guard.combine(
                (name == null || name.isBlank())
                        ? Error.of("courier.name.is.blank", "name must not be null or blank string") : null,
                (location == null) ? Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "location must not be null") : null);

        return (err == null) ? Result.success(new Courier(name, location)) : Result.failure(err);
    }

    /**
     *
     * @return unmodifiable set of Assignments
     */
    public Set<Assignment> getAssignments() {
        return Set.copyOf(orderIdToAssignMap.values());
    }

    /**
     * Checks whether the courier can take one more order with the given volume without exceeding the max allowed volume
     * (MAX_VOLUME).
     *
     * <p>
     * Contract — this is NOT a plain predicate, failure is an expected outcome:
     * <ul>
     * <li>{@code Result.success(true)} — the volume fits, the courier is eligible;</li>
     * <li>{@code Result.failure("courier.maxvol.restriction")} — the courier is overloaded for this volume. This is a
     * normal business answer, not a contract violation: callers must branch on
     * {@link Result#isSuccess()}/{@link Result#isFailure()} and must NOT call {@link Result#getValueOrThrow()}.</li>
     * </ul>
     *
     * <p>
     * Note: on success the value is always {@code true}; the {@code Boolean} payload is redundant and may be dropped in
     * favor of {@code Result<Void, Error>} in the future.
     *
     * @param newOrderVolume
     *            must not be null
     *
     * @return success(true) if the volume fits; failure with the restriction error otherwise
     */
    public Result<Boolean, Error> checkCanAssignOneMore(@NotNull Volume newOrderVolume) {
        if (newOrderVolume == null) {
            return Result.failure(Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "newOrderVolume must not be null"));
        } else {
            val currentVolume = orderIdToAssignMap.values().stream().map(it -> it.getVolume().getValue()).reduce(0,
                    Integer::sum);
            val plannedVolume = currentVolume + newOrderVolume.getValue();

            return (plannedVolume > maxVolume.getValue())
                    ? Result.failure(Error.of("courier.maxvol.restriction",
                            "Planned volume is %s but max allowed is %s only".formatted(plannedVolume, MAX_VOLUME)))
                    : Result.success(true);
        }
    }

    public Result<Courier, Error> assignOrder(@NotNull final Order order) {
        val checkResult = Optional.ofNullable(order).map(o -> checkCanAssignOneMore(o.getVolume())).map(result -> {
            if (orderIdToAssignMap.containsKey(order.getId()))
                return Result
                        .failure(Error.of("courier.already.has.order", "Courier already has orderId=" + order.getId()));
            else
                return result;
        }).orElse(Result.failure(Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "order must not be null")));

        if (checkResult.isFailure()) {
            return Result.failure(checkResult.getError());
        }

        val assignment = Assignment.create(order.getId(), order.getVolume(), order.getLocation()).getValueOrThrow();

        orderIdToAssignMap.put(assignment.getOrderId(), assignment);

        return Result.success(this);
    }

    public Result<Courier, Error> completeAssignment(@NotNull final UUID orderId) {
        if (orderId == null)
            return Result.failure(Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "orderId must not be null"));

        val assignment = orderIdToAssignMap.get(orderId);
        if (assignment == null)
            return Result.failure(Error.of("courier.order.not.found", "Courier does not have orderId=" + orderId));

        val distance = assignment.getLocation().computeDistance(location).getValueOrThrow();
        if (distance.getValue() > STEP_SIZE)
            return Result.failure(Error.of("courier.out.of.completion.zone", "Courier is out of completion zone"));

        val completeResult = assignment.complete(location);
        if (completeResult.isSuccess())
            orderIdToAssignMap.remove(orderId);
        return completeResult.map(x -> this);
    }

    public Result<Courier, Error> moveUp() {
        val newLocationRes = Location.create(location.getX(), location.getY() + STEP_SIZE);
        return setNewLocation(newLocationRes);

    }

    public Result<Courier, Error> moveDown() {
        val newLocationRes = Location.create(location.getX(), location.getY() - STEP_SIZE);
        return setNewLocation(newLocationRes);
    }

    public Result<Courier, Error> moveLeft() {
        val newLocationRes = Location.create(location.getX() - STEP_SIZE, location.getY());
        return setNewLocation(newLocationRes);
    }

    public Result<Courier, Error> moveRight() {
        val newLocationRes = Location.create(location.getX() + STEP_SIZE, location.getY());
        return setNewLocation(newLocationRes);
    }

    private Result<Courier, Error> setNewLocation(Result<Location, Error> newLocationRes) {
        if (newLocationRes.isSuccess()) {
            this.location = newLocationRes.getValue();
            return Result.success(this);
        } else {
            return Result.failure(newLocationRes.getError());
        }
    }
}