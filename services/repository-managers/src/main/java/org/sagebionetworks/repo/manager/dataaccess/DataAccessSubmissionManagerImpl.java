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
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dataaccess.ACTAccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRenewal;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequestInterface;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionPageRequest;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;
import org.sagebionetworks.repo.model.dataaccess.TermsOfUseAccessRequirementStatus;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessRequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessSubmissionDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class DataAccessSubmissionManagerImpl implements DataAccessSubmissionManager{

	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private AccessRequirementDAO accessRequirementDao;
	@Autowired
	private DataAccessRequestDAO dataAccessRequestDao;
	@Autowired
	private ResearchProjectDAO researchProjectDao;
	@Autowired
	private DataAccessSubmissionDAO dataAccessSubmissionDao;
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
	public ACTAccessRequirementStatus create(UserInfo userInfo, String requestId, String etag) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(requestId, "requestId");
		ValidateArgument.required(etag, "etag");
		DataAccessRequestInterface request = dataAccessRequestDao.get(requestId);
		ValidateArgument.requirement(etag.equals(request.getEtag()), "Etag does not match.");

		DataAccessSubmission submissionToCreate = new DataAccessSubmission();
		submissionToCreate.setDataAccessRequestId(request.getId());
		submissionToCreate.setResearchProjectSnapshot(researchProjectDao.get(request.getResearchProjectId()));

		validateRequestBasedOnRequirements(userInfo, request, submissionToCreate);
		prepareCreationFields(userInfo, submissionToCreate);
		ACTAccessRequirementStatus status = dataAccessSubmissionDao.createSubmission(submissionToCreate);
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
	public void validateRequestBasedOnRequirements(UserInfo userInfo, DataAccessRequestInterface request,
			DataAccessSubmission submissionToCreate) {

		ValidateArgument.required(request.getAccessRequirementId(), "accessRequirementId");
		submissionToCreate.setAccessRequirementId(request.getAccessRequirementId());
		ValidateArgument.requirement(!dataAccessSubmissionDao.hasSubmissionWithState(
				userInfo.getId().toString(), request.getAccessRequirementId(), DataAccessSubmissionState.SUBMITTED),
				"A submission has been created. It has to be reviewed or cancelled before another submission can be created.");

		AccessRequirement ar = accessRequirementDao.get(request.getAccessRequirementId());
		ValidateArgument.requirement(ar instanceof ACTAccessRequirement,
				"A DataAccessSubmission can only be created for an ACTAccessRequirement.");

		// validate based on the access requirement
		ACTAccessRequirement actAR = (ACTAccessRequirement) ar;
		if (actAR.getIsDUCRequired()) {
			ValidateArgument.requirement(request.getDucFileHandleId()!= null,
					"Must provide a fileHandleId of the Intended Data Use statement.");
			submissionToCreate.setDucFileHandleId(request.getDucFileHandleId());
		}
		if (actAR.getIsIRBApprovalRequired()) {
			ValidateArgument.requirement(request.getIrbFileHandleId()!= null,
					"Must provide a fileHandleId of the Institutional Review Board approval document.");
			submissionToCreate.setIrbFileHandleId(request.getIrbFileHandleId());
		}
		if (actAR.getAreOtherAttachmentsRequired()) {
			ValidateArgument.requirement(request.getAttachments()!= null && !request.getAttachments().isEmpty(),
					"Must provide a fileHandleId of the attachment.");
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

		if (!actAR.getIsAnnualReviewRequired() && request instanceof DataAccessRenewal) {
			throw new IllegalArgumentException("The associated AccessRequirement does not require renewal.");
		}
		if (actAR.getIsAnnualReviewRequired() && request instanceof DataAccessRenewal) {
			submissionToCreate.setIsRenewalSubmission(true);
			DataAccessRenewal renewalRequest = (DataAccessRenewal) request;
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
	public void prepareCreationFields(UserInfo userInfo, DataAccessSubmission submissionToCreate) {
		submissionToCreate.setSubmittedBy(userInfo.getId().toString());
		submissionToCreate.setSubmittedOn(new Date());
		submissionToCreate.setModifiedBy(userInfo.getId().toString());
		submissionToCreate.setModifiedOn(new Date());
		submissionToCreate.setState(DataAccessSubmissionState.SUBMITTED);
	}

	@WriteTransactionReadCommitted
	@Override
	public ACTAccessRequirementStatus cancel(UserInfo userInfo, String submissionId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(submissionId, "submissionId");
		DataAccessSubmission submission = dataAccessSubmissionDao.getForUpdate(submissionId);
		if (!submission.getSubmittedBy().equals(userInfo.getId().toString())) {
			throw new UnauthorizedException("Can only cancel submission you submitted.");
		}
		ValidateArgument.requirement(submission.getState().equals(DataAccessSubmissionState.SUBMITTED),
						"Cannot cancel a submission with "+submission.getState()+" state.");
		return dataAccessSubmissionDao.cancel(submissionId, userInfo.getId().toString(),
				System.currentTimeMillis(), UUID.randomUUID().toString());
	}

	@WriteTransactionReadCommitted
	@Override
	public DataAccessSubmission updateStatus(UserInfo userInfo, SubmissionStateChangeRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getSubmissionId(), "submissionId");
		ValidateArgument.required(request.getNewState(), "newState");
		ValidateArgument.requirement(request.getNewState().equals(DataAccessSubmissionState.APPROVED)
				|| request.getNewState().equals(DataAccessSubmissionState.REJECTED),
				"Do not support changing to state: "+request.getNewState());
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can perform this action.");
		}
		DataAccessSubmission submission = dataAccessSubmissionDao.getForUpdate(request.getSubmissionId());
		ValidateArgument.requirement(submission.getState().equals(DataAccessSubmissionState.SUBMITTED),
						"Cannot change state of a submission with "+submission.getState()+" state.");
		if (request.getNewState().equals(DataAccessSubmissionState.APPROVED)) {
			List<AccessApproval> approvalsToCreate = createApprovalForSubmission(submission, userInfo.getId().toString());
			accessApprovalDao.createBatch(approvalsToCreate);
		}
		submission = dataAccessSubmissionDao.updateSubmissionStatus(request.getSubmissionId(),
				request.getNewState(), request.getRejectedReason(), userInfo.getId().toString(),
				System.currentTimeMillis());
		transactionalMessenger.sendMessageAfterCommit(submission.getId(), ObjectType.DATA_ACCESS_SUBMISSION_STATUS, submission.getEtag(), ChangeType.UPDATE, userInfo.getId());
		return submission;
	}

	public List<AccessApproval> createApprovalForSubmission(DataAccessSubmission submission, String createdBy) {
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
	public DataAccessSubmissionPage listSubmission(UserInfo userInfo, DataAccessSubmissionPageRequest request){
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getAccessRequirementId(), "accessRequirementId");
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can perform this action.");
		}
		NextPageToken token = new NextPageToken(request.getNextPageToken());
		List<DataAccessSubmission> submissions = dataAccessSubmissionDao.getSubmissions(
				request.getAccessRequirementId(), request.getFilterBy(), request.getOrderBy(),
				request.getIsAscending(), token.getLimitForQuery(), token.getOffset());
		DataAccessSubmissionPage pageResult = new DataAccessSubmissionPage();
		pageResult.setResults(submissions);
		pageResult.setNextPageToken(token.getNextPageTokenForCurrentResults(submissions));
		return pageResult;
	}

	@Override
	public AccessRequirementStatus getAccessRequirementStatus(UserInfo userInfo, String accessRequirementId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		String concreteType = accessRequirementDao.getConcreteType(accessRequirementId);
		if (concreteType.equals(TermsOfUseAccessRequirement.class.getName())) {
			TermsOfUseAccessRequirementStatus status = new TermsOfUseAccessRequirementStatus();
			status.setAccessRequirementId(accessRequirementId);
			List<AccessApproval> approvals = accessApprovalDao.getForAccessRequirementsAndPrincipals(
					Arrays.asList(accessRequirementId), Arrays.asList(userInfo.getId().toString()));
			if (approvals.isEmpty()) {
				status.setIsApproved(false);
			} else if (approvals.size() == 1 
					&& approvals.get(0) instanceof TermsOfUseAccessApproval
					&& approvals.get(0).getAccessorId().equals(userInfo.getId().toString())
					&& approvals.get(0).getRequirementId().toString().equals(accessRequirementId)) {
				status.setIsApproved(true);
			} else {
				throw new IllegalStateException();
			}
			return status;
		} else if (concreteType.equals(ACTAccessRequirement.class.getName())) {
			return dataAccessSubmissionDao.getStatusByRequirementIdAndPrincipalId(
					accessRequirementId, userInfo.getId().toString());
		} else {
			throw new IllegalArgumentException("Not support AccessRequirement with type: "+concreteType);
		}
	}
}
