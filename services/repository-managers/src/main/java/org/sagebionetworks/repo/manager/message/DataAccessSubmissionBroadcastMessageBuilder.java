package org.sagebionetworks.repo.manager.message;

import java.io.IOException;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.sagebionetworks.markdown.MarkdownClientException;
import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.SendRawEmailRequestBuilder;
import org.sagebionetworks.repo.manager.SendRawEmailRequestBuilder.BodyType;
import org.sagebionetworks.repo.manager.subscription.SubscriptionManagerImpl;
import org.sagebionetworks.repo.model.broadcast.UserNotificationInfo;
import org.sagebionetworks.repo.model.dao.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

public class DataAccessSubmissionBroadcastMessageBuilder implements BroadcastMessageBuilder{

	public static final String GREETING = "Hello %1$s,\n\n";
	private String subject;
	private String emailTemplate;
	private String actorUsername;
	private String actorUserId;
	private String unsubscribe;
	private MarkdownDao markdownDao;

	DataAccessSubmissionBroadcastMessageBuilder(String subject, String emailTemplate,
			String actorUsername, String actorUserId, String unsubscribe, MarkdownDao markdownDao) {
		this.subject = subject;
		this.emailTemplate = emailTemplate;
		this.actorUsername = actorUsername;
		this.actorUserId = actorUserId;
		this.unsubscribe = unsubscribe;
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
		.withSubject(subject)
		.withBody(markdownDao.convertMarkdown(body, null), BodyType.HTML)
		.withSenderDisplayName("noreply")
		.withRecipientEmail(subscriber.getNotificationEmail())
		.build();
	}

	public String buildRawBody(Subscriber subscriber) {
		StringBuilder sb = new StringBuilder();
		String recipientName = EmailUtils.getDisplayNameWithUsername(subscriber.getFirstName(), subscriber.getLastName(), subscriber.getUsername());
		sb.append(String.format(GREETING, recipientName));
		sb.append(String.format(emailTemplate, actorUsername, actorUserId));
		sb.append(String.format(unsubscribe, subscriber.getSubscriptionId()));
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
