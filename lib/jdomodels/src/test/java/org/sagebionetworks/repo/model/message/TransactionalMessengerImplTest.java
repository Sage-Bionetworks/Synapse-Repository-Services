package org.sagebionetworks.repo.model.message;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/**
 * Unit (mocked) test for TransactionalMessengerImpl.
 * @author jmhill
 *
 */
public class TransactionalMessengerImplTest {
	
	DataSourceTransactionManager mockTxManager;
	DBOChangeDAO mockChangeDAO;
	TransactionSynchronizationProxy stubProxy;
	TransactionalMessengerObserver mockObserver;
	private TransactionalMessengerImpl messenger;
	
	@Before
	public void before(){
		mockTxManager = Mockito.mock(DataSourceTransactionManager.class);
		mockChangeDAO = Mockito.mock(DBOChangeDAO.class);
		stubProxy = new TransactionSynchronizationProxyStub();
		mockObserver = Mockito.mock(TransactionalMessengerObserver.class);
		messenger = new TransactionalMessengerImpl(mockTxManager, mockChangeDAO, stubProxy);
		messenger.registerObserver(mockObserver);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testOetObjectKeyNull(){
		messenger.getObjectKey(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testOetObjectKeyNullId(){
		messenger.getObjectKey(new ChangeMessage());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testOetObjectKeyNullType(){
		ChangeMessage message = new ChangeMessage();
		message.setObjectId("notNull");
		messenger.getObjectKey(message);
	}
	
	@Test
	public void testOetObject(){
		ChangeMessage message = new ChangeMessage();
		String id = "syn123";
		message.setObjectId(id);
		message.setObjectType(ObjectType.ENTITY);
		String key = messenger.getObjectKey(message);
		assertEquals(ObjectType.ENTITY.name()+"-"+id, key);
	}
	
	@Test
	public void testSendMessage(){
		ChangeMessage message = new ChangeMessage();
		message.setChangeNumber(new Long(123));
		message.setTimestamp(new Date(System.currentTimeMillis()/1000*1000));
		message.setObjectEtag("etag");
		message.setObjectId("syn456");
		message.setObjectType(ObjectType.ENTITY);
		message.setChangeType(ChangeType.DELETE);
		// Send the message
		messenger.sendMessageAfterCommit(message);
		assertNotNull(stubProxy.getSynchronizations());
		assertEquals(1, stubProxy.getSynchronizations().size());
		// Simulate the before commit
		stubProxy.getSynchronizations().get(0).beforeCommit(true);
		List<ChangeMessage> list = new ArrayList<ChangeMessage>();
		list.add(message);
		verify(mockChangeDAO, times(1)).replaceChange(list);
		// Simulate the after commit
		stubProxy.getSynchronizations().get(0).afterCommit();
		verify(mockObserver, times(1)).fireChangeMessage(message);
	}

}
