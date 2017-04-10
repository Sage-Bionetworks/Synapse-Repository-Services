package org.sagebionetworks.repo.manager.message;

import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessSubmissionDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class DataAccessSubmissionMessageBuilderFactory implements MessageBuilderFactory {

	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	@Autowired
	private MarkdownDao markdownDao;
	@Autowired
	private DataAccessSubmissionDAO dataAccessSubmissionDao;

	@Override
	public BroadcastMessageBuilder createMessageBuilder(String objectId, ChangeType changeType, Long userId) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.requirement(changeType != null && changeType.equals(ChangeType.CREATE),
				"Only send notification on CREATE event for this topic.");
		ValidateArgument.required(userId, "userId");
		DataAccessSubmission submission = dataAccessSubmissionDao.getSubmission(objectId);
		return new DataAccessSubmissionBroadcastMessageBuilder(principalAliasDAO.getUserName(userId),
				userId.toString(), submission.getAccessRequirementId(), markdownDao);
	}

}
