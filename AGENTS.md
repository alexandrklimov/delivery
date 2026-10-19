# Agent Context: delivery

## Table of Contents
- [🎯 System Purpose](#system-purpose)
- [🛠️ Tech Stack & Profile](#tech-stack--profile)
- [📜 Documentation Governance](#documentation-governance)
- [🏗️ Architecture & Packages Map](#architecture--packages-map)
- [🚦 AI Coding Rules](#ai-coding-rules)
- [📚 Supplementary Documentation](#supplementary-documentation)

---

## System Purpose
Educational microservice for a courier-delivery domain — the reference project of the "Domain Driven Design and Clean Architecture in Java" course (microarch.ru). It manages orders and couriers, dispatches each new order to the most suitable courier, moves couriers step-by-step across a bounded grid field, and completes orders when a courier reaches the delivery point. The application layer is exposed as commands and queries (CQRS-style) over ports & adapters; domain models never leave the Use Case layer. Inbound HTTP/gRPC/Kafka adapters are planned for later course modules — currently the codebase covers the domain, the application layer, and Postgres output adapters.

## Tech Stack & Profile
- **Profile:** SPRING_BOOT
- **Java Version:** Java 21 Baseline
- **Build Tool:** Maven (single module)
- **Key Modules:** Spring Boot Web, Spring Data JPA, PostgreSQL, spring-kafka, gRPC + Protobuf, MapStruct, OpenAPI generator (interface-only), Jackson, Testcontainers (tests)

---

## Documentation Governance

**File Location:**
- All documentation MUST live inside `./agentsmd/` or be `AGENTS.md` at the project root. No other locations are permitted.
- Engineering rules → `./agentsmd/rules/`
- Workflow docs (runtime flows) → `./agentsmd/workflows/`
- Guide docs → `./agentsmd/` (root)
- ❌ NEVER create documentation files in any other location — not in the project root, not in `./docs/`, not in any arbitrary directory.
- **Exception — Agent Skills:** skill definitions are agent tooling configuration, NOT project documentation, and are exempt from the location rule above. The preferable, agent-agnostic location is `.agents/skills/`; agent-specific directories (`.opencode/skills/`, `.claude/skills/`) are also recognized where they already exist.

**Link Integrity:**
- NEVER use line numbers in links (`File.java#L29-L34`). Use file paths and named symbols only.

**Language:**
- All documentation SHOULD use one language consistently (default: English). The project's `README.md` is intentionally kept in Russian as the course's public entry point.

**Autonomy:**
- This project is self-contained. All rules are deployed in `./agentsmd/rules/`. Do NOT assume external/global rules.

**AGENTS.md Brevity:**
- Keep AGENTS.md under ~1000 lines. Move detail to `./agentsmd/` files.

---

## Architecture & Packages Map
- **Root Package:** `microarch.delivery`
- **Key Packages:**
  - `libs.ddd` — DDD building blocks (`Aggregate`, `AggregateRoot`, `BaseEntity`, `ValueObject`, `DomainEvent`)
  - `libs.errs` — error-handling toolkit (`Result<T,E>`, `Error`, `Guard`, `UnitResult`); domain methods return `Result` instead of throwing
  - `microarch.delivery.core.domain.model` — domain model: aggregates `Courier`, `Order`; entity `Assignment`; value objects `Location`, `Distance`, `Volume`; `OrderStatus`, `Status`
  - `microarch.delivery.core.domain.service` — domain services (`OrderAssignService`: order dispatching)
  - `microarch.delivery.core.application.command` — commands + command handlers: `CreateCourier`, `CreateOrder`, `MoveCourier`, `AssignOrder`, `CompleteOrder` (each: `*Command` record with constructor validation → `*Handler` interface → `*HandlerImpl` with `@Transactional`)
  - `microarch.delivery.core.application.query` — queries + read-model DTOs: `GetAllCouriers`, `GetNotCompletedOrders` (`CourierDTO`, `OrderDTO` expose plain `x`/`y` coordinates — domain types never leak to adapters)
  - `microarch.delivery.core.ports` — output ports (`OrderRepository`, `CourierRepository`)
  - `microarch.delivery.adapters.out.postgres` — JPA adapters (`*JpaRepository` Spring Data interfaces + `*RepositoryImpl`)
  - `microarch.delivery` (root) — `DeliveryApplication`, `GlobalExceptionHandler` (`@RestControllerAdvice`), `ApplicationProperties`, `Constants`; `microarch.delivery.config` — `JacksonConfig`
- **HTTP/REST Convention:** no hand-written controllers yet (only OpenAPI-generated interfaces from `openapi-generator-maven-plugin` and a `@RestControllerAdvice`) — convention TBD; establish one when the first inbound HTTP adapter is written.

---

## AI Coding Rules

### 1. General Java & Style Rules
- **Type Inference `[PREFERRED: Lombok val]`:** Use Lombok `val` for local variables with clear right-hand assignments. This is the project's preferred style. When the existing codebase predominantly uses `var`, explicit types, or a mix, follow the "Preferred-Rule vs Codebase-Style Conflict Resolution" procedure in [`code-style-rules.md`](agentsmd/rules/code-style-rules.md). Do NOT assume the only alternatives are `val` and `var`.
- **Lombok Guardrail:** Inspect return types before generating logic when reading existing code with `val`; do not guess types.
- **Immutability & Records:** Use Java Records for DTOs/Requests/Responses. Use standard classes for Entities (never Records for DB entities).
- **Clean Code:** No unused imports, fully-qualified class names in method bodies, or dead helper methods.
- **JavaDoc:** All non-trivial methods MUST have concise but meaningful JavaDoc — **including all `@Test` methods**. All created/modified classes, interfaces, records, and enums MUST have a class-level JavaDoc (added if missing). See [`code-style-rules.md`](agentsmd/rules/code-style-rules.md) "JavaDoc Comments" and "Source Comment Language" **(read fully before coding — this rule is frequently under-applied)**.
- **Trivial vs Non-Trivial Methods (Project Examples):** Canonical classifications for this codebase:
  - ✅ Trivial (no JavaDoc required): `OrderRepositoryImpl#getAnyInCreatedStatus` — short delegation/lookup.
  - ❌ Non-trivial (JavaDoc required): `Courier#completeAssignment`, `OrderAssignServiceImpl#assign` — branching, orchestration, or side effects.
  - When unsure, classify as **non-trivial** (write the JavaDoc).

### 2. Framework & Layering Rules
- **Dependency Injection:** Mandatory constructor injection via `@RequiredArgsConstructor` on `final` fields only. `@Autowired` on fields is strictly forbidden.
- **Integration Wrappers:** External HTTP clients (Feign, RestClient, WebClient, or any HTTP library) should be wrapped in internal `*ClientService`/`*Gateway` classes for error mapping and logging. Apply this pattern to new code by default, even when the project's existing code does not consistently follow it. The user may explicitly disable this rule for a given task.

### 3. Documentation Discipline
- **Accuracy:** Every class/method/return-type in a doc code block MUST `grep` to a real match. Config values MUST match source literally. Status/rollout/deprecation claims are FORBIDDEN unless quoted verbatim from the ticket. See [`doc-accuracy-rules.md`](agentsmd/rules/doc-accuracy-rules.md) **(read fully before editing any doc — this rule is frequently violated by AI agents)**.
- **Updates on Code Change:** Minimal-diff principle — touch only the invalidated section. Single source of truth — link, don't duplicate. Scope discipline — doc changes must map to the task scope.

---

## Supplementary Documentation
Refer to active project guides in `./agentsmd/rules/`:
- 🧪 [Testing & JVM Execution Rules](./agentsmd/rules/testing-approach-rules.md)
- 🎨 [Code Style Rules](./agentsmd/rules/code-style-rules.md) — Naming, Lombok, final params, comment language, JavaDoc (methods & entities), exception handling, parameter validation, dead code
- 📄 [Documentation Accuracy Rules](./agentsmd/rules/doc-accuracy-rules.md) — Anti-fabrication, doc-update-on-code-change protocol, pre-completion verification gate
- 🔄 [Workflow Documentation Rules](./agentsmd/rules/workflow-rules.md)
- 🔌 [Integration Rules](./agentsmd/rules/integration-rules.md)
- 🐘 [PostgreSQL Indexing Rules](./agentsmd/rules/postgres-rules.md)
- 📨 [Kafka Rules](./agentsmd/rules/kafka-rules.md)
- 🗺️ [MapStruct Rules](./agentsmd/rules/mapstruct-rules.md)

### Agent Skills
No project-local skills yet — create them under `.agents/skills/<name>/SKILL.md`.
