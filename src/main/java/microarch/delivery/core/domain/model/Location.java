package microarch.delivery.core.domain.model;

import libs.ddd.ValueObject;
import libs.errs.Error;
import libs.errs.Guard;
import libs.errs.Result;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;

import java.util.List;


@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Location extends ValueObject<Location> {

    static final int MIN_X = 1;
    static final int MIN_Y = 1;
    static final int MAX_X = 10;
    static final int MAX_Y = 10;

    private final int x;
    private final int y;

    public static Result<Location, Error> create(final int x, final int y) {
        val err = Guard.combine(
            Guard.againstOutOfRange(x, MIN_X, MAX_X, "x"),
            Guard.againstOutOfRange(y, MIN_Y, MAX_Y, "y")
        );

        return (err == null) ?
            Result.success(new Location(x, y)) :
            Result.failure(err);

    }

    public static Result<Distance, Error> computeDistance(final Location from, final Location to) {
        val err = Guard.combine(
            (from == null) ? Error.of("object.is.null", "'Location from' must not be NULL!") : null,
            (to == null) ? Error.of("object.is.null", "'Location to' must not be NULL!") : null
        );

        if (err != null)
            return Result.failure(err);

        val distanceValue = Math.abs(to.getX() - from.getX()) + Math.abs(to.getY() - from.getY());
        return Distance.create(distanceValue);
    }

    @Override
    protected Iterable<Object> equalityComponents() {
        return List.of(x, y);
    }
}
