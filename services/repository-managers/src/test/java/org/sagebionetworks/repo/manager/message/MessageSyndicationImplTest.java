package org.sagebionetworks.repo.manager.message;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
	
	@Test
	public void testReFireChangeMessages() {
		List<ChangeMessage> expectedChanges = generateChanges(10, 123L);
		when(mockChgDAO.listChanges(123L, ObjectType.ENTITY, 10)).thenReturn(expectedChanges);
		Long lastChgNum = msgSyndicationImpl.rebroadcastChangeMessages(123L, 10L);
		verify(mockMsgPublisher, times(10)).fireChangeMessage(any(ChangeMessage.class));
		
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
