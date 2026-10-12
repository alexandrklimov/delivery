package microarch.delivery.core.application.command;

/**
 * This exception intended to be thrown from a command constructor on any argument validation error
 */
public class CommandCreateValidationException extends RuntimeException {
    public CommandCreateValidationException() {
    }

    public CommandCreateValidationException(String message) {
        super(message);
    }

    public CommandCreateValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandCreateValidationException(Throwable cause) {
        super(cause);
    }

    public CommandCreateValidationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
