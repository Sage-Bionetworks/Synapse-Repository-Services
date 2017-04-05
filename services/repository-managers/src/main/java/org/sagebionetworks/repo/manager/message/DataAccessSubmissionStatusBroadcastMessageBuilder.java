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
import org.sagebionetworks.repo.model.broadcast.UserNotificationInfo;
import org.sagebionetworks.repo.model.dao.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

public class DataAccessSubmissionStatusBroadcastMessageBuilder implements BroadcastMessageBuilder{

	public static final String GREETING = "Hello %1$s,\n\n";
	private String subject;
	private String emailTemplate;
	private String submissionId;
	private String rejectedReason;
	private String requirementId;
	private boolean isRejected;
	private MarkdownDao markdownDao;

	DataAccessSubmissionStatusBroadcastMessageBuilder(String subject, String emailTemplate,
			String submissionId, String rejectedReason, String requirementId,
			MarkdownDao markdownDao, boolean isRejected) {
		this.subject = subject;
		this.emailTemplate = emailTemplate;
		this.submissionId = submissionId;
		this.isRejected = isRejected;
		if (isRejected) {
			this.rejectedReason = rejectedReason;
		}
		this.requirementId = requirementId;
		this.markdownDao = markdownDao;
	}

	@Override
	public Topic getBroadcastTopic() {
		Topic topic = new Topic();
		topic.setObjectId(submissionId);
		topic.setObjectType(SubscriptionObjectType.DATA_ACCESS_SUBMISSION_STATUS);
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
		if (isRejected) {
			sb.append(String.format(emailTemplate, rejectedReason, requirementId));
		} else {
			sb.append(String.format(emailTemplate, requirementId));
		}
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
