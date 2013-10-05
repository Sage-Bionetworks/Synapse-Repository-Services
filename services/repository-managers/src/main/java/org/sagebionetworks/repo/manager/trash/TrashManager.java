package org.sagebionetworks.repo.manager.trash;

import java.sql.Timestamp;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Manages entities in the trash can.
 */
public interface TrashManager {

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
	 * @throws UnauthorizedException
	 *             When the current user is not the same user nor an
	 *             administrator.
	 */
	QueryResults<TrashedEntity> viewTrashForUser(UserInfo currentUser, UserInfo userInfo,
			Long offset, Long limit) throws DatastoreException, UnauthorizedException;

	/**
	 * Retrieves all the trash entities in the trash can.
	 *
	 * @param currentUser
	 *            The user currently logged in. Must be an administrator.
	 * @throws UnauthorizedException
	 *             When the current user is not an administrator.
	 */
	QueryResults<TrashedEntity> viewTrash(UserInfo currentUser,
			Long offset, Long limit) throws DatastoreException, UnauthorizedException;

	/**
	 * Purges the specified entity from the trash can. After purging, the entity
	 * will be permanently deleted.
	 */
	void purgeTrashForUser(UserInfo currentUser, String nodeId)
			throws DatastoreException, NotFoundException;

	/**
	 * Purges the trash can for the user. All the entities in the trash will be
	 * permanently deleted.
	 */
	void purgeTrashForUser(UserInfo currentUser) throws DatastoreException, NotFoundException;

	/**
	 * Purges the trash can for the user. All the entities in the trash will be
	 * permanently deleted.
	 */
	void purgeTrash(UserInfo currentUser) throws DatastoreException,
	NotFoundException, UnauthorizedException;

	// The following two methods are for the trash worker to clean trash older than a month

	/**
	 * Gets the list of trashed items that were moved the trash can before
	 * the specified time.
	 */
	List<TrashedEntity> getTrashBefore(Timestamp timestamp) throws DatastoreException;

	/**
	 * Purges a list of trashed entities. Once purged, the entities will be permanently deleted.
	 */
	void purgeTrash(List<TrashedEntity> trashList) throws DatastoreException, NotFoundException;
}
