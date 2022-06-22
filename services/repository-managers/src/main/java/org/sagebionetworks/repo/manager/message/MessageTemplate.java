package org.sagebionetworks.repo.manager.message;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.apache.velocity.Template;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.springframework.util.MimeTypeUtils;

/**
 * Provides all of the information needed to create an email message to a user using a {@link Template};
 *
 */
public interface MessageTemplate extends TemplateContextProvider {

	/**
	 * Get the path to the template file.
	 * 
	 * @return
	 */
	String getTemplateFile();

	/**
	 * The character set of the template.
	 * 
	 * @return
	 */
	default Charset getTemplateCharSet() {
		return StandardCharsets.UTF_8;
	}

	/**
	 * @return The mime type of the message created by this builder, default to HTML
	 */
	default String getMessageBodyMimeType() {
		return MimeTypeUtils.TEXT_HTML_VALUE;
	};

	/**
	 * Get the {@link UserInfo} that will be the sender of the notification email.
	 * 
	 * @return
	 */
	UserInfo getSender();

	/**
	 * See: {@link MessageToUser#getBcc()}
	 * @return
	 */
	default String getBcc() {
		return null;
	}

	/**
	 * See: {@link MessageToUser#getCc()}
	 * @return
	 */
	default String getCc() {
		return null;
	}

	/**
	 * If the overrideNotificationSettings is set to true the recipient notification settings will be ignored.
	 * @return
	 */
	default boolean ignoreNotificationSettings() {
		return false;
	}

	/**
	 * See: {@link MessageToUser#getSubject()}
	 * @return
	 */
	default String getSubject() {
		return null;
	}

	/**
	 * See: {@link MessageToUser#getIsNotificationMessage()}
	 * @return
	 */
	default boolean isNotificationMessage() {
		return true;
	}

	/**
	 * See: {@link MessageToUser#setWithUnsubscribeLink(Boolean)}
	 * @return
	 */
	default boolean includeUnsubscribeLink() {
		return true;
	}

	/**
	 * See: {@link MessageToUser#getWithProfileSettingLink()}
	 * @return
	 */
	default boolean includeProfileSettingLink() {
		return true;
	}

	/**
	 * See: {@link MessageToUser#setRecipients(Set)}
	 * @return
	 */
	Set<String> getRecipients();

}
