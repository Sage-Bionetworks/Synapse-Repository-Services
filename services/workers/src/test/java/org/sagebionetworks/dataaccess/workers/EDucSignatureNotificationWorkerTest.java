package org.sagebionetworks.dataaccess.workers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.docusign.DocuSignClient;
import org.sagebionetworks.docusign.EnvelopeStatusChanges;
import org.sagebionetworks.repo.manager.docusign.EDucSignatureNotificationManager;
import org.sagebionetworks.repo.manager.stack.StackStatusManager;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.EDucPendingNotification;
import org.sagebionetworks.repo.model.educ.EDucStatusEnum;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.util.progress.ProgressCallback;

import com.docusign.esign.model.Envelope;

@ExtendWith(MockitoExtension.class)
public class EDucSignatureNotificationWorkerTest {

	@Mock
	private EDucSignatureNotificationManager mockNotificationManager;
	@Mock
	private DocuSignClient mockDocuSignClient;
	@Mock
	private StackStatusManager mockStackStatusManager;
	@Mock
	private LoggerProvider mockLoggerProvider;
	@Mock
	private Logger mockLogger;
	@Mock
	private ProgressCallback mockProgressCallback;

	private EDucSignatureNotificationWorker worker;

	private static final Instant CHECKPOINT = Instant.parse("2026-11-20T10:00:00Z");
	private static final Instant QUERIED_ON = Instant.parse("2026-11-20T10:05:00Z");
	private static final EDucPendingNotification PENDING_ONE = new EDucPendingNotification(1L, "env-1",
			EDucStatusEnum.completed, 100L);
	private static final EDucPendingNotification PENDING_TWO = new EDucPendingNotification(2L, "env-2",
			EDucStatusEnum.declined, 200L);

	@BeforeEach
	public void before() {
		when(mockLoggerProvider.getLogger(any())).thenReturn(mockLogger);
		worker = new EDucSignatureNotificationWorker(mockNotificationManager, mockDocuSignClient,
				mockStackStatusManager);
		worker.configureLogger(mockLoggerProvider);
	}

	private void stubStackStatus(StatusEnum value) {
		StackStatus status = new StackStatus();
		status.setStatus(value);
		when(mockStackStatusManager.getCurrentStatus()).thenReturn(status);
	}

	private static Envelope envelope(String envelopeId, String status) {
		Envelope envelope = new Envelope();
		envelope.setEnvelopeId(envelopeId);
		envelope.setStatus(status);
		return envelope;
	}

	private void stubStatusChanges(List<Envelope> envelopes) {
		when(mockNotificationManager.getStatusCheckpoint()).thenReturn(CHECKPOINT);
		when(mockDocuSignClient.listEnvelopesReachingTerminalStatus(CHECKPOINT))
				.thenReturn(new EnvelopeStatusChanges(envelopes, QUERIED_ON));
	}

	@Test
	public void testRun() throws Exception {
		stubStatusChanges(List.of(envelope("env-1", "completed"), envelope("env-2", "declined")));
		when(mockNotificationManager.listPendingNotifications(anyLong())).thenReturn(List.of(PENDING_ONE, PENDING_TWO));
		stubStackStatus(StatusEnum.READ_WRITE);

		// call under test
		worker.run(mockProgressCallback);

		// Recording comes first, so an envelope finished in this window is dealt with in the same pass
		InOrder inOrder = inOrder(mockNotificationManager);
		inOrder.verify(mockNotificationManager).recordTerminalStatuses(
				Map.of("env-1", EDucStatusEnum.completed, "env-2", EDucStatusEnum.declined), QUERIED_ON);
		inOrder.verify(mockNotificationManager).listPendingNotifications(EDucSignatureNotificationWorker.BATCH_SIZE);
		inOrder.verify(mockNotificationManager).processPendingNotification(PENDING_ONE);
		inOrder.verify(mockNotificationManager).processPendingNotification(PENDING_TWO);
	}

	@Test
	public void testRunWithUnrecognizedStatus() throws Exception {
		stubStatusChanges(List.of(envelope("env-1", "completed"), envelope("env-2", "not-a-status")));
		when(mockNotificationManager.listPendingNotifications(anyLong())).thenReturn(List.of());

		// call under test
		worker.run(mockProgressCallback);

		// A status the client cannot map is left out rather than recorded
		verify(mockNotificationManager).recordTerminalStatuses(Map.of("env-1", EDucStatusEnum.completed),
				QUERIED_ON);
	}

	// An outage of the signing service must not hold up requesters whose envelopes are already recorded
	@Test
	public void testRunWithSigningServiceFailing() throws Exception {
		when(mockNotificationManager.getStatusCheckpoint()).thenReturn(CHECKPOINT);
		when(mockDocuSignClient.listEnvelopesReachingTerminalStatus(CHECKPOINT))
				.thenThrow(new IllegalStateException("DocuSign is down"));
		when(mockNotificationManager.listPendingNotifications(anyLong())).thenReturn(List.of(PENDING_ONE));
		stubStackStatus(StatusEnum.READ_WRITE);

		// call under test
		worker.run(mockProgressCallback);

		verify(mockNotificationManager, never()).recordTerminalStatuses(any(), any());
		verify(mockNotificationManager).processPendingNotification(PENDING_ONE);
	}

	// One requester who cannot be dealt with must not cost the rest of the batch, nor kill the timer
	@Test
	public void testRunWithOneNotificationFailing() throws Exception {
		stubStatusChanges(List.of());
		when(mockNotificationManager.listPendingNotifications(anyLong())).thenReturn(List.of(PENDING_ONE, PENDING_TWO));
		stubStackStatus(StatusEnum.READ_WRITE);
		doThrow(new IllegalStateException("nope")).when(mockNotificationManager)
				.processPendingNotification(PENDING_ONE);

		// call under test
		worker.run(mockProgressCallback);

		verify(mockNotificationManager).processPendingNotification(PENDING_TWO);
	}

	@Test
	public void testRunWithNothingPending() throws Exception {
		stubStatusChanges(List.of());
		when(mockNotificationManager.listPendingNotifications(anyLong())).thenReturn(List.of());

		// call under test
		worker.run(mockProgressCallback);

		verify(mockNotificationManager).recordTerminalStatuses(Map.of(), QUERIED_ON);
		verify(mockNotificationManager, never()).processPendingNotification(any());
		verifyNoInteractions(mockStackStatusManager);
	}

	@Test
	public void testRunWithStackNotReadWrite() throws Exception {
		stubStatusChanges(List.of());
		when(mockNotificationManager.listPendingNotifications(anyLong())).thenReturn(List.of(PENDING_ONE));
		stubStackStatus(StatusEnum.READ_ONLY);

		// call under test — nothing is sent while the stack is not accepting writes
		worker.run(mockProgressCallback);

		verify(mockNotificationManager, never()).processPendingNotification(any());
	}
}
