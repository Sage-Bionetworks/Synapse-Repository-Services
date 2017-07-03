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
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.broadcast.UserNotificationInfo;
import org.sagebionetworks.repo.model.dao.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

public class SubmissionStatusBroadcastMessageBuilder implements BroadcastMessageBuilder{

	public static final String GREETING = "Hello %1$s,\n\n";
	public static final String APPROVED_TITLE = "Synapse Notification: Your request had been approved";
	public static final String APPROVED_TEMPLATE = "A member of the Synapse Access and Compliance Team has reviewed and approved your request."
			+ " Please visit the resource of interest (Project, Folder, File, or Team) to complete any additional requirements.\n\n"
			+"[View your request](%1$s)";

	public static final String REJECTED_TITLE = "Synapse Notification: Action needed to complete your request";
	public static final String REJECTED_TEMPLATE = "A member of the Synapse Access and Compliance Team has reviewed your request and left a comment:\n"
			+ ">%1$s\n"
			+ "Please visit [your request](%2$s) and update information.\n\n";
	public static final String ENTITY_REQUIREMENT_PAGE_LINK = "https://www.synapse.org/#!AccessRequirements:ID=%1$s&AR_ID=%2$s";
	public static final String TEAM_REQUIREMENT_PAGE_LINK = "https://www.synapse.org/#!AccessRequirements:teamID=%1$s&AR_ID=%2$s";

	private String subject;
	private String emailTemplate;
	private String submissionId;
	private String rejectedReason;
	private boolean isRejected;
	private String requestLink;
	private MarkdownDao markdownDao;

	SubmissionStatusBroadcastMessageBuilder(String submissionId,
			String rejectedReason, String requirementId, RestrictableObjectDescriptor rod,
			MarkdownDao markdownDao, boolean isRejected) {
		this.submissionId = submissionId;
		this.isRejected = isRejected;
		if (isRejected) {
			this.rejectedReason = rejectedReason;
			this.subject = REJECTED_TITLE;
			this.emailTemplate = REJECTED_TEMPLATE;
		} else {
			this.subject = APPROVED_TITLE;
			this.emailTemplate = APPROVED_TEMPLATE;
		}
		if (rod.getType().equals(RestrictableObjectType.ENTITY)) {
			this.requestLink = String.format(ENTITY_REQUIREMENT_PAGE_LINK, rod.getId(), requirementId);
		} else if (rod.getType().equals(RestrictableObjectType.TEAM)) {
			this.requestLink = String.format(TEAM_REQUIREMENT_PAGE_LINK, rod.getId(), requirementId);
		} else {
			throw new IllegalArgumentException("Do not support type: "+rod.getType());
		}
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
			sb.append(String.format(emailTemplate, rejectedReason, requestLink));
		} else {
			sb.append(String.format(emailTemplate, requestLink));
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
