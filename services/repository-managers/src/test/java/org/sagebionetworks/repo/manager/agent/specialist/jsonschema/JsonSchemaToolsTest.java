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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager;
import org.sagebionetworks.repo.manager.agent.specialist.ToolResponse;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Type;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.ai.chat.model.ToolContext;

@ExtendWith(MockitoExtension.class)
public class JsonSchemaToolsTest {

	@Mock
	private JsonSchemaManager mockJsonSchemaManager;

	@Mock
	private CodeInterpreterFileManager mockCodeInterpreterFileManager;

	private JsonSchemaTools tools;
	private ToolContext toolContext;
	private ToolContext toolContextWithSession;

	@BeforeEach
	public void setup() {
		tools = new JsonSchemaTools(mockJsonSchemaManager, mockCodeInterpreterFileManager);
		toolContext = new ToolContext(Map.of());
		toolContextWithSession = new ToolContext(Map.of("sessionId", "session-123"));
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
