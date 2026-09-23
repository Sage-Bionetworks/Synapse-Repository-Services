# manager/search

OpenSearch-backed search: index lifecycle/build, the query surface, and DSL validation. `SearchIndex` is one of the defining-SQL entity types (see the parent `services/repository-managers/CLAUDE.md` "Defining-SQL Entities" section for the shared bind-schema-on-create/update pattern); this package owns the search-specific build and query internals.

## Managed OpenSearch domain + build

`SearchIndexLifecycleManagerImpl` builds/deletes indexes against a **managed OpenSearch domain** (not AOSS). Key runtime facts:

- **Endpoint is discovered via `describeDomain`**, not injected from config — code must resolve it at runtime.
- **Shard count is computed from the source table's byte size** at build time (clamped to a max), so index topology tracks data size.
- **The source's as-built `IndexAuthorizationSnapshot` is the build's single source of truth.** `buildIndex` holds a non-exclusive lock on the direct source from reading its snapshot through the end of row streaming, translates the defining SQL against a `SchemaProvider` that resolves the source's schema from the snapshot's column ids (so `select *` and column types match the rows streamed), and splices benefactor columns from the snapshot.
- **Each physical slot carries its own snapshot in its mapping `_meta`** (`OpenSearchManagerImpl.AUTHORIZATION_SNAPSHOT_META_KEY`), written by `createIndex` into the idle slot before the alias swap. The live slot's `_meta` is never modified. The snapshot is rooted at the source (`IndexAuthorizationSnapshotManager.buildSearchIndexSnapshot`), with lineage flattened through the source's snapshot.
- **A source with an AGGREGATE_DATA object anywhere in its snapshot closure fails the build**, because a per-row search document cannot honor aggregate-only release.

## Row-level access control

SearchIndex enforces per-row ACL through **benefactor columns**: one non-analyzed `_benefactor_N` long field per source dependency, populated at index time. At query time the pre-flight resolves the alias to its physical index and snapshot in one call (`OpenSearchManager.getLiveIndex`), authorizes against the snapshot's node closure, reads columns from its lineage ids, and pins the search to that physical index — so filters built from one slot's snapshot are never applied to another slot's documents. `BenefactorAccessFilter` (in `manager/table/`, shared with the table-query SQL path so both gates compute accessibility identically) produces filters that are AND-ed into every OpenSearch query via `OpenSearchManager.search(..., accessFilters)` / `autocomplete(...)`. Any new query path must apply these access filters — an unfiltered query leaks rows across benefactors.

## DSL validation (defense-in-depth)

The API accepts an opaque OpenSearch query DSL (typed passthrough POJOs generated in `lib-auto-generated`'s `search/dsl/`). `SearchDslValidator` is the safety layer behind the POJO's structural allowlist: it enforces per-kind allowlists (`ALLOWED_QUERY_KINDS`, aggregation kinds), depth/clause caps, rejects leading wildcards, and rejects anything the OpenSearch client supports but nobody explicitly allowlisted. `SearchFieldRewriter` and `SearchOpaqueJsonUtil` handle field rewriting and opaque-JSON traversal. Preserve the extensive rationale comments — the caps and rejections are security controls, not arbitrary limits.

**Opaque leaf-value shapes are schema-guided, not hand-maintained.** A number of DSL slots (`match.<col>.query`, `range.<col>.gte`, the aggregation `missing` substitution, ...) are schema-typed as a bare `"type":"object"` because their value is polymorphic. `SearchDslValidator.walkOpaqueLeaves` discovers every such leaf by walking the `dsl.Query` / `dsl.Aggregation` effective schema (`SchemaCache`/`ObjectSchema`) rather than a hand-picked list, and requires a scalar value by default. A leaf whose real shape is legitimately non-scalar, or checked elsewhere on the typed object, is an explicit entry in `OPAQUE_LEAF_EXCEPTIONS`. `SearchDslOpaqueLeafCoverageTest` fails the build if the schema's discovered opaque-leaf set drifts from a frozen list, forcing a conscious decision on any newly added opaque property.

## Anti-Patterns — Do NOT

- **Do NOT add `Global` aggregations to the `SearchDslValidator` allowlist.** A `Global` aggregation escapes the top-level query scope and would bypass the row-level benefactor ACL filter injected there (evidence: `SearchDslValidator.java:152`).
- **Do NOT add a new opaque (`"type":"object"`) property to the `dsl.Query` / `dsl.Aggregation` schema family without updating `SearchDslOpaqueLeafCoverageTest`'s frozen leaf set.** The build fails until the addition is accounted for; if the new leaf is not a plain scalar, also add it to `SearchDslValidator.OPAQUE_LEAF_EXCEPTIONS`.
- **Do NOT emit a query without the benefactor `accessFilters`** — see row-level access control above.
- **Do NOT read the entity's bound schema (`tableManagerSupport.getTableSchema(searchIndexId)`) or the source's live schema on the build or query path.** The bound schema only advances when the SearchIndex entity itself is updated, while rebuilds are driven by source changes, so it can describe a shape the source no longer has; zipping it against streamed row values silently misreads every row (reproduced by `SearchIndexLifecycleWorkerAutowireTest` inserting a column into a source MV's select list). Build from the source snapshot and query from the slot's `_meta`.
- **Do NOT search the alias after reading `_meta`.** An alias swap between the two reads would apply one slot's `_benefactor_i` filters to the other slot's documents; search the physical index `getLiveIndex` returned and re-resolve once if it was deleted.
- **Do NOT infer the benefactor-column count from the streamed row width.** The row handler is told the document-column and trailing-benefactor counts and fails closed on any mismatch. Treating surplus values as benefactors indexes rows under a benefactor they do not belong to whenever the surplus value parses as a long, which silently corrupts query-time ACL filtering instead of failing the build.

## Legacy

`search/oss/` (and the worker `search/oss/worker/SearchIndexWorker`) is the older queue-driven index writer; the lifecycle/query path here supersedes it for SearchIndex entities.
