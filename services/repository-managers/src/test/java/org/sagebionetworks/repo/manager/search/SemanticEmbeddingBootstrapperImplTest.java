package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.generic.Bodies;
import org.opensearch.client.opensearch.generic.OpenSearchClientException;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient.ClientOptions;
import org.opensearch.client.opensearch.generic.Request;
import org.opensearch.client.opensearch.generic.Response;
import org.sagebionetworks.repo.manager.search.SemanticEmbeddingBootstrapper.SemanticEmbeddingModel;

@ExtendWith(MockitoExtension.class)
public class SemanticEmbeddingBootstrapperImplTest {

	private static final String CONNECTOR_ENDPOINT = SemanticEmbeddingBootstrapperImpl.CONNECTOR_ENDPOINT_PREFIX + "connector-1";
	private static final String MODEL_HIT = "{\"hits\":{\"hits\":[{\"_id\":\"model-1\",\"_source\":{\"connector_id\":\"connector-1\"}}]}}";
	private static final String CONNECTOR = "{\"parameters\":{\"model\":\"amazon.titan-embed-text-v2:0\",\"dimensions\":\"1024\"}}";
	private static final SemanticEmbeddingModel MODEL =
			new SemanticEmbeddingModel("model-1", "amazon.titan-embed-text-v2:0", 1024);

	@Mock
	private OpenSearchClient openSearchClient;
	@Mock
	private OpenSearchGenericClient genericClient;

	private SemanticEmbeddingBootstrapperImpl bootstrapper;
	private long originalBackoffMs;

	@BeforeEach
	public void before() {
		originalBackoffMs = SemanticEmbeddingBootstrapperImpl.ML_REQUEST_INITIAL_BACKOFF_MS;
		SemanticEmbeddingBootstrapperImpl.ML_REQUEST_INITIAL_BACKOFF_MS = 1L;
		bootstrapper = new SemanticEmbeddingBootstrapperImpl(openSearchClient);
	}

	@AfterEach
	public void after() {
		SemanticEmbeddingBootstrapperImpl.ML_REQUEST_INITIAL_BACKOFF_MS = originalBackoffMs;
	}

	private void stubGenericClient() {
		when(openSearchClient.generic()).thenReturn(genericClient);
		when(genericClient.withClientOptions(any(ClientOptions.class))).thenReturn(genericClient);
	}

	private static ArgumentMatcher<Request> isModelSearch() {
		return request -> request != null && "POST".equals(request.getMethod())
				&& SemanticEmbeddingBootstrapperImpl.MODEL_SEARCH_ENDPOINT.equals(request.getEndpoint())
				&& request.getBody().map(body -> SemanticEmbeddingBootstrapperImpl.MODEL_SEARCH_BODY.equals(body.bodyAsString()))
						.orElse(false);
	}

	private static ArgumentMatcher<Request> isConnectorGet() {
		return request -> request != null && "GET".equals(request.getMethod()) && CONNECTOR_ENDPOINT.equals(request.getEndpoint())
				&& request.getBody().isEmpty();
	}

	private static Response response(String json) {
		Response response = mock(Response.class);
		when(response.getBody()).thenReturn(Optional.of(Bodies.json(json)));
		return response;
	}

	private static OpenSearchClientException httpError(int status) {
		Response response = mock(Response.class);
		when(response.getStatus()).thenReturn(status);
		return new OpenSearchClientException(response);
	}

	@Test
	public void testGetModelWithEmptyCacheResolvesModelOnce() throws IOException {
		stubGenericClient();
		Response search = response(MODEL_HIT);
		Response connector = response(CONNECTOR);
		when(genericClient.execute(argThat(isModelSearch()))).thenReturn(search);
		when(genericClient.execute(argThat(isConnectorGet()))).thenReturn(connector);

		// call under test
		assertEquals(Optional.of(MODEL), bootstrapper.getModel());

		assertEquals(Optional.of(MODEL), bootstrapper.getModel());
		verify(genericClient, times(2)).execute(any(Request.class));
	}

	@Test
	public void testGetModelWithNoDeployedModelRetriesEachCall() throws IOException {
		stubGenericClient();
		Response noHits = response("{\"hits\":{\"hits\":[]}}");
		Response modelHit = response(MODEL_HIT);
		Response connector = response(CONNECTOR);
		when(genericClient.execute(argThat(isModelSearch())))
				.thenReturn(noHits)
				.thenReturn(modelHit);
		when(genericClient.execute(argThat(isConnectorGet()))).thenReturn(connector);

		// call under test
		assertEquals(Optional.empty(), bootstrapper.getModel());

		assertEquals(Optional.of(MODEL), bootstrapper.getModel());
	}

	@Test
	public void testBootstrapSemanticEmbeddingWithDeployedModelCachesModel() throws IOException {
		stubGenericClient();
		Response search = response(MODEL_HIT);
		Response connector = response(CONNECTOR);
		when(genericClient.execute(argThat(isModelSearch()))).thenReturn(search);
		when(genericClient.execute(argThat(isConnectorGet()))).thenReturn(connector);

		// call under test
		bootstrapper.bootstrapSemanticEmbedding();

		assertEquals(Optional.of(MODEL), bootstrapper.getModel());
		assertEquals(Optional.of(MODEL), bootstrapper.getModel());
		// Reads are served from the cache: the domain was only called by the one bootstrap.
		verify(genericClient, times(2)).execute(any(Request.class));
		verify(search).close();
		verify(connector).close();
	}

	@Test
	public void testBootstrapSemanticEmbeddingWithNoModelIndexClearsCache() throws IOException {
		stubGenericClient();
		Response modelHit = response(MODEL_HIT);
		OpenSearchClientException notFound = httpError(404);
		Response connector = response(CONNECTOR);
		when(genericClient.execute(argThat(isModelSearch())))
				.thenReturn(modelHit)
				.thenThrow(notFound);
		when(genericClient.execute(argThat(isConnectorGet()))).thenReturn(connector);
		bootstrapper.bootstrapSemanticEmbedding();

		// call under test
		bootstrapper.bootstrapSemanticEmbedding();

		assertEquals(Optional.empty(), bootstrapper.getModel());
	}

	@Test
	public void testBootstrapSemanticEmbeddingWithNoDeployedModelClearsCache() throws IOException {
		stubGenericClient();
		Response modelHit = response(MODEL_HIT);
		Response noHits = response("{\"hits\":{\"hits\":[]}}");
		Response connector = response(CONNECTOR);
		when(genericClient.execute(argThat(isModelSearch())))
				.thenReturn(modelHit)
				.thenReturn(noHits);
		when(genericClient.execute(argThat(isConnectorGet()))).thenReturn(connector);
		bootstrapper.bootstrapSemanticEmbedding();

		// call under test
		bootstrapper.bootstrapSemanticEmbedding();

		assertEquals(Optional.empty(), bootstrapper.getModel());
	}

	@Test
	public void testBootstrapSemanticEmbeddingWithServerErrorKeepsCachedModel() throws IOException {
		stubGenericClient();
		Response modelHit = response(MODEL_HIT);
		OpenSearchClientException serverError = httpError(500);
		Response connector = response(CONNECTOR);
		when(genericClient.execute(argThat(isModelSearch())))
				.thenReturn(modelHit)
				.thenThrow(serverError);
		when(genericClient.execute(argThat(isConnectorGet()))).thenReturn(connector);
		bootstrapper.bootstrapSemanticEmbedding();

		// call under test
		bootstrapper.bootstrapSemanticEmbedding();

		assertEquals(Optional.of(MODEL), bootstrapper.getModel());
		verify(genericClient, times(1 + SemanticEmbeddingBootstrapperImpl.ML_REQUEST_MAX_RETRIES))
				.execute(argThat(isModelSearch()));
	}

	@Test
	public void testBootstrapSemanticEmbeddingWithTransientFailuresRetriesThenCachesModel() throws IOException {
		stubGenericClient();
		OpenSearchClientException serviceUnavailable = httpError(503);
		Response modelHit = response(MODEL_HIT);
		Response connector = response(CONNECTOR);
		when(genericClient.execute(argThat(isModelSearch())))
				.thenThrow(serviceUnavailable)
				.thenReturn(modelHit);
		when(genericClient.execute(argThat(isConnectorGet())))
				.thenThrow(new IOException("read timed out"))
				.thenReturn(connector);

		// call under test
		bootstrapper.bootstrapSemanticEmbedding();

		assertEquals(Optional.of(MODEL), bootstrapper.getModel());
		verify(genericClient, times(2)).execute(argThat(isModelSearch()));
		verify(genericClient, times(2)).execute(argThat(isConnectorGet()));
	}

	@Test
	public void testBootstrapSemanticEmbeddingWithClientErrorDoesNotRetry() throws IOException {
		stubGenericClient();
		OpenSearchClientException forbidden = httpError(403);
		when(genericClient.execute(argThat(isModelSearch()))).thenThrow(forbidden);

		// call under test
		bootstrapper.bootstrapSemanticEmbedding();

		verify(genericClient).execute(argThat(isModelSearch()));
		verify(genericClient, never()).execute(argThat(isConnectorGet()));
	}

	@Test
	public void testBootstrapSemanticEmbeddingWithMissingConnectorIdLeavesCacheEmpty() throws IOException {
		stubGenericClient();
		Response hitWithoutConnector = response("{\"hits\":{\"hits\":[{\"_id\":\"model-1\",\"_source\":{}}]}}");
		when(genericClient.execute(argThat(isModelSearch())))
				.thenReturn(hitWithoutConnector);

		// call under test
		bootstrapper.bootstrapSemanticEmbedding();

		assertEquals(Optional.empty(), bootstrapper.getModel());
		verify(genericClient, never()).execute(argThat(isConnectorGet()));
	}

	@Test
	public void testBootstrapSemanticEmbeddingWithConnectorMissingDimensionsLeavesCacheEmpty() throws IOException {
		stubGenericClient();
		Response modelHit = response(MODEL_HIT);
		Response connectorWithoutDimensions = response("{\"parameters\":{\"model\":\"amazon.titan-embed-text-v2:0\"}}");
		when(genericClient.execute(argThat(isModelSearch()))).thenReturn(modelHit);
		when(genericClient.execute(argThat(isConnectorGet())))
				.thenReturn(connectorWithoutDimensions);

		// call under test
		bootstrapper.bootstrapSemanticEmbedding();

		assertEquals(Optional.empty(), bootstrapper.getModel());
	}
}
