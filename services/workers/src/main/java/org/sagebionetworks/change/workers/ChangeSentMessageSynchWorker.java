package org.sagebionetworks.change.workers;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.manager.message.ChangeMessageUtils;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisherImpl;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.cloudwatch.model.StandardUnit;

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
public class ChangeSentMessageSynchWorker implements ProgressingRunner<Void> {

	private static final String AVG_PUBLISH = "avg publish time";

	private static final String ELAPSE_TIME = "elapse time";

	private static final String SENT_COUNT = "sent count";

	private static final String FAILURE_COUNT = "failure count";

	/**
	 * This worker will ignore any message that is newer than this time.
	 */
	private static long MINIMUMN_MESSAGE_AGE = 1000*60;
	
	static private Logger log = LogManager
			.getLogger(ChangeSentMessageSynchWorker.class);
	
	private static String METRIC_NAMESPACE = ChangeSentMessageSynchWorker.class.getName()+" - "+ StackConfiguration.getStackInstance();

	@Autowired
	DBOChangeDAO changeDao;
	@Autowired
	RepositoryMessagePublisher repositoryMessagePublisher;
	@Autowired 
	StackStatusDao stackStatusDao;
	@Autowired
	Clock clock;
	@Autowired
	StackConfiguration configuration;
	@Autowired
	WorkerLogger workerLogger;

	int pageSizeVarriance = 5000;
	/**
	 * By default use a nondeterministic pseudo-random generator
	 */
	Random random = new Random(System.currentTimeMillis());

	@Override
	public void run(final ProgressCallback<Void> progressCallback) throws Exception {
		// This worker does not run during migration. This avoids any
		// intermediate state
		// That could resulting in missed row.s
		if (!stackStatusDao.isStackReadWrite()) {
			if (log.isTraceEnabled()) {
				log.trace("Skipping synchronization since the stack is not in read-write mode");
			}
			return;
		}

		long maxChangeNumber = changeDao.getCurrentChangeNumber();
		long minChangeNumber = changeDao.getMinimumChangeNumber();
		// We only want to look at change messages that are older than this.
		Timestamp olderThan = new Timestamp(clock.currentTimeMillis() - MINIMUMN_MESSAGE_AGE);
		/*
		 * It is possible, but unlikely, that check-sums could match yet the
		 * tables are still be out-of-synch giving a false-negative. To deal
		 * with this possibility the page size used for each check-sum varies
		 * pseudo-randomly from run to run.
		 */
		int pageSize = getMinimumPageSize() + random.nextInt(pageSizeVarriance);
		// Setup the run
		for(long lowerBounds=minChangeNumber; lowerBounds <= maxChangeNumber; lowerBounds+= pageSize){
			long startTime = System.currentTimeMillis();
			long countSuccess = 0;
			long countFailures = 0;
			long publishElapseSum = 0;
			long upperBounds = lowerBounds-1+pageSize;
			// Could the tables be out-of-synch for this range?
			if(!changeDao.checkUnsentMessageByCheckSumForRange(lowerBounds, upperBounds)){
				// We are out-of-synch
				List<ChangeMessage> toSend = changeDao.listUnsentMessages(lowerBounds, upperBounds, olderThan);
				// Group the changes by object type and partition by max changes per SQS messages body.
				Map<ObjectType, List<List<ChangeMessage>>> batches = 
						ChangeMessageUtils.groupByObjectTypeAndPartitionEachGroup(
								toSend, ChangeMessageUtils.MAX_NUMBER_OF_CHANGE_MESSAGES_PER_SQS_MESSAGE);
				for(ObjectType groupType: batches.keySet()){
					List<List<ChangeMessage>> subLists = batches.get(groupType);
					// Send each sub-list as a batch
					for(List<ChangeMessage> batch: subLists){
						try {
							// For each message make progress
							progressCallback.progressMade(null);
							// publish the message.
							long pubStart = System.currentTimeMillis();
							// publish the batch
							repositoryMessagePublisher.publishBatchToTopic(groupType, batch);
							publishElapseSum += System.currentTimeMillis() - pubStart;
							countSuccess += batch.size();
						} catch (Exception e) {
							countFailures += batch.size();
							// Failing to send one messages should not stop sending the
							// rest.
							log.warn("Failed to register a send batch. message: "+ e.getMessage());
						}
					}
				}

			}
			// Extend the timeout for this worker by calling the callback
			progressCallback.progressMade(null);
			// Create some metrics
			long elapse = System.currentTimeMillis()-startTime;
			workerLogger.logCustomMetric(createElapseProfileData(elapse, ELAPSE_TIME));
			workerLogger.logCustomMetric(createSentCount(countSuccess));
			workerLogger.logCustomMetric(createFailureCount(countFailures));
			if(countSuccess > 0){
				long avgPublishMS = publishElapseSum/countSuccess;
				workerLogger.logCustomMetric(createElapseProfileData(avgPublishMS, AVG_PUBLISH));
			}
			// Terminate if the stack goes into read-only mode.
			if (!stackStatusDao.isStackReadWrite()) {
				return;
			}
			// Sleep between pages to keep from overloading the database.
			clock.sleepNoInterrupt(configuration.getChangeSynchWorkerSleepTimeMS());
		}

	}
	
	/**
	 * Metric for the runtime.
	 * @param elapseMS
	 * @return
	 */
	public static ProfileData createElapseProfileData(long elapseMS, String name){
		ProfileData nextPD = new ProfileData();
		nextPD.setNamespace(METRIC_NAMESPACE); 
		nextPD.setName(name);
		nextPD.setValue((double)elapseMS);
		nextPD.setUnit(StandardUnit.Milliseconds.name());
		nextPD.setTimestamp(new Date(System.currentTimeMillis()));
		return nextPD;
	}
	
	public static ProfileData createSentCount(long count){
		return createCountMetric(count, SENT_COUNT);
	}
	
	public static ProfileData createFailureCount(long count){
		return createCountMetric(count, FAILURE_COUNT);
	}
	
	public static ProfileData createCountMetric(long count, String name){
		ProfileData nextPD = new ProfileData();
		nextPD.setNamespace(METRIC_NAMESPACE); 
		nextPD.setName(name);
		nextPD.setValue((double)count);
		nextPD.setUnit(StandardUnit.Count.name());
		nextPD.setTimestamp(new Date(System.currentTimeMillis()));
		return nextPD;
	}

	/**
	 * The minimum page size used by this worker.
	 * @return
	 */
	public int getMinimumPageSize() {
		return configuration.getChangeSynchWorkerMinPageSize();
	}
}
