package libs.errs;

import java.util.Collection;
import java.util.UUID;

public final class Guard {

    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    private Guard() {
    }

    @SafeVarargs
    public static Error combine(Error... errors) {
        for (Error e : errors) {
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    public static Error againstNullOrEmpty(String value, String paramName) {
        if (value == null || value.isBlank()) {
            return GeneralErrors.valueIsRequired(paramName);
        }
        return null;
    }

    public static Error againstNullOrEmpty(Collection<?> collection, String paramName) {
        if (collection == null || collection.isEmpty()) {
            return GeneralErrors.valueIsRequired(paramName);
        }
        return null;
    }

    public static Error againstNullOrEmpty(UUID uuid, String paramName) {
        if (uuid == null || uuid.equals(EMPTY_UUID)) {
            return GeneralErrors.valueIsRequired(paramName);
        }
        return null;
    }

    public static <T extends Comparable<T>> Error againstGreaterThan(T value, T max, String paramName) {

        if (value == null || value.compareTo(max) > 0) {
            return GeneralErrors.valueMustBeLessThan(paramName, value, max);
        }

        return null;
    }

    public static <T extends Comparable<T>> Error againstGreaterOrEqual(T value, T max, String paramName) {

        if (value == null || value.compareTo(max) >= 0) {
            return GeneralErrors.valueMustBeLessOrEqual(paramName, value, max);
        }

        return null;
    }

    public static <T extends Comparable<T>> Error againstLessThan(T value, T min, String paramName) {

        if (value == null || value.compareTo(min) < 0) {
            return GeneralErrors.valueMustBeGreaterThan(paramName, value, min);
        }

        return null;
    }

    public static <T extends Comparable<T>> Error againstLessOrEqual(T value, T min, String paramName) {

        if (value == null || value.compareTo(min) <= 0) {
            return GeneralErrors.valueMustBeGreaterOrEqual(paramName, value, min);
        }

        return null;
    }

    public static <T extends Comparable<T>> Error againstOutOfRange(T value, T min, T max, String paramName) {

        if (value == null || value.compareTo(min) < 0 || value.compareTo(max) > 0) {

            return GeneralErrors.valueIsOutOfRange(paramName, value, min, max);
        }

        return null;
    }
}