package org.sagebionetworks.repo.web.service;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Service interface for fetching an EntityBundle
 * 
 * @author bkng
 */
public interface EntityBundleService {

	/**
	 * Get an entity and related data with a single GET. Note that childCount is
	 * calculated in the QueryController.
	 * 
	 * @param userId -The user that is doing the get.
	 * @param entityId - The ID of the entity to fetch.
	 * @param request
	 * @return The requested Entity if it exists.
	 * @throws NotFoundException - Thrown if the requested entity does not exist.
	 * @throws DatastoreException - Thrown when an there is a server failure. 
	 * @throws UnauthorizedException
	 * @throws ACLInheritanceException 
	 * @throws ParseException 
	 */
	public EntityBundle getEntityBundle(String userId, String entityId, int mask,
			HttpServletRequest request, Integer offset, Integer limit,
			String sort, Boolean ascending) throws NotFoundException,
			DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException;


	/**
	 * Get an entity and related data with a single GET. Note that childCount is
	 * calculated in the QueryController.
	 * 
	 * @param userId -The user that is doing the get.
	 * @param entityId - The ID of the entity to fetch.
	 * @param versionNumber - The version Number of the entity to fetch
	 * @param request
	 * @return The requested Entity if it exists.
	 * @throws NotFoundException - Thrown if the requested entity does not exist.
	 * @throws DatastoreException - Thrown when an there is a server failure. 
	 * @throws UnauthorizedException
	 * @throws ACLInheritanceException 
	 * @throws ParseException 
	 */
	public EntityBundle getEntityBundle(String userId, String entityId, Long versionNumber, int mask,
			HttpServletRequest request, Integer offset, Integer limit,
			String sort, Boolean ascending) throws NotFoundException,
			DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException;

}
