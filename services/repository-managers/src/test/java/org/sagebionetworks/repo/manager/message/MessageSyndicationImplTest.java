package org.sagebionetworks.repo.manager.message;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import org.mockito.Mockito;
import static org.mockito.Mockito.*;

import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.ObjectType;

import com.amazonaws.services.sqs.AmazonSQSClient;

/**
 * @author xschildw
 *
 */
public class MessageSyndicationImplTest {
	RepositoryMessagePublisher mockMsgPublisher;
	AmazonSQSClient mockSqsClient;
	DBOChangeDAO mockChgDAO;
	MessageSyndicationImpl msgSyndicationImpl;
	
	@Before
	public void before() {
		mockMsgPublisher = Mockito.mock(RepositoryMessagePublisher.class);
		mockSqsClient = Mockito.mock(AmazonSQSClient.class);
		mockChgDAO = Mockito.mock(DBOChangeDAO.class);
		msgSyndicationImpl = new MessageSyndicationImpl(mockMsgPublisher, mockSqsClient, mockChgDAO);
	}
	
	@After
	public void after() {
		
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testRefireChangeMessages() {
		msgSyndicationImpl.rebroadcastChangeMessages(null, null);
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
	
	private List<ChangeMessage> getBatchFromChanges(List<ChangeMessage> msgs, int bSize, int batchNum) {
		List<ChangeMessage> res = new ArrayList<ChangeMessage>();
		int startIdx = batchNum * bSize;
		for (int i = 0; i < bSize; i++) {
			if (startIdx+i >= msgs.size()) {
				break;
			}
			ChangeMessage cMsg = msgs.get(startIdx+i);
			res.add(cMsg);
		}
		
		return res;
	}

}
