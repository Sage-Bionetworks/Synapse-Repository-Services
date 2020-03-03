package org.sagebionetworks.repo.model.dbo.trash;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Database operations on the trash can table.
 */
public interface TrashCanDao {

	/**
	 * Creates a trash entity. This should happen when an entity is deleted into the trash can.
	 *
	 * @param userGroupId   The user who is deleting the item
	 * @param nodeId        The node being deleted
	 * @param nodeName      The name of the node being deleted
	 * @param parentId      The original parent
	 * @param priorityPurge Flag that indicates if the node should be deleted as soon as possible or after 30 days
	 */
	void create(String userGroupId, String nodeId, String nodeName, String parentId, boolean priorityPurge) throws DatastoreException;

	/** Returns true if the trash can has entities that were deleted from the specified entity. */
	boolean doesEntityHaveTrashedChildren(String entityId);

	/**
	 * Gets the trashed entity by entity ID. Returns null is the trashed entity does not exist.
	 */
	TrashedEntity getTrashedEntity(String nodeId) throws DatastoreException;

	/**
	 * Gets the trash items deleted by the specified user. Results are paged as specified by offset (inclusive; staring from
	 * 0) and limit (the max number of items retrieved) and ordered by deletion date desc.
	 * 
	 * Entities that were flagged for priority purge are not included.
	 */
	List<TrashedEntity> listTrashedEntities(String userId, long offset, long limit) throws DatastoreException;

	/**
	 * Gets the ids of nodes in the trash that have no children trash items and are more than numDays old or that are
	 * flagged for priority purge.
	 * 
	 * @param numDays number of days the item has been in the trash can
	 * @param limit   maximum number of results to return
	 * @return Set of IDs of the trash items as Longs
	 * @throws DatastoreException
	 */
	List<Long> getTrashLeavesIds(long numDays, long limit) throws DatastoreException;

	/**
	 * Flags the given list of node ids for priority purging, so that they are deleted as soon as possible
	 * 
	 * @param nodeIds
	 */
	void flagForPurge(List<Long> nodeIds);

	/**
	 * Removes all trash items in a list of node IDs
	 * 
	 * @param nodeIDs list of trash node IDs as longs
	 * @return int number of Trash nodes deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	int delete(List<Long> nodeIDs) throws DatastoreException, NotFoundException;

	/**
	 * How many entities are in the trash can (including entities with priority purge), for testing only.
	 */
	int getCount() throws DatastoreException;
	
	/**
	 * Clears the trash can table, for testing only
	 */
	void truncate();

}
