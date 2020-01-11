package org.sagebionetworks.repo.web.service;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * A service for entities in the trash can.
 */
public interface TrashService {

	/**
	 * Moves an entity and its descendants to the trash can, if the priorityPurge flag is set to true the entity
	 * will not be restorable and will be deleted as soon as possible
	 */
	void moveToTrash(Long currentUserId, String entityId, boolean priorityPurge)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Moves an entity and its descendants out of the trash can.
	 */
	void restoreFromTrash(Long currentUserId, String entityId, String newParentId)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Retrieves trash entities deleted by the specified user.
	 *
	 * @param currentUserId
	 *            The user currently logged in.
	 * @param userId
	 *            The user who deleted the entities into the trash can.
	 * @throws UnauthorizedException
	 *             When the current user is not the same user nor an
	 *             administrator.
	 */
	PaginatedResults<TrashedEntity> viewTrashForUser(Long currentUserId, Long userId,
			Long offset, Long limit, HttpServletRequest request)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Flags the specified entity for priority purge. After being flagged the entity will not be restorable 
	 * and will be deleted as soon as possible.
	 */
	void flagForPurge(Long currentUserId, String entityId)
			throws DatastoreException, NotFoundException;	
}
