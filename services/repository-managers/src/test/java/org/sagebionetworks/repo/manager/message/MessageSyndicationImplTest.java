package org.sagebionetworks.repo.manager.message;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;

import org.mockito.Mockito;
import static org.mockito.Mockito.*;

import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;

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
	public void testReFireChangeMessagesSingleBatch() {
		List<ChangeMessage> expectedChanges = generateChanges(10, 123L);
		when(mockChgDAO.listChanges(123L, ObjectType.ENTITY, 100)).thenReturn(expectedChanges);
		Long nextChgNum = msgSyndicationImpl.rebroadcastChangeMessages(123L, 10L);
		verify(mockMsgPublisher, times(10)).fireChangeMessage(any(ChangeMessage.class));
		nextChgNum--; // To get around assertEquals ambiguous
		assertEquals((expectedChanges.get(9).getChangeNumber()), nextChgNum);
	}
	
	@Test
	public void testRefireChangeMessagesTwoBatches() {
		List<ChangeMessage> expectedChanges = generateChanges(137, 100L);
		List<ChangeMessage> expectedB1 = getBatchFromChanges(expectedChanges, 100, 0);
		List<ChangeMessage> expectedB2 = getBatchFromChanges(expectedChanges, 100, 1);
		when(mockChgDAO.listChanges(100L, ObjectType.ENTITY, 100)).thenReturn(expectedB1);
		when(mockChgDAO.listChanges(200L, ObjectType.ENTITY, 100)).thenReturn(expectedB2);
		Long nextChgNum = msgSyndicationImpl.rebroadcastChangeMessages(100L, 137L);
		verify(mockMsgPublisher, times(137)).fireChangeMessage(any(ChangeMessage.class));
		nextChgNum--; // To get around assertEquals ambiguous
		assertEquals((expectedChanges.get(expectedChanges.size()-1).getChangeNumber()), nextChgNum);
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
