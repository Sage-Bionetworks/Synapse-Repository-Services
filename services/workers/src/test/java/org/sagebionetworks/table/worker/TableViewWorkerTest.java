package org.sagebionetworks.table.worker;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionUnavailableException;
import org.sagebionetworks.repo.manager.table.TableIndexManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.RowBatchHandler;
import org.sagebionetworks.repo.model.dbo.dao.table.FileEntityFields;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.test.util.ReflectionTestUtils;

public class TableViewWorkerTest {

	@Mock
	TableViewManager tableViewManager;
	@Mock
	TableManagerSupport tableManagerSupport;
	@Mock
	TableIndexConnectionFactory connectionFactory;
	@Mock
	TableIndexManager indexManager;
	@Mock
	ProgressCallback<ChangeMessage> outerCallback;
	@Mock
	ProgressCallback<ChangeMessage> innerCallback;

	TableViewWorker worker;

	String tableId;
	ChangeMessage change;
	String token;
	
	List<ColumnModel> schema;
	Long viewCRC;
	long rowCount;
	List<Row> rows;

	@SuppressWarnings("unchecked")
	@Before
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);

		worker = new TableViewWorker();
		ReflectionTestUtils.setField(worker, "tableViewManager",
				tableViewManager);
		ReflectionTestUtils.setField(worker, "tableManagerSupport",
				tableManagerSupport);
		ReflectionTestUtils.setField(worker, "connectionFactory",
				connectionFactory);

		tableId = "123";
		change = new ChangeMessage();
		change.setChangeNumber(99L);
		change.setChangeType(ChangeType.CREATE);
		change.setObjectId(tableId);
		change.setObjectType(ObjectType.FILE_VIEW);
		
		token = "statusToken";
		
		// setup default responses
	
		when(tableManagerSupport.startTableProcessing(tableId)).thenReturn(token);
		when(tableManagerSupport.isIndexSynchronizedWithTruth(tableId)).thenReturn(false);

		when(connectionFactory.connectToTableIndex(tableId)).thenReturn(
				indexManager);

		// By default the lock should proceed with the callback.
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				ProgressingCallable<Void, ChangeMessage> callable = (ProgressingCallable<Void, ChangeMessage>) invocation
						.getArguments()[3];
				// pass it along
				if(callable != null){
					callable.call(innerCallback);
				}
				return null;
			}
		}).when(tableManagerSupport).tryRunWithTableExclusiveLock(
				any(ProgressCallback.class), anyString(), anyInt(),
				any(ProgressingCallable.class));
		
		schema = FileEntityFields.getAllColumnModels();
		// Add an ID to each column.
		for(int i=0; i<schema.size(); i++){
			ColumnModel cm = schema.get(i);
			cm.setId(""+i);
		}
		
		rowCount = 1;
		rows = new LinkedList<Row>();
		for(long i=0; i<rowCount; i++){
			Row row = new Row();
			row.setRowId(i);
			rows.add(row);
		}

		when(tableViewManager.getViewSchemaWithBenefactor(tableId)).thenReturn(schema);
		viewCRC = 888L;
		doAnswer(new Answer<Long>(){
			@Override
			public Long answer(InvocationOnMock invocation) throws Throwable {
				RowBatchHandler handler = (RowBatchHandler) invocation.getArguments()[4];
				handler.nextBatch(rows, 0, rowCount);
				return viewCRC;
			}}).when(tableViewManager).streamOverAllEntitiesInViewAsBatch(anyString(), any(EntityType.class), anyListOf(ColumnModel.class), anyInt(), any(RowBatchHandler.class));
	}

	@Test
	public void testRunNonFileViewChagne() throws Exception {
		// this worker should not respond to TABLE changes.
		change.setObjectType(ObjectType.TABLE);
		// call under test
		worker.run(outerCallback, change);
		verify(connectionFactory, never()).connectToTableIndex(anyString());
	}

	@Test
	public void testRunDelete() throws Exception {
		change.setChangeType(ChangeType.DELETE);
		// call under test
		worker.run(outerCallback, change);
		verify(indexManager).deleteTableIndex();
	}

	@Test(expected = RecoverableMessageException.class)
	public void testRunConnectionUnavailable() throws Exception {
		// simulate no connection
		when(connectionFactory.connectToTableIndex(tableId)).thenThrow(
				new TableIndexConnectionUnavailableException("No connection"));
		// call under test
		worker.run(outerCallback, change);
	}

	@SuppressWarnings("unchecked")
	@Test(expected = RecoverableMessageException.class)
	public void testRunLockUnavilableException() throws Exception {
		// setup no lock
		when(
				tableManagerSupport.tryRunWithTableExclusiveLock(
						any(ProgressCallback.class), anyString(), anyInt(),
						any(ProgressingCallable.class))).thenThrow(
				new LockUnavilableException("No lock for you"));
		// call under test
		worker.run(outerCallback, change);
	}

	@SuppressWarnings("unchecked")
	@Test(expected = RecoverableMessageException.class)
	public void testRunLownerRecoverableMessageException() throws Exception {
		// If this exception is caught it must be re-thrown.
		when(
				tableManagerSupport.tryRunWithTableExclusiveLock(
						any(ProgressCallback.class), anyString(), anyInt(),
						any(ProgressingCallable.class))).thenThrow(
				new RecoverableMessageException("Should get re-thrown"));
		// call under test
		worker.run(outerCallback, change);
	}
	
	@Test
	public void testRunIsIndexSynchronizedTrue() throws Exception{
		// Setup the synched state
		when(tableManagerSupport.isIndexSynchronizedWithTruth(tableId)).thenReturn(true);
		// call under test
		worker.run(outerCallback, change);
		// progress should not start for this case
		verify(tableManagerSupport, never()).startTableProcessing(tableId);
	}
	
	@Test
	public void testRunIsIndexSynchronizedFalse() throws Exception{
		// Setup the synched state
		when(tableManagerSupport.isIndexSynchronizedWithTruth(tableId)).thenReturn(false);
		// call under test
		worker.run(outerCallback, change);
		// progress should start for this case
		verify(tableManagerSupport).startTableProcessing(tableId);
	}
	
	@Test
	public void testCreateOrUpdateIndexHoldingLock() throws RecoverableMessageException{
		// call under test
		worker.createOrUpdateIndexHoldingLock(tableId, indexManager, innerCallback, change);
		
		verify(indexManager).deleteTableIndex();
		verify(indexManager).setIndexSchema(schema);
		verify(innerCallback, times(1)).progressMade(change);
		verify(tableManagerSupport, times(1)).attemptToUpdateTableProgress(tableId, token, "Building view...", 0L, 1L);
		verify(indexManager, times(1)).applyChangeSetToIndex(any(RowSet.class), anyListOf(ColumnModel.class));
		verify(indexManager).setIndexVersion(viewCRC);
		verify(tableManagerSupport).attemptToSetTableStatusToAvailable(tableId, token, TableViewWorker.DEFAULT_ETAG);
	}

}
