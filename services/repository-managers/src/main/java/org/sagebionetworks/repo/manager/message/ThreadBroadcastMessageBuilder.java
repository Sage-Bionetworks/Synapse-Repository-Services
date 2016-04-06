package org.sagebionetworks.repo.manager.message;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.SendRawEmailRequestBuilder;
import org.sagebionetworks.repo.manager.SendRawEmailRequestBuilder.BodyType;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.util.ValidateArgument;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.google.common.collect.Maps;

public class ThreadBroadcastMessageBuilder implements BroadcastMessageBuilder {
	
	public static final String THREAD_TEMPLATE = "message/threadTemplate.html";
	public static final String THREAD_CREATED_TITLE = "Synapse Notification: New thread '%1$s'";
	public static final String THREAD_UPDATED_TITLE = "Synapse Notification: '%1$s' thread has been updated";
	public static final String THREAD_DELETED_TITLE = "Synapse Notification: '%1$s' thread has been removed";

	DiscussionThreadBundle threadBundle;
	EntityHeader projectHeader;
	ChangeType changeType;
	String actor;
	String subject;
	String emailTemplate;
	String threadTitleTruncated;

	public ThreadBroadcastMessageBuilder(DiscussionThreadBundle threadBundle,
			EntityHeader projectHeader, ChangeType changeType, String actor) {
		ValidateArgument.required(threadBundle, "threadBundle");
		ValidateArgument.required(projectHeader, "projectHeader");
		ValidateArgument.required(changeType, "changeType");
		this.threadBundle = threadBundle;
		this.projectHeader = projectHeader;
		this.changeType = changeType;
		this.actor = actor;
		this.subject = buildSubject(threadBundle.getTitle(), changeType);
		this.threadTitleTruncated = BroadcastMessageBuilderUtil.truncateString(threadBundle.getTitle(), 50);
		// Load the template file
		emailTemplate = loadTemplateFile(THREAD_TEMPLATE);
	}



	@Override
	public Topic getBroadcastTopic() {
		// The topic for a reply is its owner thread.
		Topic topic = new Topic();
		topic.setObjectId(threadBundle.getId());
		topic.setObjectType(SubscriptionObjectType.THREAD);
		return topic;
	}


	@Override
	public SendRawEmailRequest buildEmailForSubscriber(Subscriber subscriber) {
		// build the email body
		String body = buildBody(subscriber);
		return new SendRawEmailRequestBuilder()
		.withSubject(subject)
		.withBody(body, BodyType.HTML)
		.withSenderDisplayName("noreply")
		.withRecipientEmail(subscriber.getNotificationEmail())
		.build();
	}
	
	/**
	 * Build the email body.
	 * @param subscriber
	 * @return
	 */
	public String buildBody(Subscriber subscriber){
		// Setup the map for this email
		Map<String,String> fieldValues = Maps.newHashMap();
		// display name
		String displayName = EmailUtils.getDisplayNameWithUsername(subscriber.getFirstName(), subscriber.getLastName(), subscriber.getUsername());
		fieldValues.put("#displayName#", displayName);
		fieldValues.put("#actor#", actor);
		fieldValues.put("#projectId#", projectHeader.getId());
		fieldValues.put("#threadId#", threadBundle.getId());
		fieldValues.put("#threadName#", threadTitleTruncated);
		fieldValues.put("#projectName#", projectHeader.getName());
		fieldValues.put("#subscriptionID#", subscriber.getSubscriptionId());
		fieldValues.put("#action#", BroadcastMessageBuilderUtil.getAction(changeType));
		return EmailUtils.buildMailFromTemplate(emailTemplate, fieldValues);
	}

	/**
	 * Builder a subject from the title and type.
	 * @param threadTitle
	 * @param changeType
	 * @return
	 */
	public static String buildSubject(String threadTitle, ChangeType changeType){
		switch(changeType) {
			case CREATE:
				return String.format(THREAD_CREATED_TITLE, BroadcastMessageBuilderUtil.truncateString(threadTitle, 50));
			case UPDATE:
				return String.format(THREAD_UPDATED_TITLE, BroadcastMessageBuilderUtil.truncateString(threadTitle, 50));
			case DELETE:
				return String.format(THREAD_DELETED_TITLE, BroadcastMessageBuilderUtil.truncateString(threadTitle, 50));
		}
		throw new IllegalArgumentException("Change type: "+changeType+" is not supported.");
	}
	
	/**
	 * Load a template file into memory.
	 * @param filePath
	 * @return
	 */
	public static String loadTemplateFile(String filePath){
		InputStream is = ThreadBroadcastMessageBuilder.class.getClassLoader().getResourceAsStream(filePath);
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

}
