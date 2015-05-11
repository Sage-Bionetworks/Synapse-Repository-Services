package org.sagebionetworks.repo.manager;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class NotificationManagerImpl implements NotificationManager {

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
	public void sendNotifications(UserInfo userInfo, List<MessageToUserAndBody> messages) throws NotFoundException {


		// TODO we disable pending completion of the feature see PLFM-3363
		if (false) {
			for (MessageToUserAndBody message : messages) {
				if (message.getMetadata().getRecipients().isEmpty()) continue;
				try {
					FileHandle fileHandle = fileHandleManager.createCompressedFileFromString(
							userInfo.getId().toString(), new Date(), message.getBody(), message.getMimeType());

					message.getMetadata().setFileHandleId(fileHandle.getId());

					messageManager.createMessage(userInfo, message.getMetadata());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

}
