# Synapse Repository Services

Backend platform for Sage Bionetworks' Synapse — a collaborative research data sharing platform.

## Tech Stack

- **Java 21 LTS**
- **Spring 6.x** (Spring MVC, Spring JDBC, Spring AOP) — NOT Spring Boot
- **jakarta.servlet / jakarta.annotation** — migrated from javax.* for Spring 6 compatibility
- **MySQL 8.x** via Spring JdbcTemplate (no ORM, no Spring Data)
- **Tomcat 10x** (WAR deployment)
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

## Maven Dependency Management

- **Dependency versions**: ALL dependency versions (including internal lib modules) MUST be defined in the root `pom.xml` `<dependencyManagement>` section
- **Sub-module poms**: Sub-module `pom.xml` files declare dependencies WITHOUT `<version>` tags — versions are inherited from the root
- **Why**: This ensures consistent versions across all modules and prevents version conflicts in the reactor build
- **Example**: When adding a new lib module (e.g., `lib-database-configuration`), add it to the root pom's `<dependencyManagement>` with `<version>${project.version}</version>`, then sub-modules can reference it with just `<groupId>` and `<artifactId>`

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
- **Mockito 5.x** — strict stubbing is enabled by default
  - **Functional/lambda parameters**: When mocking methods that accept functional interfaces (e.g., OpenSearch Java client's `search(Function<...>, Class)`), use `doAnswer()` to execute the lambda parameter. The lambda must be invoked to trigger validation logic inside it. See `OpenSearchManagerImplTest.stubSearchToExecuteLambda()` for the pattern.
  - **Varargs parameters**: When a method has varargs and the implementation passes an array, match with the array type. Example: for `method(String... keys)` called with `String[]`, use `any(String[].class)` not `any(String.class)`. For `method(IdAndVersion... ids)` called with `IdAndVersion[]`, use `any(IdAndVersion[].class)`.
  - **Overloaded methods**: When mocking overloaded methods, be explicit about which overload to match — using `any()` without type can cause ambiguous method reference errors.
  - **No lenient stubbing**: Lenient stubbing (`@MockitoSettings(strictness = Strictness.LENIENT)`) is not allowed in this codebase — fix argument matchers instead.
- **Test method naming**: `test<methodUnderTest>With<condition>` — e.g., `testCreateWithNonSageUser`, `testGetWithNonExistentId`, `testListWithMultipleOrganizations`. For IT CRUD lifecycle tests: `testCRUDWith<context>`.
- **Test method structure**: Mark the primary method being tested with a `// call under test` comment directly above it — this makes each test's intent immediately clear during review
- **Verify no downstream calls after exceptions**: After `assertThrows`, verify that mocked methods past the exception point were NOT called — use `verifyZeroInteractions(mock)` or `verify(mock, never()).method(...)`
- **Assert on whole objects**: Use `assertEquals(expected, actual)` on objects rather than comparing individual fields — generated POJOs have correct `equals()`/`hashCode()`. Only assert individual fields when testing a specific field transformation.
- **Include real data in tests**: Don't test CRUD with empty payloads. If a feature serializes data (e.g., JSON columns), include actual values in the test fixture and verify the round-trip — because a bug in serialization won't surface if the payload is empty.
- **List/filter tests need multiple groups**: When testing list/filter operations, create entries across at least 2 categories (e.g., 2 items in org1, 2 in org2). Verify each filtered list returns the correct subset AND verify ordering is deterministic — because a single-group test can pass even if filtering is broken.
- **Update tests must verify data changed**: Assert that updated values are present in the result, not just that metadata (etag) rotated — because an etag rotation doesn't prove the data write succeeded.

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
- Key test: `MigratableTableDAOImplAutowireTest.testAllMigrationTypesRegistered()` (`lib/jdomodels/src/test/java/org/sagebionetworks/repo/model/dbo/migration/MigratableTableDAOImplAutowireTest.java`) — validates all `MigrationType` values have registered DBOs

### Moving Data Between Tables (cross-stack safe)

Use a two-stack rollout:
1. **Stack N**: Add data mirroring (write to both old and new table) + backfilling via `MigrationTypeListener` registered in `managers-spb.xml`
2. **Stack N+1**: Remove mirroring, switch reads to new table as source of truth

## Async Jobs & Workers

See `services/workers/CLAUDE.md` for the async job framework, worker types, registration, trigger configuration, and SQS queue infrastructure.

### Renaming a Column (cross-stack safe)

When a DB column is renamed (e.g., `PROJECT_ID` → `OBJECT_ID`), the backup XML from production still serializes the **old Java field name**. Use a two-stack bridge pattern:

1. **Stack N** (this stack):
   - Update the DDL to use the new column name.
   - Add a new Java field with the new name (`objectId`) mapped to the new column via `FieldColumn("objectId", COL_NEW_NAME)`.
   - Keep the old Java field (`projectId`) as a temporary bridge — it has no `FieldColumn` mapping but is still deserialized from old backup XML.
   - In `MigratableTableTranslation.createDatabaseObjectFromBackup()`, copy the old field into the new field if the new field is null: `if (dbo.getObjectId() == null && dbo.getProjectId() != null) { dbo.setObjectId(dbo.getProjectId()); }`
   - All new code reads/writes the new field (`objectId`). The old field is only used by the translator.
2. **Stack N+1** (after production has the new column):
   - Remove the old bridge field (`projectId`) and the translator bridge logic. The new field is now the sole source of truth.

## Curation Grid (Curator)

See `services/repository-managers/CLAUDE.md` and `lib/lib-grid/CLAUDE.md` for the CRDT-based grid architecture, WebSocket protocol, and AI agent integration.

## Key Conventions

- **No wildcard imports** — use explicit imports (e.g., `import java.util.List;`), not `import java.util.*;`
- Package root: `org.sagebionetworks`
- Branch naming: `PLFM-XXXX` (JIRA tickets)
- Main branch: `develop`
- Entity IDs: String-typed but numeric (`KeyFactory` converts)
- Spring config: mix of XML (`WEB-INF/` and `src/main/resources/*-spb.xml`) and annotations
- Logging: Log4j 2
- **JSON serialization**: Use `JDOSecondaryPropertyUtils.createJSONFromObject()` / `createObjectFromJSON()` for converting `JSONEntity` objects to/from JSON strings. Do not write custom `ObjectMapper` or `JSONObjectAdapter` serialization code in DAO classes.
- **SQL safety**: All SQL must use bind variables. Never concatenate strings into SQL. For generated values (UUIDs, timestamps), prefer MySQL functions (`UUID()`, `NOW(3)`) over Java-side generation. For `DELETE` without specific criteria, always add `WHERE ID > -1` (required for SQL safe-updates mode).
- **SQL style**: Write SQL inline where it's used. Do not concatenate `SqlConstants` references into SQL query strings. Constants are appropriate in DDL, DBO field mappings, and row mappers — just not for building query strings.
- **Controller testing**: Use IT tests with the Java client in `integration-test/`, not autowired controller tests (`*AutowiredTest` classes). Every new controller method needs a corresponding `SynapseClient`/`SynapseClientImpl` method and an IT test. Deep logic checks belong in manager unit tests; IT tests just verify each HTTP call works.
- **Exception mapping**: `NumberFormatException` extends `IllegalArgumentException`, which maps to HTTP 400. It is acceptable to let it propagate without wrapping.
- **Reuse existing constants**: Before defining a new string constant, check if it already exists in a shared constants class (e.g., `SqlConstants`). Add new constants to the appropriate shared class rather than defining them locally.
- **Multi-value LIST columns**: Stored as JSON on the main table (`T<id>`) and unnested at query time via `JSON_TABLE(...)` — they are **not** separate physical index tables (that model was removed in PLFM-7968).

## Code Comments

- **Prioritize Expressive Code**: Write highly readable, self-documenting code as the primary means of explanation. Use comments exclusively to provide critical context that cannot be naturally expressed through clean naming conventions and clear structure.
- **Target the Audience (Javadocs vs. Inline)**: Match documentation placement to its specific consumer:
  - **Public API (Javadocs)**: Focus class and method Javadocs strictly on the public contract, defining the behavior, parameters, and return values expected by the caller at that specific level of abstraction.
  - **Internal Logic (Inline Comments)**: Place all underlying execution details, algorithmic mechanics, and internal complexities entirely within inline comments inside the implementation body.
- **Document Intent, Refactor Mechanics**: Dedicate internal comments to explaining the underlying business logic, constraints, and rationale behind the code (the Why). Allow the code architecture to explain the execution (the What). Treat any impulse to write step-by-step prose about what the code is doing as an immediate signal to refactor the code into clearer, smaller functions.
- **Current State Only**: Code comments and CLAUDE.md files should exclusively describe the current state, logic, and intent of the code.
  - Keep historical context, diff explanations, and "before vs. after" commentary entirely within planning documents, commit messages, PR descriptions, or narrowly scoped as comments that are co-located with specific regression tests.
  - Limit references to past logic strictly to active, ongoing code migration paths that directly impact current execution.
- **Stable References**: Code comments and CLAUDE.md files should use reference points that survive automated refactoring and ongoing codebase evolution.
  - Point to other code exclusively through language-supported dynamic links (like Javadoc {@link}) or external issue keys (like PLFM-1234).
  - Define target locations using conceptual names or programmable signatures instead of brittle options like absolute file paths or hard-coded line numbers.


## Critical Constraints

1. **Java 21 LTS** — Java 21 language features are now available (records, text blocks, pattern matching, sealed classes, virtual threads)
2. **jakarta namespace** — migrated from javax.* for Spring 6 compatibility
3. **Spring 6.1** — no Spring Boot APIs
4. **Mockito 2.27** — no mockStatic, no Mockito 4/5 features
5. **No Lombok**
6. **No Spring Data** — all DB via JdbcTemplate
7. **WAR packaging** — not executable JARs
8. **Migration-safe schema changes** — new DBO tables must implement `MigratableDatabaseObject` and be registered in `dbo-beans.spb.xml`; data moves between tables require a two-stack mirroring/backfill rollout
