package org.sagebionetworks.repo.audit;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.audit.dao.ObjectRecordBatch;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.repo.manager.audit.ObjectRecordQueue;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.workers.util.aws.message.QueueCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ObjectRecordQueueAutowireTest {

	private static final String QUEUE_NAME = "OBJECT";
	
	@Autowired
	ObjectRecordDAO objectRecordDAO;
	@Autowired
	ObjectRecordQueue objectRecordQueue;
	@Autowired
	QueueCleaner queueCleaner;
	
	BulkFileDownloadResponse sampleResponse;
	ObjectRecord sampleRecord;
	
	List<ObjectRecord> records;
	ObjectRecordBatch batch;
	String type;
	
	@Before
	public void before(){
		sampleResponse = new BulkFileDownloadResponse();
		sampleResponse.setUserId("12345");
		
		sampleRecord = ObjectRecordBuilderUtils.buildObjectRecord(sampleResponse, System.currentTimeMillis());
		type = sampleRecord.getJsonClassName();
		records = Lists.newArrayList(sampleRecord);
		batch = new ObjectRecordBatch(records, type);
		// start clean
		objectRecordDAO.deleteAllStackInstanceBatches(type);
		queueCleaner.purgeQueue(StackConfigurationSingleton.singleton().getAsyncQueueName(QUEUE_NAME));
	}
	
	@Test (timeout=1000*60*2)
	public void testPushAndFire() throws InterruptedException, IOException{
		// push this record to the queue.
		objectRecordQueue.pushObjectRecordBatch(batch);
		// wait for the queue to be empty
		while(objectRecordQueue.getQueueSize() > 0){
			System.out.println("Waiting for queue to empty...");
			Thread.sleep(2000);
		}
		// Validate the record was pushed.
		Iterator<String> it = objectRecordDAO.keyIterator(type);
		boolean found = false;
		while(it.hasNext()){
			String key = it.next();
			List<ObjectRecord> result = objectRecordDAO.getBatch(key, type);
			if(result.equals(records)){
				found = true;
			}
		}
		assertTrue("Failed to find the object record pushed by the queue.",found);
	}

}
