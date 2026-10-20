package microarch.delivery;

import microarch.delivery.adapters.in.http.ApiException;
import microarch.delivery.adapters.in.http.model.Error;
import microarch.delivery.core.application.command.CommandCreateValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Central HTTP error rendering: maps adapter and application exceptions to the contract error body ({@code code} +
 * {@code message}) with the corresponding HTTP status.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Renders invalid request payloads (bad or missing fields, malformed JSON) as {@code 400}.
     *
     * @param ex
     *            the validation failure
     *
     * @return {@code 400} with the error body
     */
    @ExceptionHandler({ CommandCreateValidationException.class, HttpMessageNotReadableException.class })
    public ResponseEntity<Error> handleBadRequest(final Exception ex) {
        return errorBody(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Renders {@link ApiException} (a domain {@code Result} failure translated by the HTTP adapter) with the status
     * carried by the exception.
     *
     * @param ex
     *            the exception thrown by a controller
     *
     * @return the status from the exception with the error body
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Error> handleApiException(final ApiException ex) {
        return errorBody(ex.getStatus(), ex.getMessage());
    }

    /**
     * Fallback for unexpected failures — {@code 500} without a body.
     *
     * @param ex
     *            the unexpected failure
     *
     * @return {@code 500}
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(final Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * Builds the contract error body: {@code code} equals the HTTP status value.
     *
     * @param status
     *            HTTP status to respond with
     * @param message
     *            human-readable error message
     *
     * @return the response entity with the error body
     */
    private ResponseEntity<Error> errorBody(final HttpStatus status, final String message) {
        return ResponseEntity.status(status).body(new Error().code(status.value()).message(message));
    }
}
