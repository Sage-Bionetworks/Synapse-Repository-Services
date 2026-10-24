package org.sagebionetworks.repo.manager.search;

import java.io.IOException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.generic.Body;
import org.opensearch.client.opensearch.generic.Requests;
import org.opensearch.client.opensearch.generic.Response;
import org.sagebionetworks.util.RetryException;
import org.sagebionetworks.util.TimeUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Registers the platform's embedding model on the SearchIndex OpenSearch domain: a connector that
 * signs Bedrock {@code InvokeModel} calls, a model group to own the model, and the deployed remote
 * model itself.
 *
 * <p>Creating the connector requires {@code iam:PassRole} on the role it names, so the client's
 * credentials must hold that grant.</p>
 *
 * <p>Every resource is found by name before it is created, so a repeated or partially completed
 * registration resumes where the last one stopped.</p>
 */
public class SemanticEmbeddingModelRegistrar {

	private static final Logger LOG = LogManager.getLogger(SemanticEmbeddingModelRegistrar.class);

	/**
	 * Bedrock foundation model and output vector width behind every stored or queried vector. Both are
	 * read back off the connector by {@link SemanticEmbeddingBootstrapperImpl}, which fails a build or
	 * query whose index was embedded under a different pair, so changing either one re-embeds rather
	 * than corrupts.
	 */
	static final String FOUNDATION_MODEL = "amazon.titan-embed-text-v2:0";
	static final int DIMENSION = 1024;

	static final String CONNECTOR_NAME = "synapse-semantic-embedding-bedrock";
	static final String MODEL_GROUP_NAME = "synapse-semantic-embedding";

	private static final String REGION = "us-east-1";
	private static final int HTTP_NOT_FOUND = 404;

	// A remote model reaches DEPLOYED within seconds. The wait stays well inside the semantic
	// bootstrap trigger's semaphore lease, so a second node cannot start a concurrent registration.
	// Non-final so unit tests can lower the waits.
	static int DEPLOY_POLL_ATTEMPTS = 10;
	static long DEPLOY_POLL_INTERVAL_MS = 3000L;
	static int REQUEST_MAX_ATTEMPTS = 5;
	static long REQUEST_INITIAL_BACKOFF_MS = 1000L;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final OpenSearchClient openSearchClient;
	private final String bedrockRoleArn;

	/**
	 * @param searchIndexManagedClient Client for the SearchIndex domain.
	 * @param bedrockRoleArn           The role the connector hands to the domain to invoke Bedrock.
	 */
	public SemanticEmbeddingModelRegistrar(OpenSearchClient searchIndexManagedClient, String bedrockRoleArn) {
		this.openSearchClient = searchIndexManagedClient;
		this.bedrockRoleArn = bedrockRoleArn;
	}

	/**
	 * Register and deploy the model, reusing a model group and connector left by an earlier
	 * registration.
	 *
	 * @return The id of the deployed model.
	 * @throws IllegalStateException when ML-Commons rejects a call, keeps failing after retries, or the
	 *                               model does not reach DEPLOYED.
	 */
	public String registerModel() {
		String modelGroupId = findOrCreateModelGroup();
		String connectorId = findOrCreateConnector();
		// model_group_id is always explicit: registering without one silently auto-creates a second
		// model group alongside the one this class owns.
		String modelId = requiredId(post("/_plugins/_ml/models/_register", MAPPER.createObjectNode()
				.put("name", SemanticEmbeddingBootstrapperImpl.MODEL_NAME)
				.put("function_name", "remote")
				.put("model_group_id", modelGroupId)
				.put("connector_id", connectorId)
				.put("description", "Synapse semantic search embeddings (" + FOUNDATION_MODEL + "/" + DIMENSION + ").")
				.toString()), "model_id");
		post("/_plugins/_ml/models/" + modelId + "/_deploy", "{}");
		awaitDeployed(modelId);
		LOG.info("Registered semantic embedding model {}", modelId);
		return modelId;
	}

	private String findOrCreateModelGroup() {
		return firstHitId("/_plugins/_ml/model_groups/_search", searchByName(MODEL_GROUP_NAME))
				.orElseGet(() -> requiredId(post("/_plugins/_ml/model_groups/_register", MAPPER.createObjectNode()
						.put("name", MODEL_GROUP_NAME)
						.put("description", "Synapse semantic search embedding models.")
						.toString()), "model_group_id"));
	}

	private String findOrCreateConnector() {
		return firstHitId("/_plugins/_ml/connectors/_search", searchByName(CONNECTOR_NAME))
				.orElseGet(() -> requiredId(post("/_plugins/_ml/connectors/_create", connectorBody()), "connector_id"));
	}

	/**
	 * The Bedrock Titan text-embedding connector blueprint. {@code dimensions} and {@code normalize}
	 * are the model's own defaults but are sent explicitly, so a change to those defaults cannot
	 * silently invalidate already-built indexes.
	 */
	String connectorBody() {
		ObjectNode connector = MAPPER.createObjectNode()
				.put("name", CONNECTOR_NAME)
				.put("description", "Synapse semantic search embeddings via Amazon Bedrock.")
				.put("version", 1)
				.put("protocol", "aws_sigv4");
		connector.putObject("parameters")
				.put("region", REGION)
				.put("service_name", "bedrock")
				.put("model", FOUNDATION_MODEL)
				.put("dimensions", DIMENSION)
				.put("normalize", true)
				.putArray("embeddingTypes").add("float");
		connector.putObject("credential").put("roleArn", bedrockRoleArn);
		// Retries a Bedrock 429/5xx instead of the ML-Commons default of failing the bulk item outright.
		connector.putObject("client_config")
				.put("max_retry_times", 4)
				.put("retry_backoff_policy", "exponential_full_jitter");
		ObjectNode predict = connector.putArray("actions").addObject()
				.put("action_type", "predict")
				.put("method", "POST")
				.put("url", "https://bedrock-runtime." + REGION + ".amazonaws.com/model/" + FOUNDATION_MODEL + "/invoke");
		predict.putObject("headers")
				.put("content-type", "application/json")
				.put("x-amz-content-sha256", "required");
		// ML-Commons substitutes the ${parameters.*} placeholders at predict time.
		predict.put("request_body", "{ \"inputText\": \"${parameters.inputText}\", \"dimensions\": ${parameters.dimensions},"
						+ " \"normalize\": ${parameters.normalize}, \"embeddingTypes\": ${parameters.embeddingTypes} }")
				.put("pre_process_function", "connector.pre_process.bedrock.embedding")
				.put("post_process_function", "connector.post_process.bedrock.embedding");
		return connector.toString();
	}

	/**
	 * Blocks until the newly registered model can answer a predict call, so the caller can resolve it
	 * right away rather than at the next scheduled refresh.
	 */
	private void awaitDeployed(String modelId) {
		for (int attempt = 0; attempt < DEPLOY_POLL_ATTEMPTS; attempt++) {
			String state = post("/_plugins/_ml/models/_search",
					"{\"size\":1,\"query\":{\"ids\":{\"values\":[\"" + modelId + "\"]}}}")
							.path("hits").path("hits").path(0).path("_source").path("model_state").asText();
			if ("DEPLOYED".equals(state)) {
				return;
			}
			LOG.info("Waiting for model {} to deploy, state: {}", modelId, state);
			try {
				Thread.sleep(DEPLOY_POLL_INTERVAL_MS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted waiting for model " + modelId + " to deploy", e);
			}
		}
		throw new IllegalStateException("Semantic embedding model " + modelId + " did not reach DEPLOYED.");
	}

	private static String searchByName(String name) {
		return "{\"size\":1,\"query\":{\"term\":{\"name.keyword\":\"" + name + "\"}}}";
	}

	private Optional<String> firstHitId(String endpoint, String requestBody) {
		JsonNode hits = post(endpoint, requestBody).path("hits").path("hits");
		return hits.isEmpty() ? Optional.empty() : Optional.of(hits.get(0).path("_id").asText());
	}

	private static String requiredId(JsonNode response, String field) {
		JsonNode id = response.path(field);
		if (!id.isTextual() || id.asText().isBlank()) {
			throw new IllegalStateException("ML-Commons response carries no '" + field + "': " + response);
		}
		return id.asText();
	}

	private JsonNode post(String endpoint, String requestBody) {
		try {
			return TimeUtils.waitForExponentialMaxRetry(REQUEST_MAX_ATTEMPTS, REQUEST_INITIAL_BACKOFF_MS,
					() -> postOnce(endpoint, requestBody));
		} catch (RetryException e) {
			throw new IllegalStateException("ML-Commons request to " + endpoint + " still failed after "
					+ REQUEST_MAX_ATTEMPTS + " attempts", e.getCause());
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Failed ML-Commons request to " + endpoint, e);
		}
	}

	/**
	 * @throws RetryException when the domain could not be reached or answered with a retryable status,
	 *                        both of which a stalled-but-recovering domain clears.
	 */
	private JsonNode postOnce(String endpoint, String requestBody) throws RetryException {
		int status;
		String responseBody;
		// The generic client's default predicate never throws, so every HTTP status comes back as data.
		try (Response response = openSearchClient.generic()
				.execute(Requests.builder().method("POST").endpoint(endpoint).json(requestBody).build())) {
			status = response.getStatus();
			responseBody = response.getBody().map(Body::bodyAsString).orElse("");
		} catch (IOException e) {
			LOG.warn("ML-Commons request to {} could not complete, retrying: {}", endpoint, e.getMessage());
			throw new RetryException(new IllegalStateException("Failed ML-Commons request to " + endpoint, e));
		}
		// A domain that has never held this kind of ML resource has no backing system index, so a
		// search 404s rather than returning zero hits.
		if (status == HTTP_NOT_FOUND) {
			return MAPPER.createObjectNode();
		}
		if (status >= 300) {
			IllegalStateException failure = new IllegalStateException(
					"ML-Commons request to " + endpoint + " failed with " + status + ": " + responseBody);
			if (OpenSearchManagerImpl.isRetryableItemStatus(status)) {
				LOG.warn("ML-Commons request to {} failed with status {}, retrying", endpoint, status);
				throw new RetryException(failure);
			}
			throw failure;
		}
		try {
			return MAPPER.readTree(responseBody);
		} catch (IOException e) {
			throw new IllegalStateException("Unparseable ML-Commons response for " + endpoint + ": " + responseBody, e);
		}
	}
}
