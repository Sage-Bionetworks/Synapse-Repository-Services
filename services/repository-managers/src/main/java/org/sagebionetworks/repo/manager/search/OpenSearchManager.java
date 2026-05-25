package org.sagebionetworks.repo.manager.search;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride;
import org.sagebionetworks.repo.model.search.SearchQuery;
import org.sagebionetworks.repo.model.search.SearchQueryResults;
import org.sagebionetworks.repo.model.search.SearchQueryPart;
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
	 * @param columnSemanticEnrichment The list of columns opted in to AOSS Automatic Semantic
	 *                                 Enrichment, taken directly from the SearchConfiguration's
	 *                                 {@code columnSemanticEnrichment} field (may be {@code null}
	 *                                 or empty). Entries whose {@code columnName} is not on the
	 *                                 bound schema, or is not a top-level text-typed column
	 *                                 (STRING / MEDIUMTEXT / LARGETEXT / LINK), are silently
	 *                                 skipped at this layer — mirroring how
	 *                                 {@code columnAnalyzerOverrides} treats missing columns and
	 *                                 keeping the per-AOSS-doc restriction on top-level text
	 *                                 fields in one place.
	 * @return The JSON representation of the CreateIndexRequest, or empty if the index already existed
	 */
	Optional<String> createIndex(String indexName, List<ColumnModel> columns,
			String defaultAnalyzer,
			List<ColumnAnalyzerOverride> columnAnalyzerOverrides,
			Map<String, IndexSettingsAnalysis> resolvedAnalyzers,
			List<?> columnSemanticEnrichment);

	/**
	 * Delete an OpenSearch index. No-op if the index does not exist.
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
	 * Block until {@code indexName} reports a document count of at least
	 * {@code expectedCount}. AOSS is eventually consistent: a {@code bulkIndex} call can
	 * return success while the documents are not yet visible to {@code _search} or
	 * {@code _count}. Polling {@code _count} (allowed on AOSS under
	 * {@code aoss:ReadDocument}) is the only convergence signal available.
	 *
	 * <p>The comparison is {@code actual >= expectedCount} rather than strict equality so
	 * a leftover readiness-probe sentinel (whose cleanup is best-effort, see
	 * {@link #waitForIndexWritable}) does not strand convergence one short.</p>
	 *
	 * <p>No-op when {@code expectedCount == 0} — an empty index reports zero immediately
	 * and skipping the call avoids an unnecessary round-trip.</p>
	 *
	 * @param indexName     the OpenSearch index name
	 * @param expectedCount the document count the index must reach before returning
	 * @throws RecoverableMessageException when the index does not converge within the
	 *         retry budget, so the SearchIndex lifecycle message goes back on SQS for a
	 *         later attempt instead of being marked permanently FAILED
	 */
	void waitForDocumentCount(String indexName, long expectedCount) throws RecoverableMessageException;

	/**
	 * Execute a search query against the OpenSearch index. The {@code options} set controls
	 * which sections of the OpenSearch request are populated: omitting HITS switches the
	 * request to {@code size=0}, omitting TOTAL_HITS disables total-hits tracking, and
	 * omitting FACETS skips aggregation construction.
	 *
	 * <p>Note: query-time analysis (default and per-column analyzers) is baked into the
	 * AOSS index at build time, so this method does not take analyzer arguments — AOSS
	 * routes each field through its own configured search analyzer automatically.</p>
	 *
	 * @param indexName  The OpenSearch index name.
	 * @param query      The search query.
	 * @param columns    The column models for field routing (user-facing names).
	 * @param options    The response options requested; must be non-null and non-empty.
	 * @return The search results — only fields corresponding to requested options are populated.
	 */
	SearchQueryResults search(String indexName, SearchQuery query, List<ColumnModel> columns,
			Set<SearchQueryPart> options);

	/**
	 * Execute an autocomplete query. Forces PREFIX query type and caps size at 8.
	 * Autocomplete never produces facets regardless of the {@code options} set.
	 *
	 * @param indexName  The OpenSearch index name.
	 * @param query      The search query (queryType is overridden to PREFIX).
	 * @param columns    The column models for field routing (user-facing names).
	 * @param options    The response options requested; must be non-null and non-empty.
	 * @return The autocomplete results.
	 */
	SearchQueryResults autocomplete(String indexName, SearchQuery query, List<ColumnModel> columns,
			Set<SearchQueryPart> options);

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
