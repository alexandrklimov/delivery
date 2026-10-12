package microarch.delivery.core.ports;

import jakarta.annotation.Nonnull;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.domain.model.order.OrderStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    @Nonnull
    Order create(final @Nonnull Order order);

    @Nonnull
    Order update(final @Nonnull Order order);

    @Nonnull
    Optional<Order> findById(@Nonnull UUID id);

    @Nonnull
    Optional<Order> getAnyInCreatedStatus();

    @Nonnull
    List<Order> getAllInAssignedStatus();

    @Nonnull
    List<Order> getAllByStatuses(@Nonnull Collection<OrderStatus> statuses);
}
