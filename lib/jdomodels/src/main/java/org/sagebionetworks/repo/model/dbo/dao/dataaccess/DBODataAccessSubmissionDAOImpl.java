package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionStatus;

public class DBODataAccessSubmissionDAOImpl implements DataAccessSubmissionDAO{

	@Override
	public DataAccessSubmissionStatus getStatus(String accessRequirementId, String userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataAccessSubmission updateStatus(String submissionId, DataAccessSubmissionState newState, String reason,
			String userId, Long timestamp, String etag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataAccessSubmissionStatus create(DataAccessSubmission submission) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataAccessSubmissionStatus cancel(String submissionId, String userId, Long timestamp, String etag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasSubmissionWithState(String userId, String accessRequirementId, DataAccessSubmissionState state) {
		// TODO Auto-generated method stub
		return false;
	}
}
