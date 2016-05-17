package org.sagebionetworks.repo.manager.message;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.SendRawEmailRequestBuilder;
import org.sagebionetworks.repo.manager.SendRawEmailRequestBuilder.BodyType;
import org.sagebionetworks.repo.model.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.utils.HttpClientHelperException;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

public class DiscussionBroadcastMessageBuilder implements BroadcastMessageBuilder {
	public static final String GREETING = "Hello %1$s,\n\n";
	public static final String UNSUBSCRIBE = "[Unsubscribe to the thread](https://www.synapse.org/#!Subscription:subscriptionID=%1$s)\n";
	MarkdownDao markdownDao;
	String actorUsername;
	String actorUserId;
	String threadTitleTruncated;
	String threadId;
	String projectId;
	String projectName;
	String markdown;
	String subject;
	String emailTemplate;

	public DiscussionBroadcastMessageBuilder(String actorUsername, String actorUserId,
			String threadTitle, String threadId, String projectId, String projectName,
			String markdown, String emailTemplate, String emailTitle, MarkdownDao markdownDao) {
		ValidateArgument.required(actorUsername, "actorUsername");
		ValidateArgument.required(actorUserId, "actorUserId");
		ValidateArgument.required(threadTitle, "threadTitle");
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(projectId, "projectId");
		ValidateArgument.required(projectName, "projectName");
		ValidateArgument.required(markdown, "markdown");
		ValidateArgument.required(emailTemplate, "emailTemplate");
		ValidateArgument.required(emailTitle, "emailTitle");
		ValidateArgument.required(markdownDao, "markdownDao");
		this.actorUsername = actorUsername;
		this.actorUserId = actorUserId;
		this.threadId = threadId;
		this.threadTitleTruncated = truncateString(threadTitle, 50);
		this.projectId = projectId;
		this.projectName = projectName;
		this.markdown = markdown.replace("\n", "\n>");
		this.subject = String.format(emailTitle, threadTitleTruncated);
		this.emailTemplate = emailTemplate;
		this.markdownDao = markdownDao;
	}

	@Override
	public Topic getBroadcastTopic() {
		Topic topic = new Topic();
		topic.setObjectId(threadId);
		topic.setObjectType(SubscriptionObjectType.THREAD);
		return topic;
	}

	@Override
	public SendRawEmailRequest buildEmailForSubscriber(Subscriber subscriber) throws ClientProtocolException, JSONException, IOException, HttpClientHelperException {
		// build the email body
		String body = buildRawBody(subscriber);
		return new SendRawEmailRequestBuilder()
		.withSubject(subject)
		.withBody(markdownDao.convertMarkdown(body, null), BodyType.HTML)
		.withSenderDisplayName("noreply")
		.withRecipientEmail(subscriber.getNotificationEmail())
		.build();
	}
	
	/**
	 * Build the email body.
	 * @param subscriber
	 * @return
	 */
	public String buildRawBody(Subscriber subscriber){
		StringBuilder sb = new StringBuilder();
		String recipientName = EmailUtils.getDisplayNameWithUsername(subscriber.getFirstName(), subscriber.getLastName(), subscriber.getUsername());
		sb.append(String.format(GREETING, recipientName));
		sb.append(String.format(emailTemplate, actorUsername, actorUserId, threadTitleTruncated, projectId, threadId, projectName));
		sb.append(markdown+"\n\n");
		sb.append(String.format(UNSUBSCRIBE, subscriber.getSubscriptionId()));
		return sb.toString();
	}

	/**
	 * Truncate a string to the given max length if needed.
	 * @param toTruncate
	 * @param maxLength
	 * @return
	 */
	public static String truncateString(String toTruncate, int maxLength){
		if(toTruncate.length() <= maxLength){
			return toTruncate;
		}else{
			return toTruncate.substring(0, maxLength)+"...";
		}
	}
}
