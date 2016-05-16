package org.sagebionetworks.repo.manager.message;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.SendRawEmailRequestBuilder;
import org.sagebionetworks.repo.manager.SendRawEmailRequestBuilder.BodyType;
import org.sagebionetworks.repo.model.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.google.common.collect.Maps;

public class DiscussionBroadcastMessageBuilder implements BroadcastMessageBuilder {
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
			String markdown, String templatePath, String emailTitle, MarkdownDao markdownDao) {
		ValidateArgument.required(actorUsername, "actorUsername");
		ValidateArgument.required(actorUserId, "actorUserId");
		ValidateArgument.required(threadTitle, "threadTitle");
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(projectId, "projectId");
		ValidateArgument.required(projectName, "projectName");
		ValidateArgument.required(markdown, "markdown");
		ValidateArgument.required(templatePath, "templatePath");
		ValidateArgument.required(emailTitle, "emailTitle");
		ValidateArgument.required(markdownDao, "markdownDao");
		this.actorUsername = actorUsername;
		this.actorUserId = actorUserId;
		this.threadId = threadId;
		this.threadTitleTruncated = truncateString(threadTitle, 50);
		this.projectId = projectId;
		this.projectName = projectName;
		this.markdown = markdown;
		this.subject = String.format(emailTitle, threadTitleTruncated);
		// Load the template file
		emailTemplate = loadTemplateFile(templatePath);
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
	public SendRawEmailRequest buildEmailForSubscriber(Subscriber subscriber) {
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
		// Setup the map for this email
		Map<String,String> fieldValues = Maps.newHashMap();
		String recipientName = EmailUtils.getDisplayNameWithUsername(subscriber.getFirstName(), subscriber.getLastName(), subscriber.getUsername());
		fieldValues.put("#recipientName#", recipientName);
		fieldValues.put("#actorUsername#", actorUsername);
		fieldValues.put("#actorUserId#", actorUserId);
		fieldValues.put("#threadId#", threadId);
		fieldValues.put("#threadName#", threadTitleTruncated);
		fieldValues.put("#projectId#", projectId);
		fieldValues.put("#projectName#", projectName);
		fieldValues.put("#subscriptionID#", subscriber.getSubscriptionId());
		fieldValues.put("#content#", markdown);
		return EmailUtils.buildMailFromTemplate(emailTemplate, fieldValues);
	}

	/**
	 * Load a template file into memory.
	 * @param filePath
	 * @return
	 */
	public static String loadTemplateFile(String filePath){
		InputStream is = DiscussionBroadcastMessageBuilder.class.getClassLoader().getResourceAsStream(filePath);
		if (is==null){
			throw new IllegalStateException("Could not find file "+filePath);
		}
		try{
			try {
				return IOUtils.toString(is);
			} catch (IOException e) {
				throw new java.lang.RuntimeException(e);
			}
		}finally{
			IOUtils.closeQuietly(is);
		}
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
