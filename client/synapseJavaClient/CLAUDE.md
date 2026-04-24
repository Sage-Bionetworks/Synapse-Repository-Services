# client/synapseJavaClient

Java HTTP client for the Synapse REST API. Every controller endpoint in `services/repository` has (or should have) a matching method here, plus an IT test in `integration-test/`.

## Core pattern

```java
// SynapseClient — interface
public interface SynapseClient {
    MyResponse doThing(MyRequest request) throws SynapseException;
    AsyncJobId startMyAsyncJob(MyAsyncRequest request) throws SynapseException;
    MyAsyncResponse getMyAsyncJobResults(String asyncToken) throws SynapseException;
}

// SynapseClientImpl — implementation
@Override
public MyResponse doThing(MyRequest request) throws SynapseException {
    String url = getRepoEndpoint() + "/resource/" + request.getId();
    return postJSONEntity(url, request, MyResponse.class);
}
```

- Every new controller method requires a matching `SynapseClient` interface method AND a `SynapseClientImpl` implementation.
- Every new client method requires a corresponding `IT*Test` in `integration-test/` — one happy-path call is enough to prove wiring.
- Return types are generated POJOs from `lib-auto-generated` — do not create client-only wrapper types.

## Async job client pattern

Async job type must be registered in BOTH places — keep the enum name identical:

1. **Server-side**: `lib/jdomodels/src/main/java/org/sagebionetworks/repo/model/dbo/asynch/AsynchJobType.java`
2. **Client-side**: `client/synapseJavaClient/src/main/java/org/sagebionetworks/client/AsynchJobType.java`

Standard method signatures:

```java
// Start (returns AsyncJobId)
AsyncJobId startMyJob(MyRequest request) throws SynapseException;

// Poll (returns response body; throws if still running)
MyResponse getMyJobResults(String asyncToken) throws SynapseException;
```

For synchronous endpoints (e.g., autocomplete), return the response body directly — no start/poll split.

## Signing up new endpoints

- URL paths go through helper methods (`getRepoEndpoint()`, etc.) — don't hand-write the host.
- Use `getJSONEntity` / `postJSONEntity` / `putJSONEntity` / `deleteUri` methods on `BaseClient`; they handle serialization and error mapping.
- PUT endpoints with `{id}` in the URL: the controller sets `request.setId(id)` before forwarding to the service — the client method's signature should surface the ID separately (typically as a constructor arg to the request object the caller builds).

## Compile target

Compiled to **Java 8** for compatibility with downstream consumers (SWC — Synapse Web Client, R/Python bindings via JNI-adjacent builds). Do not use Java 9+ APIs here:

- No `List.of(...)` / `Map.of(...)` (use `Arrays.asList` / `Collections.unmodifiableMap(new HashMap<>())`).
- No `var` local-variable inference.
- No `Optional.isEmpty()` (use `!optional.isPresent()`).
- No `String.isBlank()` (use `trim().isEmpty()`).

## Exception mapping

Server-side exception types map to specific client exceptions via status code:

| HTTP | Client exception |
|------|------------------|
| 400 | `SynapseBadRequestException` |
| 401 | `SynapseUnauthorizedException` |
| 403 | `SynapseForbiddenException` |
| 404 | `SynapseNotFoundException` |
| 409 | `SynapseConflictingUpdateException` |
| 429 | `SynapseTooManyRequestsException` |

Do not add new exception types unless there's a new HTTP status with distinct semantics.

## Testing

- Unit tests for `SynapseClientImpl` are thin — the real verification is IT-level.
- Don't mock `HttpClient` to test the client; the integration test covers serialization + network layer together.
- Add IT tests in `integration-test/` following the conventions in `integration-test/CLAUDE.md`.

## Build

```
mvn clean install -pl client/synapseJavaClient -DskipTests
```
