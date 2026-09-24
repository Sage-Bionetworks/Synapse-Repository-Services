package org.sagebionetworks.repo.manager.grid.synch.handler;

import org.sagebionetworks.repo.manager.grid.synch.GridRepeatableReadTransaction;
import org.sagebionetworks.repo.manager.grid.synch.io.CopyRowSnapshot;
import org.sagebionetworks.util.FileProvider;
import org.springframework.stereotype.Service;

/**
 * Captures a consistent, disk-backed snapshot of a grid replica's copy rows
 * within a single REPEATABLE_READ transaction, so the synchronization merge
 * reads a frozen point-in-time view rather than the live, paginated query. This
 * isolates the merge from the asynchronous patches it enqueues (read-your-own-writes)
 * and from concurrent replicas writing mid-sync.
 */
@Service
public class GridCopySnapshotProvider {

	private final FileProvider fileProvider;

	public GridCopySnapshotProvider(FileProvider fileProvider) {
		this.fileProvider = fileProvider;
	}

	/**
	 * Capture all the copy handler's rows into a re-readable disk snapshot. The
	 * read runs at REPEATABLE_READ so every page sees the same MVCC view.
	 *
	 * @param copyHandler the live copy handler
	 * @return a disk-backed snapshot (caller must {@link CopyRowSnapshot#close()} it)
	 */
	@GridRepeatableReadTransaction
	public CopyRowSnapshot capture(CopyHandler copyHandler) {
		return CopyRowSnapshot.capture(copyHandler.getRows(), fileProvider);
	}
}
