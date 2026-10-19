package microarch.delivery.core.application.command;

/**
 * This command has no fields: the system takes the first not assigned order and dispatches it to the most suitable
 * courier by itself.
 */
public record AssignOrderCommand() {
}
