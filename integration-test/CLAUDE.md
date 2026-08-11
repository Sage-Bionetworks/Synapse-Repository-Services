# integration-test

End-to-end tests that exercise the real HTTP API through the Synapse Java client against a running Tomcat with both WARs deployed. This is where controller wiring is verified (deep logic is covered by manager unit tests). Module has no production code (`src/main` is a placeholder); all tests live flat under `src/test/java/org/sagebionetworks/`.

## Running

- **`mvn verify`** (from this module, or `mvn verify -pl integration-test` from root) runs the ITs.
- ITs are `IT*.java`, run by the **maven-failsafe** plugin in the `verify` lifecycle. **`mvn test` does NOT run them** — surefire explicitly excludes `IT*.java`.
- The server is a real **Tomcat 10** started by the **cargo** plugin (NOT an in-process embedded server) at `pre-integration-test`, stopped at `post-integration-test`. It deploys both `services-repository` and `services-workers` WARs. Cargo passes ~40 `org.sagebionetworks.*` system properties (stack/instance, AWS creds, DB URLs, table-cluster endpoints, etc.) into the container — a properly configured dev stack must supply these or dependent tests fail/skip. ITs are **not hermetic**.
- **Debug against a running server**: set `-Dorg.sagebionetworks.integration.debug=true` so cargo starts the WARs and waits, letting you run individual ITs from an IDE.

## Test structure

- Naming: new tests are `IT<Feature>Test` / `IT<Feature>ControllerTest`; legacy numbered names (`IT500SynapseJavaClient`) coexist.
- Non-`IT*` classes in the package are **shared helpers, not tests**: `SynapseClientHelper`, `AsyncJobHelper`, `OAuthHelper`, `AccessRequirementUtil`, `EmailValidationUtil`, `ITTestExtension`.
- Fixtures live in `src/test/resources/` (`docs/`, `images/`, `SmallTextFiles/`, ...), loaded via the classloader (note the `.replaceAll("%20", " ")` workaround for spaces in resource paths).

## Clients and users — use the extension, don't hand-roll

Annotate the class `@ExtendWith(ITTestExtension.class)`. The extension injects by **type** into constructor / `@BeforeAll` / `@BeforeEach` / `@Test` params:

- `SynapseAdminClient` — admin client.
- `SynapseClient` — a client for a freshly created, auto-**certified** test user (created lazily).
- `StackConfiguration`, `WarehouseTestHelper`, `AmazonS3`.

The extension bootstraps the admin (basic-auth → bearer token, **ensures admin 2FA is enabled**, then `clearAllLocks()`) in `beforeAll` and **auto-deletes the extension's test user** in `afterAll`. `SynapseClientHelper.createUser(admin, client)` creates additional users (random UUID identity, marked certified).

- **Extra users you create yourself must be torn down yourself** — static `SynapseClient` + `Long userToDelete`, created in `@BeforeAll`, deleted in `@AfterAll` inside try/catch (see `ITGridControllerTest`).
- **Entity cleanup is manual**: keep a `List<Entity> entitiesToDelete`, populate on create, delete each in `@AfterEach`.
- Call `adminSynapse.clearAllLocks()` in `@BeforeEach` if your feature acquires semaphore locks (tables, migration, async) — leftover locks are the top source of cross-test flakiness. Tests that mutate stack status must reset it to `READ_WRITE` in both `@BeforeEach` and `@AfterEach`.

## Calling the service

- All HTTP goes through `SynapseClient`/`SynapseClientImpl` (user) or `SynapseAdminClient`/`SynapseAdminClientImpl` (admin) from the `synapseJavaClient` dependency. Every new controller endpoint needs a client method + an IT that exercises it.
- For endpoints intentionally absent from the Java client, use raw `SimpleHttpClient` against `synapse.getRepoEndpoint()` (see `ITUnsupportedMethodTest`, the only class not using the extension).

## Gotchas

- **Async jobs — always poll, never assume immediate results.** Use `AsyncJobHelper.assertAysncJobResult(client, AsynchJobType, request, assertions, timeoutMs, maxRetries)` (note the sic spelling `Aysnc`). It polls with backoff, swallows `SynapseResultNotReadyException`, and re-submits up to `maxRetries`; use `AsyncJobHelper.INFINITE_RETRIES` for eventual-consistency-heavy jobs (grid, tables). `assertQueryBundleResults(...)` is the table-query convenience.
- **Eventual consistency**: previews, search indexing, DOI, and message delivery are async — never assert on them synchronously after the create call. Poll with `TimeUtils.waitFor(...)` or the `waitForPreviewToBeCreated`/`waitForQuery`/`waitForMessage` helpers.
- **WebSocket/grid**: `AsyncJobHelper.createConnection(presignedUrl, queue)` opens a client pushing messages onto a `BlockingQueue`; `waitForMessage(...)` polls with a 10s timeout.
- **Ordering**: failsafe runs classes alphabetically — never rely on cross-class ordering.
- **Hand-built clients must be certified** — a `SynapseClientImpl` you construct directly must be marked certified or entity/upload calls fail (the extension/`createUser` users already are).

Exemplars to copy: `ITGridControllerTest` (full lifecycle: injection, second user, async `INFINITE_RETRIES`, WebSocket, ACL grant), `IT101Administration` (admin + stack status + `clearAllLocks`), `ITUnsupportedMethodTest` (raw HTTP, no extension).
