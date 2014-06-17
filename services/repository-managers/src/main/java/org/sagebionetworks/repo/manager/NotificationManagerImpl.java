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
	public void sendNotification(UserInfo userInfo, Set<String> to, String subject,
			String message, String mimeType) throws NotFoundException {

		ContentType contentType = ContentType.create(mimeType, DEFAULT_CHARSET);
		FileItemStream fileItemStream = new ByteArrayFileItemStream(
					message.getBytes(DEFAULT_CHARSET), contentType.toString(), "");
		try {
			FileHandle fileHandle = fileHandleManager.uploadFile(userInfo.getId().toString(), fileItemStream);
			
			MessageToUser dto = new MessageToUser();
			dto.setSubject(subject);
			dto.setFileHandleId(fileHandle.getId());
			dto.setRecipients(to);
			
			messageManager.createMessage(userInfo, dto);
		} catch (ServiceUnavailableException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
