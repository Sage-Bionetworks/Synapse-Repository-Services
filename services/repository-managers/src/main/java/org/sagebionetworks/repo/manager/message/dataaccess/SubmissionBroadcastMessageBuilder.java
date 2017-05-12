package org.sagebionetworks.repo.manager.message.dataaccess;

import java.io.IOException;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.sagebionetworks.markdown.MarkdownClientException;
import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.SendRawEmailRequestBuilder;
import org.sagebionetworks.repo.manager.SendRawEmailRequestBuilder.BodyType;
import org.sagebionetworks.repo.manager.message.BroadcastMessageBuilder;
import org.sagebionetworks.repo.manager.subscription.SubscriptionManagerImpl;
import org.sagebionetworks.repo.model.broadcast.UserNotificationInfo;
import org.sagebionetworks.repo.model.dao.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

public class SubmissionBroadcastMessageBuilder implements BroadcastMessageBuilder{

	public static final String GREETING = "Hello %1$s,\n\n";
	public static final String TITLE = "Synapse Notification: New Data Access Request Submitted";
	public static final String EMAIL_TEMPLATE = "**[%1$s](https://www.synapse.org/#!Profile:%2$s)** "
			+ "has submitted a new data access request. \n"
			+ "Please visit the [Access Requirement Manager page](https://www.synapse.org/#!ACTDataAccessSubmissions:AR_ID=%3$s) to review the request.\n\n";
	public static final String UNSUBSCRIBE = "[Unsubscribe from Data Access Submission](https://www.synapse.org/#!Subscription:subscriptionID=%1$s)\n";

	private String actorUsername;
	private String actorUserId;
	private String accessRequirementId;
	private MarkdownDao markdownDao;

	SubmissionBroadcastMessageBuilder(String actorUsername, String actorUserId,
			String accessRequirementId, MarkdownDao markdownDao) {
		this.actorUsername = actorUsername;
		this.actorUserId = actorUserId;
		this.accessRequirementId = accessRequirementId;
		this.markdownDao = markdownDao;
	}

	@Override
	public Topic getBroadcastTopic() {
		Topic topic = new Topic();
		topic.setObjectId(SubscriptionManagerImpl.ALL_OBJECT_IDS);
		topic.setObjectType(SubscriptionObjectType.DATA_ACCESS_SUBMISSION);
		return topic;
	}

	@Override
	public SendRawEmailRequest buildEmailForSubscriber(Subscriber subscriber)
			throws ClientProtocolException, JSONException, IOException, MarkdownClientException {
		String body = buildRawBody(subscriber);
		return new SendRawEmailRequestBuilder()
		.withSubject(TITLE)
		.withBody(markdownDao.convertMarkdown(body, null), BodyType.HTML)
		.withSenderDisplayName("noreply")
		.withRecipientEmail(subscriber.getNotificationEmail())
		.build();
	}

	public String buildRawBody(Subscriber subscriber) {
		StringBuilder sb = new StringBuilder();
		String recipientName = EmailUtils.getDisplayNameWithUsername(subscriber.getFirstName(), subscriber.getLastName(), subscriber.getUsername());
		sb.append(String.format(GREETING, recipientName));
		sb.append(String.format(EMAIL_TEMPLATE, actorUsername, actorUserId, accessRequirementId));
		sb.append(String.format(UNSUBSCRIBE, subscriber.getSubscriptionId()));
		return sb.toString();
	}

	@Override
	public SendRawEmailRequest buildEmailForNonSubscriber(UserNotificationInfo user)
			throws ClientProtocolException, JSONException, IOException, MarkdownClientException {
		throw new IllegalArgumentException("Only subscribers receive notification of this type.");
	}

	@Override
	public Set<String> getRelatedUsers() {
		return null;
	}

}
