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
public class Distance extends ValueObject<Distance> {
    static final int MIN_VALUE = 0;

    private final int value;

    public static Result<Distance, Error> create(final int distanceValue) {
        val err = Guard.againstLessThan(distanceValue, MIN_VALUE, "distanceValue");

        return (err == null) ?
            Result.success(new Distance(distanceValue)) :
            Result.failure(err);
    }

    @Override
    protected Iterable<Object> equalityComponents() {
        return List.of(value);
    }
}
