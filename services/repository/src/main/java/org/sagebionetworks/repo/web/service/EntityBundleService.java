package org.sagebionetworks.repo.web.service;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityBundleCreate;
import org.sagebionetworks.repo.model.InvalidModelException;
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
			HttpServletRequest request) throws NotFoundException,
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
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException;
	
	/**
	 * Create an entity and associated components with a single POST.
	 * Specifically, this operation supports creation of an Entity, its
	 * Annotations, and its ACL.
	 * 
	 * Upon successful creation, an EntityBundle is returned containing the
	 * created components, as defined by the partsMask.
	 * 
	 * @param userId
	 * @param eb
	 * @param partsMask
	 * @param request
	 * @return
	 * @throws ConflictingUpdateException
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws ParseException 
	 * @throws ACLInheritanceException 
	 */
	public EntityBundle createEntityBundle(String userId, EntityBundleCreate ebc,
			HttpServletRequest request)
			throws ConflictingUpdateException, DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException, ACLInheritanceException, ParseException;

	/**
	 * Update an entity and associated components with a single POST.
	 * Specifically, this operation supports creation of an Entity, its
	 * Annotations, and its ACL.
	 * 
	 * Upon successful creation, an EntityBundle is returned containing the
	 * updated components.
	 * 
	 * @param userId
	 * @param entityId
	 * @param ebc
	 * @param request
	 * @return
	 * @throws ConflictingUpdateException
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws ACLInheritanceException
	 * @throws ParseException
	 */
	public EntityBundle updateEntityBundle(String userId, String entityId,
			EntityBundleCreate ebc,	HttpServletRequest request) throws 
			ConflictingUpdateException,	DatastoreException, 
			InvalidModelException, UnauthorizedException, NotFoundException, 
			ACLInheritanceException, ParseException;

}
