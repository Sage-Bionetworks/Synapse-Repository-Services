package org.sagebionetworks.repo.web.service.dataaccess;

import org.sagebionetworks.repo.model.dataaccess.DataAccessRequestInterface;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionPageRequest;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionStatus;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmissionPage;
import org.sagebionetworks.repo.model.RestrictionInformation;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.BatchAccessApprovalRequest;
import org.sagebionetworks.repo.model.dataaccess.BatchAccessApprovalResult;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;

public interface DataAccessService {

	ResearchProject createOrUpdate(Long userId, ResearchProject toCreate);

	ResearchProject getUserOwnResearchProjectForUpdate(Long userId, String accessRequirementId);

	DataAccessRequestInterface createOrUpdate(Long userId, DataAccessRequestInterface toCreateOrUpdate);

	DataAccessRequestInterface getRequestForUpdate(Long userId, String requirementId);

	DataAccessSubmissionStatus submit(Long userId, String requestId, String etag);

	DataAccessSubmissionStatus cancel(Long userId, String submissionId);

	DataAccessSubmission updateState(Long userId, SubmissionStateChangeRequest request);

	DataAccessSubmissionPage listSubmissions(Long userId, DataAccessSubmissionPageRequest request);

	AccessRequirementStatus getAccessRequirementStatus(Long userId, String requirementId);

	RestrictionInformation getRestrictionInformation(Long userId, String entityId);

	OpenSubmissionPage getOpenSubmissions(Long userId, String nextPageToken);

	BatchAccessApprovalResult getAccessApprovalInfo(Long userId, BatchAccessApprovalRequest batchRequest);

}
