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
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
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

public class ThreadMessageBuilder implements MessageBuilder {
	public static final String THREAD_TEMPLATE = "**[%1$s](https://www.synapse.org/#!Profile:%2$s)** "
			+ "created thread [%3$s](https://www.synapse.org/#!Synapse:%4$s/discussion/threadId=%5$s) "
			+ "in [%6$s](https://www.synapse.org/#!Synapse:%4$s) forum.\n>";
	public static final String THREAD_CREATED_TITLE = "Synapse Notification: New thread '%1$s'";
	public static final String UNSUBSCRIBE_FORUM = "[Unsubscribe to the forum](https://www.synapse.org/#!Subscription:subscriptionID=%1$s)\n";

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
	public void createMessageBuilder(String objectId, ChangeType changeType, Long userId) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(changeType, "changeType");
		Long threadId = Long.parseLong(objectId);
		// Lookup the thread
		DiscussionThreadBundle threadBundle = threadDao.getThread(threadId, DiscussionFilter.NO_FILTER);
		// Lookup the project
		String projectName = nodeDao.getEntityHeader(threadBundle.getProjectId(), null).getName();
		// Lookup the user name of the actor
		String actor = principalAliasDAO.getUserName(userId);
		this.markdown = uploadDao.getMessage(threadBundle.getMessageKey());
		broadcastTopic = new Topic();
		broadcastTopic.setObjectId(threadBundle.getForumId());
		broadcastTopic.setObjectType(SubscriptionObjectType.FORUM);
		builder = new DiscussionBroadcastMessageBuilder(actor, userId.toString(), threadBundle.getTitle(),
				threadBundle.getId(), threadBundle.getProjectId(), projectName, markdown,
				THREAD_TEMPLATE, THREAD_CREATED_TITLE, UNSUBSCRIBE_FORUM, markdownDao);
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
