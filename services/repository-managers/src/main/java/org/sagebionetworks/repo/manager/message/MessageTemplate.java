package org.sagebionetworks.repo.manager.message;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.util.MimeTypeUtils;

/**
 * Provides all of the information needed to create an email message to a user
 * using a {@link Template};
 *
 */
public class MessageTemplate {

	private final String templateFile;
	private final Charset templateCharSet;
	private final String messageBodyMimeType;
	private final UserInfo sender;
	private final String bcc;
	private final String cc;
	private final boolean ignoreNotificationSettings;
	private final String subject;
	private final boolean isNotificationMessage;
	private final boolean includeUnsubscribeLink;
	private final boolean includeProfileSettingLink;
	private final Set<String> recipients;
	private final Map<String, Object> context;

	MessageTemplate(String templateFile, Charset templateCharSet, String messageBodyMimeType, UserInfo sender,
			String bcc, String cc, boolean ignoreNotificationSettings, String subject, boolean isNotificationMessage,
			boolean includeUnsubscribeLink, boolean includeProfileSettingLink, Set<String> recipients,
			Map<String, Object> context) {
		this.templateFile = templateFile;
		this.templateCharSet = templateCharSet;
		this.messageBodyMimeType = messageBodyMimeType;
		this.sender = sender;
		this.bcc = bcc;
		this.cc = cc;
		this.ignoreNotificationSettings = ignoreNotificationSettings;
		this.subject = subject;
		this.isNotificationMessage = isNotificationMessage;
		this.includeUnsubscribeLink = includeUnsubscribeLink;
		this.includeProfileSettingLink = includeProfileSettingLink;
		this.recipients = Collections.unmodifiableSet(recipients);
		this.context = context;
	}

	/**
	 * A builder used to create an immutable template.
	 * 
	 * @return
	 */
	public static MessageTemplateBuilder builder() {
		return new MessageTemplateBuilder();
	}

	/**
	 * The path (classpath) to the file that defines the template.
	 * 
	 * @return the templateFile
	 */
	public String getTemplateFile() {
		return templateFile;
	}

	/**
	 * The {@link Charset} of the template file.
	 * 
	 * @return the templateCharSet
	 */
	public Charset getTemplateCharSet() {
		return templateCharSet;
	}

	/**
	 * The mime type of the message body.
	 * 
	 * @return the messageBodyMimeType
	 */
	public String getMessageBodyMimeType() {
		return messageBodyMimeType;
	}

	/**
	 * The user that is sending this message.
	 * 
	 * @return the sender
	 */
	public UserInfo getSender() {
		return sender;
	}

	/**
	 * Optional, when provided is the email bcc.
	 * 
	 * @return the bcc
	 */
	public Optional<String> getBcc() {
		return Optional.ofNullable(bcc);
	}

	/**
	 * Optional, when provided is the email cc.
	 * 
	 * @return the cc
	 */
	public Optional<String> getCc() {
		return Optional.ofNullable(cc);
	}

	/**
	 * Should the user's notification settings be ignored when sending this message.
	 * If true, then the message will be sent even if the user has disabled
	 * notifications in their settings.
	 * 
	 * @return the ignoreNotificationSettings
	 */
	public boolean ignoreNotificationSettings() {
		return ignoreNotificationSettings;
	}

	/**
	 * Optional, message subject.
	 * @see {@link MessageToUser#getSubject()}
	 * @return the subject
	 */
	public Optional<String> getSubject() {
		return  Optional.ofNullable(subject);
	}

	/**
	 * @see {@link MessageToUser#getIsNotificationMessage()}
	 * @return the isNotificationMessage
	 */
	public boolean isNotificationMessage() {
		return isNotificationMessage;
	}

	/**
	 * @see {@link MessageToUser#getWithUnsubscribeLink()}
	 * @return the includeUnsubscribeLink
	 */
	public boolean includeUnsubscribeLink() {
		return includeUnsubscribeLink;
	}

	/**
	 * @see {@link MessageToUser#getWithProfileSettingLink()}
	 * @return the includeProfileSettingLink
	 */
	public boolean includeProfileSettingLink() {
		return includeProfileSettingLink;
	}

	/**
	 * @see {@link MessageToUser#setRecipients(Set)}
	 * @return the recipients
	 */
	public Set<String> getRecipients() {
		return recipients;
	}

	/**
	 * The {@link VelocityContext} that the maps variable names within the message body to their replacement values.
	 * @return the context
	 */
	public Map<String, Object> getContext() {
		return context;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bcc, cc, context, ignoreNotificationSettings, includeProfileSettingLink,
				includeUnsubscribeLink, isNotificationMessage, messageBodyMimeType, recipients, sender, subject,
				templateCharSet, templateFile);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof MessageTemplate)) {
			return false;
		}
		MessageTemplate other = (MessageTemplate) obj;
		return Objects.equals(bcc, other.bcc) && Objects.equals(cc, other.cc) && Objects.equals(context, other.context)
				&& ignoreNotificationSettings == other.ignoreNotificationSettings
				&& includeProfileSettingLink == other.includeProfileSettingLink
				&& includeUnsubscribeLink == other.includeUnsubscribeLink
				&& isNotificationMessage == other.isNotificationMessage
				&& Objects.equals(messageBodyMimeType, other.messageBodyMimeType)
				&& Objects.equals(recipients, other.recipients) && Objects.equals(sender, other.sender)
				&& Objects.equals(subject, other.subject) && Objects.equals(templateCharSet, other.templateCharSet)
				&& Objects.equals(templateFile, other.templateFile);
	}

	public static class MessageTemplateBuilder {
		private String templateFile;
		private Charset templateCharSet;
		private String messageBodyMimeType;
		private UserInfo sender;
		private String bcc;
		private String cc;
		private boolean ignoreNotificationSettings;
		private String subject;
		private boolean isNotificationMessage;
		private boolean includeUnsubscribeLink;
		private boolean includeProfileSettingLink;
		private Set<String> recipients;
		private Map<String, Object> context;

		public MessageTemplateBuilder() {
			// default values
			templateCharSet = StandardCharsets.UTF_8;
			messageBodyMimeType = MimeTypeUtils.TEXT_HTML_VALUE;
			ignoreNotificationSettings = false;
			isNotificationMessage = true;
			includeUnsubscribeLink = true;
			includeProfileSettingLink = true;
			recipients = new HashSet<>();
		}

		/**
		 * @see {@link MessageTemplate#getTemplateFile()}
		 * @param templateFile the templateFile to set
		 */
		public MessageTemplateBuilder withTemplateFile(String templateFile) {
			ValidateArgument.required(templateFile, "templateFile");
			this.templateFile = templateFile;
			return this;
		}

		/**
		 * @see {@link MessageTemplate#getTemplateCharSet()}
		 * @param templateCharSet the templateCharSet to set
		 */
		public MessageTemplateBuilder withTemplateCharSet(Charset templateCharSet) {
			ValidateArgument.required(templateCharSet, "templateCharSet");
			this.templateCharSet = templateCharSet;
			return this;
		}

		/**
		 * @see {@link MessageTemplate#getMessageBodyMimeType()}
		 * @param messageBodyMimeType the messageBodyMimeType to set
		 */
		public MessageTemplateBuilder withMessageBodyMimeType(String messageBodyMimeType) {
			ValidateArgument.required(messageBodyMimeType, "messageBodyMimeType");
			this.messageBodyMimeType = messageBodyMimeType;
			return this;
		}

		/**
		 * @see {@link MessageTemplate#getSender()}
		 * @param sender the sender to set
		 */
		public MessageTemplateBuilder withSender(UserInfo sender) {
			ValidateArgument.required(sender, "sender");
			this.sender = sender;
			return this;
		}

		/**
		 * @see {@link MessageTemplate#getBcc()}
		 * @param bcc the bcc to set
		 */
		public MessageTemplateBuilder withBcc(String bcc) {
			this.bcc = bcc;
			return this;
		}

		/**
		 * @see {@link MessageTemplate#getCc()}
		 * @param cc the cc to set
		 */
		public MessageTemplateBuilder withCc(String cc) {
			this.cc = cc;
			return this;
		}

		/**
		 * @see {@link MessageTemplate#ignoreNotificationSettings()}
		 * @param ignoreNotificationSettings the ignoreNotificationSettings to set
		 */
		public MessageTemplateBuilder withIgnoreNotificationSettings(boolean ignoreNotificationSettings) {
			this.ignoreNotificationSettings = ignoreNotificationSettings;
			return this;
		}

		/**
		 * @see {@link MessageTemplate#getSubject()}
		 * @param subject the subject to set
		 */
		public MessageTemplateBuilder withSubject(String subject) {
			this.subject = subject;
			return this;
		}

		/**
		 * @see {@link MessageTemplate#isNotificationMessage()}
		 * @param isNotificationMessage the isNotificationMessage to set
		 */
		public MessageTemplateBuilder withNotificationMessage(boolean isNotificationMessage) {
			this.isNotificationMessage = isNotificationMessage;
			return this;
		}

		/**
		 * @see {@link MessageTemplate#includeUnsubscribeLink()}
		 * @param includeUnsubscribeLink the includeUnsubscribeLink to set
		 */
		public MessageTemplateBuilder withIncludeUnsubscribeLink(boolean includeUnsubscribeLink) {
			this.includeUnsubscribeLink = includeUnsubscribeLink;
			return this;
		}

		/**
		 * @see {@link MessageTemplate#includeProfileSettingLink()}
		 * @param includeProfileSettingLink the includeProfileSettingLink to set
		 */
		public MessageTemplateBuilder withIncludeProfileSettingLink(boolean includeProfileSettingLink) {
			this.includeProfileSettingLink = includeProfileSettingLink;
			return this;
		}

		/**
		 * @see {@link MessageTemplate#getRecipients()}
		 * @param recipients the recipients to set
		 */
		public MessageTemplateBuilder withRecipients(Set<String> recipients) {
			ValidateArgument.required(recipients, "recipients");
			this.recipients = recipients;
			return this;
		}

		/**
	 * The {@link VelocityContext} that the maps variable names within the message body to their replacement values.
		 * @param context the context to set
		 */
		public MessageTemplateBuilder withContext(Map<String, Object> context) {
			ValidateArgument.required(context, "context");
			this.context = context;
			return this;
		}

		/**
		 * Build a new immutable MessageTemplate
		 * 
		 * @return
		 */
		public MessageTemplate build() {
			return new MessageTemplate(templateFile, templateCharSet, messageBodyMimeType, sender, bcc, cc,
					ignoreNotificationSettings, subject, isNotificationMessage, includeUnsubscribeLink,
					includeProfileSettingLink, recipients, context);
		};
	}

}
