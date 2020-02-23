package org.sagebionetworks.repo.manager.trash;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Manages entities in the trash can.
 */
public interface TrashManager {
	/** Returns true if the trash can has entities that were deleted from the specified entity. */
	boolean doesEntityHaveTrashedChildren(String entityId);

	/**
	 * Moves an entity and its descendants to the trash can, if the priorityPurge flag is true will delete the node as soon as possible.
	 */
	void moveToTrash(UserInfo currentUser, String nodeId, boolean priorityPurge) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Flags an entity in the trash can for immediate purge, the entity will not be shown in the trash can 
	 * and will be purged as soon as possible. Once flagged the entity will not be restorable.
	 * 
	 * @param userInfo
	 * @param nodeId
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	void flagForPurge(UserInfo userInfo, String nodeId) throws DatastoreException, NotFoundException;

	/**
	 * Moves an entity and its descendants out of the trash can. If the new parent is not given (null), will restore to the original parent.
	 */
	void restoreFromTrash(UserInfo currrentUser, String nodeId, String newParentId) throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Retrieves trash entities deleted by the specified user ordered by deletion date desc. 
	 * 
	 * Will not include entities that are marked with priority purge.
	 *
	 * @param currentUser
	 *            The user currently logged in.
	 * @param userInfo
	 *            The user who deleted the entities into the trash can, must be the same as the current user if the current user is not
	 *            an administrator
	 * @param offset
	 * 				Offset to begin paging results. Must be > 0.
	 * @param limit 
	 * 				Maximum number of results to return. Must be > 0.
	 * @throws UnauthorizedException
	 *             When the current user is not the same user nor an
	 *             administrator.
	 */
	List<TrashedEntity> listTrashedEntities(UserInfo currentUser, UserInfo userInfo, long offset, long limit) throws DatastoreException, UnauthorizedException;
	
	// The following two methods are for the trash worker to clean trash older than a month or flagged for purging
	
	/**
	 * Gets rowLimit amount of trash items that have no children trash items and are more than numDays old or that are flagged for priority purge.
	 * 
	 * @param numDays number of days the item has been in the trash can
	 * @param maxTrashItems maximum number of results to return
	 * @return Set of IDs of the trash items as Longs
	 * @throws DatastoreException
	 */
	List<Long> getTrashLeavesBefore(long numDays, long maxTrashItems) throws DatastoreException;
	
	/**
	 * Purges trash by a given list of their IDs as longs. User calling this must be an admin.
	 * @param user must be an admin user.
	 * @param trashIDs list of trashEntity IDs as longs
	 * @param purgeCallback optional
	 */
	void purgeTrash(UserInfo user, List<Long> trashIDs);
	
}
