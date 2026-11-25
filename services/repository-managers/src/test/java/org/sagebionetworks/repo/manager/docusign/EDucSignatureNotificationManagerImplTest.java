package org.sagebionetworks.repo.manager.docusign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_REALM_ID;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.message.MessageTemplate;
import org.sagebionetworks.repo.manager.message.NotificationDeliveryPolicy;
import org.sagebionetworks.repo.manager.message.TemplatedMessageSender;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DBOEDucEnvelopeNotification;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.EDucEnvelopeNotificationDao;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.EDucNotificationState;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.EDucPendingNotification;
import org.sagebionetworks.repo.model.educ.EDucStatusEnum;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.util.Clock;
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
	private NotificationDeliveryPolicy mockDeliveryPolicy;
	@Mock
	private StackConfiguration mockStackConfiguration;
	@Mock
	private Clock mockClock;

	private EDucSignatureNotificationManagerImpl manager;

	private static final Long REQUEST_ID = 123L;
	private static final String ENVELOPE_ID = "env-1";
	private static final Long CREATED_BY = 100L;
	private static final Instant QUERIED_ON = Instant.parse("2026-11-20T10:05:00Z");

	private UserInfo requester;
	private UserInfo sender;

	@BeforeEach
	public void before() {
		manager = new EDucSignatureNotificationManagerImpl(mockNotificationDao, mockTemplatedMessageSender,
				mockUserManager, mockDeliveryPolicy, mockStackConfiguration, mockClock);

		requester = new UserInfo(false, CREATED_BY, DEFAULT_REALM_ID);
		sender = new UserInfo(false, BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId(),
				DEFAULT_REALM_ID);
	}

	private static EDucPendingNotification pending(EDucStatusEnum status) {
		return new EDucPendingNotification(REQUEST_ID, ENVELOPE_ID, status, CREATED_BY);
	}

	private static DBOEDucEnvelopeNotification recorded(EDucNotificationState state) {
		DBOEDucEnvelopeNotification dbo = new DBOEDucEnvelopeNotification();
		dbo.setEnvelopeId(ENVELOPE_ID);
		dbo.setNotificationState(state.name());
		return dbo;
	}

	private void stubPendingRow() {
		when(mockNotificationDao.findForUpdate(ENVELOPE_ID))
				.thenReturn(Optional.of(recorded(EDucNotificationState.PENDING)));
	}

	// Stubs everything needed for a message to actually be sent.
	private void stubDeliverableSend(String messageId) {
		stubPendingRow();
		when(mockUserManager.getUserInfo(CREATED_BY)).thenReturn(requester);
		when(mockDeliveryPolicy.shouldDeliver(requester)).thenReturn(true);
		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId()))
				.thenReturn(sender);
		when(mockStackConfiguration.getSynapseBaseUrl()).thenReturn("https://www.synapse.org/");
		MessageToUser message = new MessageToUser();
		message.setId(messageId);
		when(mockTemplatedMessageSender.sendMessage(any())).thenReturn(message);
	}

	@Test
	public void testGetStatusCheckpoint() {
		when(mockNotificationDao.getStatusCheckpoint()).thenReturn(Optional.of(QUERIED_ON));

		// call under test
		assertEquals(QUERIED_ON, manager.getStatusCheckpoint());

		verifyNoInteractions(mockClock);
	}

	@Test
	public void testGetStatusCheckpointWithNoneRecorded() {
		when(mockNotificationDao.getStatusCheckpoint()).thenReturn(Optional.empty());
		when(mockClock.now()).thenReturn(Date.from(Instant.parse("2026-11-20T00:00:00Z")));

		// call under test — a stack that has read nothing yet looks back over a window
		assertEquals(Instant.parse("2026-10-21T00:00:00Z"), manager.getStatusCheckpoint());
	}

	@Test
	public void testRecordTerminalStatuses() {
		// env-1 is new, env-2 is already recorded, env-3 is still in flight and env-4 belongs to another stack
		Map<String, EDucStatusEnum> statuses = Map.of("env-1", EDucStatusEnum.completed, "env-2",
				EDucStatusEnum.voided, "env-3", EDucStatusEnum.sent, "env-4", EDucStatusEnum.declined);
		when(mockNotificationDao.findRequestIdsByEnvelopeId(Set.of("env-1", "env-2", "env-4")))
				.thenReturn(Map.of("env-1", 1L, "env-2", 2L));
		when(mockNotificationDao.find("env-1")).thenReturn(Optional.empty());
		when(mockNotificationDao.find("env-2")).thenReturn(Optional.of(recorded(EDucNotificationState.NOT_SENT)));

		// call under test
		manager.recordTerminalStatuses(statuses, QUERIED_ON);

		verify(mockNotificationDao).createPending(1L, "env-1", EDucStatusEnum.completed);
		verify(mockNotificationDao, never()).createPending(eq(2L), any(), any());
		verify(mockNotificationDao).setStatusCheckpoint(QUERIED_ON);
		verifyNoInteractions(mockTemplatedMessageSender);
	}

	@Test
	public void testRecordTerminalStatusesWithNoChanges() {
		when(mockNotificationDao.findRequestIdsByEnvelopeId(Set.of())).thenReturn(Map.of());

		// call under test — the checkpoint still moves on, so the same empty window is not read again
		manager.recordTerminalStatuses(Map.of(), QUERIED_ON);

		verify(mockNotificationDao, never()).createPending(any(), any(), any());
		verify(mockNotificationDao).setStatusCheckpoint(QUERIED_ON);
	}

	@Test
	public void testRecordTerminalStatusesWithNullQueriedOn() {
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.recordTerminalStatuses(Map.of(), null);
		});
		verifyNoInteractions(mockNotificationDao);
	}

	@Test
	public void testListPendingNotifications() {
		List<EDucPendingNotification> expected = List.of(pending(EDucStatusEnum.completed));
		when(mockNotificationDao.listPendingNotifications(50L)).thenReturn(expected);

		// call under test
		assertEquals(expected, manager.listPendingNotifications(50L));
	}

	@Test
	public void testProcessPendingNotificationWithCompleted() {
		stubDeliverableSend("9001");

		// call under test
		manager.processPendingNotification(pending(EDucStatusEnum.completed));

		MessageTemplate expected = MessageTemplate.builder().withSender(sender)
				.withRecipients(Collections.singleton("100"))
				.withTemplateFile("message/EDucSignatureCompletedTemplate.html.vtl")
				.withSubject("Your data use certificate has been signed")
				.withContext(Map.of("signatureUrl",
						"https://www.synapse.org/RequestHistory:request/123/signature"))
				.build();
		verify(mockTemplatedMessageSender).sendMessage(expected);
		verify(mockNotificationDao).updateNotificationState(ENVELOPE_ID, EDucNotificationState.SENT, 9001L);
	}

	@Test
	public void testProcessPendingNotificationWithDeclined() {
		stubDeliverableSend("9002");

		// call under test
		manager.processPendingNotification(pending(EDucStatusEnum.declined));

		MessageTemplate expected = MessageTemplate.builder().withSender(sender)
				.withRecipients(Collections.singleton("100"))
				.withTemplateFile("message/EDucSignatureDeclinedTemplate.html.vtl")
				.withSubject("Action needed: a signer declined your data use certificate")
				.withContext(Map.of("signatureUrl",
						"https://www.synapse.org/RequestHistory:request/123/signature"))
				.build();
		verify(mockTemplatedMessageSender).sendMessage(expected);
		verify(mockNotificationDao).updateNotificationState(ENVELOPE_ID, EDucNotificationState.SENT, 9002L);
	}

	// A requester who cancelled their own envelope is no longer referencing it, so a voided envelope recorded
	// here is an expiry or an administrative void, which nobody is emailed about.
	@Test
	public void testProcessPendingNotificationWithVoided() {
		stubPendingRow();

		// call under test
		manager.processPendingNotification(pending(EDucStatusEnum.voided));

		verifyNoInteractions(mockTemplatedMessageSender);
		verify(mockNotificationDao).updateNotificationState(ENVELOPE_ID, EDucNotificationState.NOT_SENT, null);
	}

	@Test
	public void testProcessPendingNotificationWithAlreadyDealtWith() {
		when(mockNotificationDao.findForUpdate(ENVELOPE_ID))
				.thenReturn(Optional.of(recorded(EDucNotificationState.SENT)));

		// call under test — the requester is not emailed a second time
		manager.processPendingNotification(pending(EDucStatusEnum.completed));

		verifyNoInteractions(mockTemplatedMessageSender);
		verify(mockNotificationDao, never()).updateNotificationState(anyString(), any(), any());
	}

	@Test
	public void testProcessPendingNotificationWithNoRecord() {
		when(mockNotificationDao.findForUpdate(ENVELOPE_ID)).thenReturn(Optional.empty());

		// call under test — the request, and so its record, was deleted since the batch was listed
		manager.processPendingNotification(pending(EDucStatusEnum.completed));

		verifyNoInteractions(mockTemplatedMessageSender);
		verify(mockNotificationDao, never()).updateNotificationState(anyString(), any(), any());
	}

	// When delivery is suppressed on this stack the requester is still marked dealt with, so the envelope does
	// not stay pending.
	@Test
	public void testProcessPendingNotificationWithDeliverySuppressed() {
		stubPendingRow();
		when(mockUserManager.getUserInfo(CREATED_BY)).thenReturn(requester);
		when(mockDeliveryPolicy.shouldDeliver(requester)).thenReturn(false);

		// call under test
		manager.processPendingNotification(pending(EDucStatusEnum.completed));

		verifyNoInteractions(mockTemplatedMessageSender);
		verify(mockNotificationDao).updateNotificationState(ENVELOPE_ID, EDucNotificationState.NOT_SENT, null);
	}

	@Test
	public void testProcessPendingNotificationWithDeliveryUndetermined() {
		stubPendingRow();
		when(mockUserManager.getUserInfo(CREATED_BY)).thenReturn(requester);
		when(mockDeliveryPolicy.shouldDeliver(requester))
				.thenThrow(new RecoverableMessageException("Could not detect current stack version."));

		assertThrows(RecoverableMessageException.class, () -> {
			// call under test — left pending and retried, so the notification is not lost
			manager.processPendingNotification(pending(EDucStatusEnum.completed));
		});

		verifyNoInteractions(mockTemplatedMessageSender);
		verify(mockNotificationDao, never()).updateNotificationState(anyString(), any(), any());
	}

	@Test
	public void testSignatureUrl() {
		when(mockStackConfiguration.getSynapseBaseUrl()).thenReturn("https://www.synapse.org/");

		// call under test
		assertEquals("https://www.synapse.org/RequestHistory:request/123/signature", manager.signatureUrl(123L));
	}

	@Test
	public void testSignatureUrlWithNoTrailingSlash() {
		when(mockStackConfiguration.getSynapseBaseUrl()).thenReturn("https://www.synapse.org");

		// call under test
		assertEquals("https://www.synapse.org/RequestHistory:request/123/signature", manager.signatureUrl(123L));
	}

	@Test
	public void testSignatureUrlWithNonSynapseHost() {
		when(mockStackConfiguration.getSynapseBaseUrl()).thenReturn("https://example.com/");

		// call under test — a misconfigured base must not send requesters off to another host
		assertThrows(IllegalArgumentException.class, () -> manager.signatureUrl(123L));
	}
}
