package org.sagebionetworks.repo.manager.agent.specialist.gridquery;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.sagebionetworks.repo.manager.agent.specialist.ToolResponse;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityTool;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityToolBase;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityToolParam;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.QueryElement;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.query.FilterInstanceFactory;
import org.sagebionetworks.repo.model.grid.query.QueryRequest;
import org.sagebionetworks.repo.model.grid.query.SelectItemInstanceFactory;
import org.sagebionetworks.repo.model.grid.query.result.QueryResult;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Service;

/**
 * Grid query tool for the grid query specialist. Mirrors the low-level read path of the
 * legacy return-control {@code GridQueryRequestHandler}: the query is read against the
 * user's replica of the grid session identified by the trusted {@link GridAgentSessionContext}.
 * <p>
 * The tool declares a typed {@link QueryRequest} parameter, so {@link JSONEntityToolBase}
 * advertises the full request structure (including the {@code Filter}/{@code SelectItem}
 * {@code oneOf} unions) to the model as the tool's {@code inputSchema}, and deserializes the
 * model's JSON through the {@code concreteType}-aware path.
 * <p>
 * Authorization is established by construction: the {@link GridAgentSessionContext} is
 * seeded into the (agent-immutable) tool context by an upstream caller that has already
 * validated the user's access to the agent session, so this tool performs no additional
 * per-call authorization check.
 */
@Service
public class GridQueryTools extends JSONEntityToolBase {

	private final GridReplicaViewManager viewManager;
	private final GridDao gridDao;

	public GridQueryTools(GridReplicaViewManager viewManager, GridDao gridDao) {
		super();
		this.viewManager = viewManager;
		this.gridDao = gridDao;
	}

	@Override
	protected Collection<Iterator<String>> getPolymorphicImplementerSeeds() {
		return List.of(FilterInstanceFactory.singleton().getKeySetIterator(),
				SelectItemInstanceFactory.singleton().getKeySetIterator());
	}

	@JSONEntityTool(name = "queryGrid", description = "Run a structured query against the current grid session and "
			+ "return the matching rows. The query uses JSON SelectItems and Filters (NOT SQL).")
	public ToolResponse<QueryResult> queryGrid(
			@JSONEntityToolParam(description = "The query to run against the current grid session.",
					required = true) QueryRequest request,
			ToolContext toolContext) {
		GridAgentSessionContext context = extractGridContext(toolContext);
		if (context == null) {
			return new ToolResponse<>("No grid session context available");
		}
		try {
			ValidateArgument.required(request.getQuery(), "request.query");
			// An absent columnSelection would otherwise silently degrade to select-all-rows, so an
			// aggregate request (e.g. CountStar) that fails to bind would return full rows with no
			// error. Rejecting it here lets the base class feed a corrective message back to the model.
			ValidateArgument.requiredNotEmpty(request.getQuery().getColumnSelection(),
					"request.query.columnSelection");
			QueryElement element = new QueryElement(request.getQuery());

			GridConnectionInfo internalConnection = gridDao
					.getSingletonConnection(context.getGridSessionId(), EventSource.INTERNAL)
					.orElseThrow(() -> new IllegalArgumentException("Cannot get an internal grid connection."));

			GridHeader header = viewManager
					.readHeader(context.getGridSessionId(), internalConnection.getReplicaId(),
							context.getUsersReplicaId())
					.orElseThrow(() -> new IllegalArgumentException("Grid session does not exist"));

			QueryResult result = viewManager.querySinglePageAsQueryResult(header, element);
			return new ToolResponse<>(result);
		} catch (Exception e) {
			return new ToolResponse<>("Error executing grid query: " + e.getMessage());
		}
	}

	private GridAgentSessionContext extractGridContext(ToolContext toolContext) {
		return (GridAgentSessionContext) AgentToolContextKey.GRID_SESSION_CONTEXT.get(toolContext);
	}
}
