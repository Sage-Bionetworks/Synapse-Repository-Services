# services/workers

Async worker WAR — all background processing in Synapse. Workers consume SQS messages or run on fixed schedules via Quartz, coordinated by database semaphores for cluster-wide concurrency control.

## Async Job Framework

User-facing operations that are too slow for synchronous HTTP use the async job framework:
1. Client submits a request object (extends `AsynchronousRequestBody`) via a start endpoint
2. Request is serialized to an SQS queue
3. A worker implementing `AsyncJobRunner<Request, Response>` picks up the message, executes the work, and returns a response object (extends `AsynchronousResponseBody`)
4. Client polls a get endpoint with the async token until the result is ready

Key classes:
- `AsyncJobRunner<R, T>` — worker interface (`lib/lib-worker/`). Implement `getRequestType()`, `getResponseType()`, and `run()`.
- `AsynchJobType` — enum mapping request/response types to queue names (`lib/models/`). New async jobs must be registered here.
- `SynapseClient` / `SynapseClientImpl` — add client methods for submitting and polling async jobs (`client/synapseJavaClient/`)

### SQS Queue Infrastructure

Queue names are resolved at runtime via `stackConfig.getQueueName("BASE_NAME")` → `{stack}-{instance}-BASE_NAME`. The actual SQS queues and SNS topic subscriptions are provisioned by **Synapse-Stack-Builder** (a separate CloudFormation project). New queues must be added to the Stack Builder's `sns-and-sqs-config.json` before they can be used. If a queue doesn't exist in AWS, the worker will fail to get the queue URL at runtime.

## Two Worker Types

### 1. Message-Driven Workers (event-driven)

Implement `ChangeMessageDrivenRunner` (single message) or `BatchChangeMessageDrivenRunner` (batch):

```java
@Service
public class MyWorker implements ChangeMessageDrivenRunner {
    public void run(ProgressCallback progressCallback, ChangeMessage message) throws RecoverableMessageException {
        // Process message
        // Throw RecoverableMessageException for transient failures (message returns to queue)
        // Other exceptions → message deleted (permanent failure)
    }
}
```

**Execution stack:**
```
Quartz Trigger → ChangeMessageDrivenWorkerStack → SemaphoreGatedRunner
  → PollingMessageReceiver (long-polls SQS) → ChangeMessageBatchProcessor
    → ChangeMessageDrivenRunner.run(callback, message)
```

### 2. Scheduled Workers (periodic)

Implement `ProgressingRunner`:

```java
public class MyWorker implements ProgressingRunner {
    public void run(ProgressCallback progressCallback) throws Exception {
        // Do work
        // Call progressCallback.progressMade() to refresh semaphore lock
    }
}
```

**Execution stack:**
```
Quartz Trigger → SemaphoreGatedWorkerStack → SemaphoreGatedRunner
  → ProgressingRunner.run(callback)
```

## Wiring a Worker

Worker configuration lives in `@Configuration` classes under `org.sagebionetworks.worker.config`. This is the **preferred approach** — do not add new Spring XML configs.

### Config Classes

| Class | Purpose |
|-------|---------|
| `AsyncJobWorkersConfig` | Workers that run async jobs (`AsyncJobRunner`) — wrapped via `AsyncJobRunnerAdapter` |
| `ChangeMessageWorkersConfig` | Workers driven by change messages (`ChangeMessageDrivenRunner` / `BatchChangeMessageDrivenRunner`) — wrapped via `ChangeMessageBatchProcessor` |
| `MessageDrivenWorkersConfig` | Workers driven by generic SQS messages (typed JSON payloads) — wrapped via `TypedMessageDrivenRunnerAdapter` or `JsonEntityDrivenRunnerAdapter` |

### Preferred Pattern: `ConcurrentWorkerStack` + `WorkerTriggerBuilder`

```java
@Bean
public SimpleTriggerFactoryBean myWorkerTrigger(ConcurrentManager concurrentStackManager, MyWorker myWorker) {
    String queueName = stackConfig.getQueueName("MY_QUEUE");
    MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, myWorker);

    return new WorkerTriggerBuilder()
        .withStack(ConcurrentWorkerStack.builder()
            .withSemaphoreLockKey("myWorker")
            .withSemaphoreMaxLockCount(10)
            .withSemaphoreLockAndMessageVisibilityTimeoutSec(120)
            .withMaxThreadsPerMachine(3)
            .withSingleton(concurrentStackManager)
            .withCanRunInReadOnly(false)
            .withQueueName(queueName)
            .withWorker(worker)
            .build()
        )
        .withRepeatInterval(2187)
        .withStartDelay(1025)
        .build();
}
```

### Fallback: `MessageDrivenWorkerStack` (older pattern, still used)

```java
MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
config.setGate(stackStatusGate);
config.setQueueName(queueName);
config.setRunner(worker);
config.setSemaphoreLockAndMessageVisibilityTimeoutSec(60);
config.setSemaphoreMaxLockCount(4);
config.setSemaphoreLockKey("myWorker");

MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);

return new WorkerTriggerBuilder()
    .withStack(stack)
    .withRepeatInterval(1368)
    .withStartDelay(1305)
    .build();
```

### Key Config Properties

- `semaphoreLockKey` — unique name for cluster-wide lock
- `semaphoreMaxLockCount` — max concurrent instances across the cluster
- `semaphoreLockAndMessageVisibilityTimeoutSec` — lock timeout + SQS visibility timeout
- `maxThreadsPerMachine` — concurrency per JVM (`ConcurrentWorkerStack` only)
- `canRunInReadOnly` — whether worker runs during migration read-only mode
- `queueName` — SQS queue name via `stackConfig.getQueueName("QUEUE_KEY")`

### Two-Step Registration (Critical)

Creating a worker `@Bean` trigger in Java config is **not enough**. Workers require two registrations:

1. **Define the trigger bean** in a `@Configuration` class (see Config Classes above) using `WorkerTriggerBuilder` + `ConcurrentWorkerStack.builder()`.

2. **Register the trigger in the Quartz scheduler** by adding a `<ref bean="...Trigger"/>` entry to the `workerTriggersList` in `services/workers/src/main/resources/main-scheduler-spb.xml`. **If this step is missed, the worker will never run** — the bean exists but Quartz never schedules it. There will be no error at startup; the worker silently does nothing.

### Legacy XML Config (do not add new ones)

Some older workers are still wired in per-worker `*-spb.xml` files under `src/main/resources/`, imported by `main-scheduler-spb.xml`. Migrate these to `@Configuration` classes when modifying them.

**Scheduler:** `main-scheduler-spb.xml` imports worker configs, collects triggers into `workerTriggersList`, creates a single `SchedulerFactoryBean` with thread pool sized to trigger count + 1.

## Error Handling

| Exception | Behavior |
|-----------|----------|
| `RecoverableMessageException` | Message returns to SQS queue for retry |
| `NotFoundException` (common pattern) | Log and delete message (no retry) |
| Other exceptions | Message deleted (permanent failure) |
| Transient AWS/DB errors | Catch and wrap in `RecoverableMessageException` |

Common transient exceptions to catch and retry:
- `LockReleaseFailedException`, `CannotAcquireLockException`
- `DeadlockLoserDataAccessException`
- `AmazonServiceException` (service errors)
- `TemporarilyUnavailableException`

## Worker Categories

| Package | Type | Description |
|---------|------|-------------|
| `change/` | Scheduled | `ChangeSentMessageSynchWorker` — reconciles CHANGES vs SENT_MESSAGES |
| `table/` | Message-driven | Table index management, materialized view updates |
| `replication/` | Batch message | Entity replication to index database |
| `file/` | Message-driven | File preview generation |
| `search/` | Message-driven | Search index (OpenSearch) updates |
| `schema/` | Message-driven | JSON Schema validation |
| `migration/` | Batch message | Data migration workers |
| `log/` | Scheduled | S3 log collation |
| `agent/` | Message-driven | AI agent chat processing |
| `grid/` | Message-driven | Grid CRDT patch processing, validation |

## Key Architectural Worker: ChangeSentMessageSynchWorker

This worker drives index rebuilding after migration:
1. Compares CHANGES vs SENT_MESSAGES via checksum-based range scanning
2. Varies page size pseudo-randomly to catch false-negative checksums
3. Groups unsent changes by `ObjectType`
4. Publishes batches to SNS topics via `RepositoryMessagePublisher`
5. Respects stack read-write mode (skips during migration)
6. Tracks CloudWatch metrics (elapsed time, sent count, failures)

## WAR Deployment

- `web.xml` loads Spring context via `ContextLoaderListener` (no servlets — workers only)
- Spring context → `main-scheduler-spb.xml` → Quartz scheduler starts all workers
- **Local/test**: Both the repository and workers WARs are deployed to the same embedded Tomcat instance
- **Production**: Each WAR is deployed to its own **Elastic Beanstalk** Tomcat cluster (one for repository, one for workers)

## Testing

- JUnit 5 + Mockito 2.27 (`@ExtendWith(MockitoExtension.class)`)
- Mock managers/DAOs, verify expected calls
- Test both success paths and error handling (RecoverableMessageException vs permanent failure)
- Test ObjectType/ChangeType filtering logic
