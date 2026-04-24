# lib/lib-worker

Lower-level worker framework — cluster-wide concurrency control, SQS message consumption, and change-message batch processing. Consumed by `services/workers`.

This module is intentionally small and stable; most worker-authoring patterns live in `services/workers/CLAUDE.md`.

## Package Structure

```
org.sagebionetworks.asynchronous.workers
├── changes/      # Change-message-driven runners (SNS→SQS fan-out)
├── concurrent/   # ConcurrentWorkerStack + cluster-wide semaphore management
└── sqs/          # SQS message utilities + JobCanceledException
```

## Key Abstractions

### `changes/` — Change-message-driven workers

- **`ChangeMessageDrivenRunner`** — single-message worker. Implementers receive one `ChangeMessage` per `run()` invocation.
- **`BatchChangeMessageDrivenRunner`** — batch worker. Implementers receive a `List<ChangeMessage>` per `run()`; useful when ordering within the batch matters or when a downstream API supports bulk operations.
- **`ChangeMessageBatchProcessor`** — adapter that wraps a `ChangeMessageDrivenRunner` so it can be scheduled by `ConcurrentWorkerStack`.
- **`LockTimeoutAware`** — optional interface for runners that need to know the configured semaphore lock timeout (to self-regulate long operations).

### `concurrent/` — Concurrent worker stack

- **`ConcurrentWorkerStack`** — the canonical worker execution stack. Composes a cluster-wide semaphore + per-JVM thread pool + SQS long-poll loop.
- **`ConcurrentManager` / `ConcurrentManagerImpl`** — singleton bean that tracks active workers across the JVM (passed to every stack via `.withSingleton(concurrentStackManager)`).
- **`ConcurrentProgressCallback`** — `ProgressCallback` implementation that refreshes the semaphore lock while work is in flight. Workers must call `progressCallback.progressMade()` periodically or the lock expires and a second worker may pick up the same message.
- **`WorkerJob`** — internal unit of scheduled work.

### `sqs/` — SQS utilities

- **`MessageUtils`** — helpers for reading/parsing `ChangeMessage` payloads from SQS `Message` objects.
- **`JobCanceledException`** — throw from an `AsyncJobRunner` to mark a job cancelled by the user (distinct from a transient failure).
- **`WorkerProgress`** — progress-tracking interface.

## Recoverable vs permanent failure

Every worker runs inside a try/catch that interprets the exception type:

| Exception | Outcome |
|-----------|---------|
| `RecoverableMessageException` | Message returns to SQS for retry (increments receive count) |
| `JobCanceledException` | Job marked CANCELED, message deleted |
| Any other `Exception` / `Throwable` | Message deleted, no retry; logged |

The `services/workers` layer is responsible for translating common transient exceptions (`LockReleaseFailedException`, `CannotAcquireLockException`, `DeadlockLoserDataAccessException`, `TableUnavailableException`) into `RecoverableMessageException` at the worker boundary. `lib-worker` itself does not catch these — it only reacts to the final `RecoverableMessageException`.

## What lives where

| Concern | Location |
|---------|----------|
| `AsyncJobRunner` interface | `services/workers/src/main/java/org/sagebionetworks/worker/` (NOT here) |
| `AsyncJobRunnerAdapter` | `services/workers/` — wraps `AsyncJobRunner` so it can be a `MessageDrivenRunner` |
| `WorkerTriggerBuilder` | `services/workers/src/main/java/org/sagebionetworks/worker/config/` |
| `@Configuration` worker beans | `services/workers/src/main/java/org/sagebionetworks/worker/config/` |
| `AsynchJobType` enum | `lib/jdomodels` (server) + `client/synapseJavaClient` (client) — registrations in both |

If you're authoring a worker, start in `services/workers/CLAUDE.md`. Come here only to understand the framework primitives being composed.

## Testing

- Unit tests for framework classes mock the SQS client and the `ConcurrentManager`.
- Framework is stable — most commits land in `services/workers` (consumers), not here.

## Build

```
mvn clean install -pl lib/lib-worker -DskipTests
```
