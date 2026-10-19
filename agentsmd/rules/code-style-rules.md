# Code Style Rules

## Preferred-Rule vs Codebase-Style Conflict Resolution [CRITICAL]

Many rules in this document state a PREFERRED style (marked with the `[PREFERRED]` tag).
A preferred rule represents the intended direction for NEW code, but it may conflict with the style
actually used in the existing codebase of a specific project — including styles NOT anticipated by the rule.

### General Procedure (applies to ANY `[PREFERRED]` rule)

When a task touches existing files, the agent MUST, before writing code:

1. **Identify the relevant `[PREFERRED]` rule(s)** for the constructs being added/modified
   (type inference, assertion lib, DI style, DTO form, final params, logging, etc.).

2. **Sample the dominant style** for EACH such rule in the files touched by the current task
   (scope: the files this task modifies, plus their immediate package neighbours).
   Count occurrences of each distinct observed style.

3. **Classify the result:**
   - **CONSISTENT** — the dominant observed style matches the preferred rule. Apply the preferred rule. No user interaction needed.
   - **CONFLICT** — the dominant observed style differs from the preferred rule. This includes the case where the codebase uses a style the rule does not enumerate (example: a `val`-preferred rule, but the code uses explicit types — neither `val` nor `var`). Surface the conflict to the user with:
     - The preferred rule (quote it).
     - The observed dominant style + counts.
     - Any minority styles + counts.
     - Ask: "For THIS task, follow the preferred rule, the dominant codebase style, or another option?"
   - **NO DATA / empty files** — no existing style to sample. Apply the preferred rule. No user interaction.
   - **MIXED with no clear majority** — surface as CONFLICT (don't pick arbitrarily).

4. **Apply the chosen style consistently** within the task's scope. Do NOT mix styles inside a single file/method/class unless the user explicitly approves a gradual migration.

5. **Do NOT silently pick** the preferred rule when it conflicts with observed code. The user is the tie-breaker for style conflicts.

### Scope note

- This procedure applies ONLY to rules tagged `[PREFERRED]` in this document.
- Rules tagged `[MUST]` (without "by default") are hard requirements (e.g. "never swallow exceptions"). A codebase that violates them has debt, not an alternative style — fix the violation per the rule.

## Naming Conventions

- **Package naming**: Use the project's root package (e.g., `com.example.service`)
- **Service classes**: End with `Service` suffix
- **Controller/Endpoint classes**: End with `Controller`, `Endpoint`, or `Gateway` suffix
- **DTO classes**: Follow internal API module conventions
- **Repository interfaces**: End with `Repository` suffix

## Lombok Usage

- `@Value`: Immutable data classes
- `@Builder(toBuilder = true)`: Builder pattern with modification support
- `@RequiredArgsConstructor`: Constructor injection for services/controllers
- `@Slf4j`: Logging in service/controller classes

## Logging in Class Hierarchies

`@Slf4j` remains the default for a concrete class with no logger-bearing base. When a logger must be available to subclasses, one logger per hierarchy applies:

- **Abstract base class** `[PREFERRED]`: declare an instance logger — `protected final Logger log = LoggerFactory.getLogger(getClass());` — so every subclass logs under its own runtime class name (a static `@Slf4j` logger would log all subclasses under the base class name).
- **Concrete subclass of such a base**: inherit the base's logger — do NOT add `@Slf4j`; the generated field would shadow the inherited `log` and duplicate the logger.

**On conflict** (touched files already use a different variant): do NOT silently pick — ask the user which variant they prefer for the subclass-case, describing the consequence above (one shared base-class logger name vs per-subclass logger names). The user is the tie-breaker.

## Constructor Injection Exemption

**Dependency-free MapStruct mappers** (no injected collaborators) MAY use static utility-class access via `Mappers.getMapper()` instead of constructor injection. See `mapstruct-rules.md` (if deployed) for the full boundary rule. This is the **sole exemption**; all other classes require constructor injection via `@RequiredArgsConstructor` on `final` fields only.

## Code Style Guidelines

- **Local Variable Type Inference `[PREFERRED: Lombok val]`**: For local variables with clear right-hand assignments, Lombok `val` is the project's preferred style for new code. If the touched files predominantly use `var`, explicit types, or a mix, follow the "Preferred-Rule vs Codebase-Style Conflict Resolution" procedure above. Do NOT assume the only alternatives are `val` and `var` — explicit types count as a distinct style too.
- **Line Length**: Stay within the project's configured limit
- **Bracket Style**: Avoid excessive consecutive brackets (>2)
- **Java Record usage `[PREFERRED: Records for DTO/domain]`**: `Java Record` SHOULD be used for DTO and domain classes; `Java Record` MUST NOT be used for table-mapped database entities. If the touched files predominantly use Lombok `@Value`, plain classes, or a mix, follow the "Preferred-Rule vs Codebase-Style Conflict Resolution" procedure above.

## Import Management

**CRITICAL**: When writing test code or any Java code:

1. **ALWAYS add proper imports** at the top of the file instead of using fully qualified names
2. **NEVER use fully qualified class names** unless there's an actual naming conflict (same simple name from different packages)
3. **Check for existing imports** before adding new ones to avoid duplicates
4. **Different class names are NOT conflicts** - `RequestDTO` and `Request`, or `ResponseDTO` and `Response` can all be imported simultaneously

**Example of CORRECT approach:**
```java
// Add both imports at the top - different names, no conflict
import com.example.internal.dto.UserDTO;
import com.example.web.api.User;

// Then use simple names in code
UserDTO internalUser = new UserDTO();
User webUser = new User();
```

**WRONG approach to avoid:**
```java
// Don't do this when imports would work fine:
com.example.internal.dto.UserDTO user = new com.example.internal.dto.UserDTO();
```

**When fully qualified names ARE acceptable:**
- Actual naming conflicts (e.g., `java.util.List` vs `com.example.List`)
- One-time usage in comments or javadoc
- Temporary code during refactoring (must be fixed before commit)

## Code Quality: Avoid Dead Code

**CRITICAL**: All code should be purposeful and used. This principle applies to **ALL code** (production and test):

1. **No unused methods** - Every method (public or private) should be called by at least one other method
2. **No unnecessary overloads** - Don't create method overloads unless each is actually used
3. **No helper chains** - Avoid helper methods that only call other helpers without being used directly
4. **Regular cleanup** - Remove dead code discovered during development

**Applies to:**
- ✅ Production code (services, utilities, controllers, repositories)
- ✅ Test code (helpers, fixtures, test data builders, mock factories)
- ✅ Configuration and setup code
- ✅ DTOs and domain models

**Exception:**
- Methods intended for future use MUST have a TODO comment with a ticket/issue reference
- API methods required by interface contracts (even if not called internally)
- Override methods required by parent classes or interfaces

**Example of CORRECT approach:**
```java
// Single comprehensive helper with all parameters - used by multiple tests
private TestDTO buildTestDTO(String id, String name, LocalDate date) {
    return new TestDTO(id, name, date);
}

@Test
void testScenario1() {
    TestDTO dto = buildTestDTO("id1", "name1", LocalDate.now());
    // ...
}

@Test
void testScenario2() {
    TestDTO dto = buildTestDTO("id2", "name2", LocalDate.now().minusDays(1));
    // ...
}
```

**WRONG approach to avoid:**
```java
// Comprehensive helper
private TestDTO buildTestDTO(String id, String name, LocalDate date) {
    return new TestDTO(id, name, date);
}

// This overload is ONLY called by the method above, never by actual tests - REMOVE IT
private TestDTO buildTestDTO(String id, String name) {
    return buildTestDTO(id, name, LocalDate.now());
}
```

## Source Comment Language

**CRITICAL**: All comments (`//`, `/* */`) and JavaDoc (`/** */`) in `.java` source files MUST be written in a single, consistent language across the codebase.

### Default
- The default comment language is **English**.

### User Override
- If the user explicitly requests a different comment language for a given task (e.g., "write comments in Russian"), the agent MUST honor that request for the duration of that task.
- The override applies **only** to the work performed in that task; it does not change the project-wide default and MUST NOT be applied retroactively to existing comments written in the default language.
- Mixed-language comments within a single file are forbidden — pick one language per file and stay consistent.

### Rationale
- English as the default keeps the codebase grep-able and reviewable across international teams.
- Allowing an explicit per-task override avoids forcing English when the team legitimately works in another language, while preventing silent drift into mixed-language comment graphs.

## JavaDoc Comments

**CRITICAL**: All non-trivial methods MUST have concise but meaningful JavaDoc comments.

### Entities (Classes, Interfaces, Records, Enums)

Any **class, interface, record, or enum** that an agent creates or modifies MUST carry a concise class-level JavaDoc describing its responsibility within the system.

- **New entity (created by an agent):** MUST have a class-level JavaDoc from the moment it is introduced.
- **Existing entity with NO JavaDoc that an agent modifies:** the agent MUST add a class-level JavaDoc as part of the change. This is mandatory and not subject to judgment.
- **Existing entity that already has a JavaDoc and is modified by an agent:** the agent MUST decide whether the responsibility statement still holds. Ask: *"After my change, does the current class-level JavaDoc still accurately describe what this class does?"* If the answer is no, update the JavaDoc. This is a judgment call — see the checklist attestation at the end of this file.

**What a class-level JavaDoc should contain:**
- ✅ The entity's single-sentence responsibility (the "what").
- ✅ For services/processors: the layer or workflow it participates in, with `{@link}` to collaborators where useful.
- ✅ For DTOs/records: the business concept they represent.
- ❌ Restating the class name (e.g., `/** Board service. */` for `BoardService`).
- ❌ Implementation details that belong on methods.

**Example:**
```java
/**
 * Manages {@link BoardEntity} lifecycle: lookup, update, and propagation of
 * cache-invalidation events to dependent Broker Product Lvl2 entries.
 */
@Service
@RequiredArgsConstructor
public class BoardService { ... }
```

### Test Methods

Each test method MUST have a JavaDoc comment describing:
- The scenario being tested
- The expected behavior or validation
- Key methods/classes under test using `{@link}` references

**Example:**
```java
/**
 * Verifies that {@code getUserInfos()} returns empty set when user has no organizations.
 * Tests the early return scenario when {@link OrganizationService#getUserOrganizations()} returns empty list.
 */
@Test
void getUserInfos_shouldReturnEmptySetWhenNoOrganizations() {
    // ...
}
```

### Production Code Methods

Public and package-private methods MUST have JavaDoc with:
- **Purpose**: Clear description of what the method does
- **Process**: Step-by-step explanation using `<ul>` or `<ol>` lists for complex logic (see "JavaDoc Text Formatting" below)
- **Parameters**: `@param` tags for all parameters with descriptions
- **Return values**: `@return` tag describing what is returned (the inline `{@return}` tag is also valid on Java 18+)
- **Exceptions**: `@throws` tags documenting when exceptions are thrown
- **Cross-references**: `@see` tags linking to related classes and methods

**Example:**
```java
/**
 * Builds {@link UserInfos} for a user by merging data from multiple sources.
 * <p>
 * This method transforms a list of {@link IntermediateMergedData} objects into
 * a {@link UserInfos} response containing user information grouped by ID.
 * Each merged data item is converted to {@link UserInfoResp} using {@link #mapToUserInfoResp}.
 * </p>
 *
 * @param userId User ID to build information for. Must not be null or blank.
 * @param intermediateMergedData List of intermediate merged data objects containing information
 *                                  from multiple sources for each user. Must not be null or empty.
 * @return {@link UserInfos} containing the user ID and a set of user information objects.
 * @throws IllegalArgumentException if userId is blank or intermediateMergedData is empty
 * @see UserInfoResp
 * @see UserInfos
 */
public static UserInfos buildUserInfos(...) {
    // ...
}
```

### JavaDoc Text Formatting

These rules prevent JavaDoc from becoming an unreadable "wall of text". They apply to every new or modified JavaDoc block (class, method, field). They target Java 21+ toolchains.

1. **First-sentence summary (SHOULD, not MUST)** — the first sentence SHOULD be a single, short, high-level summary containing a clear verb phrase. A scannable label prefix is acceptable: `"Event entry point: locks the pipeline …"`. FORBIDDEN in any first sentence: run-on multi-clause sentences (split with `<p>`/lists instead) and abbreviations containing periods (`e.g.`, `i.e.`) — the JavaDoc parser truncates the summary at the first period. Do not start with filler ("This method allows …").
2. **Paragraphs** — separate distinct logical thoughts into `<p>` paragraphs; place the empty `<p>` tag on its own line before the new text block.
3. **Lists** — never write continuous prose for multi-step instructions, preconditions, or case variations: use `<ul>`/`<ol>` with `<li>` items.
4. **Inline code** — wrap identifiers, method signatures, literal values (`null`, `true`, `false`), and generic types in `{@code …}` (e.g. `{@code List<String>}`); never hand-encode `&lt;`/`&gt;`.
5. **Code examples** — wrap multi-line examples in `<pre>{@code …}</pre>` to preserve indentation; on Java 18+ `@snippet` may be used instead where richer rendering is warranted.
6. **Block tags** — leave one blank comment line between the description and the first block tag; order tags consistently: `@param` (every parameter) → `@return` (every non-void method) → `@throws` (checked and critical runtime) → `@see` → `@since`.
7. **Hard wrapping** — wrap all JavaDoc text lines at 120 characters maximum; do not stretch lines indefinitely (readability in code review and diffs).

**Quick reference:**
```java
/**
 * Scheduler entry point: locks one arbitrary pipeline in a pickup status.
 * <p>
 * The routine follows these steps:
 * <ul>
 *   <li>locks one row with {@code FOR UPDATE SKIP LOCKED};</li>
 *   <li>drives its transitions until the status leaves the pickup set;</li>
 *   <li>rethrows transition failures, rolling the pass back.</li>
 * </ul>
 *
 * @param pickupStatuses statuses the row must still be in
 */
```

### Exceptions (No JavaDoc Required)

JavaDoc is NOT required for:
- ❌ Simple getter/setter methods
- ❌ Record compact constructor validation logic
- ❌ Private helper methods in test classes (test data builders, mock factories, fixture methods)
- ❌ Trivial one-line utility methods

### Project Classification Examples (per-project)

Canonical trivial/non-trivial classifications of THIS project, confirmed by the user at generation time (Step 1 "JavaDoc Example Candidates"). Fill this section with the project's own examples and keep it beside the JavaDoc rule it calibrates; update it when a canonized example moves or its JavaDoc changes. When unsure, classify as **non-trivial** (write the JavaDoc).

### JavaDoc Quality Guidelines

**DO:**
- ✅ Keep comments concise but meaningful
- ✅ Use `{@code}` for inline code references
- ✅ Use `{@link}` for class/method references
- ✅ Explain the "why" not just the "what" for complex logic
- ✅ Document validation rules and business constraints

**DON'T:**
- ❌ State the obvious (e.g., "Gets the name" for `getName()`)
- ❌ Repeat what the code already clearly shows
- ❌ Write novel-length explanations for simple methods
- ❌ Forget to update JavaDoc when method signature changes

## Parameter Validation Rules

When validating method parameters, record components, or constructor arguments, follow these rules based on Jakarta Validation annotations:

**Rule 1: Match validation to annotation semantics**

| Annotation | Type | Validation Pattern | Error Message |
|------------|------|-------------------|---------------|
| `@NotBlank` | String | `if (StringUtils.isBlank(arg))` | `"argName must not be null or blank"` |
| `@NotNull` | Any | `Objects.requireNonNull(arg, "message")` | `"argName must not be null"` |
| `@NotEmpty` | Collection/Array/String | `requireNonNull()` + `.isEmpty()` check | `"argName must not be empty"` |

**Rule 2: Never combine redundant checks**

❌ **WRONG** - `StringUtils.isBlank()` already handles null:
```java
Objects.requireNonNull(userId, "userId must not be null");
if (StringUtils.isBlank(userId)) {
    throw new IllegalArgumentException("userId must not be blank");
}
```

✅ **CORRECT** - Single check for `@NotBlank`:
```java
if (StringUtils.isBlank(userId)) {
    throw new IllegalArgumentException("userId must not be null or blank");
}
```

**Rule 3: Validation examples by context**

**Record compact constructor:**
```java
public record MyRecord(@NotBlank String name, @NotNull UUID id, @NotEmpty Set<String> items) {
    public MyRecord {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name must not be null or blank");
        }
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(items, "items must not be null");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
    }
}
```

**Method parameter validation:**
```java
public void processData(String userId, UUID recordId) {
    if (StringUtils.isBlank(userId)) {
        throw new IllegalArgumentException("userId must not be blank");
    }
    Objects.requireNonNull(recordId, "recordId must not be null");
    // ... business logic
}
```

**Regular class constructor:**
```java
public class UserInfo {
    public UserInfo(User user, Set<String> permissions) {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(permissions, "permissions must not be null");
        if (permissions.isEmpty()) {
            throw new IllegalArgumentException("permissions must not be empty");
        }
    }
}
```

**Rule 4: Multiple annotations - apply the strictest validation**

```java
// @NotBlank + @Pattern - use isBlank() first, then pattern validation
public record ProductCode(@NotBlank @Pattern(regexp = "[A-Z]+") String code) {
    public ProductCode {
        if (StringUtils.isBlank(code)) {
            throw new IllegalArgumentException("code must not be null or blank");
        }
        if (!code.matches("[A-Z]+")) {
            throw new IllegalArgumentException("code must match pattern [A-Z]+");
        }
    }
}
```

**Rule 5: Error message format**
- Use lowercase after argument name: `"argName must not be null"` (not `"ArgName"`)
- Be specific: `"userId must not be blank"` is better than `"invalid argument"`
- Include context when helpful

## Enforce Final Method Parameters [PREFERRED]

**CRITICAL**: All method parameters SHOULD be declared as `final` to ensure reference immutability and prevent accidental reassignment. This is the project's preferred style. If the touched files predominantly omit `final` on parameters or use a mix, follow the "Preferred-Rule vs Codebase-Style Conflict Resolution" procedure at the top of this file.

### Rationale
- Prevents accidental parameter reassignment within the method body.
- Makes method contracts explicit and self-documenting.
- Improves code readability and maintainability.
- Aligns with functional programming principles.
- *Note: `final` only guarantees that the reference cannot be changed, not that the object itself is immutable.*

### Examples

**✅ CORRECT — All parameters declared as final:**
```java
public void processFiles(
    final String userId,
    final UUID operationId,
    final String authUserId,
    final List<FileRequest.SignedFile> files
) {
    // Parameters cannot be reassigned
}

public String generateHmac(final String macSecret, final String payload) {
    return HEX_FORMAT.formatHex(generateBytesHash(macSecret, payload));
}

// Lambda parameters (when explicitly typed)
List<String> list = Arrays.asList("a", "b");
list.forEach((final String item) -> System.out.println(item));
```

**❌ WRONG — Parameters without final modifier:**
```java
private String buildMessageBody(
    String userId,  // Missing final
    String orgName,  // Missing final
    List<ErrorCode> errorCodes  // Missing final
) {
    // Dangerous: parameters can be accidentally reassigned
}
```

### Applicability
- ✅ Public, protected, and private methods.
- ✅ Static and instance methods.
- ✅ Constructors (parameters should be final).
- ✅ Lambda parameters (when explicitly typed, e.g., `(final String x) -> {...}`).
- ✅ Overridden methods (adding `final` to parameters in subclass methods is valid and mandatory, as it does not affect the method signature defined in the parent class/interface).
- ❌ **Exception**: Java Records (parameters in compact, canonical, or custom record constructors are implicitly final or handled by the language design).

> **Note**: This rule applies to ALL methods across production and test code. Consistency in parameter declaration improves overall code quality and prevents subtle bugs related to parameter mutation.

## Exception Handling - No Swallowing Rule

**CRITICAL:** NEVER swallow exceptions or business failure responses. All failures must be either re-thrown or propagated to ensure calling methods are aware of the failure.

### Rule 1: Preserve Stack Traces
When logging exceptions inside a `catch` block, ALWAYS pass the exception object as the last parameter. Never log just the message text.

```java
// ✅ CORRECT: Stack trace preserved
try {
    processData();
} catch (Exception ex) {
    log.error("Failed to process data for id={}", id, ex);
    throw new RuntimeException("Process failed", ex);
}

// ❌ WRONG: Stack trace lost - only message text logged
try {
    processData();
} catch (Exception ex) {
    log.error("Failed to process data for id={}, due to: {}", id, ex.getMessage());
    throw new RuntimeException("Process failed", ex);
}
```

### Rule 2: Never Swallow Exceptions
After catching an exception, you MUST either re-throw it (wrapped in a `RuntimeException` if needed) or explicitly return a failure result. Returning normally from a `void` method inside a `catch` block is strictly forbidden.

```java
// ✅ CORRECT: Exception re-thrown upward
try {
    client.sendData(payload);
} catch (Exception ex) {
    log.error("Failed to send data to {}", target, ex);
    throw new RuntimeException("Data delivery failed", ex);
}

// ❌ WRONG: Exception swallowed - method exits normally as if successful
try {
    client.sendData(payload);
} catch (Exception ex) {
    log.error("Failed to send data to {}", target, ex);
    // Method continues execution or returns normally!
}
```

### Rule 3: Error Responses Must Throw Exceptions
When an API or client returns a response object indicating a failure (e.g., `isError() == true`), you must throw an exception immediately. Do not just log it and let the method complete normally.

```java
// ✅ CORRECT: Exception thrown on business error
Response response = client.sendData(payload);
if (response.isError()) {
    throw new RuntimeException("Failed to send data. Errors: " + response.errors());
}

// ❌ WRONG: Error logged but ignored, method continues
Response response = client.sendData(payload);
if (response.isError()) {
    log.error("Failed to send data to {}, errors={}", target, response.errors());
    // No exception thrown - error is silently ignored!
}
```

### Rationale
* **Debugging:** Lost stack traces make it impossible to identify the root cause of failures in production.
* **Data Consistency:** Swallowed exceptions hide failures from calling code, leading to inconsistent application states.
* **Observability:** Silent failures don't trigger alerts in error monitoring systems (e.g., Sentry, OpenTelemetry).

## Code Review Checklist for AI-generated code

Before declaring a coding task complete, the agent MUST perform a **separate diff-review pass** over its own changes and attest against this checklist (see "Pre-Completion Self-Audit" in [`redundancy-rules.md`](./redundancy-rules.md)).

Before accepting AI-generated code, verify:
- [ ] All necessary imports are present at the top of the file
- [ ] No unnecessary fully qualified class names are used
- [ ] Fully qualified names are ONLY used when there's an actual naming conflict
- [ ] Different class names (e.g., `SomethingDTO` vs `Something`, `ResponseDTO` vs `Response`) are NOT treated as conflicts
- [ ] All helper methods are actually used (no dead code or unused overloads)
- [ ] No helper method exists solely to call another helper method
- [ ] No new factory/helper method is a pure unconditional delegation to another helper without added constraint (see [`redundancy-rules.md`](./redundancy-rules.md))
- [ ] No assertion is subsumed by an equals-based comparison in the same test, unless a comment documents a serialization-pitfall justification (see [`redundancy-rules.md`](./redundancy-rules.md))
- [ ] No factory/helper method overload is added without user-approved justification (see [`redundancy-rules.md`](./redundancy-rules.md))
- [ ] All method parameters are declared `final`
- [ ] No exceptions are swallowed (re-thrown or propagated)
- [ ] JavaDoc present on all non-trivial methods
- [ ] Class-level JavaDoc present on every created/modified class, interface, record, or enum — added if missing, updated if the responsibility statement no longer holds
- [ ] All comments and JavaDoc in touched files use a single consistent language (default English, unless the user requested an override for this task)
