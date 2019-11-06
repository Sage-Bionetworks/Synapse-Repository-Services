package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageToUser;

@ExtendWith(MockitoExtension.class)
public class NotificationManagerImplTest {

	@Mock
	private FileHandleManager fileHandleManager;
	@Mock
	private MessageManager messageManager;
	
	@InjectMocks
	private NotificationManagerImpl notificationManager;
	
	private static final Long USER_ID = 101L;

	@Test
	public void testSendNotification() throws Exception {
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(USER_ID);
		Set<String> to = new HashSet<String>();
		to.add("000");
		String subject = "subject";
		String message = "message";
		S3FileHandle fh = new S3FileHandle();
		fh.setFileName("foo.bar");
		String fileHandleId = "123";
		fh.setId(fileHandleId);
		when(fileHandleManager.createCompressedFileFromString(eq(USER_ID.toString()), any(Date.class), anyString(), eq("text/plain"))).
			thenReturn(fh);
		MessageToUser mtu = new MessageToUser();
		mtu.setRecipients(to);
		mtu.setSubject(subject);
		
		// Call under test
		notificationManager.sendNotifications(userInfo, Collections.singletonList(new MessageToUserAndBody(mtu, message, "text/plain")));
		
		verify(fileHandleManager).createCompressedFileFromString(eq(USER_ID.toString()), any(Date.class), anyString(), eq("text/plain"));
		
		ArgumentCaptor<MessageToUser> mtuCaptor = ArgumentCaptor.forClass(MessageToUser.class);
		verify(messageManager).createMessage(eq(userInfo), mtuCaptor.capture());
		
		MessageToUser mtu2 = mtuCaptor.getValue();
		assertEquals(fileHandleId, mtu2.getFileHandleId());
		assertEquals(subject, mtu2.getSubject());
		assertEquals(to, mtu2.getRecipients());
		assertTrue(mtu2.getIsNotificationMessage());
		assertTrue(mtu2.getWithUnsubscribeLink());
		assertFalse(mtu2.getWithProfileSettingLink());
	}
	
	@Test
	public void testNoRecipients() throws Exception {
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(USER_ID);
		Set<String> to = new HashSet<String>(); // EMPTY SET
		MessageToUser mtu = new MessageToUser();
		mtu.setRecipients(to);
		String message = "message";
		
		// Call under test
		notificationManager.sendNotifications(userInfo, Collections.singletonList(new MessageToUserAndBody(mtu, message, "text/plain")));
		// there should be no message sent
		verify(fileHandleManager, never()).createCompressedFileFromString(anyString(), any(Date.class), anyString());
		verify(messageManager, never()).createMessage(any(UserInfo.class), any(MessageToUser.class));

	}

}
