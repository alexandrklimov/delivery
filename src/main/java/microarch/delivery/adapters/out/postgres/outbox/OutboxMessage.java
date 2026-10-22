package microarch.delivery.adapters.out.postgres.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import libs.errs.Error;
import libs.errs.Guard;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Technical outbox record: a serialized domain event persisted in the same transaction as its aggregate and dispatched
 * to the external transport later by the polling job. It is not part of the domain model.
 */
@Entity
@Table(name = "outbox")
@Getter
@NoArgsConstructor
public class OutboxMessage {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "occurred_on_utc", nullable = false)
    private Instant occurredOnUtc;

    @Column(name = "processed_on_utc")
    private Instant processedOnUtc;

    /**
     * Creates an unprocessed outbox record for one domain event.
     *
     * @param id
     *            unique event id used for safe duplicate handling
     * @param eventType
     *            fully qualified domain event class name used for routing and debugging
     * @param aggregateId
     *            id of the aggregate that raised the event
     * @param aggregateType
     *            simple name of the aggregate that raised the event
     * @param payload
     *            serialized domain event (JSON)
     * @param occurredOnUtc
     *            moment the event occurred
     */
    public OutboxMessage(final UUID id, final String eventType, final String aggregateId, final String aggregateType,
            final String payload, final Instant occurredOnUtc) {
        Error.throwIf(Guard.againstNullOrEmpty(id, "id"));
        Error.throwIf(Guard.againstNullOrEmpty(eventType, "eventType"));
        Error.throwIf(Guard.againstNullOrEmpty(aggregateId, "aggregateId"));
        Error.throwIf(Guard.againstNullOrEmpty(aggregateType, "aggregateType"));
        Error.throwIf(Guard.againstNullOrEmpty(payload, "payload"));
        Objects.requireNonNull(occurredOnUtc, "occurredOnUtc must not be null");

        this.id = id;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.payload = payload;
        this.occurredOnUtc = occurredOnUtc;
    }

    /**
     * Marks the message as successfully published to the external transport.
     */
    public void markAsProcessed() {
        this.processedOnUtc = Instant.now();
    }
}
