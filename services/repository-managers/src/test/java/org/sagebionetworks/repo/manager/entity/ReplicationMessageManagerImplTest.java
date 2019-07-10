package org.sagebionetworks.repo.manager.entity;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.message.ChangeMessageUtils;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
public class ReplicationMessageManagerImplTest {

	@Mock
	AmazonSQS mockSqsClient;

	ReplicationMessageManagerImpl manager;

	String replicationQueueName;
	String replicationQueueUrl;

	String reconciliationQueueName;
	String reconciliationQueueUrl;
	
	long messageCount;
	Map<String, String> queueAttributes;

	@Before
	public void before() {
		manager = new ReplicationMessageManagerImpl();
		ReflectionTestUtils.setField(manager, "sqsClient", mockSqsClient);

		replicationQueueName = "replicationQueueName";
		replicationQueueUrl = "replicationQueueUrl";
		reconciliationQueueName = "reconciliationQueueName";
		reconciliationQueueUrl = "reconciliationQueueUrl";

		when(mockSqsClient.getQueueUrl(replicationQueueName)).thenReturn(
				new GetQueueUrlResult().withQueueUrl(replicationQueueUrl));
		
		when(mockSqsClient.getQueueUrl(reconciliationQueueName)).thenReturn(
				new GetQueueUrlResult().withQueueUrl(reconciliationQueueUrl));

		manager.setReplicationQueueName(replicationQueueName);
		manager.setReconciliationQueueName(reconciliationQueueName);
		manager.initialize();
		
		assertEquals(replicationQueueUrl, manager.replicationQueueUrl);
		assertEquals(reconciliationQueueUrl, manager.reconciliationQueueUrl);
		
		messageCount = 99L;
		queueAttributes = new HashMap<>(0);
		queueAttributes.put(QueueAttributeName.ApproximateNumberOfMessages.name(), ""+ messageCount);
		when(mockSqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
				.thenReturn(new GetQueueAttributesResult().withAttributes(queueAttributes));
	}
	
	@Test
	public void testPushChangeMessagesToReplicationQueue(){
		// single message
		List<ChangeMessage> toPush = createMessages(1);
		// call under test
		manager.pushChangeMessagesToReplicationQueue(toPush);
		ChangeMessages messages = new ChangeMessages();
		messages.setList(toPush);
		String expectedBody = manager.createMessageBodyJSON(messages);
		verify(mockSqsClient, times(1)).sendMessage(new SendMessageRequest(replicationQueueUrl,
				expectedBody));
	}
	
	@Test
	public void testPushChangeMessagesToReplicationQueueOverMax(){
		// pushing more than the max messages should result in multiple batches.
		List<ChangeMessage> toPush = createMessages(ChangeMessageUtils.MAX_NUMBER_OF_CHANGE_MESSAGES_PER_SQS_MESSAGE+1);
		// call under test
		manager.pushChangeMessagesToReplicationQueue(toPush);
		verify(mockSqsClient, times(2)).sendMessage(any(SendMessageRequest.class));
	}
	
	@Test
	public void testPushChangeMessagesToReplicationQueueEmpty(){
		List<ChangeMessage> empty = new LinkedList<ChangeMessage>();
		// call under test
		manager.pushChangeMessagesToReplicationQueue(empty);
		verify(mockSqsClient, never()).sendMessage(any(SendMessageRequest.class));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testPushChangeMessagesToReplicationQueueNull(){
		List<ChangeMessage> empty = null;
		// call under test
		manager.pushChangeMessagesToReplicationQueue(empty);
	}
	
	@Test
	public void testPushContainerIdsToReconciliationQueue(){
		// single message
		List<Long> toPush = Lists.newArrayList(111L);
		// call under test
		manager.pushContainerIdsToReconciliationQueue(toPush);
		IdList messages = new IdList();
		messages.setList(toPush);
		String expectedBody = manager.createMessageBodyJSON(messages);
		verify(mockSqsClient, times(1)).sendMessage(new SendMessageRequest(reconciliationQueueUrl,
				expectedBody));
	}
	
	@Test
	public void testPushContainerIdsToReconciliationQueueOverMax(){
		// pushing more than the max messages should result in multiple batches.
		List<Long> toPush = createLongMessages(ReplicationMessageManagerImpl.MAX_CONTAINERS_IDS_PER_RECONCILIATION_MESSAGE+1);
		// call under test
		manager.pushContainerIdsToReconciliationQueue(toPush);
		verify(mockSqsClient, times(2)).sendMessage(any(SendMessageRequest.class));
	}
	
	@Test
	public void testPushContainerIdsToReconciliationQueueEmpty(){
		List<Long> empty = new LinkedList<Long>();
		// call under test
		manager.pushContainerIdsToReconciliationQueue(empty);
		verify(mockSqsClient, never()).sendMessage(any(SendMessageRequest.class));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testPushContainerIdsToReconciliationQueueNull(){
		List<Long> empty = null;
		// call under test
		manager.pushContainerIdsToReconciliationQueue(empty);
	}
	
	@Test
	public void testGetApproximateNumberOfMessageOnReplicationQueue() {
		// call under test
		long count = manager.getApproximateNumberOfMessageOnReplicationQueue();
		assertEquals(messageCount, count);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetApproximateNumberOfMessageOnReplicationQueueMissingAttribute() {
		this.queueAttributes.clear();
		// call under test
		manager.getApproximateNumberOfMessageOnReplicationQueue();
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
			message.setObjectId("id"+i);
			message.setObjectType(ObjectType.ENTITY);
			list.add(message);
		}
		return list;
	}
	
	/**
	 * Helper to create a list of longs.
	 * @param count
	 * @return
	 */
	public List<Long> createLongMessages(int count){
		List<Long> results = new LinkedList<Long>();
		for(long i=0; i<count; i++){
			results.add(i);
		}
		return results;
	}

}
