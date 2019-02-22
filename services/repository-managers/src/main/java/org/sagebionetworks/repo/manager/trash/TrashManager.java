package org.sagebionetworks.repo.manager.trash;

import java.sql.Timestamp;
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

	public interface PurgeCallback {
		void startPurge(String id);
		
		void startPurge(List<Long> ids);

		void endPurge();
	}

	/**
	 * Moves an entity and its descendants to the trash can.
	 */
	void moveToTrash(UserInfo currentUser, String nodeId)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Moves an entity and its descendants out of the trash can. If the new
	 * parent is not given (null), will restore to the original parent.
	 */
	void restoreFromTrash(UserInfo currrentUser, String nodeId, String newParentId)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Retrieves trash entities deleted by the specified user.
	 *
	 * @param currentUser
	 *            The user currently logged in.
	 * @param userInfo
	 *            The user who deleted the entities into the trash can.
	 * @param offset
	 * 				Offset to begin paging results. Must be > 0.
	 * @param limit 
	 * 				Maximum number of results to return. Must be > 0.
	 * @throws UnauthorizedException
	 *             When the current user is not the same user nor an
	 *             administrator.
	 */
	List<TrashedEntity> viewTrashForUser(UserInfo currentUser, UserInfo userInfo,
			long offset, long limit) throws DatastoreException, UnauthorizedException;

	/**
	 * Retrieves all the trash entities in the trash can.
	 *
	 * @param currentUser
	 *            The user currently logged in. Must be an administrator.
	 * @param offset
	 * 				Offset to begin paging results. Must be > 0.
	 * @param limit 
	 * 				Maximum number of results to return. Must be > 0.
	 * @throws UnauthorizedException
	 *             When the current user is not an administrator.
	 */
	List<TrashedEntity> viewTrash(UserInfo currentUser,
			long offset, long limit) throws DatastoreException, UnauthorizedException;

	/**
	 * Purges the specified entity from the trash can. After purging, the entity
	 * will be permanently deleted.
	 */
	void purgeTrashForUser(UserInfo currentUser, String nodeId, PurgeCallback purgeCallback)
			throws DatastoreException, NotFoundException;

	/**
	 * Purges the trash can for the user. All the entities in the trash will be
	 * permanently deleted.
	 */
	void purgeTrashForUser(UserInfo currentUser, PurgeCallback purgeCallback) throws DatastoreException, NotFoundException;

	/**
	 * Purges the trash can for the user. All the entities in the trash will be
	 * permanently deleted.
	 */
	void purgeTrash(UserInfo currentUser, PurgeCallback purgeCallback) throws DatastoreException, NotFoundException, UnauthorizedException;

	// The following two methods are for the trash worker to clean trash older than a month

	/**
	 * Gets the list of trashed items that were moved the trash can before
	 * the specified time.
	 */
	List<TrashedEntity> getTrashBefore(Timestamp timestamp) throws DatastoreException;
	
	/**
	 * Gets rowLimit amount of trash items that have no children trash items and are more than numDays old.
	 * @param numDays number of days the item has been in the trash can
	 * @param maxTrashItems maximum number of results to return
	 * @return Set of IDs of the trash items as Longs
	 * @throws DatastoreException
	 */
	public List<Long> getTrashLeavesBefore(long numDays, long maxTrashItems) throws DatastoreException;

	/**
	 * Purges a list of trashed entities. Once purged, the entities will be permanently deleted.
	 */
	public void purgeTrash(List<TrashedEntity> trashList, PurgeCallback purgeCallback) throws DatastoreException, NotFoundException;
	
	/**
	 * Purges trash by a given list of their IDs as longs. User caling this must be an admin.
	 * @param trashIDs list of trashEntity IDs as longs
	 * @param user must be an admin user.
	 * @param purgeCallback optional
	 */
	public void purgeTrashAdmin(List<Long> trashIDs, UserInfo user);
	
}
