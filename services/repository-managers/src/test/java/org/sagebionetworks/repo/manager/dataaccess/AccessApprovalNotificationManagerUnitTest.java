package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.AccessApprovalNotificationManagerImpl.RecipientProvider;
import org.sagebionetworks.repo.manager.dataaccess.AccessApprovalNotificationManagerImpl.SendConditionProvider;
import org.sagebionetworks.repo.manager.dataaccess.notifications.DataAccessNotificationBuilder;
import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.stack.ProdDetector;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DBODataAccessNotification;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessNotificationDao;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessNotificationType;
import org.sagebionetworks.repo.model.dbo.feature.Feature;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@ExtendWith(MockitoExtension.class)
public class AccessApprovalNotificationManagerUnitTest {

	@Mock
	private UserManager mockUserManager;
	@Mock
	private DataAccessNotificationDao mockNotificationDao;
	@Mock
	private AccessApprovalDAO mockAccessApprovalDao;
	@Mock
	private AccessRequirementDAO mockAccessRequirementDao;
	@Mock
	private FileHandleManager mockFileHandleManager;
	@Mock
	private MessageManager mockMessageManager;
	@Mock
	private FeatureManager mockFeatureManager;
	@Mock
	private ProdDetector mockProdDetector;
	
	@InjectMocks
	private AccessApprovalNotificationManagerImpl manager;

	@Mock
	private UserInfo mockUser;
	
	@Mock
	private DataAccessNotificationBuilder mockNotificationBuilder;
	
	@Mock
	private AccessApproval mockAccessApproval;
	
	@Mock
	private ManagedACTAccessRequirement mockManagedAccessRequirement;
	
	@Mock
	private AccessRequirement mockAccessRequirement;
	
	@Mock
	private S3FileHandle mockFileHandle;
	
	@Mock
	private MessageToUser mockMessageToUser;
	
	@Mock
	private ChangeMessage mockChangeMessage;
	
	@Mock
	private SendConditionProvider mockSendConditionProvider;	
	
	@Mock
	private RecipientProvider mockRecipientProvider;
	
	@Mock
	private DBODataAccessNotification mockNotification;
	@BeforeEach
	public void before() {
	}
	
	@Test
	public void testConfigureNotificationBuilders() {
		
		List<DataAccessNotificationType> supportedTypes = Collections.singletonList(DataAccessNotificationType.REVOCATION);
		
		when(mockNotificationBuilder.supportedTypes()).thenReturn(supportedTypes);
		List<DataAccessNotificationBuilder> builders = Collections.singletonList(mockNotificationBuilder);
		
		// Call under test
		manager.configureDataAccessNotificationBuilders(builders);
		
		assertEquals(mockNotificationBuilder, manager.getNotificationBuilder(DataAccessNotificationType.REVOCATION));
	}
	
	@Test
	public void testConfigureNotificationBuildersWithMultiple() {
		
		List<DataAccessNotificationType> supportedTypes = Arrays.asList(DataAccessNotificationType.REVOCATION, DataAccessNotificationType.FIRST_RENEWAL_REMINDER);
		
		when(mockNotificationBuilder.supportedTypes()).thenReturn(supportedTypes);
		List<DataAccessNotificationBuilder> builders = Collections.singletonList(mockNotificationBuilder);
		
		// Call under test
		manager.configureDataAccessNotificationBuilders(builders);
		
		for (DataAccessNotificationType type : supportedTypes) {
			assertEquals(mockNotificationBuilder, manager.getNotificationBuilder(type));
		}
	}
	
	@Test
	public void testConfigureNotificationBuildersWithConflicting() {
		
		List<DataAccessNotificationType> supportedTypes = Collections.singletonList(DataAccessNotificationType.REVOCATION);
		
		when(mockNotificationBuilder.supportedTypes()).thenReturn(supportedTypes);
		
		List<DataAccessNotificationBuilder> builders = Arrays.asList(mockNotificationBuilder, mockNotificationBuilder);
		
		String message = assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			manager.configureDataAccessNotificationBuilders(builders);
		}).getMessage();
		
		assertEquals("A notification builder for type " + DataAccessNotificationType.REVOCATION + " is already registred.", message);
		
	}

	@Test
	public void testConfigureNotificationBuildersWithEmpty() {
		List<DataAccessNotificationBuilder> builders = Collections.emptyList();
		
		// Call under test
		manager.configureDataAccessNotificationBuilders(builders);
	}
	
	@Test
	public void testGetNotificationBuilderWithNotConfgiured() {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.REVOCATION;
		
		String message = assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			manager.getNotificationBuilder(notificationType);
		}).getMessage();
		
		assertEquals("The message builders were not initialized.", message);
	}
	
	@Test
	public void testGetNotificationSender() {
		
		when(mockUserManager.getUserInfo(anyLong())).thenReturn(mockUser);
		
		// Call under test
		UserInfo user = manager.getNotificationsSender();
	
		assertEquals(mockUser, user);
		
		verify(mockUserManager).getUserInfo(BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId());
	}
	
	@Test
	public void testStoreMessageBody() throws Exception {
		String sender = "sender";
		String messageBody = "messageBody";
		String mimeType = "mimeType";
		String fileHandleId = "id";
		
		when(mockFileHandle.getId()).thenReturn(fileHandleId);
		when(mockFileHandleManager.createCompressedFileFromString(any(), any(), any(), any())).thenReturn(mockFileHandle);
		
		// Call under test
		String result = manager.storeMessageBody(sender, messageBody, mimeType);
		
		assertEquals(fileHandleId, result);
		
		verify(mockFileHandleManager).createCompressedFileFromString(eq(sender), any(), eq(messageBody), eq(mimeType));
		verify(mockFileHandle).getId();
	}
	
	@Test
	public void testStoreMessageBodyWithException() throws Exception {
		String sender = "sender";
		String messageBody = "messageBody";
		String mimeType = "mimeType";

		IOException ex = new IOException("Some error");
		
		when(mockFileHandleManager.createCompressedFileFromString(any(), any(), any(), any())).thenThrow(ex);
		
		IllegalStateException result = assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			manager.storeMessageBody(sender, messageBody, mimeType);
		});
		
		assertEquals(ex, result.getCause());

		verify(mockFileHandleManager).createCompressedFileFromString(eq(sender), any(), eq(messageBody), eq(mimeType));
	}
	
	@Test
	public void testDeliverMessageWithTestingUser() throws RecoverableMessageException {
		
		UserInfo recipient = mockUser;
		boolean isTestingUser = true;
		
		when(mockFeatureManager.isUserInTestingGroup(any())).thenReturn(isTestingUser);
		
		boolean expected = true;
		
		// Call under test
		boolean result = manager.deliverMessage(recipient);
	
		assertEquals(expected, result);
		verify(mockFeatureManager).isUserInTestingGroup(recipient);
		verifyZeroInteractions(mockProdDetector);
	}
	
	@Test
	public void testDeliverMessageWithProd() throws RecoverableMessageException {
		
		UserInfo recipient = mockUser;
		boolean isTestingUser = false;
		Optional<Boolean> isProd = Optional.of(true);
		
		when(mockFeatureManager.isUserInTestingGroup(any())).thenReturn(isTestingUser);
		when(mockProdDetector.isProductionStack()).thenReturn(isProd);
		
		boolean expected = true;
		
		// Call under test
		boolean result = manager.deliverMessage(recipient);
	
		assertEquals(expected, result);
		verify(mockFeatureManager).isUserInTestingGroup(recipient);
		verify(mockProdDetector).isProductionStack();
	}
	
	@Test
	public void testDeliverMessageWithStaging() throws RecoverableMessageException {
		
		UserInfo recipient = mockUser;
		boolean isTestingUser = false;
		Optional<Boolean> isProd = Optional.of(false);
		
		when(mockFeatureManager.isUserInTestingGroup(any())).thenReturn(isTestingUser);
		when(mockProdDetector.isProductionStack()).thenReturn(isProd);
		
		boolean expected = false;
		
		// Call under test
		boolean result = manager.deliverMessage(recipient);
	
		assertEquals(expected, result);
		verify(mockFeatureManager).isUserInTestingGroup(recipient);
		verify(mockProdDetector).isProductionStack();
	}
	
	@Test
	public void testDeliverMessageWithNoDetection() throws RecoverableMessageException {
		
		UserInfo recipient = mockUser;
		boolean isTestingUser = false;
		Optional<Boolean> isProd = Optional.empty();
		
		when(mockFeatureManager.isUserInTestingGroup(any())).thenReturn(isTestingUser);
		when(mockProdDetector.isProductionStack()).thenReturn(isProd);
		
		String message = assertThrows(RecoverableMessageException.class, () -> {
			// Call under test
			manager.deliverMessage(recipient);			
		}).getMessage();
	
		assertEquals("Could not detect current stack version.", message);
		verify(mockFeatureManager).isUserInTestingGroup(recipient);
		verify(mockProdDetector).isProductionStack();
	}
	
	@Test
	public void testCreateMessageToUser() throws Exception {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.REVOCATION;
		AccessApproval approval = mockAccessApproval;
		ManagedACTAccessRequirement accessRequirement = mockManagedAccessRequirement;
		UserInfo recipient = mockUser;
		UserInfo sender = mockUser;
		
		Long senderId = 2L;
		Long recipientId = 3L;
		
		String messageBody = "messageBody";
		String subject = "subject";
		String mimeType = "mimeType";
		String fileHandleId = "10";
		
		// We use a spy to mock out already tested methods
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);

		doReturn(mockNotificationBuilder).when(managerSpy).getNotificationBuilder(any());
		doReturn(sender).when(managerSpy).getNotificationsSender();
		doReturn(fileHandleId).when(managerSpy).storeMessageBody(any(), any(), any());

		when(mockNotificationBuilder.getMimeType()).thenReturn(mimeType);
		when(mockNotificationBuilder.buildMessageBody(any(), any(), any())).thenReturn(messageBody);
		when(mockNotificationBuilder.buildSubject(any(), any(), any())).thenReturn(subject);
		
		when(recipient.getId()).thenReturn(recipientId);
		when(sender.getId()).thenReturn(senderId);
		when(mockMessageManager.createMessage(any(), any(), anyBoolean())).thenReturn(mockMessageToUser);
		
		// Call under test
		MessageToUser result = managerSpy.createMessageToUser(notificationType, approval, accessRequirement, recipient);
		
		assertEquals(mockMessageToUser, result);

		verify(managerSpy).getNotificationBuilder(notificationType);
		verify(mockNotificationBuilder).getMimeType();
		verify(mockNotificationBuilder).buildSubject(accessRequirement, approval, recipient);
		verify(mockNotificationBuilder).buildMessageBody(accessRequirement, approval, recipient);
		verify(managerSpy).getNotificationsSender();
		verify(managerSpy).storeMessageBody(senderId.toString(), messageBody, mimeType);
		
		MessageToUser expectedMessage = new MessageToUser();

		expectedMessage.setSubject(subject);
		expectedMessage.setCreatedBy(sender.getId().toString());
		expectedMessage.setIsNotificationMessage(false);
		expectedMessage.setWithUnsubscribeLink(false);
		expectedMessage.setWithProfileSettingLink(false);
		expectedMessage.setFileHandleId(fileHandleId);
		expectedMessage.setRecipients(Collections.singleton(recipient.getId().toString()));
		
		verify(mockMessageManager).createMessage(sender, expectedMessage, true);
	}
	
	@Test
	public void testDiscardChangeMessage() {
		
		ObjectType objectType = ObjectType.ACCESS_APPROVAL;
		ChangeType changeType = ChangeType.UPDATE;
		Date timestamp = Date.from(Instant.now());
		
		when(mockChangeMessage.getObjectType()).thenReturn(objectType);
		when(mockChangeMessage.getChangeType()).thenReturn(changeType);
		when(mockChangeMessage.getTimestamp()).thenReturn(timestamp);
		
		boolean expected = false;
		
		// Call under test
		boolean result = manager.discardChangeMessage(mockChangeMessage);
	
		assertEquals(expected, result);
		
		verify(mockChangeMessage).getObjectType();
		verify(mockChangeMessage).getChangeType();
		verify(mockChangeMessage).getTimestamp();
	}
	
	@Test
	public void testDiscardChangeMessageWithWrongObjectType() {
		
		ObjectType objectType = ObjectType.ACCESS_CONTROL_LIST;
		
		when(mockChangeMessage.getObjectType()).thenReturn(objectType);
		
		boolean expected = true;
		
		// Call under test
		boolean result = manager.discardChangeMessage(mockChangeMessage);
	
		assertEquals(expected, result);
		
		verify(mockChangeMessage).getObjectType();
	}
	
	@Test
	public void testDiscardChangeMessageWithWrongChangeType() {
		
		ObjectType objectType = ObjectType.ACCESS_APPROVAL;
		ChangeType changeType = ChangeType.CREATE;
		
		when(mockChangeMessage.getObjectType()).thenReturn(objectType);
		when(mockChangeMessage.getChangeType()).thenReturn(changeType);
		
		boolean expected = true;
		
		// Call under test
		boolean result = manager.discardChangeMessage(mockChangeMessage);
	
		assertEquals(expected, result);
		
		verify(mockChangeMessage).getObjectType();
		verify(mockChangeMessage).getChangeType();
	}
	
	@Test
	public void testDiscardChangeMessageWithOldMessage() {
		
		ObjectType objectType = ObjectType.ACCESS_APPROVAL;
		ChangeType changeType = ChangeType.UPDATE;
		Date timestamp = Date.from(Instant.now().minus(AccessApprovalNotificationManager.CHANGE_TIMEOUT_HOURS + 1, ChronoUnit.HOURS));
		
		when(mockChangeMessage.getObjectType()).thenReturn(objectType);
		when(mockChangeMessage.getChangeType()).thenReturn(changeType);
		when(mockChangeMessage.getTimestamp()).thenReturn(timestamp);
		
		boolean expected = true;
		
		// Call under test
		boolean result = manager.discardChangeMessage(mockChangeMessage);
	
		assertEquals(expected, result);
		
		verify(mockChangeMessage).getObjectType();
		verify(mockChangeMessage).getChangeType();
		verify(mockChangeMessage).getTimestamp();
	}
	
	@Test
	public void testSendMessageIfNeeded() throws RecoverableMessageException {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.REVOCATION;
		AccessApproval approval = mockAccessApproval;
		UserInfo recipient = mockUser;
		DBODataAccessNotification existingNotification = null;
		ManagedACTAccessRequirement accessRequirement = mockManagedAccessRequirement;
		
		Long approvalId = 5L;
		Long requirementId = 1L;
		Long recipientId = 3L;
		Long messageId = 123L;
		Date createdOn = new Date();
		
		boolean canSend = true;
		boolean deliverMessage = true;
		
		// We use a spy to mock out already tested methods
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);
		
		doReturn(Optional.of(accessRequirement)).when(managerSpy).getManagedAccessRequirement(any());
		doReturn(mockRecipientProvider).when(managerSpy).getRecipientProvider(any());
		doReturn(mockSendConditionProvider).when(managerSpy).getSendConditionProvider(any());
		doReturn(deliverMessage).when(managerSpy).deliverMessage(any());
		doReturn(mockMessageToUser).when(managerSpy).createMessageToUser(any(), any(), any(), any());
		
		when(approval.getRequirementId()).thenReturn(requirementId);
		when(approval.getId()).thenReturn(approvalId);
		when(recipient.getId()).thenReturn(recipientId);
		when(mockMessageToUser.getId()).thenReturn(messageId.toString());
		when(mockMessageToUser.getCreatedOn()).thenReturn(createdOn);
		when(mockRecipientProvider.getRecipient(any())).thenReturn(recipientId);
		when(mockUserManager.getUserInfo(any())).thenReturn(mockUser);
		when(mockNotificationDao.findForUpdate(any(), any(), any())).thenReturn(Optional.ofNullable(existingNotification));
		when(mockSendConditionProvider.canSend(any(), any(), any())).thenReturn(canSend);
		
		// Call under test 
		managerSpy.sendMessageIfNeeded(notificationType, approval);
		
		verify(managerSpy).getManagedAccessRequirement(requirementId);
		verify(managerSpy).getRecipientProvider(notificationType);
		verify(mockRecipientProvider).getRecipient(approval);
		verify(mockNotificationDao).findForUpdate(notificationType, requirementId, recipientId);
		verify(managerSpy).getSendConditionProvider(notificationType);
		verify(mockSendConditionProvider).canSend(notificationType, approval, existingNotification);
		verify(managerSpy).deliverMessage(mockUser);
		verify(managerSpy).createMessageToUser(notificationType, approval, accessRequirement, recipient);
		
		DBODataAccessNotification expectedNotification = new DBODataAccessNotification();
		
		expectedNotification.setNotificationType(notificationType.name());
		expectedNotification.setRequirementId(requirementId);
		expectedNotification.setRecipientId(recipientId);
		expectedNotification.setAccessApprovalId(approvalId);
		expectedNotification.setMessageId(messageId);
		expectedNotification.setSentOn(new Timestamp(createdOn.getTime()));
		
		verify(mockNotificationDao).create(expectedNotification);
	}
	
	@Test
	public void testSendMessageIfNeededAndNoDelivery() throws RecoverableMessageException {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.REVOCATION;
		AccessApproval approval = mockAccessApproval;
		UserInfo recipient = mockUser;
		DBODataAccessNotification existingNotification = null;
		ManagedACTAccessRequirement accessRequirement = mockManagedAccessRequirement;
		
		Long approvalId = 5L;
		Long requirementId = 1L;
		Long recipientId = 3L;
		
		boolean canSend = true;
		boolean deliverMessage = false;
		
		// We use a spy to mock out already tested methods
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);
		
		doReturn(Optional.of(accessRequirement)).when(managerSpy).getManagedAccessRequirement(any());
		doReturn(mockRecipientProvider).when(managerSpy).getRecipientProvider(any());
		doReturn(mockSendConditionProvider).when(managerSpy).getSendConditionProvider(any());
		doReturn(deliverMessage).when(managerSpy).deliverMessage(any());
		
		when(approval.getRequirementId()).thenReturn(requirementId);
		when(approval.getId()).thenReturn(approvalId);
		when(recipient.getId()).thenReturn(recipientId);
		when(mockRecipientProvider.getRecipient(any())).thenReturn(recipientId);
		when(mockUserManager.getUserInfo(any())).thenReturn(mockUser);
		when(mockNotificationDao.findForUpdate(any(), any(), any())).thenReturn(Optional.ofNullable(existingNotification));
		when(mockSendConditionProvider.canSend(any(), any(), any())).thenReturn(canSend);
		
		// Call under test 
		managerSpy.sendMessageIfNeeded(notificationType, approval);
		
		verify(managerSpy).getManagedAccessRequirement(requirementId);
		verify(managerSpy).getRecipientProvider(notificationType);
		verify(mockRecipientProvider).getRecipient(approval);
		verify(mockNotificationDao).findForUpdate(notificationType, requirementId, recipientId);
		verify(managerSpy).getSendConditionProvider(notificationType);
		verify(mockSendConditionProvider).canSend(notificationType, approval, existingNotification);
		verify(managerSpy).deliverMessage(mockUser);
		verify(managerSpy, never()).createMessageToUser(any(), any(), any(), any());
		
		ArgumentCaptor<DBODataAccessNotification> notificationCaptor = ArgumentCaptor.forClass(DBODataAccessNotification.class);
		
		verify(mockNotificationDao).create(notificationCaptor.capture());
		
		DBODataAccessNotification expectedNotification = new DBODataAccessNotification();
		
		expectedNotification.setNotificationType(notificationType.name());
		expectedNotification.setRequirementId(requirementId);
		expectedNotification.setRecipientId(recipientId);
		expectedNotification.setAccessApprovalId(approvalId);
		expectedNotification.setMessageId(AccessApprovalNotificationManager.NO_MESSAGE_TO_USER);
		// Align the timestamp
		expectedNotification.setSentOn(notificationCaptor.getValue().getSentOn());
		
		assertEquals(expectedNotification, notificationCaptor.getValue());
		
	}
	
	@Test
	public void testSendMessageIfNeededWithExisting() throws RecoverableMessageException {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.REVOCATION;
		AccessApproval approval = mockAccessApproval;
		UserInfo recipient = mockUser;
		ManagedACTAccessRequirement accessRequirement = mockManagedAccessRequirement;

		Long notificationId = 1234L;
		Long approvalId = 5L;
		Long requirementId = 1L;
		Long recipientId = 3L;
		Long messageId = 123L;
		Date createdOn = new Date();
		
		DBODataAccessNotification existingNotification = new DBODataAccessNotification();
		
		existingNotification.setId(notificationId);
		
		boolean canSend = true;
		boolean deliverMessage = true;
		
		// We use a spy to mock out already tested methods
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);
		
		doReturn(Optional.of(accessRequirement)).when(managerSpy).getManagedAccessRequirement(any());
		doReturn(mockRecipientProvider).when(managerSpy).getRecipientProvider(any());
		doReturn(mockSendConditionProvider).when(managerSpy).getSendConditionProvider(any());
		doReturn(deliverMessage).when(managerSpy).deliverMessage(any());
		doReturn(mockMessageToUser).when(managerSpy).createMessageToUser(any(), any(), any(), any());
		
		when(approval.getRequirementId()).thenReturn(requirementId);
		when(approval.getId()).thenReturn(approvalId);
		when(recipient.getId()).thenReturn(recipientId);
		when(mockMessageToUser.getId()).thenReturn(messageId.toString());
		when(mockMessageToUser.getCreatedOn()).thenReturn(createdOn);
		when(mockRecipientProvider.getRecipient(any())).thenReturn(recipientId);
		when(mockUserManager.getUserInfo(any())).thenReturn(mockUser);
		when(mockNotificationDao.findForUpdate(any(), any(), any())).thenReturn(Optional.ofNullable(existingNotification));
		when(mockSendConditionProvider.canSend(any(), any(), any())).thenReturn(canSend);
		
		// Call under test 
		managerSpy.sendMessageIfNeeded(notificationType, approval);
		
		verify(managerSpy).getManagedAccessRequirement(requirementId);
		verify(managerSpy).getRecipientProvider(notificationType);
		verify(mockRecipientProvider).getRecipient(approval);
		verify(mockNotificationDao).findForUpdate(notificationType, requirementId, recipientId);
		verify(managerSpy).getSendConditionProvider(notificationType);
		verify(mockSendConditionProvider).canSend(notificationType, approval, existingNotification);
		verify(managerSpy).deliverMessage(mockUser);
		verify(managerSpy).createMessageToUser(notificationType, approval, accessRequirement, recipient);
		
		DBODataAccessNotification expectedNotification = new DBODataAccessNotification();
		
		expectedNotification.setId(notificationId);
		expectedNotification.setNotificationType(notificationType.name());
		expectedNotification.setRequirementId(requirementId);
		expectedNotification.setRecipientId(recipientId);
		expectedNotification.setAccessApprovalId(approvalId);
		expectedNotification.setMessageId(messageId);
		expectedNotification.setSentOn(new Timestamp(createdOn.getTime()));
		
		verify(mockNotificationDao).update(notificationId, expectedNotification);
	}
	
	@Test
	public void testSendMessageIfNeededAndDontSend() throws RecoverableMessageException {
		DataAccessNotificationType notificationType = DataAccessNotificationType.REVOCATION;
		AccessApproval approval = mockAccessApproval;
		UserInfo recipient = mockUser;
		DBODataAccessNotification existingNotification = null;
		ManagedACTAccessRequirement accessRequirement = mockManagedAccessRequirement;
		
		Long approvalId = 5L;
		Long requirementId = 1L;
		Long recipientId = 3L;
		
		boolean canSend = false;
		
		// We use a spy to mock out already tested methods
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);
		
		doReturn(Optional.of(accessRequirement)).when(managerSpy).getManagedAccessRequirement(any());
		doReturn(mockRecipientProvider).when(managerSpy).getRecipientProvider(any());
		doReturn(mockSendConditionProvider).when(managerSpy).getSendConditionProvider(any());
		
		when(approval.getRequirementId()).thenReturn(requirementId);
		when(approval.getId()).thenReturn(approvalId);
		when(recipient.getId()).thenReturn(recipientId);
		when(mockRecipientProvider.getRecipient(any())).thenReturn(recipientId);
		when(mockUserManager.getUserInfo(any())).thenReturn(mockUser);
		when(mockNotificationDao.findForUpdate(any(), any(), any())).thenReturn(Optional.ofNullable(existingNotification));
		when(mockSendConditionProvider.canSend(any(), any(), any())).thenReturn(canSend);
		
		// Call under test 
		managerSpy.sendMessageIfNeeded(notificationType, approval);
		
		verify(managerSpy).getManagedAccessRequirement(requirementId);
		verify(managerSpy).getRecipientProvider(notificationType);
		verify(mockRecipientProvider).getRecipient(approval);
		verify(mockNotificationDao).findForUpdate(notificationType, requirementId, recipientId);
		verify(managerSpy).getSendConditionProvider(notificationType);
		verify(mockSendConditionProvider).canSend(notificationType, approval, existingNotification);
		verify(managerSpy, never()).deliverMessage(any());
		verify(managerSpy, never()).createMessageToUser(any(), any(), any(), any());
		verifyNoMoreInteractions(mockNotificationDao);
	}
	
	@Test
	public void testIsSendRevocationWithWrongNotificationType() {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.FIRST_RENEWAL_REMINDER;
		AccessApproval approval = mockAccessApproval;
		DBODataAccessNotification existingNotification = mockNotification;
		
		String errorMessage = assertThrows(UnsupportedOperationException.class, () -> {			
			// Call under test
			manager.isSendRevocation(notificationType, approval, existingNotification);
		}).getMessage();
	
		assertEquals("Unsupported notification type " + notificationType, errorMessage);
	}
	
	@Test
	public void testIsSendRevocationWithNotExisting() {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.REVOCATION;
		AccessApproval approval = mockAccessApproval;
		DBODataAccessNotification existingNotification = null;
		
		Long requirementId = 1L;
		Long accessorId = 2L;
		
		List<Long> approvals = Collections.emptyList();
		
		when(approval.getAccessorId()).thenReturn(accessorId.toString());
		when(approval.getRequirementId()).thenReturn(requirementId);
		when(mockAccessApprovalDao.listApprovalsByAccessor(any(), any())).thenReturn(approvals);
		
		boolean expected = true;
		
		// Call under test
		boolean result = manager.isSendRevocation(notificationType, approval, existingNotification);
	
		assertEquals(expected, result);
		verify(mockAccessApprovalDao).listApprovalsByAccessor(requirementId.toString(), accessorId.toString());
	}
	
	@Test
	public void testIsSendRevocationWithOtherApprovals() {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.REVOCATION;
		AccessApproval approval = mockAccessApproval;
		DBODataAccessNotification existingNotification = null;
		
		Long requirementId = 1L;
		Long accessorId = 2L;
		
		List<Long> approvals = Arrays.asList(3L, 4L);
		
		when(approval.getAccessorId()).thenReturn(accessorId.toString());
		when(approval.getRequirementId()).thenReturn(requirementId);
		when(mockAccessApprovalDao.listApprovalsByAccessor(any(), any())).thenReturn(approvals);
		
		boolean expected = false;
		
		// Call under test
		boolean result = manager.isSendRevocation(notificationType, approval, existingNotification);
	
		assertEquals(expected, result);
		verify(mockAccessApprovalDao).listApprovalsByAccessor(requirementId.toString(), accessorId.toString());
	}
	
	@Test
	public void testIsSendRevocationWithModifiedOnBeforeSent() {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.REVOCATION;
		AccessApproval approval = mockAccessApproval;
		DBODataAccessNotification existingNotification = mockNotification;
		
		Long requirementId = 1L;
		Long accessorId = 2L;
		
		Instant sentOn = Instant.now();
		// Simulate an approval modified in the past minute
		Instant modifiedOn = Instant.now().minus(1, ChronoUnit.MINUTES);
		
		List<Long> approvals = Collections.emptyList();
		
		when(approval.getAccessorId()).thenReturn(accessorId.toString());
		when(approval.getRequirementId()).thenReturn(requirementId);
		when(mockAccessApprovalDao.listApprovalsByAccessor(any(), any())).thenReturn(approvals);
		when(existingNotification.getSentOn()).thenReturn(Timestamp.from(sentOn));
		when(approval.getModifiedOn()).thenReturn(Date.from(modifiedOn));
		
		boolean expected = false;
		
		// Call under test
		boolean result = manager.isSendRevocation(notificationType, approval, existingNotification);
	
		assertEquals(expected, result);
		verify(mockAccessApprovalDao).listApprovalsByAccessor(requirementId.toString(), accessorId.toString());
	}
	
	@Test
	public void testIsSendRevocationWithSentRecently() {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.REVOCATION;
		AccessApproval approval = mockAccessApproval;
		DBODataAccessNotification existingNotification = mockNotification;
		
		Long requirementId = 1L;
		Long accessorId = 2L;
		
		Instant sentOn = Instant.now();
		// Simulate an approval modified in the next hour
		Instant modifiedOn = Instant.now().plus(1, ChronoUnit.HOURS);
		
		List<Long> approvals = Collections.emptyList();
		
		when(approval.getAccessorId()).thenReturn(accessorId.toString());
		when(approval.getRequirementId()).thenReturn(requirementId);
		when(mockAccessApprovalDao.listApprovalsByAccessor(any(), any())).thenReturn(approvals);
		when(existingNotification.getSentOn()).thenReturn(Timestamp.from(sentOn));
		when(approval.getModifiedOn()).thenReturn(Date.from(modifiedOn));
		
		boolean expected = false;
		
		// Call under test
		boolean result = manager.isSendRevocation(notificationType, approval, existingNotification);
	
		assertEquals(expected, result);
		verify(mockAccessApprovalDao).listApprovalsByAccessor(requirementId.toString(), accessorId.toString());
	}
	
	@Test
	public void testIsSendRevocationWithModifiedOnAfterSentTimeout() {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.REVOCATION;
		AccessApproval approval = mockAccessApproval;
		DBODataAccessNotification existingNotification = mockNotification;
		
		Long requirementId = 1L;
		Long accessorId = 2L;
		
		Instant sentOn = Instant.now();
		// Simulate an approval modified after 7 days
		Instant modifiedOn = Instant.now().plus(AccessApprovalNotificationManager.RESEND_TIMEOUT_DAYS, ChronoUnit.DAYS);
		
		List<Long> approvals = Collections.emptyList();
		
		when(approval.getAccessorId()).thenReturn(accessorId.toString());
		when(approval.getRequirementId()).thenReturn(requirementId);
		when(mockAccessApprovalDao.listApprovalsByAccessor(any(), any())).thenReturn(approvals);
		when(existingNotification.getSentOn()).thenReturn(Timestamp.from(sentOn));
		when(approval.getModifiedOn()).thenReturn(Date.from(modifiedOn));
		
		boolean expected = true;
		
		// Call under test
		boolean result = manager.isSendRevocation(notificationType, approval, existingNotification);
	
		assertEquals(expected, result);
		verify(mockAccessApprovalDao).listApprovalsByAccessor(requirementId.toString(), accessorId.toString());
	}
	
	@Test
	public void testIsSendReminderWithNotExisting() {
		DataAccessNotificationType notificationType = DataAccessNotificationType.FIRST_RENEWAL_REMINDER;
		AccessApproval approval = mockAccessApproval;
		DBODataAccessNotification existingNotification = null;
		
		Long requirementId = 1L;
		Long submitterId = 2L;
		Instant expiredOn = LocalDate.now(ZoneOffset.UTC)
				.plus(notificationType.getReminderPeriod())
				.atStartOfDay()
				.toInstant(ZoneOffset.UTC);
		boolean hasApprovals = false;
		
		when(approval.getRequirementId()).thenReturn(requirementId);
		when(approval.getSubmitterId()).thenReturn(submitterId.toString());
		when(approval.getExpiredOn()).thenReturn(Date.from(expiredOn));
		when(mockAccessApprovalDao.hasSubmitterApproval(any(), any(), any())).thenReturn(hasApprovals);
		
		boolean expected = true;
		
		// Call under test
		boolean result = manager.isSendReminder(notificationType, approval, existingNotification);
		
		assertEquals(expected, result);
		verify(mockAccessApprovalDao).hasSubmitterApproval(requirementId.toString(), submitterId.toString(), expiredOn);
	}
	
	@Test
	public void testIsSendReminderWithOtherApprovals() {
		DataAccessNotificationType notificationType = DataAccessNotificationType.FIRST_RENEWAL_REMINDER;
		AccessApproval approval = mockAccessApproval;
		DBODataAccessNotification existingNotification = null;
		
		Long requirementId = 1L;
		Long submitterId = 2L;
		Instant expiredOn = LocalDate.now(ZoneOffset.UTC)
				.plus(notificationType.getReminderPeriod())
				.atStartOfDay()
				.toInstant(ZoneOffset.UTC);
		
		boolean hasApprovals = true;
		
		when(approval.getRequirementId()).thenReturn(requirementId);
		when(approval.getSubmitterId()).thenReturn(submitterId.toString());
		when(approval.getExpiredOn()).thenReturn(Date.from(expiredOn));
		when(mockAccessApprovalDao.hasSubmitterApproval(any(), any(), any())).thenReturn(hasApprovals);
		
		boolean expected = false;
		
		// Call under test
		boolean result = manager.isSendReminder(notificationType, approval, existingNotification);
		
		assertEquals(expected, result);
		verify(mockAccessApprovalDao).hasSubmitterApproval(requirementId.toString(), submitterId.toString(), expiredOn);
	}
	
	@Test
	public void testIsSendReminderWithExistingSentRecently() {
		DataAccessNotificationType notificationType = DataAccessNotificationType.FIRST_RENEWAL_REMINDER;
		AccessApproval approval = mockAccessApproval;
		DBODataAccessNotification existingNotification = mockNotification;
		
		Long requirementId = 1L;
		Long submitterId = 2L;
		
		Instant expiredOn = LocalDate.now(ZoneOffset.UTC)
				.plus(notificationType.getReminderPeriod())
				.atStartOfDay()
				.toInstant(ZoneOffset.UTC);
		
		// Sent just one day ago
		Instant sentOn = Instant.now().minus(1, ChronoUnit.DAYS);
		
		boolean hasApprovals = false;
		
		when(existingNotification.getSentOn()).thenReturn(Timestamp.from(sentOn));
		when(approval.getRequirementId()).thenReturn(requirementId);
		when(approval.getSubmitterId()).thenReturn(submitterId.toString());
		when(approval.getExpiredOn()).thenReturn(Date.from(expiredOn));
		when(mockAccessApprovalDao.hasSubmitterApproval(any(), any(), any())).thenReturn(hasApprovals);
		
		boolean expected = false;
		
		// Call under test
		boolean result = manager.isSendReminder(notificationType, approval, existingNotification);
		
		assertEquals(expected, result);
		verify(mockAccessApprovalDao).hasSubmitterApproval(requirementId.toString(), submitterId.toString(), expiredOn);
	}
	
	@Test
	public void testIsSendReminderWithExistingSentTimeout() {
		DataAccessNotificationType notificationType = DataAccessNotificationType.FIRST_RENEWAL_REMINDER;
		AccessApproval approval = mockAccessApproval;
		DBODataAccessNotification existingNotification = mockNotification;
		
		Long requirementId = 1L;
		Long submitterId = 2L;
		
		Instant expiredOn = LocalDate.now(ZoneOffset.UTC)
				.plus(notificationType.getReminderPeriod())
				.atStartOfDay()
				.toInstant(ZoneOffset.UTC);
		
		// Was sent but timed out
		Instant sentOn = Instant.now().minus(AccessApprovalNotificationManager.RESEND_TIMEOUT_DAYS, ChronoUnit.DAYS);
		
		boolean hasApprovals = false;
		
		when(existingNotification.getSentOn()).thenReturn(Timestamp.from(sentOn));
		when(approval.getRequirementId()).thenReturn(requirementId);
		when(approval.getSubmitterId()).thenReturn(submitterId.toString());
		when(approval.getExpiredOn()).thenReturn(Date.from(expiredOn));
		when(mockAccessApprovalDao.hasSubmitterApproval(any(), any(), any())).thenReturn(hasApprovals);
		
		boolean expected = true;
		
		// Call under test
		boolean result = manager.isSendReminder(notificationType, approval, existingNotification);
		
		assertEquals(expected, result);
		verify(mockAccessApprovalDao).hasSubmitterApproval(requirementId.toString(), submitterId.toString(), expiredOn);
	}
	
	@Test
	public void testIsSendReminderWithTooLate() {
		DataAccessNotificationType notificationType = DataAccessNotificationType.FIRST_RENEWAL_REMINDER;
		AccessApproval approval = mockAccessApproval;
		DBODataAccessNotification existingNotification = null;
		
		// One second too late, should have been processed earlier
		Instant expiredOn = LocalDate.now(ZoneOffset.UTC)
				.plus(notificationType.getReminderPeriod())
				.atStartOfDay()
				.minusSeconds(1)
				.toInstant(ZoneOffset.UTC);
		
		when(approval.getExpiredOn()).thenReturn(Date.from(expiredOn));
		
		boolean expected = false;
		
		// Call under test
		boolean result = manager.isSendReminder(notificationType, approval, existingNotification);
		
		assertEquals(expected, result);
		verifyZeroInteractions(mockAccessApprovalDao);
	}
	
	@Test
	public void testIsSendReminderWithTooEarly() {
		DataAccessNotificationType notificationType = DataAccessNotificationType.FIRST_RENEWAL_REMINDER;
		AccessApproval approval = mockAccessApproval;
		DBODataAccessNotification existingNotification = null;
		
		// Too early of one day, should be processed afterwards
		Instant expiredOn = LocalDate.now(ZoneOffset.UTC)
				.plus(notificationType.getReminderPeriod())
				.atStartOfDay()
				.plus(1, ChronoUnit.DAYS)
				.toInstant(ZoneOffset.UTC);
		
		when(approval.getExpiredOn()).thenReturn(Date.from(expiredOn));
		
		boolean expected = false;
		
		// Call under test
		boolean result = manager.isSendReminder(notificationType, approval, existingNotification);
		
		assertEquals(expected, result);
		verifyZeroInteractions(mockAccessApprovalDao);
	}
	
	@Test
	public void testProcessAccessApprovalChange() throws RecoverableMessageException {
		
		Long approvalId = 2L;
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.REVOCATION;
		
		boolean discardChange = false;
		
		when(mockChangeMessage.getObjectId()).thenReturn(approvalId.toString());
		
		// We use a spy to mock internal calls that are already tested
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);
		
		doReturn(discardChange).when(managerSpy).discardChangeMessage(any());
		doNothing().when(managerSpy).processAccessApproval(any(), any());
		
		// Call under test
		managerSpy.processAccessApprovalChange(mockChangeMessage);
		
		verify(managerSpy).discardChangeMessage(mockChangeMessage);
		verify(managerSpy).processAccessApproval(notificationType, approvalId);
	}
	
	@Test
	public void testProcessAccessApprovalChangeWithDiscardChange() throws RecoverableMessageException {
		
		boolean discardChange = true;
		
		// We use a spy to mock internal calls that are already tested
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);
		
		doReturn(discardChange).when(managerSpy).discardChangeMessage(any());
		
		// Call under test
		managerSpy.processAccessApprovalChange(mockChangeMessage);
		
		verify(managerSpy).discardChangeMessage(mockChangeMessage);
		verify(managerSpy, never()).sendMessageIfNeeded(any(), any());
	}
	
	@Test
	public void testProcessAccessApproval() throws RecoverableMessageException {
		
		AccessApproval approval = mockAccessApproval;
		DataAccessNotificationType notificationType = DataAccessNotificationType.REVOCATION;
		Long approvalId = 2L;
		ApprovalState state = ApprovalState.REVOKED;
		boolean featureEnabled = true;
		
		when(approval.getState()).thenReturn(state);
		when(mockFeatureManager.isFeatureEnabled(any())).thenReturn(featureEnabled);
		when(mockAccessApprovalDao.get(any())).thenReturn(approval);
		
		// We use a spy to mock internal calls that are already tested
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);
		
		doNothing().when(managerSpy).sendMessageIfNeeded(any(), any());

		// Call under test
		managerSpy.processAccessApproval(notificationType, approvalId);
		
		verify(mockFeatureManager).isFeatureEnabled(Feature.DATA_ACCESS_NOTIFICATIONS);
		verify(mockAccessApprovalDao).get(approvalId.toString());
		verify(managerSpy).sendMessageIfNeeded(notificationType, approval);
	}
	
	@Test
	public void testProcessAccessApprovalWithFeatureDisabled() throws RecoverableMessageException {

		DataAccessNotificationType notificationType = DataAccessNotificationType.REVOCATION;
		Long approvalId = 2L;
		boolean featureEnabled = false;
		
		when(mockFeatureManager.isFeatureEnabled(any())).thenReturn(featureEnabled);
		
		// We use a spy to mock internal calls that are already tested
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);

		// Call under test
		managerSpy.processAccessApproval(notificationType, approvalId);
		
		verify(mockFeatureManager).isFeatureEnabled(Feature.DATA_ACCESS_NOTIFICATIONS);
		verifyZeroInteractions(mockAccessApprovalDao);
		verify(managerSpy, never()).sendMessageIfNeeded(any(), any());
	}
	
	@Test
	public void testProcessAccessApprovalWithUnexpectedState() throws RecoverableMessageException {
		
		AccessApproval approval = mockAccessApproval;
		DataAccessNotificationType notificationType = DataAccessNotificationType.REVOCATION;
		Long approvalId = 2L;
		ApprovalState state = ApprovalState.APPROVED;
		boolean featureEnabled = true;
		
		when(approval.getState()).thenReturn(state);
		when(mockFeatureManager.isFeatureEnabled(any())).thenReturn(featureEnabled);
		when(mockAccessApprovalDao.get(any())).thenReturn(approval);
		
		// We use a spy to mock internal calls that are already tested
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);
		
		// Call under test
		managerSpy.processAccessApproval(notificationType, approvalId);
		
		verify(mockFeatureManager).isFeatureEnabled(Feature.DATA_ACCESS_NOTIFICATIONS);
		verify(mockAccessApprovalDao).get(approvalId.toString());
		verify(managerSpy, never()).sendMessageIfNeeded(any(), any());
	}
	
	@Test
	public void testGetManagedAccessRequirement() {
		
		ManagedACTAccessRequirement accessRequirement = mockManagedAccessRequirement;
		ACCESS_TYPE accessType = ACCESS_TYPE.DOWNLOAD;
		Long requirementId = 1L;
		
		when(accessRequirement.getAccessType()).thenReturn(accessType);
		when(mockAccessRequirementDao.get(any())).thenReturn(accessRequirement);
		
		// Call under test
		Optional<ManagedACTAccessRequirement> result = manager.getManagedAccessRequirement(requirementId);
		
		assertTrue(result.isPresent());
		assertEquals(accessRequirement, result.get());
		
		verify(mockAccessRequirementDao).get(requirementId.toString());
		verify(accessRequirement).getAccessType();
	}
	
	@Test
	public void testGetManagedAccessRequirementWithNotManaged() {
		
		AccessRequirement accessRequirement = mockAccessRequirement;
		Long requirementId = 1L;

		when(mockAccessRequirementDao.get(any())).thenReturn(accessRequirement);
		
		// Call under test
		Optional<ManagedACTAccessRequirement> result = manager.getManagedAccessRequirement(requirementId);
		
		assertFalse(result.isPresent());
		
		verify(mockAccessRequirementDao).get(requirementId.toString());
		verifyZeroInteractions(accessRequirement);
	}
	
	@Test
	public void testGetManagedAccessRequirementWithDifferentAccessType() {
		
		ManagedACTAccessRequirement accessRequirement = mockManagedAccessRequirement;
		ACCESS_TYPE accessType = ACCESS_TYPE.PARTICIPATE;
		Long requirementId = 1L;
		
		when(accessRequirement.getAccessType()).thenReturn(accessType);
		when(mockAccessRequirementDao.get(any())).thenReturn(accessRequirement);
		
		// Call under test
		Optional<ManagedACTAccessRequirement> result = manager.getManagedAccessRequirement(requirementId);
		
		assertFalse(result.isPresent());
		
		verify(mockAccessRequirementDao).get(requirementId.toString());
		verify(accessRequirement).getAccessType();
	}
	
	@Test
	public void testGetRecipientProviderForRevocation() {
		DataAccessNotificationType notificationType = DataAccessNotificationType.REVOCATION;
		AccessApproval approval = mockAccessApproval;
		
		Long accessorId = 1L;
		
		when(approval.getAccessorId()).thenReturn(accessorId.toString());

		// Call under test
		RecipientProvider provider = manager.getRecipientProvider(notificationType);

		assertEquals(accessorId, provider.getRecipient(mockAccessApproval));
		
		verify(mockAccessApproval).getAccessorId();
		verifyNoMoreInteractions(mockAccessApproval);
	}
	
	@Test
	public void testGetRecipientProviderForReminder() {
		
		AccessApproval approval = mockAccessApproval;
		
		Long submitterId = 1L;
		
		when(approval.getSubmitterId()).thenReturn(submitterId.toString());
		
		int times = 0;
		
		for (DataAccessNotificationType notificationType : DataAccessNotificationType.values()) {
		
			if (!notificationType.isReminder()) {
				continue;
			}
			
			// Call under test
			RecipientProvider provider = manager.getRecipientProvider(notificationType);

			assertEquals(submitterId, provider.getRecipient(mockAccessApproval));
			
			verify(mockAccessApproval, Mockito.times(++times)).getSubmitterId();
		}

		verifyNoMoreInteractions(mockAccessApproval);
	}
	
	@Test
	public void testGetSendConditionProviderForRevocation() {
		DataAccessNotificationType notificationType = DataAccessNotificationType.REVOCATION;
		AccessApproval approval = mockAccessApproval;
		DBODataAccessNotification notification = mockNotification;
		
		boolean canSend = false;
		
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);
		
		doReturn(canSend).when(managerSpy).isSendRevocation(any(), any(), any());
		
		// Call under test
		SendConditionProvider provider = managerSpy.getSendConditionProvider(notificationType);
		
		// If we invoke the provider then the correct manager function should be invoked
		provider.canSend(notificationType, approval, notification);
		
		verify(managerSpy).isSendRevocation(notificationType, approval, notification);
	}
	
	@Test
	public void testGetSendConditionProviderForReminder() {
		
		AccessApproval approval = mockAccessApproval;
		DBODataAccessNotification notification = mockNotification;
		boolean canSend = true;
		
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);
		
		doReturn(canSend).when(managerSpy).isSendReminder(any(), any(), any());
		
		for (DataAccessNotificationType notificationType : DataAccessNotificationType.values()) {
			
			if (!notificationType.isReminder()) {
				continue;
			}
			
			// Call under test
			SendConditionProvider provider = managerSpy.getSendConditionProvider(notificationType);
			
			// If we invoke the provider then the correct manager function should be invoked
			provider.canSend(notificationType, approval, notification);
			
			verify(managerSpy).isSendReminder(notificationType, approval, notification);
		}
	}
	
}
