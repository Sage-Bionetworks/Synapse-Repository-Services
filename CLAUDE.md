# Synapse Repository Services

Backend platform for Sage Bionetworks' Synapse — a collaborative research data sharing platform.

Most modules (and some packages) have their own `CLAUDE.md` with module-specific patterns, gotchas, and anti-patterns. Read it when working in that module. This file covers only what applies everywhere.

## Tech Stack

- **Java 21 LTS**: records, text blocks, pattern matching, sealed classes, and virtual threads are available
- **Spring 6.x** (MVC, JDBC, AOP). **NOT Spring Boot**, so no Boot APIs
- **jakarta.servlet / jakarta.annotation** (not javax.*)
- **MySQL 8.x** via Spring `JdbcTemplate`. **No ORM, no Spring Data**
- **Tomcat 10.x**, **WAR packaging** (not executable JARs)
- **Jackson 2.20.0**, Log4j 2, Guava 30.1.1
- **AWS SDK v1** (1.12.x) + **v2** (2.40.x), Google Cloud Storage. v1 is being phased out client-by-client (`AwsClientFactory` → `AwsClientFactoryV2` in `lib/stackConfiguration`)
- **Spring AI**: Bedrock Converse + AgentCore code interpreter for the AI agent feature (see `services/repository-managers` agent `CLAUDE.md` files)
- **No Lombok**

## Build Commands

```
mvn clean install -DskipTests                           # Full build
mvn clean install -pl <module-path> -DskipTests          # Single module
mvn test -pl <module-path>                               # Unit tests for module
mvn test -pl <module-path> -Dtest=<TestClassName>        # Single test class
```

## Maven Dependency Management

ALL dependency versions (including internal lib modules) are defined in the root `pom.xml` `<dependencyManagement>`. Sub-module poms declare dependencies **without** `<version>`. A new lib module is added to the root `<dependencyManagement>` with `<version>${project.version}</version>`.

## Module Structure

```
platform (root)
├── lib/                          # 25+ shared libraries
│   ├── lib-auto-generated/       # JSON schema → POJO (schema-to-pojo plugin)
│   ├── models/                   # DAO interfaces
│   ├── jdomodels/                # DAO implementations, DBO classes, DDL SQL (main DB)
│   ├── lib-table-cluster/        # Index database: table/view operations
│   ├── lib-table-query/          # SQL query parsing (JavaCC)
│   ├── lib-database-configuration/ # DataSource/JdbcTemplate beans + @WriteTransaction annotations
│   ├── stackConfiguration/       # Environment config
│   ├── lib-utils/                # ValidateArgument, general utilities
│   ├── lib-worker/               # Worker framework
│   ├── lib-grid/, lib-grid-db/   # Curation grid CRDT model + persistence
│   └── ...                       # securityUtilities, id-generator, lib-docusign, lib-upload, etc.
├── services/
│   ├── repository-managers/      # Business logic (Manager interfaces + impls)
│   ├── repository/               # REST controllers (WAR)
│   ├── workers/                  # Async workers (WAR)
│   └── authutil/                 # Auth utilities
├── client/                       # Java client libraries
└── integration-test/             # IT tests (embedded Tomcat)
```

## Architecture: Controller → Manager → DAO

- **Controllers** (`services/repository`, `org.sagebionetworks.repo.web.controller`) delegate to `ServiceProvider` → service → manager. No business logic.
- **Managers** (`services/repository-managers`, `org.sagebionetworks.repo.manager`): Interface + `@Service` Impl, constructor injection, `@WriteTransaction` family for writes, `ValidateArgument.required(value, "fieldName")` for input validation.
- **DAOs**: interfaces in `lib/models` (`org.sagebionetworks.repo.model`), implementations in `lib/jdomodels` (`org.sagebionetworks.repo.model.dbo.dao`).

## Code Generation

- JSON schemas: `lib/lib-auto-generated/src/main/resources/schema/org/sagebionetworks/`
- Generated POJOs: `lib/lib-auto-generated/target/auto-generated-pojos/`
- Do NOT edit generated classes. Edit the JSON schema, then rebuild

## Testing

- Unit tests: `*Test.java`, JUnit 5 + Mockito 5.x (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`)
- Integration tests: `IT*.java` in `integration-test/` (see its `CLAUDE.md`)
- **Mockito strict stubbing is on. Lenient stubbing (`Strictness.LENIENT`) is not allowed**, so fix argument matchers instead.
  - **Functional/lambda parameters**: use `doAnswer()` to invoke the lambda so the logic inside it runs (pattern: `OpenSearchManagerImplTest.stubSearchToExecuteLambda()`).
  - **Varargs**: when the implementation passes an array, match the array type (`any(String[].class)`, not `any(String.class)`).
  - **Overloaded methods**: use typed matchers. Untyped `any()` can be ambiguous.
- **Test method naming**: `test<methodUnderTest>With<condition>` (e.g., `testGetWithNonExistentId`). IT CRUD lifecycle tests: `testCRUDWith<context>`.
- **Mark the call under test** with a `// call under test` comment directly above it.
- **After `assertThrows`**, verify that downstream mocks were NOT called (`verifyZeroInteractions` / `verify(mock, never())`).
- **Assert on whole objects** with `assertEquals(expected, actual)`. Generated POJOs have correct `equals()`/`hashCode()`.
- **Include real data**: don't test CRUD with empty payloads, because serialization bugs hide behind empty values.
- **List/filter tests need multiple groups** (at least 2 categories × 2 items) and deterministic ordering, because a single-group test passes even if filtering is broken.
- **Update tests must verify the data changed**, not just that the etag rotated.

## Deployment, Databases & Migration

- **Stacks**: identified by `StackConfiguration` `stack` (dev/prod) + `instance` (developer name, or numeric for prod). Production uses blue-green: a staging stack is built in parallel (by Synapse-Stack-Builder), data is migrated into it, then CNAMEs are swapped.
- **Two databases per stack**:
  - **Main (transactional)**: `lib/jdomodels`. The only DB that is migrated between stacks.
  - **Index**: `lib/lib-table-cluster`. Derived; starts empty on a new stack and is rebuilt from change messages (see `services/workers/CLAUDE.md`, `ChangeSentMessageSynchWorker`).
- **New tables** must implement `MigratableDatabaseObject` and live in a package scanned by `DboAutoDiscovery`. A DBO elsewhere is silently never migrated. See `lib/jdomodels/CLAUDE.md`.
- **Moving or renaming persisted data** requires a two-stack rollout. See the `dbo/migration` `CLAUDE.md` in `lib/jdomodels`.

## Async Jobs & Workers

See `services/workers/CLAUDE.md`.

## Curation Grid (Curator)

See `services/repository-managers/CLAUDE.md` and `lib/lib-grid/CLAUDE.md`.

## Key Conventions

- **No wildcard imports**
- Package root: `org.sagebionetworks`. Branch naming: `PLFM-XXXX` (JIRA). Main branch: `develop`
- Entity IDs: String-typed but numeric (`KeyFactory` converts)
- Spring config: mix of XML (`WEB-INF/`, `*-spb.xml`) and annotations. Do not add new XML configs
- Logging: Log4j 2
- **SQL**: always use bind variables, never concatenate strings into SQL, and write SQL inline where it's used (see `lib/jdomodels/CLAUDE.md` for SQL and JSON-column patterns)
- **New controller methods** need a `SynapseClient`/`SynapseClientImpl` method + an IT test (not `*AutowiredTest` controller tests). Deep logic checks belong in manager unit tests.
- **Reuse existing constants**: check shared constants classes (e.g., `SqlConstants`) before defining a new one

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
