package org.sagebionetworks.repo.web.service.dataaccess;

import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
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
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;

public interface DataAccessService {

	ResearchProject createOrUpdate(Long userId, ResearchProject toCreate);

	ResearchProject getUserOwnResearchProjectForUpdate(Long userId, String accessRequirementId);

	RequestInterface createOrUpdate(Long userId, RequestInterface toCreateOrUpdate);

	RequestInterface getRequestForUpdate(Long userId, String requirementId);

	SubmissionStatus submit(Long userId, CreateSubmissionRequest request);

	SubmissionStatus cancel(Long userId, String submissionId);

	Submission updateState(Long userId, SubmissionStateChangeRequest request);

	SubmissionPage listSubmissions(Long userId, SubmissionPageRequest request);

	void deleteSubmission(Long userId, String submissionId);

	SubmissionInfoPage listInfoForApprovedSubmissions(SubmissionInfoPageRequest request);

	AccessRequirementStatus getAccessRequirementStatus(Long userId, String requirementId);

	RestrictionInformationResponse getRestrictionInformation(Long userId, RestrictionInformationRequest request);

	OpenSubmissionPage getOpenSubmissions(Long userId, String nextPageToken);

}
