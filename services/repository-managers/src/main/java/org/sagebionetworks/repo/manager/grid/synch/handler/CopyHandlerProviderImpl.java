package org.sagebionetworks.repo.manager.grid.synch.handler;

import java.io.IOException;

import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.GridReplicaSupport;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.manager.grid.synch.io.CopyRowSnapshot;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.springframework.stereotype.Service;

@Service
public class CopyHandlerProviderImpl implements CopyHandlerProvider {

	private final GridReplicaViewManager gridReplicaViewManager;
	private final GridReplicaSupport gridReplicaSupport;
	private final GridIndexDao gridIndexDao;
	private final GridManager gridManager;
	private final GridCopySnapshotProvider copySnapshotProvider;

	public CopyHandlerProviderImpl(GridReplicaViewManager gridReplicaViewManager, GridReplicaSupport gridReplicaSupport,
			GridIndexDao gridIndexDao, GridManager gridManager, GridCopySnapshotProvider copySnapshotProvider) {
		super();
		this.gridReplicaViewManager = gridReplicaViewManager;
		this.gridReplicaSupport = gridReplicaSupport;
		this.gridIndexDao = gridIndexDao;
		this.gridManager = gridManager;
		this.copySnapshotProvider = copySnapshotProvider;
	}

	/**
	 * Creates a live {@link CopyHandlerImpl} to read grid metadata, captures a
	 * consistent REPEATABLE_READ snapshot of its rows, then wraps both in a
	 * {@link SnapshotCopyHandler} that owns and closes them. The snapshot is taken
	 * here — before any patch is published — so the merge never sees its own
	 * enqueued writes.
	 */
	@Override
	public CopyHandler createCopyHandler(GridSession session) throws IOException {
		CopyHandlerImpl liveHandler = new CopyHandlerImpl(gridReplicaViewManager, gridReplicaSupport, gridIndexDao,
				gridManager, session);
		CopyRowSnapshot snapshot = copySnapshotProvider.capture(liveHandler);
		return new SnapshotCopyHandler(liveHandler, snapshot);
	}

}
