package org.sagebionetworks.repo.manager.grid.synch.handler;

import java.util.Iterator;

import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyItem;
import org.sagebionetworks.repo.model.dbo.grid.GridSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

/**
 * Handler for reading the current state of a grid copy (CRDT replica) during
 * synchronization. Provides access to the copy's metadata, schema, rows, and
 * change tracking information needed to synchronize with the source of truth.
 * 
 * <p>
 * This handler is read-only; changes to the copy are published as patches
 * through the IntendedChangePublisher rather than written directly through this
 * handler.
 */
public interface CopyHandler extends AutoCloseable {

	/**
	 * Gets the grid source identifier that this copy is replicating.
	 *
	 * @return the source identifier for this copy
	 */
	GridSource getGridSource();

	/**
	 * Gets the header metadata for this copy, including the current clock sequence
	 * maximum used for logical timestamp tracking.
	 *
	 * @return the grid header with CRDT metadata
	 */
	GridHeader getHeader();

	/**
	 * Gets the connection information needed to publish intended changes to this
	 * copy.
	 *
	 * @return the connection details for patch publishing
	 */
	GridConnectionInfo getConnectionInfo();


	/**
	 * Streams all rows currently in the copy. Used during Phase 2 (row
	 * synchronization) to compare copy rows with source rows.
	 *
	 * @return an iterator over all rows in the copy
	 */
	Iterator<RowCopyItem> getRows();

	/**
	 * Gets the last logical timestamp used for RGA (Replicated Growable Array) node
	 * creation. Used to generate unique timestamps for new items added to the copy
	 * during synchronization.
	 *
	 * @return the most recent RGA node timestamp
	 */
	LogicalTimestamp getLastRowsRgaNodeId();
}
