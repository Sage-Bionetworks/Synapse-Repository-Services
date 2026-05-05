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

import org.json.JSONArray;
import org.json.JSONObject;

import jakarta.json.stream.JsonParser;

import org.opensearch.client.json.JsonData;
import org.opensearch.client.json.JsonpDeserializer;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.opensearch._types.analysis.CharFilter;
import org.opensearch.client.opensearch._types.analysis.CharFilterDefinition;
import org.opensearch.client.opensearch._types.analysis.TokenFilter;
import org.opensearch.client.opensearch._types.analysis.TokenFilterDefinition;
import org.opensearch.client.opensearch._types.analysis.Tokenizer;
import org.opensearch.client.opensearch._types.analysis.TokenizerDefinition;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.FieldSort;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.mapping.DynamicMapping;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TextQueryType;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.search.HighlightField;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnResult;
import org.sagebionetworks.repo.model.table.FacetColumnResultValueCount;
import org.sagebionetworks.repo.model.table.FacetColumnResultValues;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverrideEntry;
import org.sagebionetworks.repo.model.search.FacetRequest;
import org.sagebionetworks.repo.model.search.FacetSortField;
import org.sagebionetworks.repo.model.search.SearchFieldValue;
import org.sagebionetworks.repo.model.search.SearchHit;
import org.sagebionetworks.repo.model.search.SearchQuery;
import org.sagebionetworks.repo.model.search.SearchQueryType;
import org.sagebionetworks.repo.model.search.SearchQueryResults;
import org.sagebionetworks.repo.model.search.SortDirection;
import org.sagebionetworks.repo.model.search.SortField;
import org.sagebionetworks.repo.model.search.table.SynonymRule;
import org.sagebionetworks.repo.model.search.table.SynonymRuleType;
import org.sagebionetworks.repo.model.search.SearchQueryPart;
import org.sagebionetworks.repo.model.search.table.SynonymSet;
import org.sagebionetworks.repo.model.search.table.TextAnalyzer;
import org.sagebionetworks.repo.model.search.table.TextAnalyzerSettings;
import org.sagebionetworks.repo.model.search.KeyRange;
import org.sagebionetworks.repo.model.search.KeyValues;
import org.sagebionetworks.util.ValidateArgument;

import org.springframework.stereotype.Service;

/**
 * Implementation of {@link OpenSearchManager} that wraps the OpenSearch Java client
 * for all AOSS operations.
 */
@Service
public class OpenSearchManagerImpl implements OpenSearchManager {

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
						buildMappings(m, columns, defaultAnalyzer, overrideMap, analyzers);
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
					.synonym(syn -> syn.synonyms(synonymRules))));
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

		List<String> filters = new ArrayList<>();
		if (settings.getFilterOrder() != null) {
			filters.addAll(settings.getFilterOrder());
		}
		if (Boolean.TRUE.equals(settings.getSynonymAware()) && hasSynonyms) {
			filters.add(SYNONYM_FILTER_NAME);
		}

		String analyzerName = ANALYZER_PREFIX + analyzer.getId();
		final String finalTokenizer = tokenizer;
		final List<String> finalFilters = filters;

		a.analyzer(analyzerName, an -> an.custom(c -> {
			c.tokenizer(finalTokenizer);
			if (!finalFilters.isEmpty()) {
				c.filter(finalFilters);
			}
			if (settings.getCharFilterOrder() != null && !settings.getCharFilterOrder().isEmpty()) {
				c.charFilter(settings.getCharFilterOrder());
			}
			return c;
		}));
	}

	private void buildMappings(org.opensearch.client.opensearch._types.mapping.TypeMapping.Builder m,
			List<ColumnModel> columns, String defaultAnalyzer,
			Map<String, ColumnAnalyzerOverrideEntry> overrideMap, Map<String, TextAnalyzer> analyzers) {
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

			m.properties(columnId, buildProperty(columnType, effectiveAnalyzer, entry, analyzers));
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
	public long bulkIndex(String indexName, List<BulkOperation> operations) {
		if (operations.isEmpty()) {
			return 0L;
		}

		try {
			BulkResponse response = openSearchClient.bulk(req -> req.operations(operations));

			List<String> errors = new ArrayList<>();
			for (var item : response.items()) {
				if (item.error() != null) {
					errors.add("doc " + item.id() + ": " + describeError(item.error()));
				}
			}

			if (!errors.isEmpty()) {
				throw new RuntimeException(String.format(
						"Bulk index to %s failed: %d document(s) rejected out of %d. First errors: %s",
						indexName, errors.size(), operations.size(),
						errors.subList(0, Math.min(errors.size(), 5))));
			}

			return (long) response.items().size();
		} catch (OpenSearchException e) {
			throw new RuntimeException("Failed to bulk index to search index: " + indexName
					+ " (" + describeError(e.error()) + ")", e);
		} catch (IOException e) {
			throw new RuntimeException("Failed to bulk index to search index: " + indexName, e);
		}
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
	private SearchQueryResults callSearchApi(String indexName, BoolQuery.Builder boolBuilder,
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
				// Disable total-hit tracking when the caller doesn't need the count
				if (!returnTotalHits) {
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
			Map<String, TextAnalyzer> analyzers) {

		if (ColumnTypeToOpenSearchMapping.isTextType(columnType)) {
			return buildTextProperty(columnType, effectiveAnalyzer, entry, analyzers);
		}

		if (ColumnTypeToOpenSearchMapping.isLinkType(columnType)) {
			if (isKeywordAnalyzer(effectiveAnalyzer)) {
				return buildKeywordWithSearchableProperty(analyzers);
			}
			return buildTextProperty(columnType, effectiveAnalyzer, entry, analyzers);
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
			Map<String, TextAnalyzer> analyzers) {
		Integer ignoreAbove = ColumnTypeToOpenSearchMapping.getIgnoreAbove(columnType);
		int ia = ignoreAbove != null ? ignoreAbove : 1000;

		String indexAnalyzerName = resolveIndexAnalyzerName(effectiveAnalyzer, entry, analyzers);
		String searchAnalyzerName = resolveSearchAnalyzerName(indexAnalyzerName, entry, analyzers);
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

	String resolveSearchAnalyzerName(String indexAnalyzerName,
			ColumnAnalyzerOverrideEntry entry, Map<String, TextAnalyzer> analyzers) {
		if (entry != null && entry.getSearchAnalyzer() != null) {
			return analyzerToOpenSearchName(analyzers.get(entry.getSearchAnalyzer()));
		}
		return indexAnalyzerName;
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

	List<String> buildSynonymRules(List<SynonymSet> synonymSets) {
		if (synonymSets == null || synonymSets.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> rules = new ArrayList<>();
		for (SynonymSet ss : synonymSets) {
			if (ss.getRules() == null) {
				continue;
			}
			for (SynonymRule rule : ss.getRules()) {
				if (rule.getTerms() == null || rule.getTerms().size() < 2) {
					continue;
				}
				if (rule.getRuleType() == SynonymRuleType.EQUIVALENT) {
					rules.add(String.join(", ", rule.getTerms()));
				} else if (rule.getRuleType() == SynonymRuleType.EXPLICIT) {
					String source = rule.getTerms().get(0);
					List<String> targets = rule.getTerms().subList(1, rule.getTerms().size());
					rules.add(source + " => " + String.join(", ", targets));
				}
			}
		}
		return rules;
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
		if (settings.getFilterOrder() == null) {
			return tokenFilters;
		}
		for (String filterName : settings.getFilterOrder()) {
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
