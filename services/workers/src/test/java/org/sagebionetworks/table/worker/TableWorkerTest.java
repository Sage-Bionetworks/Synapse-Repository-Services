package org.sagebionetworks.table.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.exception.LockUnavilableException;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.worker.TableWorker.State;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import com.amazonaws.services.sqs.model.Message;

public class TableWorkerTest {
	
	ConnectionFactory mockTableConnectionFactory;
	TableRowManager mockTableRowManager;
	TableIndexDAO mockTableIndexDAO;
	StackConfiguration mockConfiguration;
	SimpleJdbcTemplate mockConnection;


	@Before
	public void before() throws LockUnavilableException, InterruptedException, Exception{
		mockTableConnectionFactory = Mockito.mock(ConnectionFactory.class);
		mockTableRowManager = Mockito.mock(TableRowManager.class);
		mockTableIndexDAO = Mockito.mock(TableIndexDAO.class);
		mockConfiguration = Mockito.mock(StackConfiguration.class);
		mockConnection = Mockito.mock(SimpleJdbcTemplate.class);
		// Turn on the feature by default
		when(mockConfiguration.getTableEnabled()).thenReturn(true);
		// return a connection by default
		when(mockTableConnectionFactory.getConnection(anyString())).thenReturn(mockTableIndexDAO);
		
		// By default we want to the manager to just call the passed callable.
		stub(mockTableRowManager.tryRunWithTableExclusiveLock(anyString(), anyLong(), any(Callable.class))).toAnswer(new Answer<TableWorker.State>() {
			@Override
			public TableWorker.State answer(InvocationOnMock invocation) throws Throwable {
				Callable<TableWorker.State> callable = (Callable<State>) invocation.getArguments()[2];
				if(callable != null){
					return callable.call();
				}else{
					return null;
				}
			}
		});
	}
	
	/**
	 * Helper to create a new worker for a list of messages.
	 * @param messages
	 * @return
	 */
	public TableWorker createNewWorker(List<Message> messages){
		return new TableWorker(messages, mockTableConnectionFactory, mockTableRowManager, mockConfiguration);
	}
	
	/**
	 * Non-table messages should simply be returned so they can be removed from the queue.
	 * @throws Exception
	 */
	@Test
	public void testNonTableMessages() throws Exception{
		Message one = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.ACTIVITY, "etag");
		Message two = MessageUtils.buildMessage(ChangeType.CREATE, "456", ObjectType.ACTIVITY, "etag");
		List<Message> messages = Arrays.asList(one, two);
		// Create the worker
		TableWorker worker = createNewWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		// It should just return the results unchanged
		assertNotNull(results);
		assertEquals(messages, results);
		// The connection factory should never be called
		verify(mockTableConnectionFactory, never()).getConnection(anyString());
	}
	
	/**
	 * When the table feature is disabled all messages should be removed from the queue and no other methods should be called.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFeatureDisabled() throws Exception{
		// Disable the feature
		when(mockConfiguration.getTableEnabled()).thenReturn(false);
		Message one = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.TABLE, "etag");
		Message two = MessageUtils.buildMessage(ChangeType.DELETE, "456", ObjectType.TABLE, "etag");
		List<Message> messages = Arrays.asList(one, two);
		// Create the worker
		TableWorker worker = createNewWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals(messages, results);
		// The connection factory should never be called
		verify(mockTableConnectionFactory, never()).getConnection(anyString());
	}
	
	/**
	 * A delete message should result in the table index also being deleted.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTableDelete() throws Exception{
		Message two = MessageUtils.buildMessage(ChangeType.DELETE, "456", ObjectType.TABLE, "etag");
		List<Message> messages = Arrays.asList(two);
		// Create the worker
		TableWorker worker = createNewWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals(messages, results);
		// The connection factory should be called
		verify(mockTableConnectionFactory, times(1)).getConnection(anyString());
		// delete should be called
		verify(mockTableIndexDAO, times(1)).deleteTable("456");
	}
	
	/**
	 * When everything works well, the message should be removed from the queue and 
	 * the table status should be set to AVAILABLE.
	 * @throws Exception
	 */
	@Test
	public void testHappyCase() throws Exception{
		String tableId = "456";
		String resetToken = "reset-token";
		TableStatus status = new TableStatus();
		status.setResetToken(resetToken);
		when(mockTableRowManager.getTableStatus(tableId)).thenReturn(status);
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken);
		List<Message> messages = Arrays.asList(two);
		// Create the worker
		TableWorker worker = createNewWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals(messages, results);
		// The connection factory should be called
		verify(mockTableConnectionFactory, times(1)).getConnection(anyString());
		// The status should get set to available
		verify(mockTableRowManager, times(1)).attemptToSetTableStatusToAvailable(tableId, resetToken, null);
	}
	
	/**
	 * When an unknown exception is thrown the table status must get set to failed.
	 * @throws Exception
	 */
	@Test
	public void testSetToFailed() throws Exception{
		String tableId = "456";
		String resetToken = "reset-token";
		TableStatus status = new TableStatus();
		status.setResetToken(resetToken);
		when(mockTableRowManager.getTableStatus(tableId)).thenReturn(status);
		// This should trigger a failure
		RuntimeException error = new RuntimeException("Something went horribly wrong!");
		when(mockTableRowManager.getColumnModelsForTable(tableId)).thenThrow(error);
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken);
		List<Message> messages = Arrays.asList(two);
		// Create the worker
		TableWorker worker = createNewWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals(messages, results);
		// The connection factory should be called
		verify(mockTableConnectionFactory, times(1)).getConnection(anyString());
		// The status should get set to available
		verify(mockTableRowManager, times(1)).attemptToSetTableStatusToFailed(anyString(), anyString(), anyString(), anyString());
	}
	
	/**
	 * If a connection to the cluster cannot be made at this time, then the message must return the queue
	 * as we can recover from this error when a database becomes available.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNoConnection() throws Exception{
		String tableId = "456";
		String resetToken = "reset-token";
		TableStatus status = new TableStatus();
		status.setResetToken(resetToken);
		when(mockTableRowManager.getTableStatus(tableId)).thenReturn(status);
		// Without a connection the message should go back to the queue
		when(mockTableConnectionFactory.getConnection(anyString())).thenReturn(null);
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken);
		List<Message> messages = Arrays.asList(two);
		// Create the worker
		TableWorker worker = createNewWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals("The message should not have been returned since this is a recoverable failure",0, results.size());
	}
	
	/**
	 * When the reset-tokens do not match, the table has been updated since the message was sent.
	 * When this occurs the status cannot be set to AVAILABLE
	 * @throws Exception
	 */
	@Test
	public void testResetTokenDoesNotMatch() throws Exception{
		String tableId = "456";
		String resetToken1 = "reset-token1";
		String resetToken2 = "reset-token2";
		TableStatus status = new TableStatus();
		// Set the current token to be different than the token in the message
		status.setResetToken(resetToken2);
		when(mockTableRowManager.getTableStatus(tableId)).thenReturn(status);
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken1);
		List<Message> messages = Arrays.asList(two);
		// Create the worker
		TableWorker worker = createNewWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals("An old message should get returned so it can be removed from the queue",messages, results);
		// The connection factory should never be called
		verify(mockTableConnectionFactory, never()).getConnection(anyString());
		verify(mockTableRowManager, never()).attemptToSetTableStatusToAvailable(anyString(), anyString(), anyString());
	}
	
	/**
	 * When a NotFoundException is thrown the table no longer exists and the message should be removed from the queue.
	 * @throws Exception
	 */
	@Test
	public void testNotFoundException() throws Exception{
		String tableId = "456";
		String resetToken = "reset-token";
		TableStatus status = new TableStatus();
		status.setResetToken(resetToken);
		when(mockTableRowManager.getTableStatus(tableId)).thenThrow(new NotFoundException("This table does not exist"));
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken);
		List<Message> messages = Arrays.asList(two);
		// Create the worker
		TableWorker worker = createNewWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals("An old message should get returned so it can be removed from the queue",messages, results);
		// The connection factory should never be called
		verify(mockTableConnectionFactory, never()).getConnection(anyString());
	}
	
	/**
	 * When a lock cannot be acquired, the message should remain on the queue as this is a recoverable failure.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testLockUnavilableException() throws Exception{
		String tableId = "456";
		String resetToken = "reset-token";
		TableStatus status = new TableStatus();
		status.setResetToken(resetToken);
		when(mockTableRowManager.getTableStatus(tableId)).thenReturn(status);
		// Simulate a failure to get the lock
		when(mockTableRowManager.tryRunWithTableExclusiveLock(anyString(), anyLong(), any(Callable.class))).thenThrow(new LockUnavilableException("Cannot get a lock at this time"));
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken);
		List<Message> messages = Arrays.asList(two);
		// Create the worker
		TableWorker worker = createNewWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals("The message should not have been returned since this is a recoverable failure",0, results.size());
		// The connection factory should never be called
		verify(mockTableConnectionFactory, never()).getConnection(anyString());
	}
	
	/**
	 * An InterruptedException thrown while waiting for lock should be treated asn a recoverable exception.
	 * @throws Exception
	 */
	@Test
	public void testInterruptedException() throws Exception{
		String tableId = "456";
		String resetToken = "reset-token";
		TableStatus status = new TableStatus();
		status.setResetToken(resetToken);
		when(mockTableRowManager.getTableStatus(tableId)).thenReturn(status);
		// Simulate a failure to get the lock
		when(mockTableRowManager.tryRunWithTableExclusiveLock(anyString(), anyLong(), any(Callable.class))).thenThrow(new InterruptedException("Sop!!!"));
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken);
		List<Message> messages = Arrays.asList(two);
		// Create the worker
		TableWorker worker = createNewWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals("The message should not have been returned since this is a recoverable failure",0, results.size());
		// The connection factory should never be called
		verify(mockTableConnectionFactory, never()).getConnection(anyString());
	}
	
	/**
	 * A ConflictingUpdateException indicates that the table was update after the original message was sent.
	 * This is treated like a non-recoverable error and the message should be removed from the queue.
	 * @throws Exception
	 */
	@Test
	public void testConflictingUpdateException() throws Exception{
		String tableId = "456";
		String resetToken = "reset-token";
		TableStatus status = new TableStatus();
		status.setResetToken(resetToken);
		// simulate the ConflictingUpdateException
		doThrow(new ConflictingUpdateException("Cannot get a lock at this time")).when(mockTableRowManager).attemptToSetTableStatusToAvailable(anyString(), anyString(), anyString());
		
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken);
		List<Message> messages = Arrays.asList(two);
		// Create the worker
		TableWorker worker = createNewWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals("An old message should get returned so it can be removed from the queue",messages, results);
		// The connection factory should never be called
		verify(mockTableConnectionFactory, never()).getConnection(anyString());
	}
	
}
