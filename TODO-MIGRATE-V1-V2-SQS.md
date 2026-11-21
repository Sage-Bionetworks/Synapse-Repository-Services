# TODO: Migrate SQS from AWS SDK v1 to AWS SDK v2 (PLFM-9749)

Plan and rationale: [PLAN-MIGRATE-V1-V2-SQS.md](PLAN-MIGRATE-V1-V2-SQS.md)

Merge order: **PR 1 → PR 2 → (one staging validation) → PR 3**. Each PR merges to `develop` on its own.

---

## PR 1: Producers (repository-managers)

Branch from `develop`. One commit per manager (main + test).

- [x] `AsynchJobQueuePublisher` / `AsynchJobQueuePublisherImpl`: inject `SqsClient`; `sendMessage`,
      `getQueueUrl`, `receiveMessage`, `deleteMessage` move to v2 request builders; the interface's
      `recieveOneMessage` / `deleteMessage` take and return the v2 `Message` (callers are internal and tests only)
  - [x] `AsynchJobQueuePublisherImplTest`
- [x] `RecurrentAthenaQueryManagerImpl`: `SendMessageRequest` → v2 builder
  - [x] `RecurrentAthenaQueryManagerTest`
- [x] `FileHandleArchivalManagerImpl`: `getQueueUrl(String)` and `sendMessage(url, body)` → request builders
  - [x] `FileHandleArchivalManagerTest`
- [x] `FileHandleAssociationScannerNotifierImpl`
  - [x] `FileHAndleAssociationScannerNotifierUnitTest`
- [x] `MessageSyndicationImpl`: `SendMessageBatchRequest` / `Entry` / `Result` → v2; `prepareResults` uses
      `successful()` / `failed()`; catch the **v2** `QueueDoesNotExistException`
  - [x] `MessageSyndicationImplTest`
  - [x] `MessageSyndicationImplAutowiredTest` (hits real SQS)
- [x] `ReplicationMessageManagerImpl`: `GetQueueAttributesRequest` with the enum `QueueAttributeName`;
      read `attributes()` using the enum key (or `attributesAsStrings()`)
  - [x] `ReplicationMessageManagerImplTest`
- [x] `StatisticsMonthlyProcessorNotifierImpl`
  - [x] `StatisticsMonthlyProcessorNotifierImplTest`
- [x] `WebhookManagerImpl`: inject the `SqsClient` interface (it currently injects the concrete `AmazonSQSClient`);
      `MessageAttributeValue` → v2 builder
  - [x] `WebhookManagerUnitTest`
- [x] Spring wiring: confirm each manager now resolves the `SqsClient` bean (constructor injection by type)
- [x] `grep -rn "com.amazonaws.services.sqs" services/repository-managers/src` shows only
      `WebhookMessageDispatcher` and its test (consumer side, PR 3)
- [ ] `mvn test -pl services/repository-managers` green
- [ ] **Merge** once approved and CI is green

---

## PR 2: Consumer framework on the v2 client (temporary bridge)

Branch from PR 1 (rebase on `develop` after PR 1 merges). Commits in this order:

### Commit 1: Bridge
- [ ] Add `SqsMessageBridge` in `lib-worker-common` (`org.sagebionetworks.workers.util.aws.message`):
      v2 `Message` → v1 `Message` (messageId, receiptHandle, body, md5OfBody, `attributesAsStrings()`,
      messageAttributes including string, binary, and list values with dataType, md5OfMessageAttributes).
      Class javadoc: temporary, removed by PLFM-9749 PR 3
- [ ] Add the v2 `software.amazon.awssdk:sqs` dependency to `lib/lib-worker-common/pom.xml`
- [ ] `SqsMessageBridgeTest`: round-trip with real attribute data (including binary and an `ApproximateReceiveCount`)

### Commit 2: Legacy stack (`lib-worker-common`)
- [ ] `PollingMessageReceiverImpl`: `SqsClient`; v2 `ReceiveMessageRequest` with
      `messageSystemAttributeNames(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT)`, `waitTimeSeconds(0)`
      (keep the existing comment), `ChangeMessageVisibilityRequest`, `DeleteMessageRequest`. Bridge to v1
      only at `runner.run(...)`
  - [ ] `getRetryVisibilityTimeout` reads the **v2** message with the enum key (not a string key). Add a test
        that asserts the receive-count-based timeout (for example, count 3 → 3s), not the 5s fallback
- [ ] `MessageQueueImpl`: `SqsClient`; `GetQueueUrlRequest`
- [ ] `QueueCleaner`: `SqsClient`; batch receive and delete; v2 `QueueDoesNotExistException`
- [ ] `MessageDrivenWorkerStack`: constructor takes `SqsClient`
- [ ] Tests: `PollingMessageReceiverImplTest`, `MessageQueueImplTest`, `QueueCleanerTest`,
      `MessageDrivenWorkerStackTest` (typed matchers, e.g. `any(ReceiveMessageRequest.class)`; no lenient stubs)
- [ ] `mvn test -pl lib/lib-worker-common` green

### Commit 3: Concurrent stack (`lib-worker`)
- [ ] `ConcurrentManager`: `getAmazonSQSClient()` → `getSqsClient()` returning `SqsClient` (or remove it if
      unused; currently there are no callers)
- [ ] `ConcurrentManagerImpl`: receive, change visibility, and delete use v2; bridge at `worker.run(...)`;
      keep the FIFO single-message loop and the `isShutdown` delete guard unchanged
  - [ ] `ConcurrentManagerImplTest`
- [ ] `ConcurrentWorkerStack`: update the javadoc `{@link}` to the v2 `receiveMessage`

### Commit 4: Change-message path (`lib-worker`)
- [ ] `ChangeMessageDrivenWorkerStack`: take `SqsClient`; remove the `(AmazonSQSClient)` cast
- [ ] `ChangeMessageBatchProcessor`: constructor takes `SqsClient`; `getQueueUrl` and both resend
      `sendMessage(url, body)` calls → v2 builders (`run` still receives the bridged v1 `Message`)
  - [ ] `ChangeMessageBatchProcessorTest`
- [ ] `mvn test -pl lib/lib-worker` green

### Commit 5: Wiring and removal of the v1 client bean
- [ ] `ManagerConfiguration`: `@Bean(name = {"createSqsClient", "awsSQSClient"})` (or an XML `<alias>`)
- [ ] Delete the v1 `awsSQSClient` bean from `private/aws-topic-publisher.spb.xml`
- [ ] Delete `AwsClientFactory.createAmazonSQSClient()` and its imports
- [ ] `WorkersInfraConfig`, `AsyncJobWorkersConfig`, `ChangeMessageWorkersConfig`, `MessageDrivenWorkersConfig`:
      inject `SqsClient`
- [ ] `RecurrentAthenaQueryWorker`: constructor takes `SqsClient`; `GetQueueUrlRequest`
  - [ ] `RecurrentAthenaQueryWorkerTest`
- [ ] XML: verify that all 14 `ChangeMessageDrivenWorkerStack` and 3 `MessageDrivenWorkerStack` refs resolve to
      the v2 bean through the alias (no edits expected)
- [ ] Test contexts:
  - [ ] `lib/lib-worker/src/test/resources/test-context.xml`: `awsSQSClient` →
        `AwsClientFactoryV2.createSqsClient`
  - [ ] `services/workers/src/test/resources/test-context.xml` (`QueueCleaner`): resolves through the alias
  - [ ] `services/repository/src/test/resources/test-context.xml` (`QueueCleaner`): resolves through the alias
- [ ] Check: `grep -rn "AmazonSQS\b\|AmazonSQSClient\b" --include=*.java --include=*.xml lib services` is empty
      (only v1 **model** imports remain)
- [ ] `services/workers/CLAUDE.md`: update the `MessageDrivenWorkerStack` example (`amazonSQSClient` → `sqsClient`)

### Before merge
- [ ] `mvn clean install -DskipTests` (full reactor) green
- [ ] `mvn test` on `lib/lib-worker-common`, `lib/lib-worker`, `services/repository-managers`, `services/workers` green
- [ ] Workers autowired and integration tests green (`SESNotificationWorkerAutowireTest`,
      `WebhookWorkerIntegrationTest`, `*IntegrationTest`)
- [ ] Full `integration-test` run on a dev stack green
- [ ] Dev-stack CloudWatch: queue `ApproximateAgeOfOldestMessage` and `NumberOfMessagesDeleted` match the
      previous build; no rise in table-query latency
- [ ] Connection-pool sanity: peak concurrent SQS calls ≤ v2 Apache `maxConnections` (default 50); set it
      explicitly if needed
- [ ] Confirm that `SqsClient.close()` on context shutdown does not race the PLFM-6758 shutdown hook
- [ ] **Merge** once approved
- [ ] **Wait for one staging-stack validation** (a release cycle) before merging PR 3

---

## PR 3: Flip the `Message` type and remove v1 (atomic)

Branch from `develop` after PR 2 merges. The build is only guaranteed green at the tip; commits are for review.

### Commit 1: Framework interfaces and utilities
- [ ] `MessageDrivenRunner.run(ProgressCallback, Message)`: v2 `Message`
- [ ] `TypedMessageDrivenRunner.run(ProgressCallback, Message, T)`: v2 `Message`
- [ ] `TypedMessageDrivenRunnerAdapter`, `JsonEntityDrivenRunnerAdapter`, `AsyncJobRunnerAdapter`
- [ ] `MessageUtils` (including `MessageBundle` and `createTopicMessage`, which uses a v2 builder)
- [ ] `ChangeMessageBatchProcessor.run`
- [ ] `PollingMessageReceiverImpl` and `ConcurrentManagerImpl`: pass the v2 `Message` straight through
- [ ] `WorkerProfiler`: drop the v1 import and fix the javadoc `{@link}` (the pointcut needs no change)
- [ ] Delete `SqsMessageBridge` and `SqsMessageBridgeTest`
- [ ] Delete `WorkerProgress` (dead; used only by `StubWorker`)

### Commit 2: Workers (`getBody()` → `body()`, etc.)
- [ ] grid: `GridEventBrokerWorker`, `GridReplicaPatchBuilderWorker`, `GridReplicaValidationWorker`, `GridReplicaWorker`
- [ ] table: `DefiningSqlSourceUpdateWorker`, `ReplicatedToViewWorker`, `TableSnapshotWorker`, `UpdateQueryCacheWorker`
- [ ] file: `FileEventRecordWorker`, `FileHandleAssociationScanRangeWorker`, `FileHandleKeysArchiveWorker`
- [ ] webhook: `WebhookMessageWorker`, `WebhookMessageDispatcher` (repository-managers; `MessageAttributeValue` → v2)
- [ ] other: `RecurrentAthenaQueryWorker`, `DataAccessSubmissionNotificationWorker`, `ProjectStorageDataRefreshWorker`,
      `SESNotificationWorker`, `StatisticsMonthlyWorker`
- [ ] `PurgeAndRecordSqsQueue`: port to `AwsClientFactoryV2.createSqsClient()`, or delete if unused (open question 3)

### Commit 3: Tests (`new Message().withBody(..)` → `Message.builder().body(..).build()`)
- [ ] lib-worker: `MessageUtilsTest`, `ChangeMessageBatchProcessorTest`, `ConcurrentManagerImplTest`, `StubWorker`
- [ ] lib-worker-common: `PollingMessageReceiverImplTest`, `MessageDrivenWorkerStackTest`
- [ ] repository-managers: `WebhookMessageDispatcherUnitTest`
- [ ] workers/grid: `GridCSVDownloadWorkerTest`, `GridEventBrokerWorkerUnitTest`, `GridReplicaPatchBuilderWorkerTest`,
      `GridReplicaValidationWorkerTest`, `GridReplicaWorkerTest`
- [ ] workers/table: `DefiningSqlSourceUpdateWorkerTest`, `TableCSVDownloadWorkerTest`, `TableQueryWorkerTest`,
      `TableSnapshotWorkerTest`, `UpdateQueryCacheWorkerTest`
- [ ] workers/file: `FileEventRecordWorkerTest`, `FileHandleAssociationScanRangeWorkerTest`,
      `FileHandleKeysArchiveWorkerTest`, `PreviewWorkerTest`
- [ ] workers/snapshot writers: `AclObjectRecordWriterTest`, `CertifiedUserPassingRecordWriterTest`,
      `FileHandleSnapshotRecordWriterTest`, `NodeObjectRecordWriterTest`, `PrincipalObjectRecordWriterTest`,
      `ProjectSettingObjectRecordWriterTest`, `VerificationSubmissionObjectRecordWriterTest`
- [ ] workers/other: `RecurrentAthenaQueryWorkerTest`, `ProjectStorageDataRefreshWorkerTest`,
      `ObjectReplicationWorkerTest`, `SESNotificationWorkerTest`, `SESNotificationWorkerAutowireTest`,
      `StatisticsMonthlyWorkerTest`, `WebhookMessageWorkerUnitTest`, `WebhookWorkerIntegrationTest`,
      `AsyncJobProgressRunnerAdapterTest`, `JsonEntityDrivenRunnerAdapterTest`, `TypedMessageDrivenRunnerAdapterTest`

### Commit 4: Dependency and doc cleanup
- [ ] Remove `com.amazonaws:aws-java-sdk-sqs` from `lib/lib-worker-common/pom.xml` and `lib/stackConfiguration/pom.xml`
- [ ] `grep -rn "com.amazonaws.services.sqs" --include=*.java --include=*.xml .` is empty (excluding `target/`)
- [ ] `mvn dependency:tree | grep aws-java-sdk-sqs` is empty for every module (no transitive leak)
- [ ] Decide whether to keep the `awsSQSClient` alias or rename the refs (open question 2)
- [ ] Root `CLAUDE.md` tech-stack line: note that SQS is fully on v2 (if the doc tracks per-client status)

### Before merge
- [ ] Full reactor `mvn clean install` with tests green
- [ ] Full `integration-test` run on a dev stack green
- [ ] **Merge** once approved
