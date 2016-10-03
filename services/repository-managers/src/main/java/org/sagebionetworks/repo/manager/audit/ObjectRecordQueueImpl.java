package org.sagebionetworks.repo.manager.audit;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.audit.dao.ObjectRecordBatch;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;


public class ObjectRecordQueueImpl implements ObjectRecordQueue {
	

	static private Log log = LogFactory.getLog(ObjectRecordQueueImpl.class);
	
	/**
	 * At any given time, there are multiple threads creating new ObjectRecordBatch
	 * as new web services request come in. These ObjectRecordBatch are added to
	 * this queue from the threads where they originated. The queue is then
	 * processed from a separate timer thread.
	 */
	private ConcurrentLinkedQueue<ObjectRecordBatch> queue = new ConcurrentLinkedQueue<ObjectRecordBatch>();
	
	@Autowired
	ObjectRecordDAO objectRecordDAO;

	@Override
	public void pushObjectRecordBatch(ObjectRecordBatch batch) {
		ValidateArgument.required(batch, "batch");
		ValidateArgument.required(batch.getType(), "batch.type");
		ValidateArgument.required(batch.getRecords(), "batch.records");
		ValidateArgument.requirement(!batch.getRecords().isEmpty(), "batch.records cannot be empty");
		queue.add(batch);
	}
	
	/**
	 * When the timer fires we send the messages to S3.
	 * @throws IOException 
	 * 
	 * 
	 */
	@Override
	public void timerFired() throws IOException {
		// Poll all data currently on the queue.
		Map<String, List<ObjectRecord>> batchMap = pollBatches();
		if(batchMap == null){
			// there is nothing to send.
			return;
		}
		for(String type: batchMap.keySet()){
			List<ObjectRecord> batch = batchMap.get(type);
			// send the batch to S3
			try {
				objectRecordDAO.saveBatch(batch, type);
			} catch (Exception e) {
				log.error("Failed to send ObjectRecord batch to S3", e);
			}
		}
	}
	
	/**
	 * Poll all data currently on the queue and add it to a list.
	 * @return
	 */
	Map<String, List<ObjectRecord>> pollBatches(){
		if(this.queue.isEmpty()){
			return null;
		}
		Map<String, List<ObjectRecord>> batchMap = new HashMap<String, List<ObjectRecord>>(this.queue.size());
		for(ObjectRecordBatch batch = this.queue.poll(); batch != null; batch = this.queue.poll()){
			List<ObjectRecord> thisBatch = batchMap.get(batch.getType());
			if(thisBatch == null){
				thisBatch = new LinkedList<>();
				batchMap.put(batch.getType(), thisBatch);
			}
			thisBatch.addAll(batch.getRecords());
		}
		return batchMap;
	}

	@Override
	public int getQueueSize() {
		return queue.size();
	}
}
