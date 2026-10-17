# Workflow Documentation Rules

## Scope

- Workflow docs describe **runtime flows** of the system (request processing, async roundtrips, cross-service business processes) — not developer procedures.
- **Step-by-step developer procedures** (e.g., "how to add a field to an entity") are **agent skills** and live in skill directories with a `SKILL.md` per skill — preferably `.agents/skills/` (the agent-agnostic location); agent-specific directories (`.opencode/skills/`, `.claude/skills/`) are also recognized where they already exist. They are agent tooling configuration, NOT project documentation, and are NOT governed by this rule file.
- If step-by-step procedure documents are found in `./agentsmd/workflows/` at generation time, flag them to the user and propose converting them to agent skills.

## Location

- All workflow documentation files MUST live in `./agentsmd/workflows/` or a subdirectory thereof.
- ❌ NEVER place workflow docs in the project root, `./docs/`, or directly in `./agentsmd/`.

---

## Discoverability

- Every workflow doc MUST be reachable from `AGENTS.md` — either directly linked, or linked from another doc that `AGENTS.md` references (transitive reachability).
- ❌ NEVER create orphan workflow docs that no other document links to.
- Workflow docs SHOULD cross-reference each other and relevant pattern docs (e.g., `./agentsmd/integration-patterns.md`) to maintain a navigable doc graph.

---

## Language

- Workflow docs SHOULD follow the project's primary documentation language (default: English).
- If the project's documentation uses a different primary language, workflow docs SHOULD follow that convention.
- Domain-specific terms may use the business language regardless.

---

## Structure (Advisory, Not Mandatory)

Workflow docs typically include the following sections. Omit sections that don't apply rather than leaving them empty.

### Suggested Sections

| Section | Purpose | Required? |
|---------|---------|-----------|
| **Business Context** | Trigger conditions, actors, purpose of the workflow | Recommended |
| **Technical Workflow** | Call chain (entry point → service → client/repo), component responsibilities table | Recommended |
| **Data Flow** | Request/response DTOs, field tables, validation rules, data enrichment | Optional |
| **External Systems Interaction** | Contracts with external services (input, output, error handling per system) | Optional |
| **Error Handling** | Failure modes by step, error handling strategy (critical vs non-critical) | Recommended |
| **Security & Authorization** | Auth mechanism, permission checks, organization validation | Optional |
| **Configuration** | Properties, endpoints, constants, default values | Optional |
| **Related Documentation** | Links to architecture docs, pattern docs, related workflows | Recommended |

---

## Link Integrity

Per the project's link integrity rules:
- ❌ NEVER use line numbers in links (e.g., `File.java#L29-L34`).
- ✅ Use file paths and named symbols (`ClassName.java`, `ClassName#methodName`, `CONSTANT_NAME`).

---

## When to Create a Workflow Doc

Create a workflow doc for flows that have non-trivial complexity:

- **Multi-source aggregation**: A request merges data from multiple sources (DB + REST, REST + REST, etc.)
- **Async roundtrips**: A request triggers async processing (Kafka, JMS) and the response depends on correlated completion
- **Branching completion**: A request has multiple completion paths (correlated response OR timeout/poll)
- **Cross-cutting business processes**: Flows that span multiple services or domains where the overall picture isn't obvious from any single class

Do NOT create workflow docs for trivial flows (single-source synchronous CRUD).
