package org.sagebionetworks.repo.manager.agent.specialist.jsonschema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager;
import org.sagebionetworks.repo.manager.agent.specialist.ToolResponse;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Type;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

@ExtendWith(MockitoExtension.class)
public class JsonSchemaToolsTest {

	@Mock
	private JsonSchemaManager mockJsonSchemaManager;

	@Mock
	private CodeInterpreterFileManager mockCodeInterpreterFileManager;

	@Mock
	private GridManager mockGridManager;

	private JsonSchemaTools tools;
	private UserInfo userInfo;
	private GridAgentSessionContext gridContext;
	private ToolContext toolContext;
	private ToolContext toolContextWithSession;
	private ToolContext toolContextWithGrid;

	@BeforeEach
	public void setup() {
		tools = new JsonSchemaTools(mockJsonSchemaManager, mockCodeInterpreterFileManager, mockGridManager);
		userInfo = new UserInfo(false, 101L);
		gridContext = new GridAgentSessionContext().setGridSessionId("grid-1").setUsersReplicaId(1L);
		toolContext = new ToolContext(Map.of());
		toolContextWithSession = new ToolContext(Map.of(AgentToolContextKey.CODE_SESSION_ID.getKey(), "session-123"));
		toolContextWithGrid = new ToolContext(Map.of(AgentToolContextKey.USER_INFO.getKey(), userInfo,
				AgentToolContextKey.GRID_SESSION_CONTEXT.getKey(), gridContext));
	}

	private ToolCallback callback(String name) {
		return tools.getToolCallbacks().stream()
				.filter(c -> name.equals(c.getToolDefinition().name())).findFirst().orElseThrow();
	}

	@Test
	public void testToolCallbackNamesAndSchemas() {
		Set<String> names = tools.getToolCallbacks().stream().map(c -> c.getToolDefinition().name())
				.collect(Collectors.toSet());

		assertEquals(Set.of("describeSchema", "describeGridSchema", "writeSchemaToSession"), names);

		// A required scalar becomes a typed, required top-level property.
		JSONObject describeSchema = new JSONObject(callback("describeSchema").getToolDefinition().inputSchema());
		assertEquals("string", describeSchema.getJSONObject("properties").getJSONObject("schemaId").getString("type"));
		assertTrue(describeSchema.getJSONArray("required").toList().contains("schemaId"));

		// describeGridSchema takes only the ToolContext, so it advertises a valid empty-object schema.
		JSONObject describeGridSchema = new JSONObject(callback("describeGridSchema").getToolDefinition().inputSchema());
		assertEquals("object", describeGridSchema.getString("type"));

		// Both parameters of the two-argument tool are required top-level properties.
		JSONObject writeSchema = new JSONObject(callback("writeSchemaToSession").getToolDefinition().inputSchema());
		assertEquals("string", writeSchema.getJSONObject("properties").getJSONObject("filePath").getString("type"));
		assertTrue(writeSchema.getJSONArray("required").toList().contains("schemaId"));
		assertTrue(writeSchema.getJSONArray("required").toList().contains("filePath"));
	}

	@Test
	public void testDescribeSchemaThroughCallback() {
		JsonSchema schema = createSchemaWithDefinition();
		when(mockJsonSchemaManager.getValidationSchema("my.org-MySchema-1.0.0")).thenReturn(schema);

		// call under test — the model supplies schemaId as a named JSON property.
		String response = callback("describeSchema").call("{\"schemaId\": \"my.org-MySchema-1.0.0\"}", toolContext);

		assertEquals(JDOSecondaryPropertyUtils.createJSONFromObject(new ToolResponse<>(schema)), response);
		verify(mockJsonSchemaManager).getValidationSchema("my.org-MySchema-1.0.0");
	}

	@Test
	public void testDescribeSchemaThroughCallbackMissingRequired() {
		// call under test — a missing required scalar is fed back as corrective guidance, not thrown.
		String response = callback("describeSchema").call("{}", toolContext);

		assertTrue(response.contains("missing required argument 'schemaId'"), response);
		verifyNoInteractions(mockJsonSchemaManager);
	}

	private JsonSchema createSchemaWithDefinition() {
		JsonSchema referenced = new JsonSchema();
		referenced.setType(Type.string);

		Map<String, JsonSchema> definitions = new LinkedHashMap<>();
		definitions.put("my.org-Other-1.0.0", referenced);

		JsonSchema property = new JsonSchema();
		property.set$ref("#/definitions/my.org-Other-1.0.0");

		Map<String, JsonSchema> properties = new LinkedHashMap<>();
		properties.put("other", property);

		JsonSchema schema = new JsonSchema();
		schema.set$id("https://repo-prod.prod.sagebase.org/repo/v1/schema/type/registered/my.org-MySchema-1.0.0");
		schema.setProperties(properties);
		schema.setDefinitions(definitions);
		return schema;
	}

	@Test
	public void testDescribeSchemaWithValidId() {
		JsonSchema schema = createSchemaWithDefinition();
		when(mockJsonSchemaManager.getValidationSchema("my.org-MySchema-1.0.0")).thenReturn(schema);

		// call under test
		ToolResponse<JsonSchema> response = tools.describeSchema("my.org-MySchema-1.0.0", toolContext);

		assertNotNull(response.getResponseBody());
		assertNull(response.getErrorMessage());
		assertEquals(schema, response.getResponseBody());
		verify(mockJsonSchemaManager).getValidationSchema("my.org-MySchema-1.0.0");
	}

	@Test
	public void testDescribeSchemaWithNotFound() {
		when(mockJsonSchemaManager.getValidationSchema("my.org-Missing"))
				.thenThrow(new IllegalArgumentException("Schema not found"));

		// call under test
		ToolResponse<JsonSchema> response = tools.describeSchema("my.org-Missing", toolContext);

		assertNull(response.getResponseBody());
		assertNotNull(response.getErrorMessage());
		assertTrue(response.getErrorMessage().contains("Schema not found"));
	}

	@Test
	public void testDescribeGridSchemaWithBoundSchema() {
		GridSession session = new GridSession().setGridJsonSchema$Id("my.org-MySchema-1.0.0");
		when(mockGridManager.getGridSession(userInfo, "grid-1")).thenReturn(session);
		JsonSchema schema = createSchemaWithDefinition();
		when(mockJsonSchemaManager.getValidationSchema("my.org-MySchema-1.0.0")).thenReturn(schema);

		// call under test — the $id is resolved from the grid session, not supplied by the caller.
		ToolResponse<JsonSchema> response = tools.describeGridSchema(toolContextWithGrid);

		assertNotNull(response.getResponseBody());
		assertNull(response.getErrorMessage());
		assertEquals(schema, response.getResponseBody());
		verify(mockGridManager).getGridSession(userInfo, "grid-1");
		verify(mockJsonSchemaManager).getValidationSchema("my.org-MySchema-1.0.0");
	}

	@Test
	public void testDescribeGridSchemaThroughCallback() {
		GridSession session = new GridSession().setGridJsonSchema$Id("my.org-MySchema-1.0.0");
		when(mockGridManager.getGridSession(userInfo, "grid-1")).thenReturn(session);
		JsonSchema schema = createSchemaWithDefinition();
		when(mockJsonSchemaManager.getValidationSchema("my.org-MySchema-1.0.0")).thenReturn(schema);

		// call under test — the model invokes the tool with no arguments; context carries the grid session.
		String response = callback("describeGridSchema").call("{}", toolContextWithGrid);

		assertEquals(JDOSecondaryPropertyUtils.createJSONFromObject(new ToolResponse<>(schema)), response);
	}

	@Test
	public void testDescribeGridSchemaWithNoBoundSchema() {
		GridSession session = new GridSession().setGridJsonSchema$Id(null);
		when(mockGridManager.getGridSession(userInfo, "grid-1")).thenReturn(session);

		// call under test
		ToolResponse<JsonSchema> response = tools.describeGridSchema(toolContextWithGrid);

		assertNull(response.getResponseBody());
		assertEquals("The current grid session has no bound JSON schema.", response.getErrorMessage());
		verifyNoInteractions(mockJsonSchemaManager);
	}

	@Test
	public void testDescribeGridSchemaWithNoUser() {
		// call under test — user context is required before any read.
		ToolResponse<JsonSchema> response = tools
				.describeGridSchema(new ToolContext(Map.of(AgentToolContextKey.GRID_SESSION_CONTEXT.getKey(), gridContext)));

		assertNull(response.getResponseBody());
		assertEquals("No user context available", response.getErrorMessage());
		verifyNoInteractions(mockGridManager);
		verifyNoInteractions(mockJsonSchemaManager);
	}

	@Test
	public void testDescribeGridSchemaWithNoGridContext() {
		// call under test — without a grid session there is nothing to resolve.
		ToolResponse<JsonSchema> response = tools
				.describeGridSchema(new ToolContext(Map.of(AgentToolContextKey.USER_INFO.getKey(), userInfo)));

		assertNull(response.getResponseBody());
		assertEquals("No grid session context available", response.getErrorMessage());
		verifyNoInteractions(mockGridManager);
		verifyNoInteractions(mockJsonSchemaManager);
	}

	@Test
	public void testDescribeGridSchemaWithNotFound() {
		GridSession session = new GridSession().setGridJsonSchema$Id("my.org-Missing");
		when(mockGridManager.getGridSession(userInfo, "grid-1")).thenReturn(session);
		when(mockJsonSchemaManager.getValidationSchema("my.org-Missing"))
				.thenThrow(new IllegalArgumentException("Schema not found"));

		// call under test
		ToolResponse<JsonSchema> response = tools.describeGridSchema(toolContextWithGrid);

		assertNull(response.getResponseBody());
		assertTrue(response.getErrorMessage().contains("Schema not found"));
	}

	@Test
	public void testWriteSchemaToSessionWithValidId() throws Exception {
		JsonSchema schema = createSchemaWithDefinition();
		when(mockJsonSchemaManager.getValidationSchema("my.org-MySchema-1.0.0")).thenReturn(schema);

		ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
		CodeExecutionResult successResult = new CodeExecutionResult("done", false, List.of());
		when(mockCodeInterpreterFileManager.pushLocalFileToSession(eq("session-123"), fileCaptor.capture(),
				eq("application/json"), eq("schema_specialist/my_schema.json")))
				.thenAnswer(invocation -> {
					File capturedFile = fileCaptor.getValue();
					String content = Files.readString(capturedFile.toPath());
					// The written file must contain the resolved definitions section
					assertTrue(content.contains("\"definitions\""));
					assertTrue(content.contains("my.org-Other-1.0.0"));
					assertTrue(content.contains("#/definitions/my.org-Other-1.0.0"));
					return successResult;
				});

		// call under test
		ToolResponse<JsonSchema> response = tools.writeSchemaToSession(
				"my.org-MySchema-1.0.0", "schema_specialist/my_schema.json", toolContextWithSession);

		assertNotNull(response.getResponseBody());
		assertNull(response.getErrorMessage());
		assertEquals(schema, response.getResponseBody());
	}

	@Test
	public void testWriteSchemaToSessionWithNoSessionId() {
		// call under test
		ToolResponse<JsonSchema> response = tools.writeSchemaToSession(
				"my.org-MySchema-1.0.0", "schema_specialist/my_schema.json", toolContext);

		assertNull(response.getResponseBody());
		assertEquals("No code interpreter session ID available", response.getErrorMessage());
		verifyNoInteractions(mockJsonSchemaManager);
		verifyNoInteractions(mockCodeInterpreterFileManager);
	}

	@Test
	public void testWriteSchemaToSessionWithPushError() {
		JsonSchema schema = createSchemaWithDefinition();
		when(mockJsonSchemaManager.getValidationSchema("my.org-MySchema-1.0.0")).thenReturn(schema);

		CodeExecutionResult errorResult = new CodeExecutionResult("Permission denied", true, List.of());
		when(mockCodeInterpreterFileManager.pushLocalFileToSession(eq("session-123"), any(File.class),
				eq("application/json"), eq("schema_specialist/my_schema.json")))
				.thenReturn(errorResult);

		// call under test
		ToolResponse<JsonSchema> response = tools.writeSchemaToSession(
				"my.org-MySchema-1.0.0", "schema_specialist/my_schema.json", toolContextWithSession);

		assertNull(response.getResponseBody());
		assertTrue(response.getErrorMessage().contains("Permission denied"));
	}

	@Test
	public void testWriteSchemaToSessionWithNotFound() {
		when(mockJsonSchemaManager.getValidationSchema("my.org-Missing"))
				.thenThrow(new IllegalArgumentException("Schema not found"));

		// call under test
		ToolResponse<JsonSchema> response = tools.writeSchemaToSession(
				"my.org-Missing", "schema_specialist/my_schema.json", toolContextWithSession);

		assertNull(response.getResponseBody());
		assertTrue(response.getErrorMessage().contains("Schema not found"));
		verifyNoInteractions(mockCodeInterpreterFileManager);
	}
}
