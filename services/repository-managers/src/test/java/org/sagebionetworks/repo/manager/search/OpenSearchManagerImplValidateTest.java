package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.indices.AnalyzeRequest;
import org.opensearch.client.opensearch.indices.AnalyzeResponse;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link OpenSearchManagerImpl#validateAnalyzerSettings(JsonNode)}, the
 * AOSS {@code _analyze} probe that surfaces real OpenSearch-side analyzer errors at
 * TextAnalyzer create/update time. Input is the post-{@code $ref}-resolution settings
 * tree produced by {@link SearchAnalyzerJsonUtil#resolveRefs}.
 */
@ExtendWith(MockitoExtension.class)
public class OpenSearchManagerImplValidateTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Mock
	private OpenSearchClient openSearchClient;
	@Mock
	private OpenSearchIndicesClient indicesClient;
	@Mock
	private AnalyzeResponse analyzeResponse;

	private OpenSearchManagerImpl manager;

	private long originalValidateInitialBackoffMs;

	@BeforeEach
	void setUp() {
		manager = new OpenSearchManagerImpl(openSearchClient);
		// Drop validate retry backoff to 1ms so the IOException-exhaustion path
		// doesn't actually sleep through the exponential schedule on every run.
		originalValidateInitialBackoffMs = OpenSearchManagerImpl.VALIDATE_INITIAL_BACKOFF_MS;
		OpenSearchManagerImpl.VALIDATE_INITIAL_BACKOFF_MS = 1L;
	}

	@AfterEach
	void tearDown() {
		OpenSearchManagerImpl.VALIDATE_INITIAL_BACKOFF_MS = originalValidateInitialBackoffMs;
	}

	private void setupAnalyzeSuccess() throws IOException {
		when(openSearchClient.indices()).thenReturn(indicesClient);
		when(indicesClient.analyze(any(AnalyzeRequest.class))).thenReturn(analyzeResponse);
	}

	/**
	 * Parse a settings string the same way the production pipeline does: parse to
	 * JsonNode, splice in any $ref entries (none in these tests — resolver returns null),
	 * then deserialize to the typed IndexSettingsAnalysis the manager method takes.
	 */
	private static IndexSettingsAnalysis parse(String json) {
		try {
			JsonNode root = MAPPER.readTree(json);
			return SearchOpaqueJsonUtil.resolveAnalyzerSettings(root, qname -> null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testValidateWithStandardTokenizerSuccess() throws IOException {
		setupAnalyzeSuccess();

		// Bare built-in tokenizer reference; no inline registry needed.
		IndexSettingsAnalysis settings = parse("{\"analyzer\":{\"default\":{\"type\":\"custom\","
				+ "\"tokenizer\":\"standard\"}}}");

		// call under test
		assertDoesNotThrow(() -> manager.validateAnalyzerSettings(settings));
	}

	@Test
	public void testValidateWithInlineFilterRegistrySuccess() throws IOException {
		setupAnalyzeSuccess();

		// my_stop is owned by this analyzer's filter registry — submitted inline. lowercase
		// is a built-in — submitted by name.
		IndexSettingsAnalysis settings = parse("{"
				+ "\"filter\":{\"my_stop\":{\"type\":\"stop\",\"stopwords\":\"_english_\"}},"
				+ "\"analyzer\":{\"default\":{\"type\":\"custom\","
				+ "\"tokenizer\":\"standard\","
				+ "\"filter\":[\"my_stop\",\"lowercase\"]}}}");

		// call under test
		assertDoesNotThrow(() -> manager.validateAnalyzerSettings(settings));
	}

	@Test
	public void testValidateWithInlineTokenizerRegistrySuccess() throws IOException {
		setupAnalyzeSuccess();

		// Inline tokenizer definition rather than a built-in — exercises the typed deserialize
		// path through TokenizerDefinition.
		IndexSettingsAnalysis settings = parse("{"
				+ "\"tokenizer\":{\"my_ngram\":{\"type\":\"edge_ngram\",\"min_gram\":2,"
				+ "\"max_gram\":20,\"token_chars\":[\"letter\",\"digit\"]}},"
				+ "\"analyzer\":{\"default\":{\"type\":\"custom\","
				+ "\"tokenizer\":\"my_ngram\","
				+ "\"filter\":[\"lowercase\"]}}}");

		// call under test
		assertDoesNotThrow(() -> manager.validateAnalyzerSettings(settings));
	}

	@Test
	public void testValidateWithInlineCharFilterRegistrySuccess() throws IOException {
		setupAnalyzeSuccess();

		IndexSettingsAnalysis settings = parse("{"
				+ "\"char_filter\":{\"my_mapping\":{\"type\":\"mapping\",\"mappings\":[\"& => and\"]}},"
				+ "\"analyzer\":{\"default\":{\"type\":\"custom\","
				+ "\"tokenizer\":\"standard\","
				+ "\"char_filter\":[\"my_mapping\"]}}}");

		// call under test
		assertDoesNotThrow(() -> manager.validateAnalyzerSettings(settings));
	}

	@Test
	public void testValidateWithMinimalDefaultEntrySuccess() throws IOException {
		setupAnalyzeSuccess();

		// analyzer.default with the bare-minimum required tokenizer field.
		IndexSettingsAnalysis settings = parse("{\"analyzer\":{\"default\":{\"type\":\"custom\","
				+ "\"tokenizer\":\"standard\"}}}");

		// call under test
		assertDoesNotThrow(() -> manager.validateAnalyzerSettings(settings));
	}

	@Test
	public void testValidateThrowsIllegalArgumentOnOpenSearchException() throws IOException {
		when(openSearchClient.indices()).thenReturn(indicesClient);
		ErrorResponse errorResponse = ErrorResponse.of(e -> e
				.error(err -> err.type("illegal_argument_exception").reason("Unknown tokenizer type [foobar]"))
				.status(400));
		when(indicesClient.analyze(any(AnalyzeRequest.class)))
				.thenThrow(new OpenSearchException(errorResponse));

		IndexSettingsAnalysis settings = parse("{\"analyzer\":{\"default\":{\"type\":\"custom\","
				+ "\"tokenizer\":\"foobar\"}}}");

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> manager.validateAnalyzerSettings(settings));
		assertTrue(ex.getMessage().contains("Unknown tokenizer type [foobar]"));
		assertTrue(ex.getMessage().contains("Invalid analyzer configuration"));
	}

	@Test
	public void testValidateThrowsIllegalStateOnIOException() throws IOException {
		when(openSearchClient.indices()).thenReturn(indicesClient);
		when(indicesClient.analyze(any(AnalyzeRequest.class)))
				.thenThrow(new IOException("Connection refused"));

		IndexSettingsAnalysis settings = parse("{\"analyzer\":{\"default\":{\"type\":\"custom\","
				+ "\"tokenizer\":\"standard\"}}}");

		// call under test
		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> manager.validateAnalyzerSettings(settings));
		assertTrue(ex.getMessage().contains("temporarily unavailable"));
	}

	@Test
	public void testValidateThrowsOnNullSettings() {
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> manager.validateAnalyzerSettings(null));
	}

	@Test
	public void testValidateThrowsWhenAnalyzerDefaultMissing() {
		// Defense-in-depth — TextAnalyzerManagerImpl.validateSettings already enforces this,
		// but the AOSS-facing seam should not silently no-op when its core requirement
		// (an analyzer.default entry) isn't present.
		IndexSettingsAnalysis settings = parse("{\"filter\":{\"english_stop\":{\"type\":\"stop\"}}}");

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> manager.validateAnalyzerSettings(settings));
		assertTrue(ex.getMessage().contains("analyzer.default"));
	}

	@Test
	public void testValidateRetriesOnIndexNotFoundThenSucceeds() throws IOException {
		// AOSS occasionally returns transient index_not_found_exception from the cluster-
		// level _analyze endpoint while a system index is being provisioned. The retry must
		// absorb a single hit and the second attempt must succeed.
		when(openSearchClient.indices()).thenReturn(indicesClient);
		ErrorResponse indexNotFound = ErrorResponse.of(e -> e
				.error(err -> err.type("index_not_found_exception").reason("no such index"))
				.status(404));
		when(indicesClient.analyze(any(AnalyzeRequest.class)))
				.thenThrow(new OpenSearchException(indexNotFound))
				.thenReturn(analyzeResponse);

		IndexSettingsAnalysis settings = parse("{\"analyzer\":{\"default\":{\"type\":\"custom\","
				+ "\"tokenizer\":\"standard\"}}}");

		// call under test — first attempt throws index_not_found, second succeeds
		assertDoesNotThrow(() -> manager.validateAnalyzerSettings(settings));
		verify(indicesClient, times(2)).analyze(any(AnalyzeRequest.class));
	}

	@Test
	public void testValidateThrowsIllegalStateWhenIndexNotFoundExhaustsRetries() throws IOException {
		// All retries exhausted — must surface as IllegalStateException, not the underlying
		// IllegalArgumentException-shaped index_not_found message that a curator can't act on.
		when(openSearchClient.indices()).thenReturn(indicesClient);
		ErrorResponse indexNotFound = ErrorResponse.of(e -> e
				.error(err -> err.type("index_not_found_exception").reason("no such index"))
				.status(404));
		when(indicesClient.analyze(any(AnalyzeRequest.class)))
				.thenThrow(new OpenSearchException(indexNotFound));

		IndexSettingsAnalysis settings = parse("{\"analyzer\":{\"default\":{\"type\":\"custom\","
				+ "\"tokenizer\":\"standard\"}}}");

		// call under test
		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> manager.validateAnalyzerSettings(settings));
		assertTrue(ex.getMessage().contains("temporarily unavailable"));
		verify(indicesClient, times(OpenSearchManagerImpl.VALIDATE_MAX_RETRIES))
				.analyze(any(AnalyzeRequest.class));
	}

	@Test
	public void testValidateRunsOneAnalyzePerAnalyzerEntry() throws IOException {
		// The validator must exercise every entry under analyzer.* — a single _analyze call
		// against `default` doesn't cover filters/tokenizers that are only reachable from a
		// sibling entry like `default_search`.
		setupAnalyzeSuccess();

		IndexSettingsAnalysis settings = parse("{"
				+ "\"analyzer\":{"
					+ "\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\"},"
					+ "\"default_search\":{\"type\":\"custom\",\"tokenizer\":\"keyword\"},"
					+ "\"third\":{\"type\":\"custom\",\"tokenizer\":\"whitespace\"}"
				+ "}}");

		// call under test
		assertDoesNotThrow(() -> manager.validateAnalyzerSettings(settings));
		verify(indicesClient, times(3)).analyze(any(AnalyzeRequest.class));
	}

	@Test
	public void testValidateCatchesBadFilterReferencedOnlyFromNonDefaultEntry() throws IOException {
		// `default` is well-formed, but `default_search` references a filter type AOSS
		// doesn't recognize. Match by request content rather than call order — the typed
		// analyzer map's iteration order is not guaranteed.
		when(openSearchClient.indices()).thenReturn(indicesClient);
		ErrorResponse bad = ErrorResponse.of(e -> e
				.error(err -> err.type("illegal_argument_exception")
						.reason("Unknown token filter type [bogus_type]")).status(400));
		// Match by request content rather than call order — the typed analyzer map's
		// iteration order is not guaranteed, so the offending request must be identified
		// by its filter chain, not its position. The good (default) entry's analyze call,
		// when it happens, returns Mockito's default null — production code only inspects
		// the response on exception paths.
		when(indicesClient.analyze(org.mockito.ArgumentMatchers.argThat(
				(AnalyzeRequest req) -> req != null && req.filter() != null
						&& req.filter().stream().anyMatch(f -> "bogus_type".equals(f.name())))))
				.thenThrow(new OpenSearchException(bad));

		IndexSettingsAnalysis settings = parse("{"
				+ "\"analyzer\":{"
					+ "\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\"},"
					+ "\"default_search\":{\"type\":\"custom\",\"tokenizer\":\"standard\","
						+ "\"filter\":[\"bogus_type\"]}"
				+ "}}");

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> manager.validateAnalyzerSettings(settings));
		assertTrue(ex.getMessage().contains("default_search"),
				"Error must name the offending analyzer entry: " + ex.getMessage());
		assertTrue(ex.getMessage().contains("bogus_type"),
				"Error must surface AOSS's reason: " + ex.getMessage());
	}

	@ParameterizedTest
	@CsvSource({
			"synonym, word_delimiter, true",
			"synonym, word_delimiter_graph, true",
			"synonym_graph, word_delimiter, true",
			"synonym_graph, word_delimiter_graph, true",
			"synonym_graph, word_delimiter_graph, false"})
	public void testValidateWithWordDelimiterAfterSynonymFilter(String synonymType, String delimiterType,
			boolean delimiterInRegistry) {
		// A registry entry and a bare built-in name are both resolved to their filter type.
		String delimiterName = delimiterInRegistry ? "my_delimiter" : delimiterType;
		IndexSettingsAnalysis settings = parse("{\"filter\":{"
				+ "\"my_synonyms\":{\"type\":\"" + synonymType + "\",\"synonyms\":[\"apoe4, apolipoprotein e epsilon 4\"]}"
				+ (delimiterInRegistry ? ",\"my_delimiter\":{\"type\":\"" + delimiterType + "\"}" : "")
				+ "},\"analyzer\":{\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\","
				+ "\"filter\":[\"lowercase\",\"my_synonyms\",\"" + delimiterName + "\"]}}}");

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> manager.validateAnalyzerSettings(settings));

		assertEquals("Invalid analyzer configuration in 'default': token filter '" + delimiterName + "' ("
				+ delimiterType + ") runs after synonym filter 'my_synonyms'. A word delimiter after a synonym"
				+ " filter splits the synonym alternatives and can make a query exceed the search clause limit."
				+ " Remove the word delimiter from this chain.", ex.getMessage());
		verifyNoInteractions(openSearchClient);
	}

	@Test
	public void testValidateWithWordDelimiterInIndexChainAndSynonymFilterInSearchChain() throws IOException {
		setupAnalyzeSuccess();

		IndexSettingsAnalysis settings = parse("{\"filter\":{"
				+ "\"my_synonyms\":{\"type\":\"synonym_graph\",\"synonyms\":[\"apoe4, apolipoprotein e epsilon 4\"]},"
				+ "\"my_delimiter\":{\"type\":\"word_delimiter_graph\"}},"
				+ "\"analyzer\":{"
				+ "\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\",\"filter\":[\"lowercase\",\"my_delimiter\"]},"
				+ "\"default_search\":{\"type\":\"custom\",\"tokenizer\":\"standard\",\"filter\":[\"lowercase\",\"my_synonyms\"]}}}");

		// call under test
		assertDoesNotThrow(() -> manager.validateAnalyzerSettings(settings));
		verify(indicesClient, times(2)).analyze(any(AnalyzeRequest.class));
	}

	@Test
	public void testValidateWithWordDelimiterBeforeSynonymFilter() throws IOException {
		// This order is left to the _analyze probe: OpenSearch rejects it because a word delimiter
		// cannot parse synonym rules.
		setupAnalyzeSuccess();

		IndexSettingsAnalysis settings = parse("{\"filter\":{"
				+ "\"my_synonyms\":{\"type\":\"synonym_graph\",\"synonyms\":[\"apoe4, apolipoprotein e epsilon 4\"]},"
				+ "\"my_delimiter\":{\"type\":\"word_delimiter_graph\"}},"
				+ "\"analyzer\":{\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\","
				+ "\"filter\":[\"lowercase\",\"my_delimiter\",\"my_synonyms\"]}}}");

		// call under test
		assertDoesNotThrow(() -> manager.validateAnalyzerSettings(settings));
		verify(indicesClient).analyze(any(AnalyzeRequest.class));
	}

	@Test
	public void testParseThrowsOnMalformedInlineFilter() {
		// Inline filter registry entry that's not a valid TokenFilterDefinition. With the
		// boundary deserialization in SearchOpaqueJsonUtil.resolveAnalyzerSettings, this fails fast at
		// parse time (before the manager method is even invoked) rather than during the
		// _analyze probe, so the curator gets the rejection earlier in the request lifecycle.
		String settingsJson = "{"
				+ "\"filter\":{\"bad_one\":{\"type\":\"this_filter_does_not_exist\"}},"
				+ "\"analyzer\":{\"default\":{\"type\":\"custom\","
				+ "\"tokenizer\":\"standard\","
				+ "\"filter\":[\"bad_one\"]}}}";

		// call under test — parse() raises directly; the manager method is unreachable.
		RuntimeException ex = assertThrows(RuntimeException.class, () -> parse(settingsJson));
		Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
		assertTrue(cause instanceof IllegalArgumentException,
				"Underlying cause must be IllegalArgumentException: " + cause);
		assertTrue(cause.getMessage().toLowerCase().contains("invalid analyzer settings")
						|| cause.getMessage().contains("this_filter_does_not_exist"),
				"Error must surface the boundary deserialization failure: " + cause.getMessage());
	}
}
