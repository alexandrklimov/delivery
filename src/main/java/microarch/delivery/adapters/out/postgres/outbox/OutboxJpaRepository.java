package microarch.delivery.adapters.out.postgres.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for {@link OutboxMessage} records.
 */
public interface OutboxJpaRepository extends JpaRepository<OutboxMessage, UUID> {

    /**
     * Returns all outbox messages that have not been published to the external transport yet.
     *
     * @return unprocessed outbox messages
     */
    @Query("SELECT o FROM OutboxMessage o WHERE o.processedOnUtc IS NULL")
    List<OutboxMessage> findUnprocessedMessages();
}
