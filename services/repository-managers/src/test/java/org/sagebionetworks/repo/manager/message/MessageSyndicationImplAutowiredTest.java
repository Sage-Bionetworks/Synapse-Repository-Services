package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOColumnModel;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.PublishResult;
import org.sagebionetworks.repo.model.message.PublishResults;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MessageSyndicationImplAutowiredTest {
	
	public static final long MAX_WAIT = 10*1000; //ten seconds
	
	@Autowired
	MessageSyndication messageSyndication;
	
	@Autowired
	AmazonSQSClient awsSQSClient;
	
	@Autowired
	DBOChangeDAO changeDAO;
	
	@Autowired
	ColumnModelDAO columnModelDao;

	@Resource(name = "txManager")
	private PlatformTransactionManager transactionManager;

	private String queueName = StackConfiguration.getStack()+"-"+StackConfiguration.getStackInstance()+"-test-syndication";
	private String queueUrl;

	private TransactionTemplate transactionTemplate;
	
	@Before
	public void before(){
		// Create the queue if it does not exist
		CreateQueueResult cqr = awsSQSClient.createQueue(new CreateQueueRequest(queueName));
		queueUrl = cqr.getQueueUrl();
		System.out.println("Queue Name: "+queueName);
		System.out.println("Queue URL: "+queueUrl);

		DefaultTransactionDefinition transactionDef = new DefaultTransactionDefinition();
		transactionDef.setReadOnly(false);
		transactionDef.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		transactionDef.setName("CountingSemaphoreDao");
		// This will manage transactions for calls that need it.
		transactionTemplate = new TransactionTemplate(this.transactionManager, transactionDef);

	}

	public void doSomething() throws Exception {
		this.transactionTemplate.execute(new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				try {
					columnModelDao.createColumnModel(TableModelTestUtils.createColumn(null, "col" + UUID.randomUUID(), ColumnType.STRING));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				createChangeMessages(15L, ObjectType.ENTITY);
				return null;
			}
		});
	}

	@Test
	public void testDeadlock() throws Exception {
		List<Callable<Void>> tasks = Lists.newArrayList();
		int count = 10;
		final CountDownLatch latch = new CountDownLatch(count);
		for (int i = 0; i < count; i++) {
			tasks.add(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					latch.countDown();
					latch.await();
					for (int i = 0; i < 10; i++) {
						doSomething();
					}
					return null;
				}
			});
		}
		ExecutorService executor = Executors.newCachedThreadPool();
		for (Future<Void> result : executor.invokeAll(tasks)) {
			result.get();
		}
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);
	}

	@Test
	public void testRebroadcastAllChangeMessagesToQueue() throws InterruptedException{
		// Make sure the queue starts empty.
		emptyQueue();
		assertEquals(0, getQueueMessageCount());
		// Start with no change messages
		changeDAO.deleteAllChanges();
		// Create a bunch of messages
		long toCreate = 15l;
		List<ChangeMessage> created = createChangeMessages(toCreate, ObjectType.ENTITY);
		ChangeMessages allMessages = messageSyndication.listChanges(0l,  ObjectType.ENTITY,  Long.MAX_VALUE);
		assertNotNull(allMessages);
		assertNotNull(allMessages.getList());
		assertEquals(toCreate, allMessages.getList().size());
		// Now push all of these to the queue
		PublishResults results = messageSyndication.rebroadcastChangeMessagesToQueue(queueName, ObjectType.ENTITY, 0l, Long.MAX_VALUE);
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals(toCreate, results.getList().size());
		waitForMessageCount(toCreate);
		
		// Now send a single message.
		ChangeMessage toTest = created.get(13);
		// Now push all of these to the queue
		results = messageSyndication.rebroadcastChangeMessagesToQueue(queueName, ObjectType.ENTITY, toTest.getChangeNumber(), 1l);
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals(1, results.getList().size());
		// Validate that the correct message was sent
		PublishResult pr = results.getList().get(0);
		assertNotNull(pr);
		assertEquals(toTest.getChangeNumber(), pr.getChangeNumber());
		
		// Test a page
		ChangeMessage start = created.get(2);
		Long limit = 11l;
		// Now push all of these to the queue
		results = messageSyndication.rebroadcastChangeMessagesToQueue(queueName, ObjectType.ENTITY, start.getChangeNumber(), limit);
		
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals(limit.intValue(), results.getList().size());
		// validate the results
		assertEquals(start.getChangeNumber(), results.getList().get(0).getChangeNumber());
		assertEquals(created.get(2+11-1).getChangeNumber(), results.getList().get(limit.intValue()-1).getChangeNumber());
	}
	
	
	@After
	public void after(){
		if(changeDAO != null){
			changeDAO.deleteAllChanges();
		}
	}
	
	/**
	 * Create the given number of messages.
	 * 
	 * @param count
	 * @param type
	 */
	public List<ChangeMessage> createChangeMessages(long count, ObjectType type){
		List<ChangeMessage> results = new ArrayList<ChangeMessage>();
		for(int i=0; i<count; i++){
			ChangeMessage message = new ChangeMessage();
			message.setObjectType(type);
			message.setObjectId(""+i);
			// Use all types
			message.setChangeType(ChangeType.values()[i%ChangeType.values().length]);
			message.setObjectEtag("etag"+i);
			results.add(changeDAO.replaceChange(message));
		}
		return results;
	}
	
	/**
	 * Wait for a given message count;
	 * @param expectedCount
	 * @throws InterruptedException
	 */
	public void waitForMessageCount(long expectedCount) throws InterruptedException{
		long start = System.currentTimeMillis();
		long count;
		do{
			count = getQueueMessageCount();
			System.out.println("Waiting for expected message count...");
			Thread.sleep(1000);
			assertTrue("Timed out waiting for the expected message count", System.currentTimeMillis()-start < MAX_WAIT);
		}while(count != expectedCount);
	}
	
	public long getQueueMessageCount(){
		GetQueueAttributesResult result = awsSQSClient.getQueueAttributes(new GetQueueAttributesRequest(queueUrl).withAttributeNames("ApproximateNumberOfMessages"));
		return Long.parseLong(result.getAttributes().get("ApproximateNumberOfMessages"));
	}
	
	/**
	 * Helper to empty the message queue
	 */
	public void emptyQueue(){
		ReceiveMessageResult result = null;
		do{
			result = awsSQSClient.receiveMessage(new ReceiveMessageRequest(queueUrl).withMaxNumberOfMessages(10).withVisibilityTimeout(100));
			List<Message> list = result.getMessages();
			if(list.size() > 0){
				List<DeleteMessageBatchRequestEntry> batch = new LinkedList<DeleteMessageBatchRequestEntry>();
				for(int i=0; i< list.size(); i++){
					Message message = list.get(i);
					// Delete all of them.
					batch.add(new DeleteMessageBatchRequestEntry(""+i, message.getReceiptHandle()));
				}
				awsSQSClient.deleteMessageBatch(new DeleteMessageBatchRequest(queueUrl, batch));
			}
			System.out.println("Deleted "+list.size()+" messages");
		}while(result.getMessages().size() > 0);
	}

}
