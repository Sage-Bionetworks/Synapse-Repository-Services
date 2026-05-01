package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.sagebionetworks.repo.model.search.SearchFieldValue;
import org.sagebionetworks.repo.model.search.SearchQuery;
import org.sagebionetworks.repo.model.search.SearchQueryPart;
import org.sagebionetworks.repo.model.search.SearchQueryResults;
import org.sagebionetworks.repo.model.search.SearchQueryType;
import org.sagebionetworks.repo.model.search.table.TextAnalyzer;
import org.sagebionetworks.repo.model.search.table.TextAnalyzerSettings;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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

	private String indexName;
	private Map<String, TextAnalyzer> defaultAnalyzers;

	@BeforeEach
	public void setUp() {
		assertNotNull(openSearchManager);
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
	}

	@Test
	public void testCreateIndexWithValidColumns() {
		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("name").setColumnType(ColumnType.STRING),
				new ColumnModel().setId("2").setName("age").setColumnType(ColumnType.INTEGER)
		);

		// call under test
		Optional<String> appliedConfig = openSearchManager.createIndex(indexName, columns, null,
				Collections.emptyList(), Collections.emptyList(), defaultAnalyzers);

		assertTrue(appliedConfig.isPresent());
		assertTrue(appliedConfig.get().length() > 0);
	}

	@Test
	public void testDeleteIndexWithExistingIndex() {
		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("name").setColumnType(ColumnType.STRING));
		openSearchManager.createIndex(indexName, columns, null,
				Collections.emptyList(), Collections.emptyList(), defaultAnalyzers);

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
				Collections.emptyList(), Collections.emptyList(), defaultAnalyzers);

		// call under test — resource_already_exists returns empty Optional
		Optional<String> result = openSearchManager.createIndex(indexName, columns, null,
				Collections.emptyList(), Collections.emptyList(), defaultAnalyzers);

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
				Collections.emptyList(), Collections.emptyList(), defaultAnalyzers);

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
	public void testSearchWithMatchAllQueryType() {
		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("name").setColumnType(ColumnType.STRING));
		openSearchManager.createIndex(indexName, columns, null,
				Collections.emptyList(), Collections.emptyList(), defaultAnalyzers);

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
						null, Collections.emptyList(), defaultAnalyzers, EnumSet.allOf(SearchQueryPart.class)));

		assertTrue(ex.getMessage().contains("still building"),
				"Exception message should indicate the index is not ready, got: " + ex.getMessage());
	}

	@Test
	public void testValidateAnalyzerSettingsWithInvalidTokenizer() {
		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("nonexistent_tokenizer_xyz");

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> openSearchManager.validateAnalyzerSettings(settings));
		assertTrue(ex.getMessage().contains("Invalid analyzer configuration"),
				"Expected 'Invalid analyzer configuration' in message, got: " + ex.getMessage());
	}

	@Test
	public void testValidateAnalyzerSettingsWithInvalidFilter() {
		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("standard");
		settings.setFilterOrder(Arrays.asList("bogus_filter_name_xyz"));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> openSearchManager.validateAnalyzerSettings(settings));
		assertTrue(ex.getMessage().contains("Invalid analyzer configuration"),
				"Expected 'Invalid analyzer configuration' in message, got: " + ex.getMessage());
	}

	@Test
	public void testValidateAnalyzerSettingsWithCustomFilters() {
		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("standard");
		settings.setTokenFilters("{\"my_stop\":{\"type\":\"stop\",\"stopwords\":\"_english_\"}}");
		settings.setFilterOrder(Arrays.asList("my_stop", "lowercase"));

		// call under test
		assertDoesNotThrow(() -> openSearchManager.validateAnalyzerSettings(settings));
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
		// Build the same analyzer settings the bootstrapper installs in production.
		TextAnalyzer autocomplete = autocompleteIndexAnalyzer();
		TextAnalyzer autocompleteSearch = autocompleteSearchAnalyzer();
		TextAnalyzer scientific = buildAnalyzer(TextAnalyzerBootstrapper.SCIENTIFIC_ID, "standard");

		Map<String, TextAnalyzer> analyzers = new HashMap<>();
		analyzers.put("org.sagebionetworks-AUTOCOMPLETE", autocomplete);
		analyzers.put("org.sagebionetworks-AUTOCOMPLETE_SEARCH", autocompleteSearch);
		analyzers.put("org.sagebionetworks-SCIENTIFIC", scientific);

		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("1").setName("geneName").setColumnType(ColumnType.STRING));

		org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverrideEntry entry =
				new org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverrideEntry();
		entry.setColumnName("geneName");
		entry.setIndexAnalyzer("org.sagebionetworks-AUTOCOMPLETE");
		entry.setSearchAnalyzer("org.sagebionetworks-AUTOCOMPLETE_SEARCH");

		org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride override =
				new org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride();
		override.setName("AUTOCOMPLETE_OVERRIDE");
		override.setOrganizationName("org.sagebionetworks");
		override.setOverrides(List.of(entry));

		openSearchManager.createIndex(indexName, columns, null,
				Collections.emptyList(), List.of(override), analyzers);

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
				Collections.emptyList(), Collections.emptyList(), defaultAnalyzers);

		Map<String, Object> doc = new HashMap<>();
		doc.put("_row_id", 1L);
		doc.put("_row_version", 1L);
		Map<String, Object> expectedById = new LinkedHashMap<>();
		for (ColumnModel column : columns) {
			ColumnType type = column.getColumnType();
			RoundTripCase rtc = casesByType.get(type);
			Object converted = SearchIndexLifecycleManagerImpl.convertForDocument(rtc.raw, type);
			assertEquals(rtc.expected, converted,
					"convertForDocument produced unexpected value for " + type);
			doc.put(column.getId(), converted);
			expectedById.put(column.getId(), converted);
		}

		// call under test
		long indexed = openSearchManager.bulkIndex(indexName, List.of(
				BulkOperation.of(op -> op.index(idx -> idx.index(indexName).id("1").document(doc)))));

		assertEquals(1L, indexed);

		SearchQuery query = new SearchQuery();
		query.setQueryType(SearchQueryType.MATCH_ALL);
		query.setLimit(10L);
		query.setOffset(0L);
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
			String fieldName = idToName.get(column.getId());
			String actual = returnedByName.get(fieldName);
			assertNotNull(actual, "missing returned value for " + column.getColumnType());
			assertEquals(String.valueOf(expectedById.get(column.getId())), actual,
					"round-trip mismatch for " + column.getColumnType());
		}
	}

	private static Map<ColumnType, RoundTripCase> buildEveryColumnTypeCase() {
		Map<ColumnType, RoundTripCase> casesByType = new LinkedHashMap<>();
		casesByType.put(ColumnType.STRING,        new RoundTripCase("alpha",                              "alpha"));
		casesByType.put(ColumnType.STRING_LIST,   new RoundTripCase("[\"alpha\",\"beta\"]",               List.of("alpha", "beta")));
		casesByType.put(ColumnType.MEDIUMTEXT,    new RoundTripCase("alpha beta gamma",                   "alpha beta gamma"));
		casesByType.put(ColumnType.LARGETEXT,     new RoundTripCase("alpha beta gamma",                   "alpha beta gamma"));
		casesByType.put(ColumnType.LINK,          new RoundTripCase("https://example.org/a",              "https://example.org/a"));
		casesByType.put(ColumnType.INTEGER,       new RoundTripCase("123",                                123));
		casesByType.put(ColumnType.INTEGER_LIST,  new RoundTripCase("[1,2,3]",                            List.of(1, 2, 3)));
		casesByType.put(ColumnType.DATE,          new RoundTripCase("1609459200000",                      1609459200000L));
		casesByType.put(ColumnType.DATE_LIST,     new RoundTripCase("[1609459200000,1609545600000]",      List.of(1609459200000L, 1609545600000L)));
		casesByType.put(ColumnType.FILEHANDLEID,  new RoundTripCase("9876543",                            9876543));
		casesByType.put(ColumnType.SUBMISSIONID,  new RoundTripCase("555",                                555));
		casesByType.put(ColumnType.EVALUATIONID,  new RoundTripCase("777",                                777));
		casesByType.put(ColumnType.ENTITYID,      new RoundTripCase("syn123456",                          "syn123456"));
		casesByType.put(ColumnType.USERID,        new RoundTripCase("3412396",                            "3412396"));
		casesByType.put(ColumnType.ENTITYID_LIST, new RoundTripCase("[\"syn1\",\"syn2\"]",                List.of("syn1", "syn2")));
		casesByType.put(ColumnType.USERID_LIST,   new RoundTripCase("[\"100\",\"200\"]",                  List.of("100", "200")));
		casesByType.put(ColumnType.DOUBLE,        new RoundTripCase("1.5",                                1.5));
		casesByType.put(ColumnType.BOOLEAN,       new RoundTripCase("true",                               Boolean.TRUE));
		casesByType.put(ColumnType.BOOLEAN_LIST,  new RoundTripCase("[true,false]",                       List.of(true, false)));
		casesByType.put(ColumnType.JSON,          new RoundTripCase("{\"a\":1,\"b\":\"x\"}",              Map.of("a", 1, "b", "x")));
		return casesByType;
	}

	private static final class RoundTripCase {
		final String raw;
		final Object expected;
		RoundTripCase(String raw, Object expected) {
			this.raw = raw;
			this.expected = expected;
		}
	}

	private static TextAnalyzer autocompleteIndexAnalyzer() {
		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("standard");
		settings.setTokenFilters("{"
				+ "\"ac_word_delimiter\":{\"type\":\"word_delimiter\",\"preserve_original\":true,"
				+ "\"split_on_case_change\":true,\"split_on_numerics\":true,"
				+ "\"catenate_words\":true,\"catenate_numbers\":false,"
				+ "\"stem_english_possessive\":true},"
				+ "\"edge_ngram_filter\":{\"type\":\"edge_ngram\",\"min_gram\":2,\"max_gram\":20}"
				+ "}");
		settings.setFilterOrder(Arrays.asList("ac_word_delimiter", "lowercase", "edge_ngram_filter"));

		TextAnalyzer analyzer = new TextAnalyzer();
		analyzer.setId(Long.toString(TextAnalyzerBootstrapper.AUTOCOMPLETE_ID));
		analyzer.setSettings(settings);
		return analyzer;
	}

	private static TextAnalyzer autocompleteSearchAnalyzer() {
		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("standard");
		settings.setTokenFilters("{"
				+ "\"acs_word_delimiter\":{\"type\":\"word_delimiter_graph\",\"preserve_original\":true,"
				+ "\"split_on_case_change\":true,\"split_on_numerics\":true,"
				+ "\"catenate_words\":true,\"catenate_numbers\":false,"
				+ "\"stem_english_possessive\":true}"
				+ "}");
		settings.setFilterOrder(Arrays.asList("acs_word_delimiter", "lowercase"));

		TextAnalyzer analyzer = new TextAnalyzer();
		analyzer.setId(Long.toString(TextAnalyzerBootstrapper.AUTOCOMPLETE_SEARCH_ID));
		analyzer.setSettings(settings);
		return analyzer;
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
						null, Collections.emptyList(), defaultAnalyzers, EnumSet.allOf(SearchQueryPart.class));
				return result[0].getTotalHits() != null && result[0].getTotalHits() >= expectedMinHits;
			} catch (IllegalStateException e) {
				// index_not_found — not ready yet
				return false;
			}
		});
		assertTrue(success, "Timed out waiting for search results (expected at least " + expectedMinHits + " hits)");
		return result[0];
	}

	// ---- Test data helpers ----

	private static Map<String, TextAnalyzer> buildDefaultAnalyzers() {
		Map<String, TextAnalyzer> analyzers = new HashMap<>();
		analyzers.put("org.sagebionetworks-SCIENTIFIC", buildAnalyzer(TextAnalyzerBootstrapper.SCIENTIFIC_ID, "standard"));
		analyzers.put("org.sagebionetworks-KEYWORD", buildAnalyzer(TextAnalyzerBootstrapper.KEYWORD_ID, "keyword"));
		analyzers.put("org.sagebionetworks-STANDARD", buildAnalyzer(TextAnalyzerBootstrapper.STANDARD_ID, "standard"));
		return analyzers;
	}

	private static TextAnalyzer buildAnalyzer(Long id, String tokenizer) {
		TextAnalyzer analyzer = new TextAnalyzer();
		analyzer.setId(id.toString());
		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer(tokenizer);
		settings.setFilterOrder(Collections.singletonList("lowercase"));
		analyzer.setSettings(settings);
		return analyzer;
	}

	private static BulkOperation buildBulkOp(String indexName, String docId, Map<String, Object> doc) {
		return BulkOperation.of(op -> op
				.index(idx -> idx
						.index(indexName)
						.id(docId)
						.document(doc)));
	}
}
