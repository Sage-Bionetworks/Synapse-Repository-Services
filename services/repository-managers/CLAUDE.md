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

Bootstrappers use stable low IDs (1..N reserved for system rows); the `IdType` generator starts at 1000+ so there's a dedicated range for user-created rows. System rows are upserted idempotently via `INSERT ... ON DUPLICATE KEY UPDATE` so definitions can evolve without migration scripts. See `TextAnalyzerBootstrapper` for the reference pattern.

### Authorization for organization-owned resources

Three-layer gate for resources owned by an `Organization` (SynonymSet, ColumnAnalyzerOverride, TextAnalyzer, SearchConfiguration):

```java
AuthorizationUtils.disallowAnonymous(user);
if (!authorizationManager.isSynapseEmployeeOrAdmin(user)) {
    throw new UnauthorizedException("Only Sage employees or admins may X");
}
aclDao.canAccess(user, storedOrganizationId, ObjectType.ORGANIZATION, ACCESS_TYPE.CREATE)
    .checkAuthorizationOrElseThrow();
```

The org ACL check must use the **stored** `organizationId` (loaded from the DB row), not the request's — so a caller cannot alter the request to sidestep the ACL. Admins bypass the org ACL but NOT the sage-employee gate unless explicitly allowed.

### Anonymous user lookup

When a manager needs to operate as "anonymous" (e.g., to enforce public-only access on a search index build), resolve the anonymous `UserInfo` via the realm of the triggering user:

```java
UserInfo anonymous = userManager.getUserInfo(triggeringUser.getRealmAnonymousUserId());
```

Do NOT reference `AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId()` in manager code. Each realm has its own anonymous principal; `getRealmAnonymousUserId()` returns the correct one for the caller's realm. `BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER` is only appropriate in auth filters, bootstrap code, and tests.

### `@Lazy` to break full-WAR-context Spring cycles

If a circular dependency surfaces only in the full Tomcat integration context (not in unit tests), add `@Lazy` on one injection point to break the cycle and leave a comment naming the two beans involved. Example: `TableManagerSupportImpl` lazily injects `columnModelManager` because the full cycle only manifests at IT startup. `@Lazy` defers the proxy until first use so Spring's bean graph resolution can complete.

### Composition over inheritance for async request/response bodies

Reusable payload types (e.g., `SearchQuery`, `SearchQueryResults`) should NOT `implements AsynchronousRequestBody` / `AsynchronousResponseBody`. Instead, wrap them in request/response objects that implement those interfaces (`SearchIndexQuery` composes `SearchQuery`). Why: the same payload type can then be composed into multiple endpoints (search index query today, entity search query later) without coupling the payload to the async job framework.

### Switch / exception hygiene

- `switch` on an enum must have an explicit `default` branch that throws `IllegalArgumentException("Unsupported X: " + value)`. Do not rely on fallthrough; adding an enum value without updating the switch is a bug.
- Preserve the cause when re-throwing: `throw new IllegalArgumentException(msg, e)`, never bare `throw new IllegalArgumentException(msg)` if there is an underlying exception.
- Log OR throw, not both. Low-level managers propagate; the outermost worker/manager logs once at the boundary.

### Package-private for testability

Private methods with meaningful branching (translation logic, mapping helpers, state transitions) should be package-private, not private, so unit tests can exercise them directly instead of only through the public entry point. This keeps test assertions focused.

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
- **`ArgumentCaptor` for forwarded arguments**: when the manager translates user input before delegating (column names, filters, schemas), capture what the collaborator receives and assert on the translated shape. This is where translation/escaping bugs surface.
- **State-machine tests should cover every state**: use `@ParameterizedTest` + `@EnumSource` to drive behavior across all enum values rather than writing one test per state. A single method per state invites drift.
- **External-service-backed managers need autowired tests**: managers that proxy AOSS (e.g., `OpenSearchManagerImpl`), S3, or SNS need a `*AutoWiredTest` against the real service. Pure mock tests don't prove the client API contract holds. Mock-only unit tests for these were replaced, not augmented.

## Search & Indexing (SearchIndex lifecycle)

Portal search is built on **OpenSearch Serverless (AOSS)**. `SearchIndex` is a Synapse Entity (`VersionableEntity` + `HasDefiningSql`) with a one-to-one AOSS index and a lifecycle managed by `SearchIndexLifecycleWorker`.

### Components

| Component | Purpose |
|-----------|---------|
| `SearchIndexMetadataProvider` | Entity-layer validation on CREATE/UPDATE. Pilot gate (Portal Managers + admins only) and `definingSQL` validation. |
| `SearchConfigurationManager` | CRUD for `SearchConfiguration` (the composition of SynonymSets + ColumnAnalyzerOverrides + default analyzer). |
| `SearchConfigurationResolver` | Resolves the effective configuration for a given SearchIndex at build + query time. |
| `SearchIndexLifecycleManager` | Orchestrates build/rebuild/delete; owns state transitions. |
| `SearchIndexQueryManager` | Authorization, status check, translation, delegation to OpenSearchManager for search + autocomplete. |
| `OpenSearchManager` | Shared AOSS client seam (create/delete index, bulk index, search, autocomplete, analyzer validation). |
| `TextAnalyzerBootstrapper` | Ensures 6 system analyzers (IDs 1-6) exist on startup via idempotent upsert. |

### Configuration resolution (priority order)

1. Entity's explicit `searchConfigurationId` (on `NODE_REVISION`).
2. `SearchConfigurationListSetting` project setting on the parent container.
3. Platform defaults (no synonyms, no overrides, column-type defaults).

Effective analyzer per column:

1. Per-column `indexAnalyzerId` from the SearchConfiguration's `ColumnAnalyzerOverride` list.
2. `defaultAnalyzerId` on the SearchConfiguration.
3. `ColumnTypeToOpenSearchMapping.getDefaultAnalyzerId(columnType)`.

### Build-once semantics

AOSS indexes are point-in-time snapshots. Entity metadata changes (name, `definingSQL`, `searchConfigurationId`) are persisted and take effect on the **next build during stack migration** — not immediately. To force an immediate rebuild in the current stack: delete the SearchIndex entity and create a new one.

### Anonymous-user indexing

`SearchIndexLifecycleManagerImpl` streams rows via `TableQueryManager.runQueryAsStream()` using the realm's anonymous user. This causes `addRowLevelFilter()` to apply benefactor ACLs, so only publicly-visible rows enter the AOSS index. Acquire the anonymous `UserInfo` via `userManager.getUserInfo(user.getRealmAnonymousUserId())` (see "Anonymous user lookup" in Common Patterns).

### Pre-flight COUNT + 500K row cap

Before creating the AOSS index, the worker runs a count-only query (`querySinglePage(runQuery=false, runCount=true)`). If the count exceeds 500,000, the build fails immediately with FAILED status and no AOSS index is created. `SearchIndexRowHandler.nextRow()` keeps a row-level guard as a TOCTOU safety net. Error messages are truncated to 3000 chars (`MAX_ERROR_MESSAGE_LENGTH`) before persisting to `SEARCH_INDEX_STATUS`.

### State machine

```
[*] → CREATING → ACTIVE | FAILED (terminal)
ACTIVE → DELETING → [*]
```

- Query against `CREATING` → `IllegalStateException("still building")` → worker translates to `RecoverableMessageException` so SQS retries until the state flips.
- Query against `FAILED` → `IllegalArgumentException` forwarding the stored `errorMessage` verbatim (with a generic remediation hint if none is recorded).
- FAILED is terminal — rebuild means delete + recreate the entity.

### Schema derivation

Only the columns in the `definingSQL`'s SELECT clause are mapped in the AOSS index. The schema is derived at build time by `QueryTranslator.getSchemaOfSelect()` inside the `RowHandlerProvider` callback (after SQL parsing, before row execution).

### Analyzer IDs

- **1-999**: reserved for system analyzers; IDs 1-6 are bootstrapped (`SCIENTIFIC`, `STANDARD`, `IDENTIFIER`, `KEYWORD`, `AUTOCOMPLETE`, `AUTOCOMPLETE_SEARCH`).
- **1000+**: user-defined (from `IdType.TEXT_ANALYZER_ID`).
- All analyzers register in OpenSearch as `synapse_analyzer_{id}`.
- Synonyms are baked into AOSS at **build** time. They are NOT loaded at query time — only column analyzer overrides and TextAnalyzer definitions are loaded at query time for field routing.

### Dual-field strategy

- KEYWORD-analyzer columns: primary field is `keyword`; `.searchable` sub-field is `text` (for full-text search).
- All other text columns: primary field is `text` with the configured analyzer; `.keyword` sub-field (for filter/facet/sort).
- Highlight field names ending in `.searchable` are stripped in response conversion — the client never sees the sub-field suffix.

### Cross-resource deletion protection

Configuration resources (SynonymSet, ColumnAnalyzerOverride, TextAnalyzer) that are referenced by any `SearchConfiguration` cannot be deleted. The DB enforces this via `ON DELETE RESTRICT` foreign keys; DAOs catch `DataIntegrityViolationException` and re-throw as `IllegalArgumentException` with a user-facing message. Per-resource deletion endpoints for SynonymSet and ColumnAnalyzerOverride were intentionally **removed** — once created, they're permanent to prevent orphaned references. Cross-resource JSON-array lookups (e.g., "which configurations reference this synonym set?") use MySQL's `JSON_CONTAINS()`.

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
