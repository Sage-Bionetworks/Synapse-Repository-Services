package org.sagebionetworks.dataaccess.workers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.docusign.DocuSignClient;
import org.sagebionetworks.repo.manager.docusign.EDucSignatureNotificationManager;
import org.sagebionetworks.repo.manager.stack.StackStatusManager;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.EDucEnvelopeToExamine;
import org.sagebionetworks.repo.model.educ.EDucStatusEnum;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressingRunner;

import com.docusign.esign.model.Envelope;

/**
 * Notices when an eDUC signature envelope has reached a terminal state and tells the requester.
 * <p>
 * The signing service is not asked to call us, so the envelopes still outstanding are examined on a timer
 * instead. Each pass reads a bounded batch, so a backlog is worked through over successive passes rather
 * than in one long run.
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

		List<EDucEnvelopeToExamine> envelopes = notificationManager.listEnvelopesToExamine(BATCH_SIZE);
		if (envelopes.isEmpty()) {
			return;
		}

		// One request covers the whole batch: the signing service offers a bulk status read, and asking it
		// per envelope would multiply the calls by the number of envelopes in flight.
		Map<String, EDucStatusEnum> statusByEnvelopeId = readStatuses(envelopes);

		long startTime = System.currentTimeMillis();
		int processedCount = 0;
		int errorsCount = 0;

		for (EDucEnvelopeToExamine envelope : envelopes) {
			if (!StatusEnum.READ_WRITE.equals(stackStatusManager.getCurrentStatus().getStatus())) {
				break;
			}
			try {
				notificationManager.processObservedStatus(envelope, statusByEnvelopeId.get(envelope.envelopeId()));
			} catch (Throwable e) {
				// One envelope that cannot be processed must not stop the rest of the batch, nor the timer.
				logger.warn("Failed to process envelope " + envelope.envelopeId() + ".", e);
				errorsCount++;
			}
			processedCount++;
		}

		logger.info("Examined {} eDUC envelopes (Errored: {}, Time: {} ms).", processedCount, errorsCount,
				System.currentTimeMillis() - startTime);
	}

	private Map<String, EDucStatusEnum> readStatuses(List<EDucEnvelopeToExamine> envelopes) {
		List<String> envelopeIds = envelopes.stream().map(EDucEnvelopeToExamine::envelopeId).toList();

		Map<String, EDucStatusEnum> statusByEnvelopeId = new HashMap<>();
		for (Envelope envelope : docuSignClient.listEnvelopeStatuses(envelopeIds)) {
			statusByEnvelopeId.put(envelope.getEnvelopeId(),
					DocuSignClient.toEDucStatusEnum(envelope.getStatus()));
		}
		// An envelope the service did not report is absent from the map, which the manager treats as a state
		// it could not determine and leaves for the next pass.
		return statusByEnvelopeId;
	}
}
