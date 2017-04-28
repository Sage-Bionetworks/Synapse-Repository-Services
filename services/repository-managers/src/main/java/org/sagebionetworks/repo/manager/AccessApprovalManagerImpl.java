package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessApproval;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.Count;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PostMessageContentAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalResult;
import org.sagebionetworks.repo.model.dataaccess.BatchAccessApprovalRequest;
import org.sagebionetworks.repo.model.dataaccess.BatchAccessApprovalResult;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class AccessApprovalManagerImpl implements AccessApprovalManager {
	public static final Long DEFAULT_LIMIT = 50L;
	public static final Long MAX_LIMIT = 50L;
	public static final Long DEFAULT_OFFSET = 0L;
	
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;
	@Autowired
	private AccessApprovalDAO accessApprovalDAO;
	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private NodeDAO nodeDao;
	
	// check an incoming object (i.e. during 'create' and 'update')
	private void validateAccessApproval(AccessApproval accessApproval) {
		ValidateArgument.required(accessApproval.getAccessorId(), "accessorId");
		ValidateArgument.required(accessApproval.getRequirementId(), "accessRequirementId");

		// make sure the approval matches the requirement
		AccessRequirement ar = accessRequirementDAO.get(accessApproval.getRequirementId().toString());
		ValidateArgument.requirement(!(ar instanceof LockAccessRequirement)
				&& !(ar instanceof PostMessageContentAccessRequirement), "Cannot apply an approval to a "+ar.getConcreteType());

		if (((ar instanceof TermsOfUseAccessRequirement) && !(accessApproval instanceof TermsOfUseAccessApproval))
			|| ((ar instanceof ACTAccessRequirement) && !(accessApproval instanceof ACTAccessApproval))) {
			throw new IllegalArgumentException("Cannot apply an approval of type "+accessApproval.getClass().getSimpleName()+" to an access requirement of type "+ar.getClass().getSimpleName());
		}
	}

	public static void populateCreationFields(UserInfo userInfo, AccessApproval a) {
		Date now = new Date();
		a.setCreatedBy(userInfo.getId().toString());
		a.setCreatedOn(now);
		a.setModifiedBy(userInfo.getId().toString());
		a.setModifiedOn(now);
	}

	public static void populateModifiedFields(UserInfo userInfo, AccessApproval a) {
		Date now = new Date();
		a.setCreatedBy(null); // by setting to null we are telling the DAO to use the current values
		a.setCreatedOn(null);
		a.setModifiedBy(userInfo.getId().toString());
		a.setModifiedOn(now);
	}
	
	@Override
	public AccessApproval getAccessApproval(UserInfo userInfo, String approvalId) 
			throws DatastoreException, NotFoundException {
		return accessApprovalDAO.get(approvalId);
	}


	@WriteTransactionReadCommitted
	@Override
	public <T extends AccessApproval> T createAccessApproval(UserInfo userInfo, T accessApproval) throws DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException {

		if (accessApproval instanceof TermsOfUseAccessApproval) {
			// fill in the user's identity
			accessApproval.setAccessorId(userInfo.getId().toString());
		}

		validateAccessApproval(accessApproval);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(authorizationManager.canCreateAccessApproval(userInfo, accessApproval));
		populateCreationFields(userInfo, accessApproval);
		return accessApprovalDAO.create(accessApproval);
	}

	@Override
	public List<AccessApproval> getAccessApprovalsForSubject(UserInfo userInfo,
			RestrictableObjectDescriptor rod, Long limit, Long offset)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccessAccessApprovalsForSubject(userInfo, rod, ACCESS_TYPE.READ));

		if (limit == null) {
			limit = DEFAULT_LIMIT;
		}
		if (offset == null) {
			offset = DEFAULT_OFFSET;
		}
		ValidateArgument.requirement(limit > 0 && limit <= MAX_LIMIT,
				"Limit must be between 0 and "+MAX_LIMIT);
		ValidateArgument.requirement(offset >= 0, "Offset must be at least 0");
		List<String> subjectIds = new ArrayList<String>();
		if (RestrictableObjectType.ENTITY==rod.getType()) {
			subjectIds.addAll(AccessRequirementUtil.getNodeAncestorIds(nodeDao, rod.getId(), true));
		} else {
			subjectIds.add(rod.getId());
		}
		return accessApprovalDAO.getAccessApprovalsForSubjects(subjectIds, rod.getType(), limit, offset);
	}

	@WriteTransactionReadCommitted
	@Override
	public <T extends AccessApproval> T  updateAccessApproval(UserInfo userInfo, T accessApproval) throws NotFoundException,
			DatastoreException, UnauthorizedException,
			ConflictingUpdateException, InvalidModelException {

		if (accessApproval instanceof TermsOfUseAccessApproval) {
			// fill in the user's identity
			accessApproval.setAccessorId(userInfo.getId().toString());
		}

		validateAccessApproval(accessApproval);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(authorizationManager.canAccess(userInfo, accessApproval.getId().toString(), ObjectType.ACCESS_APPROVAL, ACCESS_TYPE.UPDATE));
		populateModifiedFields(userInfo, accessApproval);
		return accessApprovalDAO.update(accessApproval);
	}

	@WriteTransactionReadCommitted
	@Override
	public void deleteAccessApproval(UserInfo userInfo, String accessApprovalId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		AccessApproval accessApproval = accessApprovalDAO.get(accessApprovalId);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, accessApproval.getId().toString(), 
						ObjectType.ACCESS_APPROVAL, ACCESS_TYPE.DELETE));
			
		accessApprovalDAO.delete(accessApproval.getId().toString());
	}

	@WriteTransactionReadCommitted
	@Override
	public void deleteAccessApprovals(UserInfo userInfo, String accessRequirementId, String accessorId)
			throws UnauthorizedException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		ValidateArgument.required(accessorId, "accessorId");
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member may delete an access approval.");
		}
		AccessRequirement accessRequirement = accessRequirementDAO.get(accessRequirementId);
		ValidateArgument.requirement(accessRequirement.getConcreteType().equals(ACTAccessRequirement.class.getName()),
				"Do not support access approval deletion for access requirement type: "+accessRequirement.getConcreteType());
		accessApprovalDAO.delete(accessRequirementId, accessorId);
	}

	@WriteTransactionReadCommitted
	@Override
	public Count deleteBatch(UserInfo userInfo, IdList toDelete) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(toDelete, "toDelete");
		ValidateArgument.requirement(toDelete.getList() != null && !toDelete.getList().isEmpty(),
				"toDelete must has at least one item.");
		if (!userInfo.isAdmin()) {
			throw new UnauthorizedException("Only admin can use this API.");
		}
		Count result = new Count();
		result.setCount(new Long(accessApprovalDAO.deleteBatch(toDelete.getList())));
		return result;
	}

	@Override
	public BatchAccessApprovalResult getApprovalInfo(UserInfo userInfo, BatchAccessApprovalRequest batchRequest) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(batchRequest, "batchRequest");
		ValidateArgument.required(batchRequest.getUserIds(), "BatchAccessApprovalRequest.userIds");
		ValidateArgument.required(batchRequest.getAccessRequirementId(), "BatchAccessApprovalRequest.accessRequirementId");
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member may perform this action.");
		}
		BatchAccessApprovalResult batchResult = new BatchAccessApprovalResult();
		List<AccessApprovalResult> list = new LinkedList<AccessApprovalResult>();
		batchResult.setResults(list);

		if (batchRequest.getUserIds().isEmpty()) {
			return batchResult;
		}

		Set<String> hasApprovals = accessApprovalDAO.getApprovedUsers(batchRequest.getUserIds(), batchRequest.getAccessRequirementId());
		for (String userId : batchRequest.getUserIds()) {
			AccessApprovalResult result = new AccessApprovalResult();
			result.setAccessRequirementId(batchRequest.getAccessRequirementId());
			result.setUserId(userId);
			if (hasApprovals.contains(userId)) {
				result.setHasApproval(true);
			} else {
				result.setHasApproval(false);
			}
			list.add(result);
		}
		return batchResult;
	}
}
