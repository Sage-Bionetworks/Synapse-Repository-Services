package org.sagebionetworks.repo.manager.table;



import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.AppendableRowSet;
import org.sagebionetworks.repo.model.table.AppendableRowSetRequest;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.EntityUpdateFailureCode;
import org.sagebionetworks.repo.model.table.EntityUpdateResult;
import org.sagebionetworks.repo.model.table.EntityUpdateResults;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.model.table.SparseRowDto;
import org.sagebionetworks.repo.model.table.TableSchemaChangeRequest;
import org.sagebionetworks.repo.model.table.TableSchemaChangeResponse;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScopeType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class TableViewTransactionManagerTest {
	
	@Mock
	TableManagerSupport mockTableManagerSupport;
	@Mock
	TableViewManager mockTableViewManger;
	@Mock
	ProgressCallback mockProgressCallback;
	@Mock
	TableUploadManager mockTableUploadManager;
	@Mock
	StackConfiguration mockStackConfig;
	@InjectMocks
	TableViewTransactionManager manager;
	
	UserInfo user;
	String viewId;
	IdAndVersion idAndVersion;
	TableSchemaChangeRequest schemaChangeRequest;
	List<ColumnChange> columnChanges;
	List<ColumnModel> schema;
	TableUpdateTransactionRequest request;
	
	PartialRowSet partialRowSet;
	RowSet rowSet;
	SparseChangeSet sparseChangeSet;
	AppendableRowSetRequest appendAbleRowSetRequest;
	UploadToTableRequest uploadToTableRequest;
	UploadToTableResult uploadToTableResult;
	SparseRowDto sparseRow;
	List<String> orderedColumnIds;
	ViewScopeType viewScopeType;
	
	@BeforeEach
	public void beforeEach(){
		user = new UserInfo(false, 12L);
		viewId = "syn213";
		idAndVersion  = IdAndVersion.parse(viewId);
		schema = TableModelTestUtils.createColumsWithNames("one", "two");
		
		ColumnChange change = new ColumnChange();
		change.setNewColumnId(schema.get(0).getId());
		
		ColumnChange change2 = new ColumnChange();
		change2.setNewColumnId(schema.get(1).getId());
		columnChanges = Lists.newArrayList(change, change2);
		
		schemaChangeRequest = new TableSchemaChangeRequest();
		schemaChangeRequest.setChanges(columnChanges);
		schemaChangeRequest.setEntityId(viewId);
		orderedColumnIds = Arrays.asList(schema.get(0).getId(), schema.get(1).getId());
		schemaChangeRequest.setOrderedColumnIds(orderedColumnIds);
		
		List<TableUpdateRequest> changes = new LinkedList<>();
		changes.add(schemaChangeRequest);
		request = new TableUpdateTransactionRequest();
		request.setEntityId(viewId);
		request.setChanges(changes);
		
		partialRowSet = new PartialRowSet();
		partialRowSet.setTableId(viewId);
		partialRowSet.setRows(TableModelTestUtils.createPartialRows(schema, 2));
		
		rowSet = new RowSet();
		rowSet.setTableId(viewId);
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setRows(TableModelTestUtils.createRows(schema, 2));
		
		sparseChangeSet = TableModelUtils.createSparseChangeSet(rowSet, schema);
		sparseRow = sparseChangeSet.writeToDto().getRows().get(0);
		sparseRow.setRowId(123l);
		
		appendAbleRowSetRequest = new AppendableRowSetRequest();
		appendAbleRowSetRequest.setEntityId(viewId);
		
		uploadToTableRequest = new UploadToTableRequest();
		uploadToTableRequest.setTableId(viewId);
		
		uploadToTableResult = new UploadToTableResult();
		
		viewScopeType = new ViewScopeType(ViewObjectType.ENTITY, ViewTypeMask.File.getMask());
	}
	
	@Test
	public void testApplySchemaChange(){
		when(mockTableViewManger.applySchemaChange(user, viewId, columnChanges, orderedColumnIds)).thenReturn(schema);
		
		// call under test
		TableSchemaChangeResponse response = manager.applySchemaChange(user, schemaChangeRequest);
		assertNotNull(response);
		assertEquals(schema, response.getSchema());
	}
	
	@Test
	public void testApplyChangePartialRowSet(){
		when(mockStackConfig.getTableMaxBytesPerRequest()).thenReturn(Integer.MAX_VALUE);
		when(mockTableManagerSupport.getTableSchema(idAndVersion)).thenReturn(schema);
		when(mockTableManagerSupport.getViewScopeType(any())).thenReturn(viewScopeType);
		
		ViewObjectType objectType = viewScopeType.getObjectType();
		
		appendAbleRowSetRequest.setToAppend(partialRowSet);
		// call under test
		TableUpdateResponse result = manager.applyChange(mockProgressCallback, user, appendAbleRowSetRequest);
		assertNotNull(result);
		verify(mockTableViewManger, times(2)).updateRowInView(eq(user), eq(schema), eq(objectType), any(SparseRowDto.class));
	}
	
	@Test
	public void testApplyChangeRowSet(){
		when(mockStackConfig.getTableMaxBytesPerRequest()).thenReturn(Integer.MAX_VALUE);
		when(mockTableManagerSupport.getTableSchema(idAndVersion)).thenReturn(schema);
		when(mockTableManagerSupport.getViewScopeType(any())).thenReturn(viewScopeType);
		
		ViewObjectType objectType = viewScopeType.getObjectType();
		
		appendAbleRowSetRequest.setToAppend(rowSet);
		// call under test
		TableUpdateResponse result = manager.applyChange(mockProgressCallback, user, appendAbleRowSetRequest);
		assertNotNull(result);
		verify(mockTableViewManger, times(2)).updateRowInView(eq(user), eq(schema), eq(objectType), any(SparseRowDto.class));
	}
	
	@Test
	public void testApplyChangeUploadTable(){
		// setup 
		doAnswer(new Answer<TableUpdateResponse>(){
			@Override
			public TableUpdateResponse answer(InvocationOnMock invocation)
					throws Throwable {
				UploadRowProcessor processor = (UploadRowProcessor) invocation.getArguments()[3];
				return processor.processRows(user, viewId, schema, sparseChangeSet.writeToDto().getRows().iterator(), null, mockProgressCallback);
			}}).when(mockTableUploadManager).uploadCSV(any(ProgressCallback.class), any(UserInfo.class), any(UploadToTableRequest.class), any(UploadRowProcessor.class));
		
		when(mockTableManagerSupport.getViewScopeType(any())).thenReturn(viewScopeType);
		
		ViewObjectType objectType = viewScopeType.getObjectType();
		
		// call under test
		TableUpdateResponse result = manager.applyChange(mockProgressCallback, user, uploadToTableRequest);
		assertNotNull(result);
		verify(mockTableViewManger, times(2)).updateRowInView(eq(user), eq(schema), eq(objectType), any(SparseRowDto.class));
	}
	
	@Test
	public void testApplyChangeUnknownType() {
		TableUpdateRequest unknown = Mockito.mock(TableUpdateRequest.class);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.applyChange(mockProgressCallback, user, unknown);
		});
	}
	
	@Test
	public void testUpdateTableWithTransaction() throws RecoverableMessageException, TableUnavailableException{
		when(mockTableViewManger.applySchemaChange(user, viewId, columnChanges, orderedColumnIds)).thenReturn(schema);
	
		// call under test
		TableUpdateTransactionResponse response = manager.updateTableWithTransaction(mockProgressCallback, user, request);
		assertNotNull(response);
		assertNotNull(response.getResults());
		assertEquals(1, response.getResults().size());
		TableUpdateResponse single =  response.getResults().get(0);
		assertTrue(single instanceof TableSchemaChangeResponse);
		TableSchemaChangeResponse schemaResponse = (TableSchemaChangeResponse) single;
		assertEquals(schema, schemaResponse.getSchema());
		// user must have write on the view.
		verify(mockTableManagerSupport).validateTableWriteAccess(user, idAndVersion);
		verify(mockTableViewManger, never()).createSnapshot(any(UserInfo.class), anyString(), any(SnapshotRequest.class));
	}
	
	@Test
	public void testUpdateTableWithTransactionWithSnapshot() throws RecoverableMessageException, TableUnavailableException{
		when(mockTableViewManger.applySchemaChange(user, viewId, columnChanges, orderedColumnIds)).thenReturn(schema);

		request.setCreateSnapshot(true);
		SnapshotRequest snapshotOptions = new SnapshotRequest();
		snapshotOptions.setSnapshotLabel("some label");
		request.setSnapshotOptions(snapshotOptions);
		// call under test
		TableUpdateTransactionResponse response = manager.updateTableWithTransaction(mockProgressCallback, user, request);
		assertNotNull(response);
		assertNotNull(response.getResults());
		assertEquals(1, response.getResults().size());
		TableUpdateResponse single =  response.getResults().get(0);
		assertTrue(single instanceof TableSchemaChangeResponse);
		TableSchemaChangeResponse schemaResponse = (TableSchemaChangeResponse) single;
		assertEquals(schema, schemaResponse.getSchema());
		verify(mockTableManagerSupport).validateTableWriteAccess(user, idAndVersion);
		// calls create snapshot.
		verify(mockTableViewManger).createSnapshot(user, request.getEntityId(), snapshotOptions);
	}
	
	@Test
	public void testUpdateTableWithTransactionWithSnapshotNulChanges() throws RecoverableMessageException, TableUnavailableException{
		request.setChanges(null);
		request.setCreateSnapshot(true);
		SnapshotRequest snapshotOptions = new SnapshotRequest();
		snapshotOptions.setSnapshotLabel("some label");
		request.setSnapshotOptions(snapshotOptions);
		// call under test
		TableUpdateTransactionResponse response = manager.updateTableWithTransaction(mockProgressCallback, user, request);
		assertNotNull(response);
		assertNotNull(response.getResults());
		assertTrue(response.getResults().isEmpty());
		// calls create snapshot.
		verify(mockTableViewManger).createSnapshot(user, request.getEntityId(), snapshotOptions);
	}
	
	@Test
	public void testUpdateTableWithTransactionWithSnapshotNull() throws RecoverableMessageException, TableUnavailableException{
		request.setCreateSnapshot(null);
		// call under test
		TableUpdateTransactionResponse response = manager.updateTableWithTransaction(mockProgressCallback, user, request);
		assertNotNull(response);
		verify(mockTableViewManger, never()).createSnapshot(any(UserInfo.class), anyString(), any(SnapshotRequest.class));
	}
	
	@Test
	public void testUpdateTableWithTransactionWithSnapshotFalse() throws RecoverableMessageException, TableUnavailableException{
		request.setCreateSnapshot(false);
		// call under test
		TableUpdateTransactionResponse response = manager.updateTableWithTransaction(mockProgressCallback, user, request);
		assertNotNull(response);
		verify(mockTableViewManger, never()).createSnapshot(any(UserInfo.class), anyString(), any(SnapshotRequest.class));
	}
	
	@Test
	public void testUpdateTableWithTransactionNullCreateSnapshotButIncludesSnapshotOptions() throws RecoverableMessageException, TableUnavailableException{
		request.setCreateSnapshot(null);
		SnapshotRequest snapshotOptions = new SnapshotRequest();
		snapshotOptions.setSnapshotLabel("some label");
		request.setSnapshotOptions(snapshotOptions);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.updateTableWithTransaction(mockProgressCallback, user, request);
		});
		verify(mockTableViewManger, never()).createSnapshot(any(UserInfo.class), anyString(), any(SnapshotRequest.class));
	}
	
	@Test
	public void testUpdateTableWithTransactionNullUser() throws RecoverableMessageException, TableUnavailableException{
		user = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.updateTableWithTransaction(mockProgressCallback, user, request);
		});
	}
	
	@Test
	public void testUpdateTableWithTransactionNullProgress() throws RecoverableMessageException, TableUnavailableException{
		mockProgressCallback = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.updateTableWithTransaction(mockProgressCallback, user, request);
		});
	}
	
	@Test
	public void testUpdateTableWithTransactionNullRequest() throws RecoverableMessageException, TableUnavailableException{
		request = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.updateTableWithTransaction(mockProgressCallback, user, request);
		});
	}
	
	@Test
	public void testUpdateTableWithTransactionEmptyRequest() throws RecoverableMessageException, TableUnavailableException{
		request.setChanges(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.updateTableWithTransaction(mockProgressCallback, user, request);
		});
	}
	
	@Test
	public void testUpdateTableWithTransactionNullViewId() throws RecoverableMessageException, TableUnavailableException{
		request.setEntityId(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.updateTableWithTransaction(mockProgressCallback, user, request);
		});
	}
	
	@Test
	public void testApplyRowChangeParitalRowSet(){
		when(mockStackConfig.getTableMaxBytesPerRequest()).thenReturn(Integer.MAX_VALUE);
		when(mockTableManagerSupport.getTableSchema(idAndVersion)).thenReturn(schema);
		when(mockTableManagerSupport.getViewScopeType(any())).thenReturn(viewScopeType);
		
		appendAbleRowSetRequest.setToAppend(partialRowSet);
		// call under test
		TableUpdateResponse result = manager.applyRowChange(mockProgressCallback, user, appendAbleRowSetRequest);
		assertNotNull(result);
		verify(mockTableManagerSupport).getTableSchema(idAndVersion);
	}

	@Test
	public void testApplyRowChangeRowSet(){
		when(mockTableManagerSupport.getViewScopeType(any())).thenReturn(viewScopeType);
		appendAbleRowSetRequest.setToAppend(rowSet);
		// call under test
		TableUpdateResponse result = manager.applyRowChange(mockProgressCallback, user, appendAbleRowSetRequest);
		assertNotNull(result);
		verify(mockTableManagerSupport).getTableSchema(idAndVersion	);
	}
	
	@Test
	public void testApplyRowChangeUnknownType(){
		appendAbleRowSetRequest.setToAppend(Mockito.mock(AppendableRowSet.class));
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.applyRowChange(mockProgressCallback, user, appendAbleRowSetRequest);
		});
	}
	
	@Test
	public void testApplyRowChangeNullRequest(){
		appendAbleRowSetRequest = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.applyRowChange(mockProgressCallback, user, appendAbleRowSetRequest);
		});
	}
	
	@Test
	public void testApplyRowChangeNullToAppend(){
		appendAbleRowSetRequest.setToAppend(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.applyRowChange(mockProgressCallback, user, appendAbleRowSetRequest);
		});
	}
	
	@Test
	public void testApplyRowChangeNullEntityId(){
		appendAbleRowSetRequest.setEntityId(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.applyRowChange(mockProgressCallback, user, appendAbleRowSetRequest);
		});
	}
	
	@Test
	public void testApplyRowChangeWithMismatchingTableId(){
		rowSet.setTableId("some other table id");
		appendAbleRowSetRequest.setToAppend(rowSet);
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.applyRowChange(mockProgressCallback, user, appendAbleRowSetRequest);
		});
		assertEquals("The table id in the rowSet must match the entity id in the request (Expected: syn213, Found: some other table id)", ex.getMessage());
	}
	
	@Test
	public void testApplyRowSet(){
		when(mockTableManagerSupport.getViewScopeType(any())).thenReturn(viewScopeType);
		when(mockStackConfig.getTableMaxBytesPerRequest()).thenReturn(Integer.MAX_VALUE);
		// call under test
		TableUpdateResponse results = manager.applyRowSet(mockProgressCallback, user, schema, rowSet);
		assertNotNull(results);
	}
	
	@Test
	public void testApplyRowSetOverSize(){
		// setup a small size.
		when(mockStackConfig.getTableMaxBytesPerRequest()).thenReturn(1);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.applyRowSet(mockProgressCallback, user, schema, rowSet);
		});
	}
	
	@Test
	public void testApplyRowSetNullRowset(){
		rowSet = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.applyRowSet(mockProgressCallback, user, schema, rowSet);
		});
	}
	
	@Test
	public void testApplyRowSetNullTableId(){
		rowSet.setTableId(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.applyRowSet(mockProgressCallback, user, schema, rowSet);
		});
	}
	
	@Test
	public void testApplyPartialRowSet(){
		when(mockTableManagerSupport.getViewScopeType(any())).thenReturn(viewScopeType);
		when(mockStackConfig.getTableMaxBytesPerRequest()).thenReturn(Integer.MAX_VALUE);
	
		// call under test
		TableUpdateResponse results = manager.applyPartialRowSet(mockProgressCallback, user, schema, partialRowSet);
		assertNotNull(results);
	}
	
	@Test
	public void testApplyPartialRowSetOverSize(){
		// setup a small size.
		when(mockStackConfig.getTableMaxBytesPerRequest()).thenReturn(1);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.applyPartialRowSet(mockProgressCallback, user, schema, partialRowSet);
		});
	}
	
	@Test
	public void testApplyPartialRowSetNullRowset(){
		partialRowSet = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.applyPartialRowSet(mockProgressCallback, user, schema, partialRowSet);
		});
	}
	
	@Test
	public void testApplyPartialRowSetNullTableId(){
		partialRowSet.setTableId(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.applyPartialRowSet(mockProgressCallback, user, schema, partialRowSet);
		});
	}
	
	@Test
	public void testProcessRows(){
		when(mockTableManagerSupport.getViewScopeType(any())).thenReturn(viewScopeType);
		// call under test
		TableUpdateResponse response =  manager.processRows(user,"123", schema, sparseChangeSet.writeToDto().getRows().iterator(), "etag", mockProgressCallback);
		assertNotNull(response);
		assertTrue(response instanceof EntityUpdateResults);
		EntityUpdateResults result = (EntityUpdateResults)response;
		assertNotNull(result.getUpdateResults());
		assertEquals(2, result.getUpdateResults().size());
	}
	
	@Test
	public void testProcessRow() {
		ViewObjectType objectType = viewScopeType.getObjectType();
		
		// call under test
		EntityUpdateResult result = manager.processRow(user, schema, objectType, sparseRow, mockProgressCallback);
		assertNotNull(result);
		assertEquals("syn123",result.getEntityId());
		assertNull(result.getFailureCode());
		assertNull(result.getFailureMessage());
	}
	
	@Test
	public void testProcessRowNotFound(){
		ViewObjectType objectType = viewScopeType.getObjectType();
		
		doThrow(new NotFoundException("not")).when(mockTableViewManger).updateRowInView(any(), any(), any(), any());
		// call under test
		EntityUpdateResult result = manager.processRow(user, schema, objectType, sparseRow, mockProgressCallback);
		assertNotNull(result);
		assertEquals("syn123",result.getEntityId());
		assertEquals(EntityUpdateFailureCode.NOT_FOUND, result.getFailureCode());
		assertNull(result.getFailureMessage());
	}
	
	@Test
	public void testProcessRowConflict(){
		ViewObjectType objectType = viewScopeType.getObjectType();
		
		doThrow(new ConflictingUpdateException("conflict")).when(mockTableViewManger).updateRowInView(any(), any(), any(), any());
		
		// call under test
		EntityUpdateResult result = manager.processRow(user, schema, objectType, sparseRow, mockProgressCallback);
		assertNotNull(result);
		assertEquals("syn123",result.getEntityId());
		assertEquals(EntityUpdateFailureCode.CONCURRENT_UPDATE, result.getFailureCode());
		assertNull(result.getFailureMessage());
	}
	
	@Test
	public void testProcessRowUnauthorized(){
		ViewObjectType objectType = viewScopeType.getObjectType();
		
		doThrow(new UnauthorizedException("unathorized")).when(mockTableViewManger).updateRowInView(any(), any(), any(), any());
		// call under test
		EntityUpdateResult result = manager.processRow(user, schema, objectType, sparseRow, mockProgressCallback);
		assertNotNull(result);
		assertEquals("syn123",result.getEntityId());
		assertEquals(EntityUpdateFailureCode.UNAUTHORIZED, result.getFailureCode());
		assertNull(result.getFailureMessage());
	}
	
	@Test
	public void testProcessRowIllegalArgument(){
		ViewObjectType objectType = viewScopeType.getObjectType();
		
		doThrow(new IllegalArgumentException("illegal")).when(mockTableViewManger).updateRowInView(any(), any(), any(), any());
		// call under test
		EntityUpdateResult result = manager.processRow(user, schema, objectType, sparseRow, mockProgressCallback);
		assertNotNull(result);
		assertEquals("syn123",result.getEntityId());
		assertEquals(EntityUpdateFailureCode.ILLEGAL_ARGUMENT, result.getFailureCode());
		assertEquals("illegal", result.getFailureMessage());
	}
	
	@Test
	public void testProcessRowUnknown(){
		ViewObjectType objectType = viewScopeType.getObjectType();
		
		doThrow(new RuntimeException("unknown")).when(mockTableViewManger).updateRowInView(any(), any(), any(), any());
		// call under test
		EntityUpdateResult result = manager.processRow(user, schema, objectType, sparseRow, mockProgressCallback);
		assertNotNull(result);
		assertEquals("syn123",result.getEntityId());
		assertEquals(EntityUpdateFailureCode.UNKNOWN, result.getFailureCode());
		assertEquals("unknown", result.getFailureMessage());
	}
}
