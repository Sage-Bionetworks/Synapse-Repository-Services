# lib/lib-database-configuration

Foundational module holding the JDBC/DataSource beans and the `@WriteTransaction`-family annotations. Extracted from `lib/models` to break a dependency cycle, so it must stay dependency-light — most other modules depend on it.

## What lives here

- `org.sagebionetworks.repo.model.config.DatabaseInfrastructureConfiguration` — the `DataSource` / `JdbcTemplate` / `PlatformTransactionManager` beans for both databases.
- `org.sagebionetworks.repo.transactions.*` — the transaction meta-annotations applied on manager/DAO methods across the codebase.

## Two databases, two bean sets — qualify explicitly

There are parallel beans for the **main (transactional)** DB and the **migration** DB. They are NOT interchangeable; inject the one you mean by `@Qualifier`:

| Concern | Main DB | Migration DB |
|---------|---------|--------------|
| DataSource | `@Qualifier("dataSourcePool")` | `@Qualifier("migrationDataSourcePool")` |
| TransactionManager | `txManager` | `migrationTxManager` |
| JdbcTemplate | `@Qualifier("jdbcTemplate")` | `migrationJdbcTemplate` |

`namedParameterJdbcTemplate` wraps the main `jdbcTemplate`.

## Transaction annotations

Each annotation is a `@Transactional` meta-annotation pinned to a specific `transactionManager` + `Propagation`, all at `Isolation.READ_COMMITTED`. Choosing the wrong one silently changes locking/commit semantics — pick by intent, don't copy blindly:

| Annotation | transactionManager | Propagation | Use when |
|-----------|--------------------|-------------|----------|
| `@WriteTransaction` | `txManager` | `REQUIRED` | Standard write — join or create a transaction |
| `@MandatoryWriteTransaction` | `txManager` | `MANDATORY` | Must run inside an existing transaction (throws if none) |
| `@NewWriteTransaction` | `txManager` | `REQUIRES_NEW` | Must commit independently (suspends the outer tx) |
| `@MigrationWriteTransaction` | `migrationTxManager` | `REQUIRED` | Writes against the migration DB |
| `@TransactionNotSupported` | `txManager` | `NOT_SUPPORTED` | Must run with no transaction |

Read-only operations take no annotation (default Spring behavior).

## Constraints

- **`dataSource.setMaxTotal(-1)` (unbounded connection pool) is intentional** — do not "fix" it to a bounded value without reading PLFM-8344 (`DatabaseInfrastructureConfiguration.java:40`). A bound here caused production stalls.
- **Keep this module's dependencies minimal.** It exists specifically to break the `lib/models` ↔ transaction-annotation cycle; pulling heavy deps back in re-creates the cycle.

## Anti-Patterns — Do NOT

- **Do NOT call a `@NewWriteTransaction` method from within an existing transaction when it updates rows the outer transaction already locked** — the new (suspended-outer) transaction blocks on the outer's locks and self-deadlocks (evidence: `NewWriteTransaction.java` javadoc warning).
