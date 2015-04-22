package org.sagebionetworks.table.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.io.IOException;
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
import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.repo.manager.NodeInheritanceManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.exception.LockUnavilableException;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.ProgressCallback;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import com.amazonaws.services.sqs.model.Message;

public class TableCurrentCacheWorkerTest {

	TableRowManager mockTableRowManager;
	StackConfiguration mockConfiguration;
	NodeInheritanceManager mockNodeInheritanceManager;

	@Before
	public void before() throws LockUnavilableException, InterruptedException, Exception {
		mockTableRowManager = Mockito.mock(TableRowManager.class);
		mockConfiguration = Mockito.mock(StackConfiguration.class);
		mockNodeInheritanceManager = Mockito.mock(NodeInheritanceManager.class);
		// Turn on the feature by default
		when(mockConfiguration.getTableEnabled()).thenReturn(true);
		when(mockNodeInheritanceManager.isNodeInTrash(anyString())).thenReturn(false);
	}

	/**
	 * Helper to create a new worker for a list of messages.
	 * 
	 * @param messages
	 * @return
	 */
	public TableCurrentCacheWorker createNewWorker(List<Message> messages) {
		return new TableCurrentCacheWorker(messages, mockTableRowManager, mockConfiguration, new WorkerProgress() {
			@Override
			public void progressMadeForMessage(Message message) {
			}

			@Override
			public void retryMessage(Message message, int retryTimeoutInSeconds) {
				// TODO Auto-generated method stub

			}
		}, mockNodeInheritanceManager);
	}

	/**
	 * Non-table messages should simply be returned so they can be removed from the queue.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNonTableMessages() throws Exception {
		Message one = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.ACTIVITY, "etag");
		Message two = MessageUtils.buildMessage(ChangeType.CREATE, "456", ObjectType.ACTIVITY, "etag");
		List<Message> messages = Arrays.asList(one, two);
		// Create the worker
		TableCurrentCacheWorker worker = createNewWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		// It should just return the results unchanged
		assertNotNull(results);
		assertEquals(messages, results);
		verifyNoMoreInteractions(mockTableRowManager);
	}

	/**
	 * When the table feature is disabled all messages should be removed from the queue and no other methods should be
	 * called.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFeatureDisabled() throws Exception {
		// Disable the feature
		when(mockConfiguration.getTableEnabled()).thenReturn(false);
		Message one = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.TABLE, "etag");
		Message two = MessageUtils.buildMessage(ChangeType.DELETE, "456", ObjectType.TABLE, "etag");
		List<Message> messages = Arrays.asList(one, two);
		// Create the worker
		TableCurrentCacheWorker worker = createNewWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals(messages, results);
		verifyNoMoreInteractions(mockTableRowManager);
	}

	/**
	 * A delete message should result in the table index also being deleted.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTableDelete() throws Exception {
		String tableId = "456";
		Message two = MessageUtils.buildMessage(ChangeType.DELETE, tableId, ObjectType.TABLE, "etag");
		List<Message> messages = Arrays.asList(two);
		// Create the worker
		TableCurrentCacheWorker worker = createNewWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals(messages, results);
		verify(mockTableRowManager).removeCaches(tableId);
	}

	@Test
	public void testTableInTrash() throws Exception {
		String tableId = "456";
		String resetToken = "reset-token";
		when(mockNodeInheritanceManager.isNodeInTrash(tableId)).thenReturn(true);
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken);
		List<Message> messages = Arrays.asList(two);
		// Create the worker
		TableCurrentCacheWorker worker = createNewWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals(messages, results);
		verifyNoMoreInteractions(mockTableRowManager);
	}

	@Test
	public void testTableNotExists() throws Exception {
		String tableId = "456";
		String resetToken = "reset-token";
		when(mockNodeInheritanceManager.isNodeInTrash(tableId)).thenThrow(new NotFoundException("dummy"));
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken);
		List<Message> messages = Arrays.asList(two);
		// Create the worker
		TableCurrentCacheWorker worker = createNewWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals(messages, results);
		verifyNoMoreInteractions(mockTableRowManager);
	}

	/**
	 * When everything works well, the message should be removed from the queue and the table status should be set to
	 * AVAILABLE.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testHappyCase() throws Exception {
		String tableId = "456";
		String resetToken = "reset-token";
		TableStatus status = new TableStatus();
		status.setResetToken(resetToken);
		when(mockTableRowManager.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken);
		List<Message> messages = Arrays.asList(two);
		// Create the worker
		TableCurrentCacheWorker worker = createNewWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals(messages, results);
		verify(mockTableRowManager).updateLatestVersionCache(eq(tableId), any(ProgressCallback.class));
	}

	/**
	 * When an unknown exception is thrown the table message should be retried
	 * 
	 * @throws Exception
	 */
	@Test(expected = IOException.class)
	public void testFailing() throws Exception {
		String tableId = "456";
		String resetToken = "reset-token";
		TableStatus status = new TableStatus();
		status.setResetToken(resetToken);
		when(mockTableRowManager.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		// This should trigger a failure
		doThrow(new IOException("mock")).when(mockTableRowManager).updateLatestVersionCache(eq(tableId), any(ProgressCallback.class));
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken);
		List<Message> messages = Arrays.asList(two);
		// Create the worker
		TableCurrentCacheWorker worker = createNewWorker(messages);
		// Make the call
		worker.call();
	}

	/**
	 * When the reset-tokens do not match, the table has been updated since the message was sent. When this occurs the
	 * status cannot be set to AVAILABLE
	 * 
	 * @throws Exception
	 */
	@Test
	public void testResetTokenDoesNotMatch() throws Exception {
		String tableId = "456";
		String resetToken1 = "reset-token1";
		String resetToken2 = "reset-token2";
		TableStatus status = new TableStatus();
		// Set the current token to be different than the token in the message
		status.setResetToken(resetToken2);
		when(mockTableRowManager.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken1);
		List<Message> messages = Arrays.asList(two);
		// Create the worker
		TableCurrentCacheWorker worker = createNewWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals("An old message should get returned so it can be removed from the queue", messages, results);
		verify(mockTableRowManager).getTableStatusOrCreateIfNotExists(tableId);
		verifyNoMoreInteractions(mockTableRowManager);
	}
}
