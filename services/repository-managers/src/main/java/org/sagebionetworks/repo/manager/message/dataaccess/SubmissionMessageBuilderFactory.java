package org.sagebionetworks.repo.manager.message.dataaccess;

import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.manager.message.BroadcastMessageBuilder;
import org.sagebionetworks.repo.manager.message.MessageBuilderFactory;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class SubmissionMessageBuilderFactory implements MessageBuilderFactory {

	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	@Autowired
	private MarkdownDao markdownDao;
	@Autowired
	private SubmissionDAO submissionDao;

	@Override
	public BroadcastMessageBuilder createMessageBuilder(String objectId, ChangeType changeType, Long userId) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.requirement(changeType != null && changeType.equals(ChangeType.CREATE),
				"Only send notification on CREATE event for this topic.");
		ValidateArgument.required(userId, "userId");
		Submission submission = submissionDao.getSubmission(objectId);
		return new SubmissionBroadcastMessageBuilder(principalAliasDAO.getUserName(userId),
				userId.toString(), submission.getAccessRequirementId(), markdownDao);
	}

}
