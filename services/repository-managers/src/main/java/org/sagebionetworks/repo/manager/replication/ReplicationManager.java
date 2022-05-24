package org.sagebionetworks.repo.manager.replication;

import java.util.List;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.table.ReplicationType;
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
	void replicate(ReplicationType replicationType, String objectId);
	
	/**
	 * Reconcile the truth with the replication for the objects in the given view.
	 * 
	 * @param viewId The Id of the view.
	 * @param viewType The type of the view.
	 */
	void reconcile(IdAndVersion viewId, ObjectType viewType);

	/**
	 * Is the reconciliation synchronized for the objects in the given view?
	 * Note: This call will be O(n) when the reconciliation is synchronized for objects in this view..
	 * @param viewObjectType - The type of view.
	 * @param viewId
	 * @return
	 */
	boolean isReplicationSynchronizedForView(ViewObjectType viewObjectType, IdAndVersion viewId);
}
