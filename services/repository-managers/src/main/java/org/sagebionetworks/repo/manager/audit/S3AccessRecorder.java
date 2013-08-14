package org.sagebionetworks.repo.manager.audit;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.sagebionetworks.repo.model.audit.AccessRecorder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This implementation writes the records to S3
 * 
 * @author jmhill
 * 
 */
public class S3AccessRecorder implements AccessRecorder {

	static private Log log = LogFactory.getLog(S3AccessRecorder.class);

	/**
	 * At any given time, there are multiple threads creating new AccessRecords
	 * as new web services request come in. These AccessRecords are added to
	 * this batch from the threads where they originated. The batch is then
	 * processed from a separate timer thread. To ensure no data is lost in this
	 * multiple thread scenario, we use AtomicReference.getAndSet() method. This
	 * allows the processing thread to get the current batch for processing and
	 * replace it with a new empty list as an atomic operation. That way if 
	 * new records come in during processing no data is lost.
	 */
	private AtomicReference<List<AccessRecord>> recordBatch = new AtomicReference<List<AccessRecord>>(
			Collections.synchronizedList(new LinkedList<AccessRecord>()));
	
	@Autowired
	AccessRecordManager accessRecordManager;

	boolean shouldAccessRecordsBePushedToS3 = true;


	/**
	 * This allows us to turn off pushing access data to S3 during build and test.
	 * This value is injected via Spring
	 * @param shouldAccessMessagesBePushedToS3
	 */
	public void setShouldAccessRecordsBePushedToS3(
			boolean shouldAccessMessagesBePushedToS3) {
		this.shouldAccessRecordsBePushedToS3 = shouldAccessMessagesBePushedToS3;
	}

	/**
	 * New AccessRecords will come in from 
	 */
	@Override
	public void save(AccessRecord record) {
		// add the messages to the queue;
		recordBatch.get().add(record);
	}

	/**
	 * When the timer fires we send the messages to S3.
	 * @throws IOException 
	 * 
	 */
	public String timerFired() throws IOException {
		// Get the current batch and replace it with a new empty list as an atomic operation.
		List<AccessRecord> currentBatch = recordBatch.getAndSet(Collections.synchronizedList(new LinkedList<AccessRecord>()));
		// There is nothing to do if the batch is empty.
		if(currentBatch.isEmpty()) return null;
		// Check to see if the data should be sent to S3
		if(!shouldAccessRecordsBePushedToS3){
			if(log.isDebugEnabled() && currentBatch.size() > 0){
				log.debug("S3AccessRecorder.shouldAccessMessagesBePushedToS3 = false.  So "+currentBatch.size()+" AccessRecords will be thrown away.");
			}
			return null;
		}
		try{
			// We are now free to process the current batch with out synchronization or data loss
			return accessRecordManager.saveBatch(currentBatch);
		}catch(Exception e){
			log.error("Failed to write batch", e);
			return null;
		}
	}
	
}
