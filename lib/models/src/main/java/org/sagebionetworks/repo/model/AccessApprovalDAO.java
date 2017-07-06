package org.sagebionetworks.repo.model;

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
	 * delete all access approval that approves accessorId to access requirementId
	 * 
	 * @param accessRequirementId
	 * @param accessorId
	 * @param revokedBy
	 */
	public void revokeAll(String accessRequirementId, String accessorId, String revokedBy);

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
	 * Retrieve a list of access approvals for a given subjectIdList
	 * 
	 * @param subjectIdList
	 * @param type
	 * @param limit
	 * @param offset
	 * @return
	 */
	public List<AccessApproval> getAccessApprovalsForSubjects(List<String> subjectIdList, RestrictableObjectType type,
			long limit, long offset);

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
	 * submitted by submitterId; false otherise.
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
	 * Revoke a group of accessors
	 * 
	 * @param accessRequirementId
	 * @param submitterId
	 * @param revokedBy
	 */
	public void revokeGroup(String accessRequirementId, String submitterId, String revokedBy);

	/**
	 * Revoke a batch of accessors
	 * 
	 * @param approvalsToRenew
	 */
	public void revokeBySubmitter(String accessRequirementId, String submitterId, List<String> accessors, String revokedBy);

	/**
	 * Retrieve a set of AccessRequirement ID that userId has access approval for within the provided accessRequirementIds
	 * 
	 * @param userId
	 * @param accessRequirementIds
	 * @return
	 */
	public Set<String> getRequirementsUserHasApprovals(String userId, List<String> accessRequirementIds);
}
