package org.sagebionetworks.repo.manager.message;

import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class ReplyMessageBuilderFactory implements MessageBuilderFactory {
	public static final String REPLY_TEMPLATE = "**[%1$s](https://www.synapse.org/#!Profile:%2$s)** "
			+ "replied to [%3$s](https://www.synapse.org/#!Synapse:%4$s/discussion/threadId=%5$s) "
			+ "thread in [%6$s](https://www.synapse.org/#!Synapse:%4$s/discussion) forum.\n>";
	public static final String REPLY_CREATED_TITLE = "Synapse Notification: New reply created in thread '%1$s'";
	public static final String UNSUBSCRIBE_THREAD = "[Unsubscribe from the thread](https://www.synapse.org/#!Subscription:subscriptionID=%1$s)\n";
	
	@Autowired
	private DiscussionReplyDAO replyDao;
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
		Long replyId = Long.parseLong(objectId);
		// Lookup the reply
		DiscussionReplyBundle replyBundle = replyDao.getReply(replyId, DiscussionFilter.NO_FILTER);
		// Lookup the thread
		DiscussionThreadBundle threadBundle = threadDao.getThread(Long.parseLong(replyBundle.getThreadId()), DiscussionFilter.NO_FILTER);
		// Lookup the project
		String projectName = nodeDao.getNodeName(threadBundle.getProjectId());
		// Lookup the user name of the actor
		String actor = principalAliasDAO.getUserName(userId);
		String markdown = null;
		markdown = uploadDao.getMessage(replyBundle.getMessageKey());
		Topic broadcastTopic = new Topic();
		broadcastTopic.setObjectId(replyBundle.getThreadId());
		broadcastTopic.setObjectType(SubscriptionObjectType.THREAD);
		return new DiscussionBroadcastMessageBuilder(actor, userId.toString(),
				threadBundle.getTitle(), threadBundle.getId(), threadBundle.getProjectId(),
				projectName, markdown, REPLY_TEMPLATE, REPLY_CREATED_TITLE, UNSUBSCRIBE_THREAD,
				markdownDao, broadcastTopic, userManager);
	}

}
