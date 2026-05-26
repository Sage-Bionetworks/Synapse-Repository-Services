package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.FieldSort;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.ShardSearchFailure;
import org.opensearch.client.opensearch._types.ShardStatistics;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.LongTermsBucketKey;
import org.opensearch.client.opensearch._types.analysis.Analyzer;
import org.opensearch.client.opensearch._types.analysis.CustomAnalyzer;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.RangeQuery;
import org.opensearch.client.opensearch._types.query_dsl.TermsQuery;
import org.opensearch.client.opensearch._types.query_dsl.TextQueryType;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.DeleteResponse;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.core.search.HighlightField;
import org.opensearch.client.opensearch.core.search.HitsMetadata;
import org.opensearch.client.opensearch.core.search.TotalHits;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;
import org.opensearch.client.opensearch.core.search.TrackHits;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.sagebionetworks.repo.model.search.FacetRequest;
import org.sagebionetworks.repo.model.search.FacetSortField;
import org.sagebionetworks.repo.model.search.KeyRange;
import org.sagebionetworks.repo.model.search.KeyValues;
import org.sagebionetworks.repo.model.search.SearchQuery;
import org.sagebionetworks.repo.model.search.SearchQueryPart;
import org.sagebionetworks.repo.model.search.SearchQueryType;
import org.sagebionetworks.repo.model.search.SortDirection;
import org.sagebionetworks.repo.model.search.SortField;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverrideEntry;
import org.sagebionetworks.repo.model.search.table.ColumnSemanticEnrichmentEntry;
import org.sagebionetworks.repo.model.search.table.SemanticEnrichmentLanguageMode;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnResultValueCount;
import org.sagebionetworks.repo.model.table.FacetColumnResultValues;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.table.cluster.ColumnTypeInfo;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	@Mock
	private org.opensearch.client.opensearch.generic.OpenSearchGenericClient genericClient;
	@Mock
	private org.opensearch.client.opensearch.generic.Response genericResponse;

	@InjectMocks
	private OpenSearchManagerImpl manager;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	/**
	 * Test helper: turn a settings JSON string into the typed {@link IndexSettingsAnalysis}
	 * the manager API takes, going through the same {@link SearchAnalyzerJsonUtil} entry point
	 * the production lifecycle uses. Tests in this class don't exercise SynonymSet $refs,
	 * so the resolver returns null and a $ref would correctly raise.
	 */
	private static IndexSettingsAnalysis toAnalysis(String settingsJson) {
		try {
			return SearchOpaqueJsonUtil.resolveAnalyzerSettings(MAPPER.readTree(settingsJson), qname -> null);
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
	}

	private long originalBulkInitialBackoffMs;
	private long originalProbeInitialBackoffMs;
	private long originalCountProbeInitialBackoffMs;
	private long originalSentinelCleanupInitialBackoffMs;

	@BeforeEach
	public void setUp() {
		// Drop bulk-index retry backoff to 1ms in tests so retry-exhaustion paths don't
		// actually sleep ~21s per invocation. Restored in @AfterEach.
		originalBulkInitialBackoffMs = OpenSearchManagerImpl.BULK_INDEX_INITIAL_BACKOFF_MS;
		OpenSearchManagerImpl.BULK_INDEX_INITIAL_BACKOFF_MS = 1L;
		originalProbeInitialBackoffMs = OpenSearchManagerImpl.INDEX_WRITABLE_INITIAL_BACKOFF_MS;
		OpenSearchManagerImpl.INDEX_WRITABLE_INITIAL_BACKOFF_MS = 1L;
		originalCountProbeInitialBackoffMs = OpenSearchManagerImpl.COUNT_PROBE_INITIAL_BACKOFF_MS;
		OpenSearchManagerImpl.COUNT_PROBE_INITIAL_BACKOFF_MS = 1L;
		originalSentinelCleanupInitialBackoffMs = OpenSearchManagerImpl.SENTINEL_CLEANUP_INITIAL_BACKOFF_MS;
		OpenSearchManagerImpl.SENTINEL_CLEANUP_INITIAL_BACKOFF_MS = 1L;
	}

	@AfterEach
	public void tearDown() {
		OpenSearchManagerImpl.BULK_INDEX_INITIAL_BACKOFF_MS = originalBulkInitialBackoffMs;
		OpenSearchManagerImpl.INDEX_WRITABLE_INITIAL_BACKOFF_MS = originalProbeInitialBackoffMs;
		OpenSearchManagerImpl.COUNT_PROBE_INITIAL_BACKOFF_MS = originalCountProbeInitialBackoffMs;
		OpenSearchManagerImpl.SENTINEL_CLEANUP_INITIAL_BACKOFF_MS = originalSentinelCleanupInitialBackoffMs;
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

	// --- toAossKey ---

	@Test
	public void testToAossKeyEncodesDots() {
		// AOSS rejects '.' inside settings keys (it treats them as JSON-path separators), so
		// the qname-to-AOSS-key translation encodes dots at the wire boundary.
		assertEquals("org__dot__sagebionetworks-SCIENTIFIC",
				OpenSearchManagerImpl.toAossKey("org.sagebionetworks-SCIENTIFIC"));
	}

	@Test
	public void testToAossKeyWithoutDotsIsUnchanged() {
		// Underscores in the qname are preserved verbatim — the dot-encoding scheme is bijective
		// so qnames with underscores can never collide with qnames containing dots.
		assertEquals("biomed-medical_terms",
				OpenSearchManagerImpl.toAossKey("biomed-medical_terms"));
	}

	@Test
	public void testToAossKeyEncodingIsBijective() {
		// Two qnames that differ only in `.` vs `_` placement must encode to different
		// AOSS keys, otherwise the analysis registry collapses them to a single namespaced
		// component and the wrong TextAnalyzer wins.
		assertNotEquals(
				OpenSearchManagerImpl.toAossKey("org.sage-A.B"),
				OpenSearchManagerImpl.toAossKey("org_sage-A_B"));
	}

	@Test
	public void testToAossKeyWithNullReturnsNull() {
		assertNull(OpenSearchManagerImpl.toAossKey(null));
	}

	// --- rewriteOwnedReferences ---

	@Test
	public void testRewriteOwnedReferencesRewritesOwnedReferences() {
		// A CustomAnalyzer with a tokenizer reference, char_filter chain, and filter chain
		// mixing owned and built-in names. Owned names get namespaced; built-ins pass through.
		Analyzer entry = Analyzer.of(b -> b.custom(c -> c
				.tokenizer("std")
				.charFilter(Arrays.asList("strip_html", "icu_normalizer"))
				.filter(Arrays.asList("lowercase", "my_syn", "english_stop"))));
		Set<String> ownedCharFilters = new HashSet<>();
		ownedCharFilters.add("strip_html");
		Set<String> ownedTokenizers = new HashSet<>();
		ownedTokenizers.add("std");
		Set<String> ownedFilters = new HashSet<>();
		ownedFilters.add("my_syn");
		ownedFilters.add("english_stop");

		// call under test
		Analyzer rewritten = OpenSearchManagerImpl.rewriteOwnedReferences(entry,
				"biomed-publications", ownedCharFilters, ownedFilters, ownedTokenizers);

		assertTrue(rewritten.isCustom());
		CustomAnalyzer custom = rewritten.custom();
		assertEquals("biomed-publications__std", custom.tokenizer());
		assertEquals(Arrays.asList("biomed-publications__strip_html", "icu_normalizer"),
				custom.charFilter());
		assertEquals(Arrays.asList("lowercase", "biomed-publications__my_syn",
				"biomed-publications__english_stop"), custom.filter());
	}

	@Test
	public void testRewriteOwnedReferencesWithBuiltInsOnlyIsIdempotent() {
		Analyzer entry = Analyzer.of(b -> b.custom(c -> c
				.tokenizer("standard")
				.filter(Arrays.asList("lowercase", "english_stop"))));

		// call under test — no owned names anywhere; all references should pass through.
		Analyzer rewritten = OpenSearchManagerImpl.rewriteOwnedReferences(entry, "org-X",
				new HashSet<>(), new HashSet<>(), new HashSet<>());

		assertTrue(rewritten.isCustom());
		assertEquals("standard", rewritten.custom().tokenizer());
		assertEquals(Arrays.asList("lowercase", "english_stop"), rewritten.custom().filter());
	}

	@Test
	public void testRewriteOwnedReferencesPassesThroughNonCustomAnalyzers() {
		// A built-in analyzer (KeywordAnalyzer/StandardAnalyzer/etc.) has no chain and no
		// references to local components — return as-is.
		Analyzer keyword = Analyzer.of(b -> b.keyword(k -> k));

		// call under test
		Analyzer rewritten = OpenSearchManagerImpl.rewriteOwnedReferences(keyword, "org-X",
				new HashSet<>(), new HashSet<>(), new HashSet<>());

		assertTrue(rewritten.isKeyword());
	}

	// --- isConcurrentDeleteError ---

	@Test
	public void testIsConcurrentDeleteErrorMatchesAOSSMarker() {
		// Belt-and-braces: the static helper recognizes AOSS's "concurrent deletes" rejection
		// so the lifecycle worker can map it to a recoverable SQS retry.
		OpenSearchException e = new OpenSearchException(
				ErrorResponse.of(b -> b.status(400)
						.error(c -> c.type("any").reason("concurrent deletes detected"))));
		assertTrue(OpenSearchManagerImpl.isConcurrentDeleteError(e));
	}

	@Test
	public void testIsConcurrentDeleteErrorWithUnrelatedReasonReturnsFalse() {
		OpenSearchException e = new OpenSearchException(
				ErrorResponse.of(b -> b.status(400)
						.error(c -> c.type("validation_exception").reason("some other rejection"))));
		assertFalse(OpenSearchManagerImpl.isConcurrentDeleteError(e));
	}

	// --- isRetryableItemStatus ---

	@Test
	public void testIsRetryableItemStatusFor429() {
		assertTrue(OpenSearchManagerImpl.isRetryableItemStatus(429));
	}

	@Test
	public void testIsRetryableItemStatusForServerErrors() {
		assertTrue(OpenSearchManagerImpl.isRetryableItemStatus(500));
		assertTrue(OpenSearchManagerImpl.isRetryableItemStatus(503));
		assertTrue(OpenSearchManagerImpl.isRetryableItemStatus(599));
	}

	@Test
	public void testIsRetryableItemStatusForClientErrorsIsFalse() {
		// 4xx other than 429 are permanent — bad request, conflict, etc. shouldn't be retried.
		assertFalse(OpenSearchManagerImpl.isRetryableItemStatus(400));
		assertFalse(OpenSearchManagerImpl.isRetryableItemStatus(404));
		assertFalse(OpenSearchManagerImpl.isRetryableItemStatus(409));
	}

	@Test
	public void testIsRetryableItemStatusForSuccessIsFalse() {
		assertFalse(OpenSearchManagerImpl.isRetryableItemStatus(200));
		assertFalse(OpenSearchManagerImpl.isRetryableItemStatus(201));
	}

	// --- buildPermanentFailureMessage ---

	@Test
	public void testBuildPermanentFailureMessageWithNoSamplesReturnsSummary() {
		assertEquals("the summary",
				OpenSearchManagerImpl.buildPermanentFailureMessage("the summary", Collections.emptyList()));
	}

	@Test
	public void testBuildPermanentFailureMessageAppendsSamples() {
		String result = OpenSearchManagerImpl.buildPermanentFailureMessage(
				"summary", Arrays.asList("doc1 failed", "doc2 failed"));
		assertEquals("summary. Sample failures:\n - doc1 failed\n - doc2 failed", result);
	}

	@Test
	public void testBuildPermanentFailureMessageTruncatesAtCap() {
		// Build a sample big enough to push past MAX_BULK_ERROR_MESSAGE_CHARS so the helper
		// substring-truncates the tail with the marker.
		StringBuilder huge = new StringBuilder();
		for (int i = 0; i < OpenSearchManagerImpl.MAX_BULK_ERROR_MESSAGE_CHARS; i++) {
			huge.append('x');
		}
		String result = OpenSearchManagerImpl.buildPermanentFailureMessage(
				"summary", Collections.singletonList(huge.toString()));
		assertEquals(OpenSearchManagerImpl.MAX_BULK_ERROR_MESSAGE_CHARS, result.length());
		assertTrue(result.endsWith(OpenSearchManagerImpl.TRUNCATION_MARKER));
	}

	// --- longBucketKeyToString ---

	@Test
	public void testLongBucketKeyToStringPrefersKeyAsStringWhenPresent() {
		// keyAsString is populated for boolean/date fields with an implicit format and should
		// be used verbatim — that's what makes booleans render as "true"/"false" rather than
		// "1"/"0".
		LongTermsBucketKey signed = LongTermsBucketKey.of(b -> b.signed(1L));
		assertEquals("true", OpenSearchManagerImpl.longBucketKeyToString("true", signed));
	}

	@Test
	public void testLongBucketKeyToStringFallsBackToSignedKey() {
		// keyAsString is null for plain LONG fields — fall back to the typed key.
		LongTermsBucketKey signed = LongTermsBucketKey.of(b -> b.signed(42L));
		assertEquals("42", OpenSearchManagerImpl.longBucketKeyToString(null, signed));
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
				.setAnalyzer(new org.json.JSONObject().put("$ref", "org.sage-AUTOCOMPLETE"));
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
				.setAnalyzer(new org.json.JSONObject().put("$ref", "org.sage-AUTOCOMPLETE"));
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
				.setColumnName("geneName").setAnalyzer(new org.json.JSONObject().put("$ref", "FIRST"));
		ColumnAnalyzerOverrideEntry second = new ColumnAnalyzerOverrideEntry()
				.setColumnName("geneName").setAnalyzer(new org.json.JSONObject().put("$ref", "SECOND"));
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

	// --- getFilterFieldName ---

	@Test
	public void testGetFilterFieldNameForUnknownColumnReturnsId() {
		// Defensive: an unknown column id (e.g. system field _row_id) should pass through
		// unmodified rather than throwing.
		assertEquals("999", manager.getFilterFieldName("999", Collections.emptyMap()));
	}

	@Test
	public void testGetFilterFieldNameForTextColumnAppendsKeyword() {
		Map<String, ColumnModel> columnMap = new HashMap<>();
		columnMap.put("111",
				new ColumnModel().setId("111").setName("name").setColumnType(ColumnType.STRING));

		// call under test — text types route to the `.keyword` sub-field for filtering
		assertEquals("111.keyword", manager.getFilterFieldName("111", columnMap));
	}

	@Test
	public void testGetFilterFieldNameForLinkColumnAppendsKeyword() {
		Map<String, ColumnModel> columnMap = new HashMap<>();
		columnMap.put("222",
				new ColumnModel().setId("222").setName("link").setColumnType(ColumnType.LINK));

		// call under test — LINK shares the TEXT mapping, so .keyword sub-field for exact match
		assertEquals("222.keyword", manager.getFilterFieldName("222", columnMap));
	}

	@Test
	public void testGetFilterFieldNameForNumericColumnReturnsId() {
		Map<String, ColumnModel> columnMap = new HashMap<>();
		columnMap.put("333",
				new ColumnModel().setId("333").setName("age").setColumnType(ColumnType.INTEGER));

		// call under test — numeric columns don't need a sub-field; id returned as-is
		assertEquals("333", manager.getFilterFieldName("333", columnMap));
	}

	// --- getSearchFieldName ---

	@Test
	public void testGetSearchFieldNameReturnsBareId() {
		// Search-path always uses the analyzed primary field; .keyword sub-field is filter-only.
		assertEquals("111", manager.getSearchFieldName("111"));
	}

	// --- resolveQueryFields ---

	@ParameterizedTest(name = "resolveQueryFields({0}) = null")
	@MethodSource("resolveQueryFieldsNullProvider")
	public void testResolveQueryFieldsWithNullOrEmpty(String description, List<String> input) {
		// call under test — null/empty input is a signal to let OpenSearch use its default fields
		assertNull(manager.resolveQueryFields(input, Collections.emptyList(), true));
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
				Collections.singletonList("geneName"), columns, true);

		assertEquals(Collections.singletonList("111"), fields);
	}

	@Test
	public void testResolveQueryFieldsPreservesBoostSuffix() {
		List<ColumnModel> columns = Arrays.asList(
				new ColumnModel().setId("111").setName("geneName").setColumnType(ColumnType.STRING));

		// call under test — "geneName^3" → "111^3"
		List<String> fields = manager.resolveQueryFields(
				Collections.singletonList("geneName^3"), columns, true);

		assertEquals(Collections.singletonList("111^3"), fields);
	}

	@Test
	public void testResolveQueryFieldsForFilterUsesKeywordSubField() {
		List<ColumnModel> columns = Collections.singletonList(
				new ColumnModel().setId("111").setName("name").setColumnType(ColumnType.STRING));

		// call under test — filter side routes text fields through .keyword.
		List<String> resolved = manager.resolveQueryFields(
				Collections.singletonList("name"), columns, false);

		assertEquals(Collections.singletonList("111.keyword"), resolved);
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
				Collections.emptyMap(), Collections.emptyMap());

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
				Collections.emptyMap(), Collections.emptyMap());

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
				columnMap, nameToId);

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
		Map<String, HighlightField> fields = manager.buildHighlightFields(columns);

		assertEquals(Collections.singleton("111"), fields.keySet());
	}

	@Test
	public void testBuildHighlightFieldsIncludesLinkColumnUnderBareId() {
		List<ColumnModel> columns = Collections.singletonList(
				new ColumnModel().setId("222").setName("link").setColumnType(ColumnType.LINK));

		// call under test — LINK shares the TEXT mapping; highlight field is the bare id.
		Map<String, HighlightField> fields = manager.buildHighlightFields(columns);

		assertEquals(Collections.singleton("222"), fields.keySet());
	}

	// --- buildAggregations ---

	@ParameterizedTest(name = "buildAggregations({0}) → empty")
	@MethodSource("buildAggregationsEmptyProvider")
	public void testBuildAggregationsWithNullOrEmpty(String description, List<FacetRequest> input) {
		// call under test
		assertTrue(manager.buildAggregations(input,
				Collections.emptyMap(), Collections.emptyMap()).isEmpty());
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
				Collections.singletonList(facet), columnMap, nameToId);

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
	public void testConvertHighlightsTranslatesIdToName() {
		Map<String, List<String>> highlightMap = new HashMap<>();
		highlightMap.put("111", Arrays.asList("<em>Alice</em>"));
		Map<String, String> idToName = new HashMap<>();
		idToName.put("111", "name");

		// call under test
		List<?> highlights = manager.convertHighlights(highlightMap, idToName);

		assertEquals(1, highlights.size());
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
		// When key isn't in idToName, it's returned as-is.
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

	// convertFieldValue stringifies a single AOSS _source value for SearchFieldValue.value.
	// Lists and maps (the *_LIST and JSON column types) must be written as canonical JSON so
	// clients can parse them back; scalars must use String.valueOf so a raw String column is
	// not double-quoted. PLFM-9625 was the latter branch silently using Java's List.toString
	// (`[a, b]`) instead of JSON.

	@Test
	public void testConvertFieldValueWithNull() {
		// call under test
		assertNull(OpenSearchManagerImpl.convertFieldValue(null));
	}

	@Test
	public void testConvertFieldValueWithString() {
		// call under test
		assertEquals("alpha", OpenSearchManagerImpl.convertFieldValue("alpha"));
	}

	@Test
	public void testConvertFieldValueWithInteger() {
		// call under test
		assertEquals("123", OpenSearchManagerImpl.convertFieldValue(123));
	}

	@Test
	public void testConvertFieldValueWithLong() {
		// call under test
		assertEquals("1609459200000", OpenSearchManagerImpl.convertFieldValue(1609459200000L));
	}

	@Test
	public void testConvertFieldValueWithDouble() {
		// call under test
		assertEquals("1.5", OpenSearchManagerImpl.convertFieldValue(1.5));
	}

	@Test
	public void testConvertFieldValueWithBoolean() {
		// call under test
		assertEquals("true", OpenSearchManagerImpl.convertFieldValue(Boolean.TRUE));
	}

	@Test
	public void testConvertFieldValueWithListOfStrings() {
		// PLFM-9625: List values must round-trip as canonical JSON, not Java List.toString().
		// call under test
		assertEquals("[\"alpha\",\"beta\"]",
				OpenSearchManagerImpl.convertFieldValue(Arrays.asList("alpha", "beta")));
	}

	@Test
	public void testConvertFieldValueWithListOfStringsContainingComma() {
		// The ticket's motivating case: a list element contains a comma, so the buggy
		// `[alpha, b,c]` form would be unparseable. JSON quoting must preserve element boundaries.
		// call under test
		assertEquals("[\"alpha\",\"b,c\"]",
				OpenSearchManagerImpl.convertFieldValue(Arrays.asList("alpha", "b,c")));
	}

	@Test
	public void testConvertFieldValueWithListOfIntegers() {
		// call under test
		assertEquals("[1,2,3]",
				OpenSearchManagerImpl.convertFieldValue(Arrays.asList(1, 2, 3)));
	}

	@Test
	public void testConvertFieldValueWithMap() {
		// JSON column type: AOSS returns a Map; must be re-serialized as canonical JSON.
		// LinkedHashMap pins key order so the asserted JSON is deterministic.
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("a", 1);
		map.put("b", "x");

		// call under test
		assertEquals("{\"a\":1,\"b\":\"x\"}", OpenSearchManagerImpl.convertFieldValue(map));
	}

	@Test
	public void testConvertFieldValueWithListOfLargeLongsPreservesPrecision() {
		// Synapse entity / file-handle ids routinely exceed 2^53, so list serialization must
		// preserve full 64-bit precision. Jackson does this; org.json (which we no longer use)
		// coerces every numeric through double and would silently truncate the trailing bit.
		long beyondDouble = 9007199254740993L;  // 2^53 + 1; not exactly representable as double
		assertEquals("[9007199254740993,9007199254740994]",
				OpenSearchManagerImpl.convertFieldValue(Arrays.asList(beyondDouble, beyondDouble + 1L)));
	}

	@Test
	public void testConvertFieldValueWithMapOfLargeLongsPreservesPrecision() {
		// Same precision requirement applies to JSON column maps.
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("id", 9007199254740993L);

		// call under test
		assertEquals("{\"id\":9007199254740993}", OpenSearchManagerImpl.convertFieldValue(map));
	}

	@Test
	public void testDescribeErrorWithSingleCause() {
		ErrorCause cause = ErrorCause.of(b -> b
				.type("mapper_parsing_exception")
				.reason("failed to parse field [123]"));

		// call under test
		String desc = OpenSearchManagerImpl.describeError(cause);

		assertEquals("mapper_parsing_exception: failed to parse field [123]", desc);
	}

	@Test
	public void testDescribeErrorWalksCausedByChain() {
		// AOSS typically returns a generic outer reason; the actual cause is in caused_by.
		ErrorCause inner = ErrorCause.of(b -> b
				.type("illegal_state_exception")
				.reason("Position increment must be non-negative"));
		ErrorCause outer = ErrorCause.of(b -> b
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
	public void testCreateIndexHappyPathRegistersResolvedAnalyzersAndReturnsAppliedJson() throws IOException {
		// Happy-path createIndex with a single resolved analyzer carrying owned filter +
		// analyzer.default + analyzer.default_search entries, and one STRING column bound
		// to that analyzer as both the index default and the column-type default. The
		// applied JSON returned by the manager must include:
		//   - the namespaced filter under settings.analysis.filter.{aossKey}__english_stop
		//   - the bare reserved analyzer.default (promoted from primary's default entry)
		//   - the bare reserved analyzer.default_search (promoted from primary's default_search)
		//   - the field mapping for the STRING column under mappings.properties.{colId}
		String indexName = "search-index-syn1";
		// SCIENTIFIC is the column-type default for STRING; binding the test analyzer at that
		// qname collapses both the index-default and column-type-default to the same registered
		// analyzer so the per-column "was not registered" guard is satisfied.
		String qname = "org.sagebionetworks-SCIENTIFIC";
		String aossKey = OpenSearchManagerImpl.toAossKey(qname);
		String settingsJson = "{"
				+ "\"filter\":{\"english_stop\":{\"type\":\"stop\",\"stopwords\":\"_english_\"}},"
				+ "\"analyzer\":{"
					+ "\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\",\"filter\":[\"english_stop\"]},"
					+ "\"default_search\":{\"type\":\"custom\",\"tokenizer\":\"keyword\"}"
				+ "}}";
		Map<String, IndexSettingsAnalysis> resolvedAnalyzers = Collections.singletonMap(qname,
				toAnalysis(settingsJson));

		List<ColumnModel> columns = Collections.singletonList(
				new ColumnModel().setId("100").setName("title").setColumnType(ColumnType.STRING));

		when(openSearchClient.generic()).thenReturn(genericClient);
		when(genericResponse.getStatus()).thenReturn(200);
		ArgumentCaptor<org.opensearch.client.opensearch.generic.Request> reqCaptor =
				ArgumentCaptor.forClass(org.opensearch.client.opensearch.generic.Request.class);
		when(genericClient.execute(reqCaptor.capture())).thenReturn(genericResponse);

		// call under test
		Optional<String> appliedJson = manager.createIndex(indexName, columns, qname,
				Collections.emptyList(), resolvedAnalyzers, null);

		assertTrue(appliedJson.isPresent());
		String applied = appliedJson.get();

		// The applied analysis block must register the namespaced filter and surface the
		// primary analyzer's default / default_search entries at the bare reserved keys.
		assertTrue(applied.contains("\"" + aossKey + "__english_stop\""),
				"Owned filter must be registered under namespaced key: " + applied);
		assertTrue(applied.contains("\"default\""),
				"Reserved analyzer.default must be present: " + applied);
		assertTrue(applied.contains("\"default_search\""),
				"Reserved analyzer.default_search must be present (asymmetric search): " + applied);
		// The STRING column must land in the mappings.properties block under its column id.
		assertTrue(applied.contains("\"100\""),
				"Field mapping for the STRING column must be registered under its id: " + applied);

		// And the captured request must target the right endpoint and method.
		assertEquals("/" + indexName, reqCaptor.getValue().getEndpoint());
		assertEquals("PUT", reqCaptor.getValue().getMethod());
	}

	@Test
	public void testCreateIndexBindsSymmetricFieldSearchAnalyzerToIndexAnalyzer() throws IOException {
		// When a non-primary TextAnalyzer (one with no default_search of its own) is bound to
		// a field via ColumnAnalyzerOverride, the field must set BOTH analyzer and
		// search_analyzer to the same namespaced registry key. Otherwise the index-wide
		// `default_search` (registered for the primary analyzer) hijacks the field at query
		// time per OpenSearch's analyzer precedence rules — the per-field `analyzer` mapping
		// is rule 4, but the index `default_search` is rule 3, so rule 3 wins without an
		// explicit per-field `search_analyzer` (rule 2).
		String indexName = "search-index-syn1";
		// Primary analyzer (declares default_search); collapsed to the column-type default for STRING.
		String primaryQname = "org.sagebionetworks-SCIENTIFIC";
		String primarySettings = "{"
				+ "\"analyzer\":{"
					+ "\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\"},"
					+ "\"default_search\":{\"type\":\"custom\",\"tokenizer\":\"keyword\"}"
				+ "}}";
		// Override analyzer (symmetric — no default_search). Bound to a specific field below.
		String overrideQname = "biomed-pubs";
		String overrideAossKey = OpenSearchManagerImpl.toAossKey(overrideQname);
		String overrideSettings = "{"
				+ "\"analyzer\":{\"default\":{\"type\":\"custom\",\"tokenizer\":\"whitespace\"}}}";
		Map<String, IndexSettingsAnalysis> resolvedAnalyzers = new HashMap<>();
		resolvedAnalyzers.put(primaryQname, toAnalysis(primarySettings));
		resolvedAnalyzers.put(overrideQname, toAnalysis(overrideSettings));

		List<ColumnModel> columns = Collections.singletonList(
				new ColumnModel().setId("100").setName("title").setColumnType(ColumnType.STRING));
		ColumnAnalyzerOverride override = new ColumnAnalyzerOverride();
		ColumnAnalyzerOverrideEntry entry = new ColumnAnalyzerOverrideEntry();
		entry.setColumnName("title");
		entry.setAnalyzer(new org.json.JSONObject().put("$ref", overrideQname));
		override.setOverrides(Collections.singletonList(entry));


		when(openSearchClient.generic()).thenReturn(genericClient);
		when(genericResponse.getStatus()).thenReturn(200);
		when(genericClient.execute(any(org.opensearch.client.opensearch.generic.Request.class)))
				.thenReturn(genericResponse);

		// call under test
		Optional<String> appliedJson = manager.createIndex(indexName, columns, primaryQname,
				Collections.singletonList(override), resolvedAnalyzers, null);

		assertTrue(appliedJson.isPresent());
		// Parse the applied JSON and assert on the typed shape rather than JSON-token order
		// (the Java client doesn't guarantee a stable property order for text-field properties).
		JsonNode field100 = MAPPER.readTree(appliedJson.get())
				.at("/mappings/properties/100");
		assertEquals("text", field100.path("type").asText());
		// The field must bind analyzer AND search_analyzer both to the same namespaced key.
		// Without the explicit search_analyzer the index-wide default_search would win at query time.
		assertEquals(overrideAossKey, field100.path("analyzer").asText());
		assertEquals(overrideAossKey, field100.path("search_analyzer").asText());
	}

	@Test
	public void testCreateIndexBindsAsymmetricFieldSearchAnalyzerToDefaultSearchKey() throws IOException {
		// When the override TextAnalyzer declares its own default_search, the field's
		// search_analyzer must bind to that entry's namespaced registry key (not the bare qname).
		String indexName = "search-index-syn1";
		String primaryQname = "org.sagebionetworks-SCIENTIFIC";
		String primarySettings = "{\"analyzer\":{\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\"}}}";
		String overrideQname = "biomed-pubs";
		String overrideAossKey = OpenSearchManagerImpl.toAossKey(overrideQname);
		String overrideSettings = "{"
				+ "\"analyzer\":{"
					+ "\"default\":{\"type\":\"custom\",\"tokenizer\":\"whitespace\"},"
					+ "\"default_search\":{\"type\":\"custom\",\"tokenizer\":\"keyword\"}"
				+ "}}";
		Map<String, IndexSettingsAnalysis> resolvedAnalyzers = new HashMap<>();
		resolvedAnalyzers.put(primaryQname, toAnalysis(primarySettings));
		resolvedAnalyzers.put(overrideQname, toAnalysis(overrideSettings));

		List<ColumnModel> columns = Collections.singletonList(
				new ColumnModel().setId("100").setName("title").setColumnType(ColumnType.STRING));
		ColumnAnalyzerOverride override = new ColumnAnalyzerOverride();
		ColumnAnalyzerOverrideEntry entry = new ColumnAnalyzerOverrideEntry();
		entry.setColumnName("title");
		entry.setAnalyzer(new org.json.JSONObject().put("$ref", overrideQname));
		override.setOverrides(Collections.singletonList(entry));


		when(openSearchClient.generic()).thenReturn(genericClient);
		when(genericResponse.getStatus()).thenReturn(200);
		when(genericClient.execute(any(org.opensearch.client.opensearch.generic.Request.class)))
				.thenReturn(genericResponse);

		// call under test
		Optional<String> appliedJson = manager.createIndex(indexName, columns, primaryQname,
				Collections.singletonList(override), resolvedAnalyzers, null);

		assertTrue(appliedJson.isPresent());
		JsonNode field100 = MAPPER.readTree(appliedJson.get())
				.at("/mappings/properties/100");
		assertEquals("text", field100.path("type").asText());
		assertEquals(overrideAossKey, field100.path("analyzer").asText());
		assertEquals(overrideAossKey + "__default_search", field100.path("search_analyzer").asText());
	}

	@Test
	public void testCreateIndexWithMappingFailure() throws IOException {
		// AOSS-side rejections come back as non-2xx responses on the generic client. A
		// mapper_parsing_exception is surfaced as HTTP 400 with the typed error body — the
		// manager must wrap that into a clear RuntimeException carrying the body text so the
		// caller (and SearchIndexStatus.errorMessage) can see why the build failed.
		String indexName = "search-index-syn1";
		String errorBody = "{\"error\":{\"type\":\"mapper_parsing_exception\","
				+ "\"reason\":\"failed to parse field [col_123] of type [long]\"}}";
		when(openSearchClient.generic()).thenReturn(genericClient);
		when(genericResponse.getStatus()).thenReturn(400);
		when(genericResponse.getBody()).thenReturn(Optional.of(
				org.opensearch.client.opensearch.generic.Bodies.json(errorBody)));
		when(genericClient.execute(any(org.opensearch.client.opensearch.generic.Request.class)))
				.thenReturn(genericResponse);

		// call under test
		RuntimeException ex = assertThrows(RuntimeException.class,
				() -> manager.createIndex(indexName, Collections.emptyList(), null,
						Collections.emptyList(), Collections.emptyMap(), null));

		assertTrue(ex.getMessage().contains("Failed to create search index: " + indexName));
		assertTrue(ex.getMessage().contains("HTTP 400"));
		assertTrue(ex.getMessage().contains("mapper_parsing_exception"),
				"error body should be reported in the wrapped message: " + ex.getMessage());
	}

	@Test
	public void testCreateIndexWithResourceAlreadyExists() throws IOException {
		// AOSS surfaces resource_already_exists as HTTP 400 with that error type in the body —
		// the manager treats this as an idempotent no-op rather than a failure.
		String indexName = "search-index-syn1";
		String errorBody = "{\"error\":{\"type\":\"resource_already_exists_exception\","
				+ "\"reason\":\"index already exists\"}}";
		when(openSearchClient.generic()).thenReturn(genericClient);
		when(genericResponse.getStatus()).thenReturn(400);
		when(genericResponse.getBody()).thenReturn(Optional.of(
				org.opensearch.client.opensearch.generic.Bodies.json(errorBody)));
		when(genericClient.execute(any(org.opensearch.client.opensearch.generic.Request.class)))
				.thenReturn(genericResponse);

		// call under test
		Optional<String> result = manager.createIndex(indexName, Collections.emptyList(), null,
				Collections.emptyList(), Collections.emptyMap(), null);

		assertEquals(Optional.empty(), result);
	}

	@Test
	public void testCreateIndexWithColumnSemanticEnrichment() throws IOException {
		// columnSemanticEnrichment opts a text column in to AOSS Automatic Semantic
		// Enrichment. The opensearch-java 3.7.0 typed Property model has no slot for the
		// semantic_enrichment block, so the manager serializes the typed body, splices the
		// block on, and sends it through the generic client. Verify the body sent over the
		// wire contains both the typed analyzer-binding bits (untouched) and the
		// semantic_enrichment block on the opted-in column only.
		String indexName = "search-index-syn1";
		String qname = "org.sagebionetworks-SCIENTIFIC";
		Map<String, IndexSettingsAnalysis> resolvedAnalyzers = Collections.singletonMap(qname,
				toAnalysis("{\"analyzer\":{\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\"}}}"));

		ColumnModel enrichedColumn = new ColumnModel().setId("100").setName("abstract")
				.setColumnType(ColumnType.LARGETEXT);
		ColumnModel plainColumn = new ColumnModel().setId("200").setName("doi")
				.setColumnType(ColumnType.STRING);
		List<ColumnModel> columns = List.of(enrichedColumn, plainColumn);

		ColumnSemanticEnrichmentEntry enrichmentEntry = new ColumnSemanticEnrichmentEntry()
				.setColumnName("abstract")
				.setLanguageMode(SemanticEnrichmentLanguageMode.MULTI_LINGUAL);

		when(openSearchClient.generic()).thenReturn(genericClient);
		when(genericResponse.getStatus()).thenReturn(200);
		ArgumentCaptor<org.opensearch.client.opensearch.generic.Request> reqCaptor =
				ArgumentCaptor.forClass(org.opensearch.client.opensearch.generic.Request.class);
		when(genericClient.execute(reqCaptor.capture())).thenReturn(genericResponse);

		// call under test
		Optional<String> appliedJson = manager.createIndex(indexName, columns, qname,
				Collections.emptyList(), resolvedAnalyzers,
				Collections.singletonList(enrichmentEntry));

		assertTrue(appliedJson.isPresent());
		org.opensearch.client.opensearch.generic.Request sentReq = reqCaptor.getValue();
		assertEquals("/" + indexName, sentReq.getEndpoint());
		assertEquals("PUT", sentReq.getMethod());
		String sentBody = sentReq.getBody().orElseThrow().bodyAsString();
		com.fasterxml.jackson.databind.JsonNode applied =
				new com.fasterxml.jackson.databind.ObjectMapper().readTree(sentBody);

		// The enriched column ("abstract", column id 100) must carry the semantic_enrichment
		// block with the AOSS literal "MULTI-LINGUAL" (note the hyphen — not the Java enum
		// name MULTI_LINGUAL). The non-enriched column ("doi", id 200) must not.
		com.fasterxml.jackson.databind.JsonNode properties = applied.path("mappings").path("properties");
		com.fasterxml.jackson.databind.JsonNode enrichedProperty = properties.path("100");
		com.fasterxml.jackson.databind.JsonNode semanticBlock = enrichedProperty.path("semantic_enrichment");
		assertEquals("ENABLED", semanticBlock.path("status").asText());
		assertEquals("MULTI-LINGUAL", semanticBlock.path("language_options").asText());
		assertTrue(properties.path("200").path("semantic_enrichment").isMissingNode(),
				"Non-enriched column must not carry a semantic_enrichment block");

		// The existing typed mapping bits still apply — the enriched column kept its text
		// type and the index-wide analyzer settings landed in the patched body.
		assertEquals("text", enrichedProperty.path("type").asText());
		assertTrue(sentBody.contains("\"default\""),
				"Reserved analyzer.default must survive into the patched body: " + sentBody);
	}

	@Test
	public void testCreateIndexWithSemanticEnrichmentDefaultsToEnglish() throws IOException {
		// languageMode is optional on a ColumnSemanticEnrichmentEntry — when omitted the
		// manager must default to AOSS' "english" literal rather than passing null. English
		// mode is the documented latency-optimized path.
		String indexName = "search-index-syn1";
		String qname = "org.sagebionetworks-SCIENTIFIC";
		Map<String, IndexSettingsAnalysis> resolvedAnalyzers = Collections.singletonMap(qname,
				toAnalysis("{\"analyzer\":{\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\"}}}"));
		List<ColumnModel> columns = List.of(
				new ColumnModel().setId("100").setName("body").setColumnType(ColumnType.LARGETEXT));
		ColumnSemanticEnrichmentEntry entry = new ColumnSemanticEnrichmentEntry()
				.setColumnName("body");
		// no languageMode set — must default to english

		when(openSearchClient.generic()).thenReturn(genericClient);
		when(genericResponse.getStatus()).thenReturn(200);
		ArgumentCaptor<org.opensearch.client.opensearch.generic.Request> reqCaptor =
				ArgumentCaptor.forClass(org.opensearch.client.opensearch.generic.Request.class);
		when(genericClient.execute(reqCaptor.capture())).thenReturn(genericResponse);

		// call under test
		manager.createIndex(indexName, columns, qname,
				Collections.emptyList(), resolvedAnalyzers, Collections.singletonList(entry));

		String sentBody = reqCaptor.getValue().getBody().orElseThrow().bodyAsString();
		com.fasterxml.jackson.databind.JsonNode applied =
				new com.fasterxml.jackson.databind.ObjectMapper().readTree(sentBody);
		assertEquals("english",
				applied.path("mappings").path("properties").path("100")
						.path("semantic_enrichment").path("language_options").asText());
	}

	@Test
	public void testCreateIndexSkipsSemanticEnrichmentForMissingOrIneligibleColumn() throws IOException {
		// AOSS restricts Automatic Semantic Enrichment to top-level text fields. Entries
		// naming an unknown column or a non-text column must be silently dropped (matching
		// the ColumnAnalyzerOverride posture) so a single SearchConfiguration can be reused
		// across SearchIndexes that don't share the exact same schema.
		String indexName = "search-index-syn1";
		String qname = "org.sagebionetworks-KEYWORD";
		Map<String, IndexSettingsAnalysis> resolvedAnalyzers = Collections.singletonMap(qname,
				toAnalysis("{\"analyzer\":{\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\"}}}"));
		ColumnModel intColumn = new ColumnModel().setId("100").setName("year")
				.setColumnType(ColumnType.INTEGER);
		List<ColumnModel> columns = List.of(intColumn);

		List<ColumnSemanticEnrichmentEntry> enrichment = List.of(
				new ColumnSemanticEnrichmentEntry().setColumnName("year")
						.setLanguageMode(SemanticEnrichmentLanguageMode.ENGLISH),
				new ColumnSemanticEnrichmentEntry().setColumnName("not_on_schema")
						.setLanguageMode(SemanticEnrichmentLanguageMode.ENGLISH));

		when(openSearchClient.generic()).thenReturn(genericClient);
		when(genericResponse.getStatus()).thenReturn(200);
		when(genericClient.execute(any(org.opensearch.client.opensearch.generic.Request.class)))
				.thenReturn(genericResponse);

		// call under test
		Optional<String> appliedJson = manager.createIndex(indexName, columns, qname,
				Collections.emptyList(), resolvedAnalyzers, enrichment);

		// Both entries dropped: no semantic_enrichment anywhere in the applied request.
		assertTrue(appliedJson.isPresent());
		assertFalse(appliedJson.get().contains("semantic_enrichment"),
				"Ineligible enrichment entries must be silently dropped: " + appliedJson.get());
	}

	private static BulkResponseItem okItem(String id) {
		return BulkResponseItem.of(b -> b
				.index("search-index-syn1")
				.id(id)
				.status(201)
				.operationType(org.opensearch.client.opensearch.core.bulk.OperationType.Index));
	}

	private static BulkResponseItem failedItem(String id, int status, String type, String reason) {
		return BulkResponseItem.of(b -> b
				.index("search-index-syn1")
				.id(id)
				.status(status)
				.operationType(org.opensearch.client.opensearch.core.bulk.OperationType.Index)
				.error(ErrorCause.of(e -> e.type(type).reason(reason))));
	}

	private static BulkOperation bulkOp(String id) {
		return BulkOperation.of(op -> op
				.index(idx -> idx.index("search-index-syn1").id(id).document(Map.of("_row_id", Long.parseLong(id)))));
	}

	private static BulkResponse bulkResponseOf(BulkResponseItem... items) {
		return BulkResponse.of(b -> b.errors(Arrays.stream(items).anyMatch(i -> i.error() != null))
				.took(1L).items(Arrays.asList(items)));
	}

	/**
	 * Build a {@link BulkResponse} whose items line up one-for-one with the operations in
	 * {@code request} — all failed with the given status/type/reason. Needed because
	 * {@code bulkIndex} may submit per-op requests after a partial batch failure.
	 */
	private static BulkResponse allFailedResponse(BulkRequest request, int status, String type, String reason) {
		BulkResponseItem[] items = request.operations().stream()
				.map(op -> {
					String id = op.index() != null ? op.index().id() : "?";
					return failedItem(id, status, type, reason);
				})
				.toArray(BulkResponseItem[]::new);
		return bulkResponseOf(items);
	}

	@Test
	public void testBulkIndexWithAllItemsSucceed() throws Exception {
		BulkResponse response = bulkResponseOf(okItem("1"), okItem("2"), okItem("3"));
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenReturn(response);

		// call under test
		long indexed = manager.bulkIndex("search-index-syn1",
				Arrays.asList(bulkOp("1"), bulkOp("2"), bulkOp("3")));

		assertEquals(3L, indexed);
	}

	@Test
	public void testBulkIndexWithAllRetryableFailuresExhaustsRetriesAndThrowsRecoverableMessageException() throws Exception {
		// Every per-op bulk response fails 429 for whatever doc ids were requested — covers both
		// the initial batch attempt and the per-document retries that follow.
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenAnswer(inv -> allFailedResponse(inv.getArgument(0), 429,
						"circuit_breaking_exception", "rate limited"));

		// call under test
		RecoverableMessageException ex = assertThrows(RecoverableMessageException.class,
				() -> manager.bulkIndex("search-index-syn1",
						Arrays.asList(bulkOp("1"), bulkOp("2"), bulkOp("3"))));
		assertTrue(ex.getMessage().contains(
				"failed after " + OpenSearchManagerImpl.BULK_INDEX_MAX_RETRIES + " attempts"),
				ex.getMessage());
		assertTrue(ex.getMessage().contains("3 document(s) still retryable out of 3"), ex.getMessage());
		// 1 batch attempt, then MAX_RETRIES-1 per-document attempts with 3 ops each.
		int expected = 1 + (OpenSearchManagerImpl.BULK_INDEX_MAX_RETRIES - 1) * 3;
		verify(openSearchClient, times(expected))
				.bulk(argThat((BulkRequest req) -> req != null));
	}

	@Test
	public void testBulkIndexWithMixedFailuresThrowsPermanentRuntimeException() throws Exception {
		BulkResponse response = bulkResponseOf(
				okItem("1"),
				failedItem("2", 429, "circuit_breaking_exception", "rate limited"),
				failedItem("3", 400, "mapper_parsing_exception", "failed to parse field [geneName]"));
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenReturn(response);

		// call under test
		RuntimeException ex = assertThrows(RuntimeException.class,
				() -> manager.bulkIndex("search-index-syn1",
						Arrays.asList(bulkOp("1"), bulkOp("2"), bulkOp("3"))));
		assertFalse(ex instanceof RecoverableMessageException,
				ex.getClass().getName() + ": " + ex.getMessage());
		assertTrue(ex.getMessage().contains("1 retryable"), ex.getMessage());
		assertTrue(ex.getMessage().contains("1 permanent"), ex.getMessage());
	}

	@ParameterizedTest
	@ValueSource(ints = {500, 502, 504})
	public void testBulkIndexWith5xxItemStatusExhaustsRetriesAndThrowsRecoverableMessageException(int status) throws Exception {
		// AOSS returns 500 with type="exception" and the generic "Internal error occurred while
		// processing request" reason when shard routing hasn't fully propagated after createIndex —
		// classified as retryable so the intra-batch retry loop backs off and resubmits the subset.
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenAnswer(inv -> allFailedResponse(inv.getArgument(0), status,
						"exception", "Internal error occurred while processing request"));

		// call under test
		RecoverableMessageException ex = assertThrows(RecoverableMessageException.class,
				() -> manager.bulkIndex("search-index-syn1",
						Arrays.asList(bulkOp("1"), bulkOp("2"), bulkOp("3"))));
		assertTrue(ex.getMessage().contains(
				"failed after " + OpenSearchManagerImpl.BULK_INDEX_MAX_RETRIES + " attempts"),
				ex.getMessage());
		assertTrue(ex.getMessage().contains("3 document(s) still retryable out of 3"), ex.getMessage());
		// 1 batch attempt, then MAX_RETRIES-1 per-document attempts with 3 ops each.
		int expected = 1 + (OpenSearchManagerImpl.BULK_INDEX_MAX_RETRIES - 1) * 3;
		verify(openSearchClient, times(expected))
				.bulk(argThat((BulkRequest req) -> req != null));
	}

	@Test
	public void testBulkIndexWithMixed500And400FailuresThrowsPermanentRuntimeException() throws Exception {
		BulkResponse response = bulkResponseOf(
				failedItem("1", 500, "exception", "Internal error occurred while processing request"),
				failedItem("2", 400, "mapper_parsing_exception", "failed to parse field [geneName]"));
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenReturn(response);

		// call under test
		RuntimeException ex = assertThrows(RuntimeException.class,
				() -> manager.bulkIndex("search-index-syn1",
						Arrays.asList(bulkOp("1"), bulkOp("2"))));
		assertFalse(ex instanceof RecoverableMessageException,
				ex.getClass().getName() + ": " + ex.getMessage());
		assertTrue(ex.getMessage().contains("1 retryable"), ex.getMessage());
		assertTrue(ex.getMessage().contains("1 permanent"), ex.getMessage());
	}

	@Test
	public void testBulkIndexPermanentMessageIncludesSampleFailures() throws Exception {
		BulkResponse response = bulkResponseOf(
				failedItem("1", 400, "mapper_parsing_exception", "failed to parse field [geneName]"),
				failedItem("2", 400, "mapper_parsing_exception", "failed to parse field [geneLength]"),
				failedItem("3", 400, "document_parsing_exception", "unexpected character"));
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenReturn(response);

		// call under test
		RuntimeException ex = assertThrows(RuntimeException.class,
				() -> manager.bulkIndex("search-index-syn1",
						Arrays.asList(bulkOp("1"), bulkOp("2"), bulkOp("3"))));
		assertFalse(ex instanceof RecoverableMessageException, ex.getClass().getName());
		String msg = ex.getMessage();
		assertTrue(msg.contains("Sample failures:"), msg);
		assertTrue(msg.contains("doc 1 [status=400]"), msg);
		assertTrue(msg.contains("doc 2 [status=400]"), msg);
		assertTrue(msg.contains("doc 3 [status=400]"), msg);
		assertTrue(msg.contains("failed to parse field [geneName]"), msg);
		assertTrue(msg.contains("failed to parse field [geneLength]"), msg);
		assertTrue(msg.contains("unexpected character"), msg);
	}

	@Test
	public void testBulkIndexPermanentMessageCapsAtFiveSamples() throws Exception {
		BulkResponseItem[] items = new BulkResponseItem[8];
		BulkOperation[] ops = new BulkOperation[8];
		for (int i = 0; i < 8; i++) {
			String id = String.valueOf(i + 1);
			items[i] = failedItem(id, 400, "mapper_parsing_exception", "field [c" + i + "]");
			ops[i] = bulkOp(id);
		}
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenReturn(bulkResponseOf(items));

		// call under test
		RuntimeException ex = assertThrows(RuntimeException.class,
				() -> manager.bulkIndex("search-index-syn1", Arrays.asList(ops)));
		String msg = ex.getMessage();
		assertTrue(msg.contains("doc 1 [status=400]"), msg);
		assertTrue(msg.contains("doc 5 [status=400]"), msg);
		assertFalse(msg.contains("doc 6 [status=400]"), msg);
		assertFalse(msg.contains("doc 8 [status=400]"), msg);
	}

	@Test
	public void testBulkIndexPermanentMessageIncludesOnlyPermanentSamples() throws Exception {
		BulkResponse response = bulkResponseOf(
				failedItem("1", 429, "circuit_breaking_exception", "rate limited"),
				failedItem("2", 429, "circuit_breaking_exception", "rate limited"),
				failedItem("3", 400, "mapper_parsing_exception", "failed to parse field [geneName]"),
				failedItem("4", 400, "mapper_parsing_exception", "failed to parse field [geneLength]"),
				failedItem("5", 400, "document_parsing_exception", "unexpected character"));
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenReturn(response);

		// call under test
		RuntimeException ex = assertThrows(RuntimeException.class,
				() -> manager.bulkIndex("search-index-syn1",
						Arrays.asList(bulkOp("1"), bulkOp("2"), bulkOp("3"), bulkOp("4"), bulkOp("5"))));
		String msg = ex.getMessage();
		assertTrue(msg.contains("2 retryable"), msg);
		assertTrue(msg.contains("3 permanent"), msg);
		assertTrue(msg.contains("doc 3 [status=400]"), msg);
		assertTrue(msg.contains("doc 4 [status=400]"), msg);
		assertTrue(msg.contains("doc 5 [status=400]"), msg);
		assertFalse(msg.contains("doc 1 [status=429]"), msg);
		assertFalse(msg.contains("doc 2 [status=429]"), msg);
		assertFalse(msg.contains("rate limited"), msg);
	}

	@Test
	public void testBulkIndexPermanentMessageTruncatesWhenOverBudget() throws Exception {
		char[] huge = new char[2000];
		Arrays.fill(huge, 'x');
		String bigReason = new String(huge);
		BulkResponse response = bulkResponseOf(
				failedItem("1", 400, "mapper_parsing_exception", bigReason),
				failedItem("2", 400, "mapper_parsing_exception", bigReason));
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenReturn(response);

		// call under test
		RuntimeException ex = assertThrows(RuntimeException.class,
				() -> manager.bulkIndex("search-index-syn1", Arrays.asList(bulkOp("1"), bulkOp("2"))));
		String msg = ex.getMessage();
		assertEquals(2500, msg.length(), "message length=" + msg.length());
		assertTrue(msg.endsWith("...[truncated]"), msg.substring(msg.length() - 20));
	}

	@ParameterizedTest
	@ValueSource(ints = {500, 502, 504})
	public void testBulkIndexWithEnvelope5xxExhaustsRetriesAndThrowsRecoverableMessageException(int status) throws Exception {
		ErrorResponse serverError = ErrorResponse.of(e -> e
				.error(err -> err.type("exception").reason("Internal error occurred while processing request"))
				.status(status));
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenThrow(new OpenSearchException(serverError));

		// call under test
		assertThrows(RecoverableMessageException.class,
				() -> manager.bulkIndex("search-index-syn1", Arrays.asList(bulkOp("1"))));
		verify(openSearchClient, times(OpenSearchManagerImpl.BULK_INDEX_MAX_RETRIES))
				.bulk(argThat((BulkRequest req) -> req != null));
	}

	@Test
	public void testBulkIndexWithEnvelope429ExhaustsRetriesAndThrowsRecoverableMessageException() throws Exception {
		ErrorResponse rateLimited = ErrorResponse.of(e -> e
				.error(err -> err.type("circuit_breaking_exception").reason("rate limited"))
				.status(429));
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenThrow(new OpenSearchException(rateLimited));

		// call under test
		assertThrows(RecoverableMessageException.class,
				() -> manager.bulkIndex("search-index-syn1", Arrays.asList(bulkOp("1"))));
		verify(openSearchClient, times(OpenSearchManagerImpl.BULK_INDEX_MAX_RETRIES))
				.bulk(argThat((BulkRequest req) -> req != null));
	}

	@Test
	public void testBulkIndexWithEnvelope400ThrowsPermanentRuntimeException() throws Exception {
		ErrorResponse badRequest = ErrorResponse.of(e -> e
				.error(err -> err.type("illegal_argument_exception").reason("bad request"))
				.status(400));
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenThrow(new OpenSearchException(badRequest));

		// call under test
		RuntimeException ex = assertThrows(RuntimeException.class,
				() -> manager.bulkIndex("search-index-syn1", Arrays.asList(bulkOp("1"))));
		assertFalse(ex instanceof RecoverableMessageException, ex.getClass().getName());
		verify(openSearchClient, times(1))
				.bulk(argThat((BulkRequest req) -> req != null));
	}

	@Test
	public void testBulkIndexWithIOExceptionExhaustsRetriesAndThrowsRecoverableMessageException() throws Exception {
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenThrow(new IOException("connection reset"));

		// call under test
		assertThrows(RecoverableMessageException.class,
				() -> manager.bulkIndex("search-index-syn1", Arrays.asList(bulkOp("1"))));
		verify(openSearchClient, times(OpenSearchManagerImpl.BULK_INDEX_MAX_RETRIES))
				.bulk(argThat((BulkRequest req) -> req != null));
	}

	@Test
	public void testBulkIndexWithEnvelopeStatusZeroExhaustsRetriesAndIsRecoverable() throws Exception {
		// An OpenSearchException whose status() == 0 means the transport never produced an
		// HTTP response — e.g. AwsSdk2Transport surfaced a connection-level failure as
		// OpenSearchException rather than IOException. Treating it like a 4xx would fail the
		// whole batch permanently on transient network blips, so it must retry.
		ErrorResponse noResponse = ErrorResponse.of(e -> e
				.error(err -> err.type("transport_exception").reason("no http response"))
				.status(0));
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenThrow(new OpenSearchException(noResponse));

		// call under test
		assertThrows(RecoverableMessageException.class,
				() -> manager.bulkIndex("search-index-syn1", Arrays.asList(bulkOp("1"))));
		verify(openSearchClient, times(OpenSearchManagerImpl.BULK_INDEX_MAX_RETRIES))
				.bulk(argThat((BulkRequest req) -> req != null));
	}

	@Test
	public void testBulkIndexWithEmptyOperationsReturnsZeroAndDoesNotCallClient() {
		// call under test
		long indexed = manager.bulkIndex("search-index-syn1", Collections.emptyList());

		assertEquals(0L, indexed);
		verifyZeroInteractions(openSearchClient);
	}

	@Test
	public void testBulkIndexWithTransientRetryableFailureRecoversOnSecondAttempt() throws Exception {
		// First attempt (batch): docs 1 and 3 fail 500, doc 2 succeeds. That triggers
		// per-document mode — the next two attempts submit docs 1 and 3 individually.
		BulkResponse firstResponse = bulkResponseOf(
				failedItem("1", 500, "exception", "Internal error occurred while processing request"),
				okItem("2"),
				failedItem("3", 503, "service_unavailable", "try later"));
		BulkResponse singleOk1 = bulkResponseOf(okItem("1"));
		BulkResponse singleOk3 = bulkResponseOf(okItem("3"));
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenReturn(firstResponse)
				.thenReturn(singleOk1)
				.thenReturn(singleOk3);

		// call under test
		long indexed = manager.bulkIndex("search-index-syn1",
				Arrays.asList(bulkOp("1"), bulkOp("2"), bulkOp("3")));

		assertEquals(3L, indexed);
		verify(openSearchClient, times(3))
				.bulk(argThat((BulkRequest req) -> req != null));
	}

	@Test
	public void testBulkIndexRetryResubmitsOnlyFailedOps() throws Exception {
		// Doc 2 succeeds on first batch attempt. On partial failure the retry switches to
		// per-document mode, so only docs 1 and 3 come back — each in its own single-op request.
		BulkResponse firstResponse = bulkResponseOf(
				failedItem("1", 500, "exception", "Internal error occurred while processing request"),
				okItem("2"),
				failedItem("3", 500, "exception", "Internal error occurred while processing request"));
		BulkResponse singleOk1 = bulkResponseOf(okItem("1"));
		BulkResponse singleOk3 = bulkResponseOf(okItem("3"));
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenReturn(firstResponse)
				.thenReturn(singleOk1)
				.thenReturn(singleOk3);

		// call under test
		manager.bulkIndex("search-index-syn1",
				Arrays.asList(bulkOp("1"), bulkOp("2"), bulkOp("3")));

		ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
		verify(openSearchClient, times(3)).bulk(captor.capture());
		List<BulkRequest> requests = captor.getAllValues();
		assertEquals(3, requests.get(0).operations().size(), "first attempt submits all operations");
		assertEquals(1, requests.get(1).operations().size(), "per-doc retry submits one op");
		assertEquals(1, requests.get(2).operations().size(), "per-doc retry submits one op");
	}

	@Test
	public void testBulkIndexWithPermanentFailureDoesNotRetry() throws Exception {
		// Mixed 500 (retryable) + 400 (permanent): one permanent failure disqualifies the batch
		// from retrying, so bulk() is called exactly once.
		BulkResponse response = bulkResponseOf(
				failedItem("1", 500, "exception", "Internal error occurred while processing request"),
				failedItem("2", 400, "mapper_parsing_exception", "failed to parse field [geneName]"));
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenReturn(response);

		// call under test
		RuntimeException ex = assertThrows(RuntimeException.class,
				() -> manager.bulkIndex("search-index-syn1", Arrays.asList(bulkOp("1"), bulkOp("2"))));
		assertFalse(ex instanceof RecoverableMessageException, ex.getClass().getName());
		verify(openSearchClient, times(1))
				.bulk(argThat((BulkRequest req) -> req != null));
	}

	// --- callSearchApi: trackTotalHits wire behavior ---

	@SuppressWarnings({"rawtypes", "unchecked"})
	private SearchResponse<Map> emptySearchResponse() {
		TotalHits total = TotalHits.of(t -> t.value(0L).relation(TotalHitsRelation.Eq));
		HitsMetadata<Map> hits = HitsMetadata.of(h -> h.total(total).hits(Collections.emptyList()));
		return SearchResponse.searchResponseOf(r -> r
				.took(0L)
				.timedOut(false)
				.shards(s -> s.total(1).successful(1).failed(0))
				.hits(hits));
	}

	@Test
	public void testCallSearchApiWithTotalHitsSetsCountToIntMaxValue() throws IOException {
		when(openSearchClient.search(argThat((SearchRequest req) -> req != null), eq(Map.class)))
				.thenReturn(emptySearchResponse());

		// call under test
		manager.callSearchApi("my-index", new BoolQuery.Builder(),
				0, 10, Collections.emptyMap(), null, null,
				Collections.emptyList(), Collections.emptyMap(),
				EnumSet.of(SearchQueryPart.TOTAL_HITS));

		ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
		verify(openSearchClient).search(captor.capture(), eq(Map.class));
		TrackHits trackHits = captor.getValue().trackTotalHits();
		assertNotNull(trackHits, "trackTotalHits must be set when TOTAL_HITS requested");
		assertTrue(trackHits.isCount(), "must use count() variant, not enabled()");
		assertEquals(Integer.MAX_VALUE, trackHits.count());
	}

	@Test
	public void testCallSearchApiWithoutTotalHitsSetsEnabledFalse() throws IOException {
		when(openSearchClient.search(argThat((SearchRequest req) -> req != null), eq(Map.class)))
				.thenReturn(emptySearchResponse());

		// call under test
		manager.callSearchApi("my-index", new BoolQuery.Builder(),
				0, 10, Collections.emptyMap(), null, null,
				Collections.emptyList(), Collections.emptyMap(),
				EnumSet.of(SearchQueryPart.HITS));

		ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
		verify(openSearchClient).search(captor.capture(), eq(Map.class));
		TrackHits trackHits = captor.getValue().trackTotalHits();
		assertNotNull(trackHits, "trackTotalHits must be explicitly disabled");
		assertTrue(trackHits.isEnabled(), "must use enabled() variant");
		assertEquals(Boolean.FALSE, trackHits.enabled());
	}

	@Test
	public void testBulkIndexWithIOExceptionThenSuccessRecovers() throws Exception {
		// Transient network issue on first two attempts, then success — covers the IOException
		// branch of the retry loop.
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenThrow(new IOException("connection reset"))
				.thenThrow(new IOException("connection reset"))
				.thenReturn(bulkResponseOf(okItem("1")));

		// call under test
		long indexed = manager.bulkIndex("search-index-syn1", Arrays.asList(bulkOp("1")));

		assertEquals(1L, indexed);
		verify(openSearchClient, times(3))
				.bulk(argThat((BulkRequest req) -> req != null));
	}

	@Test
	public void testDescribeBulkItemFailureWithNoShardFailures() {
		BulkResponseItem item = BulkResponseItem.of(b -> b
				.index("search-index-syn1")
				.id("1")
				.status(500)
				.operationType(org.opensearch.client.opensearch.core.bulk.OperationType.Index)
				.error(ErrorCause.of(e -> e
						.type("exception")
						.reason("Internal error occurred while processing request"))));

		// call under test
		String result = OpenSearchManagerImpl.describeBulkItemFailure(item);

		assertEquals(
				"doc 1 [status=500]: exception: Internal error occurred while processing request",
				result);
	}

	@Test
	public void testDescribeBulkItemFailureWithShardFailuresPopulated() {
		ShardSearchFailure shardFailure = ShardSearchFailure.of(sf -> sf
				.shard(3)
				.index("search-index-syn1")
				.node("node-a")
				.reason(ErrorCause.of(e -> e
						.type("mapper_parsing_exception")
						.reason("failed to parse field [geneName]"))));
		ShardStatistics shards = ShardStatistics.of(s -> s
				.total(5).successful(4).failed(1).failures(shardFailure));

		BulkResponseItem item = BulkResponseItem.of(b -> b
				.index("search-index-syn1")
				.id("42")
				.status(400)
				.operationType(org.opensearch.client.opensearch.core.bulk.OperationType.Index)
				.shards(shards)
				.error(ErrorCause.of(e -> e
						.type("exception")
						.reason("Internal error occurred while processing request"))));

		// call under test
		String result = OpenSearchManagerImpl.describeBulkItemFailure(item);

		assertTrue(result.contains("doc 42 [status=400]"), result);
		assertTrue(result.contains("Internal error occurred while processing request"), result);
		assertTrue(result.contains("shardFailures="), result);
		assertTrue(result.contains("shard=3"), result);
		assertTrue(result.contains("index=search-index-syn1"), result);
		assertTrue(result.contains("node=node-a"), result);
		assertTrue(result.contains("mapper_parsing_exception"), result);
		assertTrue(result.contains("failed to parse field [geneName]"), result);
	}

	// --- waitForIndexWritable ---

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static IndexResponse okIndexResponse() {
		return IndexResponse.of(b -> b
				.id(OpenSearchManagerImpl.READINESS_PROBE_DOC_ID)
				.index("search-index-syn1")
				.version(1L)
				.seqNo(0L)
				.primaryTerm(1L)
				.result(org.opensearch.client.opensearch._types.Result.Created)
				.shards(s -> s.total(1).successful(1).failed(0)));
	}

	private static DeleteResponse okDeleteResponse() {
		return DeleteResponse.of(b -> b
				.id(OpenSearchManagerImpl.READINESS_PROBE_DOC_ID)
				.index("search-index-syn1")
				.version(1L)
				.seqNo(1L)
				.primaryTerm(1L)
				.result(org.opensearch.client.opensearch._types.Result.Deleted)
				.shards(s -> s.total(1).successful(1).failed(0)));
	}

	@Test
	public void testWaitForIndexWritableWithImmediateSuccessDeletesSentinelAndReturns() throws Exception {
		when(openSearchClient.index(argThat((IndexRequest<?> req) -> req != null)))
				.thenReturn(okIndexResponse());
		when(openSearchClient.delete(argThat((DeleteRequest req) -> req != null)))
				.thenReturn(okDeleteResponse());

		// call under test
		manager.waitForIndexWritable("search-index-syn1");

		ArgumentCaptor<IndexRequest> indexCaptor = ArgumentCaptor.forClass(IndexRequest.class);
		verify(openSearchClient, times(1)).index(indexCaptor.capture());
		assertEquals("search-index-syn1", indexCaptor.getValue().index());
		assertEquals(OpenSearchManagerImpl.READINESS_PROBE_DOC_ID, indexCaptor.getValue().id());

		ArgumentCaptor<DeleteRequest> deleteCaptor = ArgumentCaptor.forClass(DeleteRequest.class);
		verify(openSearchClient, times(1)).delete(deleteCaptor.capture());
		assertEquals("search-index-syn1", deleteCaptor.getValue().index());
		assertEquals(OpenSearchManagerImpl.READINESS_PROBE_DOC_ID, deleteCaptor.getValue().id());
	}

	@Test
	public void testWaitForIndexWritableWithTransientFailureThenSuccess() throws Exception {
		OpenSearchException notFound = new OpenSearchException(
				ErrorResponse.of(er -> er.error(ErrorCause.of(e -> e
						.type("index_not_found_exception")
						.reason("no such index"))).status(404)));
		when(openSearchClient.index(argThat((IndexRequest<?> req) -> req != null)))
				.thenThrow(notFound)
				.thenThrow(notFound)
				.thenReturn(okIndexResponse());
		when(openSearchClient.delete(argThat((DeleteRequest req) -> req != null)))
				.thenReturn(okDeleteResponse());

		// call under test
		manager.waitForIndexWritable("search-index-syn1");

		verify(openSearchClient, times(3)).index(argThat((IndexRequest<?> req) -> req != null));
		verify(openSearchClient, times(1)).delete(argThat((DeleteRequest req) -> req != null));
	}

	@Test
	public void testWaitForIndexWritableExhaustsRetriesAndThrowsRecoverableMessageException() throws Exception {
		OpenSearchException notFound = new OpenSearchException(
				ErrorResponse.of(er -> er.error(ErrorCause.of(e -> e
						.type("index_not_found_exception")
						.reason("no such index"))).status(404)));
		when(openSearchClient.index(argThat((IndexRequest<?> req) -> req != null)))
				.thenThrow(notFound);

		// call under test
		RecoverableMessageException ex = assertThrows(RecoverableMessageException.class,
				() -> manager.waitForIndexWritable("search-index-syn1"));

		assertTrue(ex.getMessage().contains("did not accept writes within the retry budget"),
				ex.getMessage());
		verify(openSearchClient, times(OpenSearchManagerImpl.INDEX_WRITABLE_MAX_RETRIES))
				.index(argThat((IndexRequest<?> req) -> req != null));
		// No sentinel was ever written, so no cleanup delete is attempted.
		verify(openSearchClient, times(0)).delete(argThat((DeleteRequest req) -> req != null));
	}

	@Test
	public void testWaitForIndexWritableWithIOExceptionExhaustsRetries() throws Exception {
		when(openSearchClient.index(argThat((IndexRequest<?> req) -> req != null)))
				.thenThrow(new IOException("connection reset"));

		// call under test
		assertThrows(RecoverableMessageException.class,
				() -> manager.waitForIndexWritable("search-index-syn1"));

		verify(openSearchClient, times(OpenSearchManagerImpl.INDEX_WRITABLE_MAX_RETRIES))
				.index(argThat((IndexRequest<?> req) -> req != null));
	}

	@Test
	public void testWaitForIndexWritableSentinelCleanupFailureIsSwallowedAfterRetries() throws Exception {
		// Write succeeds, every cleanup delete fails. The probe must still return normally —
		// cleanup failures are non-fatal — but it must exhaust the cleanup retry budget first
		// so a transient delete failure doesn't immediately orphan the sentinel.
		when(openSearchClient.index(argThat((IndexRequest<?> req) -> req != null)))
				.thenReturn(okIndexResponse());
		when(openSearchClient.delete(argThat((DeleteRequest req) -> req != null)))
				.thenThrow(new IOException("cleanup failed"));

		// call under test
		manager.waitForIndexWritable("search-index-syn1");

		verify(openSearchClient, times(1)).index(argThat((IndexRequest<?> req) -> req != null));
		verify(openSearchClient, times(OpenSearchManagerImpl.SENTINEL_CLEANUP_MAX_RETRIES))
				.delete(argThat((DeleteRequest req) -> req != null));
	}

	@Test
	public void testWaitForIndexWritableSentinelCleanupRetriesAndSucceeds() throws Exception {
		// AOSS doesn't honor refresh=wait_for, so a single cleanup delete that fails on a
		// transient blip would orphan the sentinel. Verify the cleanup retries and lands on
		// the second attempt — only one orphan-window's worth of MATCH_ALL exposure.
		when(openSearchClient.index(argThat((IndexRequest<?> req) -> req != null)))
				.thenReturn(okIndexResponse());
		when(openSearchClient.delete(argThat((DeleteRequest req) -> req != null)))
				.thenThrow(new IOException("transient"))
				.thenReturn(okDeleteResponse());

		// call under test
		manager.waitForIndexWritable("search-index-syn1");

		verify(openSearchClient, times(1)).index(argThat((IndexRequest<?> req) -> req != null));
		verify(openSearchClient, times(2)).delete(argThat((DeleteRequest req) -> req != null));
	}

	// --- waitForDocumentCount ---

	@Test
	public void testWaitForDocumentCountWithImmediateMatchReturns() throws Exception {
		// Happy path: AOSS reports the expected count on the first attempt; no retry, no log noise.
		when(openSearchClient.count(any(org.opensearch.client.opensearch.core.CountRequest.class)))
				.thenReturn(org.opensearch.client.opensearch.core.CountResponse.of(b -> b
						.count(5L)
						.shards(s -> s.total(1).successful(1).failed(0))));

		// call under test
		manager.waitForDocumentCount("search-index-syn1", 5L);

		verify(openSearchClient, times(1))
				.count(any(org.opensearch.client.opensearch.core.CountRequest.class));
	}

	@Test
	public void testWaitForDocumentCountConvergesAfterUnderCount() throws Exception {
		// AOSS is eventually consistent: the first probe sees fewer documents than were
		// bulk-indexed; the second sees the full count and returns.
		when(openSearchClient.count(any(org.opensearch.client.opensearch.core.CountRequest.class)))
				.thenReturn(countResponse(2L))
				.thenReturn(countResponse(5L));

		// call under test
		manager.waitForDocumentCount("search-index-syn1", 5L);

		verify(openSearchClient, times(2))
				.count(any(org.opensearch.client.opensearch.core.CountRequest.class));
	}

	@Test
	public void testWaitForDocumentCountTreatsExcessAsConverged() throws Exception {
		// >= rather than == so a leftover readiness-probe sentinel cannot strand convergence
		// one short. An excess count is therefore a successful return, not a retry.
		when(openSearchClient.count(any(org.opensearch.client.opensearch.core.CountRequest.class)))
				.thenReturn(countResponse(6L));

		// call under test
		manager.waitForDocumentCount("search-index-syn1", 5L);

		verify(openSearchClient, times(1))
				.count(any(org.opensearch.client.opensearch.core.CountRequest.class));
	}

	@Test
	public void testWaitForDocumentCountExhaustsRetriesAndThrowsRecoverableMessageException() throws Exception {
		// Persistent under-count: every attempt sees fewer documents than expected. The probe
		// must throw RecoverableMessageException so the lifecycle worker re-queues the message
		// without writing FAILED — convergence is transient by definition.
		when(openSearchClient.count(any(org.opensearch.client.opensearch.core.CountRequest.class)))
				.thenReturn(countResponse(2L));

		// call under test
		RecoverableMessageException ex = assertThrows(RecoverableMessageException.class,
				() -> manager.waitForDocumentCount("search-index-syn1", 5L));

		assertTrue(ex.getMessage().contains("did not converge"), ex.getMessage());
		assertTrue(ex.getMessage().contains("2 of 5"), ex.getMessage());
		verify(openSearchClient, times(OpenSearchManagerImpl.COUNT_PROBE_MAX_RETRIES))
				.count(any(org.opensearch.client.opensearch.core.CountRequest.class));
	}

	@Test
	public void testWaitForDocumentCountWithIOExceptionExhaustsRetries() throws Exception {
		when(openSearchClient.count(any(org.opensearch.client.opensearch.core.CountRequest.class)))
				.thenThrow(new IOException("connection reset"));

		// call under test
		assertThrows(RecoverableMessageException.class,
				() -> manager.waitForDocumentCount("search-index-syn1", 5L));

		verify(openSearchClient, times(OpenSearchManagerImpl.COUNT_PROBE_MAX_RETRIES))
				.count(any(org.opensearch.client.opensearch.core.CountRequest.class));
	}

	@Test
	public void testWaitForDocumentCountWithOpenSearchExceptionExhaustsRetries() throws Exception {
		// AOSS may briefly return index_not_found_exception (or any other transient error) from
		// _count while shards are still propagating the freshly-written documents.
		ErrorResponse notFound = ErrorResponse.of(er -> er
				.error(ErrorCause.of(e -> e.type("index_not_found_exception").reason("no such index")))
				.status(404));
		when(openSearchClient.count(any(org.opensearch.client.opensearch.core.CountRequest.class)))
				.thenThrow(new OpenSearchException(notFound));

		// call under test
		assertThrows(RecoverableMessageException.class,
				() -> manager.waitForDocumentCount("search-index-syn1", 5L));

		verify(openSearchClient, times(OpenSearchManagerImpl.COUNT_PROBE_MAX_RETRIES))
				.count(any(org.opensearch.client.opensearch.core.CountRequest.class));
	}

	@Test
	public void testWaitForDocumentCountWithZeroExpectedShortCircuits() throws Exception {
		// An empty SearchIndex needs no probe — _count would just return 0 and we'd skip the
		// retry path anyway. Avoid the round-trip entirely.
		// call under test
		manager.waitForDocumentCount("search-index-syn1", 0L);

		verifyZeroInteractions(openSearchClient);
	}

	private static org.opensearch.client.opensearch.core.CountResponse countResponse(long count) {
		return org.opensearch.client.opensearch.core.CountResponse.of(b -> b
				.count(count)
				.shards(s -> s.total(1).successful(1).failed(0)));
	}

	// --- per-document fallback on partial batch failure ---

	@Test
	public void testBulkIndexSwitchesToPerDocumentModeAfterPartialBatchFailure() throws Exception {
		// First attempt (batch mode): doc 2 succeeds, docs 1 and 3 fail with 429 — retryable.
		// Second attempt (per-doc mode): two single-op bulk requests, both succeed.
		BulkResponse firstResponse = bulkResponseOf(
				failedItem("1", 429, "circuit_breaking_exception", "rate limited"),
				okItem("2"),
				failedItem("3", 429, "circuit_breaking_exception", "rate limited"));
		BulkResponse singleOk1 = bulkResponseOf(okItem("1"));
		BulkResponse singleOk3 = bulkResponseOf(okItem("3"));
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenReturn(firstResponse)
				.thenReturn(singleOk1)
				.thenReturn(singleOk3);

		// call under test
		long indexed = manager.bulkIndex("search-index-syn1",
				Arrays.asList(bulkOp("1"), bulkOp("2"), bulkOp("3")));

		assertEquals(3L, indexed);
		ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
		verify(openSearchClient, times(3)).bulk(captor.capture());
		List<BulkRequest> requests = captor.getAllValues();
		assertEquals(3, requests.get(0).operations().size(), "first attempt submits batch of 3");
		assertEquals(1, requests.get(1).operations().size(), "per-doc retry submits one op");
		assertEquals(1, requests.get(2).operations().size(), "per-doc retry submits one op");
	}

	@Test
	public void testBulkIndexPerDocumentModeContinuesPartitioningAfterPartialFailure() throws Exception {
		// First attempt (batch): doc 2 succeeds, docs 1 and 3 fail 429 (retryable).
		// Second attempt (per-doc): single op for doc 1 fails 429; single op for doc 3 succeeds.
		// Third attempt (per-doc): single op for doc 1 succeeds.
		BulkResponse firstResponse = bulkResponseOf(
				failedItem("1", 429, "circuit_breaking_exception", "rate limited"),
				okItem("2"),
				failedItem("3", 429, "circuit_breaking_exception", "rate limited"));
		BulkResponse single1Failed = bulkResponseOf(
				failedItem("1", 429, "circuit_breaking_exception", "rate limited"));
		BulkResponse single3Ok = bulkResponseOf(okItem("3"));
		BulkResponse single1Ok = bulkResponseOf(okItem("1"));
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenReturn(firstResponse)
				.thenReturn(single1Failed)
				.thenReturn(single3Ok)
				.thenReturn(single1Ok);

		// call under test
		long indexed = manager.bulkIndex("search-index-syn1",
				Arrays.asList(bulkOp("1"), bulkOp("2"), bulkOp("3")));

		assertEquals(3L, indexed);
		verify(openSearchClient, times(4)).bulk(argThat((BulkRequest req) -> req != null));
	}

	@Test
	public void testBulkIndexPerDocumentModeEnvelopeFailureDoesNotResubmitSucceededDocs() throws Exception {
		// Partial 429 triggers per-doc mode with docs 1 and 3 outstanding.
		// In per-doc mode, doc 1 succeeds, doc 3's single-op request throws an envelope 503
		// (retryable). The next attempt must only resubmit doc 3 — doc 1 was already indexed.
		BulkResponse firstResponse = bulkResponseOf(
				failedItem("1", 429, "circuit_breaking_exception", "rate limited"),
				okItem("2"),
				failedItem("3", 429, "circuit_breaking_exception", "rate limited"));
		BulkResponse single1Ok = bulkResponseOf(okItem("1"));
		BulkResponse single3Ok = bulkResponseOf(okItem("3"));
		ErrorResponse serverError = ErrorResponse.of(e -> e
				.error(err -> err.type("exception").reason("service unavailable"))
				.status(503));
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenReturn(firstResponse)
				.thenReturn(single1Ok)
				.thenThrow(new OpenSearchException(serverError))
				.thenReturn(single3Ok);

		// call under test
		long indexed = manager.bulkIndex("search-index-syn1",
				Arrays.asList(bulkOp("1"), bulkOp("2"), bulkOp("3")));

		assertEquals(3L, indexed);
		ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
		verify(openSearchClient, times(4)).bulk(captor.capture());
		List<BulkRequest> requests = captor.getAllValues();
		// attempt 1 (batch of 3) → attempt 2 per-doc: submits doc 1 (ok) then doc 3 (envelope 503 aborts)
		// → attempt 3 per-doc: must only carry doc 3 forward, not doc 1 again.
		assertEquals(3, requests.get(0).operations().size(), "batch attempt");
		assertEquals(1, requests.get(1).operations().size(), "per-doc: doc 1 ok");
		assertEquals(1, requests.get(2).operations().size(), "per-doc: doc 3 envelope 503");
		assertEquals(1, requests.get(3).operations().size(),
				"retry after envelope failure must only resubmit the unprocessed doc, not the succeeded one");
		assertEquals("3", requests.get(3).operations().get(0).index().id(),
				"succeeded doc 1 must not be resubmitted");
	}

	@Test
	public void testBulkIndexPerDocumentModePermanentFailureStopsRetries() throws Exception {
		// Partial 429 triggers per-doc mode. On the per-doc retry, doc 1's single op comes back 400
		// (permanent), so the whole bulkIndex call fails without further retries.
		BulkResponse firstResponse = bulkResponseOf(
				failedItem("1", 429, "circuit_breaking_exception", "rate limited"),
				okItem("2"));
		BulkResponse single1Permanent = bulkResponseOf(
				failedItem("1", 400, "mapper_parsing_exception", "failed to parse field [geneName]"));
		when(openSearchClient.bulk(argThat((BulkRequest req) -> req != null)))
				.thenReturn(firstResponse)
				.thenReturn(single1Permanent);

		// call under test
		RuntimeException ex = assertThrows(RuntimeException.class,
				() -> manager.bulkIndex("search-index-syn1",
						Arrays.asList(bulkOp("1"), bulkOp("2"))));
		assertFalse(ex instanceof RecoverableMessageException, ex.getClass().getName());
		verify(openSearchClient, times(2)).bulk(argThat((BulkRequest req) -> req != null));
	}

	// --- toTermsFilterValue ---
	//
	// Filter values are received as strings on the wire but must be sent to AOSS as the typed
	// FieldValue variant (Long/Double/Boolean/String) matching the column's scalar type. _LIST
	// columns use the non-list scalar type because the bulk indexer writes each list element with
	// that type. Without the right variant, INTEGER_LIST / DOUBLE_LIST / BOOLEAN_LIST terms filters
	// silently miss rows.

	/**
	 * Per-ColumnType raw filter input + the FieldValue variant the converter is expected to produce.
	 * EnumSet.allOf coverage guard fails if a new ColumnType is added without a fixture.
	 */
	private static Map<ColumnType, TermsFilterCase> termsFilterCases() {
		Map<ColumnType, TermsFilterCase> cases = new LinkedHashMap<>();
		// Text-y columns → String variant
		cases.put(ColumnType.STRING,        new TermsFilterCase("alpha",        FieldValue.Kind.String,  "alpha"));
		cases.put(ColumnType.STRING_LIST,   new TermsFilterCase("alpha",        FieldValue.Kind.String,  "alpha"));
		cases.put(ColumnType.MEDIUMTEXT,    new TermsFilterCase("alpha beta",   FieldValue.Kind.String,  "alpha beta"));
		cases.put(ColumnType.LARGETEXT,     new TermsFilterCase("alpha beta",   FieldValue.Kind.String,  "alpha beta"));
		cases.put(ColumnType.LINK,          new TermsFilterCase("https://x",    FieldValue.Kind.String,  "https://x"));
		// JSON column type — parser canonicalizes a JSON object string. The terms-filter use case
		// for a JSON column is degenerate, but the converter must still pick a variant; document it.
		cases.put(ColumnType.JSON,          new TermsFilterCase("{\"a\":1}",    FieldValue.Kind.String,  "{\"a\":1}"));
		// Long-backed numeric / id columns → Long variant
		cases.put(ColumnType.INTEGER,       new TermsFilterCase("123",          FieldValue.Kind.Long,    123L));
		cases.put(ColumnType.INTEGER_LIST,  new TermsFilterCase("123",          FieldValue.Kind.Long,    123L));
		cases.put(ColumnType.FILEHANDLEID,  new TermsFilterCase("9876543",      FieldValue.Kind.Long,    9876543L));
		cases.put(ColumnType.SUBMISSIONID,  new TermsFilterCase("555",          FieldValue.Kind.Long,    555L));
		cases.put(ColumnType.EVALUATIONID,  new TermsFilterCase("777",          FieldValue.Kind.Long,    777L));
		cases.put(ColumnType.USERID,        new TermsFilterCase("3412396",      FieldValue.Kind.Long,    3412396L));
		cases.put(ColumnType.USERID_LIST,   new TermsFilterCase("3412396",      FieldValue.Kind.Long,    3412396L));
		// EntityId — KeyFactory strips the "syn" prefix and returns Long
		cases.put(ColumnType.ENTITYID,      new TermsFilterCase("syn123456",    FieldValue.Kind.Long,    123456L));
		cases.put(ColumnType.ENTITYID_LIST, new TermsFilterCase("syn123456",    FieldValue.Kind.Long,    123456L));
		// Date — epoch-ms wire format → Long variant
		cases.put(ColumnType.DATE,          new TermsFilterCase("1609459200000", FieldValue.Kind.Long,   1609459200000L));
		cases.put(ColumnType.DATE_LIST,     new TermsFilterCase("1609459200000", FieldValue.Kind.Long,   1609459200000L));
		// Double / Boolean
		cases.put(ColumnType.DOUBLE,        new TermsFilterCase("1.5",          FieldValue.Kind.Double,  1.5));
		cases.put(ColumnType.BOOLEAN,       new TermsFilterCase("true",         FieldValue.Kind.Boolean, Boolean.TRUE));
		cases.put(ColumnType.BOOLEAN_LIST,  new TermsFilterCase("false",        FieldValue.Kind.Boolean, Boolean.FALSE));
		return cases;
	}

	private static final class TermsFilterCase {
		final String raw;
		final FieldValue.Kind expectedKind;
		final Object expectedValue;
		TermsFilterCase(String raw, FieldValue.Kind expectedKind, Object expectedValue) {
			this.raw = raw;
			this.expectedKind = expectedKind;
			this.expectedValue = expectedValue;
		}
	}

	@Test
	public void testToTermsFilterValueWithEveryColumnType() {
		Map<ColumnType, TermsFilterCase> cases = termsFilterCases();
		assertEquals(EnumSet.allOf(ColumnType.class), cases.keySet(),
				"Every Synapse ColumnType must be represented in this round-trip test");

		for (Map.Entry<ColumnType, TermsFilterCase> entry : cases.entrySet()) {
			ColumnType type = entry.getKey();
			TermsFilterCase tc = entry.getValue();
			ColumnModel column = new ColumnModel().setId("1").setName("c").setColumnType(type);

			// call under test
			FieldValue fv = OpenSearchManagerImpl.toTermsFilterValue(tc.raw, column);

			assertEquals(tc.expectedKind, fv._kind(), "kind for " + type);
			assertEquals(tc.expectedValue, fv._get(), "value for " + type);
		}
	}

	@Test
	public void testToTermsFilterValueWithNullColumnReturnsRawString() {
		// Relaxed-name path: filter key isn't on the schema → fall through as raw String.
		// call under test
		FieldValue fv = OpenSearchManagerImpl.toTermsFilterValue("anything", null);

		assertEquals(FieldValue.Kind.String, fv._kind());
		assertEquals("anything", fv.stringValue());
	}

	@Test
	public void testToTermsFilterValueWithDateAcceptsAllThreeWireFormats() {
		// DATE schema accepts epoch-ms, SQL date, or ISO-8601. All three must normalize to the
		// same Long on the wire so a UI sending any format hits the indexed term.
		ColumnModel column = new ColumnModel().setId("1").setName("d").setColumnType(ColumnType.DATE);
		long expectedMs = 1609459200000L;

		// call under test (epoch-ms)
		FieldValue epoch = OpenSearchManagerImpl.toTermsFilterValue("1609459200000", column);
		// call under test (SQL date)
		FieldValue sqlDate = OpenSearchManagerImpl.toTermsFilterValue("2021-01-01 00:00:00.000", column);
		// call under test (ISO-8601)
		FieldValue iso = OpenSearchManagerImpl.toTermsFilterValue("2021-01-01T00:00:00.000Z", column);

		assertEquals(FieldValue.Kind.Long, epoch._kind());
		assertEquals(FieldValue.Kind.Long, sqlDate._kind());
		assertEquals(FieldValue.Kind.Long, iso._kind());
		assertEquals(expectedMs, epoch.longValue());
		assertEquals(expectedMs, sqlDate.longValue());
		assertEquals(expectedMs, iso.longValue());
	}

	// --- toRangeBound ---
	//
	// KeyRange.min / .max are strings on the wire but must reach OpenSearch as JSON numbers for
	// numeric / date columns; otherwise OS may lex-compare on the keyword sub-field (e.g. "10" < "9").

	@Test
	public void testToRangeBoundWithEveryColumnType() {
		// Reuse the terms-filter fixture: range bounds run through the same parser, so the
		// underlying scalar must be the parsed value for every column type. Coverage guard via
		// EnumSet.allOf ensures any new ColumnType is forced to declare its expected behavior.
		Map<ColumnType, TermsFilterCase> cases = termsFilterCases();
		assertEquals(EnumSet.allOf(ColumnType.class), cases.keySet(),
				"Every Synapse ColumnType must be represented in this round-trip test");

		for (Map.Entry<ColumnType, TermsFilterCase> entry : cases.entrySet()) {
			ColumnType type = entry.getKey();
			TermsFilterCase tc = entry.getValue();
			ColumnModel column = new ColumnModel().setId("1").setName("c").setColumnType(type);
			ColumnType scalar = ColumnTypeListMappings.isList(type)
					? ColumnTypeListMappings.nonListType(type)
					: type;
			Object expectedParsed = ColumnTypeInfo.getInfoForType(scalar)
					.parseValueForDatabaseWrite(tc.raw);

			// call under test
			JsonData bound = OpenSearchManagerImpl.toRangeBound(tc.raw, column);

			// JsonData.of(value) keeps the underlying object for plain Java types; toString()
			// delegates to the wrapped value, so equals on string form is a stable assertion.
			assertEquals(String.valueOf(expectedParsed), bound.toString(),
					"range bound for " + type);
		}
	}

	@Test
	public void testToRangeBoundWithNullColumnReturnsRawString() {
		// call under test
		JsonData bound = OpenSearchManagerImpl.toRangeBound("anything", null);

		assertEquals("anything", bound.toString());
	}

	@Test
	public void testToRangeBoundWithDateAcceptsAllThreeWireFormats() {
		ColumnModel column = new ColumnModel().setId("1").setName("d").setColumnType(ColumnType.DATE);
		String expectedMs = "1609459200000";

		// call under test (epoch-ms)
		assertEquals(expectedMs,
				OpenSearchManagerImpl.toRangeBound("1609459200000", column).toString());
		// call under test (SQL date)
		assertEquals(expectedMs,
				OpenSearchManagerImpl.toRangeBound("2021-01-01 00:00:00.000", column).toString());
		// call under test (ISO-8601)
		assertEquals(expectedMs,
				OpenSearchManagerImpl.toRangeBound("2021-01-01T00:00:00.000Z", column).toString());
	}

	// --- search(): terms filter shape on the wire ---

	private static SearchQuery searchQueryWithTermsFilter(String columnName, List<String> values) {
		KeyValues kvs = new KeyValues().setKey(columnName).setValues(values);
		return new SearchQuery()
				.setQueryType(SearchQueryType.MATCH_ALL)
				.setQueryText("")
				.setOffset(0L).setLimit(10L)
				.setTermsFilters(Collections.singletonList(kvs));
	}

	private static TermsQuery extractSingleTermsFilter(SearchRequest captured) {
		BoolQuery bool = captured.query().bool();
		Query filter = bool.filter().get(0);
		assertTrue(filter.isTerms(), "expected terms filter, got " + filter._kind());
		return filter.terms();
	}

	@Test
	public void testSearchWithTermsFilterOnIntegerListColumnSendsLongFieldValue() throws IOException {
		// Regression: INTEGER_LIST cells are indexed as longs, so a string "123" filter would
		// silently miss. The terms-filter value must reach AOSS as Kind.Long.
		when(openSearchClient.search(argThat((SearchRequest req) -> req != null), eq(Map.class)))
				.thenReturn(emptySearchResponse());
		List<ColumnModel> columns = Collections.singletonList(
				new ColumnModel().setId("111").setName("ages").setColumnType(ColumnType.INTEGER_LIST));

		// call under test
		manager.search("search-index-syn1",
				searchQueryWithTermsFilter("ages", Arrays.asList("123", "456")),
				columns, EnumSet.of(SearchQueryPart.HITS));

		ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
		verify(openSearchClient).search(captor.capture(), eq(Map.class));
		TermsQuery terms = extractSingleTermsFilter(captor.getValue());
		assertEquals("111", terms.field(), "list columns filter on the bare id, no .keyword");
		List<FieldValue> values = terms.terms().value();
		assertEquals(2, values.size());
		assertEquals(FieldValue.Kind.Long, values.get(0)._kind());
		assertEquals(123L, values.get(0).longValue());
		assertEquals(FieldValue.Kind.Long, values.get(1)._kind());
		assertEquals(456L, values.get(1).longValue());
	}

	@Test
	public void testSearchWithTermsFilterOnUnknownColumnFallsBackToString() throws IOException {
		// Relaxed-name path: filter key isn't on the schema. Behavior must be the pre-typing
		// fallback — raw String FieldValue against the bare key — so old clients still work.
		when(openSearchClient.search(argThat((SearchRequest req) -> req != null), eq(Map.class)))
				.thenReturn(emptySearchResponse());

		// call under test
		manager.search("search-index-syn1",
				searchQueryWithTermsFilter("not_in_schema", Collections.singletonList("anything")),
				Collections.emptyList(), EnumSet.of(SearchQueryPart.HITS));

		ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
		verify(openSearchClient).search(captor.capture(), eq(Map.class));
		TermsQuery terms = extractSingleTermsFilter(captor.getValue());
		assertEquals("not_in_schema", terms.field());
		List<FieldValue> values = terms.terms().value();
		assertEquals(1, values.size());
		assertEquals(FieldValue.Kind.String, values.get(0)._kind());
		assertEquals("anything", values.get(0).stringValue());
	}

	// --- search(): range filter shape on the wire ---

	@Test
	public void testSearchWithRangeFilterOnIntegerColumnSendsNumericBound() throws IOException {
		// Regression: KeyRange.min/max are strings; without typed parsing AOSS may lex-compare
		// against a keyword sub-field, producing "10" < "9". The bound must reach the wire as a
		// JSON number. JsonData wraps the parsed Long, so toString() of the bound is "10".
		when(openSearchClient.search(argThat((SearchRequest req) -> req != null), eq(Map.class)))
				.thenReturn(emptySearchResponse());
		List<ColumnModel> columns = Collections.singletonList(
				new ColumnModel().setId("111").setName("age").setColumnType(ColumnType.INTEGER));
		KeyRange kr = new KeyRange().setKey("age").setMin("10").setMax("99");
		SearchQuery query = new SearchQuery()
				.setQueryType(SearchQueryType.MATCH_ALL)
				.setQueryText("")
				.setOffset(0L).setLimit(10L)
				.setRangeFilters(Collections.singletonList(kr));

		// call under test
		manager.search("search-index-syn1", query, columns, EnumSet.of(SearchQueryPart.HITS));

		ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
		verify(openSearchClient).search(captor.capture(), eq(Map.class));
		Query filter = captor.getValue().query().bool().filter().get(0);
		assertTrue(filter.isRange(), "expected range filter, got " + filter._kind());
		RangeQuery range = filter.range();
		assertEquals("111", range.field());
		assertEquals("10", range.gte().toString(), "lower bound must be JSON number, not quoted string");
		assertEquals("99", range.lte().toString(), "upper bound must be JSON number, not quoted string");
	}

	// --- buildAggregations: JSON column rejection ---

	@Test
	public void testBuildAggregationsWithJsonColumnRejected() {
		// JSON columns map to `object`/`dynamic`, not a leaf doc-values field, so a `terms` agg
		// returns zero buckets silently. Reject up-front rather than confusing the caller.
		Map<String, ColumnModel> columnMap = new HashMap<>();
		columnMap.put("111",
				new ColumnModel().setId("111").setName("blob").setColumnType(ColumnType.JSON));
		Map<String, String> nameToId = new HashMap<>();
		nameToId.put("blob", "111");
		FacetRequest facet = new FacetRequest().setColumnName("blob");

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> manager.buildAggregations(Collections.singletonList(facet), columnMap, nameToId));

		assertTrue(ex.getMessage().contains("Cannot facet on JSON column"), ex.getMessage());
		assertTrue(ex.getMessage().contains("blob"), ex.getMessage());
		verifyZeroInteractions(openSearchClient);
	}

	@Test
	public void testBuildAggregationsWithUnknownColumnAllowed() {
		// Relaxed-name path: column isn't on the schema. Don't synthesize a JSON-rejection — the
		// guard only fires when we *know* the column is JSON-typed.
		FacetRequest facet = new FacetRequest().setColumnName("not_in_schema").setMaxValueCount(5L)
				.setSortField(FacetSortField.COUNT).setSortDirection(SortDirection.DESC);

		// call under test
		Map<String, Aggregation> aggs = manager.buildAggregations(
				Collections.singletonList(facet), Collections.emptyMap(), Collections.emptyMap());

		assertNotNull(aggs.get("not_in_schema"));
		assertEquals("not_in_schema", aggs.get("not_in_schema").terms().field());
	}

	// --- search(): offset / limit validation ---

	private static SearchQuery matchAllQuery() {
		return new SearchQuery()
				.setQueryType(SearchQueryType.MATCH_ALL)
				.setQueryText("")
				.setOffset(0L).setLimit(10L);
	}

	@Test
	public void testSearchWithNegativeOffsetThrows() throws IOException {
		SearchQuery query = matchAllQuery().setOffset(-1L);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> manager.search("search-index-syn1", query,
						Collections.emptyList(), EnumSet.of(SearchQueryPart.HITS)));

		assertTrue(ex.getMessage().contains("offset"), ex.getMessage());
		verify(openSearchClient, org.mockito.Mockito.never()).search(any(SearchRequest.class), eq(Map.class));
	}

	@Test
	public void testSearchWithOffsetAboveIntMaxThrows() throws IOException {
		SearchQuery query = matchAllQuery().setOffset((long) Integer.MAX_VALUE + 1L);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> manager.search("search-index-syn1", query,
						Collections.emptyList(), EnumSet.of(SearchQueryPart.HITS)));

		assertTrue(ex.getMessage().contains("offset"), ex.getMessage());
		verify(openSearchClient, org.mockito.Mockito.never()).search(any(SearchRequest.class), eq(Map.class));
	}

	@Test
	public void testSearchWithNegativeLimitThrows() throws IOException {
		SearchQuery query = matchAllQuery().setLimit(-1L);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> manager.search("search-index-syn1", query,
						Collections.emptyList(), EnumSet.of(SearchQueryPart.HITS)));

		assertTrue(ex.getMessage().contains("limit"), ex.getMessage());
		verify(openSearchClient, org.mockito.Mockito.never()).search(any(SearchRequest.class), eq(Map.class));
	}

	@Test
	public void testSearchWithLimitAboveMaxClampsToMaxLimit() throws IOException {
		// Limit above MAX_LIMIT must clamp (not throw) so the existing relaxed contract is
		// preserved — the new requirement() check only rejects negative limits.
		when(openSearchClient.search(argThat((SearchRequest req) -> req != null), eq(Map.class)))
				.thenReturn(emptySearchResponse());
		SearchQuery query = matchAllQuery().setLimit(10_000L);

		// call under test
		manager.search("search-index-syn1", query,
				Collections.emptyList(), EnumSet.of(SearchQueryPart.HITS));

		ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
		verify(openSearchClient).search(captor.capture(), eq(Map.class));
		// MAX_LIMIT is 100 in OpenSearchManagerImpl; assert against the clamped value on the wire.
		assertEquals(Integer.valueOf(100), captor.getValue().size());
	}

	// ===================== branch coverage: buildProperty per ColumnType =====================

	/**
	 * {@code buildProperty}'s if-chain dispatches by column-type category (text / link /
	 * keyword / long / double / boolean / json / fallback). Every {@link ColumnType} value
	 * must yield a non-null {@link org.opensearch.client.opensearch._types.mapping.Property}
	 * — the parameterized run also pins the dispatch against future ColumnType additions.
	 */
	@ParameterizedTest
	@EnumSource(ColumnType.class)
	public void testBuildPropertyForEveryColumnType(ColumnType type) {
		// hasDefaultSearch=false and qname=null hit the simplest branch for each category.
		org.opensearch.client.opensearch._types.mapping.Property property =
				manager.buildProperty(type, null, false);
		assertNotNull(property, "every column type must yield a Property");
	}

	@Test
	public void testBuildPropertyTextWithBoundQname() {
		// Bound qname forces buildTextProperty's "explicit search_analyzer" path so the
		// search-time analyzer doesn't fall through to the index-wide default.
		assertNotNull(manager.buildProperty(ColumnType.STRING, "biomed-pubs", false));
	}

	@Test
	public void testBuildPropertyTextWithDefaultSearchEnabled() {
		// hasDefaultSearch=true exercises the asymmetric-analyzer branch where the bound
		// search analyzer key differs from the index analyzer key.
		assertNotNull(manager.buildProperty(ColumnType.STRING, "biomed-pubs", true));
	}

	// ===================== branch coverage: isConcurrentDeleteError null edges =====================

	@Test
	public void testIsConcurrentDeleteErrorWithNullReason() {
		// Defensive: error.reason() == null returns false instead of NPE.
		ErrorResponse response = new ErrorResponse.Builder()
				.status(500)
				.error(new ErrorCause.Builder().type("internal_error").build())
				.build();
		assertFalse(OpenSearchManagerImpl.isConcurrentDeleteError(new OpenSearchException(response)));
	}

	// ===================== branch coverage: isRetryableItemStatus boundaries =====================

	@Test
	public void testIsRetryableItemStatusUpperBoundary() {
		// MAX_SERVER_ERROR is 599; 600 falls outside the retryable range.
		assertTrue(OpenSearchManagerImpl.isRetryableItemStatus(599));
		assertFalse(OpenSearchManagerImpl.isRetryableItemStatus(600));
	}

	@Test
	public void testIsRetryableItemStatusLowerBoundary() {
		// 499 is below the 5xx retry range; 500 is the lower edge.
		assertFalse(OpenSearchManagerImpl.isRetryableItemStatus(499));
		assertTrue(OpenSearchManagerImpl.isRetryableItemStatus(500));
	}

	@Test
	public void testIsRetryableItemStatusZero() {
		// 0 isn't a real HTTP status; the bulk path handles 0 separately, so this returns false.
		assertFalse(OpenSearchManagerImpl.isRetryableItemStatus(0));
	}

	// ===================== branch coverage: describeError / appendErrorCauseDetail =====================

	@Test
	public void testDescribeErrorWithRootCauseEntries() {
		// rootCause iteration walks two entries with first/non-first separator branches.
		ErrorCause root1 = new ErrorCause.Builder().type("rc1").reason("r1").build();
		ErrorCause root2 = new ErrorCause.Builder().type("rc2").reason("r2").build();
		ErrorCause cause = new ErrorCause.Builder()
				.type("primary").reason("primary reason")
				.rootCause(Arrays.asList(root1, root2)).build();

		String desc = OpenSearchManagerImpl.describeError(cause);

		assertTrue(desc.contains("rootCause="));
		assertTrue(desc.contains("rc1: r1"));
		assertTrue(desc.contains(", rc2: r2"));
	}

	@Test
	public void testDescribeErrorWithMetadataPresent() {
		// metadata-present branch in appendErrorCauseDetail.
		ErrorCause cause = new ErrorCause.Builder()
				.type("t").reason("r")
				.metadata(Collections.singletonMap("k", JsonData.of("v")))
				.build();

		String desc = OpenSearchManagerImpl.describeError(cause);

		assertTrue(desc.contains("metadata="), "metadata branch must render: " + desc);
	}

	@Test
	public void testDescribeErrorWithStackTrace() {
		// stackTrace-present branch in appendErrorCauseDetail.
		ErrorCause cause = new ErrorCause.Builder()
				.type("t").reason("r")
				.stackTrace("at com.example.Foo.bar(Foo.java:10)")
				.build();

		String desc = OpenSearchManagerImpl.describeError(cause);

		assertTrue(desc.contains("stackTrace="), "stackTrace branch must render: " + desc);
	}

	@Test
	public void testDescribeErrorChainsMultipleCausedByLinks() {
		// caused-by walk loop runs through the chain, attaching " caused by " between links.
		ErrorCause innerInner = new ErrorCause.Builder().type("i2").reason("r2").build();
		ErrorCause inner = new ErrorCause.Builder()
				.type("i1").reason("r1").causedBy(innerInner).build();
		ErrorCause outer = new ErrorCause.Builder()
				.type("outer").reason("outerReason").causedBy(inner).build();

		String desc = OpenSearchManagerImpl.describeError(outer);

		assertTrue(desc.contains("outer: outerReason"));
		assertTrue(desc.contains("i1: r1"));
		assertTrue(desc.contains("i2: r2"));
		// Each caused-by link appears once.
		int causedByCount = 0;
		int idx = 0;
		while ((idx = desc.indexOf(" caused by ", idx)) != -1) {
			causedByCount++;
			idx += " caused by ".length();
		}
		assertEquals(2, causedByCount, "two caused-by links should join three errors: " + desc);
	}

	// ===================== branch coverage: describeBulkItemFailure =====================

	// ===================== branch coverage: addExistsFilters =====================

	@Test
	public void testAddExistsFiltersWithNullFieldsIsNoop() {
		// fields == null short-circuit — no filter / mustNot calls on the BoolQuery.Builder.
		BoolQuery.Builder b = new BoolQuery.Builder();

		manager.addExistsFilters(b, null, Collections.emptyMap(), false);

		BoolQuery q = b.build();
		assertTrue(q.filter().isEmpty(), "no fields → no filters");
		assertTrue(q.mustNot().isEmpty(), "no fields → no mustNot");
	}

	@Test
	public void testAddExistsFiltersAddsFilterWhenNotNegated() {
		// negate=false branch routes the exists query into filter().
		BoolQuery.Builder b = new BoolQuery.Builder();
		Map<String, String> nameToId = Collections.singletonMap("title", "100");

		manager.addExistsFilters(b, Arrays.asList("title"), nameToId, false);

		BoolQuery q = b.build();
		assertEquals(1, q.filter().size());
		assertTrue(q.mustNot().isEmpty());
	}

	@Test
	public void testAddExistsFiltersAddsMustNotWhenNegated() {
		// negate=true branch routes the exists query into mustNot().
		BoolQuery.Builder b = new BoolQuery.Builder();
		Map<String, String> nameToId = Collections.singletonMap("title", "100");

		manager.addExistsFilters(b, Arrays.asList("title"), nameToId, true);

		BoolQuery q = b.build();
		assertEquals(1, q.mustNot().size());
		assertTrue(q.filter().isEmpty());
	}

	@Test
	public void testAddExistsFiltersFallsBackToRawNameWhenNotInMap() {
		// nameToId.getOrDefault branch — when the field name isn't in the map, the field
		// name itself is used as the field id (legacy migration path).
		BoolQuery.Builder b = new BoolQuery.Builder();

		manager.addExistsFilters(b, Arrays.asList("unmappedField"), Collections.emptyMap(), false);

		BoolQuery q = b.build();
		assertEquals(1, q.filter().size());
	}

	// ===================== branch coverage: autocomplete limit clamp =====================

	@Test
	public void testAutocompleteWithNullLimitClampsToMax() throws Exception {
		// limit == null branch clamps to AUTOCOMPLETE_MAX_LIMIT (8).
		when(openSearchClient.search(argThat((SearchRequest req) -> req != null), eq(Map.class)))
				.thenReturn(emptySearchResponse());
		SearchQuery query = new SearchQuery()
				.setQueryText("foo")
				.setQueryType(SearchQueryType.PREFIX); // limit unset

		manager.autocomplete("search-index-syn1", query,
				Collections.emptyList(), EnumSet.of(SearchQueryPart.HITS));

		ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
		verify(openSearchClient).search(captor.capture(), eq(Map.class));
		assertEquals(Integer.valueOf(8), captor.getValue().size(),
				"null limit must clamp to AUTOCOMPLETE_MAX_LIMIT");
	}

	@Test
	public void testAutocompleteWithLimitAboveMaxClampsToMax() throws Exception {
		// limit > AUTOCOMPLETE_MAX_LIMIT branch — same clamp.
		when(openSearchClient.search(argThat((SearchRequest req) -> req != null), eq(Map.class)))
				.thenReturn(emptySearchResponse());
		SearchQuery query = new SearchQuery()
				.setQueryText("foo")
				.setQueryType(SearchQueryType.PREFIX)
				.setLimit(50L);

		manager.autocomplete("search-index-syn1", query,
				Collections.emptyList(), EnumSet.of(SearchQueryPart.HITS));

		ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
		verify(openSearchClient).search(captor.capture(), eq(Map.class));
		assertEquals(Integer.valueOf(8), captor.getValue().size(),
				"limit above AUTOCOMPLETE_MAX_LIMIT must clamp");
	}

	@Test
	public void testAutocompleteWithLimitWithinRangePassesThrough() throws Exception {
		// limit <= AUTOCOMPLETE_MAX_LIMIT — no clamp.
		when(openSearchClient.search(argThat((SearchRequest req) -> req != null), eq(Map.class)))
				.thenReturn(emptySearchResponse());
		SearchQuery query = new SearchQuery()
				.setQueryText("foo")
				.setQueryType(SearchQueryType.PREFIX)
				.setLimit(5L);

		manager.autocomplete("search-index-syn1", query,
				Collections.emptyList(), EnumSet.of(SearchQueryPart.HITS));

		ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
		verify(openSearchClient).search(captor.capture(), eq(Map.class));
		assertEquals(Integer.valueOf(5), captor.getValue().size());
	}

	@Test
	public void testAutocompleteForcesPrefixQueryTypeRegardlessOfInput() throws Exception {
		// query.setQueryType(SearchQueryType.PREFIX) is unconditional — even an input that
		// arrives with a different queryType is overwritten before executeSearch runs.
		when(openSearchClient.search(argThat((SearchRequest req) -> req != null), eq(Map.class)))
				.thenReturn(emptySearchResponse());
		SearchQuery query = new SearchQuery()
				.setQueryText("foo")
				.setQueryType(SearchQueryType.SIMPLE_QUERY_STRING)
				.setLimit(3L);

		manager.autocomplete("search-index-syn1", query,
				Collections.emptyList(), EnumSet.of(SearchQueryPart.HITS));

		assertEquals(SearchQueryType.PREFIX, query.getQueryType(),
				"autocomplete must always force PREFIX queryType");
	}

	@Test
	public void testDescribeBulkItemFailureWithNoShardFailuresOmitsSection() {
		// shards==null OR shards.failures empty → no "shardFailures=" section.
		ErrorCause err = new ErrorCause.Builder().type("indexing_error").reason("bad doc").build();
		BulkResponseItem item = new BulkResponseItem.Builder()
				.id("doc-42")
				.operationType(org.opensearch.client.opensearch.core.bulk.OperationType.Index)
				.status(400)
				.error(err)
				.index("test-index")
				.build();

		String desc = OpenSearchManagerImpl.describeBulkItemFailure(item);

		assertTrue(desc.contains("doc doc-42"));
		assertTrue(desc.contains("status=400"));
		assertTrue(desc.contains("indexing_error"));
		assertFalse(desc.contains("shardFailures="),
				"no shardFailures section when shards.failures is empty: " + desc);
	}
}
