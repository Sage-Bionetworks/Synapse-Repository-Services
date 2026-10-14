package org.sagebionetworks.repo.manager.agent.specialist.gridupdate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.sagebionetworks.repo.manager.agent.specialist.ToolResponse;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityTool;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityToolBase;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityToolParam;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.query.FilterInstanceFactory;
import org.sagebionetworks.repo.model.grid.query.result.Row;
import org.sagebionetworks.repo.model.grid.update.GridUpdatePreviewResponse;
import org.sagebionetworks.repo.model.grid.update.GridUpdateRequest;
import org.sagebionetworks.repo.model.grid.update.GridUpdateResponse;
import org.sagebionetworks.repo.model.grid.update.SetValueInstanceFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Service;

/**
 * Grid update tool for the grid update specialist. Mirrors the low-level write path of the
 * legacy return-control {@code GridUpdateRequestHandler}: header reads use the user's
 * replica, while the update patches are published through the agent's replica, both
 * identified by the trusted {@link GridAgentSessionContext}.
 * <p>
 * The tool advertises the full {@link GridUpdateRequest} structure to the model as its
 * {@code inputSchema} (via {@link JSONEntityToolParam#schemaType()}), but receives the model's
 * argument as a raw {@link JSONObject} rather than a round-tripped POJO. This preserves the
 * {@code LiteralSetValue.value} distinction between an omitted value (undefined &mdash; leave the
 * cell untouched) and an explicit JSON null (clear the cell), which the generated POJO would
 * otherwise collapse. The raw payload flows straight to {@code executeGridUpdate}. Accepting a
	 * {@link JSONObject} (rather than a {@code String}) lets {@link JSONEntityToolBase} validate the
	 * payload's well-formedness and return corrective feedback to the model on malformed JSON, without
	 * sacrificing the undefined-vs-null fidelity a typed POJO would lose.
 * <p>
 * Authorization is established by construction: the {@link GridAgentSessionContext} is
 * seeded into the (agent-immutable) tool context by an upstream caller that has already
 * validated the user's access to the agent session, so this tool performs no additional
 * per-call authorization check.
 */
@Service
public class GridUpdateTools extends JSONEntityToolBase {

	private final GridManager gridManager;
	private final GridReplicaViewManager gridViewManager;

	public GridUpdateTools(GridManager gridManager, GridReplicaViewManager gridViewManager) {
		super();
		this.gridManager = gridManager;
		this.gridViewManager = gridViewManager;
	}

	@Override
	protected Collection<Iterator<String>> getPolymorphicImplementerSeeds() {
		return List.of(SetValueInstanceFactory.singleton().getKeySetIterator(),
				FilterInstanceFactory.singleton().getKeySetIterator());
	}

	@JSONEntityTool(name = "updateGrid", description = "Apply a structured batch of updates to the current grid "
			+ "session. Each Update in 'update.batch' pairs a set of column value assignments with filters "
			+ "selecting the rows to change.")
	public ToolResponse<GridUpdateResponse> updateGrid(
			@JSONEntityToolParam(schemaType = GridUpdateRequest.class,
					description = "The batch of updates to apply to the current grid session.",
					required = true) JSONObject update,
			ToolContext toolContext) {
		GridAgentSessionContext context = extractGridContext(toolContext);
		if (context == null) {
			return new ToolResponse<>("No grid session context available");
		}
		try {
			// The payload stays raw so an omitted 'value' (undefined) stays distinct from an explicit JSON null.
			JSONArray updateBatch = update.getJSONObject("update").getJSONArray("batch");
			ResolvedTarget target = resolveTarget(context);

			List<Long> updateCounts = new ArrayList<>();
			for (int i = 0; i < updateBatch.length(); i++) {
				updateCounts.add(gridManager.executeGridUpdate(target.header(), target.agentConnection(),
						updateBatch.getJSONObject(i)));
			}

			GridUpdateResponse response = new GridUpdateResponse().setUpdateResults(updateCounts)
					.setTotalRowsUpdated(updateCounts.stream().mapToLong(Long::longValue).sum());
			return new ToolResponse<>(response);
		} catch (Exception e) {
			return new ToolResponse<>("Error executing grid update: " + e.getMessage());
		}
	}

	@JSONEntityTool(name = "previewGridUpdate", description = "Preview a structured batch of updates against the "
			+ "current grid session WITHOUT applying any change. Returns a bounded sample of the rows each Update "
			+ "would affect — at most 10 rows per Update in the batch — showing how each row would look "
			+ "afterward, so the intended change can be verified before committing it with updateGrid. Takes the "
			+ "same argument as updateGrid.")
	public ToolResponse<GridUpdatePreviewResponse> previewGridUpdate(
			@JSONEntityToolParam(schemaType = GridUpdateRequest.class,
					description = "The batch of updates to preview against the current grid session.",
					required = true) JSONObject update,
			ToolContext toolContext) {
		GridAgentSessionContext context = extractGridContext(toolContext);
		if (context == null) {
			return new ToolResponse<>("No grid session context available");
		}
		try {
			// The payload stays raw so an omitted 'value' (undefined) stays distinct from an explicit JSON null.
			JSONArray updateBatch = update.getJSONObject("update").getJSONArray("batch");
			ResolvedTarget target = resolveTarget(context);

			List<Row> previewRows = new ArrayList<>();
			for (int i = 0; i < updateBatch.length(); i++) {
				previewRows.addAll(gridManager.executeGridUpdatePreview(target.header(), target.agentConnection(),
						updateBatch.getJSONObject(i)));
			}

			return new ToolResponse<>(new GridUpdatePreviewResponse().setPreviewRows(previewRows));
		} catch (Exception e) {
			return new ToolResponse<>("Error previewing grid update: " + e.getMessage());
		}
	}

	/**
	 * The header and publishing connection an update (or preview) resolves against: header reads use the
	 * user's replica, while patches publish through the agent's replica.
	 */
	private record ResolvedTarget(GridHeader header, GridConnectionInfo agentConnection) {}

	private ResolvedTarget resolveTarget(GridAgentSessionContext context) {
		GridConnectionInfo internalConnection = gridManager
				.getSingletonConnection(context.getGridSessionId(), EventSource.INTERNAL)
				.orElseThrow(() -> new IllegalArgumentException("Cannot get an internal grid connection."));

		GridHeader header = gridViewManager
				.readHeader(context.getGridSessionId(), internalConnection.getReplicaId(),
						context.getUsersReplicaId())
				.orElseThrow(() -> new IllegalArgumentException("Cannot read the grid header."));

		GridConnectionInfo agentConnection = gridManager
				.getConnection(context.getGridSessionId(), context.getAgentsReplicaId())
				.orElseThrow(() -> new IllegalArgumentException("Cannot get an agent grid connection."));

		return new ResolvedTarget(header, agentConnection);
	}

	private GridAgentSessionContext extractGridContext(ToolContext toolContext) {
		return (GridAgentSessionContext) AgentToolContextKey.GRID_SESSION_CONTEXT.get(toolContext);
	}
}
