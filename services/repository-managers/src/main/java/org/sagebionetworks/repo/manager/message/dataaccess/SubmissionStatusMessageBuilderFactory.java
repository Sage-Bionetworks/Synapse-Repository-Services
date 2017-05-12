package org.sagebionetworks.repo.manager.message.dataaccess;

import java.util.List;

import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.manager.message.BroadcastMessageBuilder;
import org.sagebionetworks.repo.manager.message.MessageBuilderFactory;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class SubmissionStatusMessageBuilderFactory implements MessageBuilderFactory {

	@Autowired
	private MarkdownDao markdownDao;
	@Autowired
	private SubmissionDAO submissionDao;
	@Autowired
	private AccessRequirementDAO accessRequirementDao;

	@Override
	public BroadcastMessageBuilder createMessageBuilder(String objectId, ChangeType changeType, Long userId) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.requirement(changeType != null && changeType.equals(ChangeType.UPDATE),
				"Only send notification on UPDATE event for this topic.");
		Submission submission = submissionDao.getSubmission(objectId);
		ValidateArgument.requirement(submission.getState().equals(SubmissionState.REJECTED)
				|| submission.getState().equals(SubmissionState.APPROVED),
				"SubmissionState not supported: "+submission.getState());
		boolean isRejected = submission.getState().equals(SubmissionState.REJECTED);
		List<RestrictableObjectDescriptor> subjects = accessRequirementDao.getSubjects(Long.parseLong(submission.getAccessRequirementId()));
		ValidateArgument.requirement(subjects != null && !subjects.isEmpty(),
				"There must be at least one subjects under access requirement.");
		return new SubmissionStatusBroadcastMessageBuilder(objectId,
				submission.getRejectedReason(), submission.getAccessRequirementId(), subjects.get(0), markdownDao, isRejected);
	}

}
