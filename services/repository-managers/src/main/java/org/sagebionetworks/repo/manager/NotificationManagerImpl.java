package org.sagebionetworks.repo.manager;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.springframework.beans.factory.annotation.Autowired;

public class NotificationManagerImpl implements NotificationManager {

	@Autowired
	private FileHandleManager fileHandleManager;

	@Autowired
	private MessageManager messageManager;

	public NotificationManagerImpl() {
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

}
