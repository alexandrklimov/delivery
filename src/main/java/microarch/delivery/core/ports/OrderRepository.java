package microarch.delivery.core.ports;

import jakarta.annotation.Nonnull;
import microarch.delivery.core.domain.model.order.Order;
import org.springframework.data.domain.Page;

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
    Page<Order> getAllInAssignedStatus(int page, int size);
}
