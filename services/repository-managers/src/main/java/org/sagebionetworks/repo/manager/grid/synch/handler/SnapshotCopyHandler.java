package org.sagebionetworks.repo.manager.grid.synch.handler;

import java.util.Iterator;

import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.synch.io.CopyRowSnapshot;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyItem;
import org.sagebionetworks.repo.model.dbo.grid.GridSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

/**
 * A {@link CopyHandler} that serves rows from a frozen {@link CopyRowSnapshot}
 * rather than the live grid query, while delegating all metadata to the
 * underlying live handler. The snapshot is captured once (at REPEATABLE_READ)
 * before any patch is published, so the merge never reads its own enqueued
 * writes (or a concurrent replica's writes) when it iterates rows in either
 * phase.
 *
 * <p>
 * This handler owns both the delegate and the snapshot; {@link #close()} shuts
 * down both.
 */
public class SnapshotCopyHandler implements CopyHandler {

	private final CopyHandler delegate;
	private final CopyRowSnapshot snapshot;

	/**
	 * @param delegate the live copy handler (supplies metadata; closed by this
	 *                 handler's {@link #close()})
	 * @param snapshot the frozen row snapshot (closed by this handler's
	 *                 {@link #close()})
	 */
	public SnapshotCopyHandler(CopyHandler delegate, CopyRowSnapshot snapshot) {
		this.delegate = delegate;
		this.snapshot = snapshot;
	}

	@Override
	public GridSource getGridSource() {
		return delegate.getGridSource();
	}

	@Override
	public GridHeader getHeader() {
		return delegate.getHeader();
	}

	@Override
	public GridConnectionInfo getConnectionInfo() {
		return delegate.getConnectionInfo();
	}

	@Override
	public Iterator<RowCopyItem> getRows() {
		return snapshot.read();
	}

	@Override
	public LogicalTimestamp getLastRowsRgaNodeId() {
		return delegate.getLastRowsRgaNodeId();
	}

	@Override
	public void close() throws Exception {
		delegate.close();
		snapshot.close();
	}
}
