package org.sagebionetworks.repo.manager.grid.synch;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.grid.SynchronizeGridRequest;
import org.sagebionetworks.repo.model.grid.SynchronizeGridResponse;

/**
 * Manager interface for orchestrating bidirectional synchronization between a
 * grid copy (CRDT replica) and its source of truth. The synchronization process
 * ensures consistency between the user's local changes and external changes
 * made to the source.
 */
public interface GridSynchronizationManager {

	/**
	 * Synchronizes a grid copy with its source of truth using a two-phase approach:
	 *
	 * <p>
	 * Phase 1: Schema Synchronization
	 * <ul>
	 * <li>Synchronizes column definitions between copy and source</li>
	 * <li>Resolves schema conflicts (no merge needed for columns)</li>
	 * <li>Produces a final synchronized schema for Phase 2</li>
	 * </ul>
	 *
	 * <p>
	 * Phase 2: Row Synchronization
	 * <ul>
	 * <li>Synchronizes row data using the final schema from Phase 1</li>
	 * <li>Merges cell-level changes when rows conflict</li>
	 * <li>Uses disk-based serialization to avoid loading all source data into
	 * memory</li>
	 * </ul>
	 *
	 * <p>
	 * The synchronization process:
	 * <ul>
	 * <li>Pushes user changes from copy to source</li>
	 * <li>Pulls external changes from source to copy</li>
	 * <li>Publishes intended changes as patches for CRDT consistency</li>
	 * </ul>
	 *
	 * @param callback progress callback for reporting synchronization status
	 * @param user     the user performing the synchronization
	 * @param session  the grid session containing connection and state information
	 * @throws Exception if synchronization fails due to I/O, authentication, or
	 *                   conflict resolution errors
	 */
	SynchronizeGridResponse synchronizeCopyWithSource(AsyncJobProgressCallback jobProgressCallback, UserInfo user,
			SynchronizeGridRequest request) throws Exception;

}
