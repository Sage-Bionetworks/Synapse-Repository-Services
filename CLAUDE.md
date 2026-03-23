# Synapse Repository Services

Backend platform for Sage Bionetworks' Synapse — a collaborative research data sharing platform.

## Tech Stack

- **Java 11** (do not use Java 17+ features)
- **Spring 5.3.39** (Spring MVC, Spring JDBC, Spring AOP) — NOT Spring Boot, NOT Spring 6
- **javax.servlet / javax.annotation** — NOT jakarta.* (Spring 6 migration pending)
- **MySQL 8.0** via Spring JdbcTemplate (no ORM, no Spring Data)
- **Tomcat 9** (WAR deployment)
- **Jackson 2.20.0**, Log4j 2, Guava 30.1.1
- **AWS SDK v1** (1.12.x) + **AWS SDK v2** (2.29.x), Google Cloud Storage
- **No Lombok**

## Build Commands

```
mvn clean install -DskipTests                           # Full build
mvn clean install -pl <module-path> -DskipTests          # Single module
mvn test -pl <module-path>                               # Unit tests for module
mvn test -pl <module-path> -Dtest=<TestClassName>        # Single test class
```

## Module Structure

```
platform (root)
├── lib/                          # 25+ shared libraries
│   ├── lib-auto-generated/       # JSON schema → POJO (schema-to-pojo plugin)
│   ├── models/                   # DAO interfaces
│   ├── jdomodels/                # DAO implementations, DBO classes, DDL SQL
│   ├── stackConfiguration/       # Environment config
│   ├── lib-table-cluster/        # Table/view operations
│   ├── lib-table-query/          # SQL query parsing (JavaCC)
│   ├── securityUtilities/        # Encryption, security
│   ├── lib-utils/                # ValidateArgument, general utilities
│   ├── lib-worker/               # Worker framework
│   ├── lib-grid/                 # JSON-Joy CRDT model objects (CBOR encoding)
│   ├── lib-grid-db/              # Grid CRDT relational database persistence
│   └── ...                       # id-generator, database-semaphore, lib-upload, etc.
├── services/
│   ├── repository-managers/      # Business logic (Manager interfaces + impls)
│   ├── authutil/                 # Auth utilities
│   ├── repository/               # REST controllers (WAR)
│   └── workers/                  # Async workers (WAR)
├── client/                       # Java client libraries
└── integration-test/             # IT tests (embedded Tomcat)
```

## Architecture: Controller → Manager → DAO

### Controllers (services/repository/)
- Package: `org.sagebionetworks.repo.web.controller`
- `@Controller` + `@RequestMapping`, delegate to `ServiceProvider`
- User ID via `@RequestParam(AuthorizationConstants.USER_ID_PARAM)`
- OAuth scopes via `@RequiredScope`

### Managers (services/repository-managers/)
- Package: `org.sagebionetworks.repo.manager`
- Interface + Impl pattern (e.g., `EntityManager` / `EntityManagerImpl`)
- `@Service` on implementations, constructor injection preferred
- `@WriteTransaction` for write operations (from `org.sagebionetworks.repo.transactions`)
- Also: `@MandatoryWriteTransaction`, `@NewWriteTransaction`
- Input validation: `ValidateArgument.required(value, "fieldName")`

### DAOs (lib/jdomodels/)
- Interfaces in lib/models/: `org.sagebionetworks.repo.model`
- Implementations in lib/jdomodels/: `org.sagebionetworks.repo.model.dbo.dao`
- Spring `JdbcTemplate` / `NamedParameterJdbcTemplate`
- DBO classes implement `MigratableDatabaseObject`
- DDL loaded from classpath via `DDLUtilsImpl`

## Code Generation

- JSON schemas: `lib/lib-auto-generated/src/main/resources/schema/org/sagebionetworks/`
- Generated POJOs: `lib/lib-auto-generated/target/auto-generated-pojos/`
- Do NOT edit generated classes — edit the JSON schema, then rebuild

## Testing

- Unit tests: `*Test.java` — JUnit 5 + Mockito 2.27
  - `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`
- Integration tests: `IT*.java` (in integration-test module)
- Mockito 2.27 — no `mockStatic` or Mockito 4/5 APIs

## Deployment & Migration

- **Stack identity**: Each stack is identified by two `StackConfiguration` values: `stack` (dev or prod) and `instance` (developer name for dev, numeric for prod — e.g., prod-578)
- **Blue-green deployment**: Production uses two parallel stacks — a production stack (e.g., prod-578) and a staging stack (e.g., prod-580). Stacks are created by a separate project (Synapse-Stack-Builder) using CloudFormation.
- **Migration**: A custom migration client replicates data from production to staging by detecting etag differences, backing up changed rows to S3 as zipped XML, then restoring on the destination. Migration is driven by Primary DBO tables; Secondary tables are migrated automatically via their owner relationship.
- **URL swap**: When staging is validated, production goes read-only for a final migration pass, then CNAMEs are swapped to promote staging to production.

### Databases

Each stack has two MySQL databases:
- **Main (transactional) database**: All user-driven state changes. This is the only database that is migrated between stacks. All tables with `MigratableDatabaseObject` live here.
- **Index database**: Contains derived/computed constructs built from the main database (e.g., entity replication tables, materialized views for table queries). Starts empty on a new stack and is rebuilt via the change messaging system. Also includes secondary indexes like OpenSearch.

### Change Messaging System

The main database has two key tables that drive index construction:
- **CHANGES table** (`DBOChange`): Records every state change in the repository (object ID, type, change type, timestamp). This is a migratable table and is always the **last table migrated** from production to staging.
- **SENT_MESSAGES table** (`DBOSentMessage`): Records which change messages have been broadcast on this specific stack. This table does **NOT migrate** — it starts empty on each new stack.

**Post-migration index rebuild flow:**
1. During migration, the destination stack is in **read-only mode** — most workers are blocked
2. After migration completes, the stack is restored to **read-write mode**
3. `ChangeSentMessageSynchWorker` starts reconciling: it compares CHANGES vs SENT_MESSAGES using checksum-based range scanning to find unsent messages
4. For each unsent change, the worker publishes a batch to the appropriate **SNS topic** (one per `ObjectType`) via `RepositoryMessagePublisher`
5. SNS topics fan out to **SQS queues** — each worker type subscribes to its relevant queue
6. Workers consuming from their queues build the corresponding constructs in the index database and other secondary indexes (OpenSearch, etc.)

Key classes:
- `ChangeSentMessageSynchWorker` — `services/workers/src/main/java/org/sagebionetworks/change/workers/`
- `TransactionalMessengerImpl` — writes to CHANGES table on transaction commit
- `RepositoryMessagePublisher` — publishes change messages to SNS topics
- `DBOChange` / `DBOSentMessage` — `lib/jdomodels/src/main/java/org/sagebionetworks/repo/model/dbo/persistence/`

### DBO Migration Pattern

When creating new database tables, the DBO must implement `MigratableDatabaseObject<D, B>`:
- Provide a `MigrationType` (order matters — must come after dependencies)
- Provide a `MigratableTableTranslation` for backup/restore conversion
- Register primary types in `lib/jdomodels/src/main/resources/dbo-beans.spb.xml` (order matters)
- Secondary types are discovered automatically via `getSecondaryTypes()`
- Primary tables need an etag column (NOT NULL) for change detection; secondary tables need a foreign key to their owner's backup ID
- Key test: `services/repository/src/test/java/org/sagebionetworks/repo/web/migration/MigrationIntegrationAutowireTest.java` — extend this when adding new migratable types

### Moving Data Between Tables (cross-stack safe)

Use a two-stack rollout:
1. **Stack N**: Add data mirroring (write to both old and new table) + backfilling via `MigrationTypeListener` registered in `managers-spb.xml`
2. **Stack N+1**: Remove mirroring, switch reads to new table as source of truth

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

## Key Conventions

- Package root: `org.sagebionetworks`
- Branch naming: `PLFM-XXXX` (JIRA tickets)
- Main branch: `develop`
- Entity IDs: String-typed but numeric (`KeyFactory` converts)
- Spring config: mix of XML (`WEB-INF/` and `src/main/resources/*-spb.xml`) and annotations
- Logging: Log4j 2
- **JSON serialization**: Use `JDOSecondaryPropertyUtils.createJSONFromObject()` / `createObjectFromJSON()` for converting `JSONEntity` objects to/from JSON strings. Do not write custom serialization code.
- **SQL safety**: All SQL must use bind variables. Never concatenate strings into SQL. For generated values (UUIDs, timestamps), prefer MySQL functions (`UUID()`, `NOW(3)`) over Java-side generation.
- **Controller testing**: Use IT tests with the Java client in `integration-test/`, not autowired controller tests. Every new controller method needs a corresponding `SynapseClient`/`SynapseClientImpl` method and an IT test.

## Critical Constraints

1. **Java 11 only** — no var in lambdas, no records, no text blocks, no sealed classes
2. **javax namespace** — not jakarta.*
3. **Spring 5.3** — no Spring 6+ or Spring Boot APIs
4. **Mockito 2.27** — no mockStatic, no Mockito 4/5 features
5. **No Lombok**
6. **No Spring Data** — all DB via JdbcTemplate
7. **WAR packaging** — not executable JARs
8. **Migration-safe schema changes** — new DBO tables must implement `MigratableDatabaseObject` and be registered in `dbo-beans.spb.xml`; data moves between tables require a two-stack mirroring/backfill rollout
