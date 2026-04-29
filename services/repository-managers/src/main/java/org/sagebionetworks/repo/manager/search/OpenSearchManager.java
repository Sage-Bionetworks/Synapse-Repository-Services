package org.sagebionetworks.repo.manager.search;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride;
import org.sagebionetworks.repo.model.search.SearchQuery;
import org.sagebionetworks.repo.model.search.SearchQueryResults;
import org.sagebionetworks.repo.model.search.SearchQueryPart;
import org.sagebionetworks.repo.model.search.table.SynonymSet;
import org.sagebionetworks.repo.model.search.table.TextAnalyzer;
import org.sagebionetworks.repo.model.search.table.TextAnalyzerSettings;

/**
 * Manager interface for OpenSearch Serverless (AOSS) operations.
 * Manages index lifecycle (create, delete) and query execution (search, autocomplete).
 */
public interface OpenSearchManager {

	/**
	 * Create an OpenSearch index with the given schema and configuration.
	 *
	 * @param indexName                The OpenSearch index name
	 * @param columns                  The column models defining the entity's schema
	 * @param defaultAnalyzer          The default analyzer qualified name (may be null for platform defaults)
	 * @param synonymSets              The resolved synonym sets (may be empty)
	 * @param columnAnalyzerOverrides  The resolved column analyzer overrides (may be empty)
	 * @param analyzers                Map of analyzer qualified name to TextAnalyzer for all analyzers needed
	 * @return The JSON representation of the CreateIndexRequest, or empty if the index already existed
	 */
	Optional<String> createIndex(String indexName, List<ColumnModel> columns, String defaultAnalyzer,
			List<SynonymSet> synonymSets, List<ColumnAnalyzerOverride> columnAnalyzerOverrides,
			Map<String, TextAnalyzer> analyzers);

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
	 * @param indexName   The OpenSearch index name
	 * @param operations  List of bulk operations to execute
	 * @return The number of documents successfully indexed
	 */
	long bulkIndex(String indexName, List<BulkOperation> operations);

	/**
	 * Execute a search query against the OpenSearch index. The {@code options} set controls
	 * which sections of the OpenSearch request are populated: omitting HITS switches the
	 * request to {@code size=0}, omitting TOTAL_HITS disables total-hits tracking, and
	 * omitting FACETS skips aggregation construction. The returned {@link SearchQueryResults}
	 * only carries the fields corresponding to the requested options plus {@code offset}.
	 *
	 * @param indexName                The OpenSearch index name
	 * @param query                    The search query
	 * @param columns                  The column models for field routing
	 * @param defaultAnalyzer          The default analyzer qualified name (may be null)
	 * @param columnAnalyzerOverrides  The resolved overrides (may be empty)
	 * @param analyzers                Map of analyzer qualified name to TextAnalyzer
	 * @param options                    The response options requested; must be non-null and non-empty.
	 * @return The search results (only fields corresponding to requested options are populated)
	 */
	SearchQueryResults search(String indexName, SearchQuery query, List<ColumnModel> columns,
			String defaultAnalyzer, List<ColumnAnalyzerOverride> columnAnalyzerOverrides,
			Map<String, TextAnalyzer> analyzers, Set<SearchQueryPart> options);

	/**
	 * Execute an autocomplete query. Forces PREFIX query type and caps size at 8.
	 * Autocomplete never produces facets regardless of the {@code options} set.
	 *
	 * @param indexName                The OpenSearch index name
	 * @param query                    The search query (queryType will be overridden to PREFIX)
	 * @param columns                  The column models
	 * @param defaultAnalyzer          The default analyzer qualified name (may be null)
	 * @param columnAnalyzerOverrides  The resolved overrides (may be empty)
	 * @param analyzers                Map of analyzer qualified name to TextAnalyzer
	 * @param options                    The response options requested; must be non-null and non-empty.
	 * @return The autocomplete results
	 */
	SearchQueryResults autocomplete(String indexName, SearchQuery query, List<ColumnModel> columns,
			String defaultAnalyzer, List<ColumnAnalyzerOverride> columnAnalyzerOverrides,
			Map<String, TextAnalyzer> analyzers, Set<SearchQueryPart> options);

	/**
	 * Validate analyzer settings by invoking the AOSS _analyze API.
	 *
	 * @param settings The TextAnalyzerSettings to validate
	 * @throws IllegalArgumentException if the analyzer configuration is rejected by OpenSearch
	 * @throws IllegalStateException if the OpenSearch service is unreachable
	 */
	void validateAnalyzerSettings(TextAnalyzerSettings settings);
}
