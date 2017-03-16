package org.sagebionetworks.repo.manager.dataaccess;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
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
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRenewal;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequestInterface;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionStatus;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessRequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessSubmissionDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectDAO;
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

	@Override
	public DataAccessSubmissionStatus create(UserInfo userInfo, String requestId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(requestId, "requestId");
		DataAccessRequestInterface request = dataAccessRequestDao.get(requestId);

		DataAccessSubmission submissionToCreate = new DataAccessSubmission();

		ValidateArgument.required(request.getId(), "request's ID");
		submissionToCreate.setDataAccessRequestId(request.getId());

		ValidateArgument.required(request.getResearchProjectId(), "researchProjectId");
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
		submissionToCreate.setState(DataAccessSubmissionState.SUBMITTED);
	}

	@Override
	public DataAccessSubmissionStatus getSubmissionStatus(UserInfo userInfo, String accessRequirementId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataAccessSubmissionStatus cancel(UserInfo userInfo, String submissionId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataAccessSubmission updateStatus(UserInfo userInfo, SubmissionStateChangeRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

}
