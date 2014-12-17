package org.sagebionetworks.acl.worker;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.audit.dao.AclRecordDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.sqs.model.Message;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-context.xml", "classpath:audit-dao.spb.xml"})
public class AclSnapshotWorkerTest {

	@Autowired
	private AclRecordDAO aclRecordDao;
	
	@Autowired
	private AmazonS3Client s3Client;
	
	private String BUCKET_NAME = "prod.acl.record.sagebase.org";

	@Before
	public void before() {
		
		assertNotNull(aclRecordDao);
		
		assertNotNull(s3Client);
		assertTrue(s3Client.doesBucketExist(BUCKET_NAME));
	}
	
	@After
	public void after() {
	}
	
	@Test
	public void testWrongObjectType() throws Exception {
		ObjectListing objListing = s3Client.listObjects(BUCKET_NAME);
		Message one = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.ACTIVITY, "etag");
		Message two = MessageUtils.buildMessage(ChangeType.CREATE, "456", ObjectType.ACTIVITY, "etag");
		List<Message> messages = Arrays.asList(one, two);
		// Create the worker
		AclSnapshotWorker worker = createNewWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		// It should just return the results unchanged
		assertNotNull(results);
		assertEquals(messages, results);
		assertEquals(objListing.getObjectSummaries().size(), s3Client.listObjects(BUCKET_NAME).getObjectSummaries().size());
	}
	
	@Test
	public void testCorrectObjectType() throws Exception {
		ObjectListing objListing = s3Client.listObjects(BUCKET_NAME);
		Message three = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.ACCESS_CONTROL_LIST, "etag");
		Message four = MessageUtils.buildMessage(ChangeType.CREATE, "456", ObjectType.ACCESS_CONTROL_LIST, "etag");
		Message five = MessageUtils.buildMessage(ChangeType.CREATE, "789", ObjectType.ACCESS_CONTROL_LIST, "etag");
		List<Message> messages = Arrays.asList(three, four,five);
		AclSnapshotWorker worker = createNewWorker(messages);
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals(messages, results);
		assertTrue(objListing.getObjectSummaries().size() + 3 == s3Client.listObjects(BUCKET_NAME).getObjectSummaries().size());
	}
	
	/**
	 * Helper to create a new worker for a list of messages.
	 * @param messages
	 * @return
	 */
	public AclSnapshotWorker createNewWorker(List<Message> messages){
		AclSnapshotWorker aclWorker = new AclSnapshotWorker();
		aclWorker.setWorkerProgress(new WorkerProgress() {
			@Override
			public void progressMadeForMessage(Message message) {}

			@Override
			public void retryMessage(Message message, int retryTimeoutInSeconds) {}
		});
		ReflectionTestUtils.setField(aclWorker, "aclRecordDao", aclRecordDao);
		aclWorker.setMessages(messages);
		return aclWorker;
	}

}
