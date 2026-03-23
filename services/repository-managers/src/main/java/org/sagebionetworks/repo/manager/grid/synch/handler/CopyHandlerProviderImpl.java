package org.sagebionetworks.repo.manager.grid.synch.handler;

import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.GridReplicaSupport;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.springframework.stereotype.Service;

@Service
public class CopyHandlerProviderImpl implements CopyHandlerProvider {

	private final GridReplicaViewManager gridReplicaViewManager;
	private final GridReplicaSupport gridReplicaSupport;
	private final GridIndexDao gridIndexDao;
	private final GridManager gridManager;

	public CopyHandlerProviderImpl(GridReplicaViewManager gridReplicaViewManager, GridReplicaSupport gridReplicaSupport,
			GridIndexDao gridIndexDao, GridManager gridManager) {
		super();
		this.gridReplicaViewManager = gridReplicaViewManager;
		this.gridReplicaSupport = gridReplicaSupport;
		this.gridIndexDao = gridIndexDao;
		this.gridManager = gridManager;
	}

	@Override
	public CopyHandler createCopyHandler(GridSession session) {
		return new CopyHandlerImpl(gridReplicaViewManager, gridReplicaSupport, gridIndexDao, gridManager, session);
	}

}
