package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageToUser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

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
		
		boolean stopOnFailure = true;
		
		// Call under test
		notificationManager.sendNotifications(userInfo, Collections.singletonList(new MessageToUserAndBody(mtu, message, "text/plain")), stopOnFailure);
		
		verify(fileHandleManager).createCompressedFileFromString(eq(USER_ID.toString()), any(Date.class), anyString(), eq("text/plain"));
		ArgumentCaptor<MessageToUser> mtuCaptor =
				ArgumentCaptor.forClass(MessageToUser.class);
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
	public void testSendNotificationWithFailureAndStopOnFailure() throws Exception {
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(USER_ID);
		String subject = "subject";
		String message = "message";
		S3FileHandle fh = new S3FileHandle();
		fh.setFileName("foo.bar");
		String fileHandleId = "123";
		fh.setId(fileHandleId);
		
		when(fileHandleManager.createCompressedFileFromString(eq(USER_ID.toString()), any(Date.class), anyString(), eq("text/plain"))).
			thenReturn(fh);
		
		MessageToUser mtu1 = new MessageToUser();
		mtu1.setRecipients(ImmutableSet.of("123"));
		mtu1.setSubject(subject);
		mtu1.setFileHandleId(fileHandleId);
		mtu1.setWithUnsubscribeLink(true);
		mtu1.setIsNotificationMessage(true);
		mtu1.setWithProfileSettingLink(false);
		
		MessageToUser mtu2 = new MessageToUser();
		mtu2.setRecipients(ImmutableSet.of("456"));
		mtu2.setSubject(subject);
		mtu2.setFileHandleId(fileHandleId);
		mtu2.setWithUnsubscribeLink(true);
		mtu2.setIsNotificationMessage(true);
		mtu2.setWithProfileSettingLink(false);
		
		IllegalArgumentException ex = new IllegalArgumentException("Some error");
		
		when(messageManager.createMessage(userInfo, mtu1)).thenThrow(ex);
		
		List<MessageToUserAndBody> messages = ImmutableList.of(
			new MessageToUserAndBody(mtu1, message, "text/plain"),
			new MessageToUserAndBody(mtu2, message, "text/plain")
		);
		
		boolean stopOnFailure = true;
		
		IllegalArgumentException result = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			notificationManager.sendNotifications(userInfo, messages, stopOnFailure);
		});
		
		assertEquals(ex, result);
		
		verify(fileHandleManager).createCompressedFileFromString(eq(USER_ID.toString()), any(Date.class), anyString(), eq("text/plain"));
		
		verify(messageManager).createMessage(userInfo, mtu1);
		verifyNoMoreInteractions(messageManager);
	}
	
	@Test
	public void testSendNotificationWithFailureAndContinueOnFailure() throws Exception {
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(USER_ID);
		String subject = "subject";
		String message = "message";
		S3FileHandle fh = new S3FileHandle();
		fh.setFileName("foo.bar");
		String fileHandleId = "123";
		fh.setId(fileHandleId);
		
		when(fileHandleManager.createCompressedFileFromString(eq(USER_ID.toString()), any(Date.class), anyString(), eq("text/plain"))).
			thenReturn(fh);
		
		MessageToUser mtu1 = new MessageToUser();
		mtu1.setRecipients(ImmutableSet.of("123"));
		mtu1.setSubject(subject);
		mtu1.setFileHandleId(fileHandleId);
		mtu1.setWithUnsubscribeLink(true);
		mtu1.setIsNotificationMessage(true);
		mtu1.setWithProfileSettingLink(false);
		
		MessageToUser mtu2 = new MessageToUser();
		mtu2.setRecipients(ImmutableSet.of("456"));
		mtu2.setSubject(subject);
		mtu2.setFileHandleId(fileHandleId);
		mtu2.setWithUnsubscribeLink(true);
		mtu2.setIsNotificationMessage(true);
		mtu2.setWithProfileSettingLink(false);
		
		IllegalArgumentException ex = new IllegalArgumentException("Some error");
		
		when(messageManager.createMessage(userInfo, mtu1)).thenThrow(ex);
		
		List<MessageToUserAndBody> messages = ImmutableList.of(
			new MessageToUserAndBody(mtu1, message, "text/plain"),
			new MessageToUserAndBody(mtu2, message, "text/plain")
		);
		
		boolean stopOnFailure = false;
	
		// Call under test
		notificationManager.sendNotifications(userInfo, messages, stopOnFailure);
		
		verify(fileHandleManager, times(2)).createCompressedFileFromString(eq(USER_ID.toString()), any(Date.class), anyString(), eq("text/plain"));
		
		verify(messageManager).createMessage(userInfo, mtu1);
		verify(messageManager).createMessage(userInfo, mtu2);
	}
	
	@Test
	public void testNoRecipients() throws Exception {
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(USER_ID);
		Set<String> to = new HashSet<String>(); // EMPTY SET
		MessageToUser mtu = new MessageToUser();
		mtu.setRecipients(to);
		String message = "message";
		boolean stopOnFailure = true;
		
		// Call under test
		notificationManager.sendNotifications(userInfo, Collections.singletonList(new MessageToUserAndBody(mtu, message, "text/plain")), stopOnFailure);
		// there should be no message sent
		verify(fileHandleManager, never()).createCompressedFileFromString(anyString(), any(Date.class), anyString());
		verify(messageManager, never()).createMessage(any(UserInfo.class), any(MessageToUser.class));

	}

}
