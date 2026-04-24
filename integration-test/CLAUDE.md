# integration-test

End-to-end tests run against a live, embedded Tomcat + MySQL + AWS stack.
They verify HTTP wiring between the `SynapseClient` Java client and the `services/repository` + `services/workers` WARs.

## What IT tests are for

- Prove each new controller endpoint is wired correctly: one happy-path per endpoint.
- Prove client method signatures match controller expectations.
- Prove async job plumbing (start → poll → result).
- Prove cross-module wiring where only the live stack exposes the integration (e.g., Spring circular dependencies that don't surface in unit tests).

## What IT tests are NOT for

- **Deep branch coverage, error paths, edge cases** — those belong in manager unit tests (`*Test.java`) or autowired DAO tests (`*DaoImplAutowiredTest`). IT runs are expensive and flaky; every extra IT test slows the suite and adds variance.
- **Business logic assertions** — if an assertion would pass with mocks, move it to the unit layer.
- **Replacing `*AutowiredControllerTest`** — those mock the servlet layer and miss real controller bugs. IT is the ONLY reliable controller test; do not write `*AutowiredTest extends AbstractAutowiredControllerTestBase` for new work.

## Structure

```java
@ExtendWith(ITTestExtension.class)
public class ITMyFeatureTest {
    private SynapseAdminClient adminSynapse;
    private SynapseClient synapse;

    // Constructor injection via the extension
    public ITMyFeatureTest(SynapseAdminClient adminSynapse, SynapseClient synapse) {
        this.adminSynapse = adminSynapse;
        this.synapse = synapse;
    }

    @BeforeEach public void before() throws Exception { /* fixtures */ }
    @AfterEach  public void after()  throws Exception { /* cleanup */ }
}
```

`ITTestExtension` (injected via `@ExtendWith`) resolves `SynapseAdminClient`, `SynapseClient`, `StackConfiguration`, `WarehouseTestHelper`, and `AmazonS3` by parameter type.
The extension authenticates the admin client, enables 2FA on the admin user, and calls `adminSynapse.clearAllLocks()` in `@BeforeAll` — individual tests do NOT need to call `clearAllLocks()`.
A dynamic test user is created on first `SynapseClient` injection and deleted in `@AfterAll`.

## Key conventions

- **UUID-suffix every created resource name.** When the resource under test has no admin DELETE endpoint, fixed names fail with "same name already exists" on the second run against a populated dev DB. Pattern: `String unique = UUID.randomUUID().toString().replace("-", ""); String name = "IT_THING_" + unique;`. Required for SynonymSet, ColumnAnalyzerOverride, SearchConfiguration and other no-delete resources.
- **Clean up in `@AfterEach` for resources with DELETE endpoints** — entities, files, projects. Wrap each cleanup in a null check so a failed fixture setup doesn't mask the real failure.
- **Assume the DB is NOT clean.** Dev stack is shared. Tests must be idempotent against any residual data.
- **Full-object `assertEquals(expected, actual)` when the POJO has an `equals()` override** (all generated POJOs do). Preferred over field-by-field. Use field assertions only when verifying a specific transformation.
- **Poll async jobs via `AsyncJobHelper`** — shared helper at `integration-test/src/test/java/org/sagebionetworks/AsyncJobHelper.java`. Do not hand-roll poll loops.

## Anonymous-user lifecycle test setup

When exercising code paths that index or query as the realm's anonymous user (e.g., SearchIndex builds):

- Set the source table's data type to `OPEN_DATA` via `adminSynapse.changeEntityDataType(id, DataType.OPEN_DATA)`.
- Grant `AuthorizationConstants.BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP` `READ` access on the parent container.
- Grant `AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP` `DOWNLOAD` access if rows reference file handles.

Without these, the lifecycle worker's `addRowLevelFilter()` sees zero rows and the build completes with an empty index — the test passes vacuously.

## Naming

- `IT<Feature><Verb>Test.java` (e.g., `ITSearchQueryTest`, `ITSearchConfigurationTest`).
- Historical numeric prefixes (`IT510*`, `IT101*`) exist for ordering but are not required for new tests.
- Test methods follow the unit test convention: `test<Method>With<Condition>`. For full CRUD lifecycle tests: `testCRUDWith<context>`.

## When NOT to add an IT test

- A bug fix entirely inside a manager — add a unit test, not an IT test.
- A DAO SQL change — add a `*DaoImplAutowiredTest`, not an IT test.
- A schema-only change with no new endpoint — the existing IT coverage already verifies wiring.

## Running

```
mvn verify -f integration-test/pom.xml -Dit.test=ITMyFeatureTest
```

Requires a live dev stack URL resolvable via `StackConfiguration`.
