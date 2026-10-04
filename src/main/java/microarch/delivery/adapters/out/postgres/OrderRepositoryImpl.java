package microarch.delivery.adapters.out.postgres;

import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.val;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.domain.model.order.OrderStatus;
import microarch.delivery.core.ports.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    @Nonnull
    @Override
    public Order create(@Nonnull final Order order) {
        return orderJpaRepository.save(order);
    }

    @Nonnull
    @Override
    public Order update(@Nonnull final Order order) {
        return orderJpaRepository.save(order);
    }

    @Nonnull
    @Override
    public Optional<Order> findById(@Nonnull final UUID id) {
        return orderJpaRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<Order> getAnyInCreatedStatus() {
        return orderJpaRepository.findFirstByStatus(OrderStatus.Created);
    }

    @Nonnull
    @Override
    public Page<Order> getAllInAssignedStatus(int page, int size) {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        return orderJpaRepository.findAllByStatus(OrderStatus.Assigned, pageable);
    }
}
