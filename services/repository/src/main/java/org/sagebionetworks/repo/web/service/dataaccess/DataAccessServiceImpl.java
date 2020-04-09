package org.sagebionetworks.repo.web.service.dataaccess;

import org.sagebionetworks.repo.manager.AccessRequirementManager;
import org.sagebionetworks.repo.manager.dataaccess.RequestManager;
import org.sagebionetworks.repo.manager.dataaccess.ResearchProjectManager;
import org.sagebionetworks.repo.manager.dataaccess.SubmissionManager;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.CreateSubmissionRequest;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPageRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
import org.springframework.beans.factory.annotation.Autowired;

public class DataAccessServiceImpl implements DataAccessService {

	@Autowired
	private ResearchProjectManager researchProjectManager;
	@Autowired
	private RequestManager dataAccessRequestManager;
	@Autowired
	private SubmissionManager dataAccessSubmissionManager;
	@Autowired
	private AccessRequirementManager accessRequirementManager;

	@Override
	public ResearchProject createOrUpdate(UserInfo userInfo, ResearchProject toCreateOrUpdate) {
		return researchProjectManager.createOrUpdate(userInfo, toCreateOrUpdate);
	}

	@Override
	public ResearchProject getUserOwnResearchProjectForUpdate(UserInfo userInfo, String accessRequirementId) {
		return researchProjectManager.getUserOwnResearchProjectForUpdate(userInfo, accessRequirementId);
	}

	@Override
	public RequestInterface createOrUpdate(UserInfo userInfo, RequestInterface toCreateOrUpdate) {
		return dataAccessRequestManager.createOrUpdate(userInfo, toCreateOrUpdate);
	}

	@Override
	public RequestInterface getRequestForUpdate(UserInfo userInfo, String requirementId) {
		return dataAccessRequestManager.getRequestForUpdate(userInfo, requirementId);
	}

	@Override
	public SubmissionStatus submit(UserInfo userInfo, CreateSubmissionRequest request) {
		return dataAccessSubmissionManager.create(userInfo, request);
	}

	@Override
	public SubmissionStatus cancel(UserInfo userInfo, String submissionId) {
		return dataAccessSubmissionManager.cancel(userInfo, submissionId);
	}

	@Override
	public Submission updateState(UserInfo userInfo, SubmissionStateChangeRequest request) {
		return dataAccessSubmissionManager.updateStatus(userInfo, request);
	}

	@Override
	public SubmissionPage listSubmissions(UserInfo userInfo, SubmissionPageRequest request) {
		return dataAccessSubmissionManager.listSubmission(userInfo, request);
	}

	@Override
	public AccessRequirementStatus getAccessRequirementStatus(UserInfo userInfo, String requirementId) {
		return dataAccessSubmissionManager.getAccessRequirementStatus(userInfo, requirementId);
	}

	@Override
	public RestrictionInformationResponse getRestrictionInformation(UserInfo userInfo, RestrictionInformationRequest request) {
		return accessRequirementManager.getRestrictionInformation(userInfo, request);
	}

	@Override
	public OpenSubmissionPage getOpenSubmissions(UserInfo userInfo, String nextPageToken) {
		return dataAccessSubmissionManager.getOpenSubmissions(userInfo, nextPageToken);
	}
}
