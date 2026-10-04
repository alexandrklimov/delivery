package microarch.delivery.adapters.out.postgres;

import lombok.val;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.courier.Courier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import({ CourierRepositoryImpl.class })
class CourierRepositoryImplTest extends BaseJpaTest {

    @Autowired
    private CourierRepositoryImpl courierRepositoryImpl;

    @Autowired
    private CourierJpaRepository courierJpaRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    private static Courier newCourier(final String name) {
        return Courier.create(name, Location.create(1, 1).getValue()).getValue();
    }

    @Test
    void create() {
        val courier = newCourier("courier-create");

        val saved = courierRepositoryImpl.create(courier);

        assertThat(saved.getId()).isEqualTo(courier.getId());
        assertThat(saved.getName()).isEqualTo("courier-create");
        assertThat(saved.getLocation()).isEqualTo(Location.create(1, 1).getValue());
        courierJpaRepository.flush();
        assertThat(courierJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void update() {
        val courier = courierRepositoryImpl.create(newCourier("courier-update"));
        courier.moveUp();

        courierRepositoryImpl.update(courier);
        courierJpaRepository.flush();
        testEntityManager.clear();

        val found = courierJpaRepository.findById(courier.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("courier-update");
        assertThat(found.get().getLocation().getY()).isEqualTo(2);
    }

    @Test
    void findById() {
        val courier = courierRepositoryImpl.create(newCourier("courier-find-by-id"));

        val found = courierRepositoryImpl.findById(courier.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(courier.getId());
        assertThat(found.get().getName()).isEqualTo("courier-find-by-id");
        assertThat(found.get().getLocation()).isEqualTo(Location.create(1, 1).getValue());

        assertThat(courierRepositoryImpl.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void getAll() {
        val couriers = courierJpaRepository
                .saveAllAndFlush(List.of(newCourier("courier-1"), newCourier("courier-2"), newCourier("courier-3")));
        val sortedIds = courierJpaRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream().map(Courier::getId)
                .toList();

        val firstPage = courierRepositoryImpl.getAll(0, 2);

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).extracting(Courier::getId).containsExactly(sortedIds.get(0),
                sortedIds.get(1));

        val secondPage = courierRepositoryImpl.getAll(1, 2);
        assertThat(secondPage.getContent()).extracting(Courier::getId).containsExactly(sortedIds.get(2));
    }
}
