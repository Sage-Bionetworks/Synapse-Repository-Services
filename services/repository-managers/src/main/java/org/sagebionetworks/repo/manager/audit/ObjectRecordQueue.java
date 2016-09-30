package org.sagebionetworks.repo.manager.audit;

import java.io.IOException;

import org.sagebionetworks.audit.dao.ObjectRecordBatch;

/**
 * Abstraction for an in-memory queue that gathers ObjectRecords from various
 * thread. The queue is emptied from a timer thread that sends the ObjectRecords
 * to S3 in batches.
 * 
 * @author John
 * 
 */
public interface ObjectRecordQueue {

	
	/**
	 * Push a new batch of ObjectRecords to the in-memory queue.
	 * The batch will be sent to S3 from a timer thread.
	 * 
	 * @param batch
	 */
	public void pushObjectRecordBatch(ObjectRecordBatch batch);
	
	/**
	 * Peek at the number of batches in the Queue.
	 * 
	 * @return
	 */
	public int peekQueueSize();

	/**
	 * When the timer fires the queue will be emptied.
	 * 
	 * @throws IOException
	 */
	void timerFired() throws IOException;
}
