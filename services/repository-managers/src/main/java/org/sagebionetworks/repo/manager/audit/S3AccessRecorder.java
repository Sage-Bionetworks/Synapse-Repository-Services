package org.sagebionetworks.repo.manager.audit;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

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
	 * processed from a separate timer thread.
	 */
	private ConcurrentLinkedQueue<AccessRecord> recordBatch = new ConcurrentLinkedQueue<AccessRecord>();
	
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
		recordBatch.add(record);
	}

	/**
	 * When the timer fires we send the messages to S3.
	 * @throws IOException 
	 * 
	 */
	public String timerFired() throws IOException {
		// Poll all data currently on the queue.
		List<AccessRecord> currentBatch = pollListFromQueue();
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
