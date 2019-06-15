package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
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
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
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
	
	@Before
	public void before(){
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
		
		when(mockTableViewManger.applySchemaChange(user, viewId, columnChanges, orderedColumnIds)).thenReturn(schema);
		when(mockStackConfig.getTableMaxBytesPerRequest()).thenReturn(Integer.MAX_VALUE);
		when(mockTableManagerSupport.getColumnModelsForTable(idAndVersion)).thenReturn(schema);
		
		// setup 
		doAnswer(new Answer<TableUpdateResponse>(){
			@Override
			public TableUpdateResponse answer(InvocationOnMock invocation)
					throws Throwable {
				UploadRowProcessor processor = (UploadRowProcessor) invocation.getArguments()[3];
				return processor.processRows(user, viewId, schema, sparseChangeSet.writeToDto().getRows().iterator(), null, mockProgressCallback);
			}}).when(mockTableUploadManager).uploadCSV(any(ProgressCallback.class), any(UserInfo.class), any(UploadToTableRequest.class), any(UploadRowProcessor.class));
	}
	
	@Test
	public void testApplySchemaChange(){
		// call under test
		TableSchemaChangeResponse response = manager.applySchemaChange(user, schemaChangeRequest);
		assertNotNull(response);
		assertEquals(schema, response.getSchema());
	}
	
	@Test
	public void testApplyChangePartialRowSet(){
		appendAbleRowSetRequest.setToAppend(partialRowSet);
		// call under test
		TableUpdateResponse result = manager.applyChange(mockProgressCallback, user, appendAbleRowSetRequest);
		assertNotNull(result);
		verify(mockTableViewManger, times(2)).updateEntityInView(eq(user), eq(schema), any(SparseRowDto.class));
	}
	
	@Test
	public void testApplyChangeRowSet(){
		appendAbleRowSetRequest.setToAppend(rowSet);
		// call under test
		TableUpdateResponse result = manager.applyChange(mockProgressCallback, user, appendAbleRowSetRequest);
		assertNotNull(result);
		verify(mockTableViewManger, times(2)).updateEntityInView(eq(user), eq(schema), any(SparseRowDto.class));
	}
	
	@Test
	public void testApplyChangeUploadTable(){
		// call under test
		TableUpdateResponse result = manager.applyChange(mockProgressCallback, user, uploadToTableRequest);
		assertNotNull(result);
		verify(mockTableViewManger, times(2)).updateEntityInView(eq(user), eq(schema), any(SparseRowDto.class));
	}
	
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyChangeUnknownType(){
		TableUpdateRequest unknown = Mockito.mock(TableUpdateRequest.class);
		// call under test
		manager.applyChange(mockProgressCallback, user, unknown);
	}
	
	@Test
	public void testUpdateTableWithTransaction() throws RecoverableMessageException, TableUnavailableException{
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
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateTableWithTransactionNullUser() throws RecoverableMessageException, TableUnavailableException{
		user = null;
		// call under test
		manager.updateTableWithTransaction(mockProgressCallback, user, request);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateTableWithTransactionNullProgress() throws RecoverableMessageException, TableUnavailableException{
		mockProgressCallback = null;
		// call under test
		manager.updateTableWithTransaction(mockProgressCallback, user, request);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateTableWithTransactionNullRequest() throws RecoverableMessageException, TableUnavailableException{
		request = null;
		// call under test
		manager.updateTableWithTransaction(mockProgressCallback, user, request);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateTableWithTransactionEmptyRequest() throws RecoverableMessageException, TableUnavailableException{
		request.setChanges(null);
		// call under test
		manager.updateTableWithTransaction(mockProgressCallback, user, request);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateTableWithTransactionNullViewId() throws RecoverableMessageException, TableUnavailableException{
		request.setEntityId(null);
		// call under test
		manager.updateTableWithTransaction(mockProgressCallback, user, request);
	}
	
	@Test
	public void testApplyRowChangeParitalRowSet(){
		appendAbleRowSetRequest.setToAppend(partialRowSet);
		// call under test
		TableUpdateResponse result = manager.applyRowChange(mockProgressCallback, user, appendAbleRowSetRequest);
		assertNotNull(result);
		verify(mockTableManagerSupport).getColumnModelsForTable(idAndVersion);
	}

	@Test
	public void testApplyRowChangeRowSet(){
		appendAbleRowSetRequest.setToAppend(rowSet);
		// call under test
		TableUpdateResponse result = manager.applyRowChange(mockProgressCallback, user, appendAbleRowSetRequest);
		assertNotNull(result);
		verify(mockTableManagerSupport).getColumnModelsForTable(idAndVersion	);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyRowChangeUnknownType(){
		appendAbleRowSetRequest.setToAppend(Mockito.mock(AppendableRowSet.class));
		// call under test
		manager.applyRowChange(mockProgressCallback, user, appendAbleRowSetRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyRowChangeNullRequest(){
		appendAbleRowSetRequest = null;
		// call under test
		manager.applyRowChange(mockProgressCallback, user, appendAbleRowSetRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyRowChangeNullToAppend(){
		appendAbleRowSetRequest.setToAppend(null);
		// call under test
		manager.applyRowChange(mockProgressCallback, user, appendAbleRowSetRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyRowChangeNullEntityId(){
		appendAbleRowSetRequest.setEntityId(null);
		// call under test
		manager.applyRowChange(mockProgressCallback, user, appendAbleRowSetRequest);
	}
	
	@Test
	public void testApplyRowSet(){
		// call under test
		TableUpdateResponse results = manager.applyRowSet(mockProgressCallback, user, schema, rowSet);
		assertNotNull(results);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyRowSetOverSize(){
		// setup a small size.
		when(mockStackConfig.getTableMaxBytesPerRequest()).thenReturn(1);
		// call under test
		manager.applyRowSet(mockProgressCallback, user, schema, rowSet);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyRowSetNullRowset(){
		rowSet = null;
		// call under test
		manager.applyRowSet(mockProgressCallback, user, schema, rowSet);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyRowSetNullTableId(){
		rowSet.setTableId(null);
		// call under test
		manager.applyRowSet(mockProgressCallback, user, schema, rowSet);
	}
	
	@Test
	public void testApplyPartialRowSet(){
		// call under test
		TableUpdateResponse results = manager.applyPartialRowSet(mockProgressCallback, user, schema, partialRowSet);
		assertNotNull(results);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyPartialRowSetOverSize(){
		// setup a small size.
		when(mockStackConfig.getTableMaxBytesPerRequest()).thenReturn(1);
		// call under test
		manager.applyPartialRowSet(mockProgressCallback, user, schema, partialRowSet);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyPartialRowSetNullRowset(){
		partialRowSet = null;
		// call under test
		manager.applyPartialRowSet(mockProgressCallback, user, schema, partialRowSet);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyPartialRowSetNullTableId(){
		partialRowSet.setTableId(null);
		// call under test
		manager.applyPartialRowSet(mockProgressCallback, user, schema, partialRowSet);
	}
	
	@Test
	public void testProcessRows(){
		// call under test
		TableUpdateResponse response =  manager.processRows(user,"tableId", schema, sparseChangeSet.writeToDto().getRows().iterator(), "etag", mockProgressCallback);
		assertNotNull(response);
		assertTrue(response instanceof EntityUpdateResults);
		EntityUpdateResults result = (EntityUpdateResults)response;
		assertNotNull(result.getUpdateResults());
		assertEquals(2, result.getUpdateResults().size());
	}
	
	@Test
	public void testProcessRow(){
		// call under test
		EntityUpdateResult result = manager.processRow(user, schema, sparseRow, mockProgressCallback);
		assertNotNull(result);
		assertEquals("syn123",result.getEntityId());
		assertNull(result.getFailureCode());
		assertNull(result.getFailureMessage());
	}
	
	@Test
	public void testProcessRowNotFound(){
		doThrow(new NotFoundException("not")).when(mockTableViewManger).updateEntityInView(user, schema, sparseRow);
		// call under test
		EntityUpdateResult result = manager.processRow(user, schema, sparseRow, mockProgressCallback);
		assertNotNull(result);
		assertEquals("syn123",result.getEntityId());
		assertEquals(EntityUpdateFailureCode.NOT_FOUND, result.getFailureCode());
		assertNull(result.getFailureMessage());
	}
	
	@Test
	public void testProcessRowConflict(){
		doThrow(new ConflictingUpdateException("conflict")).when(mockTableViewManger).updateEntityInView(user, schema, sparseRow);
		// call under test
		EntityUpdateResult result = manager.processRow(user, schema, sparseRow, mockProgressCallback);
		assertNotNull(result);
		assertEquals("syn123",result.getEntityId());
		assertEquals(EntityUpdateFailureCode.CONCURRENT_UPDATE, result.getFailureCode());
		assertNull(result.getFailureMessage());
	}
	
	@Test
	public void testProcessRowUnauthorized(){
		doThrow(new UnauthorizedException("unathorized")).when(mockTableViewManger).updateEntityInView(user, schema, sparseRow);
		// call under test
		EntityUpdateResult result = manager.processRow(user, schema, sparseRow, mockProgressCallback);
		assertNotNull(result);
		assertEquals("syn123",result.getEntityId());
		assertEquals(EntityUpdateFailureCode.UNAUTHORIZED, result.getFailureCode());
		assertNull(result.getFailureMessage());
	}
	
	@Test
	public void testProcessRowIllegalArgument(){
		doThrow(new IllegalArgumentException("illegal")).when(mockTableViewManger).updateEntityInView(user, schema, sparseRow);
		// call under test
		EntityUpdateResult result = manager.processRow(user, schema, sparseRow, mockProgressCallback);
		assertNotNull(result);
		assertEquals("syn123",result.getEntityId());
		assertEquals(EntityUpdateFailureCode.ILLEGAL_ARGUMENT, result.getFailureCode());
		assertEquals("illegal", result.getFailureMessage());
	}
	
	@Test
	public void testProcessRowUnknown(){
		doThrow(new RuntimeException("unknown")).when(mockTableViewManger).updateEntityInView(user, schema, sparseRow);
		// call under test
		EntityUpdateResult result = manager.processRow(user, schema, sparseRow, mockProgressCallback);
		assertNotNull(result);
		assertEquals("syn123",result.getEntityId());
		assertEquals(EntityUpdateFailureCode.UNKNOWN, result.getFailureCode());
		assertEquals("unknown", result.getFailureMessage());
	}
}
