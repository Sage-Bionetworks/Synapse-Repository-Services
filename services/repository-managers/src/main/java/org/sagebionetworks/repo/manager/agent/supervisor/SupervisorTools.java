package org.sagebionetworks.repo.manager.agent.supervisor;

import org.sagebionetworks.repo.manager.agent.specialist.entitymetadata.EntityMetadataSpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.filesummary.FileSummarySpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.jsonschema.JsonSchemaSpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.tablequery.TableQuerySpecialistFactory;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * Tools that let a supervisor agent delegate focused sub-tasks to the specialist agents. Each tool
 * creates a fresh specialist (with its own conversation memory) and forwards the supervisor's
 * {@code userInfo} and {@code sessionId} so the specialist operates as the same user against the
 * same shared code interpreter session. The shared session is the hand-off medium: a file written
 * by one specialist can be read or summarized by another.
 */
@Service
public class SupervisorTools {

	private final TableQuerySpecialistFactory tableQuerySpecialistFactory;
	private final JsonSchemaSpecialistFactory jsonSchemaSpecialistFactory;
	private final FileSummarySpecialistFactory fileSummarySpecialistFactory;
	private final EntityMetadataSpecialistFactory entityMetadataSpecialistFactory;

	public SupervisorTools(TableQuerySpecialistFactory tableQuerySpecialistFactory,
			JsonSchemaSpecialistFactory jsonSchemaSpecialistFactory,
			FileSummarySpecialistFactory fileSummarySpecialistFactory,
			EntityMetadataSpecialistFactory entityMetadataSpecialistFactory) {
		this.tableQuerySpecialistFactory = tableQuerySpecialistFactory;
		this.jsonSchemaSpecialistFactory = jsonSchemaSpecialistFactory;
		this.fileSummarySpecialistFactory = fileSummarySpecialistFactory;
		this.entityMetadataSpecialistFactory = entityMetadataSpecialistFactory;
	}

	@Tool(description = "Delegate a task about a Synapse table or view to the table query specialist. "
			+ "The specialist can describe a table's schema, run SQL queries, and write query results as CSV "
			+ "files to the shared session. Provide a complete, self-contained instruction; the specialist has "
			+ "no memory of this conversation.")
	public String askTableQuerySpecialist(
			@ToolParam(description = "A complete, self-contained instruction for the table query specialist", required = true) String message,
			ToolContext toolContext) {
		return tableQuerySpecialistFactory.create().chat(message, extractUserInfo(toolContext), extractSessionId(toolContext));
	}

	@Tool(description = "Delegate a task about a Synapse JSON schema to the JSON schema specialist. "
			+ "The specialist can describe a schema (with all $ref references resolved into definitions) and write "
			+ "the resolved schema as a JSON file to the shared session. Provide a complete, self-contained "
			+ "instruction; the specialist has no memory of this conversation.")
	public String askJsonSchemaSpecialist(
			@ToolParam(description = "A complete, self-contained instruction for the JSON schema specialist", required = true) String message,
			ToolContext toolContext) {
		return jsonSchemaSpecialistFactory.create().chat(message, extractUserInfo(toolContext), extractSessionId(toolContext));
	}

	@Tool(description = "Delegate a task to the file summary specialist to inspect and summarize a file already "
			+ "present on the shared code interpreter session, without loading the whole file into your context. "
			+ "Use this to understand large files (CSV, JSON, PDF) that other specialists have produced. Provide a "
			+ "complete, self-contained instruction; the specialist has no memory of this conversation.")
	public String askFileSummarySpecialist(
			@ToolParam(description = "A complete, self-contained instruction for the file summary specialist", required = true) String message,
			ToolContext toolContext) {
		return fileSummarySpecialistFactory.create().chat(message, extractUserInfo(toolContext), extractSessionId(toolContext));
	}

	@Tool(description = "Delegate a task about a Synapse entity's metadata to the entity metadata specialist. "
			+ "The specialist can describe an entity, list its annotations (including schema-derived ones), report "
			+ "its JSON schema binding, list the children of a container, and copy FileEntity contents into the shared "
			+ "session. Provide a complete, self-contained instruction; the specialist has no memory of this conversation.")
	public String askEntityMetadataSpecialist(
			@ToolParam(description = "A complete, self-contained instruction for the entity metadata specialist", required = true) String message,
			ToolContext toolContext) {
		return entityMetadataSpecialistFactory.create().chat(message, extractUserInfo(toolContext), extractSessionId(toolContext));
	}

	private UserInfo extractUserInfo(ToolContext toolContext) {
		return (UserInfo) toolContext.getContext().get("userInfo");
	}

	private String extractSessionId(ToolContext toolContext) {
		return (String) toolContext.getContext().get("sessionId");
	}
}
