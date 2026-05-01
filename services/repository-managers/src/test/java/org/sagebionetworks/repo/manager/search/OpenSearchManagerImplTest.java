package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.FieldSort;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TextQueryType;
import org.opensearch.client.opensearch.core.search.HighlightField;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.sagebionetworks.repo.model.search.FacetRequest;
import org.sagebionetworks.repo.model.search.FacetSortField;
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
import org.sagebionetworks.repo.model.table.FacetColumnResultValueCount;
import org.sagebionetworks.repo.model.table.FacetColumnResultValues;
import org.sagebionetworks.repo.model.table.FacetType;

/**
 * Direct unit tests for the package-protected helpers on {@link OpenSearchManagerImpl}.
 * The helpers were widened from {@code private} to package-private so they can be exercised
 * per-branch here — verifying behavior on each branch directly rather than only transitively
 * through {@code search()} / {@code autocomplete()} (which is the concern of the AutoWired IT).
 */
@ExtendWith(MockitoExtension.class)
public class OpenSearchManagerImplTest {

	@Mock
	private OpenSearchClient openSearchClient;
	@Mock
	private OpenSearchIndicesClient indicesClient;

	@InjectMocks
	private OpenSearchManagerImpl manager;

	/** Scientific analyzer (id=1) used by STRING/LARGETEXT column types as the platform default. */
	private TextAnalyzer scientific;
	private String scientificQualifiedName;

	@BeforeEach
	public void setUp() {
		scientificQualifiedName = "org.sagebionetworks-SCIENTIFIC";
		scientific = new TextAnalyzer().setId("1")
				.setSettings(new TextAnalyzerSettings().setTokenizer("standard"));
	}

	private TextAnalyzer keywordAnalyzer(String id) {
		return new TextAnalyzer().setId(id)
				.setSettings(new TextAnalyzerSettings().setTokenizer("keyword"));
	}

	private Map<String, TextAnalyzer> scientificAnalyzerMap() {
		Map<String, TextAnalyzer> map = new HashMap<>();
		map.put(scientificQualifiedName, scientific);
		return map;
	}

	private Map<Long, String> scientificIdToQualifiedName() {
		Map<Long, String> map = new HashMap<>();
		map.put(1L, scientificQualifiedName);
		return map;
	}

	// --- stripBoost ---

	@ParameterizedTest(name = "stripBoost(''{0}'') = ''{1}''")
	@CsvSource({
			"geneName^2,   geneName",   // trailing boost suffix is removed
			"geneName,     geneName",   // no boost — passthrough
			"^foo,         ^foo",       // leading caret preserved (split only when caretIndex > 0)
			"a^1^2,        a"           // first caret wins (substring(0, caretIndex))
	})
	public void testStripBoost(String input, String expected) {
		// call under test
		assertEquals(expected, manager.stripBoost(input));
	}

	// --- toLong ---

	@ParameterizedTest(name = "toLong({0}) = {1}")
	@MethodSource("toLongProvider")
	public void testToLong(Object input, Long expected) {
		// call under test
		assertEquals(expected, manager.toLong(input));
	}

	static Stream<Arguments> toLongProvider() {
		return Stream.of(
				Arguments.of(Integer.valueOf(42), Long.valueOf(42)),         // Number branch — Integer
				Arguments.of(Long.valueOf(42), Long.valueOf(42)),            // Number branch — Long
				Arguments.of("42", Long.valueOf(42)),                        // String branch — parseable
				Arguments.of("not-a-number", null),                          // String branch — NumberFormatException → null
				Arguments.of(null, null));                                   // String branch — String.valueOf(null) = "null" → NFE → null
	}

	// --- isKeywordAnalyzer ---

	@ParameterizedTest(name = "{0}")
	@MethodSource("isKeywordAnalyzerProvider")
	public void testIsKeywordAnalyzer(String description, TextAnalyzer analyzer, boolean expected) {
		// call under test
		assertEquals(expected, manager.isKeywordAnalyzer(analyzer));
	}

	static Stream<Arguments> isKeywordAnalyzerProvider() {
		return Stream.of(
				Arguments.of("keyword tokenizer → true",
						new TextAnalyzer().setId("9").setSettings(new TextAnalyzerSettings().setTokenizer("keyword")), true),
				Arguments.of("standard tokenizer → false",
						new TextAnalyzer().setId("1").setSettings(new TextAnalyzerSettings().setTokenizer("standard")), false),
				Arguments.of("null analyzer → false",
						null, false),
				Arguments.of("analyzer with null settings → false",
						new TextAnalyzer().setId("9"), false));
	}

	// --- buildSynonymRules ---

	@ParameterizedTest(name = "buildSynonymRules({0}) → empty")
	@MethodSource("buildSynonymRulesEmptyProvider")
	public void testBuildSynonymRulesWithNullOrEmpty(String description, List<SynonymSet> input) {
		// call under test
		assertEquals(Collections.emptyList(), manager.buildSynonymRules(input));
	}

	static Stream<Arguments> buildSynonymRulesEmptyProvider() {
		return Stream.of(
				Arguments.of("null",  null),
				Arguments.of("empty", Collections.emptyList()));
	}

	@Test
	public void testBuildSynonymRulesWithEquivalentRule() {
		SynonymSet set = new SynonymSet().setRules(Collections.singletonList(
				new SynonymRule().setRuleType(SynonymRuleType.EQUIVALENT)
						.setTerms(Arrays.asList("cancer", "tumor", "neoplasm"))));

		// call under test
		List<String> rules = manager.buildSynonymRules(Collections.singletonList(set));

		assertEquals(Collections.singletonList("cancer, tumor, neoplasm"), rules);
	}

	@Test
	public void testBuildSynonymRulesWithExplicitRule() {
		SynonymSet set = new SynonymSet().setRules(Collections.singletonList(
				new SynonymRule().setRuleType(SynonymRuleType.EXPLICIT)
						.setTerms(Arrays.asList("AD", "Alzheimer's disease"))));

		// call under test
		List<String> rules = manager.buildSynonymRules(Collections.singletonList(set));

		assertEquals(Collections.singletonList("AD => Alzheimer's disease"), rules);
	}

	@Test
	public void testBuildSynonymRulesSkipsRulesWithFewerThanTwoTerms() {
		SynonymSet set = new SynonymSet().setRules(Arrays.asList(
				new SynonymRule().setRuleType(SynonymRuleType.EQUIVALENT)
						.setTerms(Collections.singletonList("loneTerm")),
				new SynonymRule().setRuleType(SynonymRuleType.EQUIVALENT)
						.setTerms(null),
				new SynonymRule().setRuleType(SynonymRuleType.EQUIVALENT)
						.setTerms(Arrays.asList("a", "b"))));

		// call under test — only the two-term rule survives
		List<String> rules = manager.buildSynonymRules(Collections.singletonList(set));

		assertEquals(Collections.singletonList("a, b"), rules);
	}

	@Test
	public void testBuildSynonymRulesSkipsSetsWithNullRules() {
		// call under test — a SynonymSet with null rules list is silently skipped
		List<String> rules = manager.buildSynonymRules(Collections.singletonList(new SynonymSet()));

		assertEquals(Collections.emptyList(), rules);
	}

	// --- buildOverrideMap ---

	@Test
	public void testBuildOverrideMapWithNullReturnsEmpty() {
		// call under test
		assertTrue(manager.buildOverrideMap(null, Collections.emptyMap()).isEmpty());
	}

	@Test
	public void testBuildOverrideMapResolvesNameToId() {
		ColumnAnalyzerOverrideEntry entry = new ColumnAnalyzerOverrideEntry()
				.setColumnName("geneName")
				.setIndexAnalyzer("org.sage-AUTOCOMPLETE");
		ColumnAnalyzerOverride override = new ColumnAnalyzerOverride()
				.setOverrides(Collections.singletonList(entry));
		Map<String, String> nameToId = new HashMap<>();
		nameToId.put("geneName", "111");

		// call under test — "geneName" resolves to id "111"
		Map<String, ColumnAnalyzerOverrideEntry> map = manager.buildOverrideMap(
				Collections.singletonList(override), nameToId);

		assertEquals(1, map.size());
		assertEquals(entry, map.get("111"));
	}

	@Test
	public void testBuildOverrideMapSkipsUnknownColumnName() {
		ColumnAnalyzerOverrideEntry entry = new ColumnAnalyzerOverrideEntry()
				.setColumnName("unknownColumn")
				.setIndexAnalyzer("org.sage-AUTOCOMPLETE");
		ColumnAnalyzerOverride override = new ColumnAnalyzerOverride()
				.setOverrides(Collections.singletonList(entry));

		// call under test — "unknownColumn" is not in nameToId, so the entry is silently dropped
		Map<String, ColumnAnalyzerOverrideEntry> map = manager.buildOverrideMap(
				Collections.singletonList(override), Collections.emptyMap());

		assertTrue(map.isEmpty());
	}

	@Test
	public void testBuildOverrideMapFirstEntryWinsOnDuplicate() {
		// Two overrides targeting the same column — the first one wins (putIfAbsent)
		ColumnAnalyzerOverrideEntry first = new ColumnAnalyzerOverrideEntry()
				.setColumnName("geneName").setIndexAnalyzer("FIRST");
		ColumnAnalyzerOverrideEntry second = new ColumnAnalyzerOverrideEntry()
				.setColumnName("geneName").setIndexAnalyzer("SECOND");
		Map<String, String> nameToId = new HashMap<>();
		nameToId.put("geneName", "111");

		// call under test
		Map<String, ColumnAnalyzerOverrideEntry> map = manager.buildOverrideMap(
				Arrays.asList(
						new ColumnAnalyzerOverride().setOverrides(Collections.singletonList(first)),
						new ColumnAnalyzerOverride().setOverrides(Collections.singletonList(second))),
				nameToId);

		assertEquals(first, map.get("111"));
	}

	@Test
	public void testBuildOverrideMapSkipsOverridesWithNullEntries() {
		ColumnAnalyzerOverride override = new ColumnAnalyzerOverride();
		// overrides list is null

		// call under test
		Map<String, ColumnAnalyzerOverrideEntry> map = manager.buildOverrideMap(
				Collections.singletonList(override), Collections.emptyMap());

		assertTrue(map.isEmpty());
	}

	// --- buildIdToQualifiedNameMap (static) ---

	@Test
	public void testBuildIdToQualifiedNameMapWithSingleEntry() {
		Map<String, TextAnalyzer> analyzers = new HashMap<>();
		analyzers.put("org.sage-ONE", new TextAnalyzer().setId("1"));
		analyzers.put("org.sage-TWO", new TextAnalyzer().setId("2"));

		// call under test
		Map<Long, String> idToQualified = OpenSearchManagerImpl.buildIdToQualifiedNameMap(analyzers);

		assertEquals("org.sage-ONE", idToQualified.get(1L));
		assertEquals("org.sage-TWO", idToQualified.get(2L));
	}

	@Test
	public void testBuildIdToQualifiedNameMapWithEmptyInput() {
		// call under test
		assertTrue(OpenSearchManagerImpl.buildIdToQualifiedNameMap(Collections.emptyMap()).isEmpty());
	}

	// --- resolveEffectiveAnalyzerName ---

	@Test
	public void testResolveEffectiveAnalyzerNameWithOverride() {
		Map<String, ColumnAnalyzerOverrideEntry> overrides = new HashMap<>();
		overrides.put("111", new ColumnAnalyzerOverrideEntry().setIndexAnalyzer("org.sage-CUSTOM"));

		// call under test — override wins over defaultAnalyzer and column-type default
		String name = manager.resolveEffectiveAnalyzerName(
				"111", ColumnType.STRING, "org.sage-DEFAULT", overrides, scientificIdToQualifiedName());

		assertEquals("org.sage-CUSTOM", name);
	}

	@Test
	public void testResolveEffectiveAnalyzerNameFallsBackToDefault() {
		// call under test — no override; defaultAnalyzer wins
		String name = manager.resolveEffectiveAnalyzerName(
				"111", ColumnType.STRING, "org.sage-DEFAULT",
				Collections.emptyMap(), scientificIdToQualifiedName());

		assertEquals("org.sage-DEFAULT", name);
	}

	@Test
	public void testResolveEffectiveAnalyzerNameFallsBackToColumnTypeDefault() {
		// call under test — no override, no defaultAnalyzer; column type default (SCIENTIFIC for STRING) wins
		String name = manager.resolveEffectiveAnalyzerName(
				"111", ColumnType.STRING, null,
				Collections.emptyMap(), scientificIdToQualifiedName());

		assertEquals(scientificQualifiedName, name);
	}

	// --- resolveIndexAnalyzerName / resolveSearchAnalyzerName ---

	@Test
	public void testResolveIndexAnalyzerNameWithEntry() {
		Map<String, TextAnalyzer> analyzers = new HashMap<>();
		TextAnalyzer custom = new TextAnalyzer().setId("99")
				.setSettings(new TextAnalyzerSettings().setTokenizer("standard"));
		analyzers.put("org.sage-CUSTOM", custom);
		ColumnAnalyzerOverrideEntry entry = new ColumnAnalyzerOverrideEntry().setIndexAnalyzer("org.sage-CUSTOM");

		// call under test — entry.indexAnalyzer wins over effectiveAnalyzer
		String name = manager.resolveIndexAnalyzerName(scientific, entry, analyzers);

		assertEquals("synapse_analyzer_99", name);
	}

	@Test
	public void testResolveIndexAnalyzerNameWithNullEntry() {
		// call under test — null entry → effectiveAnalyzer prefix
		String name = manager.resolveIndexAnalyzerName(scientific, null, scientificAnalyzerMap());

		assertEquals("synapse_analyzer_1", name);
	}

	@Test
	public void testResolveSearchAnalyzerNameWithEntry() {
		Map<String, TextAnalyzer> analyzers = new HashMap<>();
		analyzers.put("org.sage-SEARCH",
				new TextAnalyzer().setId("42").setSettings(new TextAnalyzerSettings().setTokenizer("standard")));
		ColumnAnalyzerOverrideEntry entry = new ColumnAnalyzerOverrideEntry().setSearchAnalyzer("org.sage-SEARCH");

		// call under test — entry.searchAnalyzer wins over indexAnalyzerName
		assertEquals("synapse_analyzer_42",
				manager.resolveSearchAnalyzerName("synapse_analyzer_99", entry, analyzers));
	}

	@Test
	public void testResolveSearchAnalyzerNameFallsBackToIndexAnalyzer() {
		// call under test — no entry → indexAnalyzerName is returned unchanged
		assertEquals("synapse_analyzer_99",
				manager.resolveSearchAnalyzerName("synapse_analyzer_99", null, Collections.emptyMap()));
	}

	// --- getFilterFieldName ---

	@Test
	public void testGetFilterFieldNameForUnknownColumnReturnsId() {
		// call under test — id not in columnMap, return id unchanged
		assertEquals("999",
				manager.getFilterFieldName("999", Collections.emptyMap(), null,
						Collections.emptyMap(), Collections.emptyMap(), scientificIdToQualifiedName()));
	}

	@Test
	public void testGetFilterFieldNameForTextColumnAppendsKeyword() {
		Map<String, ColumnModel> columnMap = new HashMap<>();
		columnMap.put("111",
				new ColumnModel().setId("111").setName("name").setColumnType(ColumnType.STRING));

		// call under test — text types route to the `.keyword` sub-field for filtering
		assertEquals("111.keyword",
				manager.getFilterFieldName("111", columnMap, null,
						Collections.emptyMap(), Collections.emptyMap(), scientificIdToQualifiedName()));
	}

	@Test
	public void testGetFilterFieldNameForLinkColumnWithKeywordAnalyzerReturnsId() {
		Map<String, ColumnModel> columnMap = new HashMap<>();
		columnMap.put("222",
				new ColumnModel().setId("222").setName("link").setColumnType(ColumnType.LINK));
		TextAnalyzer keyword = keywordAnalyzer("99");
		Map<String, ColumnAnalyzerOverrideEntry> overrideMap = new HashMap<>();
		overrideMap.put("222", new ColumnAnalyzerOverrideEntry().setIndexAnalyzer("org.sage-KEYWORD"));
		Map<String, TextAnalyzer> analyzers = new HashMap<>();
		analyzers.put("org.sage-KEYWORD", keyword);

		// call under test — link column whose override resolves to a keyword analyzer returns just the id
		assertEquals("222",
				manager.getFilterFieldName("222", columnMap, null, overrideMap, analyzers, scientificIdToQualifiedName()));
	}

	@Test
	public void testGetFilterFieldNameForNumericColumnReturnsId() {
		Map<String, ColumnModel> columnMap = new HashMap<>();
		columnMap.put("333",
				new ColumnModel().setId("333").setName("age").setColumnType(ColumnType.INTEGER));

		// call under test — numeric columns don't need a sub-field; id returned as-is
		assertEquals("333",
				manager.getFilterFieldName("333", columnMap, null,
						Collections.emptyMap(), Collections.emptyMap(), scientificIdToQualifiedName()));
	}

	// --- getSearchFieldName ---

	@Test
	public void testGetSearchFieldNameForUnknownColumnReturnsId() {
		// call under test
		assertEquals("999",
				manager.getSearchFieldName("999", Collections.emptyMap(), null,
						Collections.emptyMap(), Collections.emptyMap(), scientificIdToQualifiedName()));
	}

	@Test
	public void testGetSearchFieldNameForTextColumnReturnsId() {
		Map<String, ColumnModel> columnMap = new HashMap<>();
		columnMap.put("111",
				new ColumnModel().setId("111").setName("name").setColumnType(ColumnType.STRING));

		// call under test — text columns route to the analyzer-backed primary field for searching
		assertEquals("111",
				manager.getSearchFieldName("111", columnMap, null,
						Collections.emptyMap(), Collections.emptyMap(), scientificIdToQualifiedName()));
	}

	@Test
	public void testGetSearchFieldNameForLinkColumnWithKeywordAnalyzerAppendsSearchable() {
		Map<String, ColumnModel> columnMap = new HashMap<>();
		columnMap.put("222",
				new ColumnModel().setId("222").setName("link").setColumnType(ColumnType.LINK));
		TextAnalyzer keyword = keywordAnalyzer("99");
		Map<String, ColumnAnalyzerOverrideEntry> overrideMap = new HashMap<>();
		overrideMap.put("222", new ColumnAnalyzerOverrideEntry().setIndexAnalyzer("org.sage-KEYWORD"));
		Map<String, TextAnalyzer> analyzers = new HashMap<>();
		analyzers.put("org.sage-KEYWORD", keyword);

		// call under test — link column under a keyword analyzer searches against the ".searchable" sub-field
		assertEquals("222.searchable",
				manager.getSearchFieldName("222", columnMap, null, overrideMap, analyzers, scientificIdToQualifiedName()));
	}

	// --- resolveQueryFields ---

	@ParameterizedTest(name = "resolveQueryFields({0}) = null")
	@MethodSource("resolveQueryFieldsNullProvider")
	public void testResolveQueryFieldsWithNullOrEmpty(String description, List<String> input) {
		// call under test — null/empty input is a signal to let OpenSearch use its default fields
		assertNull(manager.resolveQueryFields(input, Collections.emptyList(),
				null, Collections.emptyMap(), Collections.emptyMap(), scientificIdToQualifiedName(), true));
	}

	static Stream<Arguments> resolveQueryFieldsNullProvider() {
		return Stream.of(
				Arguments.of("null",  null),
				Arguments.of("empty", Collections.emptyList()));
	}

	@Test
	public void testResolveQueryFieldsTranslatesNamesToIds() {
		List<ColumnModel> columns = Arrays.asList(
				new ColumnModel().setId("111").setName("geneName").setColumnType(ColumnType.STRING));

		// call under test — "geneName" translates to id "111"
		List<String> fields = manager.resolveQueryFields(
				Collections.singletonList("geneName"), columns, null,
				Collections.emptyMap(), Collections.emptyMap(), scientificIdToQualifiedName(), true);

		assertEquals(Collections.singletonList("111"), fields);
	}

	@Test
	public void testResolveQueryFieldsPreservesBoostSuffix() {
		List<ColumnModel> columns = Arrays.asList(
				new ColumnModel().setId("111").setName("geneName").setColumnType(ColumnType.STRING));

		// call under test — "geneName^3" → "111^3"
		List<String> fields = manager.resolveQueryFields(
				Collections.singletonList("geneName^3"), columns, null,
				Collections.emptyMap(), Collections.emptyMap(), scientificIdToQualifiedName(), true);

		assertEquals(Collections.singletonList("111^3"), fields);
	}

	// --- buildMainQuery (per SearchQueryType arm) ---

	@Test
	public void testBuildMainQueryWithSimpleQueryString() {
		// call under test
		Query q = manager.buildMainQuery(SearchQueryType.SIMPLE_QUERY_STRING, "alice",
				Arrays.asList("111", "222"), null);

		assertTrue(q.isSimpleQueryString());
		assertEquals("alice", q.simpleQueryString().query());
		assertEquals(Arrays.asList("111", "222"), q.simpleQueryString().fields());
	}

	@Test
	public void testBuildMainQueryWithMatch() {
		// call under test
		Query q = manager.buildMainQuery(SearchQueryType.MATCH, "alice",
				Collections.singletonList("111^3"), null);

		assertTrue(q.isMatch());
		// stripBoost applied → the match field is just "111" without the caret
		assertEquals("111", q.match().field());
	}

	@Test
	public void testBuildMainQueryWithMatchAndFuzziness() {
		// call under test
		Query q = manager.buildMainQuery(SearchQueryType.MATCH, "alice",
				Collections.singletonList("111"), "AUTO");

		assertTrue(q.isMatch());
		assertEquals("AUTO", q.match().fuzziness());
	}

	/**
	 * MATCH, MATCH_PHRASE, and WILDCARD all call {@code stripBoost(fields.get(0))}, so they
	 * each require a non-empty {@code fields} list. The other query types tolerate null/empty
	 * fields and therefore aren't included in this parameterized test.
	 */
	@ParameterizedTest(name = "buildMainQuery({0}, null fields) → IllegalArgumentException")
	@EnumSource(value = SearchQueryType.class, names = {"MATCH", "MATCH_PHRASE", "WILDCARD"})
	public void testBuildMainQueryRequiresFields(SearchQueryType type) {
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> manager.buildMainQuery(type, "alice", null, null));
	}

	@Test
	public void testBuildMainQueryWithMultiMatch() {
		// call under test
		Query q = manager.buildMainQuery(SearchQueryType.MULTI_MATCH, "alice",
				Arrays.asList("111", "222"), "AUTO");

		assertTrue(q.isMultiMatch());
		assertEquals("alice", q.multiMatch().query());
		assertEquals(Arrays.asList("111", "222"), q.multiMatch().fields());
		assertEquals("AUTO", q.multiMatch().fuzziness());
	}

	@Test
	public void testBuildMainQueryWithMatchPhrase() {
		// call under test
		Query q = manager.buildMainQuery(SearchQueryType.MATCH_PHRASE, "alice smith",
				Collections.singletonList("111"), null);

		assertTrue(q.isMatchPhrase());
		assertEquals("111", q.matchPhrase().field());
	}

	@Test
	public void testBuildMainQueryWithPrefixRoutesToBoolPrefix() {
		// PREFIX uses multi_match with TextQueryType.BoolPrefix under the hood
		// call under test
		Query q = manager.buildMainQuery(SearchQueryType.PREFIX, "alic",
				Arrays.asList("111", "222"), null);

		assertTrue(q.isMultiMatch());
		assertEquals(TextQueryType.BoolPrefix, q.multiMatch().type());
	}

	@Test
	public void testBuildMainQueryWithWildcard() {
		// call under test
		Query q = manager.buildMainQuery(SearchQueryType.WILDCARD, "al*",
				Collections.singletonList("111^2"), null);

		assertTrue(q.isWildcard());
		// stripBoost applied
		assertEquals("111", q.wildcard().field());
	}

	@Test
	public void testBuildMainQueryWithMatchAll() {
		// call under test
		Query q = manager.buildMainQuery(SearchQueryType.MATCH_ALL, null,
				null, null);

		assertTrue(q.isMatchAll());
	}

	// --- buildSortOptions ---

	@Test
	public void testBuildSortOptionsWithNullReturnsScoreDesc() {
		// When no sort fields are specified, default to _score DESC.
		// call under test
		List<SortOptions> sorted = manager.buildSortOptions(null,
				Collections.emptyMap(), Collections.emptyMap(), null,
				Collections.emptyMap(), Collections.emptyMap(), scientificIdToQualifiedName());

		assertEquals(1, sorted.size());
		FieldSort fs = sorted.get(0).field();
		assertEquals("_score", fs.field());
		assertEquals(SortOrder.Desc, fs.order());
	}

	@Test
	public void testBuildSortOptionsPreservesScoreFieldNameUntouched() {
		// _score must NOT be translated through nameToId — it's a pseudo-field
		SortField sf = new SortField().setColumnName("_score").setDirection(SortDirection.DESC);

		// call under test
		List<SortOptions> sorted = manager.buildSortOptions(Collections.singletonList(sf),
				Collections.emptyMap(), Collections.emptyMap(), null,
				Collections.emptyMap(), Collections.emptyMap(), scientificIdToQualifiedName());

		assertEquals("_score", sorted.get(0).field().field());
	}

	@Test
	public void testBuildSortOptionsTranslatesColumnNameToFilterFieldName() {
		SortField sf = new SortField().setColumnName("name").setDirection(SortDirection.ASC);
		Map<String, ColumnModel> columnMap = new HashMap<>();
		columnMap.put("111",
				new ColumnModel().setId("111").setName("name").setColumnType(ColumnType.STRING));
		Map<String, String> nameToId = new HashMap<>();
		nameToId.put("name", "111");

		// call under test — STRING column routes to id.keyword for sorting
		List<SortOptions> sorted = manager.buildSortOptions(Collections.singletonList(sf),
				columnMap, nameToId, null,
				Collections.emptyMap(), Collections.emptyMap(), scientificIdToQualifiedName());

		assertEquals("111.keyword", sorted.get(0).field().field());
		assertEquals(SortOrder.Asc, sorted.get(0).field().order());
	}

	// --- buildHighlightFields ---

	@Test
	public void testBuildHighlightFieldsSkipsNonTextNonLinkColumns() {
		List<ColumnModel> columns = Arrays.asList(
				new ColumnModel().setId("111").setName("name").setColumnType(ColumnType.STRING),
				new ColumnModel().setId("222").setName("age").setColumnType(ColumnType.INTEGER),
				new ColumnModel().setId("333").setName("flag").setColumnType(ColumnType.BOOLEAN));

		// call under test
		Map<String, HighlightField> fields = manager.buildHighlightFields(columns, null,
				Collections.emptyMap(), scientificAnalyzerMap(), scientificIdToQualifiedName());

		assertEquals(Collections.singleton("111"), fields.keySet());
	}

	@Test
	public void testBuildHighlightFieldsUsesSearchableSubFieldForKeywordLinkColumn() {
		List<ColumnModel> columns = Collections.singletonList(
				new ColumnModel().setId("222").setName("link").setColumnType(ColumnType.LINK));
		TextAnalyzer keyword = keywordAnalyzer("99");
		Map<String, ColumnAnalyzerOverrideEntry> overrideMap = new HashMap<>();
		overrideMap.put("222", new ColumnAnalyzerOverrideEntry().setIndexAnalyzer("org.sage-KEYWORD"));
		Map<String, TextAnalyzer> analyzers = new HashMap<>();
		analyzers.put("org.sage-KEYWORD", keyword);

		// call under test
		Map<String, HighlightField> fields = manager.buildHighlightFields(columns, null,
				overrideMap, analyzers, scientificIdToQualifiedName());

		assertEquals(Collections.singleton("222.searchable"), fields.keySet());
	}

	// --- buildAggregations ---

	@ParameterizedTest(name = "buildAggregations({0}) → empty")
	@MethodSource("buildAggregationsEmptyProvider")
	public void testBuildAggregationsWithNullOrEmpty(String description, List<FacetRequest> input) {
		// call under test
		assertTrue(manager.buildAggregations(input,
				Collections.emptyMap(), Collections.emptyMap(), null,
				Collections.emptyMap(), Collections.emptyMap(), scientificIdToQualifiedName()).isEmpty());
	}

	static Stream<Arguments> buildAggregationsEmptyProvider() {
		return Stream.of(
				Arguments.of("null",  null),
				Arguments.of("empty", Collections.emptyList()));
	}

	@Test
	public void testBuildAggregationsWithSingleFacetUsesKeywordSubField() {
		Map<String, ColumnModel> columnMap = new HashMap<>();
		columnMap.put("111",
				new ColumnModel().setId("111").setName("name").setColumnType(ColumnType.STRING));
		Map<String, String> nameToId = new HashMap<>();
		nameToId.put("name", "111");
		FacetRequest facet = new FacetRequest().setColumnName("name").setMaxValueCount(5L)
				.setSortField(FacetSortField.COUNT).setSortDirection(SortDirection.DESC);

		// call under test
		Map<String, Aggregation> aggs = manager.buildAggregations(
				Collections.singletonList(facet), columnMap, nameToId, null,
				Collections.emptyMap(), Collections.emptyMap(), scientificIdToQualifiedName());

		// keyed by column id
		assertNotNull(aggs.get("111"));
		assertEquals("111.keyword", aggs.get("111").terms().field());
		assertEquals(5, aggs.get("111").terms().size());
	}

	// --- buildFacetResult / buildFacetValueCount ---

	@Test
	public void testBuildFacetValueCount() {
		// call under test
		FacetColumnResultValueCount vc = manager.buildFacetValueCount("cancer", 42L);

		assertEquals("cancer", vc.getValue());
		assertEquals(Long.valueOf(42), vc.getCount());
		assertEquals(Boolean.FALSE, vc.getIsSelected());
	}

	@Test
	public void testBuildFacetResult() {
		List<FacetColumnResultValueCount> values = Arrays.asList(
				new FacetColumnResultValueCount().setValue("cancer").setCount(10L).setIsSelected(false),
				new FacetColumnResultValueCount().setValue("tumor").setCount(5L).setIsSelected(false));

		// call under test
		FacetColumnResultValues result = manager.buildFacetResult("diagnosis", values);

		assertEquals("diagnosis", result.getColumnName());
		assertEquals(FacetType.enumeration, result.getFacetType());
		assertEquals(values, result.getFacetValues());
	}

	// --- convertHighlights ---

	@Test
	public void testConvertHighlightsStripsSearchableSuffix() {
		Map<String, List<String>> highlightMap = new HashMap<>();
		highlightMap.put("111.searchable", Arrays.asList("<em>Alice</em>"));
		Map<String, String> idToName = new HashMap<>();
		idToName.put("111", "name");

		// call under test
		List<?> highlights = manager.convertHighlights(highlightMap, idToName);

		assertEquals(1, highlights.size());
		// .searchable suffix stripped then id translated back to name
		org.sagebionetworks.repo.model.search.SearchFieldValue hv =
				(org.sagebionetworks.repo.model.search.SearchFieldValue) highlights.get(0);
		assertEquals("name", hv.getName());
		assertEquals("<em>Alice</em>", hv.getValue());
	}

	@Test
	public void testConvertHighlightsJoinsMultipleFragmentsWithEllipsis() {
		Map<String, List<String>> highlightMap = new HashMap<>();
		highlightMap.put("111", Arrays.asList("<em>Alice</em>", "<em>Smith</em>"));
		Map<String, String> idToName = new HashMap<>();
		idToName.put("111", "name");

		// call under test
		org.sagebionetworks.repo.model.search.SearchFieldValue hv =
				manager.convertHighlights(highlightMap, idToName).get(0);

		assertEquals("<em>Alice</em> ... <em>Smith</em>", hv.getValue());
	}

	@Test
	public void testConvertHighlightsPreservesUnmappedKey() {
		// When key isn't in idToName, it's returned as-is (no ".searchable" suffix to strip)
		Map<String, List<String>> highlightMap = new HashMap<>();
		highlightMap.put("unknown", Collections.singletonList("hit"));

		// call under test
		org.sagebionetworks.repo.model.search.SearchFieldValue hv =
				manager.convertHighlights(highlightMap, Collections.emptyMap()).get(0);

		assertEquals("unknown", hv.getName());
	}

	// Note: convertResponse, convertHit, and convertAggregations each consume OpenSearch client
	// value types (SearchResponse<Map>, Hit<Map>, Aggregate) that must be constructed through
	// the client's builder API. Those helpers are exercised end-to-end by the AutoWired IT;
	// convertHighlights above covers the only branch with non-trivial logic that isn't
	// exclusively OpenSearch-client plumbing.

	@Test
	public void testDescribeErrorWithSingleCause() {
		org.opensearch.client.opensearch._types.ErrorCause cause =
				org.opensearch.client.opensearch._types.ErrorCause.of(b -> b
						.type("mapper_parsing_exception")
						.reason("failed to parse field [123]"));

		// call under test
		String desc = OpenSearchManagerImpl.describeError(cause);

		assertEquals("mapper_parsing_exception: failed to parse field [123]", desc);
	}

	@Test
	public void testDescribeErrorWalksCausedByChain() {
		// AOSS typically returns a generic outer reason; the actual cause is in caused_by.
		org.opensearch.client.opensearch._types.ErrorCause inner =
				org.opensearch.client.opensearch._types.ErrorCause.of(b -> b
						.type("illegal_state_exception")
						.reason("Position increment must be non-negative"));
		org.opensearch.client.opensearch._types.ErrorCause outer =
				org.opensearch.client.opensearch._types.ErrorCause.of(b -> b
						.type("?")
						.reason("Internal error occurred while processing request")
						.causedBy(inner));

		// call under test
		String desc = OpenSearchManagerImpl.describeError(outer);

		assertEquals(
				"?: Internal error occurred while processing request"
						+ " caused by illegal_state_exception: Position increment must be non-negative",
				desc);
	}

	@Test
	public void testDescribeErrorWithNullReturnsPlaceholder() {
		// call under test
		assertEquals("?", OpenSearchManagerImpl.describeError(null));
	}

	@Test
	public void testDescribeErrorWithRootCause() {
		// AOSS sometimes leaves the outer reason generic and puts the real diagnostic in
		// root_cause[]. Surface it so the failure is debuggable.
		ErrorCause rootCause = ErrorCause.of(b -> b
				.type("illegal_argument_exception")
				.reason("analyzer [synapse_analyzer_1] not found"));
		ErrorCause outer = ErrorCause.of(b -> b
				.type("?")
				.reason("Internal error occurred while processing request")
				.rootCause(rootCause));

		// call under test
		String desc = OpenSearchManagerImpl.describeError(outer);

		assertEquals(
				"?: Internal error occurred while processing request"
						+ " [rootCause=illegal_argument_exception: analyzer [synapse_analyzer_1] not found]",
				desc);
	}

	@Test
	public void testCreateIndexWithOpenSearchException() throws IOException {
		String indexName = "search-index-syn1";
		ErrorCause inner = ErrorCause.of(b -> b
				.type("illegal_argument_exception")
				.reason("For input string: \"abc\""));
		ErrorCause outer = ErrorCause.of(b -> b
				.type("mapper_parsing_exception")
				.reason("failed to parse field [col_123] of type [long]")
				.causedBy(inner));
		OpenSearchException openSearchException = new OpenSearchException(
				ErrorResponse.of(er -> er.error(outer).status(400)));

		when(openSearchClient.indices()).thenReturn(indicesClient);
		when(indicesClient.create(argThat((CreateIndexRequest req) -> indexName.equals(req.index()))))
				.thenThrow(openSearchException);

		// call under test
		RuntimeException ex = assertThrows(RuntimeException.class,
				() -> manager.createIndex(indexName, Collections.emptyList(), null,
						Collections.emptyList(), Collections.emptyList(), Collections.emptyMap()));

		assertEquals(openSearchException, ex.getCause());
		assertEquals("Failed to create search index: " + indexName
				+ " (" + OpenSearchManagerImpl.describeError(outer) + ")",
				ex.getMessage());
	}

	@Test
	public void testCreateIndexWithResourceAlreadyExists() throws IOException {
		String indexName = "search-index-syn1";
		OpenSearchException openSearchException = new OpenSearchException(
				ErrorResponse.of(er -> er.error(ErrorCause.of(b -> b
						.type("resource_already_exists_exception")
						.reason("index already exists"))).status(400)));

		when(openSearchClient.indices()).thenReturn(indicesClient);
		when(indicesClient.create(argThat((CreateIndexRequest req) -> indexName.equals(req.index()))))
				.thenThrow(openSearchException);

		// call under test
		Optional<String> result = manager.createIndex(indexName, Collections.emptyList(), null,
				Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());

		assertEquals(Optional.empty(), result);
	}

}
