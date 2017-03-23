package org.sagebionetworks.repo.web.service.dataaccess;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.DataAccessRequestManager;
import org.sagebionetworks.repo.manager.dataaccess.DataAccessSubmissionManager;
import org.sagebionetworks.repo.manager.dataaccess.ResearchProjectManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalStatusRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalStatusResults;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequestInterface;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionOrder;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionStatus;
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
	public DataAccessSubmissionStatus submit(Long userId, String requestId, String etag) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessSubmissionManager.create(user, requestId, etag);
	}

	@Override
	public DataAccessSubmissionStatus getStatus(Long userId, String requirementId) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessSubmissionManager.getSubmissionStatus(user, requirementId);
	}

	@Override
	public DataAccessSubmissionStatus cancel(Long userId, String submissionId) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessSubmissionManager.cancel(user, submissionId);
	}

	@Override
	public DataAccessSubmission updateState(Long userId, SubmissionStateChangeRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessSubmissionManager.updateStatus(user, request);
	}

	@Override
	public DataAccessSubmissionPage listSubmissions(Long userId, String requirementId, String nextPageToken,
			DataAccessSubmissionState filterBy, DataAccessSubmissionOrder orderBy, Boolean isAscending) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessSubmissionManager.listSubmission(user, requirementId, nextPageToken, filterBy, orderBy, isAscending);
	}

	@Override
	public AccessApprovalStatusResults getAccessApprovalStatus(Long userId, AccessApprovalStatusRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessSubmissionManager.getAccessApprovalStatus(user, request);
	}

}
