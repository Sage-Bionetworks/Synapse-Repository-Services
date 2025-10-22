package org.sagebionetworks.repo.manager.grid.internal.replica;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.GridReplicaPatchBuilderManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Service;

@Service
public class GridReplicaSupportImpl implements GridReplicaSupport {
	
	private final GridManager gridManager;
	
	private final GridReplicaViewManager viewManager;
	
	private final EntityManager entityManager;
	
	private final GridReplicaPatchBuilderManager patchBuilderManager;

	public GridReplicaSupportImpl(GridManager gridManager, GridReplicaViewManager viewManager, EntityManager entityManager, GridReplicaPatchBuilderManager patchBuilderManager) {
		this.gridManager = gridManager;
		this.viewManager = viewManager;
		this.entityManager = entityManager;
		this.patchBuilderManager = patchBuilderManager;
	}

	@Override
	public GridHeader getGridHeaderOrThrow(GridSession session) throws RecoverableMessageException {
		String gridSessionId = session.getSessionId();
		
        GridConnectionInfo connectionInfo = gridManager.getSingletonConnection(gridSessionId, EventSource.INTERNAL)
                .orElseThrow(() -> new RecoverableMessageException("No internal connection found for session: " + gridSessionId));

        patchBuilderManager.getCurrentClockIfAllPatchesApplied(connectionInfo.getSessionId(), connectionInfo.getReplicaId())
                .orElseThrow(() -> new RecoverableMessageException("Current clock could not be retrieved, patches are still being applied to sessionId: " + connectionInfo.getSessionId() + ", replicaId: " + connectionInfo.getReplicaId()));
        
        return viewManager.readHeader(connectionInfo.getSessionId(), connectionInfo.getReplicaId())
                .orElseThrow(() -> new RecoverableMessageException("Grid header has not yet been instantiated for sessionId: " + gridSessionId));
	}

	@Override
	public RecordSet getRecordSetOrThrow(UserInfo user, GridSession session) {
		Entity entity = entityManager.getEntity(user, session.getSourceEntityId());
		
		ValidateArgument.requirement(entity instanceof RecordSet, "Unsupported grid session: only a grid created from a record set is supported.");
		
		return (RecordSet) entity;
	}

}
