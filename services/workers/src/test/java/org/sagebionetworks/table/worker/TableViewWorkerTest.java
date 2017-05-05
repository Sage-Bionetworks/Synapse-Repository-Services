package org.sagebionetworks.table.worker;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.EntityField;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Sets;

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
	ProgressCallback<Void> outerCallback;
	@Mock
	ProgressCallback<Void> innerCallback;

	TableViewWorker worker;

	String tableId;
	ChangeMessage change;
	String token;
	
	List<ColumnModel> schema;
	List<ColumnModel> expandedSchema;
	Long viewCRC;
	long rowCount;
	List<Row> rows;
	Set<Long> viewScope;
	String schemaMD5Hex;

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
		change.setObjectType(ObjectType.ENTITY_VIEW);
		
		token = "statusToken";
		
		// setup default responses
		viewScope = Sets.newHashSet(1L,2L);
	
		when(tableManagerSupport.startTableProcessing(tableId)).thenReturn(token);
		when(tableManagerSupport.isIndexSynchronizedWithTruth(tableId)).thenReturn(false);
		when(tableManagerSupport.getAllContainerIdsForViewScope(tableId)).thenReturn(viewScope);
		when(tableManagerSupport.getViewType(tableId)).thenReturn(ViewType.file);

		when(connectionFactory.connectToTableIndex(tableId)).thenReturn(
				indexManager);

		// By default the lock should proceed with the callback.
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				ProgressingCallable<Void, Void> callable = (ProgressingCallable<Void, Void>) invocation
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
		
		schema = new LinkedList<>();
		schema.add(EntityField.id.getColumnModel());
		schema.add(EntityField.name.getColumnModel());
		
		expandedSchema = new LinkedList<>(schema);
		expandedSchema.add(EntityField.id.getColumnModel());
		expandedSchema.add(EntityField.name.getColumnModel());
		// add benefactor and etag to the expanded.
		expandedSchema.add(EntityField.etag.getColumnModel());
		expandedSchema.add(EntityField.benefactorId.getColumnModel());
		// Add an ID to each column.
		for(int i=0; i<schema.size(); i++){
			ColumnModel cm = schema.get(i);
			cm.setId(""+i);
		}
		for(int i=0; i<expandedSchema.size(); i++){
			ColumnModel cm = expandedSchema.get(i);
			cm.setId(""+i);
		}
		
		schemaMD5Hex = TableModelUtils.createSchemaMD5HexCM(schema);
		
		rowCount = 1;
		rows = new LinkedList<Row>();
		for(long i=0; i<rowCount; i++){
			Row row = new Row();
			row.setRowId(i);
			rows.add(row);
		}
		when(tableManagerSupport.getColumnModelsForTable(tableId)).thenReturn(schema);
		when(tableViewManager.getViewSchemaWithRequiredColumns(tableId)).thenReturn(expandedSchema);
		viewCRC = 888L;		
		when(indexManager.populateViewFromEntityReplication(tableId, innerCallback, ViewType.file, viewScope,expandedSchema)).thenReturn(viewCRC);
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
		verify(indexManager).deleteTableIndex(tableId);
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
	public void testRunWithRecoverableMessageException() throws Exception {
		// If this exception is caught it must be re-thrown.
		when(
				tableManagerSupport.tryRunWithTableExclusiveLock(
						any(ProgressCallback.class), anyString(), anyInt(),
						any(ProgressingCallable.class))).thenThrow(
				new RecoverableMessageException("Should get re-thrown"));
		// call under test
		worker.run(outerCallback, change);
	}
	
	/**
	 * See PLFM-4366. When no work is needed on the view, the view's state must
	 * be set to available.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRunIsIndexSynchronizedTrue() throws Exception{
		// Setup the synched state
		when(tableManagerSupport.isIndexSynchronizedWithTruth(tableId)).thenReturn(true);
		// call under test
		worker.run(outerCallback, change);
		// The view's state must be set to available 
		verify(tableManagerSupport).startTableProcessing(tableId);
		verify(tableManagerSupport).attemptToSetTableStatusToAvailable(tableId, token, TableViewWorker.ALREADY_SYNCHRONIZED);
				
		// no work should occur on the view
		verifyZeroInteractions(indexManager);
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
		
		verify(indexManager).deleteTableIndex(tableId);
		verify(indexManager).setIndexSchema(tableId, innerCallback,expandedSchema);
		verify(tableViewManager).getViewSchemaWithRequiredColumns(tableId);
		verify(innerCallback, times(4)).progressMade(null);
		verify(tableManagerSupport, times(1)).attemptToUpdateTableProgress(tableId, token, "Copying data to view...", 0L, 1L);
		verify(indexManager, times(1)).populateViewFromEntityReplication(tableId, innerCallback, ViewType.file, viewScope,expandedSchema);
		verify(indexManager).setIndexVersionAndSchemaMD5Hex(tableId, viewCRC, schemaMD5Hex);
		verify(tableManagerSupport).attemptToSetTableStatusToAvailable(tableId, token, TableViewWorker.DEFAULT_ETAG);
		verify(indexManager).optimizeTableIndices(tableId);
	}
	
	/**
	 * Test added for PLFM-4223.  An exception should result in setting the view to failed.
	 * 
	 * @throws RecoverableMessageException
	 */
	@Test
	public void testCreateOrUpdateIndexHoldingLockException() throws RecoverableMessageException{
		IllegalArgumentException exception = new IllegalArgumentException("failure");
		when(indexManager.populateViewFromEntityReplication(tableId, innerCallback, ViewType.file, viewScope,expandedSchema)).thenThrow(exception);
		
		try {
			// call under test
			worker.createOrUpdateIndexHoldingLock(tableId, indexManager, innerCallback, change);
			fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {
			// expected
		}
		verify(tableManagerSupport, never()).attemptToSetTableStatusToAvailable(tableId, token, TableViewWorker.DEFAULT_ETAG);
		verify(tableManagerSupport).attemptToSetTableStatusToFailed(tableId, token, exception);
	}

}
