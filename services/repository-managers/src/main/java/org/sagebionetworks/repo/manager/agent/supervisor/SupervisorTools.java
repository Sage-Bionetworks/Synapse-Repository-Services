package org.sagebionetworks.repo.manager.agent.supervisor;

import org.sagebionetworks.repo.manager.agent.specialist.entitymetadata.EntityMetadataSpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.filesummary.FileSummarySpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.gridmetadata.GridMetadataSpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.gridquery.GridQuerySpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.gridupdate.GridUpdateSpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.jsonschema.JsonSchemaSpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.tablequery.TableQuerySpecialistFactory;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityTool;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityToolBase;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityToolParam;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Service;

/**
 * Tools that let a supervisor agent delegate focused sub-tasks to the specialist agents. Each tool
 * creates a fresh specialist (with its own conversation memory) and hands it the supervisor's own
 * tool context unchanged, so the specialist operates as the same user, against the same shared code
 * interpreter session and grid session, with the same job trace. The shared session is the hand-off
 * medium: a file written by one specialist can be read or summarized by another.
 */
@Service
public class SupervisorTools extends JSONEntityToolBase {

	public static final String TOOL_TABLE_QUERY = "askTableQuerySpecialist";
	public static final String TOOL_JSON_SCHEMA = "askJsonSchemaSpecialist";
	public static final String TOOL_FILE_SUMMARY = "askFileSummarySpecialist";
	public static final String TOOL_ENTITY_METADATA = "askEntityMetadataSpecialist";
	public static final String TOOL_GRID_QUERY = "askGridQuerySpecialist";
	public static final String TOOL_GRID_UPDATE = "askGridUpdateSpecialist";
	public static final String TOOL_GRID_METADATA = "askGridMetadataSpecialist";

	private final TableQuerySpecialistFactory tableQuerySpecialistFactory;
	private final JsonSchemaSpecialistFactory jsonSchemaSpecialistFactory;
	private final FileSummarySpecialistFactory fileSummarySpecialistFactory;
	private final EntityMetadataSpecialistFactory entityMetadataSpecialistFactory;
	private final GridQuerySpecialistFactory gridQuerySpecialistFactory;
	private final GridUpdateSpecialistFactory gridUpdateSpecialistFactory;
	private final GridMetadataSpecialistFactory gridMetadataSpecialistFactory;

	public SupervisorTools(TableQuerySpecialistFactory tableQuerySpecialistFactory,
			JsonSchemaSpecialistFactory jsonSchemaSpecialistFactory,
			FileSummarySpecialistFactory fileSummarySpecialistFactory,
			EntityMetadataSpecialistFactory entityMetadataSpecialistFactory,
			GridQuerySpecialistFactory gridQuerySpecialistFactory,
			GridUpdateSpecialistFactory gridUpdateSpecialistFactory,
			GridMetadataSpecialistFactory gridMetadataSpecialistFactory) {
		super();
		this.tableQuerySpecialistFactory = tableQuerySpecialistFactory;
		this.jsonSchemaSpecialistFactory = jsonSchemaSpecialistFactory;
		this.fileSummarySpecialistFactory = fileSummarySpecialistFactory;
		this.entityMetadataSpecialistFactory = entityMetadataSpecialistFactory;
		this.gridQuerySpecialistFactory = gridQuerySpecialistFactory;
		this.gridUpdateSpecialistFactory = gridUpdateSpecialistFactory;
		this.gridMetadataSpecialistFactory = gridMetadataSpecialistFactory;
	}

	@JSONEntityTool(name = TOOL_TABLE_QUERY, description = "Delegate a task about a Synapse table or view to the table query specialist. "
			+ "The specialist can describe a table's schema, run SQL queries, and write query results as CSV "
			+ "files to the shared session. Provide a complete, self-contained instruction; the specialist has "
			+ "no memory of this conversation.")
	public String askTableQuerySpecialist(
			@JSONEntityToolParam(description = "A complete, self-contained instruction for the table query specialist", required = true) String message,
			ToolContext toolContext) {
		return tableQuerySpecialistFactory.create().chat(message, toolContext);
	}

	@JSONEntityTool(name = TOOL_JSON_SCHEMA, description = "Delegate a task about a Synapse JSON schema to the JSON schema specialist. "
			+ "The specialist can describe a schema (with all $ref references resolved into definitions) and write "
			+ "the resolved schema as a JSON file to the shared session. Within a grid session it can also describe the "
			+ "grid's currently-bound schema directly, resolving the $id itself — you do not need to look it up first. "
			+ "Provide a complete, self-contained instruction; the specialist has no memory of this conversation.")
	public String askJsonSchemaSpecialist(
			@JSONEntityToolParam(description = "A complete, self-contained instruction for the JSON schema specialist", required = true) String message,
			ToolContext toolContext) {
		return jsonSchemaSpecialistFactory.create().chat(message, toolContext);
	}

	@JSONEntityTool(name = TOOL_FILE_SUMMARY, description = "Delegate a task to the file summary specialist to inspect and summarize a file already "
			+ "present on the shared code interpreter session, without loading the whole file into your context. "
			+ "Use this to understand large files (CSV, JSON, PDF) that other specialists have produced. Provide a "
			+ "complete, self-contained instruction; the specialist has no memory of this conversation.")
	public String askFileSummarySpecialist(
			@JSONEntityToolParam(description = "A complete, self-contained instruction for the file summary specialist", required = true) String message,
			ToolContext toolContext) {
		return fileSummarySpecialistFactory.create().chat(message, toolContext);
	}

	@JSONEntityTool(name = TOOL_ENTITY_METADATA, description = "Delegate a task about a Synapse entity's metadata to the entity metadata specialist. "
			+ "The specialist can describe an entity, list its annotations (including schema-derived ones), report "
			+ "its JSON schema binding, list the children of a container, and copy FileEntity contents into the shared "
			+ "session. Provide a complete, self-contained instruction; the specialist has no memory of this conversation.")
	public String askEntityMetadataSpecialist(
			@JSONEntityToolParam(description = "A complete, self-contained instruction for the entity metadata specialist", required = true) String message,
			ToolContext toolContext) {
		return entityMetadataSpecialistFactory.create().chat(message, toolContext);
	}

	@JSONEntityTool(name = TOOL_GRID_QUERY, description = "Delegate a task about the current grid session to the grid query specialist. "
			+ "The specialist runs structured queries (using JSON SelectItems and Filters, not SQL) to read and "
			+ "summarize grid data, including validation state, and can filter to the user's selection. Provide a "
			+ "complete, self-contained instruction; the specialist has no memory of this conversation.")
	public String askGridQuerySpecialist(
			@JSONEntityToolParam(description = "A complete, self-contained instruction for the grid query specialist", required = true) String message,
			ToolContext toolContext) {
		return gridQuerySpecialistFactory.create().chat(message, toolContext);
	}

	@JSONEntityTool(name = TOOL_GRID_UPDATE, description = "Delegate a task about the current grid session to the grid update specialist. "
			+ "The specialist applies structured updates (using JSON SetValues and Filters, not SQL) to change grid "
			+ "cell values, including literal and template-based transformations, restricted by filters. Provide a "
			+ "complete, self-contained instruction; the specialist has no memory of this conversation.")
	public String askGridUpdateSpecialist(
			@JSONEntityToolParam(description = "A complete, self-contained instruction for the grid update specialist", required = true) String message,
			ToolContext toolContext) {
		return gridUpdateSpecialistFactory.create().chat(message, toolContext);
	}

	@JSONEntityTool(name = TOOL_GRID_METADATA, description = "Delegate a task about the current grid session's metadata to the grid metadata specialist. "
			+ "The specialist describes the grid session (including its source and bound JSON schema $id) and the "
			+ "replicas participating in it, so it can explain the replicaIds that appear in query results — who last "
			+ "changed a cell or row (a user, an agent, or the system), and who else is working on the session. Provide "
			+ "a complete, self-contained instruction; the specialist has no memory of this conversation.")
	public String askGridMetadataSpecialist(
			@JSONEntityToolParam(description = "A complete, self-contained instruction for the grid metadata specialist", required = true) String message,
			ToolContext toolContext) {
		return gridMetadataSpecialistFactory.create().chat(message, toolContext);
	}
}
