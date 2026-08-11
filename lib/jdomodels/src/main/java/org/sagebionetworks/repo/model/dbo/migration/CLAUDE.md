# dbo/migration

The migration engine that backs cross-stack data replication. The parent `lib/jdomodels/CLAUDE.md` covers the DBO/`MigratableTableTranslation` authoring pattern; this package is the engine (`MigratableTableDAOImpl`) that discovers, orders, backs up, and restores those types.

## Startup invariants (enforced, will fail the stack)

`MigratableTableDAOImpl.initialize()` asserts several things at startup — violating them throws immediately, which is intentional (a misordered migration corrupts data silently otherwise):

- **Primary types are discovered and sorted to `MigrationType` enum order automatically** by `DboAutoDiscovery` — there is no manual registration list. `initialize()` re-asserts the discovered order is non-decreasing in enum index as a sanity check (`MigratableTableDAOImpl.java:159`).
- **`MigrationType.CHANGE` must be the last type** (`:165`) — asynchronous message triggering depends on CHANGE migrating last, so downstream index rebuild sees a complete state.
- **Every backup-ID column must be a `bigint` and unique-constrained** (`:308`, PLFM-2512) — a non-unique or wrong-typed backup id loses rows during migration.

## Mechanics worth knowing

- **`runWithKeyChecksIgnored`** — toggles FK/unique constraint checking off around bulk restore, then back on. Use it for restore batches, not ad-hoc.
- **`BatchUtility`** — chunks batch operations by an estimated `max_allowed_packet` byte budget rather than a fixed row count, so large rows don't blow the MySQL packet limit.

## Anti-Patterns — Do NOT

- **Do NOT declare a new `MigrationType` enum value out of dependency order, and never after `CHANGE`.** Discovery sorts primary types to enum order, so the enum declaration is what controls migration order — place the new value with its dependencies before it and keep `CHANGE` last, or the startup assertions above will reject it.
