package org.sagebionetworks.repo.manager;

import java.io.IOException;
import java.nio.charset.Charset;
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

	private static final Charset DEFAULT_CHARSET = Charset.defaultCharset();
	
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

		ContentType contentType = ContentType.create(NotificationManager.TEXT_PLAIN_MIME_TYPE, DEFAULT_CHARSET);
		FileItemStream fileItemStream = new ByteArrayFileItemStream(
					message.getBody().getBytes(DEFAULT_CHARSET), contentType.toString(), "");
		try {
			FileHandle fileHandle = fileHandleManager.uploadFile(userInfo.getId().toString(), fileItemStream);
			
			message.getMetadata().setFileHandleId(fileHandle.getId());
			
			messageManager.createMessage(userInfo, message.getMetadata());
		} catch (ServiceUnavailableException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
