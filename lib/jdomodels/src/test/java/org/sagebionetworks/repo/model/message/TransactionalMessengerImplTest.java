package org.sagebionetworks.repo.model.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.TestClock;
import org.sagebionetworks.util.ThreadLocalProvider;
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
	private TestClock testClock = new TestClock();
	
	@Before
	public void before(){
		mockTxManager = Mockito.mock(DataSourceTransactionManager.class);
		mockChangeDAO = Mockito.mock(DBOChangeDAO.class);
		stubProxy = new TransactionSynchronizationProxyStub();
		mockObserver = Mockito.mock(TransactionalMessengerObserver.class);
		messenger = new TransactionalMessengerImpl(mockTxManager, mockChangeDAO, stubProxy, testClock);
		messenger.registerObserver(mockObserver);
		ThreadLocalProvider.getInstance(AuthorizationConstants.USER_ID_PARAM, Long.class).set(null);
	}

	@After
	public void after() {
		ThreadLocalProvider.getInstance(AuthorizationConstants.USER_ID_PARAM, Long.class).set(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testMessageKeyNull() {
		new MessageKey(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testMessageKeyNullId() {
		new MessageKey(new ChangeMessage());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testChangeMessageKeyNullType(){
		ChangeMessage message = new ChangeMessage();
		message.setObjectId("notNull");
		new MessageKey(message);
	}
	
	@Test
	public void testMessageKeyEquals() {
		ChangeMessage message = new ChangeMessage();
		message.setObjectId("123");
		message.setObjectType(ObjectType.ENTITY);
		// Create the first key
		MessageKey one = new MessageKey(message);
		// Create the second key
		message = new ChangeMessage();
		message.setObjectId("123");
		message.setObjectType(ObjectType.ENTITY);
		MessageKey two = new MessageKey(message);
		assertEquals(one, two);
		// Third is not equals
		message = new ChangeMessage();
		message.setObjectId("456");
		message.setObjectType(ObjectType.ENTITY);
		MessageKey thrid = new MessageKey(message);
		assertNotSame(thrid, two);
		assertNotSame(two, thrid);
		
		// fourth is not equals
		message = new ChangeMessage();
		message.setObjectId("123");
		message.setObjectType(ObjectType.ACTIVITY);
		MessageKey fourth = new MessageKey(message);
		assertNotSame(fourth, two);
		assertNotSame(two, fourth);

		ModificationMessage modificationMessage = new DefaultModificationMessage();
		modificationMessage.setObjectId("123");
		modificationMessage.setObjectType(ObjectType.ACTIVITY);
		MessageKey fifth = new MessageKey(modificationMessage);
		assertNotSame(fourth, fifth);

		modificationMessage = new DefaultModificationMessage();
		modificationMessage.setObjectId("123");
		modificationMessage.setObjectType(ObjectType.ACTIVITY);
		MessageKey sixth = new MessageKey(modificationMessage);
		assertEquals(fifth, sixth);
	}
	
	@Test
	public void testSendMessage(){
		ChangeMessage message = new ChangeMessage();
		message.setChangeNumber(new Long(123));
		message.setTimestamp(new Date(System.currentTimeMillis()/1000*1000));
		message.setObjectEtag("etag");
		message.setObjectId("syn456");
		message.setParentId("syn789");
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
		// Verify that the one message was fired.
		verify(mockObserver, times(1)).fireChangeMessage(message);
		// It should only be called once total!
		verify(mockObserver, times(1)).fireChangeMessage(any(ChangeMessage.class));
	}
	
	@Test
	public void testSendMessageTwice() throws JSONObjectAdapterException{
		ChangeMessage first = new ChangeMessage();
		first.setChangeNumber(new Long(123));
		first.setTimestamp(new Date(System.currentTimeMillis()/1000*1000));
		first.setObjectEtag("etag");
		first.setObjectId("syn456");
		first.setParentId("syn789");
		first.setObjectType(ObjectType.ENTITY);
		first.setChangeType(ChangeType.DELETE);
		
		String json = EntityFactory.createJSONStringForEntity(first);
		ChangeMessage second = EntityFactory.createEntityFromJSONString(json, ChangeMessage.class);
		// Change the etag of the second
		second.setObjectEtag("etagtwo");
		
		// Send the message first message
		messenger.sendMessageAfterCommit(first);
		messenger.sendMessageAfterCommit(second);
		assertNotNull(stubProxy.getSynchronizations());
		assertEquals(1, stubProxy.getSynchronizations().size());
		// Simulate the before commit
		stubProxy.getSynchronizations().get(0).beforeCommit(true);
		List<ChangeMessage> list = new ArrayList<ChangeMessage>();
		// The second should get sent but not the fist.
		list.add(second);
		verify(mockChangeDAO, times(1)).replaceChange(list);
		list = new ArrayList<ChangeMessage>();
		list.add(first);
		verify(mockChangeDAO, never()).replaceChange(list);
		// Simulate the after commit
		stubProxy.getSynchronizations().get(0).afterCommit();
		// The second message should get sent but not the first
		verify(mockObserver, times(1)).fireChangeMessage(second);
		verify(mockObserver, never()).fireChangeMessage(first);
	}
	
	@Test
	public void testSendMessageSameIdDifferntType() throws JSONObjectAdapterException{
		ChangeMessage first = new ChangeMessage();
		first.setChangeNumber(new Long(123));
		first.setTimestamp(new Date(System.currentTimeMillis()/1000*1000));
		first.setObjectId("456");
		first.setObjectType(ObjectType.ACTIVITY);
		first.setChangeType(ChangeType.UPDATE);
		
		// This has the same id as the first but is of a different type.
		ChangeMessage second = new ChangeMessage();
		second.setChangeNumber(new Long(124));
		second.setTimestamp(new Date(System.currentTimeMillis()/1000*1000));
		second.setObjectId("456");
		second.setObjectType(ObjectType.PRINCIPAL);
		second.setChangeType(ChangeType.UPDATE);
		
		// Send the message first message
		messenger.sendMessageAfterCommit(first);
		messenger.sendMessageAfterCommit(second);
		assertNotNull(stubProxy.getSynchronizations());
		assertEquals(1, stubProxy.getSynchronizations().size());
		// Simulate the before commit
		stubProxy.getSynchronizations().get(0).beforeCommit(true);
		// Simulate the after commit
		stubProxy.getSynchronizations().get(0).afterCommit();
		// The second message should get sent and so should the first
		verify(mockObserver, times(1)).fireChangeMessage(second);
		verify(mockObserver, times(1)).fireChangeMessage(first);
		verify(mockObserver, times(2)).fireChangeMessage(any(ChangeMessage.class));
	}

	/**
	 * PLFM-1662 was a bug the resulted in duplicate messages being broadcast. One of the messages did not have
	 * a change number or time stamp, while the other messages was correct.
	 */
	@Test
	public void testPLFM_1662(){
		mockTxManager = Mockito.mock(DataSourceTransactionManager.class);
		// We need stub dao to detect this bug.
		DBOChangeDAO stubChangeDao = new StubDBOChangeDAO();
		mockChangeDAO = Mockito.mock(DBOChangeDAO.class);
		stubProxy = new TransactionSynchronizationProxyStub();
		mockObserver = Mockito.mock(TransactionalMessengerObserver.class);
		messenger = new TransactionalMessengerImpl(mockTxManager, stubChangeDao, stubProxy, testClock);
		messenger.registerObserver(mockObserver);
		
		ChangeMessage message = new ChangeMessage();
		message.setChangeNumber(new Long(123));
		message.setTimestamp(new Date(System.currentTimeMillis()/1000*1000));
		message.setObjectEtag("etag");
		message.setObjectId("syn456");
		message.setParentId("syn789");
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
		// Simulate the after commit
		stubProxy.getSynchronizations().get(0).afterCommit();
		// It should only be called once total!
		verify(mockObserver, times(1)).fireChangeMessage(any(ChangeMessage.class));
		
	}

}
