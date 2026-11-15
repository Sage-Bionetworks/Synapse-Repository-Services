# grid/internal/replica

CRDT replica internals for the Curation Grid. The parent `services/repository-managers/CLAUDE.md` covers the grid's hub-and-replica overview, WebSocket/json-rx protocol, and CRDT document model; this package is the server-side machinery that builds, validates, and merges replica patches. Sub-packages: `change`, `validation`, `merge`, `model`, `view`, `export`.

## Patch-span byte budgeting

CRDT patches are size-bounded, not count-bounded. `PatchUtils.MAX_BYTES_PER_PATCH` (128,000) and `MAX_CHANGE_SET_SIZE` cap how much a single patch / change-set may carry; builders chunk work to stay under these. When emitting patches, budget by serialized bytes via `PatchUtils` — don't emit unbounded patches. `GridReplicaPatchBuilderManagerImpl` buffers a change-set's patches in `BufferedPatchPublisher`, so the total span is known up front. It then reserves that range in the replica's persisted `SEQUENCE_HIGH_WATER` (main DB, see PLFM-10043) and only then publishes.

## Change-handler registry

Replica changes are dispatched through a `ChangeHandler<T>` registry keyed by `IntendedChangeType`, resolved from a constructor-injected `List<ChangeHandler<?>>`. To handle a new intended-change type, add a `ChangeHandler` bean — do not extend a central switch.

## Snapshot export

When the internal replica's sync chain completes, `GridReplicaManagerImpl` calls `GridReplicaSnapshotManager.requestSnapshotIfNeeded`. If the session is eligible on this stack (below) and a snapshot is due (`PATCH_COUNT_SNAPSHOT_THRESHOLD` patches, or `SNAPSHOT_INTERVAL_MS` since the latest snapshot), `GridSnapshotExportPublisher` queues a request on `GRID_SNAPSHOT_EXPORT.fifo`, grouped by session, with the replica's connection ID as the body. The message group delivers one request per session at a time, so a session's exports never overlap. `GridSnapshotExportWorker` (services/workers) calls `exportSnapshotIfNeeded`, which re-checks both the eligibility and the decision before exporting, so requests queued behind an export that already covered their patches, or behind a migration that removed this stack's patch, are skipped.

A session is eligible only if this stack saved a patch for it that is still in `GRID_PATCH`. The grid index tables are not migrated, so a staging stack rebuilds every session and replays migrated patches; without the gate it would export snapshots whose files the next migration orphans in the shared bucket. When `GridManagerImpl.savePatch` saves a new patch and the session has no marker (a non-locking read inside the patch transaction), it registers an after-commit callback that records the patch (logical ID and S3 key) in the non-migrated `GRID_SNAPSHOT_ELIGIBLE` table through `GridDao.markSnapshotEligible`, insert-only and in its own transaction; a failure is logged and only skips snapshots until a later patch writes the marker. A stale marker suppresses re-marking until the check clears it; the next patch then marks again. The check, `GridDao.checkSnapshotEligibility`, reads the marker joined to its patch by (session, logical ID, S3 key) in one query. If the patch is gone or replaced (migration), it deletes the marker with `DELETE ... WHERE SESSION_ID = ? AND S3_KEY = ?`, so a marker written since the read survives. The stack instance plays no part in the decision.

After saving a snapshot on the production stack (`ProdDetector`), the export deletes the session's snapshots superseded more than `SUPERSEDED_SNAPSHOT_RETENTION_MS` (7 days) ago, rows first, then their S3 files. Only `getLatestSnapshot` reads snapshots, so a superseded snapshot only matters to a client still holding its presigned URL or to the staging stack, which shares the snapshot bucket and serves the latest snapshot it has migrated.

## Anti-Patterns — Do NOT

- **Do NOT skip re-applying validation results when they are unchanged.** `GridReplicaValidationManagerImpl` re-applies results unconditionally so the client can use the timestamp to detect staleness, and to avoid an infinite snapshot-triggered revalidation loop (evidence: `validation/GridReplicaValidationManagerImpl.java:196`, PLFM-9342). "Optimizing" this to skip no-op writes reintroduces both bugs.
- **Do NOT run long-running work while handling a message from `GRID_INTERNAL_EVENT.fifo`.** Each internal replica's messages (patches, validation, queries) are handled one at a time in its message group, so every one waits behind the slowest. Queue the work on its own queue and worker, as snapshot exports do through `GridSnapshotExportPublisher`.
- **Do NOT delete snapshot files from a stack that is not production, or shorten the retention below the time between migrations.** Production and staging share `<stack>.grid.snapshot.sagebase.org`, so a delete on either removes a file the other may still serve as its latest snapshot.
