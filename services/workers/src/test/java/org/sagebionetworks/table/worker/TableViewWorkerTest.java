package org.sagebionetworks.table.worker;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionUnavailableException;
import org.sagebionetworks.repo.manager.table.TableIndexManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.EntityField;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;

import com.google.common.collect.Sets;

@RunWith(MockitoJUnitRunner.class)
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
	ProgressCallback outerCallback;
	@Mock
	ProgressCallback innerCallback;

	@InjectMocks
	TableViewWorker worker;

	String tableId;
	IdAndVersion idAndVersion;
	Long tableViewIdLong;
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

		tableId = "123";
		idAndVersion = IdAndVersion.parse(tableId);
		tableViewIdLong = KeyFactory.stringToKey(tableId);
		change = new ChangeMessage();
		change.setChangeNumber(99L);
		change.setChangeType(ChangeType.CREATE);
		change.setObjectId(tableId);
		change.setObjectType(ObjectType.ENTITY_VIEW);
		
		token = "statusToken";
		
		// setup default responses
		viewScope = Sets.newHashSet(1L,2L);
	
		when(tableManagerSupport.startTableProcessing(idAndVersion)).thenReturn(token);
		when(tableManagerSupport.isIndexWorkRequired(idAndVersion)).thenReturn(true);
		when(tableManagerSupport.getAllContainerIdsForViewScope(idAndVersion, ViewTypeMask.File.getMask())).thenReturn(viewScope);
		when(tableManagerSupport.getViewTypeMask(idAndVersion)).thenReturn(ViewTypeMask.File.getMask());

		when(connectionFactory.connectToTableIndex(idAndVersion)).thenReturn(
				indexManager);

		// By default the lock should proceed with the callback.
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				ProgressingCallable<Void> callable = (ProgressingCallable<Void>) invocation
						.getArguments()[3];
				// pass it along
				if(callable != null){
					callable.call(innerCallback);
				}
				return null;
			}
		}).when(tableManagerSupport).tryRunWithTableExclusiveLock(
				any(ProgressCallback.class), any(IdAndVersion.class), anyInt(),
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
		
		List<String> columnIds = TableModelUtils.getIds(schema);
		schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(columnIds);
		when(tableManagerSupport.getSchemaMD5Hex(idAndVersion)).thenReturn(schemaMD5Hex);
		
		rowCount = 1;
		rows = new LinkedList<Row>();
		for(long i=0; i<rowCount; i++){
			Row row = new Row();
			row.setRowId(i);
			rows.add(row);
		}
		when(tableManagerSupport.getColumnModelsForTable(idAndVersion)).thenReturn(schema);
		when(tableViewManager.getViewSchema(tableId)).thenReturn(expandedSchema);
		long viewIdLong = KeyFactory.stringToKey(tableId);
		viewCRC = 888L;		
		when(indexManager.populateViewFromEntityReplication(viewIdLong, innerCallback, ViewTypeMask.File.getMask(), viewScope,expandedSchema)).thenReturn(viewCRC);
	}

	@Test
	public void testRunNonFileViewChagne() throws Exception {
		// this worker should not respond to TABLE changes.
		change.setObjectType(ObjectType.TABLE);
		// call under test
		worker.run(outerCallback, change);
		verify(connectionFactory, never()).connectToTableIndex(any(IdAndVersion.class));
	}

	@Test
	public void testRunDelete() throws Exception {
		change.setChangeType(ChangeType.DELETE);
		// call under test
		worker.run(outerCallback, change);
		verify(indexManager).deleteTableIndex(idAndVersion);
	}

	@Test(expected = RecoverableMessageException.class)
	public void testRunConnectionUnavailable() throws Exception {
		// simulate no connection
		when(connectionFactory.connectToTableIndex(idAndVersion)).thenThrow(
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
						any(ProgressCallback.class), any(IdAndVersion.class), anyInt(),
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
						any(ProgressCallback.class), any(IdAndVersion.class), anyInt(),
						any(ProgressingCallable.class))).thenThrow(
				new RecoverableMessageException("Should get re-thrown"));
		// call under test
		worker.run(outerCallback, change);
	}
	
	@Test
	public void testRunIsIndexWorkRequiredFalse() throws Exception{
		// Setup the synched state
		when(tableManagerSupport.isIndexWorkRequired(idAndVersion)).thenReturn(false);
		// call under test
		worker.run(outerCallback, change);
		// progress should not start for this case
		verify(tableManagerSupport, never()).startTableProcessing(idAndVersion);
	}
	
	@Test
	public void testRunIsIndexWorkRequiredTrue() throws Exception{
		// Setup the synched state
		when(tableManagerSupport.isIndexSynchronizedWithTruth(idAndVersion)).thenReturn(true);
		// call under test
		worker.run(outerCallback, change);
		// progress should start for this case
		verify(tableManagerSupport).startTableProcessing(idAndVersion);
	}
	
	@Test
	public void testCreateOrUpdateIndexHoldingLock() throws RecoverableMessageException{
		// call under test
		worker.createOrUpdateIndexHoldingLock(idAndVersion, indexManager, innerCallback, change);
		
		verify(indexManager).deleteTableIndex(idAndVersion);
		boolean isTableView = true;
		verify(indexManager).setIndexSchema(idAndVersion, isTableView,expandedSchema);
		verify(tableViewManager).getViewSchema(tableId);
		verify(tableManagerSupport, times(1)).attemptToUpdateTableProgress(idAndVersion, token, "Copying data to view...", 0L, 1L);
		Long viewIdLong = KeyFactory.stringToKey(tableId);
		verify(indexManager, times(1)).populateViewFromEntityReplication(viewIdLong, innerCallback, ViewTypeMask.File.getMask(), viewScope,expandedSchema);
		verify(indexManager).setIndexVersionAndSchemaMD5Hex(idAndVersion, viewCRC, schemaMD5Hex);
		verify(tableManagerSupport).attemptToSetTableStatusToAvailable(idAndVersion, token, TableViewWorker.DEFAULT_ETAG);
		verify(indexManager).optimizeTableIndices(idAndVersion);
	}
	
	/**
	 * Test added for PLFM-4223.  An exception should result in setting the view to failed.
	 * 
	 * @throws RecoverableMessageException
	 */
	@Test
	public void testCreateOrUpdateIndexHoldingLockException() throws RecoverableMessageException{
		IllegalArgumentException exception = new IllegalArgumentException("failure");
		Long viewIdLong = KeyFactory.stringToKey(tableId);
		when(indexManager.populateViewFromEntityReplication(viewIdLong, innerCallback, ViewTypeMask.File.getMask(), viewScope,expandedSchema)).thenThrow(exception);
		
		try {
			// call under test
			worker.createOrUpdateIndexHoldingLock(idAndVersion, indexManager, innerCallback, change);
			fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {
			// expected
		}
		verify(tableManagerSupport, never()).attemptToSetTableStatusToAvailable(idAndVersion, token, TableViewWorker.DEFAULT_ETAG);
		verify(tableManagerSupport).attemptToSetTableStatusToFailed(idAndVersion, token, exception);
	}

}
