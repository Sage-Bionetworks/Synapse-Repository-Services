package org.sagebionetworks.repo.manager.audit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.audit.AccessRecordCSVUtils;
import org.sagebionetworks.audit.AccessRecordCollateUtils;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.sagebionetworks.repo.model.audit.AccessRecorder;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

/**
 * This implementation writes the records to S3
 * 
 * @author jmhill
 * 
 */
public class S3AccessRecorder implements AccessRecorder {

	static private Log log = LogFactory.getLog(S3AccessRecorder.class);
	
	@Autowired
	AmazonS3Client s3Client;

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
	public void timerFired() throws IOException {
		// Get the current batch and replace it with a new empty list as an atomic operation.
		List<AccessRecord> currentBatch = recordBatch.getAndSet(Collections.synchronizedList(new LinkedList<AccessRecord>()));
		// There is nothing to do if the batch is empty.
		if(currentBatch.isEmpty()) return;
		try{
			// We are now free to process the current batch with out synchronization or data loss. 
			// Order the batch by timestamp
			AccessRecordCollateUtils.sortByTimestamp(currentBatch);
			// Write the data to a gzip
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			AccessRecordCSVUtils.writeCSVGZip(currentBatch.iterator(), out);
			// Create an input stream
			byte[] bytes = out.toByteArray();
			ByteArrayInputStream in = new ByteArrayInputStream(bytes);
			String fileName = UUID.randomUUID().toString()+".csv.gz";
			ObjectMetadata om = new ObjectMetadata();
			om.setContentType("application/x-gzip");
			om.setContentEncoding("gzip");
			om.setContentDisposition("attachment; filename="+fileName+";");
			om.setContentLength(bytes.length);
			s3Client.putObject("dev.hill.rest.doc.sagebase.org", fileName, in, om);
		}catch(Exception e){
			log.error("Failed to write batch", e);
		}
	}

}
