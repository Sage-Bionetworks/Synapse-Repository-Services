package org.sagebionetworks.dataaccess.workers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.docusign.DocuSignClient;
import org.sagebionetworks.docusign.EnvelopeStatusChanges;
import org.sagebionetworks.repo.manager.docusign.EDucSignatureNotificationManager;
import org.sagebionetworks.repo.manager.stack.StackStatusManager;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.EDucPendingNotification;
import org.sagebionetworks.repo.model.educ.EDucStatusEnum;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressingRunner;

import com.docusign.esign.model.Envelope;

/**
 * Notices when an eDUC signature envelope has reached a terminal state and tells the requester.
 * <p>
 * The signing service is not asked to call us, so on a timer it is asked instead which envelopes reached a
 * terminal state since the last time, and those belonging to this stack's requests are recorded. The
 * requesters of recorded envelopes are then dealt with in bounded batches, so a backlog is worked through
 * over successive passes rather than in one long run.
 */
public class EDucSignatureNotificationWorker implements ProgressingRunner {

	static final long BATCH_SIZE = 100;

	private final EDucSignatureNotificationManager notificationManager;
	private final DocuSignClient docuSignClient;
	private final StackStatusManager stackStatusManager;
	private Logger logger;

	public EDucSignatureNotificationWorker(EDucSignatureNotificationManager notificationManager,
			DocuSignClient docuSignClient, StackStatusManager stackStatusManager) {
		this.notificationManager = notificationManager;
		this.docuSignClient = docuSignClient;
		this.stackStatusManager = stackStatusManager;
	}

	public void configureLogger(LoggerProvider loggerProvider) {
		this.logger = loggerProvider.getLogger(EDucSignatureNotificationWorker.class.getName());
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		if (!notificationManager.isEnabled()) {
			return;
		}
		try {
			recordTerminalStatuses();
		} catch (Throwable e) {
			// The checkpoint is left where it was, so the same window is read again next time, and
			// envelopes already recorded are still dealt with below
			logger.warn("Failed to record eDUC envelope status changes.", e);
		}
		processPendingNotifications();
	}

	private void recordTerminalStatuses() {
		EnvelopeStatusChanges changes = docuSignClient
				.listEnvelopesReachingTerminalStatus(notificationManager.getStatusCheckpoint());

		Map<String, EDucStatusEnum> statusByEnvelopeId = new HashMap<>();
		for (Envelope envelope : changes.envelopes()) {
			try {
				EDucStatusEnum status = DocuSignClient.toEDucStatusEnum(envelope.getStatus());
				if (status != null) {
					statusByEnvelopeId.put(envelope.getEnvelopeId(), status);
				}
			} catch (IllegalArgumentException e) {
				// Skipped rather than failing the window, which would hold the checkpoint on it indefinitely
				logger.warn("Skipping envelope " + envelope.getEnvelopeId() + " with unrecognized status "
						+ envelope.getStatus() + ".");
			}
		}
		notificationManager.recordTerminalStatuses(statusByEnvelopeId, changes.queriedOn());
	}

	private void processPendingNotifications() {
		List<EDucPendingNotification> pending = notificationManager.listPendingNotifications(BATCH_SIZE);
		if (pending.isEmpty()) {
			return;
		}

		long startTime = System.currentTimeMillis();
		int processedCount = 0;
		int errorsCount = 0;

		for (EDucPendingNotification notification : pending) {
			if (!StatusEnum.READ_WRITE.equals(stackStatusManager.getCurrentStatus().getStatus())) {
				break;
			}
			try {
				notificationManager.processPendingNotification(notification);
			} catch (Throwable e) {
				// One requester who cannot be dealt with must not stop the rest of the batch, nor the timer
				logger.warn("Failed to process envelope " + notification.envelopeId() + ".", e);
				errorsCount++;
			}
			processedCount++;
		}

		logger.info("Processed {} eDUC envelope notifications (Errored: {}, Time: {} ms).", processedCount,
				errorsCount, System.currentTimeMillis() - startTime);
	}
}
