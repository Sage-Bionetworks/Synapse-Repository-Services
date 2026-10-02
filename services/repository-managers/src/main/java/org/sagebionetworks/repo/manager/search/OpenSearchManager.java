package org.sagebionetworks.repo.manager.search;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;
import org.sagebionetworks.repo.manager.search.SemanticEmbeddingBootstrapper.SemanticEmbeddingModel;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.IndexAuthorizationSnapshot;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride;
import org.sagebionetworks.repo.model.search.SearchAutocompleteBody;
import org.sagebionetworks.repo.model.search.SearchQuery;
import org.sagebionetworks.repo.model.search.SearchQueryResults;
import org.sagebionetworks.repo.model.search.SearchQueryPart;
import org.sagebionetworks.repo.model.search.dsl.SearchPipeline;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

/**
 * AOSS-facing client seam for the SearchIndex feature. Owns index lifecycle
 * (create / delete / writability probe), bulk document indexing, and query execution
 * (search and autocomplete).
 *
 * <p>Implementations translate user-facing schema names to OpenSearch column-ID field
 * names before delegating to the OpenSearch Java client; callers (managers, workers)
 * pass user-facing names throughout.</p>
 */
public interface OpenSearchManager {

	/**
	 * Create an OpenSearch index with the given schema and configuration.
	 *
	 * @param indexName                The OpenSearch index name
	 * @param columns                  The column models defining the entity's schema
	 * @param defaultAnalyzer          The qualified name of the SearchConfiguration's primary
	 *                                 TextAnalyzer. Its {@code analyzer.default} entry is registered
	 *                                 at the index's {@code analysis.analyzer.default} reserved slot;
	 *                                 if the same TextAnalyzer also declares
	 *                                 {@code analyzer.default_search}, that lands at
	 *                                 {@code analysis.analyzer.default_search}. May be {@code null}
	 *                                 — in that case OpenSearch falls back to its built-in
	 *                                 {@code standard} analyzer for unbound text fields.
	 * @param columnAnalyzerOverrides  The resolved column analyzer overrides (may be empty)
	 * @param resolvedAnalyzers        Map of qualified name → typed analysis settings produced by
	 *                                 {@link SearchAnalyzerJsonUtil#resolveRefs}. Each value is the
	 *                                 {@code settings.analysis} block for one TextAnalyzer with all
	 *                                 {@code $ref} entries already substituted.
	 * @param benefactorColumnNames    The source's benefactor column names, each mapped to a
	 *                                 non-analyzed {@code long} row-level access-control field named
	 *                                 by {@link OpenSearchManagerImpl#benefactorFieldName(String)} for
	 *                                 the row-level search ACL filter. Empty for a benefactor-less
	 *                                 source (e.g. a table).
	 * @param numberOfShards           The number of primary shards for the index, computed at build
	 *                                 time from the source table's data size.
	 * @param numberOfReplicas         The number of replica shards for the index (1 on prod, 0 on the
	 *                                 single-node dev domain).
	 * @param snapshot                 The as-built {@link IndexAuthorizationSnapshot} of the source the
	 *                                 index is built from. Stored in the index's mapping {@code _meta},
	 *                                 together with the ids of {@code columns} in order, so
	 *                                 {@link #getLiveIndex(String)} can read both back alongside the
	 *                                 physical index they describe. Required.
	 * @param semanticModel            The embedding model that fills the index's
	 *                                 {@link OpenSearchManagerImpl#SEMANTIC_FIELD} vector, or {@code null}
	 *                                 when no column is flagged semantic. When set, the index gets a
	 *                                 {@code knn_vector} field of the model's dimension, an ingestion
	 *                                 pipeline that embeds each document's
	 *                                 {@link OpenSearchManagerImpl#SEMANTIC_TEXT_FIELD} with the model, and
	 *                                 the model's {@link SemanticEmbeddingModel#spec()} in its mapping
	 *                                 {@code _meta}.
	 * @return The JSON representation of the CreateIndexRequest, or empty if the index already existed
	 */
	Optional<String> createIndex(String indexName, List<ColumnModel> columns,
			String defaultAnalyzer,
			List<ColumnAnalyzerOverride> columnAnalyzerOverrides,
			Map<String, IndexSettingsAnalysis> resolvedAnalyzers,
			List<String> benefactorColumnNames, int numberOfShards, int numberOfReplicas,
			IndexAuthorizationSnapshot snapshot, SemanticEmbeddingModel semanticModel);

	/**
	 * Delete an OpenSearch index and the ingestion pipeline {@link #createIndex} attached to it. No-op
	 * for whichever of the two does not exist.
	 * If AOSS rejects the delete because another delete is already in progress
	 * for the same index, the underlying {@link org.opensearch.client.opensearch._types.OpenSearchException}
	 * is re-thrown unwrapped so the caller can recognize the concurrent-delete case
	 * and translate it into a recoverable SQS retry — by the time the retry runs,
	 * the winning delete has finished and this call becomes a no-op.
	 *
	 * @param indexName The OpenSearch index name
	 */
	void deleteIndex(String indexName);

	/**
	 * Resolve the concrete index a query alias currently points at. Blue-green rebuilds query
	 * a stable alias while documents are streamed into an inactive physical index, so this is
	 * how a build discovers which physical index is currently live (and therefore which slot to
	 * build into and repoint away from).
	 *
	 * @param aliasName The alias name.
	 * @return The single concrete index the alias resolves to, or empty when the alias does not
	 *         exist yet (the first build).
	 * @throws IllegalStateException when the alias resolves to more than one concrete index — the
	 *         blue-green invariant is exactly one live index per alias.
	 */
	Optional<String> getAliasTarget(String aliasName);

	/**
	 * The physical index a query alias currently points at, together with the source
	 * {@link IndexAuthorizationSnapshot} and the output columns that physical index was built with.
	 *
	 * @param physicalIndex The concrete index name behind the alias.
	 * @param snapshot      The source's as-built authorization snapshot stored in that index's mapping metadata.
	 * @param columnIds     The ids of the index's output columns, in select-list order.
	 * @param semanticSpec  The {@link SemanticEmbeddingModel#spec()} the index's vectors were built with,
	 *                      or {@code null} when the index has no semantic field.
	 */
	record LiveIndex(String physicalIndex, IndexAuthorizationSnapshot snapshot, List<String> columnIds,
			String semanticSpec) {
	}

	/**
	 * Resolve the physical index a query alias currently points at and read the as-built
	 * {@link IndexAuthorizationSnapshot} stored in that index's mapping metadata.
	 *
	 * @param alias The alias name.
	 * @return The live physical index and its snapshot, or empty when the alias does not exist or
	 *         the live index carries no snapshot (the caller must treat either as not yet built).
	 * @throws IllegalStateException when the alias resolves to more than one concrete index, or the
	 *         stored snapshot cannot be parsed.
	 */
	Optional<LiveIndex> getLiveIndex(String alias);

	/**
	 * Atomically repoint a query alias from its current concrete index to a newly-built one.
	 * The remove of {@code oldPhysicalIndex} (when present) and the add of {@code newPhysicalIndex}
	 * are submitted as a single update-aliases action list so queries never observe an alias that
	 * points at zero indices.
	 *
	 * @param aliasName        The alias name queries target.
	 * @param newPhysicalIndex The freshly-built, writable index the alias should point at.
	 * @param oldPhysicalIndex The currently-live index to detach, or empty on the first build.
	 */
	void swapAlias(String aliasName, String newPhysicalIndex, Optional<String> oldPhysicalIndex);

	/**
	 * Bulk index a batch of documents into the OpenSearch index.
	 *
	 * <p><b>Idempotency requirement:</b> every {@link BulkOperation} passed to this method
	 * MUST be idempotent — i.e. {@code index} or {@code delete} with an explicit {@code _id}.
	 * The implementation may retry partially-failed envelopes, and a partial transport
	 * failure can drop the response for an op that AOSS already accepted; on retry the
	 * same op is resubmitted. Idempotent op types (overwrite-by-id / delete-by-id) make
	 * that resubmission safe; {@code create} or unkeyed {@code index} ops would write
	 * duplicates.</p>
	 *
	 * @param indexName   The OpenSearch index name
	 * @param operations  List of idempotent bulk operations to execute
	 * @return The number of documents successfully indexed
	 */
	long bulkIndex(String indexName, List<BulkOperation> operations);

	/**
	 * Block until {@code indexName} accepts writes. AOSS acknowledges {@code createIndex}
	 * and the index is queryable before the shards are actually ready to accept documents,
	 * so a query-based readiness check is not reliable — the only reliable probe is a real
	 * write. Writes a sentinel document with {@code _row_id = -1} and, on success, deletes
	 * it before returning.
	 *
	 * @param indexName The OpenSearch index name
	 * @throws RecoverableMessageException when the probe does not succeed within the retry
	 *         budget, so the SearchIndex lifecycle message goes back on SQS for a later attempt
	 */
	void waitForIndexWritable(String indexName) throws RecoverableMessageException;

	/**
	 * Execute a search query against the OpenSearch index. The {@code options} set controls
	 * which sections of the OpenSearch request are populated: omitting HITS switches the
	 * request to {@code size=0}, and omitting TOTAL_HITS disables total-hits tracking.
	 * Aggregations are presence-driven by the request body
	 * ({@code body.aggregations}) and are not gated by the options set.
	 *
	 * <p>Note: query-time analysis (default and per-column analyzers) is baked into the
	 * AOSS index at build time, so this method does not take analyzer arguments — AOSS
	 * routes each field through its own configured search analyzer automatically.</p>
	 *
	 * <p>Each query in {@code accessFilters} is AND-ed (as a {@code bool.filter} clause) with
	 * the caller's query, so a document is returned only if it satisfies every filter. This
	 * enforces row-level benefactor access control; a benefactor-less source passes an empty
	 * list, applying no row filter.</p>
	 *
	 * <p>A {@code hybrid} body is sent with its resolved search pipeline: an inline
	 * {@code body.search_pipeline}, else {@code savedPipeline}, else min_max normalization with an
	 * arithmetic mean of equal weights.</p>
	 *
	 * @param indexName  The OpenSearch index name.
	 * @param body       The typed {@link SearchQuery} envelope; each slot's contents are the
	 *                   opaque OpenSearch DSL.
	 * @param columns    The column models for field routing (user-facing names).
	 * @param options    The response options requested; must be non-null and non-empty.
	 * @param accessFilters Pre-built OpenSearch filter queries (e.g. one benefactor
	 *                      {@code terms} clause per source dependency). Must not be null.
	 * @param semanticModelId The deployed embedding model id stamped on each neural clause, or null
	 *                        when the index has no semantic field (neural clauses are then dropped).
	 * @param savedPipeline The saved pipeline settings resolved for a hybrid body, or null.
	 * @return The search results — only fields corresponding to requested options are populated.
	 * @throws IllegalArgumentException if the body is invalid or OpenSearch rejects it.
	 */
	SearchQueryResults search(String indexName, SearchQuery body, List<ColumnModel> columns,
			Set<SearchQueryPart> options, List<Query> accessFilters, String semanticModelId,
			SearchPipeline savedPipeline);

	/**
	 * Execute an autocomplete query against the OpenSearch index. The body's allowlist is
	 * narrowed to the autocomplete subset (prefix-flavored {@code query} plus optional
	 * {@code _source}); page size is capped at the autocomplete server-side limit. The
	 * {@code accessFilters} are AND-ed with the caller's query exactly as in
	 * {@link #search(String, SearchQuery, List, Set, List, String, SearchPipeline)}.
	 *
	 * @param indexName  The OpenSearch index name.
	 * @param body       The typed {@link SearchAutocompleteBody} envelope.
	 * @param columns    The column models for field routing (user-facing names).
	 * @param options    The response options requested; must be non-null and non-empty.
	 * @param accessFilters Pre-built OpenSearch filter queries; must not be null.
	 * @return The autocomplete results.
	 */
	SearchQueryResults autocomplete(String indexName, SearchAutocompleteBody body, List<ColumnModel> columns,
			Set<SearchQueryPart> options, List<Query> accessFilters);

	/**
	 * Validate a TextAnalyzer's settings by sending each declared analyzer entry's chain
	 * through AOSS's cluster-level {@code _analyze} endpoint. Surfaces real OpenSearch-side
	 * errors (bad component {@code type}, bad parameters, malformed chain) at TextAnalyzer
	 * create/update time instead of letting them FAIL asynchronously the first time the
	 * analyzer is used in an index build.
	 *
	 * <p>The {@code resolvedSettings} value must already have all {@code $ref} entries
	 * substituted by {@link SearchAnalyzerJsonUtil#resolveRefs} &mdash; this method does not
	 * look up SynonymSets.</p>
	 *
	 * @param resolvedSettings The {@code settings.analysis} block for the TextAnalyzer
	 *                         being validated, post-{@code $ref} resolution.
	 * @throws IllegalArgumentException when AOSS rejects the analyzer configuration.
	 * @throws IllegalStateException when AOSS is unreachable (the curator should retry).
	 */
	void validateAnalyzerSettings(IndexSettingsAnalysis resolvedSettings);
}
