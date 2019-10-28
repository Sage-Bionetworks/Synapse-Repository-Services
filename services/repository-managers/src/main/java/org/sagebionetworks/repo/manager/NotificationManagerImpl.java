package org.sagebionetworks.repo.manager;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class NotificationManagerImpl implements NotificationManager {

	private static final Logger LOG = LogManager.getLogger(NotificationManagerImpl.class);
	
	@Autowired
	private FileHandleManager fileHandleManager;

	@Autowired
	private MessageManager messageManager;

	public NotificationManagerImpl() {}

	public NotificationManagerImpl(
			FileHandleManager fileHandleManager, 
			MessageManager messageManager) {
		this.fileHandleManager = fileHandleManager;
		this.messageManager = messageManager;
	}

	@Override
	public void sendNotifications(UserInfo userInfo, List<MessageToUserAndBody> messages, boolean stopOnFailure) throws NotFoundException {
		for (MessageToUserAndBody message : messages) {
			try {
				sendNotification(userInfo, message);
			} catch (Throwable e) {
				if (stopOnFailure) {
					throw e;
				}
				LOG.error("Cannot send notification: " + e.getMessage(), e);
			}
		}
	}
	
	private MessageToUser sendNotification(UserInfo userInfo, MessageToUserAndBody message){
		try {
			if (message.getMetadata().getRecipients().isEmpty()) {
				return null;
			}
			FileHandle fileHandle = fileHandleManager.createCompressedFileFromString(
					userInfo.getId().toString(), new Date(), message.getBody(), message.getMimeType());

			message.getMetadata().setFileHandleId(fileHandle.getId());
			message.getMetadata().setWithUnsubscribeLink(true);
			message.getMetadata().setIsNotificationMessage(true);
			message.getMetadata().setWithProfileSettingLink(false);
			return messageManager.createMessage(userInfo, message.getMetadata());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
