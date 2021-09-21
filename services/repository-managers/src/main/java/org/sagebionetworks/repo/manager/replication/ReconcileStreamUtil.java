package org.sagebionetworks.repo.manager.replication;

import java.util.Iterator;

import org.sagebionetworks.repo.model.IdAndChecksum;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;

public interface ReconcileStreamUtil {
	
	/**
	 * Find all deltas between the passed streams.
	 * @param truth
	 * @param replication
	 * @return
	 */
	Iterator<ChangeMessage> reconcileStreams(ObjectType objectType, Iterator<IdAndChecksum> truth, Iterator<IdAndChecksum> replication);

}
