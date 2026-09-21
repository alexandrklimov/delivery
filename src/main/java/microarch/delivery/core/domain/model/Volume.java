package microarch.delivery.core.domain.model;

import libs.ddd.ValueObject;
import libs.errs.Error;
import libs.errs.Guard;
import libs.errs.Result;
import lombok.Getter;
import lombok.val;

import java.util.List;

@Getter
public class Volume extends ValueObject<Volume> {
    private final int value;

    private Volume(int value) {
        this.value = value;
    }

    public static Result<Volume, Error> create(final int value) {
        val err = Guard.againstLessOrEqual(value, 0, "value");
        return (err == null) ? Result.success(new Volume(value)) : Result.failure(err);
    }

    @Override
    protected Iterable<Object> equalityComponents() {
        return List.of(value);
    }
}
