package org.sagebionetworks.repo.manager.message;

import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class ThreadMessageBuilderFactory implements MessageBuilderFactory {
	public static final String THREAD_TEMPLATE = "**[%1$s](https://www.synapse.org/#!Profile:%2$s)** "
			+ "created thread [%3$s](https://www.synapse.org/#!Synapse:%4$s/discussion/threadId=%5$s) "
			+ "in [%6$s](https://www.synapse.org/#!Synapse:%4$s/discussion) forum.\n>";
	public static final String THREAD_CREATED_TITLE = "Synapse Notification: New thread '%1$s'";
	public static final String UNSUBSCRIBE_FORUM = "[Unsubscribe from the forum](https://www.synapse.org/#!Subscription:subscriptionID=%1$s)\n";

	@Autowired
	private DiscussionThreadDAO threadDao;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	@Autowired
	private UserManager userManager;
	@Autowired
	private UploadContentToS3DAO uploadDao;
	@Autowired
	private MarkdownDao markdownDao;

	@Override
	public BroadcastMessageBuilder createMessageBuilder(String objectId,
			ChangeType changeType, Long userId) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(changeType, "changeType");
		Long threadId = Long.parseLong(objectId);
		// Lookup the thread
		DiscussionThreadBundle threadBundle = threadDao.getThread(threadId, DiscussionFilter.NO_FILTER);
		// Lookup the project
		String projectName = nodeDao.getNodeName(threadBundle.getProjectId());
		// Lookup the user name of the actor
		String actor = principalAliasDAO.getUserName(userId);
		String markdown = null;
		markdown = uploadDao.getMessage(threadBundle.getMessageKey());
		Topic broadcastTopic = new Topic();
		broadcastTopic.setObjectId(threadBundle.getForumId());
		broadcastTopic.setObjectType(SubscriptionObjectType.FORUM);
		return new DiscussionBroadcastMessageBuilder(actor, userId.toString(),
				threadBundle.getTitle(), threadBundle.getId(), threadBundle.getProjectId(),
				projectName, markdown, THREAD_TEMPLATE, THREAD_CREATED_TITLE,
				UNSUBSCRIBE_FORUM, markdownDao, broadcastTopic, userManager);
	}

}
