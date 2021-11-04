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

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.BatchResultErrorEntry;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageBatchResultEntry;

/**
 * @author xschildw
 *
 */
@ExtendWith(MockitoExtension.class)
public class MessageSyndicationImplTest {
	
	@Mock
	private RepositoryMessagePublisher mockMsgPublisher;
	@Mock
	private AmazonSQS mockSqsClient;
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
		when(mockSqsClient.getQueueUrl(qName)).thenThrow(new QueueDoesNotExistException("Could not find queue."));
		
		assertThrows(IllegalArgumentException.class, () -> {			
			msgSyndicationImpl.lookupQueueURL(qName);
		});
	}
	
	@Test
	public void testLookupQueueUrlValid() {
		String qName = "qName";
		GetQueueUrlResult res = new GetQueueUrlResult().withQueueUrl(qName);
		when(mockSqsClient.getQueueUrl(qName)).thenReturn(res);
		String s = msgSyndicationImpl.lookupQueueURL(qName);
		assertEquals(qName, s);
	}
	
	@Test
	public void testPrepareResults() {
		
		List<ChangeMessage> changeMessages = Arrays.asList(
				new ChangeMessage().setChangeNumber(5L),
				new ChangeMessage().setChangeNumber(1L)
		);
		
		SendMessageBatchResult batchResult = new SendMessageBatchResult()
				.withSuccessful(
					new SendMessageBatchResultEntry().withId("0"),
					new SendMessageBatchResultEntry().withId("1")
				);
		
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
		
		// The SendMessageBatchResult can contain out of order elements, in this
		// case the result for the second message appears first in the list
		SendMessageBatchResult batchResult = new SendMessageBatchResult()
				.withSuccessful(
					new SendMessageBatchResultEntry().withId("1"),
					new SendMessageBatchResultEntry().withId("0")
				);
		
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
		
		SendMessageBatchResult batchResult = new SendMessageBatchResult()
				.withFailed(
					new BatchResultErrorEntry().withId("0")
				)
				.withSuccessful(
					new SendMessageBatchResultEntry().withId("1")
				);
		
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
