package org.sagebionetworks.repo.manager.agent.context;

import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class GridContextValidatorHandler implements AgentContextValidatorHandler<GridAgentSessionContext> {

	private final GridManager gridManager;

	public GridContextValidatorHandler(GridManager gridManager) {
		super();
		this.gridManager = gridManager;
	}

	@Override
	public Class<? extends GridAgentSessionContext> getContextType() {
		return GridAgentSessionContext.class;
	}

	@Override
	public GridAgentSessionContext doContextValidation(UserInfo user, GridAgentSessionContext context) {
		ValidateArgument.required(context, "context");
		if (context.getAgentsReplicaId() != null) {
			throw new IllegalArgumentException("The agentsReplicaId must be null");
		}

		// the user must have access to the replica
		gridManager.getReplica(user, context.getGridSessionId(), context.getUsersReplicaId());
		GridSession session = gridManager.getGridSession(user, context.getGridSessionId());
		/*
		 * create the replica that will be used by this agent to make updates to the
		 * grid.
		 */
		GridReplica agentsReplica = gridManager.createAgentReplica(user, session);
		context.setAgentsReplicaId(agentsReplica.getReplicaId());
		return context;
	}

}
