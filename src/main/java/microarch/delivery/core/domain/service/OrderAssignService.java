package microarch.delivery.core.domain.service;

import jakarta.validation.constraints.NotNull;
import libs.errs.Error;
import libs.errs.Result;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.domain.model.order.Order;

import java.util.Collection;

public interface OrderAssignService {
    Result<Courier, Error> assign(@NotNull Order order, @NotNull Collection<Courier> couriers);
}
