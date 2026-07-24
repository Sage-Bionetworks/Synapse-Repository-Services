# services/repository-managers

Business logic layer — Manager interfaces and implementations that sit between controllers and DAOs. This module enforces authorization, validation, and transaction boundaries.

## Package Structure

```
org.sagebionetworks.repo.manager
├── (root)           # Core managers: EntityManager, UserManager, NodeManager, etc.
├── asynch/          # Async job framework (AsynchJobStatusManager, AsyncJobRunner)
├── entity/          # Entity authorization
├── file/            # File handle operations
├── table/           # Table/view managers
├── schema/          # JSON Schema managers
├── grid/            # Grid/Curator managers
├── agent/           # AI agent managers
├── config/          # Spring @Configuration classes
└── ...              # Many more domain sub-packages
```

## Manager Pattern

### Interface + Impl

```java
// Interface — defines the contract
public interface EntityManager {
    Entity getEntity(UserInfo userInfo, String entityId) throws NotFoundException, UnauthorizedException;
}

// Implementation — @Service, constructor injection, transaction annotations
@Service
public class EntityManagerImpl implements EntityManager {
    private final NodeManager nodeManager;
    private final EntityAuthorizationManager entityAuthorizationManager;
    // ... more dependencies

    // Constructor injection (preferred over @Autowired fields)
    public EntityManagerImpl(NodeManager nodeManager, EntityAuthorizationManager authManager, ...) {
        this.nodeManager = nodeManager;
        this.entityAuthorizationManager = authManager;
    }
}
```

### Authorization

Check access before performing operations:

```java
entityAuthorizationManager.hasAccess(userInfo, entityId, ACCESS_TYPE.READ)
    .checkAuthorizationOrElseThrow();
```

- Returns `AuthorizationStatus` with `.checkAuthorizationOrElseThrow()`
- Throws `UnauthorizedException` on failure
- Every public method that accepts `UserInfo` should check authorization

### Input Validation

```java
ValidateArgument.required(userInfo, "userInfo");
ValidateArgument.required(entityId, "entityId");
ValidateArgument.requiredNotBlank(name, "name");
ValidateArgument.requiredNotEmpty(list, "list");
```

## Transaction Annotations

Defined in `org.sagebionetworks.repo.transactions`. Applied on **implementation methods**, not interfaces.

| Annotation | Behavior |
|-----------|----------|
| `@WriteTransaction` | Joins existing transaction or creates a new one. Standard for most write operations. |
| `@MandatoryWriteTransaction` | **Requires** an existing transaction — throws if none exists. Used for methods that must be called within an outer transaction. |
| `@NewWriteTransaction` | Always creates a **new, independent** transaction (suspends any existing one). Used for operations that must commit independently (e.g., updating job progress). |

Read-only operations have no transaction annotation (default Spring behavior).

## Async Job Framework

For long-running operations exposed as async REST endpoints:

1. **Define request/response schemas** in `lib-auto-generated` (extend `AsynchronousRequestBody` / `AsynchronousResponseBody`)
2. **Implement `AsyncJobRunner<Req, Resp>`** in the manager layer:
   ```java
   @Service
   public class MyAsyncWorker implements AsyncJobRunner<MyRequest, MyResponse> {
       public MyResponse run(Long jobId, UserInfo user, MyRequest request, JobCancelCallback cancelCallback) {
           // Do work, return response
       }
   }
   ```
3. **Wire in worker config** — add a `@Bean` method in `AsyncJobWorkersConfig` that wraps the runner with `AsyncJobRunnerAdapter` and a `WorkerTriggerBuilder`
4. **Controller** calls `asynchJobStatusManager.startJob(userInfo, request)` to enqueue, client polls `getJobStatus()`

## Spring Configuration

- Managers use `@Service` annotation — discovered via component scan
- Constructor injection preferred (fields are `private final`)
- **Preferred**: Add new bean definitions to `ManagerConfiguration` (`org.sagebionetworks.repo.manager.config.ManagerConfiguration`)
- **Legacy**: Spring XML configs (`*-spb.xml` in `src/main/resources/`) still used for some beans and `MigrationTypeListener` registration (`managers-spb.xml`). Do not add new XML configs.
- Controllers access managers through `ServiceProvider` (not direct injection)

## Common Patterns

### ID Parsing
`NumberFormatException` extends `IllegalArgumentException`, which already maps to HTTP 400. Wrapping `Long.parseLong()` in a try-catch is **optional** — it's acceptable to let the `NumberFormatException` propagate directly. If you want a more descriptive error message, extract to a shared utility method rather than duplicating try-catch blocks:
```java
// Option 1: Let NumberFormatException propagate (acceptable — results in 400)
Long id = Long.parseLong(request.getId());

// Option 2: Wrap for better message (optional, extract to util if reused)
private Long parseId(String value, String fieldName) {
    try {
        return Long.parseLong(value);
    } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid " + fieldName + ": '" + value + "'", e);
    }
}
```

### Pagination
Use the existing `NextPageToken` utility for all paginated list operations. Do NOT create custom pagination logic:
```java
NextPageToken nextPageToken = new NextPageToken(request.getNextPageToken());
List<T> page = dao.list(nextPageToken.getLimitForQuery(), nextPageToken.getOffset());
return new ListResponse().setResults(page)
    .setNextPageToken(nextPageToken.getNextPageTokenForCurrentResults(page));
```

### Interfaces
Only create a separate interface when there's a genuine abstraction benefit (multiple implementations, or callers need to be decoupled from the implementation). For classes with a single implementation and no need for abstraction, use the concrete class directly. Don't copy the interface+impl pattern from older code just because it exists.

### Bootstrappers
Classes that bootstrap data on startup should run the bootstrap logic **in the constructor**, not via `InitializingBean.afterPropertiesSet()`. This ensures that loading the bean triggers the bootstrap:
```java
@Service
public class MyBootstrapper {
    public MyBootstrapper(MyDao dao, ...) {
        this.dao = dao;
        bootstrap(); // Run in constructor
    }
}
```

### Helper Methods Should Return Useful Results
Methods like `getOrCreate()` should return the found-or-created object so callers don't need a separate query:
```java
// Good — returns the organization either way
public Organization getOrCreateOrganization(String name) { ... }

// Bad — returns void, caller must re-query
public void ensureOrganizationExists(String name) { ... }
```

## Testing

- Unit tests: `@ExtendWith(MockitoExtension.class)` with `@Mock` and `@InjectMocks`
- Mock DAOs and other managers, verify interactions
- Test authorization failures (verify `UnauthorizedException` thrown)
- Test input validation (verify `IllegalArgumentException` thrown)
- Integration tests in `integration-test/` module test the full stack
- **Service layer tests are usually unnecessary.** Most services are thin delegation layers that convert `Long userId` → `UserInfo` and forward to the manager. If the service has no real logic (no branching, no transformation, no error handling), skip the unit test. The IT-level controller test will verify the wiring. Only test services that contain actual business logic (e.g., `EntityService`).
- **`@InjectMocks` with `@Spy`**: When you need to verify that one method in the class under test calls another method on the same class, use `@Spy` with `@InjectMocks`:
  ```java
  @Spy
  @InjectMocks
  private MyManagerImpl manager;
  // Now you can: verify(manager).someInternalMethod(...)
  ```
- **Pagination tests**: Always verify `NextPageToken` behavior — test that the response includes the correct next page token, not just the results list.

## Defining-SQL Entities

Several entity types are defined by a `definingSql` query against a Synapse table or view: `MaterializedView`, `VirtualTable`, `SearchIndex`.
The select-list columns — including computed expressions and literals — become the entity's schema and downstream artifact (materialized rows, an OpenSearch index, etc.).

These entity types share a common pattern. Follow it whenever introducing a new defining-SQL entity rather than re-translating the SQL on every read:

- **Pre-parse and bind the schema in a metadata provider.** On `entityCreated` / `entityUpdated`, parse `definingSql` via `QueryTranslator`, run each ColumnModel from `getSchemaOfSelect` through `ColumnModelManager.createColumnModel` (hash-deduplicates against existing persisted rows), and call `columnModelManager.bindColumnsToVersionOfObject(ids, entityId)`. Reference: `MaterializedViewManagerImpl.bindSchemaToView`, `SearchIndexLifecycleManagerImpl.registerSchema`. Literals (`'tag' as tag_alias`) and aliases that don't appear on the source schema (`concat(a, b) as new_alias`) get a real `ColumnModel` id and become first-class columns of the entity.
- **Read the bound schema, don't re-translate.** Build and query paths both load the schema with `tableManagerSupport.getTableSchema(entityId)`. Schema state flows through entity create/update only — never per-request — so a runtime read does not pay the cost of `QueryTranslator.build`.
- **Aliases that collide with a source column name** (`concat(name, 'x') as name`) reuse the source column's `ColumnModel` id, because `getSchemaOfDerivedColumn` clones the source's properties and `createColumnModel` hashes to the same row. This preserves any settings (e.g., analyzer overrides) registered against the source column.
- **Inner column references must still resolve.** Aliasing only frees up the *output* name, not the inputs. `concat(unknown_col, 'x') as foo` fails because `unknown_col` isn't on the source schema.
- **Constants must use single quotes** — `'foo' as foo`. Double-quoted strings (`"foo"`) are SQL identifiers (column refs) and fail validation if the identifier doesn't exist on the source schema.

Failing the parse synchronously in the metadata provider means malformed `definingSql` is rejected with `IllegalArgumentException` (HTTP 400) at create/update time, instead of silently FAILED'ing during the async build.

## Curation Grid (Curator)

A spreadsheet-style collaborative editing feature that allows data curators to annotate files (FileEntity annotations) and manage record-based metadata (RecordSet entities). Unlike the standard Controller → Manager → DAO pattern, the grid uses a **CRDT (Conflict-free Replicated Data Type)** architecture based on the [JSON-Joy](https://jsonjoy.com/) specification, enabling real-time multi-user and AI-assisted editing.

### Hub-and-Replica Architecture

- **Grid Session**: Created via async job (`POST /grid/session/async/start`). Represents a collaborative editing session backed by a CRDT document.
- **Replicas**: Each connected client (or AI agent) gets a unique replica with a numeric `replicaId`. Single writer per replica, multiple readers allowed. `GridReplicaConnectionManager` creates replicas and publishes their lifecycle events.
- **Hub**: A cluster of workers that receives patches from all replicas via an **SQS queue**, persists them, and broadcasts `"new-patch"` notifications to all connected replicas.

### WebSocket Protocol

Uses **AWS API Gateway WebSocket** (NOT Spring STOMP/SockJS) with a custom messaging protocol based on the [json-rx specification](https://jsonjoy.com/specs/json-rx/messages):
- Message format: `[type, sequence, method, payload]` — e.g., `[1, 42, "patch", <data>]`
- Methods: `"patch"` (send CRDT patch), `"synchronize-clock"` (replica sends version vector to hub)
- Notifications: `"new-patch"`, `"ping"`/`"pong"`
- Connection via **pre-signed URL** (15 min expiry) from `POST /grid/{sessionId}/presigned/url`

### CRDT Document Model

The grid document uses JSON-Joy CRDT node types:
- `con` (Constant) — immutable cell values and metadata
- `vec` (Vector) — LWW append-only arrays for column names and row data (max 256 entries)
- `arr` (RGA Array) — mutable ordered arrays for column order and row order
- Patches encoded in json-joy [compact format](https://jsonjoy.com/specs/json-crdt-patch/encoding/compact-format), serialized as **CBOR** (Jackson `jackson-dataformat-cbor`)

### Database Representation

Grid patches are stored relationally in `lib-grid-db` tables — the full CRDT document is **never loaded into memory**. A SQL template (`services/repository-managers/src/main/resources/grid/grid-index-view-template.sql`) joins patch tables to produce a paginated tabular view, enabling efficient reads over large datasets.

### Grid Synchronization

A grid session is created *from* a source entity and can be re-synchronized with it later (`POST` a `SynchronizeGridRequest` async job, package `org.sagebionetworks.repo.manager.grid.synch`). `SyncType.PULL` only updates the grid from the source; `SyncType.PULL_PUSH` also writes the merged result back to the source afterward.

- **`SourceHandler` / `SourceWriter`** (`grid.synch.handler`) bridge the generic sync engine to a concrete source type. One pair of implementations exists per source: `EntityViewSourceHandler`/`Writer` and `RecordSetSourceHandler`/`RecordSetSourceWriter`. A `SourceHandler` reports keying/matchability/deletion rules to the engine; the paired `SourceWriter` performs the (optional) push.
- **Two-phase merge**: Phase 1 reconciles schema (source columns vs. grid columns) and matches rows by a source-supplied key; Phase 2 streams the merged row set, applying CRDT patches to the grid and reporting each final row to the `SourceWriter` for an optional push.

The two source types differ enough to warrant a side-by-side comparison:

| Aspect                | EntityView                                                                                                                           | RecordSet                                                                                                                                                                                                                                                                            |
|-----------------------|--------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Supported `SyncType`s | `PULL_PUSH` only                                                                                                                     | `PULL` and `PULL_PUSH`                                                                                                                                                                                                                                                               |
| Row identity          | The ID of the entity the row represents                                                                                              | The entity's `upsertKey` columns, deterministically encoded by `UpsertKeyEncoder`                                                                                                                                                                                                    |
| Mutation on push      | **In place** — cell changes write directly to entity annotations                                                                     | **Never in place** — a `PULL_PUSH` push builds a brand-new artifact (data CSV + validation summary, via `RecordSetArtifactBuilder`) and creates a new RecordSet revision                                                                                                             |
| Deletion detection    | Immediate — every read is against live annotations, so a row/column simply absent from the current read is absent, no history needed | Baseline-relative — each revision's CSV is immutable, so a row/column is only inferred "deleted by the user" by diffing the *synced baseline* (`sourceEntityVersionNumber` recorded on the session) against the latest revision. A null baseline (first sync) never infers deletions |
| Unmatchable rows      | None — every row is intrinsically keyed by entity ID                                                                                 | Rows with an incomplete `upsertKey` get a synthetic UUID key so they're never matched — always copied through as-is instead of merged                                                                                                                                                |

### AI Agent Integration

The AI Grid Assistant binds to a grid session via `GridAgentSessionContext` (containing `gridSessionId` and `usersReplicaId`). The agent reads and writes grid data through **MCP services** (Grid Query / Grid Update) that translate SQL-like operations into CRDT patches flowing through the same hub.

### Validation Worker

A dedicated worker listens to grid changes via an SQS queue, validates each changed row against the bound **JSON Schema**, and writes validation results back as CRDT patches to `rows[*].metadata.rowValidation`.

### Key REST APIs

- `POST /grid/session/async/start` — create a grid session (async job, takes `CreateGridRequest`)
- `GET /grid/session/async/get/{asyncToken}` — poll for session creation result
- `POST /grid/{sessionId}/replica` — create a new replica
- `POST /grid/{sessionId}/presigned/url` — get pre-signed WebSocket URL
