package org.sagebionetworks.repo.manager.agent.specialist.jsonschema;

import java.io.File;
import java.io.FileWriter;

import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager;
import org.sagebionetworks.repo.manager.agent.specialist.JSONEntityResultConverter;
import org.sagebionetworks.repo.manager.agent.specialist.ToolResponse;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * Tools available to the JSON Schema specialist. These operate on the fully-resolved
 * "validation schema" for a given schema $id: all external $ref references are collapsed
 * into a single local "definitions" map and each $ref is rewritten to point at the local
 * definition (see {@link JsonSchemaManager#getValidationSchema(String)}).
 */
@Service
public class JsonSchemaTools {

	private final JsonSchemaManager jsonSchemaManager;
	private final CodeInterpreterFileManager codeInterpreterFileManager;

	public JsonSchemaTools(JsonSchemaManager jsonSchemaManager, CodeInterpreterFileManager codeInterpreterFileManager) {
		this.jsonSchemaManager = jsonSchemaManager;
		this.codeInterpreterFileManager = codeInterpreterFileManager;
	}

	@Tool(description = "Get the fully-resolved validation schema for a Synapse JSON schema $id. "
			+ "All external $ref references are resolved into the local 'definitions' section, and each $ref "
			+ "is rewritten to a local pointer such as '#/definitions/org.name-SchemaName-1.0.0'. Use this to "
			+ "answer questions about a schema's structure, properties, required fields, and referenced types.",
			resultConverter = JSONEntityResultConverter.class)
	public ToolResponse<JsonSchema> describeSchema(
			@ToolParam(description = "A JSON schema $id such as 'my.org-MySchema' or 'my.org-MySchema-1.0.0' for a specific version", required = true) String schemaId,
			ToolContext toolContext) {
		try {
			JsonSchema schema = jsonSchemaManager.getValidationSchema(schemaId);
			return new ToolResponse<>(schema);
		} catch (Exception e) {
			return new ToolResponse<>("Error describing schema '" + schemaId + "': " + e.getMessage());
		}
	}

	@Tool(description = "Write the fully-resolved validation schema for a Synapse JSON schema $id as a JSON file "
			+ "to the code interpreter session. All external $ref references are resolved into the local 'definitions' "
			+ "section. Use this to make a schema available on the session filesystem for further processing.",
			resultConverter = JSONEntityResultConverter.class)
	public ToolResponse<JsonSchema> writeSchemaToSession(
			@ToolParam(description = "A JSON schema $id such as 'my.org-MySchema' or 'my.org-MySchema-1.0.0' for a specific version", required = true) String schemaId,
			@ToolParam(description = "File path in the session, e.g. 'schema_specialist/my_schema.json'", required = true) String filePath,
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
		return (String) toolContext.getContext().get("sessionId");
	}

}
