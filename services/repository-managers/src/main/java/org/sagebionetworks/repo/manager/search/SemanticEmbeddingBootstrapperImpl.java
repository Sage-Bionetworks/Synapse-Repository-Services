package org.sagebionetworks.repo.manager.search;

import java.io.IOException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.generic.OpenSearchClientException;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient.ClientOptions;
import org.opensearch.client.opensearch.generic.Requests;
import org.opensearch.client.opensearch.generic.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Resolves the platform's embedding model through the ML-Commons REST API. The model is found by
 * name; its foundation model and vector width are read from the connector it was registered with,
 * which is the only place Synapse-Stack-Builder records them.
 *
 * <p>This class never creates, updates, or deletes an ML-Commons resource, so the deployed
 * application needs no permission beyond querying the domain.</p>
 */
public class SemanticEmbeddingBootstrapperImpl implements SemanticEmbeddingBootstrapper {

	private static final Logger LOG = LogManager.getLogger(SemanticEmbeddingBootstrapperImpl.class);

	/**
	 * Name Synapse-Stack-Builder registers the model under.
	 */
	static final String MODEL_NAME = "synapse-semantic-embedding";

	static final String MODEL_SEARCH_ENDPOINT = "/_plugins/_ml/models/_search";
	static final String CONNECTOR_ENDPOINT_PREFIX = "/_plugins/_ml/connectors/";

	// Only a DEPLOYED model can answer a predict call, and the newest one wins so a duplicate left
	// behind by an earlier registration does not shadow the current one.
	static final String MODEL_SEARCH_BODY = """
			{"size":1,"query":{"bool":{"filter":[\
			{"term":{"name.keyword":"%s"}},{"term":{"model_state":"DEPLOYED"}}]}},\
			"sort":[{"created_time":{"order":"desc"}}]}""".formatted(MODEL_NAME);

	private static final int HTTP_NOT_FOUND = 404;
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final OpenSearchClient openSearchClient;

	// The id, model, and dimension are swapped as one value so a reader never sees a model id
	// paired with another registration's dimension.
	private volatile SemanticEmbeddingModel model;

	public SemanticEmbeddingBootstrapperImpl(OpenSearchClient searchIndexManagedClient) {
		this.openSearchClient = searchIndexManagedClient;
	}

	@Override
	public Optional<SemanticEmbeddingModel> getModel() {
		// The periodic refresh is a cluster singleton, so a node it has not run on fills its own
		// cache here.
		if (model == null) {
			bootstrapSemanticEmbedding();
		}
		return Optional.ofNullable(model);
	}

	@Override
	public void bootstrapSemanticEmbedding() {
		try {
			model = findNewestDeployedModel().orElse(null);
		} catch (RuntimeException e) {
			// An unreachable ML plugin keeps the last resolved model rather than dropping semantic
			// search; the next refresh retries.
			LOG.error("Failed to resolve the semantic embedding model", e);
			return;
		}
		if (model == null) {
			// Expected on a freshly provisioned stack: the model deploys asynchronously.
			LOG.warn("No deployed '{}' model on the OpenSearch domain, so semantic search is unavailable.",
					MODEL_NAME);
		} else {
			LOG.info("Resolved semantic embedding model {} for {}", model.modelId(), model.spec());
		}
	}

	private Optional<SemanticEmbeddingModel> findNewestDeployedModel() {
		JsonNode response;
		try {
			response = execute("POST", MODEL_SEARCH_ENDPOINT, MODEL_SEARCH_BODY);
		} catch (OpenSearchClientException e) {
			// A domain that has never held a model has no backing system index, so the search 404s
			// rather than returning zero hits.
			if (e.status() == HTTP_NOT_FOUND) {
				return Optional.empty();
			}
			throw e;
		}
		JsonNode hits = response.path("hits").path("hits");
		if (hits.isEmpty()) {
			return Optional.empty();
		}
		JsonNode hit = hits.get(0);
		String modelId = hit.path("_id").asText();
		String connectorId = hit.path("_source").path("connector_id").asText();
		if (connectorId.isEmpty()) {
			throw new IllegalStateException("Semantic embedding model " + modelId + " has no connector_id");
		}
		JsonNode parameters = execute("GET", CONNECTOR_ENDPOINT_PREFIX + connectorId, null).path("parameters");
		String foundationModel = parameters.path("model").asText();
		int dimension = parameters.path("dimensions").asInt();
		if (foundationModel.isEmpty() || dimension <= 0) {
			throw new IllegalStateException("Connector " + connectorId
					+ " of semantic embedding model " + modelId + " does not declare parameters.model and parameters.dimensions");
		}
		return Optional.of(new SemanticEmbeddingModel(modelId, foundationModel, dimension));
	}

	/**
	 * Call an ML-Commons endpoint and parse the JSON response. ML-Commons has no typed client
	 * coverage, so the call goes through the generic client.
	 */
	private JsonNode execute(String method, String endpoint, String requestBody) {
		Requests.JsonBodyBuilder request = Requests.builder().method(method).endpoint(endpoint);
		if (requestBody != null) {
			request.json(requestBody);
		}
		// The generic client's default predicate never throws, so an error status would otherwise
		// arrive as a normal response.
		try (Response response = openSearchClient.generic()
				.withClientOptions(ClientOptions.throwOnHttpErrors())
				.execute(request.build())) {
			return MAPPER.readTree(response.getBody()
					.orElseThrow(() -> new IllegalStateException("Empty response from " + endpoint))
					.bodyAsString());
		} catch (IOException e) {
			throw new RuntimeException("Failed ML-Commons request to " + endpoint, e);
		}
	}
}
