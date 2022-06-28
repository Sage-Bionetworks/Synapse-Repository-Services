package org.sagebionetworks.dataaccess.workers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.dataaccess.DataAccessSubmissionNotificationManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionEvent;
import org.sagebionetworks.worker.TypedMessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.sqs.model.Message;

/**
 * Worker to send notifications for new data access requests.
 *
 */
@Repository
public class DataAccessSubmissionNotificationWorker implements TypedMessageDrivenRunner<DataAccessSubmissionEvent> {

	private static final Logger LOG = LogManager.getLogger(DataAccessSubmissionNotificationWorker.class);

	private final DataAccessSubmissionNotificationManager manager;

	@Autowired
	public DataAccessSubmissionNotificationWorker(DataAccessSubmissionNotificationManager manager) {
		this.manager = manager;
	}

	@Override
	public Class<DataAccessSubmissionEvent> getObjectClass() {
		return DataAccessSubmissionEvent.class;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message, DataAccessSubmissionEvent convertedMessage)
			throws RecoverableMessageException, Exception {
		if (ObjectType.DATA_ACCESS_SUBMISSION_EVENT.equals(convertedMessage.getObjectType())) {
			if (convertedMessage.getTimestamp().toInstant().isBefore(Instant.now().plus(1, ChronoUnit.HOURS))) {
				manager.sendNotificationToReviewers(convertedMessage.getObjectId());
			} else {
				LOG.info("Ignoring old message: " + message);
			}
		}
	}

}
