# PostgreSQL Specific Rules

> ℹ️ **APPLICABILITY:** This rule set applies ONLY if PostgreSQL JDBC/R2DBC driver (`org.postgresql:postgresql`, `io.r2dbc:r2dbc-postgresql`, etc.) is present in the **main** project dependencies (`implementation` / `compile` in `build.gradle` or `pom.xml`). Ignore if present only in test scope.

---

## PostgreSQL Indexing & Constraint Rules

### UNIQUE Constraint & Index Rule

- **Automatic Index Creation:** In PostgreSQL, declaring a column/table constraint as `UNIQUE` automatically creates a corresponding unique B-tree index under the hood.

- **FORBIDDEN (No Duplicate Indexes):** Do NOT execute an explicit `CREATE INDEX ...` statement on columns or column sets that are already governed by a `UNIQUE` constraint or a `UNIQUE` column definition.

- **Performance Impact:** Creating a duplicate index on top of a `UNIQUE` constraint wastes storage, increases write/insert overhead, and bloats WAL logs without providing any query optimization benefit.
