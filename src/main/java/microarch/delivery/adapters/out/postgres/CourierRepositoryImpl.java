package microarch.delivery.adapters.out.postgres;

import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.ports.CourierRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CourierRepositoryImpl implements CourierRepository {

    private final CourierJpaRepository courierJpaRepository;

    @Nonnull
    @Override
    public Courier create(@Nonnull final Courier courier) {
        return courierJpaRepository.save(courier);
    }

    @Nonnull
    @Override
    public Courier update(@Nonnull final Courier courier) {
        return courierJpaRepository.save(courier);
    }

    @Nonnull
    @Override
    public Optional<Courier> findById(final @Nonnull UUID id) {
        return courierJpaRepository.findById(id);
    }

    @Nonnull
    @Override
    public List<Courier> getAll() {
        return courierJpaRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }
}
