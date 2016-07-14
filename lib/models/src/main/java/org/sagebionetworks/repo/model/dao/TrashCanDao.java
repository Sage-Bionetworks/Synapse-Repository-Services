package org.sagebionetworks.repo.model.dao;

import java.sql.Timestamp;
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
	 * @param userGroupId  The user who is deleting the item
	 * @param nodeId       The node being deleted
	 * @param nodeName     The name of the node being deleted
	 * @param parentId     The original parent
	 */
	void create(String userGroupId, String nodeId, String nodeName, String parentId) throws DatastoreException;

	/**
	 * How many entities are in this user's trash can.
	 */
	int getCount(String userGroupId) throws DatastoreException;

	/**
	 * How many entities are in the trash can.
	 */
	int getCount() throws DatastoreException;

	/**
	 * Whether the user deleted the entity in the trash can.
	 */
	boolean exists(String userGroupId, String nodeId) throws DatastoreException;

	/**
	 * Gets the trashed entity. Returns null is the trashed entity does not exist.
	 */
	TrashedEntity getTrashedEntity(String userGroupId, String nodeId) throws DatastoreException;

	/**
	 * Gets the trashed entity by entity ID. Returns null is the trashed entity does not exist.
	 */
	TrashedEntity getTrashedEntity(String nodeId) throws DatastoreException;

	/**
	 * Gets the trash items deleted by the specified user. Results are paged as
	 * specified by offset (inclusive; staring from 0) and limit (the max number of items retrieved).
	 */
	List<TrashedEntity> getInRangeForUser(String userGroupId, boolean sortById, long offset, long limit) throws DatastoreException;

	/**
	 * Gets all the trash items. Results are paged as specified by offset (inclusive; staring from 0)
	 * and limit (the max number of items retrieved).
	 */
	List<TrashedEntity> getInRange(boolean sortById, long offset, long limit) throws DatastoreException;

	/**
	 * Gets all the trash items that were deleted before the specified time stamp.
	 */
	List<TrashedEntity> getTrashBefore(Timestamp timestamp) throws DatastoreException;
	
	/**
	 * Gets all the trash items that were deleted before the specified time stamp with no children trash items
	 */
	List<TrashedEntity> getTrashLeavesBefore(Timestamp timestamp) throws DatastoreException;
	
	/**
	 * Removes a trash item from the trash can table. This happens when the trash item is either restored or purged.
	 *
	 * @throws NotFoundException When the item is not deleted by the user.
	 */
	void delete(String userGroupId, String nodeId) throws DatastoreException, NotFoundException;
	
	
}
