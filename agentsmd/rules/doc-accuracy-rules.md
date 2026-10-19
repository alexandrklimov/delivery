# Documentation Accuracy & Update Rules

> ℹ️ **APPLICABILITY:** This rule set applies to EVERY project, unconditionally. It governs how AI agents write and edit documentation (in `AGENTS.md`, `./agentsmd/**`, and any other `.md` file) after code changes — and, more importantly, what they are forbidden from asserting.

---

## Core Principle

Documentation is a **claim about the code**. Every claim must be **verifiable from source, configuration, or an explicit user/ticket instruction** at the moment it is written. If a claim cannot be traced to one of those three origins, it is a fabrication and MUST NOT be written.

Weaker reasoning models are especially prone to inventing plausible-sounding but false details (method names, status declarations, config values, migration roadmaps). This file exists to constrain that failure mode with mechanical, checkable rules.

---

## Part A — Anti-Fabrication Rules

### Rule 1: Code Examples Must Be Real

**CRITICAL:** Every class, method, field, annotation, and return type referenced inside a documentation code block (` ```java `) or inline code (` ` `) MUST exist in the codebase exactly as written at the time of writing — in either `src/main/java/` (production code) or `src/test/java/` (test code), whichever the doc context refers to.

**Verification (mandatory before writing any code block):**
```bash
# Production code symbols (services, repositories, entities, controllers):
grep -rn "<symbol>" src/main/java/

# Test code symbols (test methods, fixtures, factories, base classes, mocks):
grep -rn "<symbol>" src/test/java/

# If both return zero matches, the symbol does not exist. Do not invent it.
```

**Test code is first-class documentation content.** Documenting test infrastructure (`ModelFactory`, `BaseJdbcRepoTest`, etc.), `@Test` methods, AssertJ assertions, or "how to test X" patterns all require referencing symbols from `src/test/java/`. Apply the same verification discipline as for production code — no "illustrative snippet" exceptions.

If `grep` returns zero matches, the symbol does not exist. Either find the real name or omit the code block entirely.

❌ **WRONG — inventing a plausible-but-fake API:**
```java
// In a doc, describing how a service "works":
var entity = repository.insert(dto);              // 'insert' does not exist
var createdId = repository.update(dto);            // 'update' does not exist
return converter.toReadDto(entity);                // 'toReadDto' does not exist
```

✅ **CORRECT — verify, then write:**
```bash
grep -rn "insert\|update\|toReadDto" src/main/java/
# If zero matches: do NOT use these names. Open the real service file,
# read the actual method signatures, and use those.
```

**Return types are part of the claim.** If a service method returns a projection record (e.g., `ProductLvl2AllBound`) but the doc says it returns a `ProductLvl2ReadDTO`, the doc is wrong even if the method name is right. Always open the source and read the actual signature.

---

### Rule 2: No Invented Project-Management Facts

**CRITICAL:** The following assertions are **FORBIDDEN** in documentation unless they appear **verbatim** in the triggering Jira ticket, user instruction, or an explicit stakeholder message:

- "Production-Ready" / "actively used in production"
- Migration status trackers (`✅ Active`, `🔄 In Progress`)
- Rollout or adoption percentages ("consumers gradually migrating")
- Deprecation schedules ("will be removed in a future release")
- Consumer-migration state
- "Zero-downtime migration" / "backward-compatible" claims
- "Recommended for all new consumers"

**Why:** These are project-management facts, not engineering facts. An AI agent observing code cannot know whether the feature is rolled out, who consumes it, or when it will be deprecated. Inventing these is a fabrication, not a summary.

❌ **WRONG — declaring status the agent cannot know:**
```markdown
**Status: Production-Ready (TICKET-123)**

The feature is fully implemented and **actively used** in production.
Consumer Migration: **In Progress** — consumers gradually migrating to the new topic.
```

✅ **CORRECT — stick to what the code and ticket actually say:**
```markdown
As of TICKET-123, CRUD services now dual-write to this outbox alongside the legacy
outbox; see the service layer for usage. The legacy outbox remains in place.
```

If the ticket says "wire up CRUD services", the doc says "CRUD services are wired up" — nothing more.

---

### Rule 3: Config Values Must Be Copied From Source

**CRITICAL:** Any cron expression, property name, default value, topic name, or configuration constant quoted in prose or a YAML block MUST match the actual source (`application.yml`, `application.properties`, `@Value(...)` annotations, or constant fields) at the time of writing.

**Verification (mandatory before quoting any config value):**
```bash
# Find the actual value in source:
grep -rn "<property.name>" src/main/resources/ src/main/java/
```

❌ **WRONG — copying a plausible-but-stale value from memory:**
```yaml
# In a doc:
some_feature:
  schedule-reconcile: '0 0 2 * * *'   # written from memory; actual value is '0 0 4 * * ?'
```

✅ **CORRECT — open the real config file, copy the literal value:**
```bash
grep -A2 "some_feature" src/main/resources/application.yml
# Then paste the exact text into the doc, verbatim.
```

**Never paraphrase config.** If `application.yml` says `schedule-snapshot-reconcile: 0 0 4 * * ?`, the doc says `0 0 4 * * ?` — not `0 0 2 * * *`, not "daily at 2 AM", not "runs every night".

---

### Rule 4: Scope Discipline

**CRITICAL:** Documentation changes MUST map to the triggering task's scope. A wiring task does not license a "Production-Ready" declaration; a bugfix does not license an architecture-doc rewrite.

Before editing a doc, ask: *"Did the triggering ticket/request ask me to change this doc? If not, is the doc now factually invalidated by my code change?"*

- If **yes** to either → edit the doc, but only the invalidated part.
- If **no** to both → do not touch the doc.

❌ **WRONG — opportunistic scope creep:**
- Task: "wire CRUD services to call the new outbox"
- Agent edits: rewrites the architecture doc, adds a migration-status section, declares the feature Production-Ready, writes a new 350-line usage tutorial.

✅ **CORRECT — minimal mapping:**
- Task: "wire CRUD services to call the new outbox"
- Agent edits: adds one bullet to the data-flow diagram in `AGENTS.md`; updates the one sentence in the architecture doc that previously said "rollout is pending".

---

### Rule 5: No Changelog Content

**CRITICAL:** Documentation describes the CURRENT state of the system, not the history of how it got there. A doc is not a changelog, a commit message, or a task summary.

The following are FORBIDDEN in documentation:
- Past-tense narration about code changes: "was added via migration V21", "was updated to support updates", "entities were aligned across the CRUD layer".
- References to follow-up or future tasks as part of a fact: "this is a separate follow-up task (TICKET-123) performed after the alignment".
- Enumerations of what a task touched: "the field is aligned across `CreateDTO`, `UpdateDTO`, `Service` update/merge logic" — the code itself and the field tables are the source of truth for this.

**Why:** Task history goes stale on the next change (dead task context: "call sites that predate the field" is meaningless a month later), duplicates facts that belong in field tables and architecture docs, and violates the single-source-of-truth rule.

❌ **WRONG — changelog narration in a guide doc:**
```markdown
**Nullable-field persistence pattern**: `short_description` was added via migration
V21. The field is aligned across the CRUD layer. It is NOT propagated to cache DTOs —
that is a separate follow-up task (FCMS-13760) performed after the CRUD alignment.
```

✅ **CORRECT — current-state facts only:**
```markdown
The Lvl2 DTOs include the nullable `shortDescription` field (`short_description`,
`VARCHAR(255)`); the cache/snapshot DTOs do not include it.
```

---

## Part B — Updating Docs After Code Changes

### Rule 6: When To Update Docs

Update documentation ONLY when a code change falls into one of these categories:

| Code change | Doc update required? | Which docs |
|---|---|---|
| New public API (new endpoint, new public method on a service) | ✅ Yes | `AGENTS.md` package map; relevant guide doc |
| New architectural component (new outbox, new publisher, new scheduler) | ✅ Yes | Architecture doc; `AGENTS.md` data-flow section |
| Schema change (new table, new column, new migration) | ✅ Yes | Entities doc; relevant guide doc |
| Behavior change (method now does something different) | ✅ Yes | Doc describing that behavior |
| Internal refactor / rename (no public contract change) | ⚠️ Link-only | Update symbol references in links; no prose rewrite |
| Pure implementation detail (logging, private helpers) | ❌ No | None |

**When in doubt, do less.** A missed doc update is fixable later; a fabricated doc update is actively harmful.

---

### Rule 7: Single Source Of Truth

**CRITICAL:** Each fact lives in **exactly one doc**. If a second doc needs to reference it, it links to the canonical doc instead of restating the fact.

Duplicating content across docs guarantees that the copies will drift apart on the next edit — and one of them will then be wrong. This is one of the most common causes of stale documentation.

❌ **WRONG — same flow described in three places:**
- `AGENTS.md` describes the outbox flow in a code block
- `CACHE_KAFKA.md` describes the same flow with a different diagram
- `LAZY_OUTBOX.md` describes the same flow with a third diagram
- → Next edit changes one; the other two are now wrong.

✅ **CORRECT — one canonical doc, others link to it:**
- `LAZY_OUTBOX.md` owns the full flow (the canonical source).
- `CACHE_KAFKA.md` ends with: *"For the new outbox flow, see [`LAZY_OUTBOX.md`](LAZY_OUTBOX.md)."*
- `AGENTS.md` has a high-level one-line summary + a link.

**How to decide which doc is canonical:** the most specific doc wins. A dedicated `LAZY_OUTBOX.md` beats a general `CACHE_KAFKA.md`; a general `CACHE_KAFKA.md` beats the top-level `AGENTS.md`.

---

### Rule 8: Minimal-Diff Principle

When editing a doc invalidated by a code change, touch **only the invalidated sentences/sections**. Do not opportunistically rewrite surrounding paragraphs, restyle tables, or "improve" unrelated sections.

❌ **WRONG — drive-by rewrites:**
- Code change: one method signature updated.
- Agent edits: rewrites the entire section, changes heading style, adds emoji trackers, reorders paragraphs.

✅ **CORRECT — surgical edit:**
- Code change: one method signature updated.
- Agent edits: updates exactly the one line referencing the old signature.

---

## Part C — Pre-Completion Verification Gate

**CRITICAL:** Before marking any documentation task as complete, run these three checks. If any check fails, the doc is not done.

### Check 1 — Symbol Verification
Every class, method, and field name mentioned in a code block or inline code must `grep` to a real match in `src/main/java/` (or `src/test/java/` for test symbols).

```bash
# Extract every CamelCase identifier from your doc code blocks and grep each:
grep -rn "IdentifierName" src/main/java/
```

### Check 2 — Config Verification
Every cron expression, property name, default value, topic name, and constant quoted in the doc must match the literal text in `application.yml`, `application.properties`, or the relevant `@Value`/constant declaration.

```bash
grep -rn "<property>" src/main/resources/ src/main/java/
```

### Check 3 — Status-Claim Verification
Every status/migration/rollout/deprecation claim must be traceable to:
- The triggering Jira ticket (quote it verbatim), OR
- An explicit user instruction in this session, OR
- It must be deleted.

If a claim has no source, delete it. Sober silence beats a confident fabrication.

### Check 4 — Placement & Duplication
Every added paragraph must pass three placement questions:
- **Duplication:** Is this fact already stated in another doc? If yes — replace the restatement with a link to the canonical doc (Rule 7).
- **Procedure vs documentation:** Is this content a step-by-step developer procedure ("how to add a field to an entity")? If yes — it belongs in an agent skill (`.agents/skills/`), NOT in project documentation.
- **Changelog:** Is this content a narration of what a task changed or plans to change? If yes — delete it (Rule 5).

---

## Summary Checklist

Before finishing any doc edit, verify:

- [ ] Every symbol in code blocks `grep`s to a real match
- [ ] Every config value matches the source file literally
- [ ] No status/migration/rollout claims unless quoted from the ticket
- [ ] Doc changes are within the triggering task's scope
- [ ] Each fact stated in exactly one doc (others link, not duplicate)
- [ ] Only invalidated sections were touched (no drive-by rewrites)
- [ ] No changelog narration (past-tense task history, follow-up task references)
- [ ] No step-by-step procedures outside agent skills
