# client/synapseJavaClient

The Java REST client SDK for the Synapse platform. Consumed by `integration-test/` and external callers. Source under `src/main/java/org/sagebionetworks/client/`.

## Class hierarchy

Each impl extends the one above; add methods at the layer that matches their scope:

- `BaseClient` / `BaseClientImpl` — auth, endpoints, and the low-level HTTP transport. All the `*JSONEntity` / `getJson` / download / upload helper methods live in `BaseClientImpl`. New transport helpers go here.
- `SynapseClient` / `SynapseClientImpl` — the public repo/file API surface and all URL path constants.
- `SynapseAdminClient` / `SynapseAdminClientImpl` — admin-only ops (migration, stack status, change messages, features).

Support types: `ClientUtils` (URL building + status→exception mapping), `Method` enum, `RestEndpointType` enum (`repo`, `file`, `auth` — **no `drs`**), `AsynchJobType` (async registry). The wire is touched only by the separate `client/simpleHttpClient` module (`SimpleHttpClient`); go through `BaseClientImpl` helpers, never call it directly from `SynapseClientImpl`.

## Adding an endpoint method

Two-file change (admin variants go in the admin client/impl instead):

1. Add the signature + Javadoc to the `SynapseClient` interface, `throws SynapseException`.
2. Add the `@Override` impl to `SynapseClientImpl`, and add the URL as a `private static final String` constant near the others (constants are often composed, e.g. `SUBMISSION_BUNDLE = SUBMISSION + BUNDLE`). Guard args with `ValidateArgument.required(x, "x")`.

The impl body is almost always a one-liner delegating to a `BaseClientImpl` helper, chosen by verb + body shape. Every helper takes `(endpoint, uri, ...)` where `endpoint` is `getRepoEndpoint()` / `getFileEndpoint()` / `getAuthEndpoint()` / `getDrsEndpoint()`:

- `getJSONEntity(endpoint, uri, Type.class)` — GET returning a POJO
- `postJSONEntity` / `putJSONEntity(endpoint, uri, body, Type.class)` — POST/PUT with body → POJO
- `voidPost` / `voidPut` — POST/PUT with no response body
- `deleteUri` / `putUri` — DELETE / bodyless PUT
- `getPaginatedResults(endpoint, uri, Type.class)` → `PaginatedResults<T>`
- `getListOfJSONEntity`, `getBooleanResult`, `getStringDirect`, `getUrl` (follows a redirect → pre-signed URL)

Serialization is automatic via `EntityFactory` — request POJOs implement `JSONEntity` and are imported from `org.sagebionetworks.repo.model.*` (auto-generated in `lib-auto-generated`; do NOT create them here). Never hand-write JSON.

## Async job endpoints

1. Add an `AsynchJobType` enum entry: `MyJob("/my/path", MyResponse.class, RestEndpointType.repo)` (response extends `AsynchronousResponseBody`). If the request `HasEntityId`, the URL is auto-prefixed with `/entity/{id}`.
2. Add a `String startXxx(req)` calling `startAsynchJob(type, req)` (returns a token) and a `MyResponse getXxxResults(token)` casting `getAsyncResult(type, token, ...)`. Polling primitive `getAsynchJobResponse` throws `SynapseResultNotReadyException` (HTTP 202) until ready; callers loop.

## Errors and retry

- Status→exception mapping is centralized in `ClientUtils.throwException(status, reason)`: 401→Unauthorized, 403→Forbidden, 404→NotFound, 400→BadRequest, 423→Locked, 412→ConflictingUpdate, 410→Deprecated, 429→TooManyRequests, else→`UnknownSynapseServerException`. To add a mapping, add the branch here + a subclass under `client/exceptions/` extending `SynapseServerException`.
- **Retry is automatic** in `BaseClientImpl.performRequestWithRetry` (503, 429, `SocketTimeoutException`, up to 5× with exponential backoff). Do NOT add ad-hoc retry loops.

## Gotchas

- **Pick the right base endpoint.** File/bulk-download → `getFileEndpoint()`, auth → `getAuthEndpoint()`, DRS → `getDrsEndpoint()`, else `getRepoEndpoint()`. The `AsynchJobType`'s `RestEndpointType` must match or the async call hits the wrong host.
- **DRS quirk**: `RestEndpointType` has no `drs` case (`getEndpointForType` throws on it) — DRS methods call `getJSONEntity(getDrsEndpoint(), ...)` directly and cannot use the async machinery.
- **Prefer POST-with-request-body over long query strings** for list/paginated endpoints (the codebase is moving to request-body pagination with an opaque `nextPageToken`; see the `listEDucTemplates` refactor). If the request POJO has a `nextPageToken` field, don't also append it as a query param.
- **Reuse existing `V2` path constants** rather than inventing new strings for versioned resources.
- **Never build multipart/file transfer by hand** — use `downloadFromSynapse` / `getUrl` / `putFileToURL` / the `MultipartUpload` + `upload/*` classes.

Exemplars: `startSearchIndexQuery`/`getSearchIndexQueryResults` (async start/poll pair), `listEDucTemplates` (`postJSONEntity(getRepoEndpoint(), EDUC_TEMPLATE, request, EDucTemplatePage.class)`).
