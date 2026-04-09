package org.sagebionetworks.repo.manager.grid.synch.handler;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dbo.grid.GridSource;
import org.sagebionetworks.repo.model.grid.GridSession;

/**
 * Factory interface for creating SourceHandler instances. Abstracts the
 * creation of handlers that read from and write to various source of truth
 * implementations (EntityView, Table, RecordSet, etc.), allowing for different
 * source types while maintaining a consistent synchronization interface.
 */
public interface SourceHandlerProvider {

	/**
	 * Creates a new SourceHandler for interacting with a specific source of truth
	 * during synchronization. The handler provides both read access to the source's
	 * current state and write operations for applying changes from the copy (CRDT
	 * replica).
	 *
	 * @param callback   progress callback for reporting synchronization status to
	 *                   the user
	 * @param user       the user performing the synchronization (used for
	 *                   authorization)
	 * @param session    the grid session containing connection information and user
	 *                   context
	 * @param gridSource identifier for the specific source of truth to synchronize
	 *                   with (determines which source implementation to create)
	 * @return a SourceHandler instance for the specified source type
	 */
	SourceHandler createNewProvider(AsyncJobProgressCallback callback, UserInfo user, GridSession session,
			GridSource gridSource) throws Exception;
}
