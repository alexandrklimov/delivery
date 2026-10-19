# Redundancy Rules for AI-Generated Code

> ℹ️ **Purpose:** prevent AI agents from producing code that is mechanically correct but redundant: pass-through helper wrappers, assertions subsumed by stronger assertions, and unjustified method overloads. These patterns inflate diffs and coverage metrics while proving nothing.

---

## Rule 1: No Pass-Through Factory/Helper Wrappers [CRITICAL]

Do NOT add a method to a test data factory (or any helper class) whose body is a **single unconditional delegation** to an existing generator, adding no constraint (format, length, boundary, null-handling).

**Test to apply:** if the method body is `return existingGenerator();` (or a trivial variant) and introduces no validation or business logic — inline the call and do not create the method.

Non-normative illustration of the pattern (placeholder names):

```java
// ❌ WRONG — pure delegation, no added semantics
public String rndDescription() {
    return rndStr();
}

// ✅ CORRECT — call the underlying generator directly
dto = new MyDto(..., rndStr(), ...);

// ✅ ALSO CORRECT — a wrapper is justified ONLY when it encodes a real constraint
public String rndCode() {
    return rndStr(REGEX_CONSTRAINT); // generator variant enforcing a format the DB requires
}
```

---

## Rule 2: No Assertions Subsumed by an Equals-Based Comparison [CRITICAL]

Do NOT add a field-level assertion (e.g., `assertThat(actual.field()).isEqualTo(expected.field())`) when the same test already performs an equals-based comparison of the **whole object** (records, classes with generated `equals`), because the object-level comparison covers that field.

**Exception:** the field-level assertion is allowed ONLY when the field carries a serialization/persistence risk that the object-level comparison cannot detect (e.g., a JSON property name mismatch visible only when comparing raw payloads). In that case, a short comment in the test MUST explain why the redundant-looking assertion exists.

**Test to apply:** for each new assertion ask: "can this assertion fail while all other assertions in this test still pass, for a distinct root cause?" If no — remove it.

This rule applies **within a single test**. Duplication of facts **across tests in the same layer** is governed by the testing rules ("Test Value Criteria").

---

## Rule 3: Factory/Helper Method Overloads — Escalate, Don't Decide [CRITICAL]

Do NOT add overloads to test factory/helper methods by default. Overload sets tend to grow ("overloading explosion") and are hard to keep semantically distinct.

If an overload seems justified — typically to inject a value the existing method cannot express (e.g., a boundary value such as `null`, an empty collection, a max-length string) — the agent MUST:

1. State the justification in the task plan or conversation.
2. Ask the user for approval BEFORE adding the overload.
3. If approved, document the justification in the overload's JavaDoc.

A no-argument "random everything" method plus ONE approved parameterized variant is the expected ceiling; more requires renewed approval.

---

## Rule 4: Pre-Completion Self-Audit [CRITICAL]

Before declaring any coding task complete, the agent MUST perform a separate diff-review pass over its OWN changes and attest against the "Code Review Checklist for AI-generated code" in [`code-style-rules.md`](./code-style-rules.md), which includes the redundancy items from this file.

The self-audit is a distinct step performed AFTER the implementation is written — writing rules and applying them mid-generation are different cognitive acts, and skipping the separate pass is how redundant code escapes into commits.

---

## Summary Checklist

- [ ] No new method is a pure unconditional delegation to another helper
- [ ] No assertion is subsumed by an equals-based comparison in the same test (or has a documented serialization-pitfall justification)
- [ ] No factory/helper overload added without user-approved justification
- [ ] A separate pre-completion diff-review pass was performed over the full change set
