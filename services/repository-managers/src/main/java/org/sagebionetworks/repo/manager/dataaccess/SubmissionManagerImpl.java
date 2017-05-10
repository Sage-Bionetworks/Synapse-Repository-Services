package org.sagebionetworks.repo.manager.dataaccess;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACTAccessApproval;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dataaccess.ACTAccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPageRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmission;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;
import org.sagebionetworks.repo.model.dataaccess.TermsOfUseAccessRequirementStatus;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectDAO;
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
	private RequestDAO requestDao;
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
		RequestInterface request = requestDao.get(requestId);
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
		ValidateArgument.requirement(ar instanceof ACTAccessRequirement,
				"A Submission can only be created for an ACTAccessRequirement.");

		// validate based on the access requirement
		ACTAccessRequirement actAR = (ACTAccessRequirement) ar;
		ValidateArgument.requirement(actAR.getAcceptRequest() != null
				&& actAR.getAcceptRequest(),
				"This Access Requirement doesn't accept Data Access Request.");
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
		ValidateArgument.requirement(request.getAccessors() != null && !request.getAccessors().isEmpty(),
				"Must provide at least one accessor.");
		Set<String> accessors = new HashSet<String>();
		accessors.addAll(request.getAccessors());
		if (actAR.getIsCertifiedUserRequired()) {
			ValidateArgument.requirement(groupMembersDao.areMemberOf(
					AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(),
					accessors),
					"Accessors must be Synapse Certified Users.");
		}
		if (actAR.getIsValidatedProfileRequired()) {
			ValidateArgument.requirement(verificationDao.haveValidatedProfiles(accessors),
					"Accessors must have validated profiles.");
		}
		ValidateArgument.requirement(accessors.contains(userInfo.getId().toString()),
				"Submitter has to be an accessor.");
		submissionToCreate.setAccessors(new LinkedList<String>(accessors));

		if (!actAR.getIsAnnualReviewRequired() && request instanceof Renewal) {
			throw new IllegalArgumentException("The associated AccessRequirement does not require renewal.");
		}
		if (actAR.getIsAnnualReviewRequired() && request instanceof Renewal) {
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
			List<AccessApproval> approvalsToCreate = createApprovalForSubmission(submission, userInfo.getId().toString());
			accessApprovalDao.createBatch(approvalsToCreate);
		}
		submission = submissionDao.updateSubmissionStatus(request.getSubmissionId(),
				request.getNewState(), request.getRejectedReason(), userInfo.getId().toString(),
				System.currentTimeMillis());
		transactionalMessenger.sendMessageAfterCommit(submission.getId(), ObjectType.DATA_ACCESS_SUBMISSION_STATUS, submission.getEtag(), ChangeType.UPDATE, userInfo.getId());
		return submission;
	}

	public List<AccessApproval> createApprovalForSubmission(Submission submission, String createdBy) {
		Date createdOn = new Date();
		Long requirementId = Long.parseLong(submission.getAccessRequirementId());
		List<AccessApproval> approvals = new LinkedList<AccessApproval>();
		for (String accessor : submission.getAccessors()) {
			ACTAccessApproval approval = new ACTAccessApproval();
			approval.setAccessorId(accessor);
			approval.setCreatedBy(createdBy);
			approval.setCreatedOn(createdOn);
			approval.setModifiedBy(createdBy);
			approval.setModifiedOn(createdOn);
			approval.setRequirementId(requirementId);
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
		List<AccessApproval> approvals = accessApprovalDao.getForAccessRequirementsAndPrincipals(
				Arrays.asList(accessRequirementId), Arrays.asList(userInfo.getId().toString()));
		if (concreteType.equals(TermsOfUseAccessRequirement.class.getName())) {
			TermsOfUseAccessRequirementStatus status = new TermsOfUseAccessRequirementStatus();
			status.setAccessRequirementId(accessRequirementId);
			status.setIsApproved(!approvals.isEmpty());
			return status;
		} else if (concreteType.equals(ACTAccessRequirement.class.getName())) {
			ACTAccessRequirementStatus status = new ACTAccessRequirementStatus();
			SubmissionStatus currentSubmissionStatus = submissionDao.getStatusByRequirementIdAndPrincipalId(
					accessRequirementId, userInfo.getId().toString());
			status.setAccessRequirementId(accessRequirementId);
			status.setIsApproved(!approvals.isEmpty());
			status.setCurrentSubmissionStatus(currentSubmissionStatus);
			return status;
		} else {
			throw new IllegalArgumentException("Not support AccessRequirement with type: "+concreteType);
		}
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
