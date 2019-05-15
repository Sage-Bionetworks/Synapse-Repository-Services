package org.sagebionetworks.table.worker;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
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
import org.sagebionetworks.repo.model.entity.IdAndVersion;
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

@RunWith(MockitoJUnitRunner.class)
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
	IdAndVersion oneIdAndVersion;
	ChangeMessage two;
	IdAndVersion twoIdAndVersion;
	String tableId;
	IdAndVersion idAndVersion;
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
		
		when(mockConnectionFactory.connectToTableIndex(any(IdAndVersion.class))).thenReturn(mockTableIndexManager);
		
		// By default we want to the manager to just call the passed callable.
		when(mockTableManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class),any(IdAndVersion.class), anyInt(), any(ProgressingCallable.class))).thenAnswer(new Answer<TableWorker.State>() {
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
		
		oneIdAndVersion = IdAndVersion.parse(one.getObjectId());
		
		two = new ChangeMessage();
		two.setChangeType(ChangeType.CREATE);
		two.setObjectId("456");
		two.setObjectType(ObjectType.ACTIVITY);
		two.setObjectEtag("etag");
		
		twoIdAndVersion = IdAndVersion.parse(two.getObjectId());
		
		
		tableId = "456";
		idAndVersion = IdAndVersion.parse(tableId);
		
		resetToken = "reset-token";
		currentSchema = Lists.newArrayList();
		currentSchema.add(TableModelTestUtils.createColumn(0L, "aString", ColumnType.STRING));
		currentColumnIds = TableModelUtils.getIds(currentSchema);
		when(mockTableManagerSupport.getColumnModelsForTable(idAndVersion)).thenReturn(currentSchema);
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
		when(mockTableIndexManager.isVersionAppliedToIndex(any(IdAndVersion.class), anyLong())).thenReturn(false);
		
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
		
		when(mockTableManagerSupport.startTableProcessing(idAndVersion)).thenReturn(resetToken);
		
		when(mockTableManagerSupport.isIndexWorkRequired(idAndVersion)).thenReturn(true);
		
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
		verify(mockConnectionFactory, times(1)).connectToTableIndex(twoIdAndVersion);
		// delete should be called
		verify(mockTableIndexManager, times(1)).deleteTableIndex(twoIdAndVersion);
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
		verify(mockTableManagerSupport, times(1)).startTableProcessing(idAndVersion);
		// The connection factory should be called
		verify(mockConnectionFactory, times(1)).connectToTableIndex(twoIdAndVersion);
		// The status should get set to available
		verify(mockTableManagerSupport, times(1)).attemptToSetTableStatusToAvailable(idAndVersion, resetToken, "etag2");
		verify(mockTableManagerSupport, times(3)).attemptToUpdateTableProgress(eq(idAndVersion), eq(resetToken), anyString(), anyLong(), anyLong());
		
		verify(mockTableIndexManager).applyChangeSetToIndex(twoIdAndVersion, sparseRowset1, 0L);
		verify(mockTableIndexManager).applyChangeSetToIndex(twoIdAndVersion, sparseRowset2, 1L);
		verify(mockTableIndexManager).optimizeTableIndices(twoIdAndVersion);
	}
	
	@Test
	public void testOneChangeAlreadyApplied() throws Exception{
		two.setObjectType(ObjectType.TABLE);
		two.setChangeType(ChangeType.UPDATE);
		two.setObjectEtag(resetToken);
		// For this case v0 is already applied to the index, while v1 is not.
		when(mockTableIndexManager.isVersionAppliedToIndex(twoIdAndVersion, 0L)).thenReturn(true);
		when(mockTableIndexManager.isVersionAppliedToIndex(twoIdAndVersion, 1L)).thenReturn(false);
		// call under test
		worker.run(mockProgressCallback, two);
		
		verify(mockTableIndexManager).applyChangeSetToIndex(twoIdAndVersion, sparseRowset2, 1L);
	}
	
	/**
	 * When everything works well, the message should be removed from the queue and the table status should be set to
	 * AVAILABLE.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testHappyUpdateCase() throws Exception {
		String resetToken = "reset-token";
		TableStatus status = new TableStatus();
		status.setResetToken(resetToken);
		when(mockTableManagerSupport.getColumnModelsForTable(idAndVersion)).thenReturn(currentSchema);
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(idAndVersion)).thenReturn(status);
		TableRowChange trc = new TableRowChange();
		trc.setEtag("etag");
		trc.setRowVersion(3L);
		trc.setRowCount(12L);
		trc.setChangeType(TableChangeType.ROW);
		when(mockTableEntityManager.listRowSetsKeysForTable(tableId)).thenReturn(Arrays.asList(trc));
		when(mockTableIndexManager.isVersionAppliedToIndex(idAndVersion, trc.getRowVersion())).thenReturn(false);
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
		verify(mockConnectionFactory, times(1)).connectToTableIndex(idAndVersion);
		// The status should get set to available
		verify(mockTableManagerSupport, times(1)).attemptToSetTableStatusToAvailable(idAndVersion, resetToken, "etag");
		
		verify(mockTableIndexManager).applyChangeSetToIndex(idAndVersion, sparsRowSet, trc.getRowVersion());
	}
	
	/**
	 * When an unknown exception is thrown the table status must get set to failed.
	 * @throws Exception
	 */
	@Test
	public void testSetToFailed() throws Exception{
		String resetToken = "reset-token";
		TableStatus status = new TableStatus();
		status.setResetToken(resetToken);
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(idAndVersion)).thenReturn(status);
		// This should trigger a failure
		RuntimeException error = new RuntimeException("Something went horribly wrong!");
		when(mockTableManagerSupport.getColumnModelsForTable(idAndVersion)).thenThrow(error);
		two.setObjectType(ObjectType.TABLE);
		two.setChangeType(ChangeType.UPDATE);
		two.setObjectEtag(resetToken);
		// call under test
		worker.run(mockProgressCallback, two);
		// The connection factory should be called
		verify(mockConnectionFactory, times(1)).connectToTableIndex(idAndVersion);
		// The status should get set to failed
		verify(mockTableManagerSupport, times(1)).attemptToSetTableStatusToFailed(any(IdAndVersion.class), anyString(), any(Exception.class));
	}
	
	/**
	 * If a connection to the cluster cannot be made at this time, then the message must return the queue
	 * as we can recover from this error when a database becomes available.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNoConnection() throws Exception{
		String resetToken = "reset-token";
		TableStatus status = new TableStatus();
		status.setResetToken(resetToken);
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(idAndVersion)).thenReturn(status);
		// Without a connection the message should go back to the queue
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenThrow(new TableIndexConnectionUnavailableException("Not now"));
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
		when(mockTableManagerSupport.startTableProcessing(idAndVersion)).thenThrow(new NotFoundException("This table does not exist"));
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
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(idAndVersion)).thenReturn(status);
		// Simulate a failure to get the lock
		when(mockTableManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class),any(IdAndVersion.class), anyInt(), any(ProgressingCallable.class))).thenThrow(new LockUnavilableException("Cannot get a lock at this time"));
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
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(idAndVersion)).thenReturn(status);
		// Simulate a failure to get the lock
		when(mockTableManagerSupport.tryRunWithTableExclusiveLock(any(ProgressCallback.class),any(IdAndVersion.class), anyInt(), any(ProgressingCallable.class))).thenThrow(new InterruptedException("Sop!!!"));
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
		doThrow(new ConflictingUpdateException("Cannot get a lock at this time")).when(mockTableManagerSupport).attemptToSetTableStatusToAvailable(any(IdAndVersion.class), anyString(), anyString());
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(idAndVersion)).thenReturn(status);
		two.setObjectId(tableId);
		two.setObjectType(ObjectType.TABLE);
		two.setChangeType(ChangeType.UPDATE);
		two.setObjectEtag(resetToken);
		// call under test
		worker.run(mockProgressCallback, two);
		// The connection factory should never be called
		verify(mockTableManagerSupport).attemptToSetTableStatusToAvailable(any(IdAndVersion.class), anyString(), anyString());
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
		doThrow(error).when(mockTableIndexManager).applyChangeSetToIndex(twoIdAndVersion, sparseRowset2, 1L);
		
		// call under test
		worker.run(mockProgressCallback, two);
		
		verify(mockTableIndexManager).applyChangeSetToIndex(twoIdAndVersion, sparseRowset1, 0L);
		
		// The status should get set to failed
		verify(mockTableManagerSupport, times(1)).attemptToSetTableStatusToFailed(any(IdAndVersion.class), anyString(), any(Exception.class));
	}
	
	@Test
	public void testNoWork() throws Exception {
		// setup no work
		when(mockTableManagerSupport.isIndexWorkRequired(idAndVersion)).thenReturn(false);
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
		trc.setChangeType(TableChangeType.ROW);
		
		when(mockTableEntityManager.getSparseChangeSet(trc)).thenReturn(sparseRowset1);
		
		// call under test
		worker.applyRowChange(mockTableIndexManager, idAndVersion, trc);
		// schema of the change should be applied
		verify(mockTableIndexManager).setIndexSchema(idAndVersion, isTableView, currentSchema);
		// the change set should be applied.
		verify(mockTableIndexManager).applyChangeSetToIndex(idAndVersion, sparseRowset1,trc.getRowVersion());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyRowChangeNullChange() throws IOException{
		TableRowChange trc = null;
		// call under test
		worker.applyRowChange(mockTableIndexManager, idAndVersion, trc);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyRowChangeWrongType() throws IOException{
		TableRowChange trc = new TableRowChange();
		trc.setChangeType(TableChangeType.COLUMN);
		// call under test
		worker.applyRowChange(mockTableIndexManager, idAndVersion, trc);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyColumnChangeNullChange() throws IOException{
		TableRowChange trc = null;
		// call under test
		worker.applyColumnChange(mockTableIndexManager, idAndVersion, trc);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyColumnChangeWrongType() throws IOException{
		TableRowChange trc = new TableRowChange();
		trc.setChangeType(TableChangeType.ROW);
		// call under test
		worker.applyColumnChange(mockTableIndexManager, idAndVersion, trc);
	}
	
	@Test
	public void testApplyColumnChange() throws IOException{
		TableRowChange trc = new TableRowChange();
		trc.setEtag("etag");
		trc.setRowVersion(0L);
		trc.setRowCount(12L);
		trc.setChangeType(TableChangeType.COLUMN);
		
		ColumnModel oldColumn = TableModelTestUtils.createColumn(111L);
		ColumnModel newColumn = TableModelTestUtils.createColumn(222L);
		
		List<ColumnChangeDetails> details = Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn));
		when(mockTableEntityManager.getSchemaChangeForVersion(tableId, trc.getRowVersion())).thenReturn(details);
		// call under test
		worker.applyColumnChange(mockTableIndexManager, idAndVersion, trc);
		verify(mockTableIndexManager).updateTableSchema(idAndVersion, isTableView, details);
		verify(mockTableIndexManager).setIndexVersion(idAndVersion, trc.getRowVersion());
	}
}
