package org.sagebionetworks.dataaccess.workers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.dataaccess.DataAccessRequestNotificationManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Worker to send notifications for new data access requests.
 *
 */
@Repository
public class DataAccessRequestNotificationWorker implements ChangeMessageDrivenRunner {

	private static final Logger LOG = LogManager.getLogger(DataAccessRequestNotificationWorker.class);

	private final DataAccessRequestNotificationManager manager;

	@Autowired
	public DataAccessRequestNotificationWorker(DataAccessRequestNotificationManager manager) {
		this.manager = manager;
	}

	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage message)
			throws RecoverableMessageException, Exception {

		if (ObjectType.DATA_ACCESS_REQUEST.equals(message.getObjectType())) {
			if (ChangeType.CREATE.equals(message.getChangeType())
					|| ChangeType.UPDATE.equals(message.getChangeType())) {
				if (message.getTimestamp().toInstant().isBefore(Instant.now().plus(1, ChronoUnit.HOURS))) {
					manager.dataAccessRequestCreatedOrUpdated(message.getObjectId());
				} else {
					LOG.info("Ignoring old message: " + message);
				}
			}
		}

	}

}
