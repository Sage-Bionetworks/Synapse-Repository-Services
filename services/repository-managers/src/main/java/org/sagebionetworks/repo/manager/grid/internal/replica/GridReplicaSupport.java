package org.sagebionetworks.repo.manager.grid.internal.replica;

import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

/**
 * Contains common logic shared by workers that use the internal replicas
 */
public interface GridReplicaSupport {

	/**
	 * @param session
	 * @return The grid header from the internal replica for the given session
	 * @throws RecoverableMessageException If the internal replica is not ready yet
	 */
	GridHeader getGridHeaderOrThrow(GridSession session) throws RecoverableMessageException;

	/**
	 * @param session
	 * @return The record set associated with the given set if the session was started from a record set
	 * @throws IllegalArgumentException If the session was not started from a record set
	 */
	RecordSet getRecordSetOrThrow(UserInfo user, GridSession session);
}
