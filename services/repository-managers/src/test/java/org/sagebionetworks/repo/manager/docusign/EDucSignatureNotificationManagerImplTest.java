package org.sagebionetworks.repo.manager.docusign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_REALM_ID;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.manager.message.MessageTemplate;
import org.sagebionetworks.repo.manager.message.TemplatedMessageSender;
import org.sagebionetworks.repo.manager.stack.ProdDetector;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DBOEDucEnvelopeNotification;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.EDucEnvelopeNotificationDao;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.EDucEnvelopeToExamine;
import org.sagebionetworks.repo.model.educ.EDucStatusEnum;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@ExtendWith(MockitoExtension.class)
public class EDucSignatureNotificationManagerImplTest {

	@Mock
	private EDucEnvelopeNotificationDao mockNotificationDao;
	@Mock
	private TemplatedMessageSender mockTemplatedMessageSender;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private FeatureManager mockFeatureManager;
	@Mock
	private ProdDetector mockProdDetector;
	@Mock
	private StackConfiguration mockStackConfiguration;

	private EDucSignatureNotificationManagerImpl manager;

	private static final Long REQUEST_ID = 123L;
	private static final String ENVELOPE_ID = "env-1";
	private static final Long CREATED_BY = 100L;

	private EDucEnvelopeToExamine envelope;
	private UserInfo requester;
	private UserInfo sender;

	@BeforeEach
	public void before() {
		manager = new EDucSignatureNotificationManagerImpl(mockNotificationDao, mockTemplatedMessageSender,
				mockUserManager, mockFeatureManager, mockProdDetector, mockStackConfiguration);

		envelope = new EDucEnvelopeToExamine(REQUEST_ID, ENVELOPE_ID, CREATED_BY);
		requester = new UserInfo(false, CREATED_BY, DEFAULT_REALM_ID);
		sender = new UserInfo(false, BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId(),
				DEFAULT_REALM_ID);
	}

	// Stubs everything needed for a message to actually be sent.
	private void stubDeliverableSend(String messageId) {
		when(mockNotificationDao.findForUpdate(ENVELOPE_ID)).thenReturn(Optional.empty());
		when(mockUserManager.getUserInfo(CREATED_BY)).thenReturn(requester);
		when(mockFeatureManager.isUserInTestingGroup(requester)).thenReturn(false);
		when(mockProdDetector.isProductionStack()).thenReturn(Optional.of(true));
		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId()))
				.thenReturn(sender);
		when(mockStackConfiguration.getPortalBaseEndpoint()).thenReturn("https://www.synapse.org");
		MessageToUser message = new MessageToUser();
		message.setId(messageId);
		when(mockTemplatedMessageSender.sendMessage(any())).thenReturn(message);
	}

	@Test
	public void testProcessObservedStatusWithCompleted() {
		stubDeliverableSend("9001");

		// call under test
		manager.processObservedStatus(envelope, EDucStatusEnum.completed);

		MessageTemplate expected = MessageTemplate.builder().withSender(sender)
				.withRecipients(Collections.singleton("100"))
				.withTemplateFile("message/EDucSignatureCompletedTemplate.html.vtl")
				.withSubject("Your data use certificate has been signed")
				.withContext(Map.of("signatureUrl",
						"https://www.synapse.org/RequestHistory:request/123/signature"))
				.build();
		verify(mockTemplatedMessageSender).sendMessage(expected);
		verify(mockNotificationDao).create(REQUEST_ID, ENVELOPE_ID, "completed", 9001L);
	}

	@Test
	public void testProcessObservedStatusWithDeclined() {
		stubDeliverableSend("9002");

		// call under test
		manager.processObservedStatus(envelope, EDucStatusEnum.declined);

		MessageTemplate expected = MessageTemplate.builder().withSender(sender)
				.withRecipients(Collections.singleton("100"))
				.withTemplateFile("message/EDucSignatureDeclinedTemplate.html.vtl")
				.withSubject("Action needed: a signer declined your data use certificate")
				.withContext(Map.of("signatureUrl",
						"https://www.synapse.org/RequestHistory:request/123/signature"))
				.build();
		verify(mockTemplatedMessageSender).sendMessage(expected);
		verify(mockNotificationDao).create(REQUEST_ID, ENVELOPE_ID, "declined", 9002L);
	}

	// A requester who cancelled their own envelope is no longer referencing it, so a voided envelope seen
	// here is an expiry or an administrative void. It is recorded, which is what stops it being examined on
	// every pass, but nobody is emailed about it.
	@Test
	public void testProcessObservedStatusWithVoided() {
		when(mockNotificationDao.findForUpdate(ENVELOPE_ID)).thenReturn(Optional.empty());

		// call under test
		manager.processObservedStatus(envelope, EDucStatusEnum.voided);

		verifyNoInteractions(mockTemplatedMessageSender);
		verify(mockNotificationDao).create(REQUEST_ID, ENVELOPE_ID, "voided",
				DBOEDucEnvelopeNotification.NO_MESSAGE_SENT);
	}

	@Test
	public void testProcessObservedStatusWithStillInFlight() {
		// call under test — an envelope that can still change is left for the next pass
		manager.processObservedStatus(envelope, EDucStatusEnum.sent);

		verifyNoInteractions(mockTemplatedMessageSender);
		verify(mockNotificationDao, never()).create(any(), any(), any(), any());
		verify(mockNotificationDao, never()).findForUpdate(anyString());
	}

	@Test
	public void testProcessObservedStatusWithUnknownStatus() {
		// call under test — a status the signing service did not report is not a reason to record anything
		manager.processObservedStatus(envelope, null);

		verifyNoInteractions(mockTemplatedMessageSender);
		verify(mockNotificationDao, never()).create(any(), any(), any(), any());
	}

	// Two instances can examine the same envelope at once; the one that does not win the lock must not send a
	// second email.
	@Test
	public void testProcessObservedStatusWithAlreadyRecorded() {
		when(mockNotificationDao.findForUpdate(ENVELOPE_ID))
				.thenReturn(Optional.of(new DBOEDucEnvelopeNotification()));

		// call under test
		manager.processObservedStatus(envelope, EDucStatusEnum.completed);

		verifyNoInteractions(mockTemplatedMessageSender);
		verify(mockNotificationDao, never()).create(any(), any(), any(), any());
	}

	// Staging runs the same worker against the same data, so only production delivers. The terminal state is
	// still recorded, so the envelope is not re-examined forever.
	@Test
	public void testProcessObservedStatusWithNonProductionStack() {
		when(mockNotificationDao.findForUpdate(ENVELOPE_ID)).thenReturn(Optional.empty());
		when(mockUserManager.getUserInfo(CREATED_BY)).thenReturn(requester);
		when(mockFeatureManager.isUserInTestingGroup(requester)).thenReturn(false);
		when(mockProdDetector.isProductionStack()).thenReturn(Optional.of(false));

		// call under test
		manager.processObservedStatus(envelope, EDucStatusEnum.completed);

		verifyNoInteractions(mockTemplatedMessageSender);
		verify(mockNotificationDao).create(REQUEST_ID, ENVELOPE_ID, "completed",
				DBOEDucEnvelopeNotification.NO_MESSAGE_SENT);
	}

	@Test
	public void testProcessObservedStatusWithUndetectableStack() {
		when(mockNotificationDao.findForUpdate(ENVELOPE_ID)).thenReturn(Optional.empty());
		when(mockUserManager.getUserInfo(CREATED_BY)).thenReturn(requester);
		when(mockFeatureManager.isUserInTestingGroup(requester)).thenReturn(false);
		when(mockProdDetector.isProductionStack()).thenReturn(Optional.empty());

		// call under test — retried rather than recorded, so the notification is not lost
		assertThrows(RecoverableMessageException.class,
				() -> manager.processObservedStatus(envelope, EDucStatusEnum.completed));

		verify(mockNotificationDao, never()).create(any(), any(), any(), any());
	}

	@Test
	public void testProcessObservedStatusWithUserInTestingGroup() {
		when(mockNotificationDao.findForUpdate(ENVELOPE_ID)).thenReturn(Optional.empty());
		when(mockUserManager.getUserInfo(CREATED_BY)).thenReturn(requester);
		when(mockFeatureManager.isUserInTestingGroup(requester)).thenReturn(true);
		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId()))
				.thenReturn(sender);
		when(mockStackConfiguration.getPortalBaseEndpoint()).thenReturn("https://www.synapse.org");
		MessageToUser message = new MessageToUser();
		message.setId("9003");
		when(mockTemplatedMessageSender.sendMessage(any())).thenReturn(message);

		// call under test — a testing-group user is delivered to even outside production
		manager.processObservedStatus(envelope, EDucStatusEnum.completed);

		verify(mockTemplatedMessageSender).sendMessage(any());
		verify(mockProdDetector, never()).isProductionStack();
	}

	@Test
	public void testSignatureUrl() {
		when(mockStackConfiguration.getPortalBaseEndpoint()).thenReturn("https://www.synapse.org");

		// call under test
		assertEquals("https://www.synapse.org/RequestHistory:request/123/signature", manager.signatureUrl(123L));
	}

	@Test
	public void testSignatureUrlWithNonSynapseHost() {
		when(mockStackConfiguration.getPortalBaseEndpoint()).thenReturn("https://example.com");

		// call under test — a misconfigured base must not send requesters off to another host
		assertThrows(IllegalArgumentException.class, () -> manager.signatureUrl(123L));
	}

	@Test
	public void testIsEnabled() {
		when(mockFeatureManager.isFeatureEnabled(Feature.EDUC_SIGNATURE_NOTIFICATIONS)).thenReturn(true);

		// call under test
		assertTrue(manager.isEnabled());
	}

	@Test
	public void testIsEnabledWhenDisabled() {
		when(mockFeatureManager.isFeatureEnabled(Feature.EDUC_SIGNATURE_NOTIFICATIONS)).thenReturn(false);

		// call under test
		assertFalse(manager.isEnabled());
	}

	@Test
	public void testListEnvelopesToExamine() {
		List<EDucEnvelopeToExamine> expected = List.of(envelope);
		when(mockNotificationDao.listEnvelopesToExamine(anyLong())).thenReturn(expected);

		// call under test
		assertEquals(expected, manager.listEnvelopesToExamine(50L));

		verify(mockNotificationDao).listEnvelopesToExamine(eq(50L));
	}
}
