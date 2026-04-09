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

## Curation Grid (Curator)

A spreadsheet-style collaborative editing feature that allows data curators to annotate files (FileEntity annotations) and manage record-based metadata (RecordSet entities). Unlike the standard Controller → Manager → DAO pattern, the grid uses a **CRDT (Conflict-free Replicated Data Type)** architecture based on the [JSON-Joy](https://jsonjoy.com/) specification, enabling real-time multi-user and AI-assisted editing.

### Hub-and-Replica Architecture

- **Grid Session**: Created via async job (`POST /grid/session/async/start`). Represents a collaborative editing session backed by a CRDT document.
- **Replicas**: Each connected client (or AI agent) gets a unique replica with a numeric `replicaId`. Single writer per replica, multiple readers allowed.
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

### AI Agent Integration

The AI Grid Assistant binds to a grid session via `GridAgentSessionContext` (containing `gridSessionId` and `usersReplicaId`). The agent reads and writes grid data through **MCP services** (Grid Query / Grid Update) that translate SQL-like operations into CRDT patches flowing through the same hub.

### Validation Worker

A dedicated worker listens to grid changes via an SQS queue, validates each changed row against the bound **JSON Schema**, and writes validation results back as CRDT patches to `rows[*].metadata.rowValidation`.

### Key REST APIs

- `POST /grid/session/async/start` — create a grid session (async job, takes `CreateGridRequest`)
- `GET /grid/session/async/get/{asyncToken}` — poll for session creation result
- `POST /grid/{sessionId}/replica` — create a new replica
- `POST /grid/{sessionId}/presigned/url` — get pre-signed WebSocket URL
