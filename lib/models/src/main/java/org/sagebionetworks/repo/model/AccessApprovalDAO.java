package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.List;

import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessApprovalDAO {

	/**
	 * @param dto
	 *            object to be created
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public <T extends AccessApproval> T create(T dto) throws DatastoreException, InvalidModelException;

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
	 * Get all the access approvals related to the given access requirement.  This is used by the migrator.
	 * @param accessRequirementId
	 * @return
	 * @throws DatastoreException
	 */
	public List<AccessApproval> getForAccessRequirement(String accessRequirementId) throws DatastoreException;
	
	/**
	 * 
	 * @param accessRequirementIds
	 * @param principalIds
	 * @return the AccessApprovals for the given accessRequirements, for the given principals
	 * @throws DatastoreException
	 */
	public List<AccessApproval> getForAccessRequirementsAndPrincipals(Collection<String> accessRequirementIds, Collection<String> principalIds) throws DatastoreException;

	/**
	 * Updates the 'shallow' properties of an object.
	 *
	 * @param dto
	 *            non-null id is required
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public <T extends AccessApproval> T  update(T dto) throws DatastoreException, InvalidModelException,
			NotFoundException, ConflictingUpdateException;

	/**
	 * Updates the 'shallow' properties of an object from backup.
	 *
	 * @param dto
	 *            non-null id is required
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public <T extends AccessApproval> T  updateFromBackup(T dto) throws DatastoreException, InvalidModelException,
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

}
