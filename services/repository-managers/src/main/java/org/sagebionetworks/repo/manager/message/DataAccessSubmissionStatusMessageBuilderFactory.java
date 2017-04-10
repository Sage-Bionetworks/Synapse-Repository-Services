package org.sagebionetworks.repo.manager.message;

import java.util.List;

import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessSubmissionDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class DataAccessSubmissionStatusMessageBuilderFactory implements MessageBuilderFactory {

	@Autowired
	private MarkdownDao markdownDao;
	@Autowired
	private DataAccessSubmissionDAO dataAccessSubmissionDao;
	@Autowired
	private AccessRequirementDAO accessRequirementDao;

	@Override
	public BroadcastMessageBuilder createMessageBuilder(String objectId, ChangeType changeType, Long userId) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.requirement(changeType != null && changeType.equals(ChangeType.UPDATE),
				"Only send notification on UPDATE event for this topic.");
		DataAccessSubmission submission = dataAccessSubmissionDao.getSubmission(objectId);
		ValidateArgument.requirement(submission.getState().equals(DataAccessSubmissionState.REJECTED)
				|| submission.getState().equals(DataAccessSubmissionState.APPROVED),
				"DataAccessSubmissionState not supported: "+submission.getState());
		boolean isRejected = submission.getState().equals(DataAccessSubmissionState.REJECTED);
		List<RestrictableObjectDescriptor> subjects = accessRequirementDao.getSubjects(Long.parseLong(submission.getAccessRequirementId()));
		ValidateArgument.requirement(subjects != null && !subjects.isEmpty(),
				"There must be at least one subjects under access requirement.");
		return new DataAccessSubmissionStatusBroadcastMessageBuilder(objectId,
				submission.getRejectedReason(), submission.getAccessRequirementId(), subjects.get(0), markdownDao, isRejected);
	}

}
