package org.sagebionetworks.repo.manager.replication;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.message.ChangeMessageUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.ChangeType;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.amazonaws.services.sqs.model.SendMessageRequest;

@ExtendWith(MockitoExtension.class)
public class ReplicationMessageManagerImplTest {

	@Mock
	AmazonSQS mockSqsClient;

	@Mock
	StackConfiguration mockConfig;
	
	@InjectMocks
	ReplicationMessageManagerImpl manager;

	String replicationQueueUrl;
	String reconciliationQueueUrl;
	
	long messageCount;
	Map<String, String> queueAttributes;

	@BeforeEach
	public void before() {
		String replicationQueueName = "replicationQueueName";
		String reconciliationQueueName = "reconciliationQueueName";
		
		replicationQueueUrl = "replicationQueueUrl";
		reconciliationQueueUrl = "reconciliationQueueUrl";
		
		when(mockConfig.getQueueName(ReplicationMessageManagerImpl.REPLICATION_QUEUE_NAME))
				.thenReturn(replicationQueueName);
		when(mockConfig.getQueueName(ReplicationMessageManagerImpl.RECONCILIATION_QUEUE_NAME))
				.thenReturn(reconciliationQueueName);

		when(mockSqsClient.getQueueUrl(replicationQueueName))
				.thenReturn(new GetQueueUrlResult().withQueueUrl(replicationQueueUrl));

		when(mockSqsClient.getQueueUrl(reconciliationQueueName))
				.thenReturn(new GetQueueUrlResult().withQueueUrl(reconciliationQueueUrl));

		manager.initialize();
		
		assertEquals(replicationQueueUrl, manager.replicationQueueUrl);
		assertEquals(reconciliationQueueUrl, manager.reconciliationQueueUrl);
		
		messageCount = 99L;
		queueAttributes = new HashMap<>(0);
		queueAttributes.put(QueueAttributeName.ApproximateNumberOfMessages.name(), ""+ messageCount);
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
	
	@Test
	public void testPushChangeMessagesToReplicationQueueNull(){
		List<ChangeMessage> empty = null;
		
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.pushChangeMessagesToReplicationQueue(empty);
		});
	}
	
	@Test
	public void testGetApproximateNumberOfMessageOnReplicationQueue() {
		when(mockSqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
				.thenReturn(new GetQueueAttributesResult().withAttributes(queueAttributes));
		// call under test
		long count = manager.getApproximateNumberOfMessageOnReplicationQueue();
		assertEquals(messageCount, count);
	}
	
	@Test
	public void testGetApproximateNumberOfMessageOnReplicationQueueMissingAttribute() {
		this.queueAttributes.clear();
		
		when(mockSqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
				.thenReturn(new GetQueueAttributesResult().withAttributes(queueAttributes));
		
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getApproximateNumberOfMessageOnReplicationQueue();
		});
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
