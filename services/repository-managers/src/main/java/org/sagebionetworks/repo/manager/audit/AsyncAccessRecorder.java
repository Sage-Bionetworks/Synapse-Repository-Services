package org.sagebionetworks.repo.manager.audit;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.sagebionetworks.repo.model.dbo.auth.UserStatusDao;
import org.sagebionetworks.util.Clock;
import org.springframework.stereotype.Service;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * This implementation writes the records to S3
 * 
 * @author jmhill
 * 
 */
@Service
public class AsyncAccessRecorder implements AccessRecorder {
	
	private static final long USER_ACCESS_CACHE_MAX_SIZE = 2000;
	private static final Duration USER_ACCESS_UPDATE_FREQUENCY = Duration.ofMinutes(5);

	static private Log log = LogFactory.getLog(AsyncAccessRecorder.class);
	
	public static final String ACCESS_RECORD_STREAM = "accessRecord";

	/**
	 * At any given time, there are multiple threads creating new AccessRecords
	 * as new web services request come in. These AccessRecords are added to
	 * this batch from the threads where they originated. The batch is then
	 * processed from a separate timer thread.
	 */
	private ConcurrentLinkedQueue<AccessRecord> recordBatch = new ConcurrentLinkedQueue<AccessRecord>();
	
	private AwsKinesisFirehoseLogger firehoseLogger;
	
	private UserStatusDao userStatusDao;
	
	private Clock clock;
	
	private Cache<Long, Long> userAccessCache;

	public AsyncAccessRecorder(AwsKinesisFirehoseLogger firehoseLogger, UserStatusDao userStatusDao, Clock clock) {
		this.firehoseLogger = firehoseLogger;
		this.userStatusDao = userStatusDao;
		this.clock = clock;
		this.userAccessCache = CacheBuilder.newBuilder()
			.ticker(new Ticker() {
				@Override
				public long read() {
					return clock.nanoTime();
				}
			})
			.expireAfterWrite(USER_ACCESS_UPDATE_FREQUENCY)
			.maximumSize(USER_ACCESS_CACHE_MAX_SIZE)
			.build();
	}
	

	/**
	 * New AccessRecords will come in from 
	 */
	@Override
	public void save(AccessRecord record) {
		// add the messages to the queue;
		recordBatch.add(record);
	}

	/**
	 * When the timer fires we send the messages to S3.
	 * @throws IOException 
	 * 
	 */
	public void timerFired() {
		
		// Poll all data currently on the queue.
		List<AccessRecord> currentBatch = pollListFromQueue();
		
		// There is nothing to do if the batch is empty.
		if (currentBatch.isEmpty()) {
			return;
		}
		
		try {
			// send records to firehose delivery stream
			List<KinesisJsonEntityRecord<AccessRecord>> kinesisJsonEntityRecords = currentBatch.stream()
					.map(record -> new KinesisJsonEntityRecord<>(record.getTimestamp(), record, record.getStack(), record.getInstance()))
					.collect(Collectors.toList());

			firehoseLogger.logBatch(ACCESS_RECORD_STREAM, kinesisJsonEntityRecords);
		} catch (Exception e) {
			log.error("Failed to write batch", e);
		}
				
		Date lastSeenOn = clock.now();
		
		List<Long> lastSeenOnUpdateBatch = currentBatch.stream()
			.map(AccessRecord::getUserId)
			.filter(Objects::nonNull)
			.distinct()
			.filter(userId -> {
				
				if (userAccessCache.getIfPresent(userId) == null) {
					// Only update the cache if it is expired or not present, this effectively throttles the updates
					userAccessCache.put(userId, lastSeenOn.getTime());
					return true;
				} else {
					return false;
				}
		}).collect(Collectors.toList());
		
		if (!lastSeenOnUpdateBatch.isEmpty()) {
			userStatusDao.setLastSeenOn(lastSeenOnUpdateBatch, lastSeenOn);
		}
	}
	
	/**
	 * Poll all data currently on the queue and add it to a list.
	 * @return
	 */
	private List<AccessRecord> pollListFromQueue(){
		List<AccessRecord> list = new LinkedList<AccessRecord>();
		for(AccessRecord ac = this.recordBatch.poll(); ac != null; ac = this.recordBatch.poll()){
			// Add to the list
			list.add(ac);
		}
		return list;
	}
	
	
	
}
