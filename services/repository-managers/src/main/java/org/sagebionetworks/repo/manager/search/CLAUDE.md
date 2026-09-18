# manager/search

OpenSearch-backed search: index lifecycle/build, the query surface, and DSL validation. `SearchIndex` is one of the defining-SQL entity types (see the parent `services/repository-managers/CLAUDE.md` "Defining-SQL Entities" section for the shared bind-schema-on-create/update pattern); this package owns the search-specific build and query internals.

## Managed OpenSearch domain + build

`SearchIndexLifecycleManagerImpl` builds/deletes indexes against a **managed OpenSearch domain** (not AOSS). Key runtime facts:

- **Endpoint is discovered via `describeDomain`**, not injected from config — code must resolve it at runtime.
- **Shard count is computed from the source table's byte size** at build time (clamped to a max), so index topology tracks data size.
- **The build derives its document schema from its own translation of the defining SQL, then rebinds it after the alias swap** — it must never read back the schema bound by an earlier registration. See the anti-pattern below.

## Row-level access control

SearchIndex enforces per-row ACL through **benefactor columns**: one non-analyzed `_benefactor_N` long field per source dependency, populated at index time. At query time `BenefactorAccessFilter` (in `manager/table/`, shared with the table-query SQL path so both gates compute accessibility identically) produces filters that are AND-ed into every OpenSearch query via `OpenSearchManager.search(..., accessFilters)` / `autocomplete(...)`. Any new query path must apply these access filters — an unfiltered query leaks rows across benefactors.

## DSL validation (defense-in-depth)

The API accepts an opaque OpenSearch query DSL (typed passthrough POJOs generated in `lib-auto-generated`'s `search/dsl/`). `SearchDslValidator` is the safety layer behind the POJO's structural allowlist: it enforces per-kind allowlists (`ALLOWED_QUERY_KINDS`, aggregation kinds), depth/clause caps, rejects leading wildcards, and rejects anything the OpenSearch client supports but nobody explicitly allowlisted. `SearchFieldRewriter` and `SearchOpaqueJsonUtil` handle field rewriting and opaque-JSON traversal. Preserve the extensive rationale comments — the caps and rejections are security controls, not arbitrary limits.

## Anti-Patterns — Do NOT

- **Do NOT add `Global` aggregations to the `SearchDslValidator` allowlist.** A `Global` aggregation escapes the top-level query scope and would bypass the row-level benefactor ACL filter injected there (evidence: `SearchDslValidator.java:152`).
- **Do NOT emit a query without the benefactor `accessFilters`** — see row-level access control above.
- **Do NOT build the index against the bound schema (`tableManagerSupport.getTableSchema(searchIndexId)`).** Nothing re-registers a SearchIndex when its *source's* schema changes, yet that change is exactly what fans out the rebuild — so the bound schema can describe a shape the source no longer has. Pairing it with freshly translated row values makes the positional zip in the row handler silently misread every row (evidence: PLFM-9714, reproduced by `SearchIndexLifecycleWorkerAutowireTest` inserting a column into a source MV's select list). Derive the schema from this build's own `QueryTranslator` and rebind it only after the alias swap, as `MaterializedViewManagerImpl` does around its index swap.
- **Do NOT infer the benefactor-column count from the streamed row width.** The row handler is told the document-column and trailing-benefactor counts and fails closed on any mismatch. Treating surplus values as benefactors indexes rows under a benefactor they do not belong to whenever the surplus value parses as a long, which silently corrupts query-time ACL filtering instead of failing the build.

## Legacy

`search/oss/` (and the worker `search/oss/worker/SearchIndexWorker`) is the older queue-driven index writer; the lifecycle/query path here supersedes it for SearchIndex entities.
