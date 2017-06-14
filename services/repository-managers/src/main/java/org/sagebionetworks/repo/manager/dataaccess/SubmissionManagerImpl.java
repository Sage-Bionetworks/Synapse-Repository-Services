package org.sagebionetworks.repo.manager.dataaccess;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.repo.manager.AccessRequirementManagerImpl;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.BasicAccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.ManagedACTAccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmission;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPageRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class SubmissionManagerImpl implements SubmissionManager{

	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private AccessRequirementDAO accessRequirementDao;
	@Autowired
	private RequestManager requestManager;
	@Autowired
	private ResearchProjectDAO researchProjectDao;
	@Autowired
	private SubmissionDAO submissionDao;
	@Autowired
	private GroupMembersDAO groupMembersDao;
	@Autowired
	private VerificationDAO verificationDao;
	@Autowired
	private AccessApprovalDAO accessApprovalDao;
	@Autowired
	private SubscriptionDAO subscriptionDao;
	@Autowired
	private TransactionalMessenger transactionalMessenger;

	@WriteTransactionReadCommitted
	@Override
	public SubmissionStatus create(UserInfo userInfo, String requestId, String etag) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(requestId, "requestId");
		ValidateArgument.required(etag, "etag");
		RequestInterface request = requestManager.getRequestForSubmission(requestId);
		ValidateArgument.requirement(etag.equals(request.getEtag()), "Etag does not match.");

		Submission submissionToCreate = new Submission();
		submissionToCreate.setRequestId(request.getId());
		submissionToCreate.setResearchProjectSnapshot(researchProjectDao.get(request.getResearchProjectId()));

		validateRequestBasedOnRequirements(userInfo, request, submissionToCreate);
		prepareCreationFields(userInfo, submissionToCreate);
		SubmissionStatus status = submissionDao.createSubmission(submissionToCreate);
		subscriptionDao.create(userInfo.getId().toString(), status.getSubmissionId(), SubscriptionObjectType.DATA_ACCESS_SUBMISSION_STATUS);
		transactionalMessenger.sendMessageAfterCommit(status.getSubmissionId(),
				ObjectType.DATA_ACCESS_SUBMISSION, UUID.randomUUID().toString(), ChangeType.CREATE, userInfo.getId());
		return status;
	}

	/**
	 * Validates the given request based on the existing requirements.
	 * If the request is valid, populates the fields for submissionToCreate.
	 * 
	 * @param userInfo
	 * @param request
	 * @param submissionToCreate
	 */
	public void validateRequestBasedOnRequirements(UserInfo userInfo, RequestInterface request,
			Submission submissionToCreate) {

		ValidateArgument.required(request.getAccessRequirementId(), "accessRequirementId");
		submissionToCreate.setAccessRequirementId(request.getAccessRequirementId());
		ValidateArgument.requirement(!submissionDao.hasSubmissionWithState(
				userInfo.getId().toString(), request.getAccessRequirementId(), SubmissionState.SUBMITTED),
				"A submission has been created. It has to be reviewed or cancelled before another submission can be created.");

		AccessRequirement ar = accessRequirementDao.get(request.getAccessRequirementId());
		ValidateArgument.requirement(ar instanceof ManagedACTAccessRequirement,
				"A Submission can only be created for an ManagedACTAccessRequirement.");
		submissionToCreate.setAccessRequirementVersion(ar.getVersionNumber());

		// validate based on the access requirement
		ManagedACTAccessRequirement actAR = (ManagedACTAccessRequirement) ar;
		if (actAR.getIsDUCRequired()) {
			ValidateArgument.requirement(request.getDucFileHandleId()!= null,
					"You must provide a Data Use Certification document.");
			submissionToCreate.setDucFileHandleId(request.getDucFileHandleId());
		}
		if (actAR.getIsIRBApprovalRequired()) {
			ValidateArgument.requirement(request.getIrbFileHandleId()!= null,
					"You must provide an Institutional Review Board approval document.");
			submissionToCreate.setIrbFileHandleId(request.getIrbFileHandleId());
		}
		if (actAR.getAreOtherAttachmentsRequired()) {
			ValidateArgument.requirement(request.getAttachments()!= null && !request.getAttachments().isEmpty(),
					"You must provide the required attachment(s).");
			submissionToCreate.setAttachments(request.getAttachments());
		}
		ValidateArgument.requirement(request.getAccessorChanges() != null && !request.getAccessorChanges().isEmpty(),
				"Must provide at least one accessor.");

		Set<String> accessorsWillHaveAccess = new HashSet<String>();
		Set<String> accessorsAlreadyHaveAccess = new HashSet<String>();
		for (AccessorChange ac : request.getAccessorChanges()) {
			switch (ac.getType()) {
				case GAIN_ACCESS:
					accessorsWillHaveAccess.add(ac.getUserId());
					break;
				case RENEW_ACCESS:
					accessorsWillHaveAccess.add(ac.getUserId());
					accessorsAlreadyHaveAccess.add(ac.getUserId());
					break;
				case REVOKE_ACCESS:
					accessorsAlreadyHaveAccess.add(ac.getUserId());
					break;
			}
		}

		if (actAR.getIsCertifiedUserRequired()) {
			ValidateArgument.requirement(groupMembersDao.areMemberOf(
					AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(),
					accessorsWillHaveAccess),
					"Accessors must be Synapse Certified Users.");
		}
		if (actAR.getIsValidatedProfileRequired()) {
			ValidateArgument.requirement(verificationDao.haveValidatedProfiles(accessorsWillHaveAccess),
					"Accessors must have validated profiles.");
		}

		if (!accessorsAlreadyHaveAccess.isEmpty()) {
			ValidateArgument.requirement(accessApprovalDao.hasApprovalsSubmittedBy(
					accessorsAlreadyHaveAccess, userInfo.getId().toString(), request.getAccessRequirementId()),
					"Cannot revoke / renew access for user who ");
		}

		ValidateArgument.requirement(accessorsWillHaveAccess.contains(userInfo.getId().toString()),
				"Submitter has to be an accessor.");
		submissionToCreate.setAccessorChanges(request.getAccessorChanges());

		if (request instanceof Renewal) {
			submissionToCreate.setIsRenewalSubmission(true);
			Renewal renewalRequest = (Renewal) request;
			submissionToCreate.setPublication(renewalRequest.getPublication());
			submissionToCreate.setSummaryOfUse(renewalRequest.getSummaryOfUse());
		} else {
			submissionToCreate.setIsRenewalSubmission(false);
		}
	}

	/**
	 * @param userInfo
	 * @param submissionToCreate
	 */
	public void prepareCreationFields(UserInfo userInfo, Submission submissionToCreate) {
		submissionToCreate.setSubmittedBy(userInfo.getId().toString());
		submissionToCreate.setSubmittedOn(new Date());
		submissionToCreate.setModifiedBy(userInfo.getId().toString());
		submissionToCreate.setModifiedOn(new Date());
		submissionToCreate.setState(SubmissionState.SUBMITTED);
	}

	@WriteTransactionReadCommitted
	@Override
	public SubmissionStatus cancel(UserInfo userInfo, String submissionId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(submissionId, "submissionId");
		Submission submission = submissionDao.getForUpdate(submissionId);
		if (!submission.getSubmittedBy().equals(userInfo.getId().toString())) {
			throw new UnauthorizedException("Can only cancel submission you submitted.");
		}
		ValidateArgument.requirement(submission.getState().equals(SubmissionState.SUBMITTED),
						"Cannot cancel a submission with "+submission.getState()+" state.");
		return submissionDao.cancel(submissionId, userInfo.getId().toString(),
				System.currentTimeMillis(), UUID.randomUUID().toString());
	}

	@WriteTransactionReadCommitted
	@Override
	public Submission updateStatus(UserInfo userInfo, SubmissionStateChangeRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getSubmissionId(), "submissionId");
		ValidateArgument.required(request.getNewState(), "newState");
		ValidateArgument.requirement(request.getNewState().equals(SubmissionState.APPROVED)
				|| request.getNewState().equals(SubmissionState.REJECTED),
				"Do not support changing to state: "+request.getNewState());
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can perform this action.");
		}
		Submission submission = submissionDao.getForUpdate(request.getSubmissionId());
		ValidateArgument.requirement(submission.getState().equals(SubmissionState.SUBMITTED),
						"Cannot change state of a submission with "+submission.getState()+" state.");
		if (request.getNewState().equals(SubmissionState.APPROVED)) {
			ManagedACTAccessRequirement ar = (ManagedACTAccessRequirement)accessRequirementDao.get(submission.getAccessRequirementId());
			Date expiredOn = calculateExpiredOn(ar.getExpirationPeriod());
			List<AccessApproval> approvalsToCreate = createApprovalForSubmission(submission, userInfo.getId().toString(), expiredOn);
			accessApprovalDao.createOrUpdateBatch(approvalsToCreate);
			/*
			 * See PLFM-4442.
			 */
			requestManager.updateApprovedRequest(submission.getRequestId());
		}
		submission = submissionDao.updateSubmissionStatus(request.getSubmissionId(),
				request.getNewState(), request.getRejectedReason(), userInfo.getId().toString(),
				System.currentTimeMillis());
		transactionalMessenger.sendMessageAfterCommit(submission.getId(), ObjectType.DATA_ACCESS_SUBMISSION_STATUS, submission.getEtag(), ChangeType.UPDATE, userInfo.getId());
		return submission;
	}

	public static Date calculateExpiredOn(Long expirationPeriod) {
		ValidateArgument.required(expirationPeriod, "expirationPeriod");
		if (expirationPeriod.equals(AccessRequirementManagerImpl.DEFAULT_EXPIRATION_PERIOD)) {
			return null;
		}
		return new Date(System.currentTimeMillis() + expirationPeriod);
	}

	public List<AccessApproval> createApprovalForSubmission(Submission submission, String createdBy, Date expiredOn) {
		Date createdOn = new Date();
		Long requirementId = Long.parseLong(submission.getAccessRequirementId());
		List<AccessApproval> approvals = new LinkedList<AccessApproval>();
		for (AccessorChange ac : submission.getAccessorChanges()) {
			AccessApproval approval = new AccessApproval();
			approval.setAccessorId(ac.getUserId());
			approval.setCreatedBy(createdBy);
			approval.setCreatedOn(createdOn);
			approval.setModifiedBy(createdBy);
			approval.setModifiedOn(createdOn);
			approval.setRequirementId(requirementId);
			approval.setRequirementVersion(submission.getAccessRequirementVersion());
			approval.setSubmitterId(submission.getSubmittedBy());
			approval.setExpiredOn(expiredOn);
			switch (ac.getType()) {
				case GAIN_ACCESS:
				case RENEW_ACCESS:
					approval.setState(ApprovalState.APPROVED);
					break;
				case REVOKE_ACCESS:
					approval.setState(ApprovalState.REVOKED);
					break;
			}
			approvals.add(approval);
		}
		return approvals;
	}

	@Override
	public SubmissionPage listSubmission(UserInfo userInfo, SubmissionPageRequest request){
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getAccessRequirementId(), "accessRequirementId");
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can perform this action.");
		}
		NextPageToken token = new NextPageToken(request.getNextPageToken());
		List<Submission> submissions = submissionDao.getSubmissions(
				request.getAccessRequirementId(), request.getFilterBy(), request.getOrderBy(),
				request.getIsAscending(), token.getLimitForQuery(), token.getOffset());
		SubmissionPage pageResult = new SubmissionPage();
		pageResult.setResults(submissions);
		pageResult.setNextPageToken(token.getNextPageTokenForCurrentResults(submissions));
		return pageResult;
	}

	@Override
	public AccessRequirementStatus getAccessRequirementStatus(UserInfo userInfo, String accessRequirementId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		String concreteType = accessRequirementDao.getConcreteType(accessRequirementId);
		List<AccessApproval> approvals = accessApprovalDao.getActiveApprovalsForUser(
				accessRequirementId, userInfo.getId().toString());

		boolean isApproved = !approvals.isEmpty();
		Date expiredOn = null;
		if (isApproved) {
			expiredOn = getLatestExpirationDate(approvals);
		}

		if (concreteType.equals(TermsOfUseAccessRequirement.class.getName())
				|| concreteType.equals(ACTAccessRequirement.class.getName())) {
			BasicAccessRequirementStatus status = new BasicAccessRequirementStatus();
			setApprovalStatus(accessRequirementId, isApproved, expiredOn, status);
			return status;
		} else if (concreteType.equals(ManagedACTAccessRequirement.class.getName())) {
			ManagedACTAccessRequirementStatus status = new ManagedACTAccessRequirementStatus();
			SubmissionStatus currentSubmissionStatus = submissionDao.getStatusByRequirementIdAndPrincipalId(
					accessRequirementId, userInfo.getId().toString());
			setApprovalStatus(accessRequirementId, isApproved, expiredOn, status);
			status.setCurrentSubmissionStatus(currentSubmissionStatus);
			return status;
		} else {
			throw new IllegalArgumentException("Not support AccessRequirement with type: "+concreteType);
		}
	}

	/**
	 * @param accessRequirementId
	 * @param isApproved
	 * @param expiredOn
	 * @param status
	 */
	public void setApprovalStatus(String accessRequirementId, boolean isApproved, Date expiredOn,
			AccessRequirementStatus status) {
		status.setAccessRequirementId(accessRequirementId);
		status.setIsApproved(isApproved);
		status.setExpiredOn(expiredOn);
	}

	/**
	 * @param approvals
	 * @param expiredOn
	 * @return
	 */
	public static Date getLatestExpirationDate(List<AccessApproval> approvals) {
		ValidateArgument.required(approvals, "approvals");
		Date expiredOn = null;
		for (AccessApproval approval : approvals) {
			if (approval.getExpiredOn() != null
					&& (expiredOn == null || approval.getExpiredOn().after(expiredOn))) {
					expiredOn = approval.getExpiredOn();
			}
		}
		return expiredOn;
	}

	@Override
	public OpenSubmissionPage getOpenSubmissions(UserInfo userInfo, String nextPageToken) {
		ValidateArgument.required(userInfo, "userInfo");
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can perform this action.");
		}
		NextPageToken token = new NextPageToken(nextPageToken);
		OpenSubmissionPage result = new OpenSubmissionPage();
		List<OpenSubmission> openSubmissionList = submissionDao.getOpenSubmissions(token.getLimitForQuery(), token.getOffset());
		result.setOpenSubmissionList(openSubmissionList);
		result.setNextPageToken(token.getNextPageTokenForCurrentResults(openSubmissionList));
		return result;
	}
}
