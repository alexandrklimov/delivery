# Kafka Rules

> ℹ️ **APPLICABILITY:** This rule set applies ONLY if a Kafka dependency (`org.apache.kafka:kafka-clients`, `org.springframework.kafka:spring-kafka`, `io.micronaut.kafka:micronaut-kafka`) is present in the **main** project dependencies (`implementation` / `compile` in `build.gradle` or `pom.xml`). Ignore if present only in test scope.

---

## Consumer Deserialization Rule (CRITICAL)

### Rule

Consumer configuration MUST set:
```properties
value.deserializer = org.apache.kafka.common.serialization.StringDeserializer
```

The listener method MUST:
1. **Log the raw string body** (debug or info level) **BEFORE** deserialization
2. **Deserialize manually** to the target Java object inside the listener body
3. **Handle deserialization failures explicitly** — re-throw or route to DLQ; never swallow (see [`code-style-rules.md`](./code-style-rules.md) "Exception Handling - No Swallowing Rule")

### Rationale

Raw-body logging preserves the original payload for debugging when JSON is malformed or schema-drifts. Framework `JsonDeserializer` swallows the raw bytes on failure, making production incidents harder to diagnose.

### Deserialization API by Spring Boot Version

#### Spring Boot ≤ 3.x (Jackson 2)

```java
@KafkaListener(topics = "${app.kafka.topic.my-topic}")
public void listen(String rawPayload) {
    log.debug("Received raw Kafka payload: {}", rawPayload);
    
    try {
        MyDto dto = objectMapper.readValue(rawPayload, MyDto.class);
        processMessage(dto);
    } catch (JsonProcessingException ex) {
        log.error("Failed to deserialize Kafka message. Raw payload: {}", rawPayload, ex);
        throw new MessageProcessingException("Failed to deserialize message", ex);
    }
}
```

**Key points:**
- `ObjectMapper` from `com.fasterxml.jackson.databind`
- `JsonProcessingException` is a **checked exception** — must be caught or declared
- Log the raw payload in the error message for debugging

---

#### Spring Boot ≥ 4.x (Jackson 3)

```java
@KafkaListener(topics = "${app.kafka.topic.my-topic}")
public void listen(String rawPayload) {
    log.debug("Received raw Kafka payload: {}", rawPayload);
    
    // Jackson 3: unchecked exceptions, no forced catch
    MyDto dto = jsonMapper.readValue(rawPayload, MyDto.class);
    processMessage(dto);
}
```

**Key points:**
- `JsonMapper` from `tools.jackson.databind.json`
- `JacksonException` is an **unchecked exception** — catch only if you need custom handling
- Still log the raw payload BEFORE deserialization for debugging

---

### Cross-references

- Exception handling: see [`code-style-rules.md`](./code-style-rules.md) "Exception Handling - No Swallowing Rule"
- Link integrity: per META-RULE 4, no line numbers in links

---

## Producer Idempotency Rule

### Rule

Producers MUST be idempotent:
- `enable.idempotence = true`
- `acks = all`
- `retries > 0` (typically `Integer.MAX_VALUE` for idempotent producers)

**Kafka ≥ 3.0 Note:** Idempotence is enabled by default in Kafka 3.0+. Do **NOT** add redundant `enable.idempotence=true` configuration on Kafka ≥ 3.0 unless your configuration explicitly disables it. Agents should not waste effort re-asserting defaults.

### Rationale

Prevents duplicate messages on broker retries; required for at-least-once delivery guarantees. Idempotent producers guarantee exactly-once semantics within a single producer session.

### Configuration Example

```yaml
spring:
  kafka:
    producer:
      acks: all
      # retries: Integer.MAX_VALUE (or large number) for idempotent producer
      # enable.idempotence: true  # NOT needed on Kafka >= 3.0 (enabled by default)
```

```java
// Programmatic configuration (if not using Spring Boot auto-config)
Map<String, Object> props = new HashMap<>();
props.put(ProducerConfig.ACKS_ALL_CONFIG, "all");
props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
// enable.idempotence is auto-enabled when acks=all + retries > 0 (Kafka >= 3.0)
```

---

## Exception Handling

Listener exceptions MUST be re-thrown or propagated to the error handler / DLQ. **Never caught and logged silently.**

Per the [Exception Handling - No Swallowing Rule](./code-style-rules.md), all failures must be either re-thrown or explicitly returned as failure results. Returning normally from a `void` listener method inside a `catch` block is strictly forbidden.

### Example: Correct Error Propagation

```java
@KafkaListener(topics = "${app.kafka.topic.my-topic}")
public void listen(String rawPayload) {
    log.debug("Received raw Kafka payload: {}", rawPayload);
    
    MyDto dto = jsonMapper.readValue(rawPayload, MyDto.class);
    processMessage(dto);  // If this throws, let it propagate to Spring's error handler
}
```

### Example: WRONG — Exception Swallowed

```java
@KafkaListener(topics = "${app.kafka.topic.my-topic}")
public void listen(String rawPayload) {
    try {
        MyDto dto = jsonMapper.readValue(rawPayload, MyDto.class);
        processMessage(dto);
    } catch (Exception ex) {
        log.error("Failed to process message", ex);
        // ❌ CRITICAL: Method exits normally - error is silently ignored!
        // The message is acknowledged as processed, but processing failed.
    }
}
```

---

## Code Review Checklist for Kafka Code

Before accepting AI-generated Kafka code, verify:

- [ ] Consumer uses `StringDeserializer` for value deserializer
- [ ] Listener logs raw payload BEFORE deserialization
- [ ] Deserialization uses correct Jackson API (Jackson 2 vs 3 based on Spring Boot version)
- [ ] Deserialization exceptions are re-thrown or propagated (not swallowed)
- [ ] Producer config has `acks=all` and `retries > 0`
- [ ] No redundant `enable.idempotence=true` on Kafka ≥ 3.0
- [ ] No fully-qualified class names unless there's an actual naming conflict (per [`code-style-rules.md`](./code-style-rules.md))
