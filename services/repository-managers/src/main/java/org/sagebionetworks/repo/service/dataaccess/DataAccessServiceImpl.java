package org.sagebionetworks.repo.service.dataaccess;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.RequestManager;
import org.sagebionetworks.repo.manager.dataaccess.ResearchProjectManager;
import org.sagebionetworks.repo.manager.dataaccess.RestrictionInformationManager;
import org.sagebionetworks.repo.manager.dataaccess.SubmissionManager;
import org.sagebionetworks.repo.model.RestrictionInformationBatchRequest;
import org.sagebionetworks.repo.model.RestrictionInformationBatchResponse;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.CreateSubmissionRequest;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionInfoPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionInfoPageRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPageRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionSearchRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionSearchResponse;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataAccessServiceImpl implements DataAccessService {

	@Autowired
	private UserManager userManager;
	@Autowired
	private ResearchProjectManager researchProjectManager;
	@Autowired
	private RequestManager dataAccessRequestManager;
	@Autowired
	private SubmissionManager dataAccessSubmissionManager;
	@Autowired
	private RestrictionInformationManager restrictionInformationManager;

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
	public RequestInterface createOrUpdate(Long userId, RequestInterface toCreateOrUpdate) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessRequestManager.createOrUpdate(user, toCreateOrUpdate);
	}

	@Override
	public RequestInterface getRequestForUpdate(Long userId, String requirementId) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessRequestManager.getRequestForUpdate(user, requirementId);
	}

	@Override
	public SubmissionStatus submit(Long userId, CreateSubmissionRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessSubmissionManager.create(user, request);
	}

	@Override
	public SubmissionStatus cancel(Long userId, String submissionId) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessSubmissionManager.cancel(user, submissionId);
	}

	@Override
	public Submission updateState(Long userId, SubmissionStateChangeRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessSubmissionManager.updateStatus(user, request);
	}

	@Override
	public SubmissionPage listSubmissions(Long userId, SubmissionPageRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessSubmissionManager.listSubmission(user, request);
	}
	
	@Override
	public void deleteSubmission(Long userId, String submissionId) {
		UserInfo user = userManager.getUserInfo(userId);
		dataAccessSubmissionManager.deleteSubmission(user, submissionId);
	}



	@Override
	public SubmissionInfoPage listInfoForApprovedSubmissions(Long userId, SubmissionInfoPageRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessSubmissionManager.listInfoForApprovedSubmissions(user, request);
	}

	@Override
	public AccessRequirementStatus getAccessRequirementStatus(Long userId, String requirementId) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessSubmissionManager.getAccessRequirementStatus(user, requirementId);
	}

	@Override
	public RestrictionInformationResponse getRestrictionInformation(Long userId, RestrictionInformationRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return restrictionInformationManager.getRestrictionInformation(user, request);
	}
	
	@Override
	public RestrictionInformationBatchResponse getRestrictionInformationBatch(Long userId, RestrictionInformationBatchRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return restrictionInformationManager.getRestrictionInformationBatch(user, request);
	}

	@Override
	public OpenSubmissionPage getOpenSubmissions(Long userId, String nextPageToken) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessSubmissionManager.getOpenSubmissions(user, nextPageToken);
	}
	
	@Override
	public SubmissionSearchResponse searchSubmissions(Long userId, SubmissionSearchRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessSubmissionManager.searchSubmissions(user, request);
	}
	
	@Override
	public Submission getSubmission(Long userId, String submissionId) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessSubmissionManager.getSubmission(user, submissionId);
	}
}
