package org.sagebionetworks.table.worker;



import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.AppendableRowSet;
import org.sagebionetworks.repo.model.table.AppendableRowSetRequest;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableSchemaChangeRequest;
import org.sagebionetworks.repo.model.table.TableSchemaChangeResponse;
import org.sagebionetworks.repo.model.table.TableSearchChangeRequest;
import org.sagebionetworks.repo.model.table.TableSearchChangeResponse;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableTransactionWorkerIntegrationTest {
	
	public static final int MAX_WAIT_MS = 1000 * 60;
	
	@Autowired
	StackConfiguration config;
	@Autowired
	EntityManager entityManager;
	@Autowired
	TableEntityManager tableEntityManager;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	UserManager userManager;
	@Autowired
	ColumnModelManager columnManager;
	@Autowired
	AsynchronousJobWorkerHelper asyncHelper;
	
	UserInfo adminUserInfo;	
	ColumnModel intColumn;
	ColumnModel stringColumn;
	ColumnModel booleanColumn;
	
	List<String> toDelete;
	

	@BeforeEach
	public void before(){		
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		// integer column
		intColumn = new ColumnModel();
		intColumn.setName("anInteger");
		intColumn.setColumnType(ColumnType.INTEGER);
		intColumn = columnManager.createColumnModel(adminUserInfo, intColumn);
		// string column
		stringColumn = new ColumnModel();
		stringColumn.setName("aString");
		stringColumn.setColumnType(ColumnType.STRING);
		stringColumn.setMaximumSize(100L);
		stringColumn = columnManager.createColumnModel(adminUserInfo, stringColumn);
		// boolean column
		booleanColumn = new ColumnModel();
		booleanColumn.setName("aBoolean");
		booleanColumn.setColumnType(ColumnType.BOOLEAN);
		booleanColumn = columnManager.createColumnModel(adminUserInfo, booleanColumn);
		
		toDelete = new LinkedList<>();
	}
	
	@AfterEach
	public void after(){
		if(toDelete != null){
			for(String id: toDelete){
				try {
					entityManager.deleteEntity(adminUserInfo, id);
				} catch (Exception ignored) {}
			}
		}
	}
	
	@Test
	public void testTableSchemaUpdate() throws Exception {
		// test schema change on a TableEntity
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		String tableId = entityManager.createEntity(adminUserInfo, table, null);
		table = entityManager.getEntity(adminUserInfo, tableId, TableEntity.class);
		toDelete.add(tableId);
		// Update the schema of the table
		testSchemaChange(tableId);
	}
	
	@Test
	public void testEntityViewSchemaUpdate() throws Exception {
		// test schema change on an EntityView
		EntityView view = new EntityView();
		view.setName(UUID.randomUUID().toString());
		String viewId = entityManager.createEntity(adminUserInfo, view, null);
		view = entityManager.getEntity(adminUserInfo, viewId, EntityView.class);
		toDelete.add(viewId);
		testSchemaChange(viewId);
	}
	
	/**
	 * Test schema change on the given entity Id.
	 * @param entityId
	 * @throws InterruptedException
	 */
	public void testSchemaChange(String entityId) throws Exception {
		ColumnChange add = new ColumnChange();
		add.setOldColumnId(null);
		add.setNewColumnId(intColumn.getId());
		List<ColumnChange> changes = Lists.newArrayList(add);
		TableSchemaChangeRequest request = new TableSchemaChangeRequest();
		request.setEntityId(entityId);
		request.setChanges(changes);
		List<String> orderedColumnIds = Arrays.asList(intColumn.getId());
		request.setOrderedColumnIds(orderedColumnIds);
		
		List<TableUpdateRequest> updates = new LinkedList<TableUpdateRequest>();
		updates.add(request);
		TableUpdateTransactionRequest transaction = new TableUpdateTransactionRequest();
		transaction.setEntityId(entityId);
		transaction.setChanges(updates);
	
		// wait for the change to complete
		startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {			
			assertNotNull(response);
			assertNotNull(response.getResults());
			assertEquals(1, response.getResults().size());
			TableUpdateResponse updateResponse = response.getResults().get(0);
			assertTrue(updateResponse instanceof TableSchemaChangeResponse);
			TableSchemaChangeResponse changeResponse = (TableSchemaChangeResponse) updateResponse;
			assertNotNull(changeResponse.getSchema());
			assertEquals(1, changeResponse.getSchema().size());
			assertEquals(intColumn, changeResponse.getSchema().get(0));
		});
		
		// remove the columns (see PLFM-4188)
		ColumnChange remove = new ColumnChange();
		remove.setOldColumnId(intColumn.getId());
		remove.setNewColumnId(null);
		changes = Lists.newArrayList(remove);
		request = new TableSchemaChangeRequest();
		request.setEntityId(entityId);
		request.setChanges(changes);
		request.setOrderedColumnIds(new LinkedList<String>());
		
		updates = new LinkedList<TableUpdateRequest>();
		updates.add(request);
		transaction = new TableUpdateTransactionRequest();
		transaction.setEntityId(entityId);
		transaction.setChanges(updates);
	
		// wait for the change to complete
		startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {
			assertNotNull(response);
			assertNotNull(response.getResults());
			assertEquals(1, response.getResults().size());
		});
	}


	/**
	 * Test schema change on list columns
	 * @throws InterruptedException
	 * @throws AsynchJobFailedException 
	 * @throws AssertionError 
	 */
	@Test
	public void testSchemaChangelWithLstColumnMaxSizeTooSmall() throws Exception {
		// Reproduces PLFM-6190

		// string List column
		ColumnModel stringListColumn = new ColumnModel();
		stringListColumn.setName("aString");
		stringListColumn.setColumnType(ColumnType.STRING_LIST);
		stringListColumn.setMaximumSize(5L);
		stringListColumn = columnManager.createColumnModel(stringListColumn);

		// test schema change on a TableEntity
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		String entityId = entityManager.createEntity(adminUserInfo, table, null);
		table = entityManager.getEntity(adminUserInfo, entityId, TableEntity.class);
		toDelete.add(entityId);

		ColumnChange add = new ColumnChange();
		add.setOldColumnId(null);
		add.setNewColumnId(stringListColumn.getId());
		List<ColumnChange> changes = Lists.newArrayList(add);
		TableSchemaChangeRequest request = new TableSchemaChangeRequest();
		request.setEntityId(entityId);
		request.setChanges(changes);
		List<String> orderedColumnIds = Arrays.asList(stringListColumn.getId());
		request.setOrderedColumnIds(orderedColumnIds);

		List<TableUpdateRequest> updates = new LinkedList<TableUpdateRequest>();
		updates.add(request);
		TableUpdateTransactionRequest transaction = new TableUpdateTransactionRequest();
		transaction.setEntityId(entityId);
		transaction.setChanges(updates);
		
		final ColumnModel expectedModel = stringListColumn;

		// wait for the change to complete
		startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {			
			assertNotNull(response);
			assertNotNull(response.getResults());
			assertEquals(1, response.getResults().size());
			TableUpdateResponse updateResponse = response.getResults().get(0);
			assertTrue(updateResponse instanceof TableSchemaChangeResponse);
			TableSchemaChangeResponse changeResponse = (TableSchemaChangeResponse) updateResponse;
			assertNotNull(changeResponse.getSchema());
			assertEquals(1, changeResponse.getSchema().size());
			assertEquals(expectedModel, changeResponse.getSchema().get(0));
		});


		// add row to column
		PartialRow rowOne = TableModelTestUtils.createPartialRow(null, stringListColumn.getId(), "[\"12345\"]");
		PartialRowSet rowSet = createRowSet(entityId, rowOne);
		transaction = createAddDataRequest(entityId, rowSet);
		
		startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {
			assertNotNull(response);
		});


		QueryBundleRequest queryRequest = createQueryRequest("SELECT * FROM " + table.getId(), table.getId());
		
		startAndWaitForJob(adminUserInfo, queryRequest, (QueryResultBundle response) -> {
			assertEquals(1, response.getQueryResult().getQueryResults().getRows().size());
		});

		// smaller string list column
		ColumnModel smallerStringListColumn = new ColumnModel();
		smallerStringListColumn.setName("aString");
		smallerStringListColumn.setColumnType(ColumnType.STRING_LIST);
		smallerStringListColumn.setMaximumSize(2L);
		smallerStringListColumn = columnManager.createColumnModel(smallerStringListColumn);

		// Update the column to a size much smaller than the values inside the column
		ColumnChange updateSmallerColumn = new ColumnChange();
		updateSmallerColumn.setOldColumnId(stringListColumn.getId());
		updateSmallerColumn.setNewColumnId(smallerStringListColumn.getId());
		changes = Lists.newArrayList(updateSmallerColumn);
		request = new TableSchemaChangeRequest();
		request.setEntityId(entityId);
		request.setChanges(changes);
		request.setOrderedColumnIds(Arrays.asList(smallerStringListColumn.getId()));

		updates = new LinkedList<TableUpdateRequest>();
		updates.add(request);
		
		TableUpdateTransactionRequest failingTransaction = new TableUpdateTransactionRequest();
		failingTransaction.setEntityId(entityId);
		failingTransaction.setChanges(updates);

		// wait for the change to complete
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			asyncHelper.assertJobResponse(adminUserInfo, failingTransaction, (R) -> {
				fail("This should eventually fail");
			}, MAX_WAIT_MS);			
		});
		
		assertEquals("Data truncated for column 'aString_UNNEST' at row 1", ex.getMessage());
	}
	
	@Test
	public void testSchemaChangeWithStringToStringListColumnType() throws Exception {
		// PLFM-6247
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		String tableId = entityManager.createEntity(adminUserInfo, table, null);
		table = entityManager.getEntity(adminUserInfo, tableId, TableEntity.class);
		toDelete.add(tableId);
		
		// add string column
		TableUpdateTransactionRequest transaction = createAddColumnRequest(stringColumn, tableId);
		// wait for the change to complete
		startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {			
			assertNotNull(response);
		});
		
		// add data
		PartialRow row = TableModelTestUtils.createPartialRow(null, stringColumn.getId(), "foo");
		PartialRowSet rowSet = createRowSet(tableId, row);
		transaction = createAddDataRequest(tableId, rowSet);
		startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {			
			assertNotNull(response);
		});
		
		waitForConsistentQuery(table, adminUserInfo);
		
		// new column model for list
		Long maxListLength = 10L;
		String name = "aStringList";
		Long maxStringLength = 100L;
		ColumnModel stringListColumn = createListColumnModel(ColumnType.STRING_LIST, maxListLength, maxStringLength, name);
		// create request
		transaction = createColumnUpdateRequest(stringColumn, stringListColumn, tableId);
		
		// call under test
		startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {
			assertNotNull(response);
		});
		
		// add another row
		row = TableModelTestUtils.createPartialRow(null, stringListColumn.getId(), "[\"bar\", \"baz\", \"barFoo\"]");
		rowSet = createRowSet(tableId, row);
		transaction = createAddDataRequest(tableId, rowSet);
		startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {
			assertNotNull(response);
		});
		
		// test multi-value tables are built
		QueryBundleRequest queryRequest = createQueryRequest("SELECT UNNEST(" + name + ") FROM " + table.getId(), table.getId());
		startAndWaitForJob(adminUserInfo, queryRequest, (QueryResultBundle response) -> {
			assertEquals(4, response.getQueryResult().getQueryResults().getRows().size());
		});
	}
	
	@Test
	public void testSchemaChangeWithBooleanToBooleanListColumnType() throws Exception {
		// PLFM-6247
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		String tableId = entityManager.createEntity(adminUserInfo, table, null);
		table = entityManager.getEntity(adminUserInfo, tableId, TableEntity.class);
		toDelete.add(tableId);
		
		// add boolean column
		TableUpdateTransactionRequest transaction = createAddColumnRequest(booleanColumn, tableId);
		// wait for the change to complete
		startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {			
			assertNotNull(response);
		});
		
		// add data
		PartialRow row = TableModelTestUtils.createPartialRow(null, booleanColumn.getId(), "true");
		PartialRowSet rowSet = createRowSet(tableId, row);
		transaction = createAddDataRequest(tableId, rowSet);
		startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {			
			assertNotNull(response);
		});
		
		waitForConsistentQuery(table, adminUserInfo);
		
		// new column model for list
		Long maxListLength = 10L;
		String name = "aBooleanList";
		ColumnModel booleanListColumn = createListColumnModel(ColumnType.BOOLEAN_LIST, maxListLength, null, name);
		// create request
		transaction = createColumnUpdateRequest(booleanColumn, booleanListColumn, tableId);
		
		// call under test
		startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {
			assertNotNull(response);
		});
		
		// add another row
		row = TableModelTestUtils.createPartialRow(null, booleanListColumn.getId(), "[true, false, true]");
		rowSet = createRowSet(tableId, row);
		transaction = createAddDataRequest(tableId, rowSet);
		startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {
			assertNotNull(response);
		});
		
		// test multi-value tables are built
		QueryBundleRequest queryRequest = createQueryRequest("SELECT UNNEST(" + name + ") FROM " + table.getId(), table.getId());
		startAndWaitForJob(adminUserInfo, queryRequest, (QueryResultBundle response) -> {
			assertEquals(4, response.getQueryResult().getQueryResults().getRows().size());
		});
	}
	
	@Test
	public void testSchemaChangeWithIntegerToIntegerListColumnType() throws Exception {
		// PLFM-6247
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		String tableId = entityManager.createEntity(adminUserInfo, table, null);
		table = entityManager.getEntity(adminUserInfo, tableId, TableEntity.class);
		toDelete.add(tableId);
		
		// add int column
		TableUpdateTransactionRequest transaction = createAddColumnRequest(intColumn, tableId);
		// wait for the change to complete
		startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {			
			assertNotNull(response);
		});
		
		// add data
		PartialRow row = TableModelTestUtils.createPartialRow(null, intColumn.getId(), "12");
		PartialRowSet rowSet = createRowSet(tableId, row);
		transaction = createAddDataRequest(tableId, rowSet);
		startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {			
			assertNotNull(response);
		});
		
		waitForConsistentQuery(table, adminUserInfo);
		
		// new column model for list
		Long maxListLength = 10L;
		String name = "anIntList";
		ColumnModel intListColumn = createListColumnModel(ColumnType.INTEGER_LIST, maxListLength, null, name);
		// create request
		transaction = createColumnUpdateRequest(intColumn, intListColumn, tableId);
		
		// call under test
		startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {
			assertNotNull(response);
		});
		
		// add another row
		row = TableModelTestUtils.createPartialRow(null, intListColumn.getId(), "[1, 2, 100]");
		rowSet = createRowSet(tableId, row);
		transaction = createAddDataRequest(tableId, rowSet);
		startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {
			assertNotNull(response);
		});
		
		// test multi-value tables are built
		QueryBundleRequest queryRequest = createQueryRequest("SELECT UNNEST(" + name + ") FROM " + table.getId(), table.getId());
		startAndWaitForJob(adminUserInfo, queryRequest, (QueryResultBundle response) -> {
			assertEquals(4, response.getQueryResult().getQueryResults().getRows().size());
		});
	}
	
	@Test
	public void testTableVersion() throws Exception {
		// create a table with more than one version
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.newArrayList(intColumn.getId()));
		String tableId = entityManager.createEntity(adminUserInfo, table, null);
		table = entityManager.getEntity(adminUserInfo, tableId, TableEntity.class);
		toDelete.add(tableId);
		// add add a column to the table.
		TableUpdateTransactionRequest addColumnRequest = createAddColumnRequest(intColumn, tableId);
		
		startAndWaitForJob(adminUserInfo, addColumnRequest, (TableUpdateTransactionResponse response) -> {
			assertNotNull(response);
		});
		
		// Add some data to the table and create a new version
		PartialRow rowOne = TableModelTestUtils.createPartialRow(null, intColumn.getId(), "1");
		PartialRow rowTwo = TableModelTestUtils.createPartialRow(null, intColumn.getId(), "2");
		PartialRowSet rowSet = createRowSet(tableId, rowOne, rowTwo);
		TableUpdateTransactionRequest transaction = createAddDataRequest(tableId, rowSet);
		// start a new version
		transaction.setCreateSnapshot(true);
		
		// start the transaction
		long firstVersion = startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {			
			assertNotNull(response);
			assertNotNull(response.getSnapshotVersionNumber());
		}).getSnapshotVersionNumber();
		
		// add two more rows and create another version.
		PartialRow rowThree = TableModelTestUtils.createPartialRow(null, intColumn.getId(), "3");
		PartialRow rowFour = TableModelTestUtils.createPartialRow(null, intColumn.getId(), "4");
		rowSet = createRowSet(tableId, rowThree, rowFour);
		transaction = createAddDataRequest(tableId, rowSet);
		// start a new version
		transaction.setCreateSnapshot(true);
		// start the transaction
		long secondVersion = startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {			
			assertNotNull(response);
			assertNotNull(response.getSnapshotVersionNumber());
		}).getSnapshotVersionNumber();
		
		// Add two more rows without creating a version.
		PartialRow rowFive = TableModelTestUtils.createPartialRow(null, intColumn.getId(), "5");
		PartialRow rowSix = TableModelTestUtils.createPartialRow(null, intColumn.getId(), "6");
		rowSet = createRowSet(tableId, rowFive, rowSix);
		transaction = createAddDataRequest(tableId, rowSet);
		// start the transaction
		startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {			
			assertNotNull(response);
			assertNull(response.getSnapshotVersionNumber());
		});
		// query first version
		String sql = "select * from " + tableId + "." + firstVersion;
		QueryBundleRequest queryRequest = createQueryRequest(sql, tableId);
		
		startAndWaitForJob(adminUserInfo, queryRequest, (QueryResultBundle response) -> {			
			List<Row> firstVersionRows = response.getQueryResult().getQueryResults().getRows();
			assertEquals(2, firstVersionRows.size());
		});
		
		// query second version
		sql = "select * from "+tableId+"."+secondVersion;
		queryRequest = createQueryRequest(sql, tableId);
		
		startAndWaitForJob(adminUserInfo, queryRequest, (QueryResultBundle response) -> {
			List<Row> secondVersionRows = response.getQueryResult().getQueryResults().getRows();
			assertEquals(4, secondVersionRows.size());
		});
		
		// query latest without a version
		sql = "select * from "+tableId;
		
		queryRequest = createQueryRequest(sql, tableId);
		
		startAndWaitForJob(adminUserInfo, queryRequest, (QueryResultBundle response) -> {			
			List<Row> latestVersion = response.getQueryResult().getQueryResults().getRows();
			assertEquals(6, latestVersion.size());
		});
	}
	
	/**
	 * Test for PLFM-5771.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTableVersionCurrentVersion() throws Exception {
		// create a table with more than one version
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.newArrayList(intColumn.getId()));
		String tableId = entityManager.createEntity(adminUserInfo, table, null);
		table = entityManager.getEntity(adminUserInfo, tableId, TableEntity.class);
		toDelete.add(tableId);
		// add add a column to the table.
		TableUpdateTransactionRequest addColumnRequest = createAddColumnRequest(intColumn, tableId);
		
		startAndWaitForJob(adminUserInfo, addColumnRequest, (TableUpdateTransactionResponse response) -> {
			assertNotNull(response);
		});
		
		// Add some data to the table and create a new version
		PartialRow rowOne = TableModelTestUtils.createPartialRow(null, intColumn.getId(), "1");
		PartialRow rowTwo = TableModelTestUtils.createPartialRow(null, intColumn.getId(), "2");
		PartialRowSet rowSet = createRowSet(tableId, rowOne, rowTwo);
		TableUpdateTransactionRequest transaction = createAddDataRequest(tableId, rowSet);
		// do not create a snapshot.
		transaction.setCreateSnapshot(false);
		
		// start the transaction
		startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {			
			assertNotNull(response);
			assertNull(response.getSnapshotVersionNumber());
		});
		
		long firstVersion = 1L;
		
		// query first version
		String sql = "select * from " + tableId + "." + firstVersion;
		QueryBundleRequest queryRequest = createQueryRequest(sql, tableId);
		
		AsynchJobFailedException ex = assertThrows(AsynchJobFailedException.class, () -> {
			
			asyncHelper.assertJobResponse(adminUserInfo, queryRequest, (R) -> {
				fail("This should eventually fail");
			}, MAX_WAIT_MS);
		
		});

		assertTrue(ex.getMessage().contains("does not exist"));
	}
	
	@Test
	public void testTableSearchChangeRequest() throws Exception {
	
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		String entityId = entityManager.createEntity(adminUserInfo, table, null);
		table = entityManager.getEntity(adminUserInfo, entityId, TableEntity.class);
		
		String currentEtag = table.getEtag();
		
		assertNull(table.getIsSearchEnabled());
		
		toDelete.add(entityId);

		TableUpdateTransactionRequest transaction = new TableUpdateTransactionRequest();
		transaction.setEntityId(entityId);
		transaction.setChanges(Arrays.asList(
			new TableSearchChangeRequest().setSearchEnabled(true)
		));
		
		
		TableUpdateTransactionResponse expectedResponse = new TableUpdateTransactionResponse()
				.setResults(Arrays.asList(new TableSearchChangeResponse().setSearchEnabled(true)));
		
		// wait for the change to complete
		startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {			
			assertEquals(expectedResponse, response);
		});
		
		table = entityManager.getEntity(adminUserInfo, entityId, TableEntity.class);
		
		assertNotEquals(currentEtag, table.getEtag());
		assertTrue(table.getIsSearchEnabled());

	}
	
	/**
	 * Helper to create a query request.
	 * @param sql
	 * @param tableId
	 * @return
	 */
	public static QueryBundleRequest createQueryRequest(String sql, String tableId) {
		Query query = new Query();
		query.setSql(sql);
		QueryBundleRequest request = new QueryBundleRequest();
		request.setQuery(query);
		request.setEntityId(tableId);
		return request;
	}
	
	/**
	 * Helper to create a create column transaction request.
	 * 
	 * @param column
	 * @param entityId
	 * @return
	 */
	public static TableUpdateTransactionRequest createAddColumnRequest(ColumnModel column, String entityId) {
		ColumnChange add = new ColumnChange();
		add.setOldColumnId(null);
		add.setNewColumnId(column.getId());
		List<ColumnChange> changes = Lists.newArrayList(add);
		TableSchemaChangeRequest request = new TableSchemaChangeRequest();
		request.setEntityId(entityId);
		request.setChanges(changes);
		List<String> orderedColumnIds = Arrays.asList(column.getId());
		request.setOrderedColumnIds(orderedColumnIds);
		
		List<TableUpdateRequest> updates = new LinkedList<TableUpdateRequest>();
		updates.add(request);
		TableUpdateTransactionRequest transaction = new TableUpdateTransactionRequest();
		transaction.setEntityId(entityId);
		transaction.setChanges(updates);
		return transaction;
	}
	
	/**
	 * Helper to create a column type change request
	 * 
	 * @param oldColumn
	 * @param newColumn
	 * @param entityId
	 */
	public static TableUpdateTransactionRequest createColumnUpdateRequest(ColumnModel oldColumn, ColumnModel newColumn, String entityId) {
		ColumnChange updateToList = new ColumnChange();
		updateToList.setOldColumnId(oldColumn.getId());
		updateToList.setNewColumnId(newColumn.getId());
		List<ColumnChange> changes = Lists.newArrayList(updateToList);
		TableSchemaChangeRequest request = new TableSchemaChangeRequest();
		request.setEntityId(entityId);
		request.setChanges(changes);
		List<String> orderedColumnIds = Arrays.asList(newColumn.getId());
		request.setOrderedColumnIds(orderedColumnIds);
		
		List<TableUpdateRequest> updates = new LinkedList<TableUpdateRequest>();
		updates.add(request);
		TableUpdateTransactionRequest transaction = new TableUpdateTransactionRequest();
		transaction.setEntityId(entityId);
		transaction.setChanges(updates);
		return transaction;
	}
	
	/**
	 * Helper to create a new version request.
	 * @return
	 */
	public static SnapshotRequest createVersionRequest() {
		return new SnapshotRequest();
	}
	
	/**
	 * Helper to create  PartialRowSet from the values.
	 * @param tableId
	 * @param values
	 * @return
	 */
	public static PartialRowSet createRowSet(String tableId, PartialRow...partialRows) {
		List<PartialRow> rows = new ArrayList<PartialRow>(partialRows.length);
		rows.addAll(Arrays.asList(partialRows));
		PartialRowSet rowSet = new PartialRowSet();
		rowSet.setRows(rows);
		rowSet.setTableId(tableId);
		return rowSet;
	}
	
	/**
	 * Helper to create a transaction request to add data to a table.
	 * 
	 * @param tableId
	 * @param column
	 * @param count
	 * @return
	 */
	public static TableUpdateTransactionRequest createAddDataRequest(String tableId, AppendableRowSet rowSet) {
		AppendableRowSetRequest append = new AppendableRowSetRequest();
		append.setEntityId(tableId);
		append.setToAppend(rowSet);
		TableUpdateTransactionRequest request = new TableUpdateTransactionRequest();
		request.setEntityId(tableId);
		request.setChanges(Lists.newArrayList(append));
		return request;
	}
	
	private <T extends AsynchronousResponseBody> T  startAndWaitForJob(UserInfo user, AsynchronousRequestBody body, Consumer<T> consumer) throws AssertionError, AsynchJobFailedException{
		return asyncHelper.assertJobResponse(user, body, consumer, MAX_WAIT_MS).getResponse();
	}
	
	private void waitForConsistentQuery(TableEntity table, UserInfo adminUserInfo) throws Exception {
		QueryBundleRequest queryRequest = createQueryRequest("SELECT * FROM " + table.getId(), table.getId());
		startAndWaitForJob(adminUserInfo, queryRequest, (QueryResultBundle response) -> {
			assertEquals(1, response.getQueryResult().getQueryResults().getRows().size());
		});
	}
	
	/**
	 * Helper to create a list type column model
	 * @param columnType
	 */
	public ColumnModel createListColumnModel(ColumnType columnType, Long maxListLength, Long maxStringLength, String name) {
		ColumnModel cm = new ColumnModel();
		if (columnType.equals(ColumnType.STRING_LIST)) {
			cm.setColumnType(ColumnType.STRING_LIST);
			cm.setMaximumSize(maxStringLength);
		} else {
			cm.setColumnType(columnType);
		}
		cm.setName(name);
		cm.setMaximumListLength(maxListLength);
		return columnManager.createColumnModel(adminUserInfo, cm);
	}
}
