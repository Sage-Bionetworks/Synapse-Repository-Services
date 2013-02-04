package org.sagebionetworks.repo.model.dbo.dao;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Database operations on the trash can table.
 *
 * @author Eric Wu
 */
public interface DBOTrashCanDao {

	/**
	 * Creates a trash entity. This should happen when an entity is deleted into the trash can.
	 *
	 * @param userGroupId  The user who is deleting the item
	 * @param nodeId       The node being deleted
	 * @param parentId     The original parent
	 */
	void create(String userGroupId, String nodeId, String parentId) throws DatastoreException;

	/**
	 * How many entities are in this user's trash can.
	 */
	int getCount(String userGroupId) throws DatastoreException;

	/**
	 * Whether the user deleted the entity in the trash can.
	 */
	boolean exists(String userGroupId, String nodeId) throws DatastoreException;

	/**
	 * Gets the trash items deleted by the specified user. Results are paged as
	 * specified by offset (inclusive; staring from 0) and limit (the max number of items retrieved).
	 */
	List<TrashedEntity> getInRangeForUser(String userGroupId, long offset, long limit) throws DatastoreException;

	/**
	 * Removes a trash item from the trash can table. This happens when the trash item is either restored or purged.
	 *
	 * @throws NotFoundException When the item is not deleted by the user.
	 */
	void delete(String userGroupId, String nodeId) throws DatastoreException, NotFoundException;
}
