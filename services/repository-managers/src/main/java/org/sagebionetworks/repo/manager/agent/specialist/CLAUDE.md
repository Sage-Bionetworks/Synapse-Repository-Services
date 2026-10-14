# Specialist Sub-Agents

Multi-agent architecture where specialist agents handle context-heavy operations with focused tool sets. Each specialist is conversational (multi-turn), uses Haiku for cost efficiency, and writes results to the shared code interpreter session.

## Tool Conventions

### Required: extend `JSONEntityToolBase` — do NOT use Spring AI's `@Tool`

**All new tools MUST extend `JSONEntityToolBase` and annotate their methods with `@JSONEntityTool`** (and request parameters with `@JSONEntityToolParam`). Do **not** author new tools with Spring AI's native `@Tool` / `@ToolParam` annotations wired via `.defaultTools(...)`.

Spring AI's `@Tool` mechanism suffers from **PLFM-9801** and gives up three things the base class provides for free:

- **No logging.** `JSONEntityToolBase` wraps every callback in `LoggingToolCallback`, so each delegation is logged. `@Tool` tools registered via `.defaultTools(toolObject)` bypass that wrapper and produce no log output — the symptom that surfaces first when a `@Tool`-based tool is exercised.
- **No `concreteType`-aware deserialization.** The base deserializes the model's JSON through `JDOSecondaryPropertyUtils.createObjectFromJSON`, which understands Synapse's discriminated `oneOf` unions. Spring AI's plain Jackson mapper does not, so polymorphic request POJOs (e.g. `Filter` / `SelectItem` unions) fail to bind.
- **No correct-and-retry.** On a malformed argument the base returns a plain error string that Spring AI feeds back to the model so it can self-correct on its next turn. `@Tool` throws instead, disrupting the agent loop.

A tool class extends the base, annotates each method with `@JSONEntityTool`, and calls `super()` in its constructor; the base generates the native `inputSchema` for each method and wires it as a logged `ToolCallback`. A tool method's arguments take one of two shapes — the base picks the schema and marshalling from the method signature:

- **Structured request body** — a single `@JSONEntityToolParam` `JSONEntity` parameter. The whole tool input deserializes into that POJO via the `concreteType`-aware path, and the `inputSchema` is generated from the POJO's type (this is the only shape that supports `oneOf` unions). A body parameter that must preserve the undefined-vs-null distinction can instead be a raw `JSONObject`/`String` whose `schemaType()` names the type to advertise (see `GridUpdateTools`).
- **Scalar / no argument** — zero or more plain `@JSONEntityToolParam` scalar parameters (`String`/`Long`/`Integer`/`Boolean`/`Double`), each becoming a named top-level property (bound by parameter name — the build enables `-parameters`). A method that takes only `ToolContext` gets a valid empty-object schema. This shape needs **no request-POJO schema** — prefer it when the arguments are simple scalars.

```java
@Service
public class GridQueryTools extends JSONEntityToolBase {          // structured request body

    @JSONEntityTool(name = "queryGrid", description = "Run a structured query against the current grid session ...")
    public ToolResponse<QueryResult> queryGrid(
            @JSONEntityToolParam(description = "The query to run against the current grid session.",
                    required = true) QueryRequest request,
            ToolContext toolContext) { ... }
}

@Service
public class GridMetadataSpecialistTools extends JSONEntityToolBase {   // scalar / no argument

    @JSONEntityTool(name = "getReplicaInfo", description = "Look up a single replica by its replicaId ...")
    public ToolResponse<GridReplicaInfo> getReplicaInfo(
            @JSONEntityToolParam(description = "The replicaId to look up", required = true) Long replicaId,
            ToolContext toolContext) { ... }
}
```

Wire these into a specialist's `ChatClient` via `.defaultToolCallbacks(tools.getToolCallbacks())` (NOT `.defaultTools(...)`). When a request type contains `oneOf` interface unions, override `getPolymorphicImplementerSeeds()` to feed the schema generator each interface's `InstanceFactory.singleton().getKeySetIterator()`.

Add a `List` argument or body as a request POJO with a single array property (the standard Synapse shape — see the list-of-objects convention in `lib/lib-auto-generated/CLAUDE.md`), then take that POJO as the tool's single `@JSONEntityToolParam` structured request body. Do not declare a bare `List<T>` tool parameter.

> **Legacy only:** `SupervisorTools` and other older `@Tool`-based classes predate this rule. Do not copy them for new tools; migrate them onto `JSONEntityToolBase` when touched (PLFM-9801).

### `JSONEntityResultConverter` for rich return types

Tools that return Synapse domain objects (POJOs implementing `JSONEntity`) are serialized via the `JSONEntityResultConverter`. `JSONEntityToolBase` applies this converter to every tool's return value automatically — there is nothing to configure on the annotation:

```java
@JSONEntityTool(name = "queryTable", description = "...")
public ToolResponse<QueryResultBundle> queryTable(...) { ... }
```

The converter serializes the return value via `JDOSecondaryPropertyUtils.createJSONFromObject()` — the canonical Synapse JSON serialization path. The LLM receives the full JSON structure and can reason over counts, facets, column metadata, etc. For simple string responses (error messages, confirmations), a tool can return `String`, which is passed through unchanged.

### `ToolContext` pattern

All tools receive `ToolContext` as their last parameter (unannotated — `JSONEntityToolBase` binds it automatically). Extract user and session through the `AgentToolContextKey` enum — the single source of truth for every tool-context key — rather than bare string literals:

```java
UserInfo userInfo = (UserInfo) AgentToolContextKey.USER_INFO.get(toolContext);
String sessionId = CodeSessionSupplier.resolveSessionId(toolContext);
```

`CodeSessionSupplier.resolveSessionId(...)` invokes the `CodeSessionSupplier` installed under `AgentToolContextKey.CODE_SESSION_SUPPLIER` — a lazy, memoizing supplier on the interactive Curie path, or a constant supplier over an already-started session (`CodeSessionSupplier.of(sessionId)`) on the batch and delegated-specialist paths. Never read a session-id key directly; always resolve through this method.

### `ToolResponse<T>` for structured results with error handling

Tools that return rich domain objects should wrap them in `ToolResponse<T extends JSONEntity>`. This provides a uniform JSON envelope that the LLM can parse:

```java
@JSONEntityTool(name = "queryTable", description = "...")
public ToolResponse<QueryResultBundle> queryTable(...) {
    try {
        QueryResultBundle result = ...;
        return new ToolResponse<>(result);       // {"responseBody": {...}}
    } catch (Exception e) {
        return new ToolResponse<>(e.getMessage()); // {"errorMessage": "..."}
    }
}
```

`ToolResponse` itself implements `JSONEntity` and serializes as either `{"responseBody": <T as JSON>}` on success or `{"errorMessage": "..."}` on failure. This allows tools to return meaningful error messages without throwing exceptions that disrupt the agent loop.

### Authorization

All tool methods must verify the user has access before returning data. Internal utilities like `TableManagerSupport.getTableSchema()` do NOT check authorization — they are designed for system-internal use. Agent tools must call an authorization-checked method (e.g., `EntityManager.getEntity(userInfo, id)`) before returning any entity metadata. An agent must never leak information the user cannot access directly via the REST API.

## Factory Pattern

Specialists are created via a `@Service` factory. The specialist instance itself is NOT a Spring bean — it holds per-conversation state (ChatMemory):

```java
@Service
public class TableQuerySpecialistFactory {
    public TableQuerySpecialist create() { ... }
}
```

## System Prompts

Stored as Velocity templates (`.vtp`) in `src/main/resources/prompts/`. The factory renders them at creation time, merging in dynamic content (e.g., SQL reference examples from classpath CSVs).
