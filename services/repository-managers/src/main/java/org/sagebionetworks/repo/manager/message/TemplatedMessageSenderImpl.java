package org.sagebionetworks.repo.manager.message;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Repository;

@Repository
public class TemplatedMessageSenderImpl implements TemplatedMessageSender {
	
	private final VelocityEngine velocityEngine;
	private final FileHandleManager fileHandleManager;
	private final MessageManager messageManager;
	
	public TemplatedMessageSenderImpl(VelocityEngine velocityEngine,
			FileHandleManager fileHandleManager, MessageManager messageManager) {
		super();
		this.velocityEngine = velocityEngine;
		this.fileHandleManager = fileHandleManager;
		this.messageManager = messageManager;
	}

	@Override
	public MessageToUser sendMessage(MessageTemplate t) {
		ValidateArgument.required(t, "MessageTemplate");
		ValidateArgument.required(t.getTemplateFile(), "MessageTemplate.templateFile");
		ValidateArgument.required(t.getTemplateCharSet(), "MessageTemplate.templateCharSet");
		ValidateArgument.required(t.getMessageBodyMimeType(), "MessageTemplate.messageBodyMimeType");
		ValidateArgument.required(t.getSender(), "MessageTemplate.sender");
		ValidateArgument.required(t.getRecipients(), "MessageTemplate.recipients");
		
		Map<String, Object> templateContext = t.getTemplateContext();
		ValidateArgument.required(t.getTemplateFile(), "MessageTemplate.templateContext()");
		
		String messageBody = buildMessageBody(t.getTemplateFile(), t.getTemplateCharSet(), templateContext);
		String boddyFileHandleId = storeMessageBody(t.getSender(), messageBody, t.getMessageBodyMimeType());
		MessageToUser mtu = new MessageToUser();
		mtu.setBcc(t.getBcc());
		mtu.setCc(t.getCc());
		mtu.setFileHandleId(boddyFileHandleId);
		mtu.setSubject(t.getSubject());
		mtu.setIsNotificationMessage(t.isNotificationMessage());
		mtu.setWithUnsubscribeLink(t.includeUnsubscribeLink());
		mtu.setWithProfileSettingLink(t.includeProfileSettingLink());
		mtu.setRecipients(t.getRecipients());
		return messageManager.createMessage(t.getSender(), mtu, t.ignoreNotificationSettings());
	}
	
	String buildMessageBody(String templateFile, Charset templateCharset, Map<String, Object> templateContext) {
		Template template = velocityEngine.getTemplate(templateFile, templateCharset.toString());
		VelocityContext context = new VelocityContext(templateContext);
		StringWriter writer = new StringWriter();
		template.merge(context, writer);
		return writer.toString();
	}
	
	String storeMessageBody(UserInfo sender, String messageBody, String mimeType) {
		try {
			return fileHandleManager
					.createCompressedFileFromString(sender.getId().toString(), Date.from(Instant.now()), messageBody, mimeType).getId();
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

}
