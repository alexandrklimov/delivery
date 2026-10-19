# Testing Approach & JVM Execution Rules

> 🔎 **Scope:** This file defines the **general, project-wide testing approach**: test-type selection, test-layer placement, assertion/Mocking standards, and coverage rules. It does NOT aggregate per-technology test setup — technology-specific testing guidance (e.g., HTTP client mocking in `integration-rules.md`, Kafka container setup in `kafka-rules.md`) lives in the corresponding technology rule files, when those files are deployed for the project. See the "Project Test Infrastructure" section for the list of deployed technology-specific rule files with testing content.

## JVM Environment & Command Execution Strategy

Before executing **ANY** console command that depends on Java/JVM (builds, tests, code generation, migrations, etc.):

1. **Detect Target Java Version:** Inspect project configuration (`pom.xml`, `build.gradle`, or `build.gradle.kts`) to extract the target JDK version string `{V}` (e.g., `8`, `11`, `17`, `21`, `25`, `27`, etc.).
   
2. **Execution Wrapper Rule (Single Shell Instance):**
   Environment variables do NOT persist across separate command calls in the agent runner. Therefore, any JDK switch script MUST be combined inline with the target command using `&&`.

   - **Dynamic Script Resolution:** Substitute `{V}` with the exact version detected in Step 1:
     ```bash
     . ~/.set_jdk{V}_path && <YOUR_COMMAND_HERE>
     ```
     *Template Execution Examples:*
     - `. ~/.set_jdk{V}_path && ./gradlew build`
     - `. ~/.set_jdk{V}_path && ./gradlew test`
     - `. ~/.set_jdk{V}_path && ./mvnw test`

   - **Fallback (If custom scripts are NOT available on host machine):**
     Execute `<YOUR_COMMAND_HERE>` directly, relying on system `JAVA_HOME` or build toolchains.

> ⚠️ **DYNAMIC VERSION RULE:** `{V}` is a variable, NOT a fixed set of options. If the project specifies Java 27, run `. ~/.set_jdk27_path && ...`. Do NOT default to hardcoded version numbers.

---

## Testing Strategy

### Test Type Selection

| Test Type | When to Use | Base Class / Setup | Dependencies |
|-----------|-------------|-------------------|--------------|
| **Unit Test with Mockito** | Simple CRUD operations, pure business logic, isolated component testing, service methods that only call simple repository CRUD operations | `@ExtendWith(MockitoExtension.class)` | `@Mock` for repositories/collaborators, `@InjectMocks` for service OR explicit `new Service(mock)` in `@BeforeEach` |
| **Integration Test with Testcontainers** | Complex database queries/joins, transactional boundaries, event publishing (Kafka, JMS), multiple repository interactions, services with complex business logic involving multiple entities | Project-specific base class (e.g., `BaseJdbcRepoTest`, `AbstractIntegrationTest`) | `@Autowired` for repositories, service, and real beans |

> 💡 **Instantiation Flexibility:** While `@InjectMocks` is standard for simple services, explicit instantiation via constructor in `@BeforeEach` is preferred when set-up requires custom configuration, spy objects, or complex collaborator wiring.

### When to Use Each Test Type

**Use Mockito Unit Test When:**
- ✅ Service method only calls simple repository CRUD operations (`save`, `findById`, `delete`, etc.)
- ✅ No complex database queries are involved
- ✅ No transactional boundaries need to be tested
- ✅ No event publishing logic needs verification
- ✅ Service has a single repository dependency

**Use Integration Test with Testcontainers When:**
- ✅ Service method involves complex queries or joins
- ✅ Transactional boundaries must be tested
- ✅ Event publishing logic (Kafka, JMS, etc.) needs verification
- ✅ Multiple repositories interact in the service method
- ✅ Related/bound entities need to be created/verified

---

### Test Layer Placement Rules [CRITICAL]

Each test scenario belongs to exactly ONE layer based on what the assertion proves. Placing a test in the wrong layer makes it useless — it asserts a fact the layer cannot validate.

| What the assertion proves | Correct layer | Typical Spring annotation / base | Why |
|---------------------------|---------------|-----------------------------------|-----|
| HTTP shape: status code, JSON path, routing (headers/params/path) | Web slice test | `@WebMvcTest` (Spring) / framework web-test equivalent | Web slice only; service deps are stubbed; no DB, no real service logic |
| Service orchestration: which collaborator is called, in what order, with what args | Service unit test | `@ExtendWith(MockitoExtension.class)` + `@Mock` | Mockito `verify(...)` proves orchestration without a DB |
| DB side-effects: rows inserted/deleted, SQL correctness, transaction commit/rollback | DB integration test | extends project base class with Testcontainers (or real DB) | Only a real DB + real Tx manager can prove side-effects |
| End-to-end regression: real server port + real serialization + full context (e.g., migration/regression snapshot tests) | Full-context test (`@SpringBootTest` RANDOM_PORT or equivalent) | Last resort — slices cannot prove the whole HTTP stack; introducing such a test requires explicit user approval |

> **Class naming:** The exact class-name suffixes (`*ControllerIT`, `*ServiceTest`, `*RepositoryIT`, `*InputPortTest`, etc.) are project-specific. Do NOT assume a suffix — discover the project's actual test classes during Step 1 discovery (see generator SKILL.md §"Test Infrastructure") and place each test in the class that matches its layer. If a layer has no test class in the project, create one or ask the user.

**FORBIDDEN combinations (regardless of project naming conventions):**
- ❌ Asserting DB side-effects (rows changed, transaction rollback) inside a web-slice test (`@WebMvcTest` or equivalent). There is no DB and no transaction — the stub returns whatever you declared.
- ❌ Asserting service orchestration (e.g. "remove-then-add sequence") inside a web-slice test that stubs the output port. The stub hides the service; the assertion proves only the stub.
- ❌ Asserting `@Transactional` rollback in any layer EXCEPT the DB-integration layer.

**Decision procedure (apply when planning or writing a test):**
1. What single fact is this assertion proving? (HTTP shape / orchestration / DB state)
2. Which layer owns that fact? (Web / Service / DB)
3. Find the project's test class for that layer (from discovery). If none exists, create one or ask the user.
4. If unsure which layer — ask the user. Do NOT default to "wherever there is already a test file".

---

### Test Value Criteria [CRITICAL]

A mechanically correct test (right layer, right assertions, right factories) can still be useless. Before writing any new test, apply these five criteria:

1. **Value test:** Answer "what breaks in THIS project's code if this test fails?" If the answer is "nothing" (the test can only fail when a framework or tool misbehaves), do not write the test.
2. **No duplicates:** Before adding a test, check existing tests in the same layer for an equivalent assertion (same fact being proven about the same code path). Do not add duplicates. If you find existing duplicates, propose removing or merging them instead of adding more. When adding boundary cases, add the new `@Test` methods to the existing test class instead of creating a new class for them. Do NOT subclass a concrete test class (`XTest extends YTest`) to host new cases — subclassing is reserved for abstract test-infrastructure base classes.
3. **Do not test frameworks or tools:** "Flyway applied the migration", "Jackson serializes JSON", "the column exists in information_schema" are facts about tools, not about the project. Column presence is implicitly proven by any CRUD round-trip test that writes and reads the field.
4. **Stub tautology:** `verify(x).m(req)` immediately after `when(x.m(req)).thenReturn(...)` proves nothing by itself — the stub returns whatever was declared. Keep such a `verify` only when the invocation itself (that the SUT calls the collaborator at all, how many times, with which arguments) is the fact under test.
5. **Constraint-boundary scope:** Every constraint (length, range, format/pattern, nullability, enum membership, uniqueness, ...) is verified by tests ONLY at the layer(s) where it is enforced — incoming DTO validation, DB schema/CHECK constraints (round-trip), or service logic. Layers that pass the value through unchanged (mappers, projections, cache DTOs, JSON serialization) inherit correctness and get NO separate boundary tests; add one only for a layer that itself transforms the value (truncation, normalization, defaulting, coercion).

### Test Data Factory Rules

Rules for authoring or extending test data factories (pass-through wrapper ban, overload escalation policy, assertion redundancy inside factories' tests) live in [`redundancy-rules.md`](./redundancy-rules.md) — refer to that file as the single source of truth.

---

## Mockito Testing Patterns

### Verification Rules

- **Positive Verification:** When a service method calls collaborators, the test MUST verify execution:
  ```java
  verify(repository).deleteById(id);
  ```

- **Negative Verification:** When testing early returns or no-change scenarios, the test MUST assert non-execution:
  ```java
  verify(repository, never()).save(any());
  ```

### Strictness & `lenient()` Rules

- **General Principle:** Do NOT use `lenient()` globally just to silence `UnnecessaryStubbingException` in simple, localized unit tests.

- **Allowed Exceptions for `lenient()`:**
  1. **Shared Setup:** Shared `@BeforeEach` blocks with conditional execution paths.
  2. **Parameterized Tests:** Parameterized test cases with branching internal execution.
  3. **Universal Helper / Factory Stubs:** Helper methods or fixture builders (e.g., `createUniversalServiceStub()`) that setup comprehensive mocks for complex collaborators where individual tests invoke only a subset of configured methods.

> 💡 **Guideline:** When creating reusable stubbing helper methods for complex entities/services, wrap individual `when(...)` calls in `lenient()` to allow flexible method invocation across different test cases without code duplication.

### Argument Selection: Direct Values vs. Matchers (`any()`, `eq()`)

- **Default Choice — Direct Values:**
  Always default to strict, explicit values for predictable inputs (constants, known DTOs, specific IDs). Overusing `any()` in happy-path scenarios masks subtle parameter-passing bugs.

- **When to Use Matchers (`any()`, `eq()`):**
  Use argument matchers ONLY in the following clear scenarios:
  1. **Dynamic / Non-Deterministic Data:** Values generated inside the System Under Test (random UUIDs, `Instant.now()`, cryptographic hashes).
  2. **Exception & Failure Testing:** Scenarios where the stubbed method throws an exception (e.g., `when(service.doWork(any())).thenThrow(new RuntimeException())`). In failure-path tests, the specific input payload is irrelevant to the exception handling behavior under test.
  3. **Irrelevant Context in Multi-Param Calls:** Secondary arguments that do not impact the specific decision branch being tested (e.g., logging metadata or optional flags).

- **No Mixed Arguments:**
  If one argument uses a matcher, ALL arguments for that method invocation must use matchers (wrap direct values in `eq()`).

---

### Anti-pattern: Pass-Through Exception Unit Tests [CRITICAL]

A unit test of the form:
```java
when(collaborator.method(any())).thenThrow(new RuntimeException());
assertThrows(RuntimeException.class, () -> sut.method(...));
```
is **USELESS** when the SUT does NOT transform, wrap, or handle the exception — i.e. the exception propagates through the SUT unchanged.

**Rule:** Write an exception-path unit test ONLY when the SUT does at least ONE of:
- ✅ Catches and transforms/wraps the exception (new type, new message, error-code mapping).
- ✅ Catches it, performs a side-effect (logging, cleanup, fallback), THEN re-throws.
- ✅ Swallows it and returns a sentinel/failure result (assert on the returned result, NOT on `assertThrows`).

**When the SUT is pass-through** (no try/catch, no transformation), the exception path is covered by the integration test layer (`*ControllerIT`) which verifies the global exception handler (`@ControllerAdvice`) maps the exception to the correct HTTP status + body.

**Legacy grandfathering:** Existing tests in the codebase that violate this rule are NOT a reason to replicate the pattern for new methods. New methods without exception-handling logic do NOT get a pass-through exception unit test.

> **Why this matters:** Pass-through exception tests inflate coverage metrics while proving nothing about product behavior — they assert that Java propagates exceptions, which the JVM guarantees by spec.

---

## AssertJ Assertion Rules [PREFERRED: AssertJ]

AssertJ is the project's preferred assertion library. If the touched test files predominantly use another library (JUnit assertions, Hamcrest) or a mix, follow the "Preferred-Rule vs Codebase-Style Conflict Resolution" procedure in `code-style-rules.md`.

### 1. Imports
Always use static imports for `assertThat` and exception helpers:
```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
```

### 2. Fluent API Syntax
Write assertions in a natural-language fluent chain starting with `assertThat(actual)`:
```java
assertThat(actualResult).isEqualTo(expectedResult);
```

### 3. Prefer Type-Specific Assertions
Never use generic `.isEqualTo(true/false)` or check sizes manually if a specialized AssertJ method exists:

| ❌ AVOID | ✅ CORRECT |
|----------|------------|
| `assertThat(list.size()).isEqualTo(3);` | `assertThat(list).hasSize(3);` |
| `assertThat(string.contains("abc")).isTrue();` | `assertThat(string).contains("abc");` |
| `assertThat(optional.isPresent()).isTrue();` | `assertThat(optional).isPresent().hasValue(expected);` |
| `assertThat(map.containsKey("key")).isTrue();` | `assertThat(map).containsKey("key");` |
| `assertThat(collection.isEmpty()).isTrue();` | `assertThat(collection).isEmpty();` |

### 4. Collection Handling
Leverage AssertJ's power to extract fields and verify collection contents without manual loops:
```java
assertThat(users).extracting(User::getName).containsExactlyInAnyOrder("Alice", "Bob");
assertThat(items).extracting(Item::getId).containsOnly(expectedIds);
```

### 5. Field-by-Field Object Comparison
If domain objects do not override `.equals()`, or if you need a deep structural check, use recursive comparison:
```java
assertThat(actualUser).usingRecursiveComparison().isEqualTo(expectedUser);
assertThat(actualDto).usingRecursiveComparison().ignoringFields("updatedAt").isEqualTo(expectedDto);
```

### 6. Exception Testing
Always use `assertThatThrownBy` for readable exception verifications. Avoid try-catch blocks or JUnit `@Test(expected = ...)` annotations:
```java
assertThatThrownBy(() -> service.process(null))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("ID required");

assertThatThrownBy(() -> service.validate(input))
    .isInstanceOf(RuntimeException.class)
    .hasMessage("Validation failed");
```

### 7. Soft Assertions
When a single test requires multiple independent validations that should all execute even if one fails, use `SoftAssertions.assertSoftly`:
```java
SoftAssertions.assertSoftly(softly -> {
    softly.assertThat(response.getStatusCode()).isEqualTo(200);
    softly.assertThat(response.getBody()).isNotNull();
    softly.assertThat(response.getHeaders()).containsKey("X-Custom-Header");
});
```

---

## Entity Stub Creation

**CRITICAL**: Do NOT use static factory methods like `Entity.createNew()` to create stub entities that represent **already-persisted** data.

**Rationale:** Static factory methods are designed for service-layer entity creation BEFORE persistence. They intentionally omit system-generated fields (`id`, `createdAt`, `updatedAt`) because these are populated by the database/entity listeners during persistence.

**Correct Pattern for Persisted Entity Stubs:**
```java
// NOTE: Example uses explicit types to stay neutral. Before writing code, apply the
// "Preferred-Rule vs Codebase-Style Conflict Resolution" from code-style-rules.md to decide
// between `val`, `var`, and explicit types for THIS project/task.
ZonedDateTime now = ZonedDateTime.now();
EntityEntity entity = new EntityEntity(UUID.randomUUID(), reqDto.code(), now);  // Without updatedAt
EntityEntity entity = new EntityEntity(UUID.randomUUID(), reqDto.code(), now, now);  // With updatedAt
```

**When creating entity stubs representing persisted data, MUST include all system-generated fields.**

---

## External API & Dependency Inspection Protocol

When writing tests or code that uses external API classes (from JAR dependencies not in the local workspace):

1. **Standard Usage:**
   Write code using expected getters/setters/records based on domain context or existing project usages/factories.

2. **On Compilation Failure or Missing Signatures (Bytecode First):**
   If compilation fails due to `cannot find symbol` (or if you are unsure of the exact record/Lombok getter syntax), you MUST inspect the class bytecode using `javap` before making code changes:
   ```bash
   javap -cp $(find ~/.gradle ~/.m2 -name "dependency-name-*.jar" 2>/dev/null | head -n 1) com.example.package.TargetClass
   ```

3. **No Trial-and-Error Loops:**
   Do NOT attempt multiple back-to-back edits guessing different method names (`.getFileId()`, then `.getDocumentId()`). Always inspect exact signatures via `javap` first.

4. **Escalation Boundary:**
   Ask the user for signature details ONLY if the JAR file is physically not present in the local cache (`~/.gradle` or `~/.m2`) and cannot be resolved by the build system.

5. **Use Existing Test Factories:**
   Leverage existing test helper/factory classes (e.g., `ModelFactory`, `TestDataBuilder`, `ObjectMother`) that already construct external DTOs correctly.

6. **Reference Existing Test Files:**
   Look at how external API classes are instantiated and accessed in existing tests within the project.

7. **Use Mocks for Unknown APIs:**
   When accessor methods are unknown, mock the external dependencies instead of constructing real instances.

---

## Test Coverage of New Logic [CRITICAL]

When a task adds new business logic, the test plan MUST cover the layer where that logic lives — not just the entry point (controller).

**Coverage matrix (apply during planning):**

| Where new logic is added | Required test layer(s) | Example |
|--------------------------|------------------------|---------|
| Controller only (new routing, new mapping, delegates to existing service) | Web slice test | New `@PostMapping` that delegates to an existing service method |
| Service/InputPort (branching, grouping, orchestration, new collaborator calls) | Service unit test **+** Web slice test (HTTP contract) | New service method that groups requests and calls multiple collaborators |
| Repository (new SQL, new upsert, transactional boundary) | DB integration test | New delete-all-then-insert upsert |

> **Class naming:** Replace "Web slice test", "Service unit test", "DB integration test" with the project's actual test-class names discovered during Step 1 discovery.

**Common pitfall:** A task adds logic to a service method (e.g. grouping by key before calling the repository), but the test plan only lists a controller test. The service logic is then UNTESTED. The service unit test is mandatory in this case, because the controller test (with stubbed service) cannot see the new branching.

**Planning checklist:**
- [ ] For each new method/class, identify the layer that contains the new logic.
- [ ] Add at least one test in that layer's test class.
- [ ] If that layer has no test class yet, either create one or ask the user.
- [ ] Verify no test-case in the plan proves a DB-side-effect via a stubbed web-slice test (see "Test Layer Placement Rules").

---

## Project Test Infrastructure

> **Note:** This section is project-specific; it was filled during AGENTS.md generation from actual repository inspection.

### Test Base Classes

- **`BaseJpaTest`** — `src/test/java/microarch/delivery/adapters/out/postgres/BaseJpaTest.java`
  - Class-level annotations: `@DataJpaTest`, `@Testcontainers`, `@AutoConfigureTestDatabase(replace = Replace.NONE)`
  - Starts `PostgreSQLContainer<>("postgres:17")` as a static `@Container` wired via `@ServiceConnection`
  - Purpose: DB-integration base for repository adapter tests (real Postgres, real transactions, rollback per test)
  - Usage pattern: `class OrderRepositoryImplTest extends BaseJpaTest { @Autowired OrderRepositoryImpl orderRepositoryImpl; ... }`

### Test Data Factories

No centralized test data factories; test inputs are inline literals or small `private static` helper methods inside test classes (e.g., `newOrder()`, `courierAt(x, y)` in `*HandlerImplTest` classes). Follow this inline-helper convention for new tests.

### Fixture Conventions

No on-disk fixture convention (`src/test/resources` does not exist); inputs are inline.

### Layer → Class Mapping

| Layer | Project's actual class naming | Notes |
|-------|------------------------------|-------|
| Web slice test | — | **GAP** (see below) |
| Service unit test | `*Test` with `@ExtendWith(MockitoExtension.class)`: `*HandlerImplTest` (application-layer handlers), `OrderAssignServiceImplTest` (domain service), domain model `*Test` (`CourierTest`, `OrderTest`, `AssignmentTest`, `LocationTest`, `VolumeTest`, `DistanceTest`), command-record validation `*CommandTest` | Ports are `@Mock`ed; handlers constructed explicitly in `@BeforeEach` |
| DB integration test | `*RepositoryImplTest` extending `BaseJpaTest` (`CourierRepositoryImplTest`, `OrderRepositoryImplTest`) | Real Postgres via Testcontainers; requires Docker |

### GAPs [CRITICAL]

- **Web-slice layer GAP:** no `@WebMvcTest` (or equivalent) test classes exist — the project has no hand-written controllers yet (only OpenAPI-generated interfaces and a `@RestControllerAdvice`). When the first inbound HTTP adapter appears, create a web-slice test class (e.g., `*ControllerTest`) or ask the user how to proceed.
