package org.sagebionetworks.dataaccess.workers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.docusign.DocuSignClient;
import org.sagebionetworks.repo.manager.docusign.EDucSignatureNotificationManager;
import org.sagebionetworks.repo.manager.stack.StackStatusManager;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.EDucEnvelopeToExamine;
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

	private static final EDucEnvelopeToExamine ENVELOPE_ONE = new EDucEnvelopeToExamine(1L, "env-1", 100L);
	private static final EDucEnvelopeToExamine ENVELOPE_TWO = new EDucEnvelopeToExamine(2L, "env-2", 200L);

	@BeforeEach
	public void before() {
		when(mockLoggerProvider.getLogger(any())).thenReturn(mockLogger);
		worker = new EDucSignatureNotificationWorker(mockNotificationManager, mockDocuSignClient,
				mockStackStatusManager);
		worker.configureLogger(mockLoggerProvider);
	}

	private void stubReadWrite() {
		StackStatus status = new StackStatus();
		status.setStatus(StatusEnum.READ_WRITE);
		when(mockStackStatusManager.getCurrentStatus()).thenReturn(status);
	}

	private Envelope envelope(String envelopeId, String status) {
		Envelope envelope = new Envelope();
		envelope.setEnvelopeId(envelopeId);
		envelope.setStatus(status);
		return envelope;
	}

	@Test
	public void testRun() throws Exception {
		when(mockNotificationManager.isEnabled()).thenReturn(true);
		when(mockNotificationManager.listEnvelopesToExamine(anyLong()))
				.thenReturn(List.of(ENVELOPE_ONE, ENVELOPE_TWO));
		when(mockDocuSignClient.listEnvelopeStatuses(any()))
				.thenReturn(List.of(envelope("env-1", "completed"), envelope("env-2", "sent")));
		stubReadWrite();

		// call under test
		worker.run(mockProgressCallback);

		// The whole batch is read in one request rather than one per envelope.
		verify(mockDocuSignClient, times(1)).listEnvelopeStatuses(List.of("env-1", "env-2"));
		verify(mockNotificationManager).processObservedStatus(ENVELOPE_ONE, EDucStatusEnum.completed);
		verify(mockNotificationManager).processObservedStatus(ENVELOPE_TWO, EDucStatusEnum.sent);
	}

	@Test
	public void testRunWithFeatureDisabled() throws Exception {
		when(mockNotificationManager.isEnabled()).thenReturn(false);

		// call under test
		worker.run(mockProgressCallback);

		verify(mockNotificationManager, never()).listEnvelopesToExamine(anyLong());
		verifyNoInteractions(mockDocuSignClient);
	}

	@Test
	public void testRunWithNothingToExamine() throws Exception {
		when(mockNotificationManager.isEnabled()).thenReturn(true);
		when(mockNotificationManager.listEnvelopesToExamine(anyLong())).thenReturn(List.of());

		// call under test — no envelopes means the signing service is not called at all
		worker.run(mockProgressCallback);

		verifyNoInteractions(mockDocuSignClient);
		verify(mockNotificationManager, never()).processObservedStatus(any(), any());
	}

	// One envelope that cannot be processed must not cost the rest of the batch, nor kill the timer.
	@Test
	public void testRunWithOneEnvelopeFailing() throws Exception {
		when(mockNotificationManager.isEnabled()).thenReturn(true);
		when(mockNotificationManager.listEnvelopesToExamine(anyLong()))
				.thenReturn(List.of(ENVELOPE_ONE, ENVELOPE_TWO));
		when(mockDocuSignClient.listEnvelopeStatuses(any()))
				.thenReturn(List.of(envelope("env-1", "completed"), envelope("env-2", "declined")));
		stubReadWrite();
		doThrow(new IllegalStateException("nope")).when(mockNotificationManager)
				.processObservedStatus(eq(ENVELOPE_ONE), any());

		// call under test
		worker.run(mockProgressCallback);

		verify(mockNotificationManager).processObservedStatus(ENVELOPE_TWO, EDucStatusEnum.declined);
	}

	// An envelope the signing service did not report has no known status, which the manager treats as a
	// state it could not determine.
	@Test
	public void testRunWithEnvelopeMissingFromResponse() throws Exception {
		when(mockNotificationManager.isEnabled()).thenReturn(true);
		when(mockNotificationManager.listEnvelopesToExamine(anyLong())).thenReturn(List.of(ENVELOPE_ONE));
		when(mockDocuSignClient.listEnvelopeStatuses(any())).thenReturn(List.of());
		stubReadWrite();

		// call under test
		worker.run(mockProgressCallback);

		verify(mockNotificationManager).processObservedStatus(ENVELOPE_ONE, null);
	}

	@Test
	public void testRunWithStackNotReadWrite() throws Exception {
		when(mockNotificationManager.isEnabled()).thenReturn(true);
		when(mockNotificationManager.listEnvelopesToExamine(anyLong())).thenReturn(List.of(ENVELOPE_ONE));
		when(mockDocuSignClient.listEnvelopeStatuses(any()))
				.thenReturn(List.of(envelope("env-1", "completed")));
		StackStatus status = new StackStatus();
		status.setStatus(StatusEnum.READ_ONLY);
		when(mockStackStatusManager.getCurrentStatus()).thenReturn(status);

		// call under test — nothing is written while the stack is not accepting writes
		worker.run(mockProgressCallback);

		verify(mockNotificationManager, never()).processObservedStatus(any(), any());
	}
}
