package org.sagebionetworks.repo.model;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.dataaccess.AccessorGroup;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessApprovalDAO {

	/**
	 * @param dto
	 *            object to be created
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public AccessApproval create(AccessApproval dto);

	/**
	 * Retrieves the object given its id
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public AccessApproval get(String id) throws NotFoundException;

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
	 * Return true if there is an unmet access requirement for the given user; false otherwise.
	 * 
	 * @param requirementIdSet
	 * @param userId
	 * @return
	 */
	public Boolean hasUnmetAccessRequirement(Set<String> requirementIdSet, String userId);

	/**
	 * Create or update a batch of access approval
	 * 
	 * @param approvalsToCreate - objects to be created
	 */
	public void createOrUpdateBatch(List<AccessApproval> approvalsToCreate);

	/**
	 * Retrieve all active approvals, approvals that have APPROVED state and haven't expired, for the given user.
	 * 
	 * @param accessRequirementId
	 * @param userId
	 * @return
	 */
	public List<AccessApproval> getActiveApprovalsForUser(String accessRequirementId, String userId);

	/**
	 * Retrieve an access approval given the primary fields
	 * 
	 * @param requirementId
	 * @param requirementVersion
	 * @param submitterId
	 * @param accessorId
	 * @return
	 */
	public AccessApproval getByPrimaryKey(Long requirementId, Long requirementVersion, String submitterId, String accessorId);

	/**
	 * @param accessorsIds
	 * @param string
	 * @param accessRequirementId
	 * @return true if all accessors have an approval for the given access requirement
	 * (regardless of its state) submitted by submitterId; false otherise.
	 */
	public boolean hasApprovalsSubmittedBy(Set<String> accessorsIds, String submitterId, String accessRequirementId);

	/**
	 * Retrieve a page of AccessorGroup, group by accessRequirementId and submitterId.
	 * 
	 * @param accessRequirementId
	 * @param submitterId
	 * @param expireBefore
	 * @param limit
	 * @param offset
	 * @return
	 */
	public List<AccessorGroup> listAccessorGroup(String accessRequirementId, String submitterId, Date expireBefore,
			long limit, long offset);

	/**
	 * Retrieve a set of AccessRequirement ID that userId has access approval for within the provided accessRequirementIds
	 * 
	 * @param userId
	 * @param accessRequirementIds
	 * @return
	 */
	public Set<String> getRequirementsUserHasApprovals(String userId, List<String> accessRequirementIds);
	
	/**
	 * Fetch the list of approval ids for the given access requirement and accessor id
	 * 
	 * @param accessRequirementId The id of the access requirement
	 * @param accessorId The id of the accessor
	 * @return The list of approved access approval ids for the given access requirement and accessor
	 */
	List<Long> listApprovalsByAccessor(String accessRequirementId, String accessorId);
	
	/**
	 * Fetch the list of approval ids for the given access requirement and submitter id
	 * 
	 * @param accessRequirementId The id of the access requirement
	 * @param submitterId The id of the submitter
	 * @return The list of approved access approval ids for the given access requirement and submitter id
	 */
	List<Long> listApprovalsBySubmitter(String accessRequirementId, String submitterId);
	
	/**
	 * Fetch the list of approval ids for the given access requirement, submitter id and list of accessors
	 * 
	 * @param accessRequirementId The id of the access requirement
	 * @param submitterId The id of the submitter
	 * @return The list of approved access approval ids for the given access requirement and submitter id
	 */
	List<Long> listApprovalsBySubmitter(String accessRequirementId, String submitterId, List<String> accessorIds);
	
	/**
	 * Fetch the list of expired approval ids
	 * 
	 * @param expiredAfter Minimum expiration date
	 * @param limit Maximum number of approval to fetch
	 * @return The list of approval ids that are expired
	 */
	List<Long> listExpiredApprovals(Instant expiredAfter, int limit);
	
	/**
	 * Checks if the accessor with the given id has any approval for the given access requirement.
	 * 
	 * @param accessRequirementId The id of the access requirement
	 * @param accessorId The id of the accessor
	 * @return True if the accessor has at least one approval for the requirement with the given id
	 */
	boolean hasAccessorApproval(String accessRequirementId, String accessorId);
	
	/**
	 * Checks if the given submmiter has any approval for the given access requirement that does not have an expiration
	 * date or that expires after the given instant.
	 * 
	 * @param accessRequirementId The id of the access requirement
	 * @param submitterId The id of the submitter
	 * @param expireAfter The expiration instant to filter for, approvals that do not expire count towards the submitter approvals
	 * @return True if the submmiter has at least one approval for the requirement with the given id that expire after the given instant, false otherwise
	 */
	boolean hasSubmitterApproval(String accessRequirementId, String submitterId, Instant expireAfter);

	/**
	 * Revokes the given batch of ids
	 * 
	 * @param userId The id of the user revoking access
	 * @param ids The list of access approval ids to revoke
	 * @return The list of ids of the revoked access approvals
	 */
	List<Long> revokeBatch(Long userId, List<Long> ids);
	
	// For testing
	
	void clear();
}
