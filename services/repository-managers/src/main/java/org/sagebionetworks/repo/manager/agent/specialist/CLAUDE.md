# Specialist Sub-Agents

Multi-agent architecture where specialist agents handle context-heavy operations with focused tool sets. Each specialist is conversational (multi-turn), uses Haiku for cost efficiency, and writes results to the shared code interpreter session.

## Tool Conventions

### `@ToolParam` for all parameters

Every tool method parameter (except `ToolContext`) must use `@ToolParam` with both `description` and `required`:

```java
@Tool(description = "...", resultConverter = JSONEntityResultConverter.class)
public QueryResultBundle queryTable(
        @ToolParam(description = "A complete Synapse SQL query", required = true) String sql,
        @ToolParam(description = "Max rows to return (capped at 100)", required = false) Long limit,
        ToolContext toolContext) {
```

### `JSONEntityResultConverter` for rich return types

Tools that return Synapse domain objects (POJOs implementing `JSONEntity`) use the `JSONEntityResultConverter`:

```java
@Tool(description = "...", resultConverter = JSONEntityResultConverter.class)
public QueryResultBundle queryTable(...) { ... }
```

This serializes the return value via `JDOSecondaryPropertyUtils.createJSONFromObject()` — the canonical Synapse JSON serialization path. The LLM receives the full JSON structure and can reason over counts, facets, column metadata, etc.

For simple string responses (error messages, confirmations), tools can still return `String` with the default converter.

### `ToolContext` pattern

All tools receive `ToolContext` as their last parameter (not annotated with `@ToolParam`). Extract user and session:

```java
UserInfo userInfo = (UserInfo) toolContext.getContext().get("userInfo");
String sessionId = (String) toolContext.getContext().get("sessionId");
```

### `ToolResponse<T>` for structured results with error handling

Tools that return rich domain objects should wrap them in `ToolResponse<T extends JSONEntity>`. This provides a uniform JSON envelope that the LLM can parse:

```java
@Tool(description = "...", resultConverter = JSONEntityResultConverter.class)
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
