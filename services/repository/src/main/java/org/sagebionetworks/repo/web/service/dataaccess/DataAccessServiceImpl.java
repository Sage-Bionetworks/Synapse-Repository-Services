package org.sagebionetworks.repo.web.service.dataaccess;

import org.sagebionetworks.repo.manager.AccessRequirementManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.DataAccessRequestManager;
import org.sagebionetworks.repo.manager.dataaccess.DataAccessSubmissionManager;
import org.sagebionetworks.repo.manager.dataaccess.ResearchProjectManager;
import org.sagebionetworks.repo.model.RestrictionInformation;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequestInterface;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionPageRequest;
import org.sagebionetworks.repo.model.dataaccess.ACTAccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;
import org.springframework.beans.factory.annotation.Autowired;

public class DataAccessServiceImpl implements DataAccessService{

	@Autowired
	private UserManager userManager;
	@Autowired
	private ResearchProjectManager researchProjectManager;
	@Autowired
	private DataAccessRequestManager dataAccessRequestManager;
	@Autowired
	private DataAccessSubmissionManager dataAccessSubmissionManager;
	@Autowired
	private AccessRequirementManager accessRequirementManager;

	@Override
	public ResearchProject createOrUpdate(Long userId, ResearchProject toCreateOrUpdate) {
		UserInfo user = userManager.getUserInfo(userId);
		return researchProjectManager.createOrUpdate(user, toCreateOrUpdate);
	}

	@Override
	public ResearchProject getUserOwnResearchProjectForUpdate(Long userId, String accessRequirementId) {
		UserInfo user = userManager.getUserInfo(userId);
		return researchProjectManager.getUserOwnResearchProjectForUpdate(user, accessRequirementId);
	}

	@Override
	public DataAccessRequestInterface createOrUpdate(Long userId, DataAccessRequestInterface toCreateOrUpdate) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessRequestManager.createOrUpdate(user, toCreateOrUpdate);
	}

	@Override
	public DataAccessRequestInterface getRequestForUpdate(Long userId, String requirementId) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessRequestManager.getDataAccessRequestForUpdate(user, requirementId);
	}

	@Override
	public ACTAccessRequirementStatus submit(Long userId, String requestId, String etag) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessSubmissionManager.create(user, requestId, etag);
	}

	@Override
	public ACTAccessRequirementStatus cancel(Long userId, String submissionId) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessSubmissionManager.cancel(user, submissionId);
	}

	@Override
	public DataAccessSubmission updateState(Long userId, SubmissionStateChangeRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessSubmissionManager.updateStatus(user, request);
	}

	@Override
	public DataAccessSubmissionPage listSubmissions(Long userId, DataAccessSubmissionPageRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessSubmissionManager.listSubmission(user, request);
	}

	@Override
	public AccessRequirementStatus getAccessRequirementStatus(Long userId, String requirementId) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessSubmissionManager.getAccessRequirementStatus(user, requirementId);
	}

	@Override
	public RestrictionInformation getRestrictionInformation(Long userId, String entityId) {
		UserInfo user = userManager.getUserInfo(userId);
		return accessRequirementManager.getRestrictionInformation(user, entityId);
	}
}
