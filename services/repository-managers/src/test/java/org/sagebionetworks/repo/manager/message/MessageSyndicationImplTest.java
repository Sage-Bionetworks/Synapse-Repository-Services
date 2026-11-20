package org.sagebionetworks.repo.manager.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.PublishResult;
import org.sagebionetworks.repo.model.message.PublishResults;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;

/**
 * @author xschildw
 *
 */
@ExtendWith(MockitoExtension.class)
public class MessageSyndicationImplTest {
	
	@Mock
	private RepositoryMessagePublisher mockMsgPublisher;
	@Mock
	private SqsClient mockSqsClient;
	@Mock
	private DBOChangeDAO mockChgDAO;
	@InjectMocks
	private MessageSyndicationImpl msgSyndicationImpl;
	
	
	@Test
	public void testRefireChangeMessagesWithNullStartChangeNumber() {
		assertThrows(IllegalArgumentException.class, () -> {			
			msgSyndicationImpl.rebroadcastChangeMessages(null, null);
		});
	}
	
	@Test
	public void testPastLastNumber(){
		// Return an empty list when past the last change.
		when(mockChgDAO.listChanges(100l, null, 100)).thenReturn(new LinkedList<ChangeMessage>());
		Long next = msgSyndicationImpl.rebroadcastChangeMessages(100l, 100l);
		Long expecedNext = -1l;
		assertEquals(expecedNext, next);
		verify(mockMsgPublisher, never()).fireChangeMessage(any(ChangeMessage.class));
	}
	
	@Test
	public void testUnderLimit(){
		List<ChangeMessage> expectedChanges = generateChanges(10, 123L);
		when(mockChgDAO.listChanges(123l, null, 10)).thenReturn(expectedChanges);
		when(mockChgDAO.getCurrentChangeNumber()).thenReturn(133L);
		Long next = msgSyndicationImpl.rebroadcastChangeMessages(123l, 10l);
		Long expecedNext = 133l;
		assertEquals(expecedNext, next);
		verify(mockMsgPublisher, times(10)).fireChangeMessage(any(ChangeMessage.class));
	}
	
	@Test
	public void testOverLimit(){
		List<ChangeMessage> expectedChanges = generateChanges(10, 123L);
		when(mockChgDAO.listChanges(123l, null, 10)).thenReturn(expectedChanges);
		when(mockChgDAO.getCurrentChangeNumber()).thenReturn(132L);
		Long next = msgSyndicationImpl.rebroadcastChangeMessages(123l, 10l);
		Long expecedNext = -1l;
		assertEquals(expecedNext, next);
		verify(mockMsgPublisher, times(10)).fireChangeMessage(any(ChangeMessage.class));
	}
	

	@Test
	public void testGetLastChangeNumber() {
		long startNum = 2345;
		int numMsgs = 34;
		List<ChangeMessage> expectedChanges = generateChanges(numMsgs, startNum);
		when(mockChgDAO.getCurrentChangeNumber()).thenReturn(new Long(startNum + expectedChanges.size() - 1));
		long currentChangeMsgNum = msgSyndicationImpl.getCurrentChangeNumber();
		assertEquals((startNum + numMsgs - 1l), currentChangeMsgNum);
	}
	
	@Test
	public void testLookupQueueUrlInvalid() {
		String qName = "qName";
		when(mockSqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(qName).build()))
				.thenThrow(QueueDoesNotExistException.builder().message("Could not find queue.").build());
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// call under test
			msgSyndicationImpl.lookupQueueURL(qName);
		}).getMessage();
		
		assertEquals("Failed to find a queue named: qName", message);
	}
	
	@Test
	public void testLookupQueueUrlValid() {
		String qName = "qName";
		String qUrl = "https://sqs.us-east-1.amazonaws.com/123/qName";
		GetQueueUrlResponse res = GetQueueUrlResponse.builder().queueUrl(qUrl).build();
		when(mockSqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(qName).build())).thenReturn(res);
		// call under test
		String s = msgSyndicationImpl.lookupQueueURL(qName);
		assertEquals(qUrl, s);
	}
	
	@Test
	public void testRebroadcastChangeMessagesToQueue() throws JSONObjectAdapterException {
		String qName = "qName";
		String qUrl = "https://sqs.us-east-1.amazonaws.com/123/qName";
		List<ChangeMessage> changes = generateChanges(2, 10L);
		
		when(mockSqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(qName).build()))
				.thenReturn(GetQueueUrlResponse.builder().queueUrl(qUrl).build());
		when(mockChgDAO.listChanges(10L, ObjectType.ENTITY, 2L)).thenReturn(changes);
		
		SendMessageBatchRequest expectedRequest = SendMessageBatchRequest.builder()
				.queueUrl(qUrl)
				.entries(
					SendMessageBatchRequestEntry.builder().id("0").messageBody(EntityFactory.createJSONStringForEntity(changes.get(0))).build(),
					SendMessageBatchRequestEntry.builder().id("1").messageBody(EntityFactory.createJSONStringForEntity(changes.get(1))).build()
				).build();
		
		when(mockSqsClient.sendMessageBatch(expectedRequest)).thenReturn(SendMessageBatchResponse.builder()
				.successful(SendMessageBatchResultEntry.builder().id("0").build())
				.failed(BatchResultErrorEntry.builder().id("1").build())
				.build());
		
		PublishResults expected = new PublishResults().setList(Arrays.asList(
				new PublishResult().setChangeNumber(10L).setSuccess(true),
				new PublishResult().setChangeNumber(11L).setSuccess(false)
		));
		
		// call under test
		PublishResults result = msgSyndicationImpl.rebroadcastChangeMessagesToQueue(qName, ObjectType.ENTITY, 10L, 2L);
		
		assertEquals(expected, result);
		verify(mockSqsClient).sendMessageBatch(expectedRequest);
	}
	
	@Test
	public void testPrepareResults() {
		
		List<ChangeMessage> changeMessages = Arrays.asList(
				new ChangeMessage().setChangeNumber(5L),
				new ChangeMessage().setChangeNumber(1L)
		);
		
		SendMessageBatchResponse batchResult = SendMessageBatchResponse.builder()
				.successful(
					SendMessageBatchResultEntry.builder().id("0").build(),
					SendMessageBatchResultEntry.builder().id("1").build()
				).build();
		
		List<PublishResult> expected = Arrays.asList(
				new PublishResult().setChangeNumber(5L).setSuccess(true),
				new PublishResult().setChangeNumber(1L).setSuccess(true)
		);
		
		// Call under test
		List<PublishResult> result = msgSyndicationImpl.prepareResults(changeMessages, batchResult);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testPrepareResultsOutOfOrder() {
		
		List<ChangeMessage> changeMessages = Arrays.asList(
				new ChangeMessage().setChangeNumber(5L),
				new ChangeMessage().setChangeNumber(1L)
		);
		
		// The SendMessageBatchResponse can contain out of order elements, in this
		// case the result for the second message appears first in the list
		SendMessageBatchResponse batchResult = SendMessageBatchResponse.builder()
				.successful(
					SendMessageBatchResultEntry.builder().id("1").build(),
					SendMessageBatchResultEntry.builder().id("0").build()
				).build();
		
		List<PublishResult> expected = Arrays.asList(
				new PublishResult().setChangeNumber(5L).setSuccess(true),
				new PublishResult().setChangeNumber(1L).setSuccess(true)
		);
		
		// Call under test
		List<PublishResult> result = msgSyndicationImpl.prepareResults(changeMessages, batchResult);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testPrepareResultsWithFailures() {
		
		List<ChangeMessage> changeMessages = Arrays.asList(
				new ChangeMessage().setChangeNumber(5L),
				new ChangeMessage().setChangeNumber(1L)
		);
		
		SendMessageBatchResponse batchResult = SendMessageBatchResponse.builder()
				.failed(
					BatchResultErrorEntry.builder().id("0").build()
				)
				.successful(
					SendMessageBatchResultEntry.builder().id("1").build()
				).build();
		
		List<PublishResult> expected = Arrays.asList(
				new PublishResult().setChangeNumber(5L).setSuccess(false),
				new PublishResult().setChangeNumber(1L).setSuccess(true)
		);
		
		// Call under test
		List<PublishResult> result = msgSyndicationImpl.prepareResults(changeMessages, batchResult);
		
		assertEquals(expected, result);
	}
	
	/**
	 * Generates a list of numChanges change messages starting at startChangeNumber
	 * @param numChanges
	 * @param startChangeNumber
	 * @return List<ChangeMessage>
	 */
	private List<ChangeMessage> generateChanges(int numChanges, Long startChangeNumber) {
		List<ChangeMessage> changes = new ArrayList<ChangeMessage>();
		for (int i = 0; i < numChanges; i++) {
			ChangeMessage chgMsg = new ChangeMessage();
			chgMsg.setChangeNumber(startChangeNumber + i);
			chgMsg.setObjectType(ObjectType.ENTITY);
			changes.add(chgMsg);
		}
		return changes;
	}

}
