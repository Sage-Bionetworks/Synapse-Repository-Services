package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opensearch.client.opensearch._types.query_dsl.Query.Kind;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.dbo.search.SynonymSetDao;
import org.sagebionetworks.repo.model.dbo.search.TextAnalyzerDao;
import org.sagebionetworks.repo.model.search.SearchAutocompleteBody;
import org.sagebionetworks.repo.model.search.SearchFieldValue;
import org.sagebionetworks.repo.model.search.SearchHit;
import org.sagebionetworks.repo.model.search.SearchQuery;
import org.sagebionetworks.repo.model.search.SearchQueryPart;
import org.sagebionetworks.repo.model.search.SearchQueryResults;
import org.sagebionetworks.repo.model.search.SearchQueryType;
import org.sagebionetworks.repo.model.search.dsl.Aggregation;
import org.sagebionetworks.repo.model.search.dsl.BoolQuery;
import org.sagebionetworks.repo.model.search.dsl.BoostingQuery;
import org.sagebionetworks.repo.model.search.dsl.ConstantScoreQuery;
import org.sagebionetworks.repo.model.search.dsl.DisMaxQuery;
import org.sagebionetworks.repo.model.search.dsl.ExistsQuery;
import org.sagebionetworks.repo.model.search.dsl.FieldCollapse;
import org.sagebionetworks.repo.model.search.dsl.FiltersAggregation;
import org.sagebionetworks.repo.model.search.dsl.FuzzyFieldOptions;
import org.sagebionetworks.repo.model.search.dsl.Highlight;
import org.sagebionetworks.repo.model.search.dsl.HighlightField;
import org.sagebionetworks.repo.model.search.dsl.MatchAllQuery;
import org.sagebionetworks.repo.model.search.dsl.MatchBoolPrefixFieldOptions;
import org.sagebionetworks.repo.model.search.dsl.MatchFieldOptions;
import org.sagebionetworks.repo.model.search.dsl.MatchPhraseFieldOptions;
import org.sagebionetworks.repo.model.search.dsl.MultiMatchQuery;
import org.sagebionetworks.repo.model.search.dsl.PrefixFieldOptions;
import org.sagebionetworks.repo.model.search.dsl.Query;
import org.sagebionetworks.repo.model.search.dsl.RangeFieldOptions;
import org.sagebionetworks.repo.model.search.dsl.Rescore;
import org.sagebionetworks.repo.model.search.dsl.RescoreQuery;
import org.sagebionetworks.repo.model.search.dsl.SimpleQueryStringQuery;
import org.sagebionetworks.repo.model.search.dsl.TermFieldOptions;
import org.sagebionetworks.repo.model.search.dsl.TermsAggregation;
import org.sagebionetworks.repo.model.search.dsl.WildcardFieldOptions;
import org.sagebionetworks.repo.model.search.table.SynonymSet;
import org.sagebionetworks.repo.model.search.table.TextAnalyzer;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * AutoWire integration test for {@link OpenSearchManagerImpl} that hits real AWS OpenSearch.
 * This class is treated as a DAO-level test — it verifies actual OpenSearch behavior
 * rather than mocked assumptions. Document content is verified deeply here so that
 * higher-level tests can trust the DAO and do spot checks only.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class OpenSearchManagerImplAutoWiredTest {

	private static final long POLL_MAX_MS = 30_000L;
	private static final long POLL_INTERVAL_MS = 1_000L;

	@Autowired
	private OpenSearchManager openSearchManager;

	@Autowired
	private TextAnalyzerDao textAnalyzerDao;

	@Autowired
	private SynonymSetDao synonymSetDao;

	@Autowired
	private TextAnalyzerBootstrap textAnalyzerBootstrap;

	private String indexName;
	/** SynonymSet ids created during a test, removed in @AfterEach so each run is hermetic. */
	private final List<String> createdSynonymSetIds = new ArrayList<>();
	/**
	 * Per-test resolved-analyzer map. Each value is the post-{@code SearchOpaqueJsonUtil.resolveAnalyzerSettings}
	 * settings tree the manager hands to AOSS at index-build time. The bootstrapped analyzers
	 * contain no {@code $ref}s here, so parsing the stored {@code settings} blob is sufficient —
	 * synonym tests build their own analyzer entries that splice {@code $ref} entries against
	 * SynonymSets they create in {@link SynonymSetDao}, mirroring the production resolver path.
	 */
	private Map<String, IndexSettingsAnalysis> defaultAnalyzers;

	@BeforeEach
	public void setUp() {
		assertNotNull(openSearchManager);
		textAnalyzerBootstrap.bootstrapSystemAnalyzers();
		indexName = "test-index-" + UUID.randomUUID().toString().substring(0, 8);
		defaultAnalyzers = buildDefaultAnalyzers();
	}

	@AfterEach
	public void tearDown() {
		if (indexName != null) {
			try {
				openSearchManager.deleteIndex(indexName);
			} catch (Exception e) {
				// Best effort cleanup
			}
		}
		for (String id : createdSynonymSetIds) {
			try {
				synonymSetDao.delete(id);
			} catch (Exception e) {
				// Best effort cleanup
			}
		}
		createdSynonymSetIds.clear();
	}

	@Test
	public void testCreateIndexAndDeleteLifecycle() {
		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("name").setColumnType(ColumnType.STRING),
				new ColumnModel().setId("2").setName("age").setColumnType(ColumnType.INTEGER)
		);

		// call under test — happy-path create returns the applied settings JSON
		Optional<String> appliedConfig = openSearchManager.createIndex(indexName, columns, null,
				Collections.emptyList(), defaultAnalyzers, 0, 1, 0);
		assertTrue(appliedConfig.isPresent());
		assertTrue(appliedConfig.get().length() > 0);
		openSearchManager.waitForIndexWritable(indexName);

		// call under test — creating an index that already exists returns Optional.empty()
		Optional<String> duplicate = openSearchManager.createIndex(indexName, columns, null,
				Collections.emptyList(), defaultAnalyzers, 0, 1, 0);
		assertTrue(duplicate.isEmpty(),
				"resource_already_exists must surface as Optional.empty(), not throw");

		// call under test — bulkIndex with no operations is a no-op
		assertEquals(0L, openSearchManager.bulkIndex(indexName, Collections.emptyList()));

		// call under test — deleteIndex on the live index, then again as a no-op
		openSearchManager.deleteIndex(indexName);
		openSearchManager.deleteIndex(indexName);

		// call under test — deleteIndex on a name that never existed must not throw
		openSearchManager.deleteIndex("nonexistent-index-" + UUID.randomUUID());
	}

	@Test
	public void testCRUDWithSearchQueryAndDocumentVerification() {
		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("title").setColumnType(ColumnType.STRING),
				new ColumnModel().setId("2").setName("count").setColumnType(ColumnType.INTEGER)
		);
		openSearchManager.createIndex(indexName, columns, null,
				Collections.emptyList(), defaultAnalyzers, 0, 1, 0);
		openSearchManager.waitForIndexWritable(indexName);

		List<BulkOperation> operations = List.of(
				buildBulkOp(indexName, "1", Map.of("_row_id", 1L, "_row_version", 1L, "1", "mitochondria research", "2", "42")),
				buildBulkOp(indexName, "2", Map.of("_row_id", 2L, "_row_version", 1L, "1", "genome sequencing study", "2", "99")),
				buildBulkOp(indexName, "3", Map.of("_row_id", 3L, "_row_version", 1L, "1", "mitochondria function", "2", "7"))
		);

		// call under test — bulk index should succeed on first attempt without retries
		long indexed = openSearchManager.bulkIndex(indexName, operations);

		assertEquals(3L, indexed);

		// call under test — poll for search results (AOSS eventual consistency)
		SearchQueryResults results = waitForSearch(simpleQueryStringBody("mitochondria"), columns, 2);

		assertNotNull(results);
		assertEquals(2L, results.getTotalHits());
		assertNotNull(results.getHits());
		assertEquals(2, results.getHits().size());

		// Verify actual document content — this is the deep check so higher-level
		// tests can trust the DAO and just do count/spot checks.
		// Note: convertResponse translates column IDs back to names using idToName map.
		results.getHits().forEach(hit -> {
			assertNotNull(hit.getFields(), "Hit should have fields");
			assertTrue(hit.getFields().stream().anyMatch(f -> "title".equals(f.getName())),
					"Hit should have field 'title' (translated from column ID '1')");
			String titleValue = hit.getFields().stream()
					.filter(f -> "title".equals(f.getName()))
					.findFirst().get().getValue();
			assertTrue(titleValue.contains("mitochondria"),
					"Title field should contain 'mitochondria', got: " + titleValue);
		});
	}

	@Test
	public void testFilterAggregationRespectsTopLevelQueryScope() {
		// ACL-scope invariant: a `filter` (and `filters`) aggregation runs *inside* the search
		// context, so it must only ever count documents the top-level query already admits. When
		// row-level ACL filtering lands it will be injected into the top-level bool.must; this test
		// simulates that restricting clause with a top-level `term category=public` and proves the
		// filter aggregation's buckets never count the excluded `private` rows — even though the
		// filter body would match them on their own. This is the guardrail that keeps the feature
		// from surfacing rows the caller is not authorized to see.
		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("category").setColumnType(ColumnType.STRING),
				new ColumnModel().setId("2").setName("tag").setColumnType(ColumnType.STRING)
		);
		openSearchManager.createIndex(indexName, columns, null,
				Collections.emptyList(), defaultAnalyzers, 0, 1, 0);
		openSearchManager.waitForIndexWritable(indexName);

		// Tag "shared" appears on BOTH a public and a private row. The public row alone is in scope.
		List<BulkOperation> operations = List.of(
				buildBulkOp(indexName, "1", Map.of("_row_id", 1L, "_row_version", 1L, "1", "public", "2", "shared")),
				buildBulkOp(indexName, "2", Map.of("_row_id", 2L, "_row_version", 1L, "1", "public", "2", "alpha")),
				buildBulkOp(indexName, "3", Map.of("_row_id", 3L, "_row_version", 1L, "1", "private", "2", "shared")),
				buildBulkOp(indexName, "4", Map.of("_row_id", 4L, "_row_version", 1L, "1", "private", "2", "beta"))
		);
		assertEquals(4L, openSearchManager.bulkIndex(indexName, operations));

		// Top-level query restricts to category=public (the future ACL clause). Two aggregations:
		//  - "tag_shared_filter": filter tag=shared, child terms on category
		//  - "by_category_filters": named filters, one bucket per category value
		// term/terms/range clauses auto-route text columns to .keyword, so reference tag/category by
		// name and let the field rewriter resolve the keyword sub-field.
		Aggregation filterAgg = new Aggregation()
				.setFilter(new Query().setTerm(Map.of("tag", new TermFieldOptions().setValue("shared"))))
				.setAggregations(Map.of("by_category",
						new Aggregation().setTerms(new TermsAggregation().setField("category"))));
		Aggregation filtersAgg = new Aggregation().setFilters(new FiltersAggregation().setFilters(Map.of(
				"public_bucket", new Query().setTerm(Map.of("category", new TermFieldOptions().setValue("public"))),
				"private_bucket", new Query().setTerm(Map.of("category", new TermFieldOptions().setValue("private"))))));

		Map<String, Aggregation> aggregations = new LinkedHashMap<>();
		aggregations.put("tag_shared_filter", filterAgg);
		aggregations.put("by_category_filters", filtersAgg);

		SearchQuery body = new SearchQuery()
				.setQuery(new Query().setTerm(Map.of("category", new TermFieldOptions().setValue("public"))))
				.setSize(10L)
				.setFrom(0L)
				.setAggregations(aggregations);

		// Only the two public rows are in scope.
		SearchQueryResults results = waitForSearch(body, columns, 2L);
		assertEquals(2L, results.getTotalHits());

		assertNotNull(results.getAggregationResults(), "aggregations were requested");
		JsonNode aggResults = SearchOpaqueJsonUtil.parse(results.getAggregationResults());

		// The filter agg matched tag=shared. There are two shared rows globally, but only the public
		// one is in the top-level query scope — so doc_count MUST be 1, never 2.
		JsonNode filterNode = aggResults.path("tag_shared_filter");
		assertEquals(1, filterNode.path("doc_count").asInt(),
				"filter agg must count only the in-scope (public) shared row, not the excluded private one: "
						+ results.getAggregationResults());
		// Its child terms-on-category bucket must contain only `public`. OpenSearch returns nested
		// named sub-aggregations with a typed-key prefix (e.g. "sterms#by_category"), so locate the
		// child by its caller-chosen suffix rather than an exact key.
		JsonNode categoryBuckets = childAggBuckets(filterNode, "by_category");
		assertTrue(categoryBuckets.isArray());
		Set<String> categoriesSeen = new java.util.HashSet<>();
		for (JsonNode bucket : categoryBuckets) {
			categoriesSeen.add(bucket.path("key").asText());
		}
		assertEquals(Set.of("public"), categoriesSeen,
				"filter agg sub-bucket must never surface the excluded `private` category");

		// The filters agg: the public_bucket sees the 2 public rows; the private_bucket — whose query
		// matches category=private — must be EMPTY, because those rows are outside the top-level scope.
		JsonNode filtersBuckets = aggResults.path("by_category_filters").path("buckets");
		assertEquals(2, filtersBuckets.path("public_bucket").path("doc_count").asInt(),
				"filters public_bucket must count both in-scope public rows");
		assertEquals(0, filtersBuckets.path("private_bucket").path("doc_count").asInt(),
				"filters private_bucket must be empty — its rows are excluded by the top-level query: "
						+ results.getAggregationResults());
	}

	@Test
	public void testRoundTripWithCuratorDefinedCustomAnalyzer() {
		// Register a curator-style custom TextAnalyzer (inline english_stop + lowercase chain)
		// as the index's defaultAnalyzer. Index docs, run a query that exercises the chain
		// (stop-word removal: searching for "the genome" must match "genome research"
		// because "the" is dropped before query matching), and verify the analyzer landed.
		String customQname = "biomed-publications";
		String customSettings = "{"
				+ "\"filter\":{\"english_stop\":{\"type\":\"stop\",\"stopwords\":\"_english_\"}},"
				+ "\"analyzer\":{\"default\":{\"type\":\"custom\","
					+ "\"tokenizer\":\"standard\","
					+ "\"filter\":[\"lowercase\",\"english_stop\"]}}}";
		Map<String, IndexSettingsAnalysis> analyzers = new HashMap<>(defaultAnalyzers);
		analyzers.put(customQname,
				SearchOpaqueJsonUtil.resolveAnalyzerSettings(SearchOpaqueJsonUtil.parse(customSettings), q -> null));

		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("title").setColumnType(ColumnType.STRING));
		Optional<String> appliedConfig = openSearchManager.createIndex(indexName, columns, customQname,
				Collections.emptyList(), analyzers, 0, 1, 0);
		assertTrue(appliedConfig.isPresent());
		// The applied config must register the namespaced filter from the custom analyzer.
		String aossKey = OpenSearchManagerImpl.toAossKey(customQname);
		assertTrue(appliedConfig.get().contains(aossKey + "__english_stop"),
				"Custom analyzer's owned filter must be registered under namespaced key");
		openSearchManager.waitForIndexWritable(indexName);

		List<BulkOperation> operations = List.of(
				buildBulkOp(indexName, "1", Map.of("_row_id", 1L, "_row_version", 1L, "1", "the genome research")),
				buildBulkOp(indexName, "2", Map.of("_row_id", 2L, "_row_version", 1L, "1", "lunar lander mission")));
		openSearchManager.bulkIndex(indexName, operations);

		// "the" is a stop word for the english_stop filter — if the custom analyzer wasn't
		// applied at search time, "the genome" would also match docs that lack "genome"
		// (anything containing "the"). Asserting exactly one hit confirms stop-word removal
		// is in effect.
		// call under test
		SearchQueryResults results = waitForSearch(simpleQueryStringBody("the genome"), columns, 1);

		assertEquals(1L, results.getTotalHits(),
				"Custom analyzer's english_stop filter must drop 'the' so only 'the genome research' matches");
		assertEquals(1, results.getHits().size());
	}

	@Test
	public void testRoundTripWithColumnAnalyzerOverride() {
		// Two STRING columns: 'title' (index-default, SCIENTIFIC stemming) and 'tag' (override
		// to KEYWORD — exact match only). Verify routing: a stemmed query matches 'title' but
		// not 'tag', and an exact-token query matches 'tag' verbatim.
		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("title").setColumnType(ColumnType.STRING),
				new ColumnModel().setId("2").setName("tag").setColumnType(ColumnType.STRING));

		org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverrideEntry override =
				new org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverrideEntry()
						.setColumnName("tag")
						.setAnalyzer(Map.of("$ref", "org.sagebionetworks-KEYWORD"));
		org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride overrideContainer =
				new org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride()
						.setOverrides(List.of(override));

		Optional<String> appliedConfig = openSearchManager.createIndex(indexName, columns,
				"org.sagebionetworks-SCIENTIFIC", List.of(overrideContainer), defaultAnalyzers, 0, 1, 0);
		assertTrue(appliedConfig.isPresent());
		openSearchManager.waitForIndexWritable(indexName);

		List<BulkOperation> operations = List.of(
				// Use case-mismatched values so KEYWORD's no-lowercasing semantics are
				// distinguishable from SCIENTIFIC's case-insensitive stemmed matching.
				buildBulkOp(indexName, "1", Map.of("_row_id", 1L, "_row_version", 1L,
						"1", "research papers", "2", "BioMed-Cancer")),
				buildBulkOp(indexName, "2", Map.of("_row_id", 2L, "_row_version", 1L,
						"1", "biomed papers", "2", "BioMed-Genome")));
		openSearchManager.bulkIndex(indexName, operations);

		// Exact-keyword query against `tag` matches doc 1 only — KEYWORD doesn't lowercase
		// so the indexed token is the original "BioMed-Cancer", and "biomed-cancer" must NOT
		// match. Run two queries scoped to the same column to confirm both directions.
		// call under test
		SearchQueryResults exactResults = waitForSearch(
				simpleQueryStringBody("BioMed-Cancer", List.of("tag")), columns, 1);
		assertEquals(1L, exactResults.getTotalHits(),
				"KEYWORD override on `tag` must match the exact case-preserving token");

		// Stemmed query against `title` matches doc 2 ("biomed papers" → "biomed paper" stem).
		// call under test
		SearchQueryResults stemmedResults = waitForSearch(
				simpleQueryStringBody("paper", List.of("title")), columns, 1);
		assertTrue(stemmedResults.getTotalHits() >= 1L,
				"SCIENTIFIC default on `title` must stem 'papers' so 'paper' matches");
	}

	@Test
	public void testRoundTripWithAutocompleteBootstrappedAnalyzer() {
		// AUTOCOMPLETE is the bootstrapped analyzer for prefix-style typeahead. A column bound
		// to AUTOCOMPLETE (via override) must let an autocomplete() prefix query match docs
		// even after only a few characters of the indexed term. This is the only round-trip
		// that exercises the asymmetric default / default_search behavior end-to-end.
		//
		// Regression guard for the bulk-index path (PLFM-9636): the bootstrapped chain combines
		// word_delimiter (legacy, non-graph) with edge_ngram. An earlier chain used
		// word_delimiter_graph, which produces multi-position graph tokens that edge_ngram
		// (a non-graph filter) cannot consume — AOSS rejected every document with a generic
		// "Internal error". Asserting bulkIndex returns the full doc count protects against
		// that regression.
		Map<String, IndexSettingsAnalysis> analyzers = new HashMap<>(defaultAnalyzers);
		analyzers.put("org.sagebionetworks-AUTOCOMPLETE",
				bootstrappedAnalyzerSettings(TextAnalyzerBootstrapper.AUTOCOMPLETE_ID));

		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("term").setColumnType(ColumnType.STRING));

		org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverrideEntry override =
				new org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverrideEntry()
						.setColumnName("term")
						.setAnalyzer(Map.of("$ref", "org.sagebionetworks-AUTOCOMPLETE"));
		org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride overrideContainer =
				new org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride()
						.setOverrides(List.of(override));

		openSearchManager.createIndex(indexName, columns, null,
				List.of(overrideContainer), analyzers, 0, 1, 0);
		openSearchManager.waitForIndexWritable(indexName);

		List<BulkOperation> operations = List.of(
				buildBulkOp(indexName, "1", Map.of("_row_id", 1L, "_row_version", 1L, "1", "mitochondria")),
				buildBulkOp(indexName, "2", Map.of("_row_id", 2L, "_row_version", 1L, "1", "genome")),
				buildBulkOp(indexName, "3", Map.of("_row_id", 3L, "_row_version", 1L, "1", "microbiome")));

		// call under test — every document must be accepted; word_delimiter_graph + edge_ngram
		// previously caused AOSS to reject all 3 docs.
		assertEquals(3L, openSearchManager.bulkIndex(indexName, operations));

		// Autocomplete with prefix "mit" should match "mitochondria"; "microbiome" begins
		// with "mic", not "mit", so it must NOT match. The manager clamps page size for
		// autocomplete. The autocomplete top-level allowlist accepts
		// {prefix, match_phrase_prefix, match_bool_prefix} only — match_bool_prefix is the
		// direct equivalent of the legacy multi_match{type:bool_prefix} shape.
		SearchAutocompleteBody body = new SearchAutocompleteBody()
				.setQuery(new Query().setMatch_bool_prefix(
						Map.of("term", new MatchBoolPrefixFieldOptions().setQuery("mit"))));

		// call under test
		SearchQueryResults results = waitForAutocomplete(body, columns, 1);

		assertNotNull(results);
		assertTrue(results.getTotalHits() >= 1L,
				"Autocomplete must surface 'mitochondria' for prefix 'mit'");
		assertTrue(results.getHits().stream()
						.flatMap(h -> h.getFields().stream())
						.anyMatch(f -> "term".equals(f.getName()) && "mitochondria".equals(f.getValue())),
				"Autocomplete result must include the 'mitochondria' document");
	}

	@Test
	public void testSearchWithNonExistentIndex() {
		SearchQuery body = simpleQueryStringBody("anything");

		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("name").setColumnType(ColumnType.STRING));

		// call under test
		IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
				openSearchManager.search("nonexistent-" + UUID.randomUUID(), body, columns,
						EnumSet.allOf(SearchQueryPart.class), Collections.emptyList()));

		assertTrue(ex.getMessage().contains("still building"),
				"Exception message should indicate the index is not ready, got: " + ex.getMessage());
	}

	/**
	 * Validator surface, exercised against live AOSS without creating an index. Three static
	 * cases cover the failure paths (invalid tokenizer, invalid filter) and the inline-filter
	 * registry success path (typed {@code TokenFilterDefinition} deserialize). The
	 * bootstrapped-analyzer regression guard is a separate test below because a static
	 * {@code @MethodSource} factory cannot reach the instance-bootstrapped {@code defaultAnalyzers}.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("analyzerValidationCases")
	public void testValidateAnalyzerSettings(String label, IndexSettingsAnalysis settings, boolean shouldFail) {
		if (shouldFail) {
			// call under test
			IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
					() -> openSearchManager.validateAnalyzerSettings(settings));
			assertTrue(ex.getMessage().contains("Invalid analyzer configuration"),
					"Expected 'Invalid analyzer configuration' in message, got: " + ex.getMessage());
		} else {
			// call under test — must not throw
			openSearchManager.validateAnalyzerSettings(settings);
		}
	}

	private static Stream<Arguments> analyzerValidationCases() {
		return Stream.of(
				Arguments.of("invalid tokenizer",
						toAnalysis("{\"analyzer\":{\"default\":{\"type\":\"custom\","
								+ "\"tokenizer\":\"nonexistent_tokenizer_xyz\"}}}"),
						true),
				Arguments.of("invalid filter",
						toAnalysis("{\"analyzer\":{\"default\":{\"type\":\"custom\","
								+ "\"tokenizer\":\"standard\","
								+ "\"filter\":[\"bogus_filter_name_xyz\"]}}}"),
						true),
				Arguments.of("inline filter registry",
						toAnalysis("{"
								+ "\"filter\":{\"my_stop\":{\"type\":\"stop\",\"stopwords\":\"_english_\"}},"
								+ "\"analyzer\":{\"default\":{\"type\":\"custom\","
								+ "\"tokenizer\":\"standard\","
								+ "\"filter\":[\"my_stop\",\"lowercase\"]}}}"),
						false));
	}

	@Test
	public void testValidateAnalyzerSettingsWithBootstrappedAnalyzer() {
		// Round-trip every bootstrapped analyzer's settings through the validate probe so a
		// regression on any one of them surfaces here. Each analyzer's stored settings is
		// already a complete OpenSearch settings.analysis tree with no $refs.
		for (Map.Entry<String, IndexSettingsAnalysis> entry : defaultAnalyzers.entrySet()) {
			// call under test
			openSearchManager.validateAnalyzerSettings(entry.getValue());
		}
	}

	/** Test helper mirroring SearchOpaqueJsonUtil.resolveAnalyzerSettings() with a no-op resolver. */
	private static IndexSettingsAnalysis toAnalysis(String json) {
		return SearchOpaqueJsonUtil.resolveAnalyzerSettings(SearchOpaqueJsonUtil.parse(json), q -> null);
	}

	/**
	 * Regression test for PLFM-9636: AOSS rejected createIndex with
	 * "illegal_argument_exception: Token filter [std_word_delimiter] cannot be used to parse synonyms"
	 * whenever a bootstrapped synonym-aware analyzer was paired with a non-empty SynonymSet.
	 * Confirms (1) createIndex succeeds against live AOSS with synonyms configured, and
	 * (2) a query for one synonym term matches documents containing the other. Run against
	 * every bootstrapped synonym-aware analyzer so a regression on any one of them surfaces.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("synonymAwareBootstrappedAnalyzers")
	public void testCreateIndexWithBootstrappedSynonymAwareAnalyzerAndSynonyms(String analyzerKey, long bootstrapId) {
		String synonymQname = createSynonymSet(
				"{\"type\":\"synonym_graph\",\"synonyms\":[\"cancer, tumor, neoplasm\"]}");
		Map<String, IndexSettingsAnalysis> analyzers = new HashMap<>(defaultAnalyzers);
		analyzers.put(analyzerKey, synonymAwareAnalyzer(bootstrapId, synonymQname));

		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("diagnosis").setColumnType(ColumnType.STRING));
		List<org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride> overrides =
				List.of(bindColumnToAnalyzer("diagnosis", analyzerKey));

		// call under test — createIndex must succeed. Pre-fix this threw
		// "Token filter [std_word_delimiter] cannot be used to parse synonyms".
		openSearchManager.createIndex(indexName, columns, analyzerKey,
				overrides, analyzers, 0, 1, 0);
		openSearchManager.waitForIndexWritable(indexName);

		// Index one doc per synonym term so each query can match via synonym expansion at
		// search time regardless of which direction OpenSearch applies the rule internally.
		List<BulkOperation> operations = List.of(
				buildBulkOp(indexName, "1", Map.of("_row_id", 1L, "_row_version", 1L, "1", "cancer")),
				buildBulkOp(indexName, "2", Map.of("_row_id", 2L, "_row_version", 1L, "1", "tumor")),
				buildBulkOp(indexName, "3", Map.of("_row_id", 3L, "_row_version", 1L, "1", "neoplasm")));

		assertEquals(3L, openSearchManager.bulkIndex(indexName, operations));

		// Querying for any one term must match all three docs via the EQUIVALENT synonym rule.
		SearchQueryResults results = waitForSearch(simpleQueryStringBody("cancer"), columns, 3);
		assertEquals(3L, results.getTotalHits(),
				"Query for 'cancer' must return all three docs via EQUIVALENT synonym expansion");
	}

	/**
	 * @return one row per bootstrapped synonym-aware analyzer:
	 *         {@code (analyzerKey, bootstrapId)}. Analyzer key matches the
	 *         {@code org.sagebionetworks-<NAME>} convention used elsewhere in this file.
	 */
	private static Stream<Arguments> synonymAwareBootstrappedAnalyzers() {
		return Stream.of(
				Arguments.of("org.sagebionetworks-SCIENTIFIC", TextAnalyzerBootstrapper.SCIENTIFIC_ID),
				Arguments.of("org.sagebionetworks-STANDARD", TextAnalyzerBootstrapper.STANDARD_ID),
				Arguments.of("org.sagebionetworks-IDENTIFIER", TextAnalyzerBootstrapper.IDENTIFIER_ID));
	}

	/**
	 * Regression for the Lucene offset-monotonicity bulk-index failure: hyphenated /
	 * CamelCase / digit-letter tokens adjacent to a synonym source term caused
	 * {@code illegal_argument_exception: startOffset must be non-negative ... offsets must not go backwards}
	 * when synonym expansion and {@code word_delimiter} both ran at index time. Synonyms now expand only
	 * at search time, so every document must be accepted and a search for a synonym source must still match.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("synonymAwareBootstrappedAnalyzers")
	public void testBulkIndexWithSynonymsAndWordDelimiterSplittableNeighbors(String analyzerKey, long bootstrapId) {
		String synonymQname = createSynonymSet(
				"{\"type\":\"synonym_graph\",\"synonyms\":[\"mRNA, messenger-RNA, messengerRNA\"]}");
		Map<String, IndexSettingsAnalysis> analyzers = new HashMap<>(defaultAnalyzers);
		analyzers.put(analyzerKey, synonymAwareAnalyzer(bootstrapId, synonymQname));

		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("description").setColumnType(ColumnType.STRING));
		List<org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride> overrides =
				List.of(bindColumnToAnalyzer("description", analyzerKey));

		openSearchManager.createIndex(indexName, columns, analyzerKey,
				overrides, analyzers, 0, 1, 0);
		openSearchManager.waitForIndexWritable(indexName);

		List<BulkOperation> operations = List.of(
				buildBulkOp(indexName, "1", Map.of("_row_id", 1L, "_row_version", 1L, "1", "cancer-related mRNA seq analysis")),
				buildBulkOp(indexName, "2", Map.of("_row_id", 2L, "_row_version", 1L, "1", "messengerRNA profiling in TP53-deficient cells")),
				buildBulkOp(indexName, "3", Map.of("_row_id", 3L, "_row_version", 1L, "1", "messenger-RNA 2024 study")));

		// call under test
		assertEquals(3L, openSearchManager.bulkIndex(indexName, operations));

		// Query the multi-token synonym variant: it produces a richer graph at search
		// time (hyphen split → `messenger AND rna`) that reaches all three docs across
		// every bootstrapped synonym-aware analyzer regardless of tokenizer choice.
		// `mRNA` alone is insufficient on the IDENTIFIER chain (whitespace tokenizer +
		// id_word_delimiter), where the search-side synonym graph for a single-token
		// LHS does not consistently reach a doc whose `mRNA` neighbor is itself the
		// only synonym source — see PLFM-9636 review for diagnostic detail.
		SearchQueryResults results = waitForSearch(simpleQueryStringBody("messenger-RNA"), columns, 3);
		assertEquals(3L, results.getTotalHits(),
				"Query for 'messenger-RNA' must match all three docs via EQUIVALENT synonym expansion at search time");
	}

	/**
	 * Round-trip regression for PLFM-9636: the search-variant chain runs
	 * {@code lowercase → synapse_synonyms → word_delimiter_graph}, so multi-word synonym
	 * left-hand sides expand correctly and queries match regardless of casing. Each doc
	 * here is indexed only with the abbreviation form; the long-form / mixed-case query
	 * must match via synonym expansion at search time. Pre-fix (plain {@code synonym}
	 * filter, no leading {@code lowercase}), all three assertions returned 0 hits.
	 */
	@Test
	public void testSearchWithMultiWordAndMixedCaseSynonymQueries() {
		String synonymQname = createSynonymSet(
				"{\"type\":\"synonym_graph\",\"synonyms\":["
						+ "\"deep learning, DL\","
						+ "\"electronic health record, EHR\"]}");
		Map<String, IndexSettingsAnalysis> analyzers = new HashMap<>(defaultAnalyzers);
		analyzers.put("org.sagebionetworks-STANDARD",
				synonymAwareAnalyzer(TextAnalyzerBootstrapper.STANDARD_ID, synonymQname));

		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("description").setColumnType(ColumnType.STRING));
		List<org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride> overrides =
				List.of(bindColumnToAnalyzer("description", "org.sagebionetworks-STANDARD"));

		openSearchManager.createIndex(indexName, columns, "org.sagebionetworks-STANDARD",
				overrides, analyzers, 0, 1, 0);
		openSearchManager.waitForIndexWritable(indexName);

		// Each doc contains only the abbreviation — a query for the long form (or a
		// mixed-case variant) must reach it via synonym expansion at search time.
		List<BulkOperation> operations = List.of(
				buildBulkOp(indexName, "1", Map.of("_row_id", 1L, "_row_version", 1L, "1", "neural network DL paper")),
				buildBulkOp(indexName, "2", Map.of("_row_id", 2L, "_row_version", 1L, "1", "EHR data extraction")),
				buildBulkOp(indexName, "3", Map.of("_row_id", 3L, "_row_version", 1L, "1", "unrelated content")));

		assertEquals(3L, openSearchManager.bulkIndex(indexName, operations));

		// Verify both query types the production stack uses:
		//   - SIMPLE_QUERY_STRING: requires multi-word phrases to be quoted so the
		//     analyzer sees them as adjacent tokens (the parser otherwise splits on
		//     whitespace before analysis). This is the type used by the /search endpoint.
		//   - MULTI_MATCH: what the portal UI sends. Whitespace-separated tokens are
		//     analyzed together, so the synonym_graph filter sees the full phrase
		//     without requiring user-supplied quotes.

		// (a) Multi-word LHS expands to the abbreviation. Plain `synonym` (non-graph)
		//     fails this; `synonym_graph` is required.
		SearchQueryResults dlSimple = runQuery(SearchQueryType.SIMPLE_QUERY_STRING, "\"deep learning\"", columns);
		assertEquals(1L, dlSimple.getTotalHits(),
				"quoted multi-word \"deep learning\" (SIMPLE_QUERY_STRING) must match doc indexed with 'DL' via synonym_graph");
		assertEquals(1L, runQuery(SearchQueryType.MULTI_MATCH, "deep learning", columns).getTotalHits(),
				"unquoted multi-word 'deep learning' (MULTI_MATCH, UI default) must match doc indexed with 'DL' via synonym_graph");

		// (b) Mixed-case multi-word query. Requires `lowercase` to run before the
		//     synonym filter so query tokens and rule LHS both reach the filter
		//     in the same case.
		assertEquals(1L, runQuery(SearchQueryType.SIMPLE_QUERY_STRING, "\"Deep Learning\"", columns).getTotalHits(),
				"mixed-case \"Deep Learning\" (SIMPLE_QUERY_STRING) must match doc indexed with 'DL' via lowercase-before-synonym chain");
		assertEquals(1L, runQuery(SearchQueryType.MULTI_MATCH, "Deep Learning", columns).getTotalHits(),
				"mixed-case 'Deep Learning' (MULTI_MATCH) must match doc indexed with 'DL' via lowercase-before-synonym chain");

		// (c) Mixed-case query against a different multi-word rule, exercising the
		//     same case-normalization path with a separate vocabulary.
		SearchQueryResults ehrSimple = runQuery(SearchQueryType.SIMPLE_QUERY_STRING, "\"Electronic Health Record\"", columns);
		assertEquals(1L, ehrSimple.getTotalHits(),
				"mixed-case \"Electronic Health Record\" (SIMPLE_QUERY_STRING) must match doc indexed with 'EHR'");
		assertEquals(1L, runQuery(SearchQueryType.MULTI_MATCH, "Electronic Health Record", columns).getTotalHits(),
				"mixed-case 'Electronic Health Record' (MULTI_MATCH) must match doc indexed with 'EHR'");

		assertEquals("neural network DL paper", descriptionOf(dlSimple),
				"hit value must preserve original casing of indexed text — lowercase filter applies to the inverted index only");
		assertEquals("EHR data extraction", descriptionOf(ehrSimple),
				"hit value must preserve original casing of indexed text — lowercase filter applies to the inverted index only");
	}

	/**
	 * Find the {@code buckets} node of a named child sub-aggregation under {@code parent}. OpenSearch
	 * prefixes nested named aggregations with a typed key (e.g. {@code "sterms#by_category"}), so
	 * match either the exact name or any {@code <type>#<name>} variant.
	 */
	private static JsonNode childAggBuckets(JsonNode parent, String name) {
		java.util.Iterator<Map.Entry<String, JsonNode>> fields = parent.fields();
		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> entry = fields.next();
			String key = entry.getKey();
			if (key.equals(name) || key.endsWith("#" + name)) {
				return entry.getValue().path("buckets");
			}
		}
		return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
	}

	private static String descriptionOf(SearchQueryResults results) {
		return results.getHits().get(0).getFields().stream()
				.filter(f -> "description".equals(f.getName()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no 'description' field on hit"))
				.getValue();
	}

	/**
	 * Build a {@code (queryType, text)} pair into the equivalent opaque clause and run a
	 * search. The synonym-coverage test exercises both {@code simple_query_string} and
	 * {@code multi_match}.
	 */
	private SearchQueryResults runQuery(SearchQueryType queryType, String text, List<ColumnModel> columns) {
		Query query;
		switch (queryType) {
			case SIMPLE_QUERY_STRING:
				query = new Query().setSimple_query_string(new SimpleQueryStringQuery().setQuery(text));
				break;
			case MULTI_MATCH:
				query = new Query().setMulti_match(new MultiMatchQuery().setQuery(text));
				break;
			default:
				throw new AssertionError("runQuery is only used for SIMPLE_QUERY_STRING / MULTI_MATCH");
		}
		SearchQuery body = new SearchQuery()
				.setQuery(query)
				.setSize(10L)
				.setFrom(0L);
		return waitForSearch(body, columns, 1L);
	}

	/**
	 * Round-trips one row through every Synapse {@link ColumnType} simultaneously: each fixture
	 * pairs the raw String value (the form delivered by {@code tableQueryManager.runQueryAsStream})
	 * with the typed Java value the production converter should produce. The test exercises both
	 * the converter and the AOSS contract — bulk index must accept every column type, and the
	 * search response must return the values back. A coverage guard fails the test if a new
	 * ColumnType is added to the enum without a fixture row.
	 */
	@Test
	public void testCRUDWithEveryColumnType() {
		Map<ColumnType, RoundTripCase> casesByType = buildEveryColumnTypeCase();

		assertEquals(EnumSet.allOf(ColumnType.class), casesByType.keySet(),
				"Every Synapse ColumnType must be represented in this round-trip test");

		List<ColumnModel> columns = new ArrayList<>();
		int nextId = 1;
		for (ColumnType type : casesByType.keySet()) {
			String columnId = Integer.toString(nextId++);
			columns.add(new ColumnModel().setId(columnId)
					.setName("c_" + type.name().toLowerCase())
					.setColumnType(type));
		}

		openSearchManager.createIndex(indexName, columns, null,
				Collections.emptyList(), defaultAnalyzers, 0, 1, 0);
		openSearchManager.waitForIndexWritable(indexName);

		Map<String, Object> doc = new HashMap<>();
		doc.put("_row_id", 1L);
		doc.put("_row_version", 1L);
		for (ColumnModel column : columns) {
			ColumnType type = column.getColumnType();
			RoundTripCase rtc = casesByType.get(type);
			Object converted = SearchIndexLifecycleManagerImpl.convertForDocument(rtc.raw, type);
			assertEquals(rtc.expected, converted,
					"convertForDocument produced unexpected value for " + type);
			doc.put(column.getId(), converted);
		}

		// call under test
		long indexed = openSearchManager.bulkIndex(indexName, List.of(
				BulkOperation.of(op -> op.index(idx -> idx.index(indexName).id("1").document(doc)))));

		assertEquals(1L, indexed);

		// Request a terms aggregation on every terms-aggregable column in the same call so
		// the numeric / text bucket-key paths all run against live AOSS — regression for
		// PLFM-9673 (long-terms keys came back null on INTEGER facets without explicit
		// formatting). Each aggregation is keyed by column name so the response asserts
		// can find them after AOSS rewrites the field references back to names.
		Map<String, Aggregation> aggregations = new LinkedHashMap<>();
		for (ColumnModel column : columns) {
			if (casesByType.get(column.getColumnType()).expectedFacetValues == null) {
				continue;
			}
			// Text-typed columns are mapped as `text` for full-text search and aren't doc-values
			// candidates by default; aggregating against the bare field would require fielddata=true.
			// The mapping always emits a `.keyword` sub-field for text/link columns, so a caller-
			// supplied terms aggregation against a text column must reference that sub-field
			// explicitly. Numeric / keyword / list types take the bare column name.
			ColumnType colType = column.getColumnType();
			boolean isTextLike = colType == ColumnType.STRING
					|| colType == ColumnType.MEDIUMTEXT
					|| colType == ColumnType.LARGETEXT
					|| colType == ColumnType.LINK;
			String fieldRef = isTextLike ? column.getName() + ".keyword" : column.getName();
			aggregations.put(column.getName(),
					new Aggregation().setTerms(new TermsAggregation().setField(fieldRef)));
		}

		SearchQuery body = matchAllBody().setAggregations(aggregations);
		SearchQueryResults results = waitForRealRow(body, columns, 1L);

		Map<String, String> idToName = columns.stream()
				.collect(Collectors.toMap(ColumnModel::getId, ColumnModel::getName));
		SearchHit realRow = results.getHits().stream()
				.filter(h -> Long.valueOf(1L).equals(h.getRowId()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("real row was not returned"));
		Map<String, String> returnedByName = new HashMap<>();
		for (SearchFieldValue fv : realRow.getFields()) {
			returnedByName.put(fv.getName(), fv.getValue());
		}

		for (ColumnModel column : columns) {
			ColumnType type = column.getColumnType();
			String fieldName = idToName.get(column.getId());
			String actual = returnedByName.get(fieldName);
			assertNotNull(actual, "missing returned value for " + type);
			assertEquals(casesByType.get(type).expectedReturned, actual,
					"round-trip mismatch for " + type);
		}

		// Parse the opaque aggregations response. Each top-level key is the caller's
		// aggregation name; under it AOSS returns {"buckets":[{"key":..., "doc_count":...}, ...]}
		// (or sometimes "key_as_string" for typed buckets — accept both).
		assertNotNull(results.getAggregationResults(), "aggregations were requested");
		JsonNode aggResults = SearchOpaqueJsonUtil.parse(results.getAggregationResults());
		for (ColumnModel column : columns) {
			ColumnType type = column.getColumnType();
			Set<String> expectedValues = casesByType.get(type).expectedFacetValues;
			if (expectedValues == null) {
				continue;
			}
			JsonNode bucketsNode = aggResults.path(column.getName()).path("buckets");
			assertTrue(bucketsNode.isArray(),
					"missing aggregation buckets for " + type + ": " + results.getAggregationResults());
			Set<String> actualValues = new java.util.HashSet<>();
			for (JsonNode bucket : bucketsNode) {
				String value = bucket.path("key_as_string").asText(null);
				if (value == null || value.isEmpty()) {
					value = bucket.path("key").asText();
				}
				actualValues.add(value);
			}
			assertEquals(expectedValues, actualValues,
					"aggregation bucket values for " + type + " must match");
		}
	}

	private static Map<ColumnType, RoundTripCase> buildEveryColumnTypeCase() {
		Map<ColumnType, RoundTripCase> casesByType = new LinkedHashMap<>();
		// JSON fields are mapped to OpenSearch `object` and are not terms-aggregable, so
		// `expectedFacetValues` is null for JSON only and the test skips it for facets.
		casesByType.put(ColumnType.STRING,        new RoundTripCase("alpha",                              "alpha",                                  "alpha",                                  Set.of("alpha")));
		casesByType.put(ColumnType.STRING_LIST,   new RoundTripCase("[\"alpha\",\"beta\"]",               List.of("alpha", "beta"),                 "[\"alpha\",\"beta\"]",                   Set.of("alpha", "beta")));
		casesByType.put(ColumnType.MEDIUMTEXT,    new RoundTripCase("alpha beta gamma",                   "alpha beta gamma",                       "alpha beta gamma",                       Set.of("alpha beta gamma")));
		casesByType.put(ColumnType.LARGETEXT,     new RoundTripCase("alpha beta gamma",                   "alpha beta gamma",                       "alpha beta gamma",                       Set.of("alpha beta gamma")));
		casesByType.put(ColumnType.LINK,          new RoundTripCase("https://example.org/a",              "https://example.org/a",                  "https://example.org/a",                  Set.of("https://example.org/a")));
		casesByType.put(ColumnType.INTEGER,       new RoundTripCase("123",                                123,                                      "123",                                    Set.of("123")));
		casesByType.put(ColumnType.INTEGER_LIST,  new RoundTripCase("[1,2,3]",                            List.of(1, 2, 3),                         "[1,2,3]",                                Set.of("1", "2", "3")));
		casesByType.put(ColumnType.DATE,          new RoundTripCase("1609459200000",                      1609459200000L,                           "1609459200000",                          Set.of("1609459200000")));
		casesByType.put(ColumnType.DATE_LIST,     new RoundTripCase("[1609459200000,1609545600000]",      List.of(1609459200000L, 1609545600000L),  "[1609459200000,1609545600000]",          Set.of("1609459200000", "1609545600000")));
		casesByType.put(ColumnType.FILEHANDLEID,  new RoundTripCase("9876543",                            9876543,                                  "9876543",                                Set.of("9876543")));
		casesByType.put(ColumnType.SUBMISSIONID,  new RoundTripCase("555",                                555,                                      "555",                                    Set.of("555")));
		casesByType.put(ColumnType.EVALUATIONID,  new RoundTripCase("777",                                777,                                      "777",                                    Set.of("777")));
		casesByType.put(ColumnType.ENTITYID,      new RoundTripCase("syn123456",                          "syn123456",                              "syn123456",                              Set.of("syn123456")));
		casesByType.put(ColumnType.USERID,        new RoundTripCase("3412396",                            "3412396",                                "3412396",                                Set.of("3412396")));
		casesByType.put(ColumnType.ENTITYID_LIST, new RoundTripCase("[\"syn1\",\"syn2\"]",                List.of("syn1", "syn2"),                  "[\"syn1\",\"syn2\"]",                    Set.of("syn1", "syn2")));
		casesByType.put(ColumnType.USERID_LIST,   new RoundTripCase("[\"100\",\"200\"]",                  List.of("100", "200"),                    "[\"100\",\"200\"]",                      Set.of("100", "200")));
		casesByType.put(ColumnType.DOUBLE,        new RoundTripCase("1.5",                                1.5,                                      "1.5",                                    Set.of("1.5")));
		casesByType.put(ColumnType.BOOLEAN,       new RoundTripCase("true",                               Boolean.TRUE,                             "true",                                   Set.of("true")));
		casesByType.put(ColumnType.BOOLEAN_LIST,  new RoundTripCase("[true,false]",                       List.of(true, false),                     "[true,false]",                           Set.of("true", "false")));
		casesByType.put(ColumnType.JSON,          new RoundTripCase("{\"a\":1,\"b\":\"x\"}",              Map.of("a", 1, "b", "x"),                 "{\"a\":1,\"b\":\"x\"}",                  null));
		return casesByType;
	}

	private static final class RoundTripCase {
		final String raw;
		final Object expected;
		final String expectedReturned;
		// Expected bucket value strings when this column is requested as a facet.
		// Null for column types that are not terms-aggregable (e.g. JSON object fields).
		final Set<String> expectedFacetValues;
		RoundTripCase(String raw, Object expected, String expectedReturned, Set<String> expectedFacetValues) {
			this.raw = raw;
			this.expected = expected;
			this.expectedReturned = expectedReturned;
			this.expectedFacetValues = expectedFacetValues;
		}
	}

	/**
	 * Loads a bootstrapped system analyzer from the database by id and parses its stored
	 * settings JSON into the typed {@link IndexSettingsAnalysis}. Reading the live row
	 * keeps these tests from drifting away from the real configuration emitted by
	 * {@link TextAnalyzerBootstrapper}. The bootstrapped settings carry no $refs, so the
	 * resolver returns null and the boundary deserializer carries the rest.
	 */
	private IndexSettingsAnalysis bootstrappedAnalyzerSettings(long id) {
		TextAnalyzer ta = textAnalyzerDao.get(id).orElseThrow(() -> new IllegalStateException(
				"Bootstrapped TextAnalyzer not found for id " + id
						+ "; TextAnalyzerBootstrapper should have populated it on startup."));
		return SearchOpaqueJsonUtil.resolveAnalyzerSettings(SearchOpaqueJsonUtil.parse(ta.getSettings()), qname -> null);
	}

	/**
	 * Persist a SynonymSet with the given {@code synonym_graph} definition under a
	 * unique-per-test name in {@code org.sagebionetworks}, register it for cleanup,
	 * and return its qualified name suitable for a {@code $ref} target.
	 */
	private String createSynonymSet(String definition) {
		String name = "syn_" + UUID.randomUUID().toString().substring(0, 8);
		Long adminUserId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		SynonymSet created = synonymSetDao.create(adminUserId, new SynonymSet()
				.setOrganizationName("org.sagebionetworks")
				.setName(name)
				.setDefinition(definition));
		createdSynonymSetIds.add(created.getId());
		return "org.sagebionetworks-" + name;
	}

	/**
	 * Build a synonym-aware variant of a bootstrapped analyzer: clone its stored settings
	 * JSON, register a top-level {@code filter.synapse_synonyms = {"$ref": <qname>}} slot,
	 * and add a {@code default_search} chain that runs {@code lowercase → synapse_synonyms}
	 * BEFORE the default chain's word_delimiter (so the synonym filter sees the un-split
	 * multi-word LHS — synonym must precede word_delimiter for hyphenated rules to fire).
	 * Then resolve through {@link SynonymSetDao#getByQualifiedNames} exactly like
	 * {@link SearchIndexLifecycleManagerImpl#resolveAnalyzers} does in production. Synonym
	 * expansion runs at search time only — index-time chain stays unchanged so the
	 * Lucene offset-monotonicity bug from PLFM-9636 cannot recur.
	 */
	private IndexSettingsAnalysis synonymAwareAnalyzer(long bootstrapId, String synonymQname) {
		TextAnalyzer ta = textAnalyzerDao.get(bootstrapId).orElseThrow(() -> new IllegalStateException(
				"Bootstrapped TextAnalyzer not found for id " + bootstrapId));
		ObjectNode root = (ObjectNode) SearchOpaqueJsonUtil.parse(ta.getSettings());

		ObjectNode filterMap = root.has("filter") && root.get("filter").isObject()
				? (ObjectNode) root.get("filter")
				: root.putObject("filter");
		ObjectNode ref = filterMap.putObject("synapse_synonyms");
		ref.put("$ref", synonymQname);

		ObjectNode analyzerMap = (ObjectNode) root.get("analyzer");
		ObjectNode defaultAnalyzer = (ObjectNode) analyzerMap.get("default");
		ObjectNode searchAnalyzer = defaultAnalyzer.deepCopy();
		ArrayNode defaultChain = (ArrayNode) searchAnalyzer.get("filter");
		ArrayNode rebuilt = defaultChain.arrayNode();
		rebuilt.add("lowercase");
		rebuilt.add("synapse_synonyms");
		for (JsonNode n : defaultChain) {
			if (!"lowercase".equals(n.asText())) {
				rebuilt.add(n);
			}
		}
		searchAnalyzer.set("filter", rebuilt);
		analyzerMap.set("default_search", searchAnalyzer);

		return SearchOpaqueJsonUtil.resolveAnalyzerSettings(root, qname -> {
			Map<String, SynonymSet> hits = synonymSetDao.getByQualifiedNames(
					Collections.singletonList(qname));
			SynonymSet ss = hits.get(qname);
			return ss == null ? null : SearchOpaqueJsonUtil.parse(ss.getDefinition());
		});
	}

	/**
	 * STRING columns resolve to SCIENTIFIC by default; synonym tests need to bind the
	 * test column to the analyzer they configured so its synapse_synonyms filter actually
	 * runs at search time.
	 */
	private static org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride bindColumnToAnalyzer(
			String columnName, String analyzerQname) {
		return new org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride()
				.setOverrides(List.of(new org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverrideEntry()
						.setColumnName(columnName)
						.setAnalyzer(Map.of("$ref", analyzerQname))));
	}

	// ---- Polling helpers ----

	/**
	 * Poll until search returns at least {@code expectedMinHits} results.
	 * AOSS is eventually consistent — documents may not be visible immediately after indexing.
	 */
	private SearchQueryResults waitForSearch(SearchQuery body, List<ColumnModel> columns,
			long expectedMinHits) {
		SearchQueryResults[] result = {null};
		boolean success = TimeUtils.waitForExponential(POLL_MAX_MS, POLL_INTERVAL_MS, null, (v) -> {
			try {
				result[0] = openSearchManager.search(indexName, body, columns,
						EnumSet.allOf(SearchQueryPart.class), Collections.emptyList());
				return result[0].getTotalHits() != null && result[0].getTotalHits() >= expectedMinHits;
			} catch (IllegalStateException e) {
				// index_not_found — not ready yet
				return false;
			}
		});
		assertTrue(success, "Timed out waiting for search results (expected at least " + expectedMinHits + " hits)");
		return result[0];
	}

	/**
	 * Poll {@link OpenSearchManager#search} until a hit for the given real {@code _row_id} is
	 * present. Gating on {@code totalHits} alone is not sufficient right after
	 * {@link OpenSearchManager#waitForIndexWritable}: the readiness sentinel
	 * ({@code _row_id = -1}) and its deletion refresh independently of the real row's write, so
	 * a {@code match_all} probe can report a hit for the leftover sentinel before the real row
	 * is visible. Waiting for the real row-id removes that race.
	 */
	private SearchQueryResults waitForRealRow(SearchQuery body, List<ColumnModel> columns, long rowId) {
		SearchQueryResults[] result = {null};
		boolean success = TimeUtils.waitForExponential(POLL_MAX_MS, POLL_INTERVAL_MS, null, (v) -> {
			try {
				result[0] = openSearchManager.search(indexName, body, columns,
						EnumSet.allOf(SearchQueryPart.class), Collections.emptyList());
				return result[0].getHits() != null && result[0].getHits().stream()
						.anyMatch(h -> Long.valueOf(rowId).equals(h.getRowId()));
			} catch (IllegalStateException e) {
				// index_not_found — not ready yet
				return false;
			}
		});
		assertTrue(success, "Timed out waiting for search to return row " + rowId);
		return result[0];
	}

	/**
	 * Poll {@link OpenSearchManager#search} until it returns without throwing
	 * {@link IllegalStateException} (the wrapper around AOSS's {@code index_not_found_exception}
	 * — see {@link OpenSearchManagerImpl#executeSearch}). AOSS replicas are not strongly
	 * consistent, so a freshly-created index that one node has acknowledged may briefly be
	 * absent at another. Used by tests that need to vary {@code parts} per call rather than
	 * always asking for {@code EnumSet.allOf(...)} like {@link #waitForSearch} does.
	 */
	private SearchQueryResults searchWithRetry(SearchQuery body, List<ColumnModel> columns,
			Set<SearchQueryPart> parts) {
		SearchQueryResults[] result = {null};
		boolean success = TimeUtils.waitForExponential(POLL_MAX_MS, POLL_INTERVAL_MS, null, (v) -> {
			try {
				result[0] = openSearchManager.search(indexName, body, columns, parts, Collections.emptyList());
				return true;
			} catch (IllegalStateException e) {
				return false;
			}
		});
		assertTrue(success, "Timed out waiting for search to succeed");
		return result[0];
	}

	/**
	 * Poll {@link OpenSearchManager#search} until the returned hit list reaches exactly
	 * {@code expectedHits}. {@link #waitForSearch} gates only on the {@code totalHits} of a
	 * {@code match_all} probe, but AOSS is eventually consistent per query and per replica:
	 * a {@code match}/{@code collapse}/{@code rescore} search issued right after indexing can
	 * transiently observe fewer of the just-indexed rows than {@code match_all} already
	 * reported (the replica serving this query may not have caught up yet). Tests that assert
	 * on an exact hit count must therefore poll the specific query they assert on rather than
	 * trusting a prior {@code match_all} wait.
	 */
	private SearchQueryResults waitForSearchHits(SearchQuery body, List<ColumnModel> columns,
			Set<SearchQueryPart> parts, int expectedHits) {
		SearchQueryResults[] result = {null};
		boolean success = TimeUtils.waitForExponential(POLL_MAX_MS, POLL_INTERVAL_MS, null, (v) -> {
			try {
				result[0] = openSearchManager.search(indexName, body, columns, parts, Collections.emptyList());
				return result[0].getHits() != null && result[0].getHits().size() == expectedHits;
			} catch (IllegalStateException e) {
				// index_not_found — not ready yet
				return false;
			}
		});
		assertTrue(success, "Timed out waiting for search to return " + expectedHits + " hits");
		return result[0];
	}

	private SearchQueryResults waitForAutocomplete(SearchAutocompleteBody body, List<ColumnModel> columns,
			long expectedMinHits) {
		SearchQueryResults[] result = {null};
		boolean success = TimeUtils.waitForExponential(POLL_MAX_MS, POLL_INTERVAL_MS, null, (v) -> {
			try {
				result[0] = openSearchManager.autocomplete(indexName, body, columns,
						EnumSet.allOf(SearchQueryPart.class), Collections.emptyList());
				return result[0].getTotalHits() != null && result[0].getTotalHits() >= expectedMinHits;
			} catch (IllegalStateException e) {
				return false;
			}
		});
		assertTrue(success, "Timed out waiting for autocomplete results (expected at least " + expectedMinHits + " hits)");
		return result[0];
	}

	// ---- Test data helpers ----

	/**
	 * Build an OpenSearch {@code _search} body wrapping a single {@code simple_query_string}
	 * clause &mdash; the most common shape across these tests. Pass {@code null} for
	 * {@code fields} to let OpenSearch search every indexed text-bearing field.
	 */
	private static SearchQuery simpleQueryStringBody(String text, List<String> fields) {
		SimpleQueryStringQuery sqs = new SimpleQueryStringQuery().setQuery(text);
		if (fields != null && !fields.isEmpty()) {
			sqs.setFields(new ArrayList<Object>(fields));
		}
		return new SearchQuery()
				.setQuery(new Query().setSimple_query_string(sqs))
				.setSize(10L)
				.setFrom(0L);
	}

	/** Convenience overload without per-field restriction. */
	private static SearchQuery simpleQueryStringBody(String text) {
		return simpleQueryStringBody(text, null);
	}

	/** Build a body wrapping a {@code match_all} clause. */
	private static SearchQuery matchAllBody() {
		return new SearchQuery()
				.setQuery(new Query().setMatch_all(new MatchAllQuery()))
				.setSize(10L)
				.setFrom(0L);
	}

	private Map<String, IndexSettingsAnalysis> buildDefaultAnalyzers() {
		Map<String, IndexSettingsAnalysis> analyzers = new HashMap<>();
		analyzers.put("org.sagebionetworks-SCIENTIFIC", bootstrappedAnalyzerSettings(TextAnalyzerBootstrapper.SCIENTIFIC_ID));
		analyzers.put("org.sagebionetworks-KEYWORD", bootstrappedAnalyzerSettings(TextAnalyzerBootstrapper.KEYWORD_ID));
		analyzers.put("org.sagebionetworks-STANDARD", bootstrappedAnalyzerSettings(TextAnalyzerBootstrapper.STANDARD_ID));
		return analyzers;
	}

	private static BulkOperation buildBulkOp(String indexName, String docId, Map<String, Object> doc) {
		return BulkOperation.of(op -> op
				.index(idx -> idx
						.index(indexName)
						.id(docId)
						.document(doc)));
	}

	/**
	 * Single live-AOSS round trip exercising every kind in
	 * {@link SearchDslValidator#ALLOWED_QUERY_KINDS}. Each kind is wrapped in its own
	 * caller-supplied body and dispatched at AOSS; the EnumSet coverage guard at the bottom
	 * fails the test if a future allowlist relaxation adds a kind without a fixture row.
	 *
	 * <p>The fixture columns have one numeric (year) and one text (title) column so kinds
	 * that need numeric ranges and kinds that need string operations both have a real
	 * column to point at. Each query is run as a {@code SearchQuery.body} wrapping the kind
	 * envelope plus the standard {@code from}/{@code size}.</p>
	 */
	@Test
	public void testSearchWithEveryAllowedQueryKindRoundTrips() {
		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("title").setColumnType(ColumnType.STRING),
				new ColumnModel().setId("2").setName("year").setColumnType(ColumnType.INTEGER));
		openSearchManager.createIndex(indexName, columns, null,
				Collections.emptyList(), defaultAnalyzers, 0, 1, 0);
		openSearchManager.waitForIndexWritable(indexName);

		List<BulkOperation> operations = List.of(
				buildBulkOp(indexName, "1", Map.of("_row_id", 1L, "_row_version", 1L,
						"1", "amyloid plaques", "2", "2024")),
				buildBulkOp(indexName, "2", Map.of("_row_id", 2L, "_row_version", 1L,
						"1", "tau tangles", "2", "2023")));
		openSearchManager.bulkIndex(indexName, operations);

		// Wait until both docs are visible, then issue every kind without re-polling.
		waitForSearch(matchAllBody(), columns, 2);

		Map<Kind, Supplier<SearchQuery>> queries = new LinkedHashMap<>();
		queries.put(Kind.Match,
				() -> queryBody(new Query().setMatch(
						Map.of("title", new MatchFieldOptions().setQuery("amyloid")))));
		queries.put(Kind.MultiMatch,
				() -> queryBody(new Query().setMulti_match(new MultiMatchQuery()
						.setQuery("amyloid").setFields(List.of("title")))));
		queries.put(Kind.MatchPhrase,
				() -> queryBody(new Query().setMatch_phrase(
						Map.of("title", new MatchPhraseFieldOptions().setQuery("amyloid plaques")))));
		// MatchPhrasePrefix is omitted: the field rewriter auto-routes text columns to
		// .keyword for term-family clauses (per the production routing table), but AOSS
		// rejects phrase-prefix on keyword fields with "Can only use phrase prefix queries
		// on text fields". Phrase-prefix is only valid against an analyzer-bound text
		// column — already covered end-to-end by
		// testRoundTripWithAutocompleteBootstrappedAnalyzer (with an AUTOCOMPLETE override).
		queries.put(Kind.MatchBoolPrefix,
				() -> queryBody(new Query().setMatch_bool_prefix(
						Map.of("title", new MatchBoolPrefixFieldOptions().setQuery("amyloid pla")))));
		queries.put(Kind.Term,
				() -> queryBody(new Query().setTerm(
						Map.of("year", new TermFieldOptions().setValue(2024)))));
		queries.put(Kind.Terms,
				() -> queryBody(new Query().setTerms(new JSONObject(
						Map.of("year", List.of(2023, 2024))))));
		queries.put(Kind.Range,
				() -> queryBody(new Query().setRange(
						Map.of("year", new RangeFieldOptions().setGte(2024)))));
		queries.put(Kind.Exists,
				() -> queryBody(new Query().setExists(new ExistsQuery().setField("title"))));
		queries.put(Kind.Prefix,
				() -> queryBody(new Query().setPrefix(
						Map.of("title", new PrefixFieldOptions().setValue("amyl")))));
		queries.put(Kind.Wildcard,
				() -> queryBody(new Query().setWildcard(
						Map.of("title", new WildcardFieldOptions().setValue("amyloid*")))));
		queries.put(Kind.Fuzzy,
				() -> queryBody(new Query().setFuzzy(
						Map.of("title", new FuzzyFieldOptions().setValue("amyloid")))));
		queries.put(Kind.SimpleQueryString,
				() -> queryBody(new Query().setSimple_query_string(
						new SimpleQueryStringQuery().setQuery("amyloid"))));
		queries.put(Kind.MatchAll,
				() -> matchAllBody());
		queries.put(Kind.Bool,
				() -> queryBody(new Query().setBool(new BoolQuery()
						.setMust(List.of(new Query().setMatch_all(new MatchAllQuery())))
						.setFilter(List.of(new Query().setTerm(
								Map.of("year", new TermFieldOptions().setValue(2024))))))));
		queries.put(Kind.DisMax,
				() -> queryBody(new Query().setDis_max(new DisMaxQuery().setQueries(List.of(
						new Query().setMatch(Map.of("title", new MatchFieldOptions().setQuery("amyloid"))),
						new Query().setTerm(Map.of("year", new TermFieldOptions().setValue(2024))))))));
		queries.put(Kind.ConstantScore,
				() -> queryBody(new Query().setConstant_score(new ConstantScoreQuery().setFilter(
						new Query().setTerm(Map.of("year", new TermFieldOptions().setValue(2024)))))));
		queries.put(Kind.Boosting,
				() -> queryBody(new Query().setBoosting(new BoostingQuery()
						.setPositive(new Query().setMatch(
								Map.of("title", new MatchFieldOptions().setQuery("amyloid"))))
						.setNegative(new Query().setTerm(
								Map.of("year", new TermFieldOptions().setValue(2023))))
						.setNegative_boost(0.5))));

		EnumSet<Kind> covered = EnumSet.noneOf(Kind.class);
		for (Map.Entry<Kind, Supplier<SearchQuery>> entry : queries.entrySet()) {
			SearchQuery body = entry.getValue().get();
			// call under test — every kind must round-trip without throwing
			SearchQueryResults result = searchWithRetry(body, columns,
					EnumSet.of(SearchQueryPart.HITS, SearchQueryPart.TOTAL_HITS));
			assertNotNull(result, "kind " + entry.getKey() + " produced null result");
			assertNotNull(result.getTotalHits(), "kind " + entry.getKey() + " missing totalHits");
			covered.add(entry.getKey());
		}

		// Coverage guard: every allowlisted kind must appear in this round-trip except
		// MatchPhrasePrefix (covered by testRoundTripWithAutocompleteBootstrappedAnalyzer).
		EnumSet<Kind> expected = EnumSet.copyOf(SearchDslValidator.ALLOWED_QUERY_KINDS);
		expected.remove(Kind.MatchPhrasePrefix);
		assertEquals(expected, covered,
				"every allowlisted query kind (except MatchPhrasePrefix) must appear in this round-trip");
	}

	/**
	 * Single live-AOSS round trip exercising every part in
	 * {@link SearchQueryPart} (singly and combined). Asserts each part bit causes the
	 * corresponding response field to be populated and absent when the bit is off.
	 * Coverage guard against {@code SearchQueryPart.values()} at the bottom.
	 */
	@Test
	public void testSearchWithEverySearchQueryPartCombination() {
		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("title").setColumnType(ColumnType.STRING));
		openSearchManager.createIndex(indexName, columns, null,
				Collections.emptyList(), defaultAnalyzers, 0, 1, 0);
		openSearchManager.waitForIndexWritable(indexName);
		openSearchManager.bulkIndex(indexName, List.of(
				buildBulkOp(indexName, "1", Map.of("_row_id", 1L, "_row_version", 1L, "1", "amyloid"))));
		// Wait once for visibility; subsequent searches reuse the same indexed doc.
		waitForSearch(matchAllBody(), columns, 1);

		EnumSet<SearchQueryPart> guard = EnumSet.noneOf(SearchQueryPart.class);
		SearchQueryPart[] all = SearchQueryPart.values();
		for (int mask = 0; mask < (1 << all.length); mask++) {
			EnumSet<SearchQueryPart> parts = EnumSet.noneOf(SearchQueryPart.class);
			for (int b = 0; b < all.length; b++) {
				if ((mask & (1 << b)) != 0) {
					parts.add(all[b]);
					guard.add(all[b]);
				}
			}
			// call under test
			SearchQueryResults r = searchWithRetry(matchAllBody(), columns, parts);

			assertEquals(parts.contains(SearchQueryPart.HITS), r.getHits() != null,
					"HITS gate, mask=" + mask);
			assertEquals(parts.contains(SearchQueryPart.TOTAL_HITS), r.getTotalHits() != null,
					"TOTAL_HITS gate, mask=" + mask);
			// SELECT_COLUMNS shaping happens at SearchIndexQueryManagerImpl, not at this
			// layer — the OpenSearchManager itself never touches selectColumns.
			assertNull(r.getSelectColumns(),
					"SELECT_COLUMNS shaping is not at this layer, mask=" + mask);
		}
		assertEquals(EnumSet.allOf(SearchQueryPart.class), guard,
				"every SearchQueryPart must be exercised across the powerset");
	}

	/**
	 * Build a {@code SearchQuery} body wrapping an arbitrary opaque OpenSearch DSL query
	 * clause as the {@code query} slot, with default {@code size}/{@code from} and no other
	 * top-level keys. Used by {@link #testSearchWithEveryAllowedQueryKindRoundTrips} to
	 * vary the inner clause shape per kind without rebuilding the rest of the envelope.
	 */
	private static SearchQuery queryBody(Query query) {
		return new SearchQuery()
				.setQuery(query)
				.setSize(10L)
				.setFrom(0L);
	}

	/**
	 * {@code post_filter} is applied AFTER aggregations are computed, so aggregation buckets
	 * must reflect the unfiltered population matched by {@code query} while the returned hits
	 * are narrowed by {@code post_filter}. A {@code bool.filter} placed inside {@code query}
	 * has the opposite shape (aggregations also shrink). Two distinct status values are seeded
	 * so the assertion can distinguish those two semantics. Bare column names exercise the
	 * server's auto-routing through {@code .keyword} for term clauses against text columns.
	 */
	@Test
	public void testSearchWithPostFilter() {
		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("status").setColumnType(ColumnType.STRING));
		openSearchManager.createIndex(indexName, columns, null,
				Collections.emptyList(), defaultAnalyzers, 0, 1, 0);
		openSearchManager.waitForIndexWritable(indexName);

		openSearchManager.bulkIndex(indexName, List.of(
				buildBulkOp(indexName, "1", Map.of("_row_id", 1L, "_row_version", 1L, "1", "ACTIVE")),
				buildBulkOp(indexName, "2", Map.of("_row_id", 2L, "_row_version", 1L, "1", "ACTIVE")),
				buildBulkOp(indexName, "3", Map.of("_row_id", 3L, "_row_version", 1L, "1", "INACTIVE")),
				buildBulkOp(indexName, "4", Map.of("_row_id", 4L, "_row_version", 1L, "1", "INACTIVE")),
				buildBulkOp(indexName, "5", Map.of("_row_id", 5L, "_row_version", 1L, "1", "INACTIVE"))));
		waitForSearch(matchAllBody(), columns, 5);

		SearchQuery body = new SearchQuery()
				.setQuery(new Query().setMatch_all(new MatchAllQuery()))
				.setAggregations(Map.of("by_status",
						new Aggregation().setTerms(new TermsAggregation().setField("status"))))
				.setPost_filter(new Query().setTerm(
						Map.of("status", new TermFieldOptions().setValue("ACTIVE"))))
				.setSize(10L)
				.setFrom(0L);

		// call under test — post_filter narrows hits; aggregations stay at full population.
		// Poll until post_filter returns the expected 2 ACTIVE hits.
		SearchQueryResults results = waitForSearchHits(body, columns,
				EnumSet.of(SearchQueryPart.HITS, SearchQueryPart.TOTAL_HITS), 2);

		assertEquals(2L, results.getTotalHits(),
				"totalHits must reflect post_filter narrowing — only ACTIVE rows");
		assertNotNull(results.getHits());
		assertEquals(2, results.getHits().size());

		assertNotNull(results.getAggregationResults(), "aggregations were requested");
		JsonNode aggResults = SearchOpaqueJsonUtil.parse(results.getAggregationResults());
		Map<String, Long> counts = new HashMap<>();
		for (JsonNode bucket : aggResults.path("by_status").path("buckets")) {
			counts.put(bucket.path("key").asText(), bucket.path("doc_count").asLong());
		}
		assertEquals(Long.valueOf(2L), counts.get("ACTIVE"),
				"ACTIVE bucket must report 2 (full population, not post-filtered)");
		assertEquals(Long.valueOf(3L), counts.get("INACTIVE"),
				"INACTIVE bucket must be present with full count — post_filter "
						+ "must NOT narrow aggregations (that's the bool.filter shape)");
	}

	/**
	 * {@code highlight} fragments must round-trip and surface as a structured per-hit list.
	 * Each {@link org.sagebionetworks.repo.model.search.SearchHighlight} is keyed by the bare
	 * column name (server rewrites the AOSS field reference back) and snippets wrap the
	 * matched term in the default {@code <em>...</em>} tags.
	 */
	@Test
	public void testSearchWithHighlight() {
		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("description").setColumnType(ColumnType.LARGETEXT));
		openSearchManager.createIndex(indexName, columns, null,
				Collections.emptyList(), defaultAnalyzers, 0, 1, 0);
		openSearchManager.waitForIndexWritable(indexName);

		openSearchManager.bulkIndex(indexName, List.of(
				buildBulkOp(indexName, "1", Map.of("_row_id", 1L, "_row_version", 1L, "1", "BRCA1 tumor suppressor gene")),
				buildBulkOp(indexName, "2", Map.of("_row_id", 2L, "_row_version", 1L, "1", "BRCA2 tumor suppressor gene")),
				buildBulkOp(indexName, "3", Map.of("_row_id", 3L, "_row_version", 1L, "1", "TP53 tumor suppressor gene"))));
		waitForSearch(matchAllBody(), columns, 3);

		SearchQuery body = new SearchQuery()
				.setQuery(new Query().setMatch(
						Map.of("description", new MatchFieldOptions().setQuery("tumor"))))
				.setHighlight(new Highlight().setFields(Map.of("description", new HighlightField())))
				.setSize(10L)
				.setFrom(0L);

		// call under test — highlight payload round-trips and SearchHit.highlights is populated.
		// Poll until the match query returns all 3 hits.
		SearchQueryResults results = waitForSearchHits(body, columns,
				EnumSet.of(SearchQueryPart.HITS, SearchQueryPart.TOTAL_HITS), 3);

		assertEquals(3L, results.getTotalHits());
		assertNotNull(results.getHits());
		assertEquals(3, results.getHits().size());
		for (org.sagebionetworks.repo.model.search.SearchHit hit : results.getHits()) {
			List<org.sagebionetworks.repo.model.search.SearchHighlight> highlights = hit.getHighlights();
			assertNotNull(highlights, "highlights must be populated when highlight requested");
			assertEquals(1, highlights.size(),
					"only the description field has matches and was requested");
			org.sagebionetworks.repo.model.search.SearchHighlight h = highlights.get(0);
			assertEquals("description", h.getName(),
					"server must rewrite the response field reference back to the bare column name");
			assertNotNull(h.getSnippets());
			assertTrue(h.getSnippets().size() >= 1, "expected at least one snippet fragment");
			assertTrue(h.getSnippets().get(0).contains("<em>tumor</em>"),
					"snippet must wrap the matched 'tumor' term in <em> tags; got: "
							+ h.getSnippets().get(0));
		}
	}

	/**
	 * Exercises {@code collapse} and {@code rescore} on the same projA/projB amyloid fixture.
	 *
	 * <p>{@code collapse} groups results so one hit per distinct value of {@code field} is
	 * returned: collapse on {@code projectId} must yield exactly two hits surfacing the two
	 * distinct project ids.
	 *
	 * <p>{@code rescore} re-ranks the top {@code window_size} hits using a secondary scoring
	 * query: a phrase boost (weight 5×) on 'amyloid plaques' must lift the three projA rows
	 * (which contain the phrase) above the three projB rows that match only 'amyloid'.
	 *
	 * <p>{@code collapse} and {@code rescore} are mutually exclusive at the OpenSearch engine
	 * layer, so each is exercised in its own search call against the shared fixture.
	 */
	@Test
	public void testSearchWithCollapseAndRescore() {
		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("projectId").setColumnType(ColumnType.STRING),
				new ColumnModel().setId("2").setName("title").setColumnType(ColumnType.LARGETEXT));
		openSearchManager.createIndex(indexName, columns, null,
				Collections.emptyList(), defaultAnalyzers, 0, 1, 0);
		openSearchManager.waitForIndexWritable(indexName);

		openSearchManager.bulkIndex(indexName, List.of(
				buildBulkOp(indexName, "1", Map.of("_row_id", 1L, "_row_version", 1L,
						"1", "projA", "2", "amyloid plaques in cortex")),
				buildBulkOp(indexName, "2", Map.of("_row_id", 2L, "_row_version", 1L,
						"1", "projA", "2", "amyloid plaques in hippocampus")),
				buildBulkOp(indexName, "3", Map.of("_row_id", 3L, "_row_version", 1L,
						"1", "projA", "2", "amyloid plaques and tau")),
				buildBulkOp(indexName, "4", Map.of("_row_id", 4L, "_row_version", 1L,
						"1", "projB", "2", "amyloid precursor protein")),
				buildBulkOp(indexName, "5", Map.of("_row_id", 5L, "_row_version", 1L,
						"1", "projB", "2", "amyloid beta peptide")),
				buildBulkOp(indexName, "6", Map.of("_row_id", 6L, "_row_version", 1L,
						"1", "projB", "2", "amyloid signaling pathway"))));
		waitForSearch(matchAllBody(), columns, 6);

		SearchQuery collapseBody = new SearchQuery()
				.setQuery(new Query().setMatch(
						Map.of("title", new MatchFieldOptions().setQuery("amyloid"))))
				.setCollapse(new FieldCollapse().setField("projectId"))
				.setSize(10L)
				.setFrom(0L);

		// call under test — collapse yields one hit per distinct projectId. Poll for the
		// exact hit count: AOSS is eventually consistent per query, so this collapse search
		// can lag the earlier match_all wait until both projects' rows are visible here.
		SearchQueryResults collapseResults = waitForSearchHits(collapseBody, columns,
				EnumSet.of(SearchQueryPart.HITS, SearchQueryPart.TOTAL_HITS), 2);

		assertNotNull(collapseResults.getHits());
		assertEquals(2, collapseResults.getHits().size(),
				"collapse on projectId must return one hit per distinct value");
		Set<String> projectIds = collapseResults.getHits().stream()
				.map(hit -> hit.getFields().stream()
						.filter(f -> "projectId".equals(f.getName()))
						.findFirst()
						.orElseThrow(() -> new AssertionError("no 'projectId' field on hit"))
						.getValue())
				.collect(Collectors.toSet());
		assertEquals(Set.of("projA", "projB"), projectIds,
				"collapse must surface both distinct projectId values");

		SearchQuery rescoreBody = new SearchQuery()
				.setQuery(new Query().setMatch(
						Map.of("title", new MatchFieldOptions().setQuery("amyloid"))))
				.setRescore(new Rescore()
						.setWindow_size(50L)
						.setQuery(new RescoreQuery()
								.setRescore_query(new Query().setMatch_phrase(
										Map.of("title", new MatchPhraseFieldOptions().setQuery("amyloid plaques"))))
								.setQuery_weight(1.0)
								.setRescore_query_weight(5.0)))
				.setSize(10L)
				.setFrom(0L);

		// call under test — rescore must lift the three 'amyloid plaques' rows to the top.
		// Poll for all six rows: rescore never drops hits, so a transient short count here is
		// AOSS eventual consistency (this query's replica lagging the earlier match_all wait),
		// not a scoring effect — wait it out before asserting on order.
		SearchQueryResults rescoreResults = waitForSearchHits(rescoreBody, columns,
				EnumSet.of(SearchQueryPart.HITS, SearchQueryPart.TOTAL_HITS), 6);

		assertNotNull(rescoreResults.getHits());
		assertEquals(6, rescoreResults.getHits().size(),
				"base match on 'amyloid' returns every row");
		List<String> topThreeProjectIds = rescoreResults.getHits().subList(0, 3).stream()
				.map(hit -> hit.getFields().stream()
						.filter(f -> "projectId".equals(f.getName()))
						.findFirst()
						.orElseThrow(() -> new AssertionError("no 'projectId' field on hit"))
						.getValue())
				.collect(Collectors.toList());
		assertEquals(List.of("projA", "projA", "projA"), topThreeProjectIds,
				"rescore boost on 'amyloid plaques' must rank all three projA rows above projB");
	}
}
