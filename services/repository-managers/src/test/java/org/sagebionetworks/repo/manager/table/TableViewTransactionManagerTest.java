package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.AppendableRowSetRequest;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.RowReferenceSetResults;
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
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;

public class TableViewTransactionManagerTest {
	
	@Mock
	TableManagerSupport mockTableManagerSupport;
	@Mock
	TableViewManager mockTableViewManger;
	@Mock
	ProgressCallback<Void> mockProgressCallback;
	@Mock
	TableUploadManager mockTableUploadManager;
	@Mock
	StackConfiguration mockStackConfig;
	
	TableViewTransactionManager manager;
	
	UserInfo user;
	String viewId;
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
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		manager = new TableViewTransactionManager();
		ReflectionTestUtils.setField(manager, "tableViewManger", mockTableViewManger);
		ReflectionTestUtils.setField(manager, "tableManagerSupport", mockTableManagerSupport);
		ReflectionTestUtils.setField(manager, "tableUploadManager", mockTableUploadManager);
		ReflectionTestUtils.setField(manager, "stackConfig", mockStackConfig);
		
		user = new UserInfo(false, 12L);
		viewId = "syn213";
		schema = TableModelTestUtils.createColumsWithNames("one", "two");
		
		ColumnChange change = new ColumnChange();
		change.setNewColumnId(schema.get(0).getId());
		
		ColumnChange change2 = new ColumnChange();
		change2.setNewColumnId(schema.get(1).getId());
		columnChanges = Lists.newArrayList(change, change2);
		
		schemaChangeRequest = new TableSchemaChangeRequest();
		schemaChangeRequest.setChanges(columnChanges);
		schemaChangeRequest.setEntityId(viewId);
		
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
		
		appendAbleRowSetRequest = new AppendableRowSetRequest();
		appendAbleRowSetRequest.setEntityId(viewId);
		
		uploadToTableRequest = new UploadToTableRequest();
		uploadToTableRequest.setTableId(viewId);
		
		uploadToTableResult = new UploadToTableResult();
		
		when(mockTableViewManger.applySchemaChange(user, viewId, columnChanges)).thenReturn(schema);
		when(mockStackConfig.getTableMaxBytesPerRequest()).thenReturn(Integer.MAX_VALUE);
		when(mockTableManagerSupport.getColumnModelsForTable(viewId)).thenReturn(schema);
		
		// setup 
		doAnswer(new Answer<UploadToTableResult>(){
			@Override
			public UploadToTableResult answer(InvocationOnMock invocation)
					throws Throwable {
				UploadRowProcessor processor = (UploadRowProcessor) invocation.getArguments()[3];
				processor.processRows(user, viewId, schema, sparseChangeSet.writeToDto().getRows().iterator(), null, mockProgressCallback);
				return uploadToTableResult;
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
		verify(mockTableManagerSupport).validateTableWriteAccess(user, viewId);
		verify(mockProgressCallback, times(1)).progressMade(null);
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

}
