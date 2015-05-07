package org.sagebionetworks.repo.manager;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Set;

import org.apache.commons.fileupload.FileItemStream;
import org.apache.http.entity.ContentType;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
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
	public void sendNotification(UserInfo userInfo, MessageToUserAndBody message) throws NotFoundException {
		
		if (message.getMetadata().getRecipients().isEmpty()) return;

		// TODO we disable pending completion of the feature see PLFM-3363
		if (false) {
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
