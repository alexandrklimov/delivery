package microarch.delivery.core.application.command;

import org.springframework.util.StringUtils;

public record CreateCourierCommand(String name) {
    public CreateCourierCommand {
        if (!StringUtils.hasText(name))
            throw new CommandCreateValidationException("name can't be empty");
    }
}
