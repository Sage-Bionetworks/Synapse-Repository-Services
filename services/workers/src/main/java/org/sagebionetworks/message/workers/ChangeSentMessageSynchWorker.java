package org.sagebionetworks.message.workers;

import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This worker will synchronize the changes table with the sent message table.
 * 
 * It will find all messages not that have not been sent and send them. This
 * worker will persist a high-water mark to avoid re-synchronizing change
 * numbers that have already been synchronized.
 * 
 * If the last synched change number (high-water marke) no longer exists in the
 * sent table then one of two events could have occurred:
 * <ol>
 * <li>Migration deleted changes for events that occurred on staging that did
 * not occur in production.</li>
 * <li>The object associated with a change message changed again and was issued
 * a new, larger, change number.</li>
 * </ol>
 * The second case would not cause this process to miss a messages that still
 * needs to be sent, but the first could. Therefore, whenever the last synched
 * message no longer exists we assume the worst case senario and use the maximum
 * change number that still exists and is less than or equal to the
 * lastSynchedChangeNumber as the starting point for the synchronization
 * process.
 * 
 * @author jmhill
 * 
 */
public class ChangeSentMessageSynchWorker implements Runnable {

	@Autowired
	DBOChangeDAO changeDao;

	@Override
	public void run() {
		Long maxChangeNumber = changeDao.getCurrentChangeNumber();
		Long lastSychedChangeNumber = changeDao.getLastSynchedChangeNumber();
		// Use the max sent change number that still exits that is less than or equal to
		// the last sent change message as the starting point for this synchronization run.
		Long synchStart = changeDao
				.getMaxSentChangeNumber(lastSychedChangeNumber);

	}

}
