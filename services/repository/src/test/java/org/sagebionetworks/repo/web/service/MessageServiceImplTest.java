package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.CloudMailInManager;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.cloudmailin.Message;
import org.springframework.test.util.ReflectionTestUtils;

public class MessageServiceImplTest {

	private MessageServiceImpl service;	
	@Mock
	private MessageManager messageManager;
	@Mock
	private UserManager userManager;
	@Mock
	private CloudMailInManager cloudMailInManager;
	@Mock
	private FileHandleManager fileHandleManager;
	@Mock
	private S3FileHandle mockS3FileHandle;
	private MessageToUser mtu;
	private String notificationUnsubscribeEndpoint;
	private Message message;
	private UserInfo userInfo;
	private MessageToUserAndBody mtub;
	private String fileHandleId;
	private String subject;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		service = new MessageServiceImpl();
		ReflectionTestUtils.setField(service, "messageManager", messageManager);
		ReflectionTestUtils.setField(service, "userManager", userManager);
		ReflectionTestUtils.setField(service, "cloudMailInManager", cloudMailInManager);
		ReflectionTestUtils.setField(service, "fileHandleManager", fileHandleManager);
		when(fileHandleManager.createCompressedFileFromString(anyString(), any(Date.class), any(), any())).thenReturn(mockS3FileHandle);
		subject = "subject";
		fileHandleId = "888";
		when(mockS3FileHandle.getId()).thenReturn(fileHandleId);
		message = new Message();
		notificationUnsubscribeEndpoint = "https://www.synapse.org/#foo:";
		mtub = new MessageToUserAndBody();
		mtu = new MessageToUser();
		Long creator = 101L;
		mtu.setCreatedBy(creator.toString());
		mtu.setSubject(subject);
		userInfo = new UserInfo(false);
		userInfo.setId(creator);
		when(userManager.getUserInfo(creator)).thenReturn(userInfo);

	}

	@Test
	public void testCreateCloudMailInMessage() throws Exception {
		Set<String> to = new HashSet<String>(); // EMPTY SET
		to.add("111");
		mtu.setRecipients(to);
		mtub.setMetadata(mtu);
		when(cloudMailInManager.convertMessage(message, notificationUnsubscribeEndpoint)).thenReturn(Collections.singletonList(mtub));
		service.create(message, notificationUnsubscribeEndpoint);
		verify(fileHandleManager).createCompressedFileFromString(eq(userInfo.getId()+""), any(Date.class), eq(mtub.getBody()), eq(mtub.getMimeType()));
		ArgumentCaptor<MessageToUser> captor = ArgumentCaptor.forClass(MessageToUser.class);
		verify(messageManager).createMessage(eq(userInfo), captor.capture());
		MessageToUser mtu2 = captor.getValue();
		assertEquals(fileHandleId, mtu2.getFileHandleId());
		assertEquals(subject, mtu2.getSubject());
		assertEquals(to, mtu2.getRecipients());
	}

	@Test
	public void testCreateCloudMailInMessageNoRecipients() throws Exception {
		Set<String> to = new HashSet<String>(); // EMPTY SET
		mtu.setRecipients(to);
		mtub.setMetadata(mtu);
		when(cloudMailInManager.convertMessage(message, notificationUnsubscribeEndpoint)).thenReturn(Collections.singletonList(mtub));
		service.create(message, notificationUnsubscribeEndpoint);
		// there should be no message sent
		verify(fileHandleManager, never()).createCompressedFileFromString(anyString(), any(Date.class), anyString());
		verify(messageManager, never()).createMessage(any(UserInfo.class), any(MessageToUser.class));
	}
}
