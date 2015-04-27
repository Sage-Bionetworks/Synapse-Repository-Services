package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.fileupload.FileItemStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageToUser;

public class NotificationManagerImplTest {

	private FileHandleManager fileHandleManager;
	private MessageManager messageManager;
	private NotificationManager notificationManager;
	
	private static final Long USER_ID = 101L;

	@Before
	public void setUp() throws Exception {
		fileHandleManager = Mockito.mock(FileHandleManager.class);
		messageManager = Mockito.mock(MessageManager.class);
		notificationManager = new NotificationManagerImpl(fileHandleManager, messageManager);
	}

	@Test
	public void testSendNotification() throws Exception {
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(USER_ID);
		Set<String> to = new HashSet<String>();
		String subject = "subject";
		String message = "message";
		String mimeType = "text/html";
		ArgumentCaptor<FileItemStream> fisCaptor = 
				ArgumentCaptor.forClass(FileItemStream.class);
		S3FileHandle fh = new S3FileHandle();
		fh.setFileName("foo.bar");
		String fileHandleId = "123";
		fh.setId(fileHandleId);
		when(fileHandleManager.uploadFile(eq(USER_ID.toString()), fisCaptor.capture())).
			thenReturn(fh);
		MessageToUser mtu = new MessageToUser();
		mtu.setRecipients(to);
		mtu.setSubject(subject);
		notificationManager.sendNotification(userInfo, mtu, message);
		verify(fileHandleManager).uploadFile(eq(USER_ID.toString()), any(FileItemStream.class));
		FileItemStream fis = fisCaptor.getValue();
		assertEquals("text/html; charset=UTF-8", fis.getContentType());
		ArgumentCaptor<MessageToUser> mtuCaptor =
				ArgumentCaptor.forClass(MessageToUser.class);
		verify(messageManager).createMessage(eq(userInfo), mtuCaptor.capture());
		MessageToUser mtu2 = mtuCaptor.getValue();
		assertEquals(fileHandleId, mtu2.getFileHandleId());
		assertEquals(subject, mtu2.getSubject());
		assertEquals(to, mtu2.getRecipients());
	}

}
