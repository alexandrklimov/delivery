package microarch.delivery.core.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import libs.ddd.ValueObject;
import libs.errs.Error;
import libs.errs.Guard;
import libs.errs.Result;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.val;
import microarch.delivery.Constants;

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PUBLIC, force = true)
@Getter
@Embeddable
public class Location extends ValueObject<Location> {

    static final int MIN_X = 1;
    static final int MIN_Y = 1;
    static final int MAX_X = 10;
    static final int MAX_Y = 10;

    @Column(name = "location_x")
    private final int x;
    @Column(name = "location_y")
    private final int y;

    public static Result<Location, Error> create(final int x, final int y) {
        val err = Guard.combine(Guard.againstOutOfRange(x, MIN_X, MAX_X, "x"),
                Guard.againstOutOfRange(y, MIN_Y, MAX_Y, "y"));

        return (err == null) ? Result.success(new Location(x, y)) : Result.failure(err);

    }

    public Result<Distance, Error> computeDistance(final Location to) {
        val err = (to == null) ? Error.of(Constants.ERR_CODE_OBJ_IS_NULL, "'Location to' must not be NULL!") : null;

        if (err != null)
            return Result.failure(err);

        val distanceValue = Math.abs(to.getX() - x) + Math.abs(to.getY() - y);
        return Distance.create(distanceValue);
    }

    @Override
    protected Iterable<Object> equalityComponents() {
        return List.of(x, y);
    }
}
