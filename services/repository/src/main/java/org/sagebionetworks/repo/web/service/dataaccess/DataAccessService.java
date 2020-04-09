package org.sagebionetworks.repo.web.service.dataaccess;

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

public interface DataAccessService {

	ResearchProject createOrUpdate(UserInfo userInfo, ResearchProject toCreate);

	ResearchProject getUserOwnResearchProjectForUpdate(UserInfo userInfo, String accessRequirementId);

	RequestInterface createOrUpdate(UserInfo userInfo, RequestInterface toCreateOrUpdate);

	RequestInterface getRequestForUpdate(UserInfo userInfo, String requirementId);

	SubmissionStatus submit(UserInfo userInfo, CreateSubmissionRequest request);

	SubmissionStatus cancel(UserInfo userInfo, String submissionId);

	Submission updateState(UserInfo userInfo, SubmissionStateChangeRequest request);

	SubmissionPage listSubmissions(UserInfo userInfo, SubmissionPageRequest request);

	AccessRequirementStatus getAccessRequirementStatus(UserInfo userInfo, String requirementId);

	RestrictionInformationResponse getRestrictionInformation(UserInfo userInfo, RestrictionInformationRequest request);

	OpenSubmissionPage getOpenSubmissions(UserInfo userInfo, String nextPageToken);

}
