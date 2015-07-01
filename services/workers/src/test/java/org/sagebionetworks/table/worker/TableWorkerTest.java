package org.sagebionetworks.table.worker;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.manager.NodeInheritanceManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.exception.LockUnavilableException;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.worker.TableWorker.State;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.progress.ProgressCallback;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.model.Message;
import com.google.common.collect.Lists;

public class TableWorkerTest {
	
	ProgressCallback<Message> mockProgressCallback;
	ConnectionFactory mockTableConnectionFactory;
	TableRowManager mockTableRowManager;
	TableIndexDAO mockTableIndexDAO;
	StackConfiguration mockConfiguration;
	SimpleJdbcTemplate mockConnection;
	NodeInheritanceManager mockNodeInheritanceManager;
	TableWorker worker;


	@Before
	public void before() throws LockUnavilableException, InterruptedException, Exception{
		mockProgressCallback = Mockito.mock(ProgressCallback.class);
		mockTableConnectionFactory = Mockito.mock(ConnectionFactory.class);
		mockTableRowManager = Mockito.mock(TableRowManager.class);
		mockTableIndexDAO = Mockito.mock(TableIndexDAO.class);
		mockConfiguration = Mockito.mock(StackConfiguration.class);
		mockNodeInheritanceManager = mock(NodeInheritanceManager.class);
		mockConnection = Mockito.mock(SimpleJdbcTemplate.class);
		// Turn on the feature by default
		when(mockConfiguration.getTableEnabled()).thenReturn(true);
		// return a connection by default
		when(mockTableConnectionFactory.getConnection(anyString())).thenReturn(mockTableIndexDAO);
		when(mockNodeInheritanceManager.isNodeInTrash(anyString())).thenReturn(false);
		
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
		worker = new TableWorker();
		ReflectionTestUtils.setField(worker, "tableConnectionFactory", mockTableConnectionFactory);
		ReflectionTestUtils.setField(worker, "tableRowManager", mockTableRowManager);
		ReflectionTestUtils.setField(worker, "configuration", mockConfiguration);
		ReflectionTestUtils.setField(worker, "nodeInheritanceManager", mockNodeInheritanceManager);
	}
	
	
	/**
	 * Non-table messages should simply be returned so they can be removed from the queue.
	 * @throws Exception
	 */
	@Test
	public void testNonTableMessages() throws Exception{
		Message one = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.ACTIVITY, "etag");
		Message two = MessageUtils.buildMessage(ChangeType.CREATE, "456", ObjectType.ACTIVITY, "etag");
		// call under test
		worker.run(mockProgressCallback, one);
		worker.run(mockProgressCallback, two);
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
		// call under test
		worker.run(mockProgressCallback, one);
		worker.run(mockProgressCallback, two);
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
		// call under test
		worker.run(mockProgressCallback, two);
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
		List<ColumnModel> currentSchema = Lists.newArrayList();
		when(mockTableRowManager.getColumnModelsForTable(tableId)).thenReturn(currentSchema);
		when(mockTableRowManager.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		when(mockTableIndexDAO.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);
		when(mockTableRowManager.getCurrentRowVersions(tableId, 0L, 0L, 16000L)).thenReturn(Collections.singletonMap(0L, 0L));
		TableRowChange trc = new TableRowChange();
		trc.setEtag("etag");
		trc.setRowVersion(0L);
		when(mockTableRowManager.getLastTableRowChange(tableId)).thenReturn(trc);
		RowSet rowSet = new RowSet();
		rowSet.setRows(Collections.singletonList(TableModelTestUtils.createRow(0L, 0L, "2")));
		when(mockTableRowManager.getRowSet(eq(tableId), eq(0L), eq(Collections.singleton(0L)), any(ColumnMapper.class))).thenReturn(rowSet);
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken);
		// call under test
		worker.run(mockProgressCallback, two);
		// The connection factory should be called
		verify(mockTableConnectionFactory, times(1)).getConnection(anyString());
		// The status should get set to available
		verify(mockTableRowManager, times(1)).attemptToSetTableStatusToAvailable(tableId, resetToken, "etag");
		verify(mockTableIndexDAO).createOrUpdateOrDeleteRows(rowSet, currentSchema);
		verify(mockTableIndexDAO).setMaxCurrentCompleteVersionForTable(tableId, 0L);
	}

	/**
	 * When everything works well, the message should be removed from the queue and the table status should be set to
	 * AVAILABLE.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testHappyUpdateCase() throws Exception {
		String tableId = "456";
		String resetToken = "reset-token";
		TableStatus status = new TableStatus();
		status.setResetToken(resetToken);
		List<ColumnModel> currentSchema = Lists.newArrayList();
		when(mockTableRowManager.getColumnModelsForTable(tableId)).thenReturn(currentSchema);
		when(mockTableRowManager.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		when(mockTableIndexDAO.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(2L);
		when(mockTableRowManager.getCurrentRowVersions(tableId, 3L, 0L, 16000L)).thenReturn(Collections.singletonMap(0L, 3L));
		TableRowChange trc = new TableRowChange();
		trc.setEtag("etag");
		trc.setRowVersion(3L);
		when(mockTableRowManager.getLastTableRowChange(tableId)).thenReturn(trc);
		RowSet rowSet = new RowSet();
		rowSet.setRows(Collections.singletonList(TableModelTestUtils.createRow(0L, 3L, "2")));
		when(mockTableRowManager.getRowSet(eq(tableId), eq(3L), eq(Collections.singleton(0L)), any(ColumnMapper.class))).thenReturn(rowSet);
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken);
		// call under test
		worker.run(mockProgressCallback, two);
		// The connection factory should be called
		verify(mockTableConnectionFactory, times(1)).getConnection(anyString());
		// The status should get set to available
		verify(mockTableRowManager, times(1)).attemptToSetTableStatusToAvailable(tableId, resetToken, "etag");
		verify(mockTableIndexDAO).createOrUpdateOrDeleteRows(rowSet, currentSchema);
		verify(mockTableIndexDAO).setMaxCurrentCompleteVersionForTable(tableId, 3L);
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
		when(mockTableRowManager.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		// This should trigger a failure
		RuntimeException error = new RuntimeException("Something went horribly wrong!");
		when(mockTableRowManager.getColumnModelsForTable(tableId)).thenThrow(error);
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken);
		// call under test
		worker.run(mockProgressCallback, two);
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
		when(mockTableRowManager.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		// Without a connection the message should go back to the queue
		when(mockTableConnectionFactory.getConnection(anyString())).thenReturn(null);
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken);
		try {
			// call under test
			worker.run(mockProgressCallback, two);
			fail();
		} catch (RecoverableMessageException e) {
			// expected
		}
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
		when(mockTableRowManager.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken1);
		// call under test
		worker.run(mockProgressCallback, two);
		// The connection factory should never be called
		verify(mockTableConnectionFactory, never()).getConnection(anyString());
		verify(mockTableRowManager, never()).attemptToSetTableStatusToAvailable(anyString(), anyString(), anyString());
		// The token must be checked before we acquire the lock
		verify(mockTableRowManager, never()).tryRunWithTableExclusiveLock(anyString(), anyLong(), any(Callable.class));
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
		when(mockTableRowManager.getTableStatusOrCreateIfNotExists(tableId)).thenThrow(new NotFoundException("This table does not exist"));
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken);
		// call under test
		worker.run(mockProgressCallback, two);
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
		when(mockTableRowManager.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		// Simulate a failure to get the lock
		when(mockTableRowManager.tryRunWithTableExclusiveLock(anyString(), anyLong(), any(Callable.class))).thenThrow(new LockUnavilableException("Cannot get a lock at this time"));
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken);
		try {
			// call under test
			worker.run(mockProgressCallback, two);
			fail();
		} catch (RecoverableMessageException e) {
			// expected
		}
		// The connection factory should never be called
		verify(mockTableConnectionFactory, never()).getConnection(anyString());
	}
	
	/**
	 * When a lock cannot be acquired, the message should remain on the queue as this is a recoverable failure.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCacheBehindException() throws Exception {
		String tableId = "456";
		String resetToken = "reset-token";
		TableStatus status = new TableStatus();
		status.setResetToken(resetToken);
		status.setProgressMessage("going");
		status.setProgressCurrent(2L);
		status.setProgressTotal(3L);
		List<ColumnModel> currentSchema = Lists.newArrayList();
		when(mockTableRowManager.getColumnModelsForTable(tableId)).thenReturn(currentSchema);
		when(mockTableRowManager.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		when(mockTableIndexDAO.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);
		when(mockTableRowManager.getCurrentRowVersions(tableId, 0L, 0L, 16000L)).thenThrow(new TableUnavilableException(status));
		TableRowChange trc = new TableRowChange();
		trc.setEtag("etag");
		trc.setRowVersion(0L);
		when(mockTableRowManager.getLastTableRowChange(tableId)).thenReturn(trc);
		RowSet rowSet = new RowSet();
		rowSet.setRows(Collections.singletonList(TableModelTestUtils.createRow(0L, 0L, "2")));
		when(mockTableRowManager.getRowSet(eq(tableId), eq(0L), eq(Collections.singleton(0L)), any(ColumnMapper.class))).thenReturn(rowSet);
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken);
		try {
			// call under test
			worker.run(mockProgressCallback, two);
			fail();
		} catch (RecoverableMessageException e) {
			// expected
		}
		// The connection factory should be called
		verify(mockTableConnectionFactory, times(1)).getConnection(anyString());
		// The status should get set to available
		verify(mockTableRowManager, times(1)).attemptToUpdateTableProgress(tableId, resetToken, "going", 2L, 3L);
	}

	/**
	 * An InterruptedException thrown while waiting for lock should be treated asn a recoverable exception.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testInterruptedException() throws Exception{
		String tableId = "456";
		String resetToken = "reset-token";
		TableStatus status = new TableStatus();
		status.setResetToken(resetToken);
		when(mockTableRowManager.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		// Simulate a failure to get the lock
		when(mockTableRowManager.tryRunWithTableExclusiveLock(anyString(), anyLong(), any(Callable.class))).thenThrow(new InterruptedException("Sop!!!"));
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken);
		try {
			// call under test
			worker.run(mockProgressCallback, two);
			fail();
		} catch (RecoverableMessageException e) {
			// expected
		}
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
		when(mockTableRowManager.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		when(mockTableRowManager.getCurrentRowVersions(tableId, 1L, 0L, 16000L)).thenReturn(Collections.<Long, Long> emptyMap());
		
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken);
		// call under test
		worker.run(mockProgressCallback, two);
		// The connection factory should never be called
		verify(mockTableRowManager).attemptToSetTableStatusToAvailable(anyString(), anyString(), anyString());
	}

	@Test
	public void testDoNotCreateTrashedTables() throws Exception {
		String tableId = "456";
		when(mockNodeInheritanceManager.isNodeInTrash(tableId)).thenReturn(true);
		String resetToken = "reset-token";
		Message two = MessageUtils.buildMessage(ChangeType.UPDATE, tableId, ObjectType.TABLE, resetToken);
		// call under test
		worker.run(mockProgressCallback, two);
		// The connection factory should not be called
		verifyZeroInteractions(mockTableConnectionFactory);
	}
}
