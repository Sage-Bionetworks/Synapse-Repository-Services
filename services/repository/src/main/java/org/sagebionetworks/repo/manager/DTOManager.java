package org.sagebionetworks.repo.manager;

import java.util.Collection;

import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.web.NotFoundException;

public interface DTOManager<T extends Base> {
	/**
	 * @param dto
	 *            object to be created
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public String create(T dto) throws DatastoreException,
			InvalidModelException, UnauthorizedException;

	/**
	 * Retrieves the object given its id
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public T get(String id) throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * This updates the 'shallow' properties of an object
	 * 
	 * @param dto
	 *            non-null id is required
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public void update(T dto) throws DatastoreException, InvalidModelException,
			NotFoundException, UnauthorizedException;

	/**
	 * delete the object given by the given ID
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id) throws DatastoreException, NotFoundException, UnauthorizedException;

	// TODO need a clean way to convey Public access
	/**
	 * Use case:  Need to find out who has authority to add a new user to a group.
	 * Here the 'resource' refers to the group and 'accessType' = 'change'.  The method
	 * would return the administrative group who can modify the group of interest.
	 * 
	 * @param resource the resource of interest
	 * @param accessType a type of access to the object
	 * @return those user groups that have the specified type of access to the given object
	 */
	public Collection<UserGroup> whoHasAccess(T resource, String accessType) throws NotFoundException, DatastoreException ;
	
	/**
	 * Use case:  Need to find out if a user can download a resource.
	 * 
	 * @param resource the resource of interest
	 * @param user
	 * @param accessType
	 * @return
	 */
	public boolean hasAccess(T resource, String accessType) throws NotFoundException, DatastoreException ;


}
