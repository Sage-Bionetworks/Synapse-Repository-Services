package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

@ExtendWith(MockitoExtension.class)
public class SemanticEmbeddingModelRegistrarTest {

	private static final String PROXY_FUNCTION = "dev-test-synidx-ml-proxy";
	private static final String ROLE_ARN = "arn:aws:iam::123456789012:role/dev-test-synidx-bedrock-embed";
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Mock
	private LambdaClient lambdaClient;

	/**
	 * Responses queued by request path, and the paths actually requested in order. A path with more
	 * than one queued response (e.g. a models search before and after a deploy) is drained in order; a
	 * path with exactly one keeps returning it, matching a real poll loop.
	 */
	private Map<String, Deque<String>> payloadByPath;
	private List<String> requestedPaths;
	private List<String> requestBodies;

	private SemanticEmbeddingModelRegistrar registrar;
	private long originalBackoffMs;
	private long originalPollIntervalMs;

	@BeforeEach
	public void before() {
		payloadByPath = new HashMap<>();
		requestedPaths = new ArrayList<>();
		requestBodies = new ArrayList<>();
		originalBackoffMs = SemanticEmbeddingModelRegistrar.REQUEST_INITIAL_BACKOFF_MS;
		originalPollIntervalMs = SemanticEmbeddingModelRegistrar.DEPLOY_POLL_INTERVAL_MS;
		SemanticEmbeddingModelRegistrar.REQUEST_INITIAL_BACKOFF_MS = 1L;
		SemanticEmbeddingModelRegistrar.DEPLOY_POLL_INTERVAL_MS = 1L;
		registrar = new SemanticEmbeddingModelRegistrar(lambdaClient, PROXY_FUNCTION, ROLE_ARN);
	}

	@AfterEach
	public void after() {
		SemanticEmbeddingModelRegistrar.REQUEST_INITIAL_BACKOFF_MS = originalBackoffMs;
		SemanticEmbeddingModelRegistrar.DEPLOY_POLL_INTERVAL_MS = originalPollIntervalMs;
	}

	@Test
	public void testRegisterModelWithNothingProvisioned() throws Exception {
		stubProxy();
		// No system indexes yet, so every ML-Commons search 404s.
		respondWithStatus("/_plugins/_ml/model_groups/_search", 404, "{\"error\":\"index_not_found_exception\"}");
		respondWithStatus("/_plugins/_ml/connectors/_search", 404, "{\"error\":\"index_not_found_exception\"}");
		respond("/_plugins/_ml/model_groups/_register", "{\"model_group_id\":\"group-1\"}");
		respond("/_plugins/_ml/connectors/_create", "{\"connector_id\":\"connector-1\"}");
		respond("/_plugins/_ml/models/_register", "{\"model_id\":\"model-1\"}");
		respond("/_plugins/_ml/models/model-1/_deploy", "{\"task_id\":\"task-1\"}");
		respond("/_plugins/_ml/models/_search", deployState("model-1", "DEPLOYED"));

		// call under test
		assertEquals("model-1", registrar.registerModel());

		assertEquals(List.of("/_plugins/_ml/model_groups/_search", "/_plugins/_ml/model_groups/_register",
				"/_plugins/_ml/connectors/_search", "/_plugins/_ml/connectors/_create", "/_plugins/_ml/models/_register",
				"/_plugins/_ml/models/model-1/_deploy", "/_plugins/_ml/models/_search"), requestedPaths);
		JsonNode connector = MAPPER.readTree(requestBodies.get(3));
		// The connector must name the role the domain template creates and carry the model/dimension the
		// bootstrapper reads back to validate indexes.
		assertEquals(ROLE_ARN, connector.path("credential").path("roleArn").asText());
		assertEquals("amazon.titan-embed-text-v2:0", connector.path("parameters").path("model").asText());
		assertEquals(1024, connector.path("parameters").path("dimensions").asInt());
		// ML-Commons substitutes these at predict time, so they must reach it untouched.
		assertEquals("{ \"inputText\": \"${parameters.inputText}\", \"dimensions\": ${parameters.dimensions},"
				+ " \"normalize\": ${parameters.normalize}, \"embeddingTypes\": ${parameters.embeddingTypes} }",
				connector.path("actions").path(0).path("request_body").asText());
		// Registering without an explicit group silently auto-creates a second one.
		JsonNode register = MAPPER.readTree(requestBodies.get(4));
		assertEquals("group-1", register.path("model_group_id").asText());
		assertEquals("connector-1", register.path("connector_id").asText());
		assertEquals("synapse-semantic-embedding", register.path("name").asText());
	}

	@Test
	public void testRegisterModelWithExistingConnectorAndGroup() throws Exception {
		stubProxy();
		respond("/_plugins/_ml/model_groups/_search", hit("group-existing"));
		respond("/_plugins/_ml/connectors/_search", hit("connector-existing"));
		respond("/_plugins/_ml/models/_register", "{\"model_id\":\"model-2\"}");
		respond("/_plugins/_ml/models/model-2/_deploy", "{}");
		respond("/_plugins/_ml/models/_search", deployState("model-2", "DEPLOYED"));

		// call under test
		assertEquals("model-2", registrar.registerModel());

		// A partially completed earlier registration is resumed rather than duplicated.
		assertFalse(requestedPaths.contains("/_plugins/_ml/model_groups/_register"), requestedPaths.toString());
		assertFalse(requestedPaths.contains("/_plugins/_ml/connectors/_create"), requestedPaths.toString());
		JsonNode register = MAPPER.readTree(requestBodies.get(2));
		assertEquals("group-existing", register.path("model_group_id").asText());
		assertEquals("connector-existing", register.path("connector_id").asText());
	}

	@Test
	public void testRegisterModelWithDeployInProgress() throws Exception {
		stubProxy();
		respond("/_plugins/_ml/model_groups/_search", hit("group-existing"));
		respond("/_plugins/_ml/connectors/_search", hit("connector-existing"));
		respond("/_plugins/_ml/models/_register", "{\"model_id\":\"model-3\"}");
		respond("/_plugins/_ml/models/model-3/_deploy", "{}");
		respond("/_plugins/_ml/models/_search", deployState("model-3", "DEPLOYING"));
		respond("/_plugins/_ml/models/_search", deployState("model-3", "DEPLOYED"));

		// call under test
		assertEquals("model-3", registrar.registerModel());

		assertEquals(2, requestedPaths.stream().filter("/_plugins/_ml/models/_search"::equals).count());
	}

	@Test
	public void testRegisterModelWithModelNeverDeployed() throws Exception {
		stubProxy();
		respond("/_plugins/_ml/model_groups/_search", hit("group-existing"));
		respond("/_plugins/_ml/connectors/_search", hit("connector-existing"));
		respond("/_plugins/_ml/models/_register", "{\"model_id\":\"model-4\"}");
		respond("/_plugins/_ml/models/model-4/_deploy", "{}");
		respond("/_plugins/_ml/models/_search", deployState("model-4", "DEPLOY_FAILED"));

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			registrar.registerModel();
		}).getMessage();

		assertEquals("Semantic embedding model model-4 did not reach DEPLOYED.", message);
		assertEquals(SemanticEmbeddingModelRegistrar.DEPLOY_POLL_ATTEMPTS,
				requestedPaths.stream().filter("/_plugins/_ml/models/_search"::equals).count());
	}

	@Test
	public void testRegisterModelWithFailedConnectorCreate() throws Exception {
		stubProxy();
		respond("/_plugins/_ml/model_groups/_search", hit("group-existing"));
		respondWithStatus("/_plugins/_ml/connectors/_search", 404, "{\"error\":\"index_not_found_exception\"}");
		respondWithStatus("/_plugins/_ml/connectors/_create", 403,
				"{\"error\":{\"type\":\"security_exception\",\"reason\":\"not authorized to perform: iam:PassRole\"}}");

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			registrar.registerModel();
		}).getMessage();

		assertTrue(message.contains("iam:PassRole"), message);
		// A 4xx is not retried, and nothing is registered against a connector that was never created.
		assertEquals(1, requestedPaths.stream().filter("/_plugins/_ml/connectors/_create"::equals).count());
		assertFalse(requestedPaths.contains("/_plugins/_ml/models/_register"), requestedPaths.toString());
	}

	@Test
	public void testRegisterModelWithTransientGatewayTimeout() throws Exception {
		stubProxy();
		respondWithStatus("/_plugins/_ml/model_groups/_search", 504, "<html>504 Gateway Time-out</html>");
		respond("/_plugins/_ml/model_groups/_search", hit("group-existing"));
		respond("/_plugins/_ml/connectors/_search", hit("connector-existing"));
		respond("/_plugins/_ml/models/_register", "{\"model_id\":\"model-5\"}");
		respond("/_plugins/_ml/models/model-5/_deploy", "{}");
		respond("/_plugins/_ml/models/_search", deployState("model-5", "DEPLOYED"));

		// call under test
		assertEquals("model-5", registrar.registerModel());

		assertEquals(2, requestedPaths.stream().filter("/_plugins/_ml/model_groups/_search"::equals).count());
	}

	@Test
	public void testRegisterModelWithPersistentGatewayTimeout() throws Exception {
		stubProxy();
		respondWithStatus("/_plugins/_ml/model_groups/_search", 504, "<html>504 Gateway Time-out</html>");

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			// call under test
			registrar.registerModel();
		});

		assertEquals("ML-Commons request to /_plugins/_ml/model_groups/_search still failed after 5 attempts",
				exception.getMessage());
		assertEquals("ML-Commons request to /_plugins/_ml/model_groups/_search failed with 504: <html>504 Gateway Time-out</html>",
				exception.getCause().getMessage());
		assertEquals(List.of("/_plugins/_ml/model_groups/_search", "/_plugins/_ml/model_groups/_search",
				"/_plugins/_ml/model_groups/_search", "/_plugins/_ml/model_groups/_search",
				"/_plugins/_ml/model_groups/_search"), requestedPaths);
	}

	@Test
	public void testRegisterModelWithUnreachableDomain() throws Exception {
		// The proxy raises (rather than returning a status) when it cannot reach the domain at all.
		when(lambdaClient.invoke(any(InvokeRequest.class))).thenReturn(InvokeResponse.builder()
				.functionError("Unhandled")
				.payload(SdkBytes.fromUtf8String("{\"errorType\":\"TimeoutError\",\"errorMessage\":\"timed out\"}"))
				.build());

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			// call under test
			registrar.registerModel();
		});

		String cause = exception.getCause().getMessage();
		assertTrue(cause.startsWith("Failed ML-Commons request to /_plugins/_ml/model_groups/_search"), cause);
		assertTrue(cause.contains("timed out"), cause);
		verify(lambdaClient, times(SemanticEmbeddingModelRegistrar.REQUEST_MAX_ATTEMPTS)).invoke(any(InvokeRequest.class));
	}

	/**
	 * Records each request the proxy function is asked to forward and replays the status and body
	 * queued for its path, so the assertions can read the JSON that would actually reach ML-Commons.
	 */
	private void stubProxy() {
		when(lambdaClient.invoke(any(InvokeRequest.class))).thenAnswer(invocation -> {
			InvokeRequest request = invocation.getArgument(0);
			assertEquals(PROXY_FUNCTION, request.functionName());
			assertEquals(InvocationType.REQUEST_RESPONSE, request.invocationType());
			JsonNode proxied = MAPPER.readTree(request.payload().asUtf8String());
			String path = proxied.path("path").asText();
			requestedPaths.add(path);
			requestBodies.add(proxied.path("body").asText());

			Deque<String> payloads = payloadByPath.get(path);
			if (payloads == null || payloads.isEmpty()) {
				throw new IllegalStateException("No stubbed response for " + path);
			}
			String payload = payloads.size() > 1 ? payloads.poll() : payloads.peek();
			return InvokeResponse.builder().statusCode(200).payload(SdkBytes.fromUtf8String(payload)).build();
		});
	}

	private void respond(String path, String body) {
		respondWithStatus(path, 200, body);
	}

	private void respondWithStatus(String path, int status, String body) {
		String payload = MAPPER.createObjectNode().put("status", status).put("body", body).toString();
		payloadByPath.computeIfAbsent(path, p -> new ArrayDeque<>()).add(payload);
	}

	private static String hit(String id) {
		return "{\"hits\":{\"hits\":[{\"_id\":\"" + id + "\"}]}}";
	}

	private static String deployState(String id, String state) {
		return "{\"hits\":{\"hits\":[{\"_id\":\"" + id + "\",\"_source\":{\"model_state\":\"" + state + "\"}}]}}";
	}
}
