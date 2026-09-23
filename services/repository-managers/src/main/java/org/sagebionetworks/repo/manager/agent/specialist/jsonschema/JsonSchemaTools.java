package org.sagebionetworks.repo.manager.agent.specialist.jsonschema;

import java.io.File;
import java.io.FileWriter;

import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.sagebionetworks.repo.manager.agent.CodeSessionSupplier;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager;
import org.sagebionetworks.repo.manager.agent.specialist.ToolResponse;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityTool;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityToolBase;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityToolParam;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Service;

/**
 * Tools available to the JSON Schema specialist. These operate on the fully-resolved
 * "validation schema" for a schema $id: all external $ref references are collapsed
 * into a single local "definitions" map and each $ref is rewritten to point at the local
 * definition (see {@link JsonSchemaManager#getValidationSchema(String)}).
 * <p>
 * A caller that already knows the $id uses {@link #describeSchema}. When the specialist is
 * delegated a task within a grid session (the trusted {@link GridAgentSessionContext} is present
 * in the {@link ToolContext}), {@link #describeGridSchema} resolves the grid's currently-bound
 * schema $id from the live session itself, so the supervisor never has to look up and forward a
 * $id that could be stale if the binding changed.
 */
@Service
public class JsonSchemaTools extends JSONEntityToolBase {

	private final JsonSchemaManager jsonSchemaManager;
	private final CodeInterpreterFileManager codeInterpreterFileManager;
	private final GridManager gridManager;

	public JsonSchemaTools(JsonSchemaManager jsonSchemaManager, CodeInterpreterFileManager codeInterpreterFileManager,
			GridManager gridManager) {
		super();
		this.jsonSchemaManager = jsonSchemaManager;
		this.codeInterpreterFileManager = codeInterpreterFileManager;
		this.gridManager = gridManager;
	}

	@JSONEntityTool(description = "Get the fully-resolved validation schema for a Synapse JSON schema $id. "
			+ "All external $ref references are resolved into the local 'definitions' section, and each $ref "
			+ "is rewritten to a local pointer such as '#/definitions/org.name-SchemaName-1.0.0'. Use this to "
			+ "answer questions about a schema's structure, properties, required fields, and referenced types.")
	public ToolResponse<JsonSchema> describeSchema(
			@JSONEntityToolParam(description = "A JSON schema $id such as 'my.org-MySchema' or 'my.org-MySchema-1.0.0' for a specific version", required = true) String schemaId,
			ToolContext toolContext) {
		try {
			JsonSchema schema = jsonSchemaManager.getValidationSchema(schemaId);
			return new ToolResponse<>(schema);
		} catch (Exception e) {
			return new ToolResponse<>("Error describing schema '" + schemaId + "': " + e.getMessage());
		}
	}

	@JSONEntityTool(description = "Get the fully-resolved validation schema bound to the CURRENT grid session, without "
			+ "needing a schema $id. The grid's bound schema $id is resolved automatically from the live session, so the "
			+ "result is always current even if the binding has changed. Use this to answer questions about the structure, "
			+ "properties, required fields, and constraints that the grid's data must satisfy. Only available when operating "
			+ "within a grid session.")
	public ToolResponse<JsonSchema> describeGridSchema(ToolContext toolContext) {
		UserInfo userInfo = extractUserInfo(toolContext);
		if (userInfo == null) {
			return new ToolResponse<>("No user context available");
		}
		GridAgentSessionContext gridContext = extractGridContext(toolContext);
		if (gridContext == null) {
			return new ToolResponse<>("No grid session context available");
		}
		try {
			GridSession session = gridManager.getGridSession(userInfo, gridContext.getGridSessionId());
			String schemaId = session.getGridJsonSchema$Id();
			if (schemaId == null) {
				return new ToolResponse<>("The current grid session has no bound JSON schema.");
			}
			return new ToolResponse<>(jsonSchemaManager.getValidationSchema(schemaId));
		} catch (Exception e) {
			return new ToolResponse<>("Error describing the grid's schema: " + e.getMessage());
		}
	}

	@JSONEntityTool(description = "Write the fully-resolved validation schema for a Synapse JSON schema $id as a JSON file "
			+ "to the code interpreter session. All external $ref references are resolved into the local 'definitions' "
			+ "section. Use this to make a schema available on the session filesystem for further processing.")
	public ToolResponse<JsonSchema> writeSchemaToSession(
			@JSONEntityToolParam(description = "A JSON schema $id such as 'my.org-MySchema' or 'my.org-MySchema-1.0.0' for a specific version", required = true) String schemaId,
			@JSONEntityToolParam(description = "File path in the session, e.g. 'schema_specialist/my_schema.json'", required = true) String filePath,
			ToolContext toolContext) {
		String sessionId = extractSessionId(toolContext);
		if (sessionId == null) {
			return new ToolResponse<>("No code interpreter session ID available");
		}
		File tempFile = null;
		try {
			JsonSchema schema = jsonSchemaManager.getValidationSchema(schemaId);
			String schemaJson = JDOSecondaryPropertyUtils.createJSONFromObject(schema);

			tempFile = File.createTempFile("schema_", ".json");
			try (FileWriter fileWriter = new FileWriter(tempFile)) {
				fileWriter.write(schemaJson);
			}

			CodeExecutionResult pushResult = codeInterpreterFileManager.pushLocalFileToSession(sessionId, tempFile,
					"application/json", filePath);
			if (pushResult.isError()) {
				return new ToolResponse<>("Error writing schema to session: " + pushResult.textOutput());
			}

			return new ToolResponse<>(schema);
		} catch (Exception e) {
			return new ToolResponse<>("Error writing schema '" + schemaId + "' to session: " + e.getMessage());
		} finally {
			if (tempFile != null) {
				tempFile.delete();
			}
		}
	}

	private String extractSessionId(ToolContext toolContext) {
		return CodeSessionSupplier.resolveSessionId(toolContext);
	}

	private UserInfo extractUserInfo(ToolContext toolContext) {
		return (UserInfo) AgentToolContextKey.USER_INFO.get(toolContext);
	}

	private GridAgentSessionContext extractGridContext(ToolContext toolContext) {
		return (GridAgentSessionContext) AgentToolContextKey.GRID_SESSION_CONTEXT.get(toolContext);
	}

}
