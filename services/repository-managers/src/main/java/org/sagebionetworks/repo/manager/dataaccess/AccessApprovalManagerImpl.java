package org.sagebionetworks.repo.manager.dataaccess;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.UserCertificationRequiredException;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessApprovalInfo;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoRequest;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoResponse;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.HasAccessorRequirement;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PostMessageContentAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.SelfSignAccessRequirementInterface;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalSearchRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalSearchResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalSearchResult;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalSearchSort;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalSortField;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroup;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRevokeRequest;
import org.sagebionetworks.repo.model.dbo.verification.VerificationDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.MessageToSend;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

@Service
public class AccessApprovalManagerImpl implements AccessApprovalManager {
	public static final Long DEFAULT_LIMIT = 50L;
	public static final Long MAX_LIMIT = 50L;
	public static final Long DEFAULT_OFFSET = 0L;
	
	private final AccessRequirementDAO accessRequirementDAO;
	private final AccessApprovalDAO accessApprovalDAO;
	private final VerificationDAO verificationDao;
	private final GroupMembersDAO groupMembersDao;
	private final TransactionalMessenger transactionalMessenger;
	private final NodeDAO nodeDao;
	
	@Autowired
	public AccessApprovalManagerImpl(AccessRequirementDAO accessRequirementDAO, AccessApprovalDAO accessApprovalDAO,
			VerificationDAO verificationDao, GroupMembersDAO groupMembersDao,
			TransactionalMessenger transactionalMessenger, NodeDAO nodeDao) {
		super();
		this.accessRequirementDAO = accessRequirementDAO;
		this.accessApprovalDAO = accessApprovalDAO;
		this.verificationDao = verificationDao;
		this.groupMembersDao = groupMembersDao;
		this.transactionalMessenger = transactionalMessenger;
		this.nodeDao = nodeDao;
	}

	public static void populateCreationFields(UserInfo userInfo, AccessApproval a) {
		Date now = new Date();
		a.setState(ApprovalState.APPROVED);
		a.setCreatedBy(userInfo.getId().toString());
		a.setCreatedOn(now);
		populateModifiedFields(userInfo, a);
	}

	public static void populateModifiedFields(UserInfo userInfo, AccessApproval a) {
		Date now = new Date();
		a.setModifiedBy(userInfo.getId().toString());
		a.setModifiedOn(now);
	}

	@Override
	public AccessApproval getAccessApproval(UserInfo userInfo, String approvalId) 
			throws DatastoreException, NotFoundException {
		return accessApprovalDAO.get(approvalId);
	}

	@WriteTransaction
	@Override
	public AccessApproval createAccessApproval(UserInfo userInfo, AccessApproval accessApproval) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessApproval, "accessApproval");
		ValidateArgument.required(accessApproval.getRequirementId(), "accessRequirementId");
		AccessRequirement ar = accessRequirementDAO.get(accessApproval.getRequirementId().toString());

		ValidateArgument.requirement(!(ar instanceof LockAccessRequirement)
				&& !(ar instanceof PostMessageContentAccessRequirement), "Cannot apply an approval to a "+ar.getConcreteType());
		if (ar instanceof SelfSignAccessRequirementInterface) {
			accessApproval.setAccessorId(userInfo.getId().toString());
		} else if (!AuthorizationUtils.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("User is not an ACT Member.");
		}

		ValidateArgument.required(accessApproval.getAccessorId(), "accessorId");
		ValidateArgument.requirement(
				!BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString().equals(accessApproval.getAccessorId()),
				"Cannot create an AccessApproval for anonymous user.");
		if (ar instanceof HasAccessorRequirement) {
			validateHasAccessorRequirement((HasAccessorRequirement) ar,
					Sets.newHashSet(accessApproval.getAccessorId()));
		}
		if (accessApproval.getRequirementVersion() == null) {
			accessApproval.setRequirementVersion(ar.getVersionNumber());
		}
		if (accessApproval.getSubmitterId() == null) {
			accessApproval.setSubmitterId(accessApproval.getAccessorId());
		}
		populateCreationFields(userInfo, accessApproval);
		return accessApprovalDAO.create(accessApproval);
	}

	@WriteTransaction
	@Override
	public void revokeAccessApprovals(UserInfo userInfo, String accessRequirementId, String accessorId)
			throws UnauthorizedException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		ValidateArgument.required(accessorId, "accessorId");
		if (!userInfo.getId().toString().equals(accessorId) && !AuthorizationUtils.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member may delete access approvals of other users.");
		}
		AccessRequirement accessRequirement = accessRequirementDAO.get(accessRequirementId);
		
		ValidateArgument.requirement(accessRequirement.getConcreteType().equals(ACTAccessRequirement.class.getName()),
				"Do not support access approval deletion for access requirement type: "+accessRequirement.getConcreteType());
		
		final List<Long> approvals = accessApprovalDAO.listApprovalsByAccessor(accessRequirementId, accessorId);
		
		if (approvals.isEmpty()) {
			return;
		}
		
		final List<Long> revokedApprovals = accessApprovalDAO.revokeBatch(userInfo.getId(), approvals);
		
		sendUpdateChange(userInfo, revokedApprovals);
	}

	@Override
	public AccessorGroupResponse listAccessorGroup(UserInfo userInfo, AccessorGroupRequest request){
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		if (!AuthorizationUtils.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can perform this action.");
		}
		NextPageToken nextPageToken = new NextPageToken(request.getNextPageToken());
		List<AccessorGroup> groups = accessApprovalDAO.listAccessorGroup(
				request.getAccessRequirementId(), request.getSubmitterId(), request.getAccessorId(),
				request.getExpireBefore(), nextPageToken.getLimitForQuery(),
				nextPageToken.getOffset());
		AccessorGroupResponse response = new AccessorGroupResponse();
		response.setResults(groups);
		response.setNextPageToken(nextPageToken.getNextPageTokenForCurrentResults(groups));
		return response;
	}

	@WriteTransaction
	@Override
	public void revokeGroup(UserInfo userInfo, AccessorGroupRevokeRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getAccessRequirementId(), "requirementId");
		ValidateArgument.required(request.getSubmitterId(), "submitterId");
		
		if (!AuthorizationUtils.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can perform this action.");
		}
		
		final List<Long> approvals = accessApprovalDAO.listApprovalsBySubmitter(request.getAccessRequirementId(), request.getSubmitterId());
		
		if (approvals.isEmpty()) {
			return;
		}
		
		final List<Long> revokedApprovals = accessApprovalDAO.revokeBatch(userInfo.getId(), approvals);
		
		sendUpdateChange(userInfo, revokedApprovals);
	}

	@Override
	public BatchAccessApprovalInfoResponse getAccessApprovalInfo(UserInfo userInfo, BatchAccessApprovalInfoRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getUserId(), "BatchAccessApprovalInfoRequest.userId");
		ValidateArgument.required(request.getAccessRequirementIds(), "BatchAccessApprovalInfoRequest.accessRequirementIds");
		
		BatchAccessApprovalInfoResponse response = new BatchAccessApprovalInfoResponse();
		List<AccessApprovalInfo> results = new LinkedList<AccessApprovalInfo>();
		response.setResults(results);
		
		if (!request.getAccessRequirementIds().isEmpty()) {
			Set<String> requirementsUserHasApproval = accessApprovalDAO.getRequirementsUserHasApprovals(request.getUserId(), request.getAccessRequirementIds());
			for (String requirementId : request.getAccessRequirementIds()) {
				AccessApprovalInfo info = new AccessApprovalInfo();
				info.setUserId(request.getUserId());
				info.setAccessRequirementId(requirementId);
				info.setHasAccessApproval(requirementsUserHasApproval.contains(requirementId));
				results.add(info);
			}
		}
		return response;
	}
	
	@Override
	@WriteTransaction
	public void revokeGroup(UserInfo userInfo, String accessRequirementId, String submitterId, List<String> accessorIds) {
		ValidateArgument.required(userInfo, "The user");
		ValidateArgument.required(accessRequirementId, "The access requirement id");
		ValidateArgument.required(submitterId, "The submitter id");
		ValidateArgument.required(accessorIds, "The list of accessor ids");
		
		if (!AuthorizationUtils.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can perform this action.");
		}
		
		if (accessorIds.isEmpty()) {
			return;
		}
		
		final List<Long> approvals = accessApprovalDAO.listApprovalsBySubmitter(accessRequirementId, submitterId, accessorIds);
		
		if (approvals.isEmpty()) {
			return;
		}
		
		final List<Long> revokedApprovals = accessApprovalDAO.revokeBatch(userInfo.getId(), approvals);
		
		sendUpdateChange(userInfo, revokedApprovals);
	};
	
	@Override
	@WriteTransaction
	public int revokeExpiredApprovals(UserInfo userInfo, Instant expiredAfter, int maxBatchSize) {
		ValidateArgument.required(userInfo, "The user");
		ValidateArgument.required(expiredAfter, "The expiredAfter");
		ValidateArgument.requirement(maxBatchSize > 0, "The maxBatchSize must be greater than 0.");
		ValidateArgument.requirement(expiredAfter.isBefore(Instant.now()), "The expiredAfter must be a value in the past.");
		
		if (!AuthorizationUtils.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can perform this action.");
		}
	
		// Fetch the list of expired approval
		final List<Long> expiredApprovals = accessApprovalDAO.listExpiredApprovals(expiredAfter, maxBatchSize);
		
		if (expiredApprovals.isEmpty()) {
			return 0;
		}
		
		// Batch revoke the approvals
		final List<Long> revokedApprovals = accessApprovalDAO.revokeBatch(userInfo.getId(), expiredApprovals);
		
		// For each revocation send out a change message		
		sendUpdateChange(userInfo, revokedApprovals);
		
		return revokedApprovals.size();
		
	}
	
	@Override
	public void validateHasAccessorRequirement(HasAccessorRequirement req, Set<String> accessors) {
		if (req.getIsCertifiedUserRequired()) {
			if(!groupMembersDao.areMemberOf(
					AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(),
					accessors)){
				throw new UserCertificationRequiredException("Accessors must be Synapse Certified Users.");
			}
		}
		if (req.getIsValidatedProfileRequired()) {
			ValidateArgument.requirement(verificationDao.haveValidatedProfiles(accessors),
					"Accessors must have validated profiles.");
		}
	}
	
	/**
	 * Checks whether the parent (or other ancestors) are subject to access restrictions and, if so, whether 
	 * userInfo is a member of the ACT.
	 * 
	 * @param userInfo
	 * @param sourceParentId
	 * @param destParentId
	 * @return
	 */
	@Override
	public AuthorizationStatus canUserMoveRestrictedEntity(UserInfo userInfo, String sourceParentId, String destParentId) throws NotFoundException {
		if (AuthorizationUtils.isACTTeamMemberOrAdmin(userInfo)) {
			return AuthorizationStatus.authorized();
		}
		if (sourceParentId.equals(destParentId)) {
			return AuthorizationStatus.authorized();
		}
		List<Long> sourceParentAncestorIds = nodeDao.getEntityPathIds(sourceParentId);
		List<Long> destParentAncestorIds = nodeDao.getEntityPathIds(destParentId);

		List<String> missingRequirements = accessRequirementDAO.getAccessRequirementDiff(sourceParentAncestorIds, destParentAncestorIds, RestrictableObjectType.ENTITY);
		if (missingRequirements.isEmpty()) { // only OK if destParent has all the requirements that source parent has
			return AuthorizationStatus.authorized();
		} else {
			return AuthorizationStatus.accessDenied("Cannot move restricted entity to a location having fewer access restrictions.");
		}
	}
	
	@Override
	public AccessApprovalSearchResponse searchAccessApprovals(UserInfo userInfo, AccessApprovalSearchRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		
		if (!AuthorizationUtils.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can perform this action.");
		}
		
		NextPageToken pageToken = new NextPageToken(request.getNextPageToken());
		
		long limit = pageToken.getLimitForQuery();
		long offset = pageToken.getOffset();
		
		List<AccessApprovalSearchSort> sort = request.getSort() == null || request.getSort().isEmpty() ? 
				Arrays.asList(new AccessApprovalSearchSort().setField(AccessApprovalSortField.MODIFIED_ON)) : request.getSort();
		
		List<AccessApproval> results = accessApprovalDAO.searchAccessApprovals(request.getAccessorId(), request.getAccessRequirementId(), sort, limit, offset);
		
		String nextPageToken = pageToken.getNextPageTokenForCurrentResults(results);
		
		Map<Long, String> namesMap = accessRequirementDAO.getAccessRequirementNames(
			results.stream().map(ap -> ap.getRequirementId()).collect(Collectors.toSet())
		);
		
		List<AccessApprovalSearchResult> mappedResults = results.stream().map(ap -> {
			return new AccessApprovalSearchResult()
				.setId(ap.getId().toString())
				.setAccessRequirementId(ap.getRequirementId().toString())
				.setAccessRequirementVersion(ap.getRequirementVersion().toString())
				.setAccessRequirementName(namesMap.get(ap.getRequirementId()))
				.setModifiedOn(ap.getModifiedOn())
				.setExpiredOn(ap.getExpiredOn())
				.setReviewerId(ap.getModifiedBy())
				.setState(ap.getState())
				.setSubmitterId(ap.getSubmitterId());
		}).collect(Collectors.toList());
		
		AccessApprovalSearchResponse response = new AccessApprovalSearchResponse()
			.setResults(mappedResults)
			.setNextPageToken(nextPageToken);
		
		return response;
	}
	
	private void sendUpdateChange(UserInfo user, List<Long> accessApprovalIds) {
		accessApprovalIds.forEach( id -> {
			MessageToSend message = new MessageToSend()
					.withUserId(user.getId())
					.withObjectType(ObjectType.ACCESS_APPROVAL)
					.withObjectId(id.toString())
					.withChangeType(ChangeType.UPDATE);
			
			transactionalMessenger.sendMessageAfterCommit(message);
		});
	}
}
