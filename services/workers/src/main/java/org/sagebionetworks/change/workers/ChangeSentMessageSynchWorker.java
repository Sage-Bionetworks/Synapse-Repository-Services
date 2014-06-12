package org.sagebionetworks.change.workers;

import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This worker will synchronize the changes table with the sent message table.
 * 
 * It will find all messages that have not been sent and send them. This worker
 * scans for ranges where the changes and sent table are out-of-synch by
 * comparing the check-sums of the change numbers of both tables for that range.
 * It is possible, but unlikely, that check-sums could match yet the tables are
 * still be out-of-synch giving a false-negative. To deal with this possibility
 * the page size used for each check-sum varies pseudo-randomly from run to run.
 * 
 * @author jmhill
 * 
 */
public class ChangeSentMessageSynchWorker implements Runnable {

	static private Logger log = LogManager
			.getLogger(ChangeSentMessageSynchWorker.class);

	@Autowired
	DBOChangeDAO changeDao;
	@Autowired
	RepositoryMessagePublisher repositoryMessagePublisher;
	@Autowired
	StackStatusDao stackStatusDao;

	int pageSizeVarriance = 5000;
	int minimumPageSize = 25 * 1000;
	/**
	 * By default use a nondeterministic pseudo-random generator
	 */
	Random random = new Random(System.currentTimeMillis());

	/**
	 * Override the default random.
	 * 
	 * @param batchSize
	 */
	public void setRandom(Random random) {
		if (random == null) {
			throw new IllegalArgumentException("Random cannot be set to null");
		}
		this.random = random;
	}

	@Override
	public void run() {
		// This worker does not run during migration. This avoids any
		// intermediate state
		// That could resulting in missed row.s
		if (!StatusEnum.READ_WRITE.equals(stackStatusDao.getCurrentStatus())) {
			if (log.isTraceEnabled()) {
				log.trace("Skipping synchronization since the stack is not in read-write mode");
			}
			return;
		}
		long maxChangeNumber = changeDao.getCurrentChangeNumber();
		long minChangeNumber = changeDao.getMinimumChangeNumber();
		/*
		 * It is possible, but unlikely, that check-sums could match yet the
		 * tables are still be out-of-synch giving a false-negative. To deal
		 * with this possibility the page size used for each check-sum varies
		 * pseudo-randomly from run to run.
		 */
		int pageSize = minimumPageSize + random.nextInt(pageSizeVarriance);
		// Setup the run
		for(long lowerBounds=minChangeNumber; lowerBounds < maxChangeNumber; lowerBounds=+ pageSize){
			long upperBounds = lowerBounds+pageSize;
			// Could the tables be out-of-synch for this range?
			if(!changeDao.checkUnsentMessageByCheckSumForRange(lowerBounds, upperBounds)){
				// We are out-of-synch
				List<ChangeMessage> toSend = changeDao.listUnsentMessages(lowerBounds, upperBounds);
				for(ChangeMessage send: toSend){
					try {
						changeDao.registerMessageSent(send);
					} catch (Exception e) {
						// Failing to send one messages should not stop sending the rest.
						log.warn("Failed to register a send: "+send+" message: "+e.getMessage());
					}
				}
			}
		}
	}
}
