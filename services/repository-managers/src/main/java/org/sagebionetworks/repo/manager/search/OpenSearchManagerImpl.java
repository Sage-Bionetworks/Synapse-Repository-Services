package org.sagebionetworks.repo.manager.search;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.json.JsonpDeserializer;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.FieldSort;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.ShardSearchFailure;
import org.opensearch.client.opensearch._types.ShardStatistics;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.analysis.CharFilter;
import org.opensearch.client.opensearch._types.analysis.CharFilterDefinition;
import org.opensearch.client.opensearch._types.analysis.TokenFilter;
import org.opensearch.client.opensearch._types.analysis.TokenFilterDefinition;
import org.opensearch.client.opensearch._types.analysis.Tokenizer;
import org.opensearch.client.opensearch._types.analysis.TokenizerDefinition;
import org.opensearch.client.opensearch._types.mapping.DynamicMapping;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TextQueryType;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.core.search.HighlightField;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;
import org.sagebionetworks.repo.model.search.FacetRequest;
import org.sagebionetworks.repo.model.search.FacetSortField;
import org.sagebionetworks.repo.model.search.KeyRange;
import org.sagebionetworks.repo.model.search.KeyValues;
import org.sagebionetworks.repo.model.search.SearchFieldValue;
import org.sagebionetworks.repo.model.search.SearchHit;
import org.sagebionetworks.repo.model.search.SearchQuery;
import org.sagebionetworks.repo.model.search.SearchQueryPart;
import org.sagebionetworks.repo.model.search.SearchQueryResults;
import org.sagebionetworks.repo.model.search.SearchQueryType;
import org.sagebionetworks.repo.model.search.SortDirection;
import org.sagebionetworks.repo.model.search.SortField;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverrideEntry;
import org.sagebionetworks.repo.model.search.table.SynonymRule;
import org.sagebionetworks.repo.model.search.table.SynonymRuleType;
import org.sagebionetworks.repo.model.search.table.SynonymSet;
import org.sagebionetworks.repo.model.search.table.TextAnalyzer;
import org.sagebionetworks.repo.model.search.table.TextAnalyzerSettings;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnResult;
import org.sagebionetworks.repo.model.table.FacetColumnResultValueCount;
import org.sagebionetworks.repo.model.table.FacetColumnResultValues;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.util.RetryException;
import org.sagebionetworks.util.TimeUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Service;

import jakarta.json.stream.JsonParser;

/**
 * Implementation of {@link OpenSearchManager} that wraps the OpenSearch Java client
 * for all AOSS operations.
 */
@Service
public class OpenSearchManagerImpl implements OpenSearchManager {

	private static final Logger LOG = LogManager.getLogger(OpenSearchManagerImpl.class);

	private static final int HTTP_TOO_MANY_REQUESTS = 429;
	private static final int HTTP_INTERNAL_SERVER_ERROR = 500;
	private static final int HTTP_MAX_SERVER_ERROR = 599;

	// Per-item bulk-failure descriptors are logged in full, but the first N are also
	// embedded in the thrown RuntimeException message so the reason reaches the user
	// via SEARCH_INDEX_STATUS.ERROR_MESSAGE (VARCHAR(3000)) and ASYNCH_JOB_STATUS.
	static final int MAX_FAILURE_SAMPLES = 5;
	static final int MAX_BULK_ERROR_MESSAGE_CHARS = 2500;
	static final String TRUNCATION_MARKER = "...[truncated]";

	// Intra-batch retry for transient AOSS rejections (429 / 5xx on a subset of items).
	// Resubmit only the incomplete subset with exponential backoff so the batch can drain
	// instead of bouncing the whole change message back to SQS on every partial failure.
	// Non-final so unit tests can lower the values and avoid real-wall-clock sleeps.
	static int BULK_INDEX_MAX_RETRIES = 10;
	static long BULK_INDEX_INITIAL_BACKOFF_MS = 10000L;

	// Readiness probe for a freshly-created index. AOSS acknowledges createIndex and is
	// queryable before shards are ready to accept writes, so the only reliable probe is
	// an actual write. Same budget as the bulk-index retry for consistency.
	static int INDEX_WRITABLE_MAX_RETRIES = 10;
	static long INDEX_WRITABLE_INITIAL_BACKOFF_MS = 10000L;
	static final String READINESS_PROBE_DOC_ID = "__readiness_probe__";

	private static final String SYSTEM_FIELD_ROW_ID = "_row_id";
	private static final String SYSTEM_FIELD_ROW_VERSION = "_row_version";
	private static final String SUB_FIELD_KEYWORD = "keyword";
	private static final String SUB_FIELD_SEARCHABLE = "searchable";
	private static final String INDEX_NOT_FOUND_EXCEPTION = "index_not_found_exception";
	// AOSS reports a concurrent index-delete attempt with a reason text containing
	// "concurrent deletes". Package-visible so callers can recognize and translate
	// it into a recoverable SQS retry.
	static final String CONCURRENT_DELETES_MARKER = "concurrent deletes";
	private static final String ANALYZER_PREFIX = "synapse_analyzer_";
	private static final String SYNONYM_FILTER_NAME = "synapse_synonyms";
	static final String SEARCH_ANALYZER_SUFFIX = "_search";

	/** True when the OpenSearch error is AOSS's "concurrent deletes" rejection. */
	static boolean isConcurrentDeleteError(OpenSearchException e) {
		String reason = e.error() == null ? null : e.error().reason();
		return reason != null && reason.contains(CONCURRENT_DELETES_MARKER);
	}

	private static final int DEFAULT_LIMIT = 25;
	private static final int MAX_LIMIT = 100;
	private static final int AUTOCOMPLETE_MAX_LIMIT = 8;
	private static final int DEFAULT_FACET_SIZE = 25;

	private final OpenSearchClient openSearchClient;

	public OpenSearchManagerImpl(OpenSearchClient openSearchClient) {
		this.openSearchClient = openSearchClient;
	}

	@Override
	public Optional<String> createIndex(String indexName, List<ColumnModel> columns, String defaultAnalyzer,
			List<SynonymSet> synonymSets, List<ColumnAnalyzerOverride> columnAnalyzerOverrides,
			Map<String, TextAnalyzer> analyzers) {

		List<String> synonymRules = buildSynonymRules(synonymSets);
		boolean hasSynonyms = !synonymRules.isEmpty();

		Map<String, String> nameToId = columns.stream()
				.collect(Collectors.toMap(ColumnModel::getName, ColumnModel::getId, (a2, b) -> a2));
		Map<String, ColumnAnalyzerOverrideEntry> overrideMap = buildOverrideMap(columnAnalyzerOverrides, nameToId);

		try {
			CreateIndexRequest request = CreateIndexRequest.of(req -> req
					.index(indexName)
					.settings(s -> s.analysis(a -> {
						buildAnalysisSettings(a, analyzers, hasSynonyms, synonymRules);
						return a;
					}))
					.mappings(m -> {
						buildMappings(m, columns, defaultAnalyzer, overrideMap, analyzers, hasSynonyms);
						return m;
					})
			);

			String appliedConfigJson = request.toJsonString();
			CreateIndexResponse response = openSearchClient.indices().create(request);

			if (!Boolean.TRUE.equals(response.acknowledged())) {
				throw new IllegalStateException("Search index " + indexName + " creation was not acknowledged.");
			}

			return Optional.of(appliedConfigJson);
		} catch (OpenSearchException e) {
			if ("resource_already_exists_exception".equals(e.error().type())) {
				return Optional.empty();
			}
			throw new RuntimeException("Failed to create search index: " + indexName
					+ " (" + describeError(e.error()) + ")", e);
		} catch (IOException e) {
			throw new RuntimeException("Failed to create search index: " + indexName, e);
		}
	}

	private void buildAnalysisSettings(IndexSettingsAnalysis.Builder a,
			Map<String, TextAnalyzer> analyzers, boolean hasSynonyms, List<String> synonymRules) {
		if (hasSynonyms) {
			a.filter(SYNONYM_FILTER_NAME, f -> f.definition(d -> d
					.synonymGraph(syn -> syn.synonyms(synonymRules))));
		}
		for (Map.Entry<String, TextAnalyzer> entry : analyzers.entrySet()) {
			registerAnalyzer(a, entry.getValue(), hasSynonyms);
		}
	}

	private void registerAnalyzer(IndexSettingsAnalysis.Builder a, TextAnalyzer analyzer, boolean hasSynonyms) {
		TextAnalyzerSettings settings = analyzer.getSettings();

		if (settings.getTokenFilters() != null) {
			registerTokenFilters(a, settings.getTokenFilters());
		}
		if (settings.getCharFilters() != null) {
			registerCharFilters(a, settings.getCharFilters());
		}

		String tokenizer = settings.getTokenizer() != null ? settings.getTokenizer() : "standard";
		if (settings.getTokenizerConfig() != null && !settings.getTokenizerConfig().isEmpty()) {
			String customTokenizerName = "synapse_tokenizer_" + analyzer.getId();
			registerTokenizer(a, customTokenizerName, settings.getTokenizerConfig());
			tokenizer = customTokenizerName;
		}

		String analyzerName = ANALYZER_PREFIX + analyzer.getId();
		List<String> indexFilters = settings.getIndexFilterOrder() != null ? settings.getIndexFilterOrder() : Collections.emptyList();
		registerCustomAnalyzer(a, analyzerName, tokenizer, indexFilters, settings.getCharFilterOrder());

		if (shouldRegisterSearchVariant(settings, hasSynonyms)) {
			registerCustomAnalyzer(a, analyzerName + SEARCH_ANALYZER_SUFFIX, tokenizer,
					settings.getSearchFilterOrder(), settings.getCharFilterOrder());
		}
	}

	/**
	 * A {@code _search} analyzer variant is registered when the analyzer is asymmetric —
	 * i.e., it provides a {@code searchFilterOrder} distinct from {@code indexFilterOrder}.
	 * One special case: if the search chain references {@code synapse_synonyms} but the
	 * SearchIndex has no synonyms configured, the synonym filter is not registered in the
	 * index settings; we skip the variant and the analyzer behaves symmetrically.
	 */
	private static boolean shouldRegisterSearchVariant(TextAnalyzerSettings settings, boolean hasSynonyms) {
		List<String> search = settings.getSearchFilterOrder();
		if (search == null || search.isEmpty()) {
			return false;
		}
		return hasSynonyms || !search.contains(SYNONYM_FILTER_NAME);
	}

	private void registerCustomAnalyzer(IndexSettingsAnalysis.Builder a, String name,
			String tokenizer, List<String> filters, List<String> charFilters) {
		a.analyzer(name, an -> an.custom(c -> {
			c.tokenizer(tokenizer);
			if (!filters.isEmpty()) {
				c.filter(filters);
			}
			if (charFilters != null && !charFilters.isEmpty()) {
				c.charFilter(charFilters);
			}
			return c;
		}));
	}

	private void buildMappings(org.opensearch.client.opensearch._types.mapping.TypeMapping.Builder m,
			List<ColumnModel> columns, String defaultAnalyzer,
			Map<String, ColumnAnalyzerOverrideEntry> overrideMap, Map<String, TextAnalyzer> analyzers,
			boolean hasSynonyms) {
		m.properties(SYSTEM_FIELD_ROW_ID, p -> p.long_(l -> l));
		m.properties(SYSTEM_FIELD_ROW_VERSION, p -> p.long_(l -> l));

		Map<Long, String> idToQualifiedName = buildIdToQualifiedNameMap(analyzers);

		for (ColumnModel column : columns) {
			String columnId = column.getId();
			ColumnType columnType = column.getColumnType();
			String effectiveAnalyzerName = resolveEffectiveAnalyzerName(
					columnId, columnType, defaultAnalyzer, overrideMap, idToQualifiedName);
			TextAnalyzer effectiveAnalyzer = analyzers.get(effectiveAnalyzerName);
			ValidateArgument.required(effectiveAnalyzer, "analyzer '" + effectiveAnalyzerName + "' for column " + columnId);
			ColumnAnalyzerOverrideEntry entry = overrideMap.get(columnId);

			m.properties(columnId, buildProperty(columnType, effectiveAnalyzer, entry, analyzers, hasSynonyms));
		}
	}

	@Override
	public void deleteIndex(String indexName) {
		try {
			openSearchClient.indices().delete(req -> req.index(indexName));
		} catch (OpenSearchException e) {
			if (INDEX_NOT_FOUND_EXCEPTION.equals(e.error().type())) {
				return;
			}
			// Concurrent deletes: rethrow the OpenSearchException (a RuntimeException)
			// unwrapped so callers can recognize this case via isConcurrentDeleteError
			// and translate to a recoverable SQS retry.
			if (isConcurrentDeleteError(e)) {
				throw e;
			}
			throw new RuntimeException("Failed to delete search index: " + indexName
					+ " (" + describeError(e.error()) + ")", e);
		} catch (IOException e) {
			throw new RuntimeException("Failed to delete search index: " + indexName, e);
		}
	}

	@Override
	public void waitForIndexWritable(String indexName) throws RecoverableMessageException {
		// Fixed sentinel id so repeated probe writes on the same index overwrite the same
		// document rather than accumulating copies — one delete at the end is sufficient.
		Map<String, Object> sentinel = new HashMap<>();
		sentinel.put(SYSTEM_FIELD_ROW_ID, -1L);
		sentinel.put(SYSTEM_FIELD_ROW_VERSION, -1L);
		final int[] attempt = {0};
		try {
			TimeUtils.waitForExponentialMaxRetry(INDEX_WRITABLE_MAX_RETRIES, INDEX_WRITABLE_INITIAL_BACKOFF_MS,
					() -> {
						attempt[0]++;
						try {
							openSearchClient.index(IndexRequest.of(r -> r
									.index(indexName)
									.id(READINESS_PROBE_DOC_ID)
									.document(sentinel)));
							return Boolean.TRUE;
						} catch (OpenSearchException e) {
							LOG.warn("Index {} not yet writable (attempt {}/{}): {}",
									indexName, attempt[0], INDEX_WRITABLE_MAX_RETRIES, describeError(e.error()));
							throw new RetryException(e);
						} catch (IOException e) {
							LOG.warn("Index {} not yet writable (attempt {}/{}): {}",
									indexName, attempt[0], INDEX_WRITABLE_MAX_RETRIES, e.getMessage());
							throw new RetryException(e);
						}
					});
		} catch (RetryException e) {
			LOG.error("Index {} did not accept writes after {} attempts", indexName, INDEX_WRITABLE_MAX_RETRIES);
			throw new RecoverableMessageException(
					"AOSS index " + indexName + " did not accept writes within the retry budget",
					e.getCause());
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed readiness probe for search index: " + indexName, e);
		}
		// Remove the sentinel so real indexing never observes it. Cleanup failures are
		// non-fatal: the sentinel's _row_id = -1 cannot collide with real row ids.
		try {
			openSearchClient.delete(DeleteRequest.of(r -> r
					.index(indexName)
					.id(READINESS_PROBE_DOC_ID)));
		} catch (OpenSearchException | IOException e) {
			LOG.warn("Failed to delete readiness probe document from index {}: {}",
					indexName, e.getMessage());
		}
	}

	@Override
	public long bulkIndex(String indexName, List<BulkOperation> operations) {
		if (operations.isEmpty()) {
			return 0L;
		}
		final int totalOps = operations.size();
		// Ops still needing to be submitted. Attempt 1 sends the whole list as one bulk request;
		// later attempts iterate this list and send each op as its own single-op bulk request
		// so a single slow shard can't reject the whole batch.
		final List<BulkOperation> incomplete = new ArrayList<>(operations);
		// Most recent classification's retryable items — used for final ERROR-per-item diagnostics
		// when retries are exhausted.
		final List<BulkResponseItem> lastRetryable = new ArrayList<>();
		final int[] attempt = {0};

		try {
			TimeUtils.waitForExponentialMaxRetry(BULK_INDEX_MAX_RETRIES, BULK_INDEX_INITIAL_BACKOFF_MS, () -> {
				attempt[0]++;
				runAttempt(indexName, totalOps, attempt[0], incomplete, lastRetryable);
				return Boolean.TRUE;
			});
			return totalOps;
		} catch (RetryException e) {
			for (BulkResponseItem item : lastRetryable) {
				LOG.error("Bulk index item failed in {}: {}", indexName, describeBulkItemFailure(item));
			}
			throw new RecoverableMessageException(String.format(
					"Bulk index to %s failed after %d attempts: %d document(s) still retryable out of %d",
					indexName, BULK_INDEX_MAX_RETRIES, incomplete.size(), totalOps),
					e.getCause());
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to bulk index to search index: " + indexName, e);
		}
	}

	/**
	 * Runs one retry iteration. Attempt 1 submits {@code incomplete} as a single bulk request;
	 * later attempts submit each op in {@code incomplete} as its own single-op bulk request.
	 * Mutates {@code incomplete} and {@code lastRetryable} in place to reflect the set of ops
	 * still incomplete after this attempt. Returns normally when everything has been indexed, throws
	 * {@link RuntimeException} on any permanent failure, or throws {@link RetryException} so
	 * {@link TimeUtils#waitForExponentialMaxRetry} backs off and invokes us again.
	 */
	private void runAttempt(String indexName, int totalOps, int attempt,
			List<BulkOperation> incomplete, List<BulkResponseItem> lastRetryable) throws RetryException {
		List<BulkResponseItem> items;
		List<BulkOperation> opsSubmitted;
		if (attempt == 1) {
			BulkResponse response = executeBulk(indexName, incomplete);
			if (!response.errors()) {
				incomplete.clear();
				lastRetryable.clear();
				return;
			}
			items = response.items();
			opsSubmitted = new ArrayList<>(incomplete);
			incomplete.clear();
		} else {
			// Per-op: remove each op from `incomplete` only AFTER executeBulk returns. If
			// executeBulk throws an envelope RetryException mid-iteration, the current op + any
			// not-yet-iterated ops remain in `incomplete` so the next attempt resubmits them
			// without reprocessing the ops that already landed.
			items = new ArrayList<>(incomplete.size());
			opsSubmitted = new ArrayList<>(incomplete.size());
			while (!incomplete.isEmpty()) {
				BulkOperation op = incomplete.get(0);
				BulkResponse single = executeBulk(indexName, Collections.singletonList(op));
				items.addAll(single.items());
				opsSubmitted.add(op);
				incomplete.remove(0);
			}
		}

		BulkItemClassification c = classifyItems(indexName, items, opsSubmitted);
		if (c.hasPermanentFailures()) {
			throw new RuntimeException(buildPermanentFailureMessage(
					attemptFailureSummary(indexName, totalOps, c), c.permanentSamples));
		}
		incomplete.addAll(c.nextRemaining);
		lastRetryable.clear();
		lastRetryable.addAll(c.retryableItems);
		if (incomplete.isEmpty()) {
			return;
		}
		LOG.warn("Bulk index attempt {}/{} for {}: {} retryable of {}; backing off",
				attempt, BULK_INDEX_MAX_RETRIES, indexName, c.retryableItems.size(), opsSubmitted.size());
		throw new RetryException(attemptFailureSummary(indexName, totalOps, c));
	}

	/**
	 * Partitions bulk response items into (ok, retryable, permanent), emits an ERROR log per
	 * permanent failure, and captures up to {@link #MAX_FAILURE_SAMPLES} descriptors for the
	 * exception message. The returned classification is the input to the caller's retry decision.
	 * {@code items} and {@code ops} must be the same length and aligned by index.
	 */
	private BulkItemClassification classifyItems(String indexName, List<BulkResponseItem> items,
			List<BulkOperation> ops) {
		BulkItemClassification c = new BulkItemClassification();
		for (int i = 0; i < items.size(); i++) {
			BulkResponseItem item = items.get(i);
			if (item.error() == null) {
				continue;
			}
			if (isRetryableItemStatus(item.status())) {
				c.retryableItems.add(item);
				c.nextRemaining.add(ops.get(i));
			} else {
				c.permanentFailures++;
				String descriptor = describeBulkItemFailure(item);
				LOG.error("Bulk index item failed in {}: {}", indexName, descriptor);
				if (c.permanentSamples.size() < MAX_FAILURE_SAMPLES) {
					c.permanentSamples.add(descriptor);
				}
			}
		}
		return c;
	}

	private static String attemptFailureSummary(String indexName, int totalOps, BulkItemClassification c) {
		int totalFailures = c.retryableItems.size() + c.permanentFailures;
		return String.format(
				"Bulk index to %s failed: %d document(s) rejected out of %d (%d retryable, %d permanent)",
				indexName, totalFailures, totalOps, c.retryableItems.size(), c.permanentFailures);
	}

	/**
	 * Executes a single bulk request. Envelope-level 429/5xx and IOExceptions are translated to
	 * {@link RetryException} so the outer retry loop backs off; envelope-level 4xx becomes a
	 * permanent {@link RuntimeException}.
	 */
	private BulkResponse executeBulk(String indexName, List<BulkOperation> ops) throws RetryException {
		// Snapshot the list so the BulkRequest isn't mutated when retry state is advanced in place
		// between attempts.
		List<BulkOperation> snapshot = new ArrayList<>(ops);
		try {
			return openSearchClient.bulk(BulkRequest.of(req -> req.operations(snapshot)));
		} catch (OpenSearchException e) {
			String detail = "Failed to bulk index to search index: " + indexName
					+ " (" + describeError(e.error()) + ")";
			if (isRetryableItemStatus(e.status())) {
				throw new RetryException(detail, e);
			}
			throw new RuntimeException(detail, e);
		} catch (IOException e) {
			throw new RetryException("Failed to bulk index to search index: " + indexName, e);
		}
	}

	/** Output of {@link #classifyItems} — used by both the batch and per-document paths. */
	private static final class BulkItemClassification {
		final List<BulkOperation> nextRemaining = new ArrayList<>();
		final List<BulkResponseItem> retryableItems = new ArrayList<>();
		final List<String> permanentSamples = new ArrayList<>();
		int permanentFailures;

		boolean hasPermanentFailures() {
			return permanentFailures > 0;
		}
	}

	static boolean isRetryableItemStatus(int status) {
		return status == HTTP_TOO_MANY_REQUESTS
				|| (status >= HTTP_INTERNAL_SERVER_ERROR && status <= HTTP_MAX_SERVER_ERROR);
	}

	/**
	 * Appends up to {@link #MAX_FAILURE_SAMPLES} per-item descriptors to the summary so the
	 * reason reaches the user via SEARCH_INDEX_STATUS.ERROR_MESSAGE (VARCHAR(3000)). The whole
	 * message is hard-capped at {@link #MAX_BULK_ERROR_MESSAGE_CHARS} by substring truncation
	 * to stay safely inside that column width.
	 */
	static String buildPermanentFailureMessage(String summary, List<String> permanentSamples) {
		if (permanentSamples.isEmpty()) {
			return summary;
		}
		StringBuilder sb = new StringBuilder(summary).append(". Sample failures:");
		for (String sample : permanentSamples) {
			sb.append("\n - ").append(sample);
		}
		if (sb.length() > MAX_BULK_ERROR_MESSAGE_CHARS) {
			return sb.substring(0, MAX_BULK_ERROR_MESSAGE_CHARS - TRUNCATION_MARKER.length())
					+ TRUNCATION_MARKER;
		}
		return sb.toString();
	}

	/**
	 * AOSS often returns a generic {@code reason} ("Internal error occurred while processing
	 * request") on the outer error, with the actual cause buried in {@code caused_by},
	 * {@code root_cause[]}, {@code metadata}, or {@code stack_trace}. Surface all of them so
	 * the failure is diagnosable.
	 */
	static String describeError(ErrorCause error) {
		if (error == null) {
			return "?";
		}
		StringBuilder sb = new StringBuilder();
		appendErrorCauseDetail(sb, error);
		ErrorCause current = error.causedBy();
		while (current != null) {
			sb.append(" caused by ");
			appendErrorCauseDetail(sb, current);
			current = current.causedBy();
		}
		return sb.toString();
	}

	private static void appendErrorCauseDetail(StringBuilder sb, ErrorCause c) {
		sb.append(c.type() == null ? "?" : c.type())
				.append(": ")
				.append(c.reason() == null ? "?" : c.reason());
		if (!c.rootCause().isEmpty()) {
			sb.append(" [rootCause=");
			boolean first = true;
			for (ErrorCause rc : c.rootCause()) {
				if (!first) sb.append(", ");
				sb.append(rc.type() == null ? "?" : rc.type())
						.append(": ")
						.append(rc.reason() == null ? "?" : rc.reason());
				first = false;
			}
			sb.append("]");
		}
		if (!c.metadata().isEmpty()) {
			sb.append(" [metadata=").append(c.metadata()).append("]");
		}
		if (c.stackTrace() != null) {
			sb.append(" [stackTrace=").append(c.stackTrace()).append("]");
		}
	}

	/**
	 * Format a single failed {@link BulkResponseItem}.
	 * AOSS often returns a generic {@code error.reason} on each item while leaving the real
	 * cause in {@code shards.failures[]} and {@code status}. Surface both so callers (and the
	 * persisted {@code SEARCH_INDEX_STATUS.errorMessage}) see the whole story.
	 */
	static String describeBulkItemFailure(
			BulkResponseItem item) {
		StringBuilder sb = new StringBuilder();
		sb.append("doc ").append(item.id())
				.append(" [status=").append(item.status()).append("]: ")
				.append(describeError(item.error()));
		ShardStatistics shards = item.shards();
		if (shards != null && !shards.failures().isEmpty()) {
			sb.append(" [shardFailures=");
			boolean first = true;
			for (ShardSearchFailure sf : shards.failures()) {
				if (!first) sb.append(", ");
				sb.append("shard=").append(sf.shard());
				if (sf.index() != null) sb.append(" index=").append(sf.index());
				if (sf.node() != null) sb.append(" node=").append(sf.node());
				sb.append(" reason=").append(describeError(sf.reason()));
				first = false;
			}
			sb.append("]");
		}
		return sb.toString();
	}

	@Override
	public SearchQueryResults search(String indexName, SearchQuery query, List<ColumnModel> columns,
			String defaultAnalyzer, List<ColumnAnalyzerOverride> columnAnalyzerOverrides,
			Map<String, TextAnalyzer> analyzers, Set<SearchQueryPart> options) {
		return executeSearch(indexName, query, columns, defaultAnalyzer, columnAnalyzerOverrides, analyzers, options);
	}

	@Override
	public SearchQueryResults autocomplete(String indexName, SearchQuery query, List<ColumnModel> columns,
			String defaultAnalyzer, List<ColumnAnalyzerOverride> columnAnalyzerOverrides,
			Map<String, TextAnalyzer> analyzers, Set<SearchQueryPart> options) {
		query.setQueryType(SearchQueryType.PREFIX);
		if (query.getLimit() == null || query.getLimit() > AUTOCOMPLETE_MAX_LIMIT) {
			query.setLimit((long) AUTOCOMPLETE_MAX_LIMIT);
		}
		return executeSearch(indexName, query, columns, defaultAnalyzer, columnAnalyzerOverrides, analyzers, options);
	}

	// ---- Private helpers ----

	private void registerTokenFilters(IndexSettingsAnalysis.Builder a, String filtersJson) {
		Map<String, TokenFilterDefinition> defs = deserializeDefinitionMap(filtersJson, TokenFilterDefinition._DESERIALIZER);
		for (Map.Entry<String, TokenFilterDefinition> entry : defs.entrySet()) {
			TokenFilterDefinition def = entry.getValue();
			a.filter(entry.getKey(), f -> f.definition(def));
		}
	}

	private void registerCharFilters(IndexSettingsAnalysis.Builder a, String filtersJson) {
		Map<String, CharFilterDefinition> defs = deserializeDefinitionMap(filtersJson, CharFilterDefinition._DESERIALIZER);
		for (Map.Entry<String, CharFilterDefinition> entry : defs.entrySet()) {
			CharFilterDefinition def = entry.getValue();
			a.charFilter(entry.getKey(), f -> f.definition(def));
		}
	}

	private void registerTokenizer(IndexSettingsAnalysis.Builder a, String tokenizerName, String tokenizerConfigJson) {
		TokenizerDefinition def = deserializeDefinition(tokenizerConfigJson, TokenizerDefinition._DESERIALIZER);
		a.tokenizer(tokenizerName, t -> t.definition(def));
	}

	private <T> Map<String, T> deserializeDefinitionMap(String json, JsonpDeserializer<T> valueDeserializer) {
		return deserialize(json, JsonpDeserializer.stringMapDeserializer(valueDeserializer));
	}

	private <T> T deserializeDefinition(String json, JsonpDeserializer<T> deserializer) {
		return deserialize(json, deserializer);
	}

	private <T> T deserialize(String json, JsonpDeserializer<T> deserializer) {
		JsonpMapper mapper = openSearchClient._transport().jsonpMapper();
		try (JsonParser parser = mapper.jsonProvider().createParser(new StringReader(json))) {
			return deserializer.deserialize(parser, mapper);
		}
	}

	private SearchQueryResults executeSearch(String indexName, SearchQuery query, List<ColumnModel> columns,
			String defaultAnalyzer, List<ColumnAnalyzerOverride> columnAnalyzerOverrides,
			Map<String, TextAnalyzer> analyzers, Set<SearchQueryPart> options) {

		Map<String, String> nameToId = columns.stream()
				.collect(Collectors.toMap(ColumnModel::getName, ColumnModel::getId, (a2, b) -> a2));
		Map<String, ColumnAnalyzerOverrideEntry> overrideMap = buildOverrideMap(columnAnalyzerOverrides, nameToId);
		Map<String, ColumnModel> columnMap = columns.stream()
				.collect(Collectors.toMap(ColumnModel::getId, c -> c, (a2, b) -> a2));

		SearchQueryType queryType = query.getQueryType() != null ? query.getQueryType()
				: SearchQueryType.SIMPLE_QUERY_STRING;
		String queryText = query.getQueryText();
		int offset = query.getOffset() != null ? query.getOffset().intValue() : 0;
		int limit = query.getLimit() != null ? Math.min(query.getLimit().intValue(), MAX_LIMIT) : DEFAULT_LIMIT;
		String fuzziness = query.getFuzziness();

		if (queryText == null || queryText.trim().isEmpty()) {
			queryType = SearchQueryType.MATCH_ALL;
		}

		final SearchQueryType finalQueryType = queryType;
		final String finalQueryText = queryText;
		Map<Long, String> idToQualifiedName = buildIdToQualifiedNameMap(analyzers);

		List<String> resolvedQueryFields = resolveQueryFields(query.getQueryFields(), columns, defaultAnalyzer, overrideMap, analyzers, idToQualifiedName, true);

		BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

		Query mainQuery = buildMainQuery(finalQueryType, finalQueryText, resolvedQueryFields, fuzziness);
		boolBuilder.must(mainQuery);

		addFilters(boolBuilder, query, columnMap, nameToId, defaultAnalyzer, overrideMap, analyzers, idToQualifiedName);

		// Skip aggregation construction entirely when the caller didn't ask for FACETS.
		Map<String, Aggregation> aggregations = options.contains(SearchQueryPart.FACETS)
				? buildAggregations(query.getFacetRequests(), columnMap, nameToId, defaultAnalyzer,
						overrideMap, analyzers, idToQualifiedName)
				: Collections.emptyMap();

		Map<String, HighlightField> highlightFields = null;
		if (options.contains(SearchQueryPart.HITS) && Boolean.TRUE.equals(query.getHighlight())) {
			highlightFields = buildHighlightFields(columns, defaultAnalyzer, overrideMap, analyzers, idToQualifiedName);
		}

		List<String> returnFields = query.getReturnFields();

		List<SortOptions> sortOptions = options.contains(SearchQueryPart.HITS)
				? buildSortOptions(query.getSort(), columnMap, nameToId, defaultAnalyzer, overrideMap, analyzers, idToQualifiedName)
				: Collections.emptyList();

		Map<String, String> idToName = columns.stream()
				.collect(Collectors.toMap(ColumnModel::getId, ColumnModel::getName, (a2, b) -> a2));

		return callSearchApi(indexName, boolBuilder, offset, limit, aggregations,
				highlightFields, returnFields, sortOptions, idToName, options);
	}

	@SuppressWarnings("rawtypes")
	SearchQueryResults callSearchApi(String indexName, BoolQuery.Builder boolBuilder,
			int offset, int limit, Map<String, Aggregation> aggregations,
			Map<String, HighlightField> highlightFields, List<String> returnFields,
			List<SortOptions> sortOptions, Map<String, String> idToName,
			Set<SearchQueryPart> options) {
		boolean returnHits = options.contains(SearchQueryPart.HITS);
		boolean returnTotalHits = options.contains(SearchQueryPart.TOTAL_HITS);
		try {
			SearchResponse<Map> response = openSearchClient.search(req -> {
				req.index(indexName);
				req.query(q -> q.bool(boolBuilder.build()));
				req.from(offset);
				// size=0 when hits aren't requested — saves source fetch + transport cost
				req.size(returnHits ? limit : 0);
				if (returnTotalHits) {
					// Use a count value instead of enabled(true) — the boolean form caps at 10k
					req.trackTotalHits(t -> t.count(Integer.MAX_VALUE));
				} else {
					req.trackTotalHits(t -> t.enabled(false));
				}

				if (!aggregations.isEmpty()) {
					req.aggregations(aggregations);
				}
				// Highlights and source filters are meaningless without hits.
				if (returnHits) {
					if (highlightFields != null && !highlightFields.isEmpty()) {
						req.highlight(h -> h.fields(highlightFields));
					}
					if (returnFields != null && !returnFields.isEmpty()) {
						req.source(src -> src.filter(f -> f.includes(returnFields)));
					}
					if (!sortOptions.isEmpty()) {
						req.sort(sortOptions);
					}
				}

				return req;
			}, Map.class);

			return convertResponse(response, indexName, offset, idToName, options);
		} catch (OpenSearchException e) {
			if (INDEX_NOT_FOUND_EXCEPTION.equals(e.error().type())) {
				throw new IllegalStateException("Search index is still building. Please try again later.", e);
			}
			throw new RuntimeException("Failed to execute search on search index: " + indexName
					+ " (" + describeError(e.error()) + ")", e);
		} catch (IOException e) {
			throw new RuntimeException("Failed to execute search on search index: " + indexName, e);
		}
	}

	Query buildMainQuery(SearchQueryType queryType, String queryText, List<String> fields, String fuzziness) {
		switch (queryType) {
			case SIMPLE_QUERY_STRING:
				return Query.of(q -> q.simpleQueryString(sqs -> {
					sqs.query(queryText);
					if (fields != null && !fields.isEmpty()) {
						sqs.fields(fields);
					}
					return sqs;
				}));
			case MATCH:
				ValidateArgument.requiredNotEmpty(fields, "fields for MATCH query");
				String matchField = stripBoost(fields.get(0));
				return Query.of(q -> q.match(m -> {
					m.field(matchField).query(FieldValue.of(queryText));
					if (fuzziness != null) {
						m.fuzziness(fuzziness);
					}
					return m;
				}));
			case MULTI_MATCH:
				return Query.of(q -> q.multiMatch(mm -> {
					mm.query(queryText);
					if (fields != null && !fields.isEmpty()) {
						mm.fields(fields);
					}
					if (fuzziness != null) {
						mm.fuzziness(fuzziness);
					}
					return mm;
				}));
			case MATCH_PHRASE:
				ValidateArgument.requiredNotEmpty(fields, "fields for MATCH_PHRASE query");
				String phraseField = stripBoost(fields.get(0));
				return Query.of(q -> q.matchPhrase(mp -> mp.field(phraseField).query(queryText)));
			case PREFIX:
				return Query.of(q -> q.multiMatch(mm -> {
					mm.query(queryText);
					mm.type(TextQueryType.BoolPrefix);
					if (fields != null && !fields.isEmpty()) {
						mm.fields(fields);
					}
					return mm;
				}));
			case WILDCARD:
				ValidateArgument.requiredNotEmpty(fields, "fields for WILDCARD query");
				String wildcardField = stripBoost(fields.get(0));
				return Query.of(q -> q.wildcard(w -> w.field(wildcardField).value(queryText)));
			case MATCH_ALL:
				return Query.of(q -> q.matchAll(m -> m));
			default:
				throw new IllegalArgumentException("Unsupported query type: " + queryType);
		}
	}

	private void addFilters(BoolQuery.Builder boolBuilder, SearchQuery query,
			Map<String, ColumnModel> columnMap, Map<String, String> nameToId, String defaultAnalyzer,
			Map<String, ColumnAnalyzerOverrideEntry> overrideMap, Map<String, TextAnalyzer> analyzers,
			Map<Long, String> idToQualifiedName) {
		addTermsFilters(boolBuilder, query.getTermsFilters(), columnMap, nameToId, defaultAnalyzer, overrideMap, analyzers, idToQualifiedName);
		addRangeFilters(boolBuilder, query.getRangeFilters(), columnMap, nameToId, defaultAnalyzer, overrideMap, analyzers, idToQualifiedName);
		addExistsFilters(boolBuilder, query.getExistsFilters(), nameToId, false);
		addExistsFilters(boolBuilder, query.getNotExistsFilters(), nameToId, true);
	}

	private void addTermsFilters(BoolQuery.Builder boolBuilder, List<KeyValues> termsFilters,
			Map<String, ColumnModel> columnMap, Map<String, String> nameToId, String defaultAnalyzer,
			Map<String, ColumnAnalyzerOverrideEntry> overrideMap, Map<String, TextAnalyzer> analyzers,
			Map<Long, String> idToQualifiedName) {
		if (termsFilters == null) {
			return;
		}
		for (KeyValues kvs : termsFilters) {
			String columnId = nameToId.getOrDefault(kvs.getKey(), kvs.getKey());
			String fieldName = getFilterFieldName(columnId, columnMap, defaultAnalyzer, overrideMap, analyzers, idToQualifiedName);
			List<FieldValue> fieldValues = kvs.getValues().stream()
					.map(FieldValue::of)
					.collect(Collectors.toList());
			Query termsQuery = Query.of(q -> q.terms(t -> t
					.field(fieldName)
					.terms(tv -> tv.value(fieldValues))));
			if (Boolean.TRUE.equals(kvs.getNot())) {
				boolBuilder.mustNot(termsQuery);
			} else {
				boolBuilder.filter(termsQuery);
			}
		}
	}

	private void addRangeFilters(BoolQuery.Builder boolBuilder, List<KeyRange> rangeFilters,
			Map<String, ColumnModel> columnMap, Map<String, String> nameToId, String defaultAnalyzer,
			Map<String, ColumnAnalyzerOverrideEntry> overrideMap, Map<String, TextAnalyzer> analyzers,
			Map<Long, String> idToQualifiedName) {
		if (rangeFilters == null) {
			return;
		}
		for (KeyRange kr : rangeFilters) {
			String columnId = nameToId.getOrDefault(kr.getKey(), kr.getKey());
			String fieldName = getFilterFieldName(columnId, columnMap, defaultAnalyzer, overrideMap, analyzers, idToQualifiedName);
			Query rangeQuery = Query.of(q -> q.range(r -> {
				r.field(fieldName);
				if (kr.getMin() != null) {
					r.gte(JsonData.of(kr.getMin()));
				}
				if (kr.getMax() != null) {
					r.lte(JsonData.of(kr.getMax()));
				}
				return r;
			}));
			boolBuilder.filter(rangeQuery);
		}
	}

	private void addExistsFilters(BoolQuery.Builder boolBuilder, List<String> fields,
			Map<String, String> nameToId, boolean negate) {
		if (fields == null) {
			return;
		}
		for (String fieldName : fields) {
			String fieldId = nameToId.getOrDefault(fieldName, fieldName);
			Query existsQuery = Query.of(q -> q.exists(e -> e.field(fieldId)));
			if (negate) {
				boolBuilder.mustNot(existsQuery);
			} else {
				boolBuilder.filter(existsQuery);
			}
		}
	}

	Map<String, Aggregation> buildAggregations(List<FacetRequest> facetRequests,
			Map<String, ColumnModel> columnMap, Map<String, String> nameToId, String defaultAnalyzer,
			Map<String, ColumnAnalyzerOverrideEntry> overrideMap, Map<String, TextAnalyzer> analyzers,
			Map<Long, String> idToQualifiedName) {
		if (facetRequests == null || facetRequests.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<String, Aggregation> aggregations = new HashMap<>();
		for (FacetRequest facet : facetRequests) {
			String columnId = nameToId.getOrDefault(facet.getColumnName(), facet.getColumnName());
			String fieldName = getFilterFieldName(columnId, columnMap, defaultAnalyzer, overrideMap, analyzers, idToQualifiedName);
			int maxValues = facet.getMaxValueCount() != null ? facet.getMaxValueCount().intValue() : DEFAULT_FACET_SIZE;

			String sortField = facet.getSortField() == FacetSortField.KEY ? "_key" : "_count";
			SortOrder sortOrder = (facet.getSortDirection() != null && facet.getSortDirection() == SortDirection.ASC)
					? SortOrder.Asc : SortOrder.Desc;
			final String finalSortField = sortField;
			final SortOrder finalSortOrder = sortOrder;

			aggregations.put(columnId, Aggregation.of(a -> a
					.terms(t -> t.field(fieldName).size(maxValues)
							.order(List.of(Map.of(finalSortField, finalSortOrder))))));
		}
		return aggregations;
	}

	Map<String, HighlightField> buildHighlightFields(List<ColumnModel> columns,
			String defaultAnalyzer, Map<String, ColumnAnalyzerOverrideEntry> overrideMap,
			Map<String, TextAnalyzer> analyzers, Map<Long, String> idToQualifiedName) {
		Map<String, HighlightField> highlightFields = new HashMap<>();
		for (ColumnModel column : columns) {
			String columnId = column.getId();
			ColumnType colType = column.getColumnType();
			if (!ColumnTypeToOpenSearchMapping.isTextType(colType) && !ColumnTypeToOpenSearchMapping.isLinkType(colType)) {
				continue;
			}
			String effectiveName = resolveEffectiveAnalyzerName(
					columnId, colType, defaultAnalyzer, overrideMap, idToQualifiedName);
			TextAnalyzer analyzer = analyzers.get(effectiveName);
			if (ColumnTypeToOpenSearchMapping.isLinkType(colType) && isKeywordAnalyzer(analyzer)) {
				highlightFields.put(columnId + "." + SUB_FIELD_SEARCHABLE, HighlightField.of(h -> h));
			} else {
				highlightFields.put(columnId, HighlightField.of(h -> h));
			}
		}
		return highlightFields;
	}

	List<SortOptions> buildSortOptions(List<SortField> sortFields,
			Map<String, ColumnModel> columnMap, Map<String, String> nameToId, String defaultAnalyzer,
			Map<String, ColumnAnalyzerOverrideEntry> overrideMap, Map<String, TextAnalyzer> analyzers,
			Map<Long, String> idToQualifiedName) {
		if (sortFields == null || sortFields.isEmpty()) {
			return Collections.singletonList(
				SortOptions.of(so -> so.field(FieldSort.of(fs -> fs.field("_score").order(SortOrder.Desc))))
			);
		}
		return sortFields.stream()
				.map(sf -> {
					String sortField;
					if ("_score".equals(sf.getColumnName())) {
						sortField = "_score";
					} else {
						String columnId = nameToId.getOrDefault(sf.getColumnName(), sf.getColumnName());
						sortField = getFilterFieldName(columnId, columnMap, defaultAnalyzer, overrideMap, analyzers, idToQualifiedName);
					}
					SortOrder order = (sf.getDirection() == SortDirection.ASC) ? SortOrder.Asc : SortOrder.Desc;
					return SortOptions.of(so -> so.field(FieldSort.of(fs -> fs.field(sortField).order(order))));
				})
				.collect(Collectors.toList());
	}

	String getFilterFieldName(String columnId, Map<String, ColumnModel> columnMap,
			String defaultAnalyzer, Map<String, ColumnAnalyzerOverrideEntry> overrideMap,
			Map<String, TextAnalyzer> analyzers, Map<Long, String> idToQualifiedName) {
		ColumnModel column = columnMap.get(columnId);
		if (column == null) {
			return columnId;
		}

		ColumnType colType = column.getColumnType();

		if (ColumnTypeToOpenSearchMapping.isTextType(colType)) {
			return columnId + "." + SUB_FIELD_KEYWORD;
		}

		if (ColumnTypeToOpenSearchMapping.isLinkType(colType)) {
			String effectiveName = resolveEffectiveAnalyzerName(columnId, colType, defaultAnalyzer, overrideMap, idToQualifiedName);
			TextAnalyzer analyzer = analyzers != null ? analyzers.get(effectiveName) : null;
			if (isKeywordAnalyzer(analyzer)) {
				return columnId;
			}
			return columnId + "." + SUB_FIELD_KEYWORD;
		}

		return columnId;
	}

	String getSearchFieldName(String columnId, Map<String, ColumnModel> columnMap,
			String defaultAnalyzer, Map<String, ColumnAnalyzerOverrideEntry> overrideMap,
			Map<String, TextAnalyzer> analyzers, Map<Long, String> idToQualifiedName) {
		ColumnModel column = columnMap.get(columnId);
		if (column == null) {
			return columnId;
		}

		ColumnType colType = column.getColumnType();

		if (ColumnTypeToOpenSearchMapping.isLinkType(colType)) {
			String effectiveName = resolveEffectiveAnalyzerName(columnId, colType, defaultAnalyzer, overrideMap, idToQualifiedName);
			TextAnalyzer analyzer = analyzers != null ? analyzers.get(effectiveName) : null;
			if (isKeywordAnalyzer(analyzer)) {
				return columnId + "." + SUB_FIELD_SEARCHABLE;
			}
		}

		return columnId;
	}

	List<String> resolveQueryFields(List<String> queryFields, List<ColumnModel> columns,
			String defaultAnalyzer, Map<String, ColumnAnalyzerOverrideEntry> overrideMap,
			Map<String, TextAnalyzer> analyzers, Map<Long, String> idToQualifiedName, boolean forSearch) {
		if (queryFields == null || queryFields.isEmpty()) {
			return null;
		}

		Map<String, ColumnModel> columnMap = columns.stream()
				.collect(Collectors.toMap(ColumnModel::getId, c -> c, (a2, b) -> a2));
		Map<String, String> nameToIdLocal = columns.stream()
				.collect(Collectors.toMap(ColumnModel::getName, ColumnModel::getId, (a2, b) -> a2));

		return queryFields.stream()
				.map(field -> {
					String boost = null;
					String fieldName = field;
					int caretIndex = field.indexOf('^');
					if (caretIndex > 0) {
						fieldName = field.substring(0, caretIndex);
						boost = field.substring(caretIndex);
					}
					String columnId = nameToIdLocal.getOrDefault(fieldName, fieldName);
					String resolved = forSearch
							? getSearchFieldName(columnId, columnMap, defaultAnalyzer, overrideMap, analyzers, idToQualifiedName)
							: getFilterFieldName(columnId, columnMap, defaultAnalyzer, overrideMap, analyzers, idToQualifiedName);
					return boost != null ? resolved + boost : resolved;
				})
				.collect(Collectors.toList());
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	SearchQueryResults convertResponse(SearchResponse<Map> response, String indexName, int offset,
			Map<String, String> idToName, Set<SearchQueryPart> options) {
		SearchQueryResults results = new SearchQueryResults();
		results.setOffset((long) offset);

		if (options.contains(SearchQueryPart.TOTAL_HITS)) {
			results.setTotalHits(response.hits().total() != null ? response.hits().total().value() : 0L);
		}

		if (options.contains(SearchQueryPart.HITS)) {
			List<SearchHit> hits = new ArrayList<>();
			for (Hit<Map> hit : response.hits().hits()) {
				hits.add(convertHit(hit, idToName));
			}
			results.setHits(hits);
		}

		if (options.contains(SearchQueryPart.FACETS)
				&& response.aggregations() != null && !response.aggregations().isEmpty()) {
			results.setFacets(convertAggregations(response.aggregations(), idToName));
		}

		return results;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	SearchHit convertHit(Hit<Map> hit, Map<String, String> idToName) {
		SearchHit searchHit = new SearchHit();
		searchHit.setScore(hit.score());

		Map<String, Object> source = hit.source();
		if (source != null) {
			searchHit.setRowId(toLong(source.get(SYSTEM_FIELD_ROW_ID)));
			searchHit.setRowVersion(toLong(source.get(SYSTEM_FIELD_ROW_VERSION)));

			List<SearchFieldValue> fields = source.entrySet().stream()
					.filter(e -> !SYSTEM_FIELD_ROW_ID.equals(e.getKey()) && !SYSTEM_FIELD_ROW_VERSION.equals(e.getKey()))
					.map(e -> {
						SearchFieldValue fv = new SearchFieldValue();
						fv.setName(idToName.getOrDefault(e.getKey(), e.getKey()));
						fv.setValue(convertFieldValue(e.getValue()));
						return fv;
					})
					.collect(Collectors.toList());
			searchHit.setFields(fields);
		}

		if (hit.highlight() != null && !hit.highlight().isEmpty()) {
			searchHit.setHighlights(convertHighlights(hit.highlight(), idToName));
		}

		return searchHit;
	}

	/**
	 * Stringify a value from an AOSS hit's {@code _source} for {@link SearchFieldValue#setValue(String)}.
	 * Lists and maps (i.e. {@code *_LIST} and {@code JSON} columns) are written as canonical JSON
	 * so clients can parse them back; scalars use {@link String#valueOf(Object)} so a raw {@code String}
	 * column is not double-quoted in the response. Mirrors the pattern at {@code SQLUtils#bindListColumns}
	 * (lib-table-cluster) which serializes typed Java lists for the table index DB the same way.
	 */
	static String convertFieldValue(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Collection) {
			return new JSONArray((Collection<?>) value).toString();
		}
		if (value instanceof Map) {
			return new JSONObject((Map<?, ?>) value).toString();
		}
		return String.valueOf(value);
	}

	List<SearchFieldValue> convertHighlights(Map<String, List<String>> highlightMap,
			Map<String, String> idToName) {
		List<SearchFieldValue> highlights = new ArrayList<>();
		for (Map.Entry<String, List<String>> entry : highlightMap.entrySet()) {
			String fieldName = entry.getKey();
			if (fieldName.endsWith("." + SUB_FIELD_SEARCHABLE)) {
				fieldName = fieldName.substring(0, fieldName.length() - SUB_FIELD_SEARCHABLE.length() - 1);
			}
			fieldName = idToName.getOrDefault(fieldName, fieldName);
			SearchFieldValue hv = new SearchFieldValue();
			hv.setName(fieldName);
			hv.setValue(String.join(" ... ", entry.getValue()));
			highlights.add(hv);
		}
		return highlights;
	}

	List<FacetColumnResult> convertAggregations(Map<String, Aggregate> aggregations,
			Map<String, String> idToName) {
		List<FacetColumnResult> facets = new ArrayList<>();
		for (Map.Entry<String, Aggregate> entry : aggregations.entrySet()) {
			String columnName = idToName.getOrDefault(entry.getKey(), entry.getKey());
			Aggregate aggregate = entry.getValue();

			if (aggregate.isSterms()) {
				List<FacetColumnResultValueCount> valueCounts = aggregate.sterms().buckets().array().stream()
						.map(bucket -> buildFacetValueCount(bucket.key(), bucket.docCount()))
						.collect(Collectors.toList());
				facets.add(buildFacetResult(columnName, valueCounts));
			} else if (aggregate.isLterms()) {
				List<FacetColumnResultValueCount> valueCounts = aggregate.lterms().buckets().array().stream()
						.map(bucket -> buildFacetValueCount(bucket.keyAsString(), bucket.docCount()))
						.collect(Collectors.toList());
				facets.add(buildFacetResult(columnName, valueCounts));
			} else if (aggregate.isDterms()) {
				List<FacetColumnResultValueCount> valueCounts = aggregate.dterms().buckets().array().stream()
						.map(bucket -> buildFacetValueCount(String.valueOf(bucket.key()), bucket.docCount()))
						.collect(Collectors.toList());
				facets.add(buildFacetResult(columnName, valueCounts));
			}
		}
		return facets;
	}

	FacetColumnResultValues buildFacetResult(String columnName, List<FacetColumnResultValueCount> valueCounts) {
		FacetColumnResultValues result = new FacetColumnResultValues();
		result.setColumnName(columnName);
		result.setFacetType(FacetType.enumeration);
		result.setFacetValues(valueCounts);
		return result;
	}

	FacetColumnResultValueCount buildFacetValueCount(String key, long docCount) {
		FacetColumnResultValueCount vc = new FacetColumnResultValueCount();
		vc.setValue(key);
		vc.setCount(docCount);
		vc.setIsSelected(false);
		return vc;
	}

	/**
	 * Build the OpenSearch field property for a given column type and effective analyzer.
	 */
	private Property buildProperty(ColumnType columnType,
			TextAnalyzer effectiveAnalyzer, ColumnAnalyzerOverrideEntry entry,
			Map<String, TextAnalyzer> analyzers, boolean hasSynonyms) {

		if (ColumnTypeToOpenSearchMapping.isTextType(columnType)) {
			return buildTextProperty(columnType, effectiveAnalyzer, entry, analyzers, hasSynonyms);
		}

		if (ColumnTypeToOpenSearchMapping.isLinkType(columnType)) {
			if (isKeywordAnalyzer(effectiveAnalyzer)) {
				return buildKeywordWithSearchableProperty(analyzers);
			}
			return buildTextProperty(columnType, effectiveAnalyzer, entry, analyzers, hasSynonyms);
		}

		if (ColumnTypeToOpenSearchMapping.isKeywordType(columnType)) {
			Integer ignoreAbove = ColumnTypeToOpenSearchMapping.getIgnoreAbove(columnType);
			int ia = ignoreAbove != null ? ignoreAbove : 256;
			return Property.of(p -> p.keyword(k -> k.ignoreAbove(ia)));
		}

		if (ColumnTypeToOpenSearchMapping.isLongType(columnType)) {
			return Property.of(p -> p.long_(l -> l));
		}

		if (ColumnTypeToOpenSearchMapping.isDoubleType(columnType)) {
			return Property.of(p -> p.double_(d -> d));
		}

		if (ColumnTypeToOpenSearchMapping.isBooleanType(columnType)) {
			return Property.of(p -> p.boolean_(b -> b));
		}

		if (ColumnTypeToOpenSearchMapping.isJsonType(columnType)) {
			return Property.of(p -> p.object(o -> o.dynamic(DynamicMapping.True)));
		}

		// Fallback: text
		return Property.of(p -> p.text(t -> t));
	}

	private Property buildTextProperty(ColumnType columnType,
			TextAnalyzer effectiveAnalyzer, ColumnAnalyzerOverrideEntry entry,
			Map<String, TextAnalyzer> analyzers, boolean hasSynonyms) {
		Integer ignoreAbove = ColumnTypeToOpenSearchMapping.getIgnoreAbove(columnType);
		int ia = ignoreAbove != null ? ignoreAbove : 1000;

		String indexAnalyzerName = resolveIndexAnalyzerName(effectiveAnalyzer, entry, analyzers);
		String searchAnalyzerName = resolveSearchAnalyzerName(
				indexAnalyzerName, effectiveAnalyzer, entry, analyzers, hasSynonyms);
		final int finalIa = ia;

		return Property.of(p -> p.text(t -> {
			t.analyzer(indexAnalyzerName);
			if (!indexAnalyzerName.equals(searchAnalyzerName)) {
				t.searchAnalyzer(searchAnalyzerName);
			}
			t.fields(SUB_FIELD_KEYWORD, f -> f.keyword(k -> k.ignoreAbove(finalIa)));
			return t;
		}));
	}

	String resolveIndexAnalyzerName(TextAnalyzer effectiveAnalyzer,
			ColumnAnalyzerOverrideEntry entry, Map<String, TextAnalyzer> analyzers) {
		if (entry != null && entry.getIndexAnalyzer() != null) {
			return analyzerToOpenSearchName(analyzers.get(entry.getIndexAnalyzer()));
		}
		return analyzerToOpenSearchName(effectiveAnalyzer);
	}

	String resolveSearchAnalyzerName(String indexAnalyzerName, TextAnalyzer effectiveAnalyzer,
			ColumnAnalyzerOverrideEntry entry, Map<String, TextAnalyzer> analyzers, boolean hasSynonyms) {
		TextAnalyzer chosen = effectiveAnalyzer;
		String chosenName = indexAnalyzerName;
		if (entry != null && entry.getSearchAnalyzer() != null) {
			chosen = analyzers.get(entry.getSearchAnalyzer());
			chosenName = analyzerToOpenSearchName(chosen);
		} else if (entry != null && entry.getIndexAnalyzer() != null) {
			chosen = analyzers.get(entry.getIndexAnalyzer());
		}
		// Mirror registerAnalyzer's decision: pick the _search variant exactly when one
		// was registered. Otherwise the analyzer is symmetric and OpenSearch reuses the
		// index-time analyzer.
		if (chosen != null && chosen.getSettings() != null
				&& shouldRegisterSearchVariant(chosen.getSettings(), hasSynonyms)) {
			return chosenName + SEARCH_ANALYZER_SUFFIX;
		}
		return chosenName;
	}

	private Property buildKeywordWithSearchableProperty(Map<String, TextAnalyzer> analyzers) {
		Map<Long, String> reverse = buildIdToQualifiedNameMap(analyzers);
		String scientificQualifiedName = reverse.get(TextAnalyzerBootstrapper.SCIENTIFIC_ID);
		TextAnalyzer scientificAnalyzer = analyzers.get(scientificQualifiedName);
		String scientificName = analyzerToOpenSearchName(scientificAnalyzer);
		return Property.of(p -> p.keyword(k -> k
				.ignoreAbove(1000)
				.fields(SUB_FIELD_SEARCHABLE, f -> f.text(t -> t
						.analyzer(scientificName)))));
	}

	/**
	 * Resolve the effective analyzer qualified name for a column, checking overrides first,
	 * then the default analyzer, then the column type default.
	 */
	String resolveEffectiveAnalyzerName(String columnId, ColumnType columnType,
			String defaultAnalyzer, Map<String, ColumnAnalyzerOverrideEntry> overrideMap,
			Map<Long, String> idToQualifiedName) {
		ColumnAnalyzerOverrideEntry entry = overrideMap.get(columnId);
		if (entry != null && entry.getIndexAnalyzer() != null) {
			return entry.getIndexAnalyzer();
		}
		if (defaultAnalyzer != null) {
			return defaultAnalyzer;
		}
		Long defaultId = ColumnTypeToOpenSearchMapping.getDefaultAnalyzerId(columnType);
		return idToQualifiedName.get(defaultId);
	}

	Map<String, ColumnAnalyzerOverrideEntry> buildOverrideMap(
			List<ColumnAnalyzerOverride> columnAnalyzerOverrides, Map<String, String> nameToId) {
		Map<String, ColumnAnalyzerOverrideEntry> map = new HashMap<>();
		if (columnAnalyzerOverrides == null) {
			return map;
		}
		for (ColumnAnalyzerOverride cao : columnAnalyzerOverrides) {
			if (cao.getOverrides() == null) {
				continue;
			}
			for (ColumnAnalyzerOverrideEntry entry : cao.getOverrides()) {
				String columnId = nameToId.get(entry.getColumnName());
				if (columnId != null) {
					map.putIfAbsent(columnId, entry);
				}
			}
		}
		return map;
	}

	/**
	 * Flattens user-authored {@link SynonymSet}s into the OpenSearch synonym filter's wire
	 * format. Each rule is emitted twice: once with the user's original casing, once
	 * lowercased (skipped if the rule is already all-lowercase). This means a query in any
	 * casing — {@code BRCA1}, {@code brca1}, {@code Brca1} — triggers expansion regardless
	 * of where {@code lowercase} sits in the analyzer's search-time chain. Users author
	 * rules in natural casing without learning the chain's casing rules.
	 *
	 * <p>Duplicates collapse via {@link java.util.LinkedHashSet}, so a rule whose terms
	 * are already lowercase doesn't produce two identical lines.
	 */
	List<String> buildSynonymRules(List<SynonymSet> synonymSets) {
		if (synonymSets == null || synonymSets.isEmpty()) {
			return Collections.emptyList();
		}
		java.util.LinkedHashSet<String> rules = new java.util.LinkedHashSet<>();
		for (SynonymSet ss : synonymSets) {
			if (ss.getRules() == null) {
				continue;
			}
			for (SynonymRule rule : ss.getRules()) {
				if (rule.getTerms() == null || rule.getTerms().size() < 2 || rule.getRuleType() == null) {
					continue;
				}
				addRuleVariant(rules, rule.getRuleType(), rule.getTerms());
				List<String> lowered = lowercaseAll(rule.getTerms());
				if (!lowered.equals(rule.getTerms())) {
					addRuleVariant(rules, rule.getRuleType(), lowered);
				}
			}
		}
		return new ArrayList<>(rules);
	}

	private static void addRuleVariant(java.util.LinkedHashSet<String> rules, SynonymRuleType type, List<String> terms) {
		if (type == SynonymRuleType.EQUIVALENT) {
			rules.add(String.join(", ", terms));
		} else if (type == SynonymRuleType.EXPLICIT) {
			rules.add(terms.get(0) + " => " + String.join(", ", terms.subList(1, terms.size())));
		}
	}

	private static List<String> lowercaseAll(List<String> terms) {
		List<String> result = new ArrayList<>(terms.size());
		for (String term : terms) {
			result.add(term == null ? null : term.toLowerCase(java.util.Locale.ROOT));
		}
		return result;
	}

	private String analyzerToOpenSearchName(TextAnalyzer analyzer) {
		return ANALYZER_PREFIX + analyzer.getId();
	}

	static Map<Long, String> buildIdToQualifiedNameMap(Map<String, TextAnalyzer> analyzers) {
		Map<Long, String> result = new HashMap<>();
		for (Map.Entry<String, TextAnalyzer> entry : analyzers.entrySet()) {
			result.put(Long.parseLong(entry.getValue().getId()), entry.getKey());
		}
		return result;
	}

	boolean isKeywordAnalyzer(TextAnalyzer analyzer) {
		return analyzer != null && analyzer.getSettings() != null
				&& "keyword".equals(analyzer.getSettings().getTokenizer());
	}

	String stripBoost(String fieldSpec) {
		int caretIndex = fieldSpec.indexOf('^');
		return caretIndex > 0 ? fieldSpec.substring(0, caretIndex) : fieldSpec;
	}

	Long toLong(Object value) {
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		try {
			return Long.parseLong(String.valueOf(value));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public void validateAnalyzerSettings(TextAnalyzerSettings settings) {
		ValidateArgument.required(settings, "settings");

		if (Boolean.TRUE.equals(settings.getSynonymAware())) {
			List<String> searchOrder = settings.getSearchFilterOrder();
			if (searchOrder == null || searchOrder.isEmpty() || !searchOrder.contains(SYNONYM_FILTER_NAME)) {
				throw new IllegalArgumentException("synonymAware analyzers must declare 'searchFilterOrder' and include '"
						+ SYNONYM_FILTER_NAME + "' in it. Place '" + SYNONYM_FILTER_NAME
						+ "' upstream of any 'word_delimiter'/'word_delimiter_graph' filters, and downstream of 'lowercase' for case-insensitive matching.");
			}
		}

		Tokenizer tokenizer = buildTokenizer(settings);
		List<TokenFilter> tokenFilters = buildTokenFilters(settings);
		List<CharFilter> charFilters = buildCharFilters(settings);

		try {
			openSearchClient.indices().analyze(req -> {
				req.tokenizer(tokenizer);
				req.text("The quick brown fox jumps over the lazy dog");
				if (!tokenFilters.isEmpty()) {
					req.filter(tokenFilters);
				}
				if (!charFilters.isEmpty()) {
					req.charFilter(charFilters);
				}
				return req;
			});
		} catch (OpenSearchException e) {
			throw new IllegalArgumentException(
				"Invalid analyzer configuration: " + describeError(e.error())
				+ ". Check your tokenizer, token filters, and character filters.", e);
		} catch (IOException e) {
			throw new IllegalStateException(
				"Unable to validate analyzer settings: the search service is temporarily unavailable. Please try again later.", e);
		}
	}

	private Tokenizer buildTokenizer(TextAnalyzerSettings settings) {
		try {
			if (settings.getTokenizerConfig() != null && !settings.getTokenizerConfig().isEmpty()) {
				TokenizerDefinition def = deserializeDefinition(settings.getTokenizerConfig(), TokenizerDefinition._DESERIALIZER);
				return Tokenizer.of(t -> t.definition(def));
			}
			String tokenizerName = settings.getTokenizer() != null ? settings.getTokenizer() : "standard";
			return Tokenizer.of(t -> t.name(tokenizerName));
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid tokenizer configuration: " + e.getMessage(), e);
		}
	}

	private List<TokenFilter> buildTokenFilters(TextAnalyzerSettings settings) {
		Map<String, TokenFilterDefinition> tokenFilterDefs = Collections.emptyMap();
		try {
			if (settings.getTokenFilters() != null && !settings.getTokenFilters().isEmpty()) {
				tokenFilterDefs = deserializeDefinitionMap(settings.getTokenFilters(), TokenFilterDefinition._DESERIALIZER);
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid tokenFilters JSON: " + e.getMessage(), e);
		}

		List<TokenFilter> tokenFilters = new ArrayList<>();
		if (settings.getIndexFilterOrder() == null) {
			return tokenFilters;
		}
		for (String filterName : settings.getIndexFilterOrder()) {
			TokenFilterDefinition def = tokenFilterDefs.get(filterName);
			if (def != null) {
				tokenFilters.add(TokenFilter.of(f -> f.definition(def)));
			} else {
				tokenFilters.add(TokenFilter.of(f -> f.name(filterName)));
			}
		}
		return tokenFilters;
	}

	private List<CharFilter> buildCharFilters(TextAnalyzerSettings settings) {
		Map<String, CharFilterDefinition> charFilterDefs = Collections.emptyMap();
		try {
			if (settings.getCharFilters() != null && !settings.getCharFilters().isEmpty()) {
				charFilterDefs = deserializeDefinitionMap(settings.getCharFilters(), CharFilterDefinition._DESERIALIZER);
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid charFilters JSON: " + e.getMessage(), e);
		}

		List<CharFilter> charFilters = new ArrayList<>();
		if (settings.getCharFilterOrder() == null) {
			return charFilters;
		}
		for (String filterName : settings.getCharFilterOrder()) {
			CharFilterDefinition def = charFilterDefs.get(filterName);
			if (def != null) {
				charFilters.add(CharFilter.of(f -> f.definition(def)));
			} else {
				charFilters.add(CharFilter.of(f -> f.name(filterName)));
			}
		}
		return charFilters;
	}
}
