package org.sagebionetworks.repo.model;

import java.util.Collection;

import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;

public interface AccessApprovalDAO {

	/**
	 * @param dto
	 *            object to be created
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public String create(AccessApproval dto) throws DatastoreException,
			InvalidModelException;

	/**
	 * Retrieves the object given its id
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public AccessApproval get(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param accessRequirementIds
	 * @param principalIds
	 * @return the AccessApprovals for the given accessRequirements, for the given principals
	 * @throws DatastoreException
	 */
	public Collection<AccessApproval> getForAccessRequirementsAndPrincipals(Collection<String> accessRequirementIds, Collection<String> principalIds) throws DatastoreException;
	
	/**
	 * this updates the 'shallow' properties of an object
	 * 
	 * @param dto
	 *            non-null id is required
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public AccessApproval update(AccessApproval dto) throws DatastoreException, InvalidModelException,
			NotFoundException, ConflictingUpdateException;

	/**
	 * delete the object given by the given ID
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * gets the parameters of the given Access Approval Object
	 * @param id the ID of the AccessApproval object
	 * @param paramsDto
	 * @param schema
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void getAccessApprovalParameters(String id, Object paramsDto, ObjectSchema schema)  throws DatastoreException, NotFoundException;


	/**
	 * Set the parameters field of the Access Approval.
	 * @param id
	 * @param etag
	 * @param paramsDto
	 * @param schema
	 * @return the updated etag
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 */
	public String setAccessApprovalParameters(String id, String etag, Object paramsDto, ObjectSchema schema) throws DatastoreException, InvalidModelException,
	NotFoundException, ConflictingUpdateException;
	
}
