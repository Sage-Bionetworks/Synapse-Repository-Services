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
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.TableSchemaChangeRequest;
import org.sagebionetworks.repo.model.table.TableSchemaChangeResponse;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
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
	
	TableViewTransactionManager manager;
	
	UserInfo user;
	String viewId;
	TableSchemaChangeRequest schemaChangeRequest;
	List<ColumnChange> columnChanges;
	List<ColumnModel> schema;
	TableUpdateTransactionRequest request;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		manager = new TableViewTransactionManager();
		ReflectionTestUtils.setField(manager, "tableViewManger", mockTableViewManger);
		ReflectionTestUtils.setField(manager, "tableManagerSupport", mockTableManagerSupport);
		
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
		
		when(mockTableViewManger.applySchemaChange(user, viewId, columnChanges)).thenReturn(schema);
	}
	
	@Test
	public void testApplySchemaChange(){
		// call under test
		TableSchemaChangeResponse response = manager.applySchemaChange(user, schemaChangeRequest);
		assertNotNull(response);
		assertEquals(schema, response.getSchema());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyChangeUnknownType(){
		TableUpdateRequest unknown = Mockito.mock(TableUpdateRequest.class);
		// call under test
		manager.applyChange(user, unknown);
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
