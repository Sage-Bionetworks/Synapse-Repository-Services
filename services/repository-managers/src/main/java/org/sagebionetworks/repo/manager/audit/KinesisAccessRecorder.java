package org.sagebionetworks.repo.manager.audit;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This implementation writes the records to S3
 * 
 * @author jmhill
 * 
 */
@Service
public class KinesisAccessRecorder implements AccessRecorder {

	static private Log log = LogFactory.getLog(KinesisAccessRecorder.class);
	public static final String ACCESS_RECORD_STREAM = "accessRecord";

	/**
	 * At any given time, there are multiple threads creating new AccessRecords
	 * as new web services request come in. These AccessRecords are added to
	 * this batch from the threads where they originated. The batch is then
	 * processed from a separate timer thread.
	 */
	private ConcurrentLinkedQueue<AccessRecord> recordBatch = new ConcurrentLinkedQueue<AccessRecord>();

	AwsKinesisFirehoseLogger firehoseLogger;

	boolean shouldAccessRecordsBePushedToS3 = true;

	@Autowired
	public KinesisAccessRecorder(AwsKinesisFirehoseLogger firehoseLogger) {
		this.firehoseLogger = firehoseLogger;
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
