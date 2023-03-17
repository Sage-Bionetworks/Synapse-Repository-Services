package org.sagebionetworks.repo.manager;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.message.MessageTemplate;
import org.sagebionetworks.repo.manager.message.TemplatedMessageSender;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class NotificationManagerImpl implements NotificationManager {

	private FileHandleManager fileHandleManager;

	private MessageManager messageManager;
	
	private TemplatedMessageSender templatedMessageSender;
	
	private UserProfileManager userProfileManager;

	@Autowired
	public NotificationManagerImpl(FileHandleManager fileHandleManager, MessageManager messageManager, TemplatedMessageSender templatedMessageSender, UserProfileManager userProfileManager) {
		this.fileHandleManager = fileHandleManager;
		this.messageManager = messageManager;
		this.templatedMessageSender = templatedMessageSender;
		this.userProfileManager = userProfileManager;
	}

	public NotificationManagerImpl(FileHandleManager fileHandleManager, MessageManager messageManager) {
		this.fileHandleManager = fileHandleManager;
		this.messageManager = messageManager;
	}

	@Override
	public void sendNotifications(UserInfo userInfo, List<MessageToUserAndBody> messages) {
		for (MessageToUserAndBody message : messages) {
			sendNotification(userInfo, message);
		}
	}
	
	private void sendNotification(UserInfo userInfo, MessageToUserAndBody message) {
		
		if (message.getMetadata().getRecipients().isEmpty()) {
			return;
		}
		
		FileHandle fileHandle;
		
		try {
			fileHandle = fileHandleManager.createCompressedFileFromString(userInfo.getId().toString(), new Date(), message.getBody(),
					message.getMimeType());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		message.getMetadata().setFileHandleId(fileHandle.getId());
		message.getMetadata().setWithUnsubscribeLink(true);
		message.getMetadata().setIsNotificationMessage(true);
		message.getMetadata().setWithProfileSettingLink(false);

		messageManager.createMessage(userInfo, message.getMetadata());

	}
	
	@Override
	public void sendTemplatedNotification(UserInfo user, String template, String subject, Map<String, Object> context) {
		ValidateArgument.required(user, "The user");
		ValidateArgument.requiredNotEmpty(template, "The template");
		ValidateArgument.requiredNotEmpty(subject, "The subject");
		
		if (context == null) {
			context = new HashMap<>();
		}
		
		if (!context.containsKey("displayName")) {
			context.put("displayName", EmailUtils.getDisplayNameOrUsername(userProfileManager.getUserProfile(user.getId().toString())));
		}
		
		templatedMessageSender.sendMessage(MessageTemplate.builder()
			.withNotificationMessage(true)
			.withIncludeProfileSettingLink(false)
			.withIncludeUnsubscribeLink(false)
			.withIgnoreNotificationSettings(true)
			.withSender(new UserInfo(true, AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()))
			.withRecipients(Collections.singleton(user.getId().toString()))
			.withTemplateFile(template)
			.withSubject(subject)
			.withContext(context).build()
		);
		
	}

}
