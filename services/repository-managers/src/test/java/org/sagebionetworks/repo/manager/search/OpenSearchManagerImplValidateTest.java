package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.indices.AnalyzeRequest;
import org.opensearch.client.opensearch.indices.AnalyzeResponse;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.sagebionetworks.repo.model.search.table.TextAnalyzerSettings;

@ExtendWith(MockitoExtension.class)
public class OpenSearchManagerImplValidateTest {

	@Mock
	private OpenSearchClient openSearchClient;
	@Mock
	private OpenSearchIndicesClient indicesClient;
	@Mock
	private AnalyzeResponse analyzeResponse;
	@Mock
	private OpenSearchTransport transport;

	private OpenSearchManagerImpl manager;

	@BeforeEach
	void setUp() {
		manager = new OpenSearchManagerImpl(openSearchClient);
	}

	private void setupAnalyzeSuccess() throws IOException {
		when(openSearchClient.indices()).thenReturn(indicesClient);
		when(indicesClient.analyze(any(AnalyzeRequest.class))).thenReturn(analyzeResponse);
	}

	private void setupJsonpMapper() {
		when(openSearchClient._transport()).thenReturn(transport);
		when(transport.jsonpMapper()).thenReturn(new JacksonJsonpMapper());
	}

	@Test
	public void testValidateWithStandardTokenizerSuccess() throws IOException {
		setupAnalyzeSuccess();

		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("standard");

		// call under test
		assertDoesNotThrow(() -> manager.validateAnalyzerSettings(settings));
	}

	@Test
	public void testValidateWithFiltersSuccess() throws IOException {
		setupAnalyzeSuccess();
		setupJsonpMapper();

		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("standard");
		settings.setTokenFilters("{\"my_stop\":{\"type\":\"stop\",\"stopwords\":\"_english_\"}}");
		settings.setFilterOrder(Arrays.asList("my_stop", "lowercase"));

		// call under test
		assertDoesNotThrow(() -> manager.validateAnalyzerSettings(settings));
	}

	@Test
	public void testValidateWithCustomTokenizerConfig() throws IOException {
		setupAnalyzeSuccess();
		setupJsonpMapper();

		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizerConfig("{\"type\":\"edge_ngram\",\"min_gram\":2,\"max_gram\":20,\"token_chars\":[\"letter\",\"digit\"]}");
		settings.setFilterOrder(Arrays.asList("lowercase"));

		// call under test
		assertDoesNotThrow(() -> manager.validateAnalyzerSettings(settings));
	}

	@Test
	public void testValidateWithCharFilters() throws IOException {
		setupAnalyzeSuccess();
		setupJsonpMapper();

		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("standard");
		settings.setCharFilters("{\"my_mapping\":{\"type\":\"mapping\",\"mappings\":[\"& => and\"]}}");
		settings.setCharFilterOrder(Arrays.asList("my_mapping"));

		// call under test
		assertDoesNotThrow(() -> manager.validateAnalyzerSettings(settings));
	}

	@Test
	public void testValidateWithMinimalSettings() throws IOException {
		setupAnalyzeSuccess();

		TextAnalyzerSettings settings = new TextAnalyzerSettings();

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

		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("foobar");

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

		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("standard");

		// call under test
		IllegalStateException ex = assertThrows(IllegalStateException.class,
			() -> manager.validateAnalyzerSettings(settings));
		assertTrue(ex.getMessage().contains("temporarily unavailable"));
	}

	@Test
	public void testValidateThrowsOnMalformedTokenFiltersJson() {
		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("standard");
		settings.setTokenFilters("not valid json");
		settings.setFilterOrder(Arrays.asList("my_filter"));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
			() -> manager.validateAnalyzerSettings(settings));
		assertTrue(ex.getMessage().contains("Invalid tokenFilters JSON"));
	}

	@Test
	public void testValidateThrowsOnMalformedCharFiltersJson() {
		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("standard");
		settings.setCharFilters("not valid json");
		settings.setCharFilterOrder(Arrays.asList("my_filter"));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
			() -> manager.validateAnalyzerSettings(settings));
		assertTrue(ex.getMessage().contains("Invalid charFilters JSON"));
	}

	@Test
	public void testValidateThrowsOnMalformedTokenizerConfig() {
		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizerConfig("not valid json");

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
			() -> manager.validateAnalyzerSettings(settings));
		assertTrue(ex.getMessage().contains("Invalid tokenizer configuration"));
	}

	@Test
	public void testValidateThrowsOnNullSettings() {
		// call under test
		assertThrows(IllegalArgumentException.class,
			() -> manager.validateAnalyzerSettings(null));
	}

	@Test
	public void testValidateRetriesOnIndexNotFoundThenSucceeds() throws IOException {
		when(openSearchClient.indices()).thenReturn(indicesClient);
		ErrorResponse indexNotFound = ErrorResponse.of(e -> e
			.error(err -> err.type("index_not_found_exception").reason("no such index"))
			.status(404));
		when(indicesClient.analyze(any(AnalyzeRequest.class)))
			.thenThrow(new OpenSearchException(indexNotFound))
			.thenReturn(analyzeResponse);

		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("standard");

		// call under test — first attempt throws index_not_found, second succeeds
		assertDoesNotThrow(() -> manager.validateAnalyzerSettings(settings));
		verify(indicesClient, times(2)).analyze(any(AnalyzeRequest.class));
	}

	@Test
	public void testValidateThrowsIllegalStateWhenIndexNotFoundExhaustsRetries() throws IOException {
		when(openSearchClient.indices()).thenReturn(indicesClient);
		ErrorResponse indexNotFound = ErrorResponse.of(e -> e
			.error(err -> err.type("index_not_found_exception").reason("no such index"))
			.status(404));
		when(indicesClient.analyze(any(AnalyzeRequest.class)))
			.thenThrow(new OpenSearchException(indexNotFound));

		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("standard");

		// call under test — all retries exhausted, must surface as IllegalStateException
		IllegalStateException ex = assertThrows(IllegalStateException.class,
			() -> manager.validateAnalyzerSettings(settings));
		assertTrue(ex.getMessage().contains("temporarily unavailable"));
		verify(indicesClient, times(OpenSearchManagerImpl.ANALYZE_RETRY_MAX)).analyze(any(AnalyzeRequest.class));
	}
}
