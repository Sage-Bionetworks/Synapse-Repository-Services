package org.sagebionetworks.repo.web.service;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Generic service class to support controllers accessing UserGroups.
 *
 */
public interface UserGroupService {

	/**
	 * Get the user-groups in the system
	 * @param userId - The user that is making the request.
	 * @param request
	 * @return The UserGroups for individuals
	 * @throws DatastoreException - Thrown when there is a server-side problem.
	 */
	public PaginatedResults<UserGroup> getUserGroups(HttpServletRequest request,
			Long userId, Integer offset, Integer limit, String sort,
			Boolean ascending) throws DatastoreException,
			UnauthorizedException, NotFoundException;
}
