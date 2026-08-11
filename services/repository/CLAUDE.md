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

### One controller per resource family

A related family of resource types can share a single `@Controller`. Example: `SearchManagementController` serves TextAnalyzer, ColumnAnalyzerOverride, SynonymSet, SearchConfiguration, SearchConfigBinding, and the async search query/autocomplete endpoints — it replaced four separate controllers (`SearchQueryController`, `SynonymSetController`, `TextAnalyzerController`, `ColumnAnalyzerOverrideController`). Prefer consolidating a tightly related family over one controller per type.

## Servlet Filter Chain

Request filters are declared in `src/main/webapp/WEB-INF/web.xml`; **filter order is the `<filter-mapping>` declaration order and is load-bearing**. Some filter classes live in the `services/authutil` module (`SimpleCORSFilter`, `CookieSessionTokenFilter`) — that module has no CLAUDE.md of its own because its behavior is only meaningful in this chain. Current order: `trailingSlashRedirect → httpToHttps → unexpectedException → requestSizeThrottle → httpMethod → cookie → simpleCORS → stackStatus → authFilter → adminServiceAuth → … → throttles → …`.

Guardrails (each has bitten before — do not "clean up"):
- **Keep `unexpectedExceptionFilter` high in the chain, above the auth/business filters** — it is the last-chance handler that logs unexpected errors before they reach users (PLFM-3205/3206).
- **Do NOT add `/auth/v1/*` paths to `acceptTermsOfUseFilter` or `twoFactorAuthRequiredFilter`** `<url-pattern>`s — users must be able to accept the terms of use / enable 2FA before those filters would otherwise block them.
- **Do NOT broaden `cloudMailInAuthFilter` beyond `/cloudMailInMessage/*`** — CloudMailIn does not send BasicAuth on the `/cloudMailInAuthorization/*` endpoint, so the filter is deliberately scoped out of it.

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
