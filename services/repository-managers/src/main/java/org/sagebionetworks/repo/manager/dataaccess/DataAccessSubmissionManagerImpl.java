package org.sagebionetworks.repo.manager.dataaccess;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRenewal;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequestInterface;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionOrder;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionStatus;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessRequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessSubmissionDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectDAO;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class DataAccessSubmissionManagerImpl implements DataAccessSubmissionManager{

	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private IdGenerator idGenerator;
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

	@WriteTransactionReadCommitted
	@Override
	public DataAccessSubmissionStatus create(UserInfo userInfo, String requestId, String etag) {
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
		return dataAccessSubmissionDao.create(submissionToCreate);
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
		submissionToCreate.setId(idGenerator.generateNewId(TYPE.DATA_ACCESS_SUBMISSION_ID).toString());
		submissionToCreate.setEtag(UUID.randomUUID().toString());
		submissionToCreate.setSubmittedBy(userInfo.getId().toString());
		submissionToCreate.setSubmittedOn(new Date());
		submissionToCreate.setModifiedBy(userInfo.getId().toString());
		submissionToCreate.setModifiedOn(new Date());
		submissionToCreate.setState(DataAccessSubmissionState.SUBMITTED);
	}

	@Override
	public DataAccessSubmissionStatus getSubmissionStatus(UserInfo userInfo, String accessRequirementId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		return dataAccessSubmissionDao.getStatus(accessRequirementId, userInfo.getId().toString());
	}

	@WriteTransactionReadCommitted
	@Override
	public DataAccessSubmissionStatus cancel(UserInfo userInfo, String submissionId) {
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
		return dataAccessSubmissionDao.updateStatus(request.getSubmissionId(),
				request.getNewState(), request.getRejectedReason(), userInfo.getId().toString(),
				System.currentTimeMillis(), UUID.randomUUID().toString());
	}

	@Override
	public DataAccessSubmissionPage listSubmission(UserInfo userInfo, String accessRequirementId,
			String nextPageToken, DataAccessSubmissionState filterBy, DataAccessSubmissionOrder orderBy, Boolean isAscending){
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can perform this action.");
		}
		NextPageToken token = new NextPageToken(nextPageToken);
		List<DataAccessSubmission> submissions = dataAccessSubmissionDao.getSubmissions(accessRequirementId, filterBy, orderBy, isAscending, token.getLimitForQuery(), token.getOffset());
		DataAccessSubmissionPage pageResult = new DataAccessSubmissionPage();
		pageResult.setResults(submissions);
		pageResult.setNextPageToken(token.getNextPageTokenForCurrentResults(submissions));
		return pageResult;
	}
}
