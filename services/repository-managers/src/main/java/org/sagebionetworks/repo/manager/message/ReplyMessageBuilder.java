package org.sagebionetworks.repo.manager.message;

import java.io.IOException;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.manager.discussion.DiscussionUtils;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.sagebionetworks.repo.model.broadcast.UserNotificationInfo;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

public class ReplyMessageBuilder implements MessageBuilder {
	public static final String REPLY_TEMPLATE = "**[%1$s](https://www.synapse.org/#!Profile:%2$s)** "
			+ "replied to [%3$s](https://www.synapse.org/#!Synapse:%4$s/discussion/threadId=%5$s) "
			+ "thread in [%6$s](https://www.synapse.org/#!Synapse:%4$s) forum.\n>";
	public static final String REPLY_CREATED_TITLE = "Synapse Notification: New reply created in thread '%1$s'";
	public static final String UNSUBSCRIBE_THREAD = "[Unsubscribe to the thread](https://www.synapse.org/#!Subscription:subscriptionID=%1$s)\n";
	
	@Autowired
	private DiscussionReplyDAO replyDao;
	@Autowired
	private DiscussionThreadDAO threadDao;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	@Autowired
	private UploadContentToS3DAO uploadDao;
	@Autowired
	private MarkdownDao markdownDao;

	private DiscussionBroadcastMessageBuilder builder;
	String markdown;
	Topic broadcastTopic;

	@Override
	public void createMessageBuilder(String objectId,
			ChangeType changeType, Long userId) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(changeType, "changeType");
		Long replyId = Long.parseLong(objectId);
		// Lookup the reply
		DiscussionReplyBundle replyBundle = replyDao.getReply(replyId, DiscussionFilter.NO_FILTER);
		// Lookup the thread
		DiscussionThreadBundle threadBundle = threadDao.getThread(Long.parseLong(replyBundle.getThreadId()), DiscussionFilter.NO_FILTER);
		// Lookup the project
		String projectName = nodeDao.getEntityHeader(threadBundle.getProjectId(), null).getName();
		// Lookup the user name of the actor
		String actor = principalAliasDAO.getUserName(userId);
		this.markdown = uploadDao.getMessage(replyBundle.getMessageKey());
		broadcastTopic = new Topic();
		broadcastTopic.setObjectId(replyBundle.getThreadId());
		broadcastTopic.setObjectType(SubscriptionObjectType.THREAD);
		builder =  new DiscussionBroadcastMessageBuilder(actor, userId.toString(),
				threadBundle.getTitle(), threadBundle.getId(), threadBundle.getProjectId(),
				projectName, markdown, REPLY_TEMPLATE, REPLY_CREATED_TITLE, UNSUBSCRIBE_THREAD,
				markdownDao);
	}

	@Override
	public Topic getBroadcastTopic() {
		return broadcastTopic;
	}

	@Override
	public SendRawEmailRequest buildEmailForSubscriber(Subscriber subscriber)
			throws ClientProtocolException, JSONException, IOException, HttpClientHelperException {
		return builder.buildEmailForSubscriber(subscriber);
	}

	@Override
	public SendRawEmailRequest buildEmailForNonSubscriber(UserNotificationInfo user)
			throws ClientProtocolException, JSONException, IOException, HttpClientHelperException {
		return builder.buildEmailForNonSubscriber(user);
	}

	@Override
	public Set<String> getRelatedUsers() {
		Set<String> usernameList = DiscussionUtils.getMentionedUsername(markdown);
		return principalAliasDAO.lookupPrincipalIds(usernameList);
	}
}
