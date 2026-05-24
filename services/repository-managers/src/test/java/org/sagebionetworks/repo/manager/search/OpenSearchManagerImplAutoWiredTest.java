package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.dbo.search.SynonymSetDao;
import org.sagebionetworks.repo.model.dbo.search.TextAnalyzerDao;
import org.sagebionetworks.repo.model.search.FacetRequest;
import org.sagebionetworks.repo.model.search.SearchFieldValue;
import org.sagebionetworks.repo.model.search.SearchQuery;
import org.sagebionetworks.repo.model.search.SearchQueryPart;
import org.sagebionetworks.repo.model.search.SearchQueryResults;
import org.sagebionetworks.repo.model.search.SearchQueryType;
import org.sagebionetworks.repo.model.search.table.SynonymSet;
import org.sagebionetworks.repo.model.search.table.TextAnalyzer;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnResult;
import org.sagebionetworks.repo.model.table.FacetColumnResultValueCount;
import org.sagebionetworks.repo.model.table.FacetColumnResultValues;
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
	public void testCreateIndexWithValidColumns() {
		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("name").setColumnType(ColumnType.STRING),
				new ColumnModel().setId("2").setName("age").setColumnType(ColumnType.INTEGER)
		);

		// call under test
		Optional<String> appliedConfig = openSearchManager.createIndex(indexName, columns, null,
				Collections.emptyList(), defaultAnalyzers, null);

		assertTrue(appliedConfig.isPresent());
		assertTrue(appliedConfig.get().length() > 0);
	}

	@Test
	public void testDeleteIndexWithExistingIndex() {
		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("name").setColumnType(ColumnType.STRING));
		openSearchManager.createIndex(indexName, columns, null,
				Collections.emptyList(), defaultAnalyzers, null);
		openSearchManager.waitForIndexWritable(indexName);

		// call under test
		openSearchManager.deleteIndex(indexName);

		// call under test — deleting again should be a no-op
		openSearchManager.deleteIndex(indexName);
	}

	@Test
	public void testDeleteIndexWithNonExistentIndex() {
		// call under test — should not throw
		openSearchManager.deleteIndex("nonexistent-index-" + UUID.randomUUID());
	}

	@Test
	public void testCreateIndexWithDuplicateName() {
		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("name").setColumnType(ColumnType.STRING));
		openSearchManager.createIndex(indexName, columns, null,
				Collections.emptyList(), defaultAnalyzers, null);
		openSearchManager.waitForIndexWritable(indexName);

		// call under test — resource_already_exists returns empty Optional
		Optional<String> result = openSearchManager.createIndex(indexName, columns, null,
				Collections.emptyList(), defaultAnalyzers, null);

		assertTrue(result.isEmpty());
	}

	@Test
	public void testBulkIndexWithEmptyList() {
		// call under test
		long indexed = openSearchManager.bulkIndex(indexName, Collections.emptyList());

		assertEquals(0L, indexed);
	}

	@Test
	public void testCRUDWithSearchQueryAndDocumentVerification() {
		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("title").setColumnType(ColumnType.STRING),
				new ColumnModel().setId("2").setName("count").setColumnType(ColumnType.INTEGER)
		);
		openSearchManager.createIndex(indexName, columns, null,
				Collections.emptyList(), defaultAnalyzers, null);
		openSearchManager.waitForIndexWritable(indexName);

		List<BulkOperation> operations = List.of(
				buildBulkOp(indexName, "1", Map.of("_row_id", 1L, "_row_version", 1L, "1", "mitochondria research", "2", "42")),
				buildBulkOp(indexName, "2", Map.of("_row_id", 2L, "_row_version", 1L, "1", "genome sequencing study", "2", "99")),
				buildBulkOp(indexName, "3", Map.of("_row_id", 3L, "_row_version", 1L, "1", "mitochondria function", "2", "7"))
		);

		// call under test — bulk index should succeed on first attempt without retries
		long indexed = openSearchManager.bulkIndex(indexName, operations);

		assertEquals(3L, indexed);

		SearchQuery query = new SearchQuery();
		query.setQueryText("mitochondria");
		query.setQueryType(SearchQueryType.SIMPLE_QUERY_STRING);
		query.setLimit(10L);
		query.setOffset(0L);

		// call under test — poll for search results (AOSS eventual consistency)
		SearchQueryResults results = waitForSearch(query, columns, 2);

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
				Collections.emptyList(), analyzers, null);
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

		SearchQuery query = new SearchQuery();
		// "the" is a stop word for the english_stop filter — if the custom analyzer wasn't
		// applied at search time, "the genome" would also match docs that lack "genome"
		// (anything containing "the"). Asserting exactly one hit confirms stop-word removal
		// is in effect.
		query.setQueryText("the genome");
		query.setQueryType(SearchQueryType.SIMPLE_QUERY_STRING);
		query.setLimit(10L);
		query.setOffset(0L);

		// call under test
		SearchQueryResults results = waitForSearch(query, columns, 1);

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
				"org.sagebionetworks-SCIENTIFIC", List.of(overrideContainer), defaultAnalyzers, null);
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
		SearchQuery exactQuery = new SearchQuery();
		exactQuery.setQueryText("BioMed-Cancer");
		exactQuery.setQueryType(SearchQueryType.SIMPLE_QUERY_STRING);
		exactQuery.setQueryFields(List.of("tag"));
		exactQuery.setLimit(10L);
		exactQuery.setOffset(0L);

		// call under test
		SearchQueryResults exactResults = waitForSearch(exactQuery, columns, 1);
		assertEquals(1L, exactResults.getTotalHits(),
				"KEYWORD override on `tag` must match the exact case-preserving token");

		// Stemmed query against `title` matches doc 2 ("biomed papers" → "biomed paper" stem).
		SearchQuery stemmedQuery = new SearchQuery();
		stemmedQuery.setQueryText("paper");
		stemmedQuery.setQueryType(SearchQueryType.SIMPLE_QUERY_STRING);
		stemmedQuery.setQueryFields(List.of("title"));
		stemmedQuery.setLimit(10L);
		stemmedQuery.setOffset(0L);

		// call under test
		SearchQueryResults stemmedResults = waitForSearch(stemmedQuery, columns, 1);
		assertTrue(stemmedResults.getTotalHits() >= 1L,
				"SCIENTIFIC default on `title` must stem 'papers' so 'paper' matches");
	}

	@Test
	public void testRoundTripWithAutocompleteBootstrappedAnalyzer() {
		// AUTOCOMPLETE is the bootstrapped analyzer for prefix-style typeahead. A column bound
		// to AUTOCOMPLETE (via override) must let an autocomplete() prefix query match docs
		// even after only a few characters of the indexed term. This is the only round-trip
		// that exercises the asymmetric default / default_search behavior end-to-end.
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
				List.of(overrideContainer), analyzers, null);
		openSearchManager.waitForIndexWritable(indexName);

		List<BulkOperation> operations = List.of(
				buildBulkOp(indexName, "1", Map.of("_row_id", 1L, "_row_version", 1L, "1", "mitochondria")),
				buildBulkOp(indexName, "2", Map.of("_row_id", 2L, "_row_version", 1L, "1", "genome")),
				buildBulkOp(indexName, "3", Map.of("_row_id", 3L, "_row_version", 1L, "1", "microbiome")));
		openSearchManager.bulkIndex(indexName, operations);

		// Autocomplete with prefix "mit" should match "mitochondria"; "microbiome" begins
		// with "mic", not "mit", so it must NOT match.
		SearchQuery query = new SearchQuery();
		query.setQueryText("mit");
		query.setLimit(8L);
		query.setOffset(0L);

		// call under test
		SearchQueryResults results = waitForAutocomplete(query, columns, 1);

		assertNotNull(results);
		assertTrue(results.getTotalHits() >= 1L,
				"Autocomplete must surface 'mitochondria' for prefix 'mit'");
		assertTrue(results.getHits().stream()
						.flatMap(h -> h.getFields().stream())
						.anyMatch(f -> "term".equals(f.getName()) && "mitochondria".equals(f.getValue())),
				"Autocomplete result must include the 'mitochondria' document");
	}

	@Test
	public void testSearchWithMatchAllQueryType() {
		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("name").setColumnType(ColumnType.STRING));
		openSearchManager.createIndex(indexName, columns, null,
				Collections.emptyList(), defaultAnalyzers, null);
		openSearchManager.waitForIndexWritable(indexName);

		List<BulkOperation> operations = List.of(
				buildBulkOp(indexName, "1", Map.of("_row_id", 1L, "_row_version", 1L, "1", "alpha")),
				buildBulkOp(indexName, "2", Map.of("_row_id", 2L, "_row_version", 1L, "1", "beta"))
		);
		openSearchManager.bulkIndex(indexName, operations);

		SearchQuery query = new SearchQuery();
		query.setQueryType(SearchQueryType.MATCH_ALL);
		query.setLimit(10L);
		query.setOffset(0L);

		// call under test
		SearchQueryResults results = waitForSearch(query, columns, 2);

		assertNotNull(results);
		assertEquals(2L, results.getTotalHits());
		assertNotNull(results.getHits());
		assertEquals(2, results.getHits().size());
	}

	@Test
	public void testSearchWithNonExistentIndex() {
		SearchQuery query = new SearchQuery();
		query.setQueryText("anything");
		query.setQueryType(SearchQueryType.SIMPLE_QUERY_STRING);
		query.setLimit(10L);
		query.setOffset(0L);

		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("name").setColumnType(ColumnType.STRING));

		// call under test
		IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
				openSearchManager.search("nonexistent-" + UUID.randomUUID(), query, columns,
						EnumSet.allOf(SearchQueryPart.class)));

		assertTrue(ex.getMessage().contains("still building"),
				"Exception message should indicate the index is not ready, got: " + ex.getMessage());
	}

	@Test
	public void testValidateAnalyzerSettingsWithInvalidTokenizer() {
		// Bare built-in tokenizer reference that AOSS doesn't recognize.
		IndexSettingsAnalysis settings = toAnalysis("{\"analyzer\":{\"default\":{\"type\":\"custom\","
				+ "\"tokenizer\":\"nonexistent_tokenizer_xyz\"}}}");

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> openSearchManager.validateAnalyzerSettings(settings));
		assertTrue(ex.getMessage().contains("Invalid analyzer configuration"),
				"Expected 'Invalid analyzer configuration' in message, got: " + ex.getMessage());
	}

	@Test
	public void testValidateAnalyzerSettingsWithInvalidFilter() {
		// Built-in tokenizer paired with a filter chain that names a nonexistent built-in filter.
		IndexSettingsAnalysis settings = toAnalysis("{\"analyzer\":{\"default\":{\"type\":\"custom\","
				+ "\"tokenizer\":\"standard\","
				+ "\"filter\":[\"bogus_filter_name_xyz\"]}}}");

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> openSearchManager.validateAnalyzerSettings(settings));
		assertTrue(ex.getMessage().contains("Invalid analyzer configuration"),
				"Expected 'Invalid analyzer configuration' in message, got: " + ex.getMessage());
	}

	@Test
	public void testValidateAnalyzerSettingsWithInlineFilterRegistry() {
		// Inline filter registry (my_stop) plus a built-in (lowercase). Exercises the typed
		// TokenFilterDefinition deserialize path against live AOSS.
		IndexSettingsAnalysis settings = toAnalysis("{"
				+ "\"filter\":{\"my_stop\":{\"type\":\"stop\",\"stopwords\":\"_english_\"}},"
				+ "\"analyzer\":{\"default\":{\"type\":\"custom\","
				+ "\"tokenizer\":\"standard\","
				+ "\"filter\":[\"my_stop\",\"lowercase\"]}}}");

		// call under test — must not throw
		openSearchManager.validateAnalyzerSettings(settings);
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
	 * The bootstrapped AUTOCOMPLETE analyzer combines word_delimiter (legacy, non-graph) with
	 * edge_ngram. Earlier the chain used word_delimiter_graph which produces multi-position
	 * graph tokens that edge_ngram (a non-graph filter) cannot consume — AOSS rejected every
	 * document during bulk index with a generic "Internal error". This test reproduces the
	 * production analyzer config end-to-end against AOSS and asserts that bulk index succeeds.
	 */
	@Test
	public void testBulkIndexWithAutocompleteAnalyzerOverride() {
		Map<String, IndexSettingsAnalysis> analyzers = new HashMap<>(defaultAnalyzers);
		analyzers.put("org.sagebionetworks-AUTOCOMPLETE",
				bootstrappedAnalyzerSettings(TextAnalyzerBootstrapper.AUTOCOMPLETE_ID));

		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("geneName").setColumnType(ColumnType.STRING));

		org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverrideEntry entry =
				new org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverrideEntry()
						.setColumnName("geneName")
						.setAnalyzer(Map.of("$ref", "org.sagebionetworks-AUTOCOMPLETE"));
		org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride override =
				new org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride()
						.setName("AUTOCOMPLETE_OVERRIDE")
						.setOrganizationName("org.sagebionetworks")
						.setOverrides(List.of(entry));

		openSearchManager.createIndex(indexName, columns, null,
				List.of(override), analyzers, null);
		openSearchManager.waitForIndexWritable(indexName);

		List<BulkOperation> operations = List.of(
				buildBulkOp(indexName, "1", Map.of("_row_id", 1L, "_row_version", 1L, "1", "BRCA1")),
				buildBulkOp(indexName, "2", Map.of("_row_id", 2L, "_row_version", 1L, "1", "BRCA2")),
				buildBulkOp(indexName, "3", Map.of("_row_id", 3L, "_row_version", 1L, "1", "TP53"))
		);

		// call under test — every document must be accepted; the previous word_delimiter_graph
		// + edge_ngram chain caused AOSS to reject all 3 here with "Internal error".
		long indexed = openSearchManager.bulkIndex(indexName, operations);

		assertEquals(3L, indexed);
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
				overrides, analyzers, null);
		openSearchManager.waitForIndexWritable(indexName);

		// Index one doc per synonym term so each query can match via synonym expansion at
		// search time regardless of which direction OpenSearch applies the rule internally.
		List<BulkOperation> operations = List.of(
				buildBulkOp(indexName, "1", Map.of("_row_id", 1L, "_row_version", 1L, "1", "cancer")),
				buildBulkOp(indexName, "2", Map.of("_row_id", 2L, "_row_version", 1L, "1", "tumor")),
				buildBulkOp(indexName, "3", Map.of("_row_id", 3L, "_row_version", 1L, "1", "neoplasm")));

		assertEquals(3L, openSearchManager.bulkIndex(indexName, operations));

		// Querying for any one term must match all three docs via the EQUIVALENT synonym rule.
		SearchQuery query = new SearchQuery();
		query.setQueryText("cancer");
		query.setQueryType(SearchQueryType.SIMPLE_QUERY_STRING);
		query.setLimit(10L);
		query.setOffset(0L);

		SearchQueryResults results = waitForSearch(query, columns, 3);
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
				overrides, analyzers, null);
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
		SearchQuery query = new SearchQuery();
		query.setQueryText("messenger-RNA");
		query.setQueryType(SearchQueryType.SIMPLE_QUERY_STRING);
		query.setLimit(10L);
		query.setOffset(0L);

		SearchQueryResults results = waitForSearch(query, columns, 3);
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
				overrides, analyzers, null);
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

	private static String descriptionOf(SearchQueryResults results) {
		return results.getHits().get(0).getFields().stream()
				.filter(f -> "description".equals(f.getName()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no 'description' field on hit"))
				.getValue();
	}

	private SearchQueryResults runQuery(SearchQueryType queryType, String text, List<ColumnModel> columns) {
		SearchQuery query = new SearchQuery();
		query.setQueryText(text);
		query.setQueryType(queryType);
		query.setLimit(10L);
		query.setOffset(0L);
		return waitForSearch(query, columns, 1L);
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
				Collections.emptyList(), defaultAnalyzers, null);
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

		// Request facets on every terms-aggregable column in the same call so the
		// numeric (lterms / dterms) and text (sterms) bucket-key paths all run
		// against live AOSS — regression for PLFM-9673 (lterms.keyAsString() is
		// null without an explicit `format`, so INTEGER facets came back with
		// null `value`).
		List<FacetRequest> facetRequests = columns.stream()
				.filter(c -> casesByType.get(c.getColumnType()).expectedFacetValues != null)
				.map(c -> new FacetRequest().setColumnName(c.getName()))
				.collect(Collectors.toList());

		SearchQuery query = new SearchQuery();
		query.setQueryType(SearchQueryType.MATCH_ALL);
		query.setLimit(10L);
		query.setOffset(0L);
		query.setFacetRequests(facetRequests);
		SearchQueryResults results = waitForSearch(query, columns, 1L);

		assertEquals(1L, results.getTotalHits());
		assertEquals(1, results.getHits().size());

		Map<String, String> idToName = columns.stream()
				.collect(Collectors.toMap(ColumnModel::getId, ColumnModel::getName));
		Map<String, String> returnedByName = new HashMap<>();
		for (SearchFieldValue fv : results.getHits().get(0).getFields()) {
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

		Map<String, FacetColumnResultValues> facetsByColumn = results.getFacets().stream()
				.collect(Collectors.toMap(FacetColumnResult::getColumnName,
						f -> (FacetColumnResultValues) f));
		for (ColumnModel column : columns) {
			ColumnType type = column.getColumnType();
			Set<String> expectedValues = casesByType.get(type).expectedFacetValues;
			if (expectedValues == null) {
				continue;
			}
			FacetColumnResultValues facet = facetsByColumn.get(column.getName());
			assertNotNull(facet, "missing facet result for " + type);
			Set<String> actualValues = facet.getFacetValues().stream()
					.map(FacetColumnResultValueCount::getValue)
					.collect(Collectors.toSet());
			assertEquals(expectedValues, actualValues,
					"facet bucket values for " + type + " must match (PLFM-9673: null on "
							+ "numeric types prior to fix)");
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
	private SearchQueryResults waitForSearch(SearchQuery query, List<ColumnModel> columns,
			long expectedMinHits) {
		SearchQueryResults[] result = {null};
		boolean success = TimeUtils.waitForExponential(POLL_MAX_MS, POLL_INTERVAL_MS, null, (v) -> {
			try {
				result[0] = openSearchManager.search(indexName, query, columns,
						EnumSet.allOf(SearchQueryPart.class));
				return result[0].getTotalHits() != null && result[0].getTotalHits() >= expectedMinHits;
			} catch (IllegalStateException e) {
				// index_not_found — not ready yet
				return false;
			}
		});
		assertTrue(success, "Timed out waiting for search results (expected at least " + expectedMinHits + " hits)");
		return result[0];
	}

	private SearchQueryResults waitForAutocomplete(SearchQuery query, List<ColumnModel> columns,
			long expectedMinHits) {
		SearchQueryResults[] result = {null};
		boolean success = TimeUtils.waitForExponential(POLL_MAX_MS, POLL_INTERVAL_MS, null, (v) -> {
			try {
				result[0] = openSearchManager.autocomplete(indexName, query, columns,
						EnumSet.allOf(SearchQueryPart.class));
				return result[0].getTotalHits() != null && result[0].getTotalHits() >= expectedMinHits;
			} catch (IllegalStateException e) {
				return false;
			}
		});
		assertTrue(success, "Timed out waiting for autocomplete results (expected at least " + expectedMinHits + " hits)");
		return result[0];
	}

	// ---- Test data helpers ----

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
}
