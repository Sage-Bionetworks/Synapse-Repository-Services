# manager/search

OpenSearch-backed search: index lifecycle/build, the query surface, and DSL validation. `SearchIndex` is one of the defining-SQL entity types (see the parent `services/repository-managers/CLAUDE.md` "Defining-SQL Entities" section for the shared bind-schema-on-create/update pattern); this package owns the search-specific build and query internals.

## Managed OpenSearch domain + build

`SearchIndexLifecycleManagerImpl` builds/deletes indexes against a **managed OpenSearch domain** (not AOSS). Key runtime facts:

- **Endpoint is discovered via `describeDomain`**, not injected from config — code must resolve it at runtime.
- **Shard count is computed from the source table's byte size** at build time (clamped to a max), so index topology tracks data size.
- The build reads the bound schema (via `tableManagerSupport.getTableSchema`) rather than re-translating the defining SQL.

## Row-level access control

SearchIndex enforces per-row ACL through **benefactor columns**: one non-analyzed `_benefactor_N` long field per source dependency, populated at index time. At query time `BenefactorAccessFilter` (in `manager/table/`, shared with the table-query SQL path so both gates compute accessibility identically) produces filters that are AND-ed into every OpenSearch query via `OpenSearchManager.search(..., accessFilters)` / `autocomplete(...)`. Any new query path must apply these access filters — an unfiltered query leaks rows across benefactors.

## DSL validation (defense-in-depth)

The API accepts an opaque OpenSearch query DSL (typed passthrough POJOs generated in `lib-auto-generated`'s `search/dsl/`). `SearchDslValidator` is the safety layer behind the POJO's structural allowlist: it enforces per-kind allowlists (`ALLOWED_QUERY_KINDS`, aggregation kinds), depth/clause caps, rejects leading wildcards, and rejects anything the OpenSearch client supports but nobody explicitly allowlisted. `SearchFieldRewriter` and `SearchOpaqueJsonUtil` handle field rewriting and opaque-JSON traversal. Preserve the extensive rationale comments — the caps and rejections are security controls, not arbitrary limits.

**Opaque leaf-value shapes are schema-guided, not hand-maintained.** A number of DSL slots (`match.<col>.query`, `range.<col>.gte`, the aggregation `missing` substitution, ...) are schema-typed as a bare `"type":"object"` because their value is polymorphic. `SearchDslValidator.walkOpaqueLeaves` discovers every such leaf by walking the `dsl.Query` / `dsl.Aggregation` effective schema (`SchemaCache`/`ObjectSchema`) rather than a hand-picked list, and requires a scalar value by default. A leaf whose real shape is legitimately non-scalar, or checked elsewhere on the typed object, is an explicit entry in `OPAQUE_LEAF_EXCEPTIONS`. `SearchDslOpaqueLeafCoverageTest` fails the build if the schema's discovered opaque-leaf set drifts from a frozen list, forcing a conscious decision on any newly added opaque property.

**A clause that carries column references inside a string expression is validated in `SearchFieldRewriter`, not `SearchDslValidator`.**
`query_string` names its columns inside the Lucene `query` text, so `SearchFieldRewriter.rewriteQueryStringClause` tokenizes that expression to substitute column ids, and that tokenizer is consequently where the clause's unknown-column and leading-wildcard rejections live.
An in-expression name that resolves to nothing must be rejected rather than passed through: OpenSearch answers an unmapped field with zero hits and HTTP 200, so passing it through returns a silent wrong answer instead of surfacing the typo.

## Anti-Patterns — Do NOT

- **Do NOT add `Global` aggregations to the `SearchDslValidator` allowlist.** A `Global` aggregation escapes the top-level query scope and would bypass the row-level benefactor ACL filter injected there (evidence: `SearchDslValidator.java:152`).
- **Do NOT add a new opaque (`"type":"object"`) property to the `dsl.Query` / `dsl.Aggregation` schema family without updating `SearchDslOpaqueLeafCoverageTest`'s frozen leaf set.** The build fails until the addition is accounted for; if the new leaf is not a plain scalar, also add it to `SearchDslValidator.OPAQUE_LEAF_EXCEPTIONS`.
- **Do NOT emit a query without the benefactor `accessFilters`** — see row-level access control above.

## Legacy

`search/oss/` (and the worker `search/oss/worker/SearchIndexWorker`) is the older queue-driven index writer; the lifecycle/query path here supersedes it for SearchIndex entities.
