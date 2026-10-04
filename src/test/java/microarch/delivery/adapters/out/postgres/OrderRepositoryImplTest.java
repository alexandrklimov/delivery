package microarch.delivery.adapters.out.postgres;

import lombok.val;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Volume;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.domain.model.order.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(OrderRepositoryImpl.class)
class OrderRepositoryImplTest extends BaseJpaTest {

    @Autowired
    private OrderRepositoryImpl orderRepositoryImpl;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    private static Order newOrder() {
        return Order.create(UUID.randomUUID(), Location.create(5, 5).getValue(), Volume.create(10).getValue())
                .getValue();
    }

    private static Order newAssignedOrder() {
        val order = newOrder();
        order.markAsAssigned();
        return order;
    }

    @Test
    void create() {
        val id = UUID.randomUUID();
        val order = Order.create(id, Location.create(5, 5).getValue(), Volume.create(10).getValue()).getValue();

        val saved = orderRepositoryImpl.create(order);

        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.Created);
        assertThat(saved.getLocation()).isEqualTo(Location.create(5, 5).getValue());
        assertThat(saved.getVolume()).isEqualTo(Volume.create(10).getValue());
        orderJpaRepository.flush();
        assertThat(orderJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void update() {
        val order = orderRepositoryImpl.create(newOrder());
        order.markAsAssigned();

        orderRepositoryImpl.update(order);
        orderJpaRepository.flush();
        testEntityManager.clear();

        val found = orderJpaRepository.findById(order.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.Assigned);
    }

    @Test
    void findById() {
        val order = orderRepositoryImpl.create(newOrder());

        val found = orderRepositoryImpl.findById(order.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(order.getId());
        assertThat(found.get().getLocation()).isEqualTo(order.getLocation());
        assertThat(found.get().getVolume()).isEqualTo(order.getVolume());

        assertThat(orderRepositoryImpl.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void getAnyInCreatedStatus() {
        assertThat(orderRepositoryImpl.getAnyInCreatedStatus()).isEmpty();

        val assignedOrder = newAssignedOrder();
        val createdOrder = newOrder();
        orderJpaRepository.saveAllAndFlush(List.of(assignedOrder, createdOrder));

        val found = orderRepositoryImpl.getAnyInCreatedStatus();

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(createdOrder.getId());
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.Created);
    }

    @Test
    void getAllInAssignedStatus() {
        val assigned = orderJpaRepository.saveAllAndFlush(List.of(newAssignedOrder(), newAssignedOrder()));
        val assignedIdSet = assigned.stream().map(Order::getId).collect(Collectors.toSet());
        orderJpaRepository.saveAndFlush(newOrder());

        val expectedIds = orderJpaRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream().map(Order::getId)
                .filter(id -> assignedIdSet.contains(id)).toList();

        val page = orderRepositoryImpl.getAllInAssignedStatus(0, 2);

        assertThat(page.getContent()).extracting(Order::getId).containsExactlyElementsOf(expectedIds);
        assertThat(page.getContent()).extracting(Order::getStatus).containsOnly(OrderStatus.Assigned);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(1);
    }
}
