package org.sagebionetworks.repo.manager.message;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.util.MimeTypeUtils;

/**
 * A builder used to build an immutable {@link MessageTemplate}
 *
 */
public class MessageTemplateBuilder {

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
	private TemplateContextProvider templateContextProvider;

	public MessageTemplateBuilder() {
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
	 * @see {@link TemplateContextProvider#getTemplateContext(UserNameProvider)}
	 * @param templateContextProvider the templateContextProvider to set
	 */
	public MessageTemplateBuilder withTemplateContextProvider(TemplateContextProvider templateContextProvider) {
		ValidateArgument.required(templateContextProvider, "templateContextProvider");
		this.templateContextProvider = templateContextProvider;
		return this;
	}

	/**
	 * Build a new immutable MessageTemplate
	 * @return
	 */
	public MessageTemplate build() {
		return new MessageTemplateImpl(templateFile, templateCharSet, messageBodyMimeType, sender, bcc, cc,
				ignoreNotificationSettings, subject, isNotificationMessage, includeUnsubscribeLink,
				includeProfileSettingLink, recipients, templateContextProvider);
	};

	private static class MessageTemplateImpl implements MessageTemplate {

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
		private final TemplateContextProvider templateContextProvider;

		public MessageTemplateImpl(String templateFile, Charset templateCharSet, String messageBodyMimeType,
				UserInfo sender, String bcc, String cc, boolean ignoreNotificationSettings, String subject,
				boolean isNotificationMessage, boolean includeUnsubscribeLink, boolean includeProfileSettingLink,
				Set<String> recipients, TemplateContextProvider templateContextProvider) {
			super();
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
			this.templateContextProvider = templateContextProvider;
		}

		@Override
		public Map<String, Object> getTemplateContext() {
			return templateContextProvider.getTemplateContext();
		}

		@Override
		public String getTemplateFile() {
			return templateFile;
		}

		@Override
		public Charset getTemplateCharSet() {
			return templateCharSet;
		}

		@Override
		public String getMessageBodyMimeType() {
			return messageBodyMimeType;
		}

		@Override
		public UserInfo getSender() {
			return sender;
		}

		@Override
		public String getBcc() {
			return bcc;
		}

		@Override
		public String getCc() {
			return cc;
		}

		@Override
		public boolean ignoreNotificationSettings() {
			return ignoreNotificationSettings;
		}

		@Override
		public String getSubject() {
			return subject;
		}

		@Override
		public boolean isNotificationMessage() {
			return isNotificationMessage;
		}

		@Override
		public boolean includeUnsubscribeLink() {
			return includeUnsubscribeLink;
		}

		@Override
		public boolean includeProfileSettingLink() {
			return includeProfileSettingLink;
		}

		@Override
		public Set<String> getRecipients() {
			return recipients;
		}

	}
}
