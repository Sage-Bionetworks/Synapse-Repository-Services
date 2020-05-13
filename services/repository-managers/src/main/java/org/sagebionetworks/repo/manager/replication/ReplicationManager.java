package org.sagebionetworks.repo.manager.replication;

import java.util.List;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.table.ViewObjectType;
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
	 * Replicate all data for a single object.
	 * 
	 * @param objectType The type of object
	 * @param objectId   The identifier of the object
	 * 
	 */
	void replicate(ViewObjectType objectType, String objectId);
	
	/**
	 * Reconcile the view with the given id
	 * 
	 * @param idAndVersion
	 */
	void reconcile(IdAndVersion idAndVersion);
}
