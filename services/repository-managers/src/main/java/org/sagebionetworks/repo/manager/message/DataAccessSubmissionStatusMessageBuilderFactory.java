package org.sagebionetworks.repo.manager.message;

import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessSubmissionDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class DataAccessSubmissionStatusMessageBuilderFactory implements MessageBuilderFactory {

	public static final String APPROVED_TITLE = "Synapse Notification: Your request had been approved";
	public static final String APPROVED_TEMPLATE = "A member of the Synapse Access and Compliance Team has reviewed and approved your request.\n"
			// TODO: verify this link with Jay
			+"[View your request](https://www.synapse.org/#!Synapse:%1$s)";

	public static final String REJECTED_TITLE = "Synapse Notification: Action needed to complete your request";
	public static final String REJECTED_TEMPLATE = "A member of the Synapse Access and Compliance Team has reviewed your request and left a comment:\n"
			+ ">%1$s\n"
			// TODO: verify this link with Jay
			+ "Please visit [your request](https://www.synapse.org/#!Synapse:%2$s) and update information.\n\n";

	@Autowired
	private MarkdownDao markdownDao;
	@Autowired
	private DataAccessSubmissionDAO dataAccessSubmissionDao;

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
		return new DataAccessSubmissionStatusBroadcastMessageBuilder(APPROVED_TITLE, APPROVED_TEMPLATE,
					objectId, submission.getRejectedReason(), submission.getAccessRequirementId(),
					markdownDao, isRejected);
	}

}
