package microarch.delivery.core.ports;

import jakarta.annotation.Nonnull;
import microarch.delivery.core.domain.model.courier.Courier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourierRepository {

    @Nonnull
    Courier create(final @Nonnull Courier courier);

    @Nonnull
    Courier update(final @Nonnull Courier courier);

    @Nonnull
    Optional<Courier> findById(final @Nonnull UUID id);

    @Nonnull
    List<Courier> getAll();
}
