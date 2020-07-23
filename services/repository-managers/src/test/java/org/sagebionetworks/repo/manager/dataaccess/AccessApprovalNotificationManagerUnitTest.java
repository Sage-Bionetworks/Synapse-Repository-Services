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
import org.sagebionetworks.repo.manager.dataaccess.AccessApprovalNotificationManagerImpl.ReSendCondition;
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
	private ReSendCondition mockResendCondition;
	
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
	public void testGetRecipientForRevocation() {
		
		String accessorId = "1";
		
		when(mockAccessApproval.getAccessorId()).thenReturn(accessorId);
		when(mockUserManager.getUserInfo(anyLong())).thenReturn(mockUser);
		
		// Call under test
		UserInfo user = manager.getRecipientForRevocation(mockAccessApproval);
		
		assertEquals(mockUser, user);
		verify(mockAccessApproval).getAccessorId();
		verify(mockUserManager).getUserInfo(Long.valueOf(accessorId));
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
		Long requirementId = 1L;
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
		doReturn(Optional.of(accessRequirement)).when(managerSpy).getManagedAccessRequirement(any());

		when(mockNotificationBuilder.getMimeType()).thenReturn(mimeType);
		when(mockNotificationBuilder.buildMessageBody(any(), any(), any())).thenReturn(messageBody);
		when(mockNotificationBuilder.buildSubject(any(), any(), any())).thenReturn(subject);
		
		when(recipient.getId()).thenReturn(recipientId);
		when(sender.getId()).thenReturn(senderId);
		when(approval.getRequirementId()).thenReturn(requirementId);
		
		when(mockMessageManager.createMessage(any(), any(), anyBoolean())).thenReturn(mockMessageToUser);
		
		// Call under test
		MessageToUser result = managerSpy.createMessageToUser(notificationType, approval, recipient);
		
		assertEquals(mockMessageToUser, result);

		verify(managerSpy).getNotificationBuilder(notificationType);
		verify(managerSpy).getManagedAccessRequirement(requirementId);
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
	public void testCreateMessageToUserWithNonManagedAccessRequirement() throws Exception {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.REVOCATION;
		AccessApproval approval = mockAccessApproval;
		AccessRequirement accessRequirement = null;
		UserInfo recipient = mockUser;
		
		// We use a spy to mock out already tested methods
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);

		doReturn(mockNotificationBuilder).when(managerSpy).getNotificationBuilder(any());
		doReturn(Optional.ofNullable(accessRequirement)).when(managerSpy).getManagedAccessRequirement(any());
		
		String message = assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			managerSpy.createMessageToUser(notificationType, approval, recipient);
		}).getMessage();

		assertEquals("Cannot send a notification for a non managed access requirement.", message);
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
	public void testDiscardAccessApproval() {
		
		ManagedACTAccessRequirement accessRequirement = mockManagedAccessRequirement;
		AccessApproval approval = mockAccessApproval;
		ApprovalState state = ApprovalState.REVOKED;
		Long requirementId = 1L;
		
		when(approval.getState()).thenReturn(state);
		when(approval.getRequirementId()).thenReturn(requirementId);
		
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);
		
		doReturn(Optional.ofNullable(accessRequirement)).when(managerSpy).getManagedAccessRequirement(any());
		
		boolean expected = false;
		
		// Call under test
		boolean result = managerSpy.discardAccessApproval(approval, state);
		
		assertEquals(expected, result);
		
		verify(approval).getRequirementId();
		verify(approval).getState();
		verify(managerSpy).getManagedAccessRequirement(requirementId);
	}
	
	@Test
	public void testDiscardAccessApprovalWithDifferentState() {
		
		AccessApproval approval = mockAccessApproval;
		ApprovalState state = ApprovalState.REVOKED;
		
		when(approval.getState()).thenReturn(state);
		
		boolean expected = true;
		
		// Call under test
		boolean result = manager.discardAccessApproval(approval, ApprovalState.APPROVED);
		
		assertEquals(expected, result);
		
		verify(approval).getState();
	}
	
	@Test
	public void testDiscardAccessApprovalWithUnsupportedAccessRequirementType() {
		
		ManagedACTAccessRequirement accessRequirement = null;
		AccessApproval approval = mockAccessApproval;
		ApprovalState state = ApprovalState.REVOKED;
		Long requirementId = 1L;
		
		// We spy the manager to mock out already tested function
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);
		
		when(approval.getState()).thenReturn(state);
		when(approval.getRequirementId()).thenReturn(requirementId);
		doReturn(Optional.ofNullable(accessRequirement)).when(managerSpy).getManagedAccessRequirement(any());
		
		boolean expected = true;
		
		// Call under test
		boolean result = managerSpy.discardAccessApproval(approval, state);
		
		assertEquals(expected, result);
		
		verify(approval).getRequirementId();
		verify(approval).getState();
		verify(managerSpy).getManagedAccessRequirement(requirementId);
	}
	
	@Test
	public void testSendMessageIfNeeded() throws RecoverableMessageException {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.REVOCATION;
		AccessApproval approval = mockAccessApproval;
		UserInfo recipient = mockUser;
		DBODataAccessNotification existingNotification = null;
		
		Long approvalId = 5L;
		Long requirementId = 1L;
		Long recipientId = 3L;
		Long messageId = 123L;
		Date createdOn = new Date();
		
		boolean deliverMessage = true;
		
		// We use a spy to mock out already tested methods
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);
		
		doReturn(deliverMessage).when(managerSpy).deliverMessage(any());
		doReturn(mockMessageToUser).when(managerSpy).createMessageToUser(any(), any(), any());
		
		when(approval.getRequirementId()).thenReturn(requirementId);
		when(approval.getId()).thenReturn(approvalId);
		when(recipient.getId()).thenReturn(recipientId);
		when(mockMessageToUser.getId()).thenReturn(messageId.toString());
		when(mockMessageToUser.getCreatedOn()).thenReturn(createdOn);
		
		when(mockNotificationDao.findForUpdate(any(), any(), any())).thenReturn(Optional.ofNullable(existingNotification));
		
		// Call under test 
		managerSpy.sendMessageIfNeeded(notificationType, approval, recipient, mockResendCondition);
		
		verify(approval).getRequirementId();
		verify(approval).getId();
		verify(recipient).getId();
		verify(mockNotificationDao).findForUpdate(notificationType, requirementId, recipientId);
		// The notification was not present
		verifyZeroInteractions(mockResendCondition);
		verify(managerSpy).createMessageToUser(notificationType, approval, recipient);
		
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
		
		Long approvalId = 5L;
		Long requirementId = 1L;
		Long recipientId = 3L;
		
		boolean deliverMessage = false;
		
		// We use a spy to mock out already tested methods
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);
		
		doReturn(deliverMessage).when(managerSpy).deliverMessage(any());
		
		when(approval.getRequirementId()).thenReturn(requirementId);
		when(approval.getId()).thenReturn(approvalId);
		when(recipient.getId()).thenReturn(recipientId);
		
		when(mockNotificationDao.findForUpdate(any(), any(), any())).thenReturn(Optional.ofNullable(existingNotification));
		
		// Call under test 
		managerSpy.sendMessageIfNeeded(notificationType, approval, recipient, mockResendCondition);
		
		verify(approval).getRequirementId();
		verify(approval).getId();
		verify(recipient).getId();
		verify(mockNotificationDao).findForUpdate(notificationType, requirementId, recipientId);
		// The notification was not present
		verifyZeroInteractions(mockResendCondition);
		
		verify(managerSpy, never()).createMessageToUser(notificationType, approval, recipient);
		
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
		
		Long notificationId = 1234L;
		Long approvalId = 5L;
		Long requirementId = 1L;
		Long recipientId = 3L;
		Long messageId = 123L;
		Date createdOn = new Date();
		
		DBODataAccessNotification existingNotification = new DBODataAccessNotification();
		
		existingNotification.setId(notificationId);
		
		boolean deliverMessage = true;
		boolean canResend = true;
		
		// We use a spy to mock out already tested methods
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);
		
		doReturn(deliverMessage).when(managerSpy).deliverMessage(any());
		doReturn(mockMessageToUser).when(managerSpy).createMessageToUser(any(), any(), any());
		
		when(approval.getRequirementId()).thenReturn(requirementId);
		when(approval.getId()).thenReturn(approvalId);
		when(recipient.getId()).thenReturn(recipientId);
		
		when(mockMessageToUser.getId()).thenReturn(messageId.toString());
		when(mockMessageToUser.getCreatedOn()).thenReturn(createdOn);
		
		when(mockNotificationDao.findForUpdate(any(), any(), any())).thenReturn(Optional.ofNullable(existingNotification));
		when(mockResendCondition.canSend(any(), any())).thenReturn(canResend);
		
		// Call under test 
		managerSpy.sendMessageIfNeeded(notificationType, approval, recipient, mockResendCondition);
		
		verify(approval).getRequirementId();
		verify(approval).getId();
		verify(recipient).getId();
		verify(mockNotificationDao).findForUpdate(notificationType, requirementId, recipientId);
		// The notification was present, check that the condition was verified
		verify(mockResendCondition).canSend(existingNotification, approval);
		verify(managerSpy).createMessageToUser(notificationType, approval, recipient);
		
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
	public void testSendMessageIfNeededWithExistingAndDontSend() throws RecoverableMessageException {
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.REVOCATION;
		AccessApproval approval = mockAccessApproval;
		UserInfo recipient = mockUser;
		
		Long notificationId = 1234L;
		Long approvalId = 5L;
		Long requirementId = 1L;
		Long recipientId = 3L;
		
		DBODataAccessNotification existingNotification = new DBODataAccessNotification();
		
		existingNotification.setId(notificationId);
		
		boolean canResend = false;
		
		// We use a spy to mock out already tested methods
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);
		
		when(approval.getRequirementId()).thenReturn(requirementId);
		when(approval.getId()).thenReturn(approvalId);
		when(recipient.getId()).thenReturn(recipientId);
		
		when(mockNotificationDao.findForUpdate(any(), any(), any())).thenReturn(Optional.ofNullable(existingNotification));
		when(mockResendCondition.canSend(any(), any())).thenReturn(canResend);
		
		// Call under test 
		managerSpy.sendMessageIfNeeded(notificationType, approval, recipient, mockResendCondition);
		
		verify(approval).getRequirementId();
		verify(approval).getId();
		verify(recipient).getId();
		verify(mockNotificationDao).findForUpdate(notificationType, requirementId, recipientId);
		// The notification was present, check that the condition was verified
		verify(mockResendCondition).canSend(existingNotification, approval);
		verify(managerSpy, never()).createMessageToUser(any(), any(), any());
		verifyNoMoreInteractions(mockNotificationDao);
	}
	
	@Test
	public void testIsSendRevocationWithModifiedOnBeforeSent() {
		
		AccessApproval approval = mockAccessApproval;
		DBODataAccessNotification existingNotification = mockNotification;
		
		Instant sentOn = Instant.now();
		// Simulate an approval modified in the past minute
		Instant modifiedOn = Instant.now().minus(1, ChronoUnit.MINUTES);
		
		when(existingNotification.getSentOn()).thenReturn(Timestamp.from(sentOn));
		when(approval.getModifiedOn()).thenReturn(Date.from(modifiedOn));
		
		boolean expected = false;
		
		// Call under test
		boolean result = manager.isSendRevocation(existingNotification, approval);
	
		assertEquals(expected, result);
	}
	
	@Test
	public void testIsSendRevocationWithModifiedOnAfterSent() {
		
		AccessApproval approval = mockAccessApproval;
		DBODataAccessNotification existingNotification = mockNotification;
		
		Instant sentOn = Instant.now();
		// Simulate an approval modified in the next hour
		Instant modifiedOn = Instant.now().plus(1, ChronoUnit.HOURS);
		
		when(existingNotification.getSentOn()).thenReturn(Timestamp.from(sentOn));
		when(approval.getModifiedOn()).thenReturn(Date.from(modifiedOn));
		
		boolean expected = false;
		
		// Call under test
		boolean result = manager.isSendRevocation(existingNotification, approval);
	
		assertEquals(expected, result);
	}
	
	@Test
	public void testIsSendRevocationWithModifiedOnAfterSentTimeout() {
		
		AccessApproval approval = mockAccessApproval;
		DBODataAccessNotification existingNotification = mockNotification;
		
		Instant sentOn = Instant.now();
		// Simulate an approval modified after 7 days
		Instant modifiedOn = Instant.now().plus(AccessApprovalNotificationManager.REVOKE_RESEND_TIMEOUT_DAYS, ChronoUnit.DAYS);
		
		when(existingNotification.getSentOn()).thenReturn(Timestamp.from(sentOn));
		when(approval.getModifiedOn()).thenReturn(Date.from(modifiedOn));
		
		boolean expected = true;
		
		// Call under test
		boolean result = manager.isSendRevocation(existingNotification, approval);
	
		assertEquals(expected, result);
	}
	
	@Test
	public void testProcessAccessApprovalChange() throws RecoverableMessageException {
		
		Long requirementId = 1L;
		Long approvalId = 2L;
		Long recipientId = 5L;
		
		DataAccessNotificationType notificationType = DataAccessNotificationType.REVOCATION;
		AccessApproval approval = mockAccessApproval;
		UserInfo recipient = mockUser;
		
		boolean featureEnabled = true;
		boolean discardChange = false;
		boolean discardApproval = false;
		
		
		List<Long> userApprovals = Collections.emptyList();
		
		when(approval.getRequirementId()).thenReturn(requirementId);
		when(recipient.getId()).thenReturn(recipientId);
		
		when(mockChangeMessage.getObjectId()).thenReturn(approvalId.toString());
		when(mockFeatureManager.isFeatureEnabled(any())).thenReturn(featureEnabled);
		when(mockAccessApprovalDao.get(any())).thenReturn(approval);
		when(mockAccessApprovalDao.listApprovalsByAccessor(any(), any())).thenReturn(userApprovals);

		// We use a spy to mock internal calls that are already tested
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);
		
		doReturn(discardChange).when(managerSpy).discardChangeMessage(any());
		doReturn(discardApproval).when(managerSpy).discardAccessApproval(any(), any());
		doReturn(recipient).when(managerSpy).getRecipientForRevocation(any());
		doNothing().when(managerSpy).sendMessageIfNeeded(any(), any(), any(), any());
		
		
		// Call under test
		managerSpy.processAccessApprovalChange(mockChangeMessage);
		
		verify(mockFeatureManager).isFeatureEnabled(Feature.DATA_ACCESS_NOTIFICATIONS);
		verify(managerSpy).discardChangeMessage(mockChangeMessage);
		verify(mockAccessApprovalDao).get(approvalId.toString());
		verify(managerSpy).discardAccessApproval(approval, ApprovalState.REVOKED);
		verify(managerSpy).getRecipientForRevocation(approval);
		verify(mockAccessApprovalDao).listApprovalsByAccessor(requirementId.toString(), recipientId.toString());
		
		ArgumentCaptor<ReSendCondition> lambdaCaptor = ArgumentCaptor.forClass(ReSendCondition.class);
		
		verify(managerSpy).sendMessageIfNeeded(eq(notificationType), eq(approval), eq(recipient), lambdaCaptor.capture());
		
		// Verify that the correct function will be invoked
		Instant sentOn = Instant.now();
		Instant modifiedOn = Instant.now().minus(1, ChronoUnit.HOURS);
		
		when(mockNotification.getSentOn()).thenReturn(Timestamp.from(sentOn));
		when(approval.getModifiedOn()).thenReturn(Date.from(modifiedOn));
		
		boolean expected = false;
		boolean result = lambdaCaptor.getValue().canSend(mockNotification, approval);
		
		assertEquals(expected, result);
		
		verify(mockNotification).getSentOn();
		verify(approval).getModifiedOn();
		
	}
	
	@Test
	public void testProcessAccessApprovalChangeWithFeatureDisabled() throws RecoverableMessageException {
		
		boolean featureEnabled = false;
		
		when(mockFeatureManager.isFeatureEnabled(any())).thenReturn(featureEnabled);
		
		// Call under test
		manager.processAccessApprovalChange(mockChangeMessage);
		
		verify(mockFeatureManager).isFeatureEnabled(Feature.DATA_ACCESS_NOTIFICATIONS);
		verifyZeroInteractions(mockAccessApprovalDao);
		
	}
	
	@Test
	public void testProcessAccessApprovalChangeWithDiscardChange() throws RecoverableMessageException {
		
		boolean featureEnabled = true;
		boolean discardChange = true;
		
		when(mockFeatureManager.isFeatureEnabled(any())).thenReturn(featureEnabled);
		
		// We use a spy to mock internal calls that are already tested
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);
		
		doReturn(discardChange).when(managerSpy).discardChangeMessage(any());
		
		// Call under test
		managerSpy.processAccessApprovalChange(mockChangeMessage);
		
		verify(mockFeatureManager).isFeatureEnabled(Feature.DATA_ACCESS_NOTIFICATIONS);
		verify(managerSpy).discardChangeMessage(mockChangeMessage);
		verifyZeroInteractions(mockAccessApprovalDao);
		
	}
	
	@Test
	public void testProcessAccessApprovalChangeWithDiscardApproval() throws RecoverableMessageException {
		
		Long approvalId = 2L;
		
		AccessApproval approval = mockAccessApproval;
		
		boolean featureEnabled = true;
		boolean discardChange = false;
		boolean discardApproval = true;
		
		when(mockChangeMessage.getObjectId()).thenReturn(approvalId.toString());
		when(mockFeatureManager.isFeatureEnabled(any())).thenReturn(featureEnabled);
		when(mockAccessApprovalDao.get(any())).thenReturn(approval);

		// We use a spy to mock internal calls that are already tested
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);
		
		doReturn(discardChange).when(managerSpy).discardChangeMessage(any());
		doReturn(discardApproval).when(managerSpy).discardAccessApproval(any(), any());
		
		// Call under test
		managerSpy.processAccessApprovalChange(mockChangeMessage);
		
		verify(mockFeatureManager).isFeatureEnabled(Feature.DATA_ACCESS_NOTIFICATIONS);
		verify(managerSpy).discardChangeMessage(mockChangeMessage);
		verify(mockAccessApprovalDao).get(approvalId.toString());
		verify(managerSpy).discardAccessApproval(approval, ApprovalState.REVOKED);
		
		verifyNoMoreInteractions(mockAccessApprovalDao);
		verify(managerSpy, never()).getRecipientForRevocation(any());
		verify(managerSpy, never()).sendMessageIfNeeded(any(), any(), any(), any());
		
	}
	
	@Test
	public void testProcessAccessApprovalChangeWithOtherApprovals() throws RecoverableMessageException {
		
		Long requirementId = 1L;
		Long approvalId = 2L;
		Long recipientId = 5L;
		
		AccessApproval approval = mockAccessApproval;
		UserInfo recipient = mockUser;
		
		boolean featureEnabled = true;
		boolean discardChange = false;
		boolean discardApproval = false;
		
		
		List<Long> userApprovals = Collections.singletonList(1L);
		
		when(approval.getRequirementId()).thenReturn(requirementId);
		when(recipient.getId()).thenReturn(recipientId);
		
		when(mockChangeMessage.getObjectId()).thenReturn(approvalId.toString());
		when(mockFeatureManager.isFeatureEnabled(any())).thenReturn(featureEnabled);
		when(mockAccessApprovalDao.get(any())).thenReturn(approval);
		when(mockAccessApprovalDao.listApprovalsByAccessor(any(), any())).thenReturn(userApprovals);

		// We use a spy to mock internal calls that are already tested
		AccessApprovalNotificationManagerImpl managerSpy = Mockito.spy(manager);
		
		doReturn(discardChange).when(managerSpy).discardChangeMessage(any());
		doReturn(discardApproval).when(managerSpy).discardAccessApproval(any(), any());
		doReturn(recipient).when(managerSpy).getRecipientForRevocation(any());
		
		// Call under test
		managerSpy.processAccessApprovalChange(mockChangeMessage);
		
		verify(mockFeatureManager).isFeatureEnabled(Feature.DATA_ACCESS_NOTIFICATIONS);
		verify(managerSpy).discardChangeMessage(mockChangeMessage);
		verify(mockAccessApprovalDao).get(approvalId.toString());
		verify(managerSpy).discardAccessApproval(approval, ApprovalState.REVOKED);
		verify(managerSpy).getRecipientForRevocation(approval);
		verify(mockAccessApprovalDao).listApprovalsByAccessor(requirementId.toString(), recipientId.toString());
		
		verify(managerSpy, never()).sendMessageIfNeeded(any(), any(), any(), any());
		
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
}
