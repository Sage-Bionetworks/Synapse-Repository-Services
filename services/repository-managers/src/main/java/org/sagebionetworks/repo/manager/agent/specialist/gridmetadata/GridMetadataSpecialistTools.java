package org.sagebionetworks.repo.manager.agent.specialist.gridmetadata;

import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.sagebionetworks.repo.manager.agent.specialist.ToolResponse;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityTool;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityToolBase;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityToolParam;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.grid.GridReplicaInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.ListGridReplicasRequest;
import org.sagebionetworks.repo.model.grid.ListGridReplicasResponse;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Service;

/**
 * Tools available to the grid metadata specialist. They describe the current grid session and the
 * replicas participating in it, so the agent can interpret the {@code replicaId}s it encounters in
 * query results (who last changed a cell/row, which replica belongs to the chatting user, and which
 * belong to other users, their agents, or the system). All reads operate as the user carried in the
 * {@link ToolContext}; the grid session is fixed by the trusted {@link GridAgentSessionContext}, so
 * the agent can never target a session other than its own.
 * <p>
 * These tools take no argument or plain scalar arguments, so {@link JSONEntityToolBase} advertises
 * them with a generated scalar {@code inputSchema} and wires each method as a logged
 * {@link org.springframework.ai.tool.ToolCallback}.
 */
@Service
public class GridMetadataSpecialistTools extends JSONEntityToolBase {

	private final GridManager gridManager;
	private final PrincipalAliasDAO principalAliasDAO;

	public GridMetadataSpecialistTools(GridManager gridManager, PrincipalAliasDAO principalAliasDAO) {
		super();
		this.gridManager = gridManager;
		this.principalAliasDAO = principalAliasDAO;
	}

	@JSONEntityTool(name = "getGridSession", description = "Get information about the current grid session, including "
			+ "its source entity, the bound JSON schema $id used to validate rows, and its owner. Use this to answer "
			+ "questions about what the grid represents and which schema its data is validated against.")
	public ToolResponse<GridSession> getGridSession(ToolContext toolContext) {
		UserInfo userInfo = extractUserInfo(toolContext);
		if (userInfo == null) {
			return new ToolResponse<>("No user context available");
		}
		GridAgentSessionContext gridContext = extractGridContext(toolContext);
		if (gridContext == null) {
			return new ToolResponse<>("No grid session context available");
		}
		try {
			return new ToolResponse<>(gridManager.getGridSession(userInfo, gridContext.getGridSessionId()));
		} catch (Exception e) {
			return new ToolResponse<>("Error getting grid session: " + e.getMessage());
		}
	}

	@JSONEntityTool(name = "getReplicaInfo", description = "Look up a single replica of the current grid session by its "
			+ "replicaId. Query results identify who last changed a cell or row by its replicaId; use this to resolve "
			+ "that replicaId to its type (USER, AGENT, or SERVICE), the user who created it, and whether it is "
			+ "currently connected. A SERVICE replica indicates a system change such as a source reset or reload "
			+ "during synchronization.")
	public ToolResponse<GridReplicaInfo> getReplicaInfo(
			@JSONEntityToolParam(description = "The replicaId to look up, as seen in a query result",
					required = true) Long replicaId,
			ToolContext toolContext) {
		UserInfo userInfo = extractUserInfo(toolContext);
		if (userInfo == null) {
			return new ToolResponse<>("No user context available");
		}
		GridAgentSessionContext gridContext = extractGridContext(toolContext);
		if (gridContext == null) {
			return new ToolResponse<>("No grid session context available");
		}
		try {
			return new ToolResponse<>(gridManager.getReplicaInfo(userInfo, gridContext.getGridSessionId(), replicaId));
		} catch (Exception e) {
			return new ToolResponse<>("Error getting replica '" + replicaId + "': " + e.getMessage());
		}
	}

	@JSONEntityTool(name = "listReplicas", description = "List all replicas participating in the current grid session, "
			+ "with each replica's type (USER, AGENT, or SERVICE), the user who created it, and its current connection "
			+ "status. Use this to answer who else is working on the grid session. Results are paged; if the response "
			+ "includes a nextPageToken, pass it back to retrieve the next page.")
	public ToolResponse<ListGridReplicasResponse> listReplicas(
			@JSONEntityToolParam(description = "A page token from a previous response to get the next page of replicas",
					required = false) String nextPageToken,
			ToolContext toolContext) {
		UserInfo userInfo = extractUserInfo(toolContext);
		if (userInfo == null) {
			return new ToolResponse<>("No user context available");
		}
		GridAgentSessionContext gridContext = extractGridContext(toolContext);
		if (gridContext == null) {
			return new ToolResponse<>("No grid session context available");
		}
		try {
			return new ToolResponse<>(gridManager.listReplicas(userInfo, new ListGridReplicasRequest()
					.setGridSessionId(gridContext.getGridSessionId())
					.setNextPageToken(nextPageToken)));
		} catch (Exception e) {
			return new ToolResponse<>("Error listing replicas: " + e.getMessage());
		}
	}

	@JSONEntityTool(name = "getUserName", description = "Resolve a user ID to its Synapse username. Replicas report the "
			+ "user that created them by numeric user ID; use this to present a human-readable username. Usernames are "
			+ "public in Synapse.")
	public String getUserName(
			@JSONEntityToolParam(description = "The numeric Synapse user ID to resolve", required = true) String userId,
			ToolContext toolContext) {
		try {
			return principalAliasDAO.getUserName(Long.parseLong(userId));
		} catch (Exception e) {
			return "Error getting username for user '" + userId + "': " + e.getMessage();
		}
	}

	private UserInfo extractUserInfo(ToolContext toolContext) {
		return (UserInfo) AgentToolContextKey.USER_INFO.get(toolContext);
	}

	private GridAgentSessionContext extractGridContext(ToolContext toolContext) {
		return (GridAgentSessionContext) AgentToolContextKey.GRID_SESSION_CONTEXT.get(toolContext);
	}

}
