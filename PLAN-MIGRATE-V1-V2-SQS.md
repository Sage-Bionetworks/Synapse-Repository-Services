# Plan: Migrate SQS from AWS SDK v1 to AWS SDK v2 (PLFM-9749)

Companion checklist: [TODO-MIGRATE-V1-V2-SQS.md](TODO-MIGRATE-V1-V2-SQS.md)

## Goal

Remove every use of `com.amazonaws.services.sqs.*` (artifact `com.amazonaws:aws-java-sdk-sqs`) and route all SQS
traffic through the AWS SDK v2 `software.amazon.awssdk.services.sqs.SqsClient`. This continues the
client-by-client phase-out of SDK v1 (`AwsClientFactory` → `AwsClientFactoryV2`).

## Current state

### Already on v2
- `AwsClientFactoryV2.createSqsClient()` exists.
- `ManagerConfiguration` defines a singleton `SqsClient` bean (bean name `createSqsClient`), built with the
  shared `AwsCredentialsProvider` and `us-east-1`. It already serves production traffic through the grid
  publishers (`PatchBuilderPublisherImpl`, `InternalHubToReplicaPublishHandler`,
  `InternalReplicaToHubEventPublisherImpl`). Client construction, credentials, and the sync HTTP client
  (`apache-client`, from `lib/stackConfiguration`) are therefore already proven.

### Still on v1 (~50 main files, ~50 test files, 5 Maven modules)

| Area | Module | Classes |
|---|---|---|
| Client bean | repository-managers | `awsSQSClient` bean in `private/aws-topic-publisher.spb.xml` → `AwsClientFactory.createAmazonSQSClient()` |
| XML wiring | workers, repository (test), lib-worker (test) | 21 `ref="awsSQSClient"`: 14× `ChangeMessageDrivenWorkerStack`, 3× `MessageDrivenWorkerStack`, 2× `QueueCleaner` in test contexts, plus the bean definitions |
| Producers | repository-managers | `AsynchJobQueuePublisherImpl`, `RecurrentAthenaQueryManagerImpl`, `FileHandleArchivalManagerImpl`, `FileHandleAssociationScannerNotifierImpl`, `MessageSyndicationImpl`, `ReplicationMessageManagerImpl`, `StatisticsMonthlyProcessorNotifierImpl`, `WebhookManagerImpl` |
| Legacy consumer stack | lib-worker-common | `MessageDrivenRunner`, `PollingMessageReceiverImpl`, `MessageDrivenWorkerStack`, `MessageQueueImpl`, `QueueCleaner` |
| Concurrent consumer stack | lib-worker | `ConcurrentManager`/`ConcurrentManagerImpl`, `ConcurrentWorkerStack` (javadoc only), `ChangeMessageDrivenWorkerStack`, `ChangeMessageBatchProcessor`, `MessageUtils`, `WorkerProgress` (dead, used only by a test stub) |
| Worker infra | workers | `WorkersInfraConfig`, `AsyncJobWorkersConfig`, `ChangeMessageWorkersConfig`, `MessageDrivenWorkersConfig`, `TypedMessageDrivenRunner`/`Adapter`, `JsonEntityDrivenRunnerAdapter`, `AsyncJobRunnerAdapter`, `WorkerProfiler` (import/javadoc only; its pointcut uses `run(..)`, so it is SDK-agnostic), `PurgeAndRecordSqsQueue` (standalone `main`, builds its own client) |
| Workers receiving v1 `Message` | workers, repository-managers | `RecurrentAthenaQueryWorker`, `DataAccessSubmissionNotificationWorker`, `FileEventRecordWorker`, `FileHandleAssociationScanRangeWorker`, `FileHandleKeysArchiveWorker`, `GridEventBrokerWorker`, `GridReplicaPatchBuilderWorker`, `GridReplicaValidationWorker`, `GridReplicaWorker`, `ProjectStorageDataRefreshWorker`, `SESNotificationWorker`, `StatisticsMonthlyWorker`, `DefiningSqlSourceUpdateWorker`, `ReplicatedToViewWorker`, `TableSnapshotWorker`, `UpdateQueryCacheWorker`, `WebhookMessageWorker`, `WebhookMessageDispatcher` |
| POMs | lib-worker-common, stackConfiguration | `aws-java-sdk-sqs` (version comes from the v1 BOM; no explicit root entry) |

The 20 `ChangeMessageDrivenRunner` and 31 `AsyncJobRunner` implementations do **not** depend on the SQS
type; their adapters shield them.

## Why this is more than a client swap

The v1 model class `com.amazonaws.services.sqs.model.Message` is part of the **public worker contract**:
- `MessageDrivenRunner.run(ProgressCallback, Message)`
- `TypedMessageDrivenRunner.run(ProgressCallback, Message, T)`
- `MessageUtils.*(Message)`, `MessageUtils.MessageBundle`
- `AsynchJobQueuePublisher.recieveOneMessage` / `deleteMessage`

Changing that type breaks every implementor at once. That is the only part of the migration that has to
land atomically.

## v1 → v2 differences that matter here

| Concern | v1 | v2 | Risk |
|---|---|---|---|
| Message accessors | `getBody()`, `getMessageId()`, `getReceiptHandle()`, `getMessageAttributes()` | `body()`, `messageId()`, `receiptHandle()`, `messageAttributes()` | Compile-time, mechanical |
| System attributes | `getAttributes()` → `Map<String,String>` | `attributes()` → `Map<MessageSystemAttributeName,String>`; `attributesAsStrings()` for string keys | **Silent**: `PollingMessageReceiverImpl.getRetryVisibilityTimeout` does a string-key lookup of `ApproximateReceiveCount`. A naive port compiles, returns `null`, and silently falls back to the fixed 5s retry, which slows table-query polling and ITs |
| Model construction | Mutable, `new X().withY(..)` | Immutable builders | Mechanical (~35 `new Message()` in tests) |
| Convenience overloads | `getQueueUrl(String)`, `sendMessage(url, body)`, `deleteMessage(url, handle)` | Request objects / `Consumer<Builder>` only | Mechanical (15 main call sites) |
| Batch results | `SendMessageBatchResult.getSuccessful()/getFailed()` | `SendMessageBatchResponse.successful()/failed()` | `MessageSyndicationImpl.prepareResults` |
| Exceptions | `com.amazonaws.services.sqs.model.QueueDoesNotExistException` | `software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException` (extends `SqsException`) | `QueueCleaner`, `MessageSyndicationImpl`. A stale import still compiles while v1 is on the classpath but **never matches**. Grep for it before removing v1 |
| Mocking | Tests mock the concrete `AmazonSQSClient` | Mock the `SqsClient` interface | v2 has `Consumer<Builder>` overloads; bare `any()` becomes ambiguous. Use `any(ReceiveMessageRequest.class)` etc. (strict stubs, no lenient) |
| Concrete-type cast | `ChangeMessageDrivenWorkerStack` casts `(AmazonSQSClient) awsSQSClient` | n/a | Remove the cast |
| Lifecycle | No close | `SqsClient` is `AutoCloseable`; Spring calls `close()` on context shutdown | Confirm it does not interfere with the PLFM-6758 shutdown hook (no deletes during JVM shutdown) |

**No wire or data impact.** Queue payloads are JSON strings. The SNS envelope is unwrapped by
`MessageUtils.extractMessageBodyAsJSONObject`, which does not depend on the SDK. There are no DB or
migration changes, so no two-stack bridge release is needed: v1 and v2 clients can run against the same
queues at the same time, and each PR below can ship in any release.

## PR strategy

**Short answer: it does not need to be one PR.** v1 and v2 already coexist, so the work splits into
**3 PRs, each merged to `develop` on its own.** Only PR 3 (the `Message` type change) is atomic, and it is
mechanical. Each PR should still be organized as reviewable commits.

### Why not a single PR
- One PR of about 100 files would mix the **risky** part (polling, visibility, and delete behaviour in the
  consumer loop) with the **bulky** part (renaming accessors across about 40 workers and tests). Reviewers
  would skim the risky lines in the noise.
- A regression found on staging would mean reverting everything, including unrelated producer changes.
- A single long-lived branch keeps conflicting with `develop` as other people's worker PRs land.

### Why not more than 3
- PR 3 cannot be split without a second temporary bridge, which would mean more throwaway code than it
  saves.

### The three PRs

| PR | Scope | Size | Risk | Independently mergeable? |
|---|---|---|---|---|
| **PR 1: Producers** | 8 repository-managers producers and their tests, moved to the existing `SqsClient` bean | ~8 main + ~9 test | Low | Yes |
| **PR 2: Consumer framework on v2 client** | Every SQS **client** call in the consumer stacks (receive, change visibility, delete, getQueueUrl, resend) moves to v2. A temporary `SqsMessageBridge` converts the received v2 `Message` to a v1 `Message` at the `runner.run(...)` boundary, so worker signatures stay as they are. Delete the v1 client bean; alias `awsSQSClient` to the v2 bean | ~15 main + ~8 test + XML | **Medium-high** (core polling loop) | Yes, after PR 1 (needs the v1 bean free of producer dependents) |
| **PR 3: Flip `Message` type and remove v1** | Runner interfaces, adapters, `MessageUtils`, 18 workers, ~40 tests move to v2 `Message`. Delete the bridge and `WorkerProgress`. Remove `aws-java-sdk-sqs` from the POMs. Update docs | ~30 main + ~45 test | Low (compile-checked) | Yes, after PR 2. **Atomic**: intermediate commits may not compile |

Order of merge: PR 1 → PR 2 → PR 3. PR 1 and PR 2 can be **reviewed** in parallel. PR 2 is branched
from PR 1 until PR 1 merges.

### When to merge each PR
- **PR 1**: as soon as it is approved and CI (unit tests plus repository-managers autowired tests) is green.
- **PR 2**: once approved, unit tests are green, **and** a full `integration-test` run on a dev stack is
  green (it exercises async jobs, change messages, and table queries end to end through both consumer
  stacks). Then let it go through **one staging-stack validation** (a full release cycle) before merging
  PR 3, so that reverting PR 2 stays a clean single-commit revert.
- **PR 3**: once approved, CI is green, the integration-test run is green, and
  `mvn dependency:tree | grep aws-java-sdk-sqs` is empty for every module.

### Review guidance per PR
- **PR 1**: one commit per manager (main + test), then one wiring commit. Reviewers check each converted
  request field by field (queue URL, body, message attributes, delay seconds, group id).
- **PR 2**: commits in this order. Reviewers spend most of their time on commits 2 and 3.
  1. `SqsMessageBridge` plus its unit test (round-trip of body, ids, system attributes, and message
     attributes including binary and list values).
  2. `PollingMessageReceiverImpl`, `MessageQueueImpl`, `QueueCleaner`, `MessageDrivenWorkerStack`
     (legacy stack).
  3. `ConcurrentManager`/`Impl` (concurrent stack).
  4. `ChangeMessageDrivenWorkerStack`, `ChangeMessageBatchProcessor` (resend path).
  5. `RecurrentAthenaQueryWorker` constructor, Java configs, XML aliasing, test contexts, delete the v1 bean
     and `AwsClientFactory.createAmazonSQSClient`.
- **PR 3**: commits by layer: (1) interfaces, adapters, and `MessageUtils`; (2) workers by package
  (grid, table, file, webhook, other); (3) tests by package; (4) delete the bridge and `WorkerProgress`,
  POM cleanup, docs. Review the whole PR diff for the tip build. Per-commit review is for readability
  only, because the build is only guaranteed green at the tip.

## Design decisions

1. **Keep the bean name `awsSQSClient` via an alias** instead of rewriting 21 XML refs. Use
   `@Bean(name = {"createSqsClient", "awsSQSClient"})` in `ManagerConfiguration`, or an `<alias>` in
   `aws-topic-publisher.spb.xml`. Test contexts that do not load `ManagerConfiguration`
   (`lib/lib-worker/src/test/resources/test-context.xml`) define `awsSQSClient` with
   `AwsClientFactoryV2.createSqsClient`.
2. **Use the v2 `Message` directly in worker signatures** rather than a Synapse-owned wrapper. Workers only
   read body, id, and attributes, and the v2 type is immutable and stable. A wrapper would add a layer for
   no current benefit. (Open question 1.)
3. **Inject the interface (`SqsClient`) everywhere.** No concrete-type references or casts.
4. **Use `attributesAsStrings()` vs. the enum map consistently.** Framework code that asks for
   `MessageSystemAttributeName` values uses `attributes()` with the enum key. Worker code that reads
   custom attributes uses `messageAttributes()`.
5. **`SqsMessageBridge` is temporary.** It is package-private where possible and has a class javadoc that
   names PLFM-9749 as its removal ticket. PR 3 deletes it.

## Validation

- Unit tests per module: `lib/stackConfiguration`, `lib/lib-worker-common`, `lib/lib-worker`,
  `services/repository-managers`, `services/workers`.
- Tests that hit real SQS by design: `MessageSyndicationImplAutowiredTest`,
  `AsynchJobQueuePublisherImplTest`, `SESNotificationWorkerAutowireTest`, `WebhookWorkerIntegrationTest`,
  lib-worker tests using `test-context.xml`, and the workers `*IntegrationTest` suite.
- Full `integration-test` module on a dev stack after PR 2 and after PR 3.
- After PR 2, on a dev or staging stack, compare against the previous build in CloudWatch:
  `ApproximateAgeOfOldestMessage` and `NumberOfMessagesDeleted` per queue, worker `JobTracker` metrics, and
  table-query latency (this is where the retry-visibility bug would show).
- Connection pool: all polling now goes through the single v2 Apache pool (default 50 connections, the same
  default the v1 client used). Confirm that the peak number of concurrent SQS calls (worker threads plus
  progress-listener heartbeats) does not exceed the pool. If it can, set `maxConnections` explicitly in the
  `SqsClient` bean.

## Rollout and rollback

- No data or schema impact, so a plain revert of any PR is safe in any release.
- PR 2 is the only behavioural risk. If a staging stack shows a stuck queue, growing message age, or
  messages processed twice, revert PR 2. PR 1 stays in place.
- PR 3 adds no behaviour beyond PR 2, so its rollback risk is limited to compile-level mistakes caught by
  CI.

## Open questions

1. Is a Synapse-owned `SqsMessage` wrapper wanted for SDK isolation? The default is no, as described in
   design decision 2.
2. Should the XML refs be renamed to the v2 bean name in PR 3 (removing the alias), or should the alias
   stay permanently? The default is to keep the alias.
3. Is `PurgeAndRecordSqsQueue` (an ops-only `main`) still used? If not, delete it in PR 3 instead of porting
   it.
