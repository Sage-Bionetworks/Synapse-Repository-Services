package org.sagebionetworks.worker.entity;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.message.ChangeMessageUtils;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Unit test for EntityReplicationDeltaWorker.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class EntityReplicationDeltaWorkerTest {

	@Mock
	NodeDAO mockNodeDao;
	
	@Mock
	ConnectionFactory mockConnectionFactory;
	
	@Mock
	TableIndexDAO mockIndexDao;
	
	@Mock
	AmazonSQSClient mockSqsClient;
	
	@Mock
	WorkerLogger mockWorkerLog;
	
	@Mock
	ProgressCallback<Void> mockProgressCallback;
	
	String queueName;
	String queueUrl;
	
	EntityReplicationDeltaWorker worker;
	
	@Before
	public void before(){
		worker = new EntityReplicationDeltaWorker();
		ReflectionTestUtils.setField(worker, "nodeDao", mockNodeDao);
		ReflectionTestUtils.setField(worker, "connectionFactory", mockConnectionFactory);
		ReflectionTestUtils.setField(worker, "sqsClient", mockSqsClient);
		ReflectionTestUtils.setField(worker, "workerLogger", mockWorkerLog);
		
		when(mockConnectionFactory.getAllConnections()).thenReturn(Lists.newArrayList(mockIndexDao));
		
		queueName = "someQueueName";
		queueUrl = "someQueueUrl";
		worker.setQueueName(queueName);
		CreateQueueResult cqr = new CreateQueueResult();
		cqr.setQueueUrl(queueUrl);
		when(mockSqsClient.createQueue(queueName)).thenReturn(cqr);
	}
	
	@Test
	public void testLazyLoadQueueUrl(){
		// call under test
		String loadedUrl = worker.lazyLoadQueueUrl();
		assertEquals(queueUrl, loadedUrl);
		// multiple calls should only load the URL once.
		loadedUrl = worker.lazyLoadQueueUrl();
		loadedUrl = worker.lazyLoadQueueUrl();
		// should only be called once.
		verify(mockSqsClient, times(1)).createQueue(queueName);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testLazyLoadQueueUrlNullQueueName(){
		worker.setQueueName(null);
		// call under test
		worker.lazyLoadQueueUrl();
	}
	
	@Test
	public void testPushMessagesToQueueOverLimit() throws JSONObjectAdapterException{
		// Create more messages than the max per batch.
		List<ChangeMessage> changes = createMessages(ChangeMessageUtils.MAX_NUMBER_OF_CHANGE_MESSAGES_PER_SQS_MESSAGE+1);
		// call under test
		worker.pushMessagesToQueue(changes);
		// two batches are needed to send all of the messages.
		verify(mockSqsClient, times(2)).sendMessage(any(SendMessageRequest.class));
	}
	
	@Test
	public void testPushMessagesToQueue() throws JSONObjectAdapterException{
		// create a single message
		List<ChangeMessage> changes = createMessages(1);
		// call under test
		worker.pushMessagesToQueue(changes);
		ChangeMessages messages = new ChangeMessages();
		messages.setList(changes);
		String messageBody = EntityFactory.createJSONStringForEntity(messages);
		SendMessageRequest expected = new SendMessageRequest(queueUrl, messageBody);
		verify(mockSqsClient).sendMessage(expected);
	}
	
	@Test
	public void testCompareCheckSums(){
		// truth
		Map<Long, Long> truthCRCs = new HashMap<Long, Long>();
		truthCRCs.put(1L, 111L);
		truthCRCs.put(2L, 222L);
		truthCRCs.put(3L, 333L);
		truthCRCs.put(4L, 333L);
		truthCRCs.put(6L, 666L);
		truthCRCs.put(6L, 666L);
		when(mockNodeDao.getSumOfChildCRCsForEachParent(anyListOf(Long.class))).thenReturn(truthCRCs);
		// replica
		Map<Long, Long> replicaCRCs = new HashMap<Long, Long>();
		// 1 is missing
		// 2 matches
		replicaCRCs.put(2L, 222L);
		// 3 matches but is in the trash.
		replicaCRCs.put(3L, 333L);
		// 4 does not match
		replicaCRCs.put(4L, -444L);
		// 5 is not in the truth.
		replicaCRCs.put(5L, 555L);
		// 6 is missing
		when(mockIndexDao.getSumOfChildCRCsForEachParent(anyListOf(Long.class))).thenReturn(replicaCRCs);
		
		List<Long> parentIds = Lists.newArrayList(1L,2L,3L,4L,5L,6L);
		Set<Long> trashedParents = Sets.newHashSet(3L, 6L);
		// call under test
		Set<Long> results = worker.compareCheckSums(mockProgressCallback, parentIds, mockIndexDao, trashedParents);
		assertNotNull(results);
		// 1 is in the truth but not replica
		assertTrue(results.contains(1L));
		// 2 is the same in the truth and replica
		assertFalse(results.contains(2L));
		// 3 three is in the trash and the replica
		assertTrue(results.contains(3L));
		// 4 is in both but does not match
		assertTrue(results.contains(5L));
		// 5 is in the replica but not the truth.
		assertTrue(results.contains(5L));
		// 6 is in the trash and missing from the replica
		assertFalse(results.contains(6L));
	}
	
	/**
	 * Helper to create some messages.
	 * @param count
	 * @return
	 */
	public List<ChangeMessage> createMessages(int count){
		List<ChangeMessage> list = new LinkedList<ChangeMessage>();
		for(int i=0; i<count; i++){
			ChangeMessage message = new ChangeMessage();
			message.setChangeNumber(new Long(i));
			message.setChangeType(ChangeType.UPDATE);
			message.setObjectEtag("etag"+i);
			message.setObjectId("id"+i);
			message.setObjectType(ObjectType.ENTITY);
			list.add(message);
		}
		return list;
	}

}
