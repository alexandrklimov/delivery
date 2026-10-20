package microarch.delivery.adapters.in.http;

import libs.errs.Error;
import lombok.val;
import microarch.delivery.Constants;
import org.springframework.http.HttpStatus;

import java.util.Set;

/**
 * RuntimeException that carries a domain {@link Error} translated into an HTTP semantics (status + message) so that
 * {@code GlobalExceptionHandler} can render the contract error body.
 */
public class ApiException extends RuntimeException {

    private static final Set<String> CONFLICT_CODES = Set.of("courier.not.found", "order.not.found",
            "courier.order.not.found", "courier.out.of.completion.zone", "out.of.completion.zone",
            "order.transition.not.allowed", "courier.already.has.order", "courier.maxvol.restriction",
            "courier.name.is.blank");

    private final HttpStatus status;

    private ApiException(final HttpStatus status, final String message) {
        super(message);
        this.status = status;
    }

    /**
     * Maps a domain {@link Error} to an {@link ApiException}:
     * <ul>
     * <li>input-validation codes ({@code value.*}, {@code object.is.null}) — {@code 400 Bad Request};</li>
     * <li>known business/state-conflict codes — {@code 409 Conflict};</li>
     * <li>anything else — {@code 500 Internal Server Error}.</li>
     * </ul>
     *
     * @param error
     *            must not be null
     *
     * @return the exception to be thrown up to {@code GlobalExceptionHandler}
     */
    public static ApiException of(final Error error) {
        val code = error.getCode();
        if (code.startsWith("value.") || Constants.ERR_CODE_OBJ_IS_NULL.equals(code)) {
            return new ApiException(HttpStatus.BAD_REQUEST, error.getMessage());
        }
        if (CONFLICT_CODES.contains(code)) {
            return new ApiException(HttpStatus.CONFLICT, error.getMessage());
        }
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, error.getMessage());
    }

    public HttpStatus getStatus() {
        return status;
    }
}
