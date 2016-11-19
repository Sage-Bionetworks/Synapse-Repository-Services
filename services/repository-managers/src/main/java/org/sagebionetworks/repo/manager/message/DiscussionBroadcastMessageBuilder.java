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
import org.sagebionetworks.repo.manager.discussion.DiscussionUtils;
import org.sagebionetworks.repo.model.broadcast.UserNotificationInfo;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.dao.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.utils.HttpClientHelperException;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

public class DiscussionBroadcastMessageBuilder implements BroadcastMessageBuilder {
	public static final String GREETING = "Hello %1$s,\n\n";
	public static final String SUBSCRIBE_THREAD = "[Subscribe to the thread](https://www.synapse.org/#!Subscription:objectID=%1$s&objectType=THREAD)\n";
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
	String unsubscribe;
	Topic broadcastTopic;
	PrincipalAliasDAO principalAliasDao;

	public DiscussionBroadcastMessageBuilder(String actorUsername, String actorUserId,
			String threadTitle, String threadId, String projectId, String projectName,
			String markdown, String emailTemplate, String emailTitle, String unsubscribe,
			MarkdownDao markdownDao, Topic broadcastTopic, PrincipalAliasDAO principalAliasDao) {
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
		ValidateArgument.required(unsubscribe, "unsubscribe");
		ValidateArgument.required(broadcastTopic, "broadcastTopic");
		ValidateArgument.required(principalAliasDao, "principalAliasDao");
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
		this.unsubscribe = unsubscribe;
		this.broadcastTopic = broadcastTopic;
		this.principalAliasDao = principalAliasDao;
	}

	@Override
	public Topic getBroadcastTopic() {
		return broadcastTopic;
	}

	@Override
	public SendRawEmailRequest buildEmailForSubscriber(Subscriber subscriber) throws ClientProtocolException, JSONException, IOException, MarkdownClientException {
		// build the email body
		String body = buildRawBodyForSubscriber(subscriber);
		return new SendRawEmailRequestBuilder()
		.withSubject(subject)
		.withBody(markdownDao.convertMarkdown(body, null), BodyType.HTML)
		.withSenderDisplayName("noreply")
		.withRecipientEmail(subscriber.getNotificationEmail())
		.build();
	}

	@Override
	public SendRawEmailRequest buildEmailForNonSubscriber(UserNotificationInfo user) throws ClientProtocolException, JSONException, IOException, MarkdownClientException {
		// build the email body
		String body = buildRawBodyForNonSubscriber(user);
		return new SendRawEmailRequestBuilder()
		.withSubject(subject)
		.withBody(markdownDao.convertMarkdown(body, null), BodyType.HTML)
		.withSenderDisplayName("noreply")
		.withRecipientEmail(user.getNotificationEmail())
		.build();
	}
	
	/**
	 * Build the email body.
	 * @param subscriber
	 * @return
	 */
	public String buildRawBodyForSubscriber(Subscriber subscriber){
		StringBuilder sb = new StringBuilder();
		String recipientName = EmailUtils.getDisplayNameWithUsername(subscriber.getFirstName(), subscriber.getLastName(), subscriber.getUsername());
		sb.append(String.format(GREETING, recipientName));
		sb.append(String.format(emailTemplate, actorUsername, actorUserId, threadTitleTruncated, projectId, threadId, projectName));
		sb.append(markdown+"\n\n");
		// only add subscribe to thread link to message that send to non thread subscribers
		if (broadcastTopic.getObjectType() != SubscriptionObjectType.THREAD) {
			sb.append(String.format(SUBSCRIBE_THREAD, threadId));
		}
		sb.append(String.format(unsubscribe, subscriber.getSubscriptionId()));
		return sb.toString();
	}

	/**
	 * Build the email body.
	 * @param user
	 * @return
	 */
	public String buildRawBodyForNonSubscriber(UserNotificationInfo user){
		StringBuilder sb = new StringBuilder();
		String recipientName = EmailUtils.getDisplayNameWithUsername(user.getFirstName(), user.getLastName(), user.getUsername());
		sb.append(String.format(GREETING, recipientName));
		sb.append(String.format(emailTemplate, actorUsername, actorUserId, threadTitleTruncated, projectId, threadId, projectName));
		sb.append(markdown+"\n\n");
		sb.append(String.format(SUBSCRIBE_THREAD, threadId));
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

	public String getMarkdown() {
		return markdown;
	}

	@Override
	public Set<String> getRelatedUsers() {
		Set<String> usernameList = DiscussionUtils.getMentionedUsername(markdown);
		return principalAliasDao.lookupPrincipalIds(usernameList);
	}
}
