package org.sagebionetworks.dataaccess.workers;

import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Repository;

/**
 * Worker to send notifications for new data access requests.
 * @author John
 *
 */
@Repository
public class DataAccessRequestNotificationWorker implements ChangeMessageDrivenRunner {

	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage message)
			throws RecoverableMessageException, Exception {
		// TODO Auto-generated method stub

	}

}
