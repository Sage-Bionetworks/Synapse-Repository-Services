# grid/internal/replica

CRDT replica internals for the Curation Grid. The parent `services/repository-managers/CLAUDE.md` covers the grid's hub-and-replica overview, WebSocket/json-rx protocol, and CRDT document model; this package is the server-side machinery that builds, validates, and merges replica patches. Sub-packages: `change`, `validation`, `merge`, `model`, `view`, `export`.

## Patch-span byte budgeting

CRDT patches are size-bounded, not count-bounded. `PatchUtils.MAX_BYTES_PER_PATCH` (128,000) and `MAX_CHANGE_SET_SIZE` cap how much a single patch / change-set may carry; builders chunk work to stay under these. When emitting patches, budget by serialized bytes via `PatchUtils`, and let `PatchSpanPublisherProxy` track cumulative span across a whole change-set so the logical clock advances correctly — don't emit unbounded patches.

## Patch ID allocation

- A patch `(replica, S)` with span `N` consumes sequence numbers `S…S+N-1`; IDs must never be reused.
- Don't pick the next sequence from the index DB alone: it's empty on a new stack while `GRID_PATCH` is migrated. `GridReplicaPatchBuilderManagerImpl.buildPatch` uses `max(source clock max, index clock, last stored seq + span - 1)`.
- `GridManagerImpl.savePatch` accepts a duplicate ID only when the body hash matches; a different body is rejected. Never rely on `INSERT IGNORE` to dedupe.

## Change-handler registry

Replica changes are dispatched through a `ChangeHandler<T>` registry keyed by `IntendedChangeType`, resolved from a constructor-injected `List<ChangeHandler<?>>`. To handle a new intended-change type, add a `ChangeHandler` bean — do not extend a central switch.

## Anti-Patterns — Do NOT

- **Do NOT skip re-applying validation results when they are unchanged.** `GridReplicaValidationManagerImpl` re-applies results unconditionally so the client can use the timestamp to detect staleness, and to avoid an infinite snapshot-triggered revalidation loop (evidence: `validation/GridReplicaValidationManagerImpl.java:196`, PLFM-9342). "Optimizing" this to skip no-op writes reintroduces both bugs.
