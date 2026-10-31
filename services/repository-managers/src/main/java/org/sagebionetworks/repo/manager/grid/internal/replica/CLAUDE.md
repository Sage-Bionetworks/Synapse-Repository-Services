# grid/internal/replica

CRDT replica internals for the Curation Grid. The parent `services/repository-managers/CLAUDE.md` covers the grid's hub-and-replica overview, WebSocket/json-rx protocol, and CRDT document model; this package is the server-side machinery that builds, validates, and merges replica patches. Sub-packages: `change`, `validation`, `merge`, `model`, `view`, `export`.

## Patch-span byte budgeting

CRDT patches are size-bounded, not count-bounded. `PatchUtils.MAX_BYTES_PER_PATCH` (128,000) and `MAX_CHANGE_SET_SIZE` cap how much a single patch / change-set may carry; builders chunk work to stay under these. When emitting patches, budget by serialized bytes via `PatchUtils` — don't emit unbounded patches. `GridReplicaPatchBuilderManagerImpl` buffers a change-set's patches in `BufferedPatchPublisher`, so the total span is known up front. It then reserves that range in the replica's persisted `SEQUENCE_HIGH_WATER` (main DB, see PLFM-10043) and only then publishes.

## Change-handler registry

Replica changes are dispatched through a `ChangeHandler<T>` registry keyed by `IntendedChangeType`, resolved from a constructor-injected `List<ChangeHandler<?>>`. To handle a new intended-change type, add a `ChangeHandler` bean — do not extend a central switch.

## Anti-Patterns — Do NOT

- **Do NOT skip re-applying validation results when they are unchanged.** `GridReplicaValidationManagerImpl` re-applies results unconditionally so the client can use the timestamp to detect staleness, and to avoid an infinite snapshot-triggered revalidation loop (evidence: `validation/GridReplicaValidationManagerImpl.java:196`, PLFM-9342). "Optimizing" this to skip no-op writes reintroduces both bugs.
