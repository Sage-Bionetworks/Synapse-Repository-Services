package org.sagebionetworks.repo.web.service;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * A service for entities in the trash can.
 *
 * @author Eric Wu
 */
public interface TrashService {

	/**
	 * Moves an entity and its descendants to the trash can.
	 */
	void moveToTrash(String userId, String entityId)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Moves an entity and its descendants out of the trash can.
	 */
	void restoreFromTrash(String userId, String entityId, String newParentId)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Retrieves entities (in the trash can) deleted by the specified user.
	 */
	PaginatedResults<TrashedEntity> viewTrash(String userId, Long offset, Long limit,
			HttpServletRequest request) throws DatastoreException, NotFoundException;
}
