package microarch.delivery.adapters.in.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import microarch.delivery.core.application.command.AssignOrderCommand;
import microarch.delivery.core.application.command.AssignOrderHandler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic job (a time-driven inbound adapter) that triggers the Assign Order use case every second: the first order in
 * the Created status gets dispatched to the most suitable courier.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssignOrderJob {

    private static final String ERR_CODE_ORDER_NOT_FOUND = "order.not.found";

    private final AssignOrderHandler assignOrderHandler;

    /**
     * Runs every second and delegates to {@link AssignOrderHandler}.
     * <p>
     * Absence of assignable orders is an expected outcome logged at DEBUG; any other failure is logged at WARN.
     */
    @Scheduled(fixedRate = 1000)
    public void assignOrder() {
        val result = assignOrderHandler.handle(new AssignOrderCommand());
        if (result.isSuccess()) {
            log.info("Order has been assigned to courier id={}", result.getValue().getId());
            return;
        }
        if (ERR_CODE_ORDER_NOT_FOUND.equals(result.getError().getCode())) {
            log.debug("No order to assign: {}", result.getError().serialize());
            return;
        }
        log.warn("Assign order job failed: {}", result.getError().serialize());
    }
}
