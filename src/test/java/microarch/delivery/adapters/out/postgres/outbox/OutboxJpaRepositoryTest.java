package microarch.delivery.adapters.out.postgres.outbox;

import lombok.val;
import microarch.delivery.adapters.out.postgres.BaseJpaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxJpaRepositoryTest extends BaseJpaTest {

    @Autowired
    private OutboxJpaRepository jpa;

    /**
     * Builds one unprocessed outbox record.
     */
    private OutboxMessage newMessage() {
        return new OutboxMessage(UUID.randomUUID(), "some.Event", UUID.randomUUID().toString(), "Order",
                "{\"orderId\":\"" + UUID.randomUUID() + "\"}", Instant.now());
    }

    /**
     * Verifies that only messages with an empty {@code processedOnUtc} are returned by {@code findUnprocessedMessages}.
     */
    @Test
    void findUnprocessedMessagesReturnsOnlyPending() {
        val pending = jpa.save(newMessage());
        val processed = newMessage();
        processed.markAsProcessed();
        jpa.save(processed);
        jpa.flush();

        val found = jpa.findUnprocessedMessages();

        assertThat(found).extracting(OutboxMessage::getId).containsExactly(pending.getId());
    }

    /**
     * Verifies that a processed message disappears from the unprocessed selection after saving.
     */
    @Test
    void markAsProcessedRemovesMessageFromSelection() {
        val message = jpa.save(newMessage());
        jpa.flush();

        message.markAsProcessed();
        jpa.save(message);
        jpa.flush();

        assertThat(jpa.findUnprocessedMessages()).isEmpty();
    }
}
