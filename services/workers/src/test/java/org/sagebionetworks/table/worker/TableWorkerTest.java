package org.sagebionetworks.table.worker;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionUnavailableException;
import org.sagebionetworks.repo.manager.table.TableIndexManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableChangeType;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ColumnChangeDetails;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.table.worker.TableWorker.State;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;

public class TableWorkerTest {
	
	@Mock
	ProgressCallback mockProgressCallback;
	@Mock
	TableEntityManager mockTableEntityManager;
	@Mock
	TableIndexManager mockTableIndexManager;
	@Mock
	TableIndexConnectionFactory mockConnectionFactory;
	@Mock
	StackConfiguration mockConfiguration;
	@Mock
	TableManagerSupport mockTableManagerSupport;

	TableWorker worker;
	ChangeMessage one;
	ChangeMessage two;
	String tableId;
	String resetToken;
	List<ColumnModel> currentSchema;
	List<String> currentColumnIds;
	RowSet rowSet1;
	SparseChangeSet sparseRowset1;
	RowSet rowSet2;
	SparseChangeSet sparseRowset2;
	boolean isTableView;
	
	@Before
	public void before() throws LockUnavilableException, InterruptedException, Exception{
		MockitoAnnotations.initMocks(this);
		when(mockConnectionFactory.connectToTableIndex(anyString())).thenReturn(mockTableIndexManager);
		
		// By default we want to the manager to just call the passed callable.
		stub(mockTableManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class),anyString(), anyInt(), any(ProgressingCallable.class))).toAnswer(new Answer<TableWorker.State>() {
			@Override
			public TableWorker.State answer(InvocationOnMock invocation) throws Throwable {
				ProgressingCallable<TableWorker.State> callable = (ProgressingCallable<State>) invocation.getArguments()[3];
				if(callable != null){
					return callable.call(mockProgressCallback);
				}else{
					return null;
				}
			}
		});
		worker = new TableWorker();
		ReflectionTestUtils.setField(worker, "connectionFactory", mockConnectionFactory);
		ReflectionTestUtils.setField(worker, "tableEntityManager", mockTableEntityManager);
		ReflectionTestUtils.setField(worker, "configuration", mockConfiguration);
		ReflectionTestUtils.setField(worker, "tableManagerSupport", mockTableManagerSupport);
		worker.setTimeoutSeconds(1200L);
		
		one = new ChangeMessage();
		one.setChangeType(ChangeType.CREATE);
		one.setObjectId("123");
		one.setObjectType(ObjectType.ACTIVITY);
		one.setObjectEtag("etag");
		
		two = new ChangeMessage();
		two.setChangeType(ChangeType.CREATE);
		two.setObjectId("456");
		two.setObjectType(ObjectType.ACTIVITY);
		two.setObjectEtag("etag");
		
		
		tableId = "456";
		resetToken = "reset-token";
		currentSchema = Lists.newArrayList();
		currentSchema.add(TableModelTestUtils.createColumn(0L, "aString", ColumnType.STRING));
		currentColumnIds = TableModelUtils.getIds(currentSchema);
		when(mockTableManagerSupport.getColumnModelsForTable(tableId)).thenReturn(currentSchema);
		TableRowChange trc1 = new TableRowChange();
		trc1.setEtag("etag");
		trc1.setRowVersion(0L);
		trc1.setRowCount(12L);
		trc1.setChangeType(TableChangeType.ROW);
		TableRowChange trc2 = new TableRowChange();
		trc2.setEtag("etag2");
		trc2.setRowVersion(1L);
		trc2.setRowCount(3L);
		trc2.setChangeType(TableChangeType.ROW);
		when(mockTableEntityManager.listRowSetsKeysForTable(tableId)).thenReturn(Arrays.asList(trc1,trc2));
		when(mockTableIndexManager.isVersionAppliedToIndex(anyString(), anyLong())).thenReturn(false);
		
		rowSet1 = new RowSet();
		rowSet1.setTableId(tableId);
		rowSet1.setHeaders(TableModelUtils.getSelectColumns(currentSchema));
		rowSet1.setRows(Collections.singletonList(TableModelTestUtils.createRow(0L, 0L, "2")));
		
		sparseRowset1 = TableModelUtils.createSparseChangeSet(rowSet1, currentSchema);
		
		rowSet2 = new RowSet();
		rowSet2.setTableId(tableId);
		rowSet2.setHeaders(TableModelUtils.getSelectColumns(currentSchema));
		rowSet2.setRows(Collections.singletonList(TableModelTestUtils.createRow(0L, 1L, "3")));
		
		sparseRowset2 = TableModelUtils.createSparseChangeSet(rowSet2, currentSchema);
		
		when(mockTableManagerSupport.startTableProcessing(tableId)).thenReturn(resetToken);
		
		when(mockTableManagerSupport.isIndexWorkRequired(tableId)).thenReturn(true);
		
		when(mockTableEntityManager.getSparseChangeSet(trc1)).thenReturn(sparseRowset1);
		when(mockTableEntityManager.getSparseChangeSet(trc2)).thenReturn(sparseRowset2);
		isTableView = false;
	}
	
	
	/**
	 * Non-table messages should simply be returned so they can be removed from the queue.
	 * @throws Exception
	 */
	@Test
	public void testNonTableMessages() throws Exception{
		// call under test
		worker.run(mockProgressCallback, one);
		worker.run(mockProgressCallback, two);
		// The connection factory should never be called
		verifyZeroInteractions(mockConnectionFactory);
	}
	
	@Test (expected=IllegalStateException.class)
	public void testLockTimeoutNotSet() throws Exception{
		worker = new TableWorker();
		ReflectionTestUtils.setField(worker, "connectionFactory", mockConnectionFactory);
		ReflectionTestUtils.setField(worker, "tableEntityManager", mockTableEntityManager);
		ReflectionTestUtils.setField(worker, "configuration", mockConfiguration);
		// call under test
		worker.run(mockProgressCallback, one);
	}
	
	/**
	 * A delete message should result in the table index also being deleted.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTableDelete() throws Exception{
		two.setObjectType(ObjectType.TABLE);
		two.setChangeType(ChangeType.DELETE);
		// call under test
		worker.run(mockProgressCallback, two);
		// The connection factory should be called
		verify(mockConnectionFactory, times(1)).connectToTableIndex(two.getObjectId());
		// delete should be called
		verify(mockTableIndexManager, times(1)).deleteTableIndex(tableId);
		verify(mockTableEntityManager, times(1)).deleteTableIfDoesNotExist(tableId);
	}
	
	/**
	 * When everything works well, the message should be removed from the queue and 
	 * the table status should be set to AVAILABLE.
	 * @throws Exception
	 */
	@Test
	public void testHappyCase() throws Exception{
		two.setObjectType(ObjectType.TABLE);
		two.setChangeType(ChangeType.UPDATE);
		two.setObjectEtag(resetToken);
		// call under test
		worker.run(mockProgressCallback, two);
		// The worker should ensure the table is processing.
		verify(mockTableManagerSupport, times(1)).startTableProcessing(tableId);
		// The connection factory should be called
		verify(mockConnectionFactory, times(1)).connectToTableIndex(tableId);
		// The status should get set to available
		verify(mockTableManagerSupport, times(1)).attemptToSetTableStatusToAvailable(tableId, resetToken, "etag2");
		verify(mockTableManagerSupport, times(3)).attemptToUpdateTableProgress(eq(tableId), eq(resetToken), anyString(), anyLong(), anyLong());
		
		verify(mockTableIndexManager).applyChangeSetToIndex(tableId, sparseRowset1, 0L);
		verify(mockTableIndexManager).applyChangeSetToIndex(tableId, sparseRowset2, 1L);
		verify(mockTableIndexManager).optimizeTableIndices(tableId);
	}
	
	@Test
	public void testOneChangeAlreadyApplied() throws Exception{
		two.setObjectType(ObjectType.TABLE);
		two.setChangeType(ChangeType.UPDATE);
		two.setObjectEtag(resetToken);
		// For this case v0 is already applied to the index, while v1 is not.
		when(mockTableIndexManager.isVersionAppliedToIndex(tableId, 0L)).thenReturn(true);
		when(mockTableIndexManager.isVersionAppliedToIndex(tableId, 1L)).thenReturn(false);
		// call under test
		worker.run(mockProgressCallback, two);
		
		verify(mockTableIndexManager).applyChangeSetToIndex(tableId, sparseRowset2, 1L);
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
		when(mockTableManagerSupport.getColumnModelsForTable(tableId)).thenReturn(currentSchema);
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		TableRowChange trc = new TableRowChange();
		trc.setEtag("etag");
		trc.setRowVersion(3L);
		trc.setRowCount(12L);
		trc.setChangeType(TableChangeType.ROW);
		when(mockTableEntityManager.listRowSetsKeysForTable(tableId)).thenReturn(Arrays.asList(trc));
		when(mockTableIndexManager.isVersionAppliedToIndex(tableId, trc.getRowVersion())).thenReturn(false);
		RowSet rowSet = new RowSet();
		rowSet.setRows(Collections.singletonList(TableModelTestUtils.createRow(0L, 3L, "2")));
		rowSet.setTableId(tableId);
		rowSet.setHeaders(TableModelUtils.getSelectColumns(currentSchema));
		SparseChangeSet sparsRowSet = TableModelUtils.createSparseChangeSet(rowSet, currentSchema);
		when(mockTableEntityManager.getSparseChangeSet(trc)).thenReturn(sparsRowSet);
		two.setObjectType(ObjectType.TABLE);
		two.setChangeType(ChangeType.UPDATE);
		two.setObjectEtag(resetToken);
		// call under test
		worker.run(mockProgressCallback, two);
		// The connection factory should be called
		verify(mockConnectionFactory, times(1)).connectToTableIndex(tableId);
		// The status should get set to available
		verify(mockTableManagerSupport, times(1)).attemptToSetTableStatusToAvailable(tableId, resetToken, "etag");
		
		verify(mockTableIndexManager).applyChangeSetToIndex(tableId, sparsRowSet, trc.getRowVersion());
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
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		// This should trigger a failure
		RuntimeException error = new RuntimeException("Something went horribly wrong!");
		when(mockTableManagerSupport.getColumnModelsForTable(tableId)).thenThrow(error);
		two.setObjectType(ObjectType.TABLE);
		two.setChangeType(ChangeType.UPDATE);
		two.setObjectEtag(resetToken);
		// call under test
		worker.run(mockProgressCallback, two);
		// The connection factory should be called
		verify(mockConnectionFactory, times(1)).connectToTableIndex(tableId);
		// The status should get set to failed
		verify(mockTableManagerSupport, times(1)).attemptToSetTableStatusToFailed(anyString(), anyString(), any(Exception.class));
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
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		// Without a connection the message should go back to the queue
		when(mockConnectionFactory.connectToTableIndex(tableId)).thenThrow(new TableIndexConnectionUnavailableException("Not now"));
		two.setObjectType(ObjectType.TABLE);
		two.setChangeType(ChangeType.UPDATE);
		two.setObjectEtag(resetToken);
		two.setObjectId(tableId);
		try {
			// call under test
			worker.run(mockProgressCallback, two);
			fail();
		} catch (RecoverableMessageException e) {
			// expected
		}
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
		when(mockTableManagerSupport.startTableProcessing(tableId)).thenThrow(new NotFoundException("This table does not exist"));
		two.setObjectType(ObjectType.TABLE);
		two.setChangeType(ChangeType.UPDATE);
		two.setObjectEtag(resetToken);
		// call under test
		worker.run(mockProgressCallback, two);
		// The index connection should not be used.
		verifyZeroInteractions(mockTableIndexManager);
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
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		// Simulate a failure to get the lock
		when(mockTableManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class),anyString(), anyInt(), any(ProgressingCallable.class))).thenThrow(new LockUnavilableException("Cannot get a lock at this time"));
		two.setObjectType(ObjectType.TABLE);
		two.setChangeType(ChangeType.UPDATE);
		two.setObjectEtag(resetToken);
		try {
			// call under test
			worker.run(mockProgressCallback, two);
			fail();
		} catch (RecoverableMessageException e) {
			// expected
		}
		// The index connection should not be used.
		verifyZeroInteractions(mockTableIndexManager);
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
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		// Simulate a failure to get the lock
		when(mockTableManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class),anyString(), anyInt(), any(ProgressingCallable.class))).thenThrow(new InterruptedException("Sop!!!"));
		two.setObjectType(ObjectType.TABLE);
		two.setChangeType(ChangeType.UPDATE);
		two.setObjectEtag(resetToken);
		try {
			// call under test
			worker.run(mockProgressCallback, two);
			fail();
		} catch (RecoverableMessageException e) {
			// expected
		}
		// The index connection should not be used.
		verifyZeroInteractions(mockTableIndexManager);
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
		doThrow(new ConflictingUpdateException("Cannot get a lock at this time")).when(mockTableManagerSupport).attemptToSetTableStatusToAvailable(anyString(), anyString(), anyString());
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		two.setObjectId(tableId);
		two.setObjectType(ObjectType.TABLE);
		two.setChangeType(ChangeType.UPDATE);
		two.setObjectEtag(resetToken);
		// call under test
		worker.run(mockProgressCallback, two);
		// The connection factory should never be called
		verify(mockTableManagerSupport).attemptToSetTableStatusToAvailable(anyString(), anyString(), anyString());
	}
	
	
	/**
	 * For this case the second (and last) change set is broken so the job should fail.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBrokenChangeSetNotFixed() throws Exception{
		two.setObjectType(ObjectType.TABLE);
		two.setChangeType(ChangeType.UPDATE);
		two.setObjectEtag(resetToken);
		
		// this time the second change set has an error
		RuntimeException error = new RuntimeException("Bad Change Set");
		doThrow(error).when(mockTableIndexManager).applyChangeSetToIndex(tableId, sparseRowset2, 1L);
		
		// call under test
		worker.run(mockProgressCallback, two);
		
		verify(mockTableIndexManager).applyChangeSetToIndex(tableId, sparseRowset1, 0L);
		
		// The status should get set to failed
		verify(mockTableManagerSupport, times(1)).attemptToSetTableStatusToFailed(anyString(), anyString(), any(Exception.class));
	}
	
	@Test
	public void testNoWork() throws Exception {
		// setup no work
		when(mockTableManagerSupport.isIndexWorkRequired(tableId)).thenReturn(false);
		two.setObjectType(ObjectType.TABLE);
		two.setChangeType(ChangeType.UPDATE);
		two.setObjectEtag(resetToken);
		// call under test
		worker.run(mockProgressCallback, two);
		// no work should be performed.
		verifyZeroInteractions(mockTableIndexManager);
	}
	
	@Test
	public void testApplyRowChange() throws IOException{
		TableRowChange trc = new TableRowChange();
		trc.setEtag("etag");
		trc.setRowVersion(0L);
		trc.setRowCount(12L);
		trc.setIds(currentColumnIds);
		trc.setChangeType(TableChangeType.ROW);
		
		when(mockTableEntityManager.getSparseChangeSet(trc)).thenReturn(sparseRowset1);
		
		// call under test
		worker.applyRowChange(mockProgressCallback, mockTableIndexManager, tableId, trc);
		// schema of the change should be applied
		verify(mockTableIndexManager).setIndexSchema(tableId, isTableView, mockProgressCallback, currentSchema);
		// the change set should be applied.
		verify(mockTableIndexManager).applyChangeSetToIndex(tableId, sparseRowset1,trc.getRowVersion());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyRowChangeNullChange() throws IOException{
		TableRowChange trc = null;
		// call under test
		worker.applyRowChange(mockProgressCallback, mockTableIndexManager, tableId, trc);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyRowChangeWrongType() throws IOException{
		TableRowChange trc = new TableRowChange();
		trc.setChangeType(TableChangeType.COLUMN);
		// call under test
		worker.applyRowChange(mockProgressCallback, mockTableIndexManager, tableId, trc);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyColumnChangeNullChange() throws IOException{
		TableRowChange trc = null;
		// call under test
		worker.applyColumnChange(mockProgressCallback, mockTableIndexManager, tableId, trc);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyColumnChangeWrongType() throws IOException{
		TableRowChange trc = new TableRowChange();
		trc.setChangeType(TableChangeType.ROW);
		// call under test
		worker.applyColumnChange(mockProgressCallback, mockTableIndexManager, tableId, trc);
	}
	
	@Test
	public void testApplyColumnChange() throws IOException{
		List<String> columnIds = Lists.newArrayList("222");
		TableRowChange trc = new TableRowChange();
		trc.setEtag("etag");
		trc.setRowVersion(0L);
		trc.setRowCount(12L);
		trc.setIds(columnIds);
		trc.setChangeType(TableChangeType.COLUMN);
		
		ColumnModel oldColumn = TableModelTestUtils.createColumn(111L);
		ColumnModel newColumn = TableModelTestUtils.createColumn(222L);
		
		List<ColumnChangeDetails> details = Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn));
		when(mockTableEntityManager.getSchemaChangeForVersion(tableId, trc.getRowVersion())).thenReturn(details);
		// call under test
		worker.applyColumnChange(mockProgressCallback, mockTableIndexManager, tableId, trc);
		verify(mockTableIndexManager).updateTableSchema(tableId, isTableView, mockProgressCallback, details);
		verify(mockTableIndexManager).setIndexVersion(tableId, trc.getRowVersion());
	}
}
