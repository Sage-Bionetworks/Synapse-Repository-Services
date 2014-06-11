package org.sagebionetworks.change.workers;

import java.sql.Timestamp;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.util.ClockProvider;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This worker will synchronize the changes table with the sent message table.
 * 
 * It will find all messages that have not been sent and send them. This worker
 * will persist a high-water mark to avoid re-synchronizing change numbers that
 * have already been synchronized.
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
 * message no longer exists we assume the worst case scenario and use the
 * maximum change number that still exists and is less than or equal to the
 * lastSynchedChangeNumber as the starting point for the synchronization
 * process.
 * 
 * @author jmhill
 * 
 */
public class ChangeSentMessageSynchWorker implements Runnable {

	/**
	 * This worker will ignore any message that is newer than this time.
	 */
	private static long MINIMUMN_MESSAGE_AGE = 1000*60;
	
	static private Logger log = LogManager
			.getLogger(ChangeSentMessageSynchWorker.class);

	@Autowired
	DBOChangeDAO changeDao;
	@Autowired
	RepositoryMessagePublisher repositoryMessagePublisher;
	@Autowired 
	StackStatusDao stackStatusDao;
	@Autowired
	ClockProvider clockProvider;
	// Start with a default batch size of 10K
	private int batchSize = 10 * 1000;

	private int runCount = 0;

	/**
	 * Override the batch size.
	 * 
	 * @param batchSize
	 */
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	@Override
	public void run() {
		// This worker does not run during migration. This avoids any intermediate state
		// That could resulting in missed row.s
		if(!StatusEnum.READ_WRITE.equals(stackStatusDao.getCurrentStatus())){
			if(log.isTraceEnabled()){
				log.trace("Skipping synchronization since the stack is not in read-write mode");
			}
			return;
		}
		long maxChangeNumber = changeDao.getCurrentChangeNumber();
		long minChangeNumber = changeDao.getMinimumChangeNumber();
		long lastSychedChangeNumberStart = changeDao
				.getLastSynchedChangeNumber();
		// Use the max sent change number that still exits that is less than
		// or equal to the last sent change message as the starting point for
		// this synchronization run. This ensures we never miss a message that
		// still needs to be sent.
		long synchStart = changeDao
				.getMaxSentChangeNumber(lastSychedChangeNumberStart);
		synchStart = Math.max(synchStart, minChangeNumber);
		long upperBounds = synchStart+batchSize;
		int rowsUpdated = 0;
		if(log.isTraceEnabled()){
			log.trace("Starting with synchStart: " + synchStart+" upperBounds: "+upperBounds);
		}
		// We only want to look at change messages that are older than this.
		Timestamp olderThan = new Timestamp(clockProvider.currentTimeMillis() - MINIMUMN_MESSAGE_AGE);
		// Keep going until we either reach the end or process a single batch size.
		while(synchStart < maxChangeNumber && rowsUpdated < batchSize){
			List<ChangeMessage> toBeSent = changeDao.listUnsentMessages(synchStart,
					upperBounds, olderThan);
			if(log.isTraceEnabled()){
				log.trace("\t loop synchStart: " + synchStart+" upperBounds: "+upperBounds+" returned: "+toBeSent.size()+" rows");
			}
			// Send
			for (ChangeMessage toSend : toBeSent) {
				repositoryMessagePublisher.publishToTopic(toSend);
				rowsUpdated++;
			}
			synchStart = upperBounds+1;
			upperBounds = synchStart+batchSize;
		}
		// the new Last synched change number is the max sent change number
		// that exists that is less than or equal to the upper bounds for this
		// round.
		long newLastSynchedChangeNumber = changeDao.getMaxSentChangeNumber(upperBounds);

		// Set the new high-water-mark if it has changed
		if (newLastSynchedChangeNumber > lastSychedChangeNumberStart) {
			changeDao.setLastSynchedChangeNunber(lastSychedChangeNumberStart,
					newLastSynchedChangeNumber);
			if(log.isTraceEnabled()){
				log.trace("newLastSynchedChangeNumber: "
						+ newLastSynchedChangeNumber);
			}
		}
		if (log.isTraceEnabled()) {
			log.trace("maxChangeNumber: " + maxChangeNumber);
			log.trace("minChangeNumber: " + minChangeNumber);
			log.trace("lastSychedChangeNumberStart: "
					+ lastSychedChangeNumberStart);
			log.trace("synchStart: " + synchStart);
			log.trace("upperBounds: " + upperBounds);
			log.trace("rowsUpdated: " + rowsUpdated);
			log.trace("Run count: " + runCount++);
		}
	}

}
