package org.sagebionetworks.repo.manager.dataaccess.notifications;

import java.util.List;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.DataAccessNotificationType;
import org.springframework.util.MimeTypeUtils;

public interface DataAccessNotificationBuilder {

	/**
	 * @return The list of {@link DataAccessNotificationType} supported by this builder
	 */
	List<DataAccessNotificationType> supportedTypes();

	/**
	 * @return The mime type of the message created by this builder, default to HTML
	 */
	default String getMimeType() {
		return MimeTypeUtils.TEXT_HTML_VALUE;
	};

	/**
	 * @param accessRequirement The access requirement for the notification
	 * @param approval          The approval that led to this notification
	 * @param recipient         The recipient of the notification
	 * @return The subject line of the notification
	 */
	String buildSubject(ManagedACTAccessRequirement accessRequirement, AccessApproval approval, RestrictableObjectDescriptor objectReference, UserInfo recipient);

	/**
	 * @param accessRequirement The access requirement for the notification
	 * @param approval          The approval that led to this notification
	 * @param recipient         The recipient of the notification
	 * @return The message body of the notification
	 */
	String buildMessageBody(ManagedACTAccessRequirement accessRequirement, AccessApproval approval, RestrictableObjectDescriptor objectReference, UserInfo recipient);

}
