package org.sagebionetworks.repo.web.service.dataaccess;

import org.sagebionetworks.repo.model.dataaccess.DataAccessRequestInterface;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionStatus;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;

public interface DataAccessService {

	ResearchProject createOrUpdate(Long userId, ResearchProject toCreate);

	ResearchProject getUserOwnResearchProjectForUpdate(Long userId, String accessRequirementId);

	DataAccessRequestInterface createOrUpdate(Long userId, DataAccessRequestInterface toCreateOrUpdate);

	DataAccessRequestInterface getRequestForUpdate(Long userId, String requirementId);

	DataAccessSubmissionStatus submit(Long userId, String requestId, String etag);

	DataAccessSubmissionStatus getStatus(Long userId, String requirementId);

	DataAccessSubmissionStatus cancel(Long userId, String submissionId);

	DataAccessSubmission updateState(Long userId, SubmissionStateChangeRequest request);

}
