package org.sagebionetworks.repo.manager.message;

import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class DataAccessSubmissionMessageBuilderFactory implements MessageBuilderFactory {

	public static final String TITLE = "Synapse Notification: New Data Access Request Submitted";
	public static final String EMAIL_TEMPLATE = "**[%1$s](https://www.synapse.org/#!Profile:%2$s)** "
			+ "has submitted a new data access request. \n"
			// TODO: verify this link with Jay
			+ "Please visit the [Access Requirement Manager page](https://www.synapse.org/#!Synapse:) to review the request.\n\n";
	public static final String UNSUBSCRIBE = "[Unsubscribe from Data Access Submission](https://www.synapse.org/#!Subscription:subscriptionID=%1$s)\n";

	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	@Autowired
	private MarkdownDao markdownDao;

	@Override
	public BroadcastMessageBuilder createMessageBuilder(String objectId, ChangeType changeType, Long userId) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.requirement(changeType != null && changeType.equals(ChangeType.CREATE),
				"Only send notification on CREATE event for this topic.");
		ValidateArgument.required(userId, "userId");
		return new DataAccessSubmissionBroadcastMessageBuilder(TITLE, EMAIL_TEMPLATE,
				principalAliasDAO.getUserName(userId), userId.toString(), UNSUBSCRIBE, markdownDao);
	}

}
