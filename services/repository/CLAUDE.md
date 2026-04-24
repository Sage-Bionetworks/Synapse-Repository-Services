# services/repository

REST controller WAR — the HTTP API layer for Synapse. Controllers handle request routing, parameter binding, and OAuth scope enforcement, then delegate all business logic to managers via `ServiceProvider`.

## Controller Pattern

```java
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class EntityController {
    @Autowired
    ServiceProvider serviceProvider;

    @RequiredScope({OAuthScope.view})
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = UrlHelpers.ENTITY_ID, method = RequestMethod.GET)
    public @ResponseBody Entity getEntity(
            @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
            @PathVariable String id) {
        return serviceProvider.getEntityService().getEntity(userId, id);
    }
}
```

### Key Conventions

- **User identity**: `@RequestParam(AuthorizationConstants.USER_ID_PARAM) Long userId` — injected by auth filter, not from client
- **OAuth scopes**: `@RequiredScope({OAuthScope.view})` on every endpoint
- **Delegation**: Controllers never contain business logic — always delegate to `ServiceProvider` → service → manager
- **URL paths**: Constants in `UrlHelpers`
- **Response**: `@ResponseBody` + `@ResponseStatus(HttpStatus.OK/CREATED/NO_CONTENT)`
- **Request body**: `@RequestBody` for POST/PUT payloads
- **PUT endpoint ID validation**: When a PUT endpoint has `{id}` in the URL path AND an `id` field in the request body, the controller must validate they match or set the body's ID from the path variable before forwarding to the service. This prevents callers from updating the wrong resource via a mismatched body:
  ```java
  @RequestMapping(value = "/resource/{id}", method = RequestMethod.PUT)
  public @ResponseBody MyResource update(
          @PathVariable String id,
          @RequestBody MyResource request) {
      request.setId(id);  // Override body ID with path ID
      return serviceProvider.getMyService().update(userId, request);
  }
  ```

## ServiceProvider

`org.sagebionetworks.repo.service.ServiceProvider` — facade that exposes all service interfaces. Controllers inject this single bean rather than individual managers.

## Spring Configuration

- `web.xml` → `ContextLoaderListener` loads root context, `DispatcherServlet` loads MVC context
- Spring XML configs in `src/main/webapp/WEB-INF/` and `src/main/resources/`
- Component scan discovers `@Controller` classes

## WAR Deployment

- **Local/test**: Deployed alongside the workers WAR in the same embedded Tomcat instance
- **Production**: Deployed to its own **Elastic Beanstalk** Tomcat cluster (separate from workers)

## Testing

- Unit tests: Mock `ServiceProvider` and verify delegation
- **Do NOT write autowired controller tests** (`*AutowiredTest` extending `AbstractAutowiredControllerTestBase`). These mock the servlet layer and have repeatedly failed to catch real controller bugs.
- **DO write IT-level integration tests** in `integration-test/src/test/java/` that use the `SynapseClient` Java client to make real HTTP requests against Tomcat. These are the only reliable way to verify controller wiring.
- IT tests should cover all endpoints with basic happy-path verification. Deep branch coverage is handled by manager unit tests. Follow the pattern in existing IT tests (e.g., `ITGridControllerTest.java`).
- Migration test: `MigratableTableDAOImplAutowireTest.testAllMigrationTypesRegistered()` — validates all `MigrationType` values have registered DBOs
- **New controller endpoints**: Every new controller method needs a corresponding method in `SynapseClient`/`SynapseClientImpl` and an IT test in `integration-test/`.
- **IT tests are expensive — push business-logic checks to the manager layer.** An IT test only proves HTTP wiring works (one happy-path per endpoint). Deep branch coverage, error paths, and edge cases belong in manager unit tests and `*DaoImplAutowiredTest`. If an assertion would pass with mocks, it does not belong in an IT test.
- **UUID-suffix resource names in IT tests when there is no admin DELETE endpoint.** Fixed names fail with "same name already exists" on the second run against a populated dev DB. See `integration-test/CLAUDE.md` for the full pattern.

## Stubbing endpoints for parallel UI development

When the backend is blocking UI work, ship a stub-first PR: schemas + controllers that return mock data directly, with no service/manager/DAO chain behind them. This unblocks the UI against real endpoints and keeps the API contract stable. Replace the stub in a follow-up PR without changing the wire shape. See root `CLAUDE.md` → PR & Process Patterns.
