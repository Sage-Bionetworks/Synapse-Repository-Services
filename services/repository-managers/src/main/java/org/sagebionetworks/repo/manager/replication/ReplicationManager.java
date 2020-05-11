package org.sagebionetworks.repo.manager.replication;

import java.util.List;

import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

/**
 * Manager to replicate Entity to the table's database.
 * 
 * @author John
 *
 */
public interface ReplicationManager {

	/**
	 * Replicate all data for a batch of changes.
	 * 
	 * @param messages
	 * @throws RecoverableMessageException
	 */
	void replicate(List<ChangeMessage> messages) throws RecoverableMessageException;

	/**
	 * Replicate all data for a single entity.
	 * @param entityId
	 */
	void replicate(String entityId);
}
