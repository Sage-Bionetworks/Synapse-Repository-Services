package org.sagebionetworks.table.worker;



import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
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
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableSchemaChangeRequest;
import org.sagebionetworks.repo.model.table.TableSchemaChangeResponse;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
	AsynchJobStatusManager asynchJobStatusManager;
	
	UserInfo adminUserInfo;	
	ColumnModel intColumn;
	ColumnModel stringColumn;
	
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
	public void testTableSchemaUpdate() throws InterruptedException{
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
	public void testEntityViewSchemaUpdate() throws InterruptedException{
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
	public void testSchemaChange(String entityId) throws InterruptedException{
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
		TableUpdateTransactionResponse response = startAndWaitForJob(adminUserInfo, transaction, TableUpdateTransactionResponse.class);
		assertNotNull(response);
		assertNotNull(response.getResults());
		assertEquals(1, response.getResults().size());
		TableUpdateResponse updateResponse = response.getResults().get(0);
		assertTrue(updateResponse instanceof TableSchemaChangeResponse);
		TableSchemaChangeResponse changeResponse = (TableSchemaChangeResponse) updateResponse;
		assertNotNull(changeResponse.getSchema());
		assertEquals(1, changeResponse.getSchema().size());
		assertEquals(intColumn, changeResponse.getSchema().get(0));
		
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
		response = startAndWaitForJob(adminUserInfo, transaction, TableUpdateTransactionResponse.class);
		assertNotNull(response);
		assertNotNull(response.getResults());
		assertEquals(1, response.getResults().size());
	}


	/**
	 * Test schema change on list columns
	 * @throws InterruptedException
	 */
	@Test
	public void testSchemaChange_listColumnMaxSizeTooSmall() throws InterruptedException{
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

		// wait for the change to complete
		TableUpdateTransactionResponse response = startAndWaitForJob(adminUserInfo, transaction, TableUpdateTransactionResponse.class);
		assertNotNull(response);
		assertNotNull(response.getResults());
		assertEquals(1, response.getResults().size());
		TableUpdateResponse updateResponse = response.getResults().get(0);
		assertTrue(updateResponse instanceof TableSchemaChangeResponse);
		TableSchemaChangeResponse changeResponse = (TableSchemaChangeResponse) updateResponse;
		assertNotNull(changeResponse.getSchema());
		assertEquals(1, changeResponse.getSchema().size());
		assertEquals(stringListColumn, changeResponse.getSchema().get(0));


		// add row to column
		PartialRow rowOne = TableModelTestUtils.createPartialRow(null, stringListColumn.getId(), "[\"12345\"]");
		PartialRowSet rowSet = createRowSet(entityId, rowOne);
		transaction = createAddDataRequest(entityId, rowSet);
		response = startAndWaitForJob(adminUserInfo, transaction, TableUpdateTransactionResponse.class);


		QueryBundleRequest queryRequest = createQueryRequest("SELECT * FROM " + table.getId(), table.getId());
		List<Row> rows = startAndWaitForJob(adminUserInfo, queryRequest, QueryResultBundle.class)
				.getQueryResult().getQueryResults().getRows();
		assertEquals(1, rows.size());

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
		transaction = new TableUpdateTransactionRequest();
		transaction.setEntityId(entityId);
		transaction.setChanges(updates);

		// wait for the change to complete
		String errorMessage = startAndWaitForFailedJob(adminUserInfo, transaction);
		assertEquals("Data truncated for column 'aString_UNNEST' at row 1", errorMessage);
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
		TableUpdateTransactionResponse response = startAndWaitForJob(adminUserInfo, addColumnRequest, TableUpdateTransactionResponse.class);
		
		// Add some data to the table and create a new version
		PartialRow rowOne = TableModelTestUtils.createPartialRow(null, intColumn.getId(), "1");
		PartialRow rowTwo = TableModelTestUtils.createPartialRow(null, intColumn.getId(), "2");
		PartialRowSet rowSet = createRowSet(tableId, rowOne, rowTwo);
		TableUpdateTransactionRequest transaction = createAddDataRequest(tableId, rowSet);
		// start a new version
		transaction.setCreateSnapshot(true);
		// start the transaction
		response = startAndWaitForJob(adminUserInfo, transaction, TableUpdateTransactionResponse.class);
		assertNotNull(response);
		assertNotNull(response.getSnapshotVersionNumber());
		long firstVersion = response.getSnapshotVersionNumber();
		
		// add two more rows and create another version.
		PartialRow rowThree = TableModelTestUtils.createPartialRow(null, intColumn.getId(), "3");
		PartialRow rowFour = TableModelTestUtils.createPartialRow(null, intColumn.getId(), "4");
		rowSet = createRowSet(tableId, rowThree, rowFour);
		transaction = createAddDataRequest(tableId, rowSet);
		// start a new version
		transaction.setCreateSnapshot(true);
		// start the transaction
		response = startAndWaitForJob(adminUserInfo, transaction, TableUpdateTransactionResponse.class);
		assertNotNull(response);
		assertNotNull(response.getSnapshotVersionNumber());
		long secondVersion = response.getSnapshotVersionNumber();
		
		// Add two more rows without creating a version.
		PartialRow rowFive = TableModelTestUtils.createPartialRow(null, intColumn.getId(), "5");
		PartialRow rowSix = TableModelTestUtils.createPartialRow(null, intColumn.getId(), "6");
		rowSet = createRowSet(tableId, rowFive, rowSix);
		transaction = createAddDataRequest(tableId, rowSet);
		// start the transaction
		response = startAndWaitForJob(adminUserInfo, transaction, TableUpdateTransactionResponse.class);
		assertNotNull(response);
		assertNull(response.getSnapshotVersionNumber());
		// query first version
		String sql = "select * from " + tableId + "." + firstVersion;
		QueryBundleRequest queryRequest = createQueryRequest(sql, tableId);
		List<Row> firstVersionRows = startAndWaitForJob(adminUserInfo, queryRequest, QueryResultBundle.class)
				.getQueryResult().getQueryResults().getRows();
		assertEquals(2, firstVersionRows.size());
		// query second version
		sql = "select * from "+tableId+"."+secondVersion;
		queryRequest = createQueryRequest(sql, tableId);
		List<Row> secondVersionRows = startAndWaitForJob(adminUserInfo, queryRequest, QueryResultBundle.class)
				.getQueryResult().getQueryResults().getRows();
		assertEquals(4, secondVersionRows.size());
		// query latest without a version
		sql = "select * from "+tableId;
		queryRequest = createQueryRequest(sql, tableId);
		List<Row> latestVersion = startAndWaitForJob(adminUserInfo, queryRequest, QueryResultBundle.class)
				.getQueryResult().getQueryResults().getRows();
		assertEquals(6, latestVersion.size());
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
		TableUpdateTransactionResponse response = startAndWaitForJob(adminUserInfo, addColumnRequest, TableUpdateTransactionResponse.class);
		
		// Add some data to the table and create a new version
		PartialRow rowOne = TableModelTestUtils.createPartialRow(null, intColumn.getId(), "1");
		PartialRow rowTwo = TableModelTestUtils.createPartialRow(null, intColumn.getId(), "2");
		PartialRowSet rowSet = createRowSet(tableId, rowOne, rowTwo);
		TableUpdateTransactionRequest transaction = createAddDataRequest(tableId, rowSet);
		// do not create a snapshot.
		transaction.setCreateSnapshot(false);
		// start the transaction
		response = startAndWaitForJob(adminUserInfo, transaction, TableUpdateTransactionResponse.class);
		assertNotNull(response);
		assertNull(response.getSnapshotVersionNumber());
		long firstVersion = 1L;
		
		// query first version
		String sql = "select * from " + tableId + "." + firstVersion;
		QueryBundleRequest queryRequest = createQueryRequest(sql, tableId);
		String error = startAndWaitForFailedJob(adminUserInfo, queryRequest);
		assertTrue(error.contains("does not exist"));
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
	
	/**
	 * Start an asynchronous job and wait for the results.
	 * @param user
	 * @param body
	 * @return
	 * @throws InterruptedException 
	 */
	@SuppressWarnings("unchecked")
	public <T extends AsynchronousResponseBody> T  startAndWaitForJob(UserInfo user, AsynchronousRequestBody body, Class<? extends T> clazz) throws InterruptedException{
		long startTime = System.currentTimeMillis();
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(user, body);
		while(true){
			status = asynchJobStatusManager.getJobStatus(user, status.getJobId());
			switch(status.getJobState()){
			case FAILED:
				fail("Job failed: " + status.getErrorDetails());
			case PROCESSING:
				assertTrue((System.currentTimeMillis()-startTime) < MAX_WAIT_MS, "Timed out waiting for job to complete");
				System.out.println("Waiting for job: "+status.getProgressMessage());
				Thread.sleep(1000);
				break;
			case COMPLETE:
				return (T)status.getResponseBody();
			}
		}
	}
	
	/**
	 * Wait for the given job to fail.
	 * @param user
	 * @param body
	 * @return
	 * @throws InterruptedException
	 */
	public String  startAndWaitForFailedJob(UserInfo user, AsynchronousRequestBody body) throws InterruptedException{
		long startTime = System.currentTimeMillis();
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(user, body);
		while(true){
			status = asynchJobStatusManager.getJobStatus(user, status.getJobId());
			switch(status.getJobState()){
			case FAILED:
				return status.getErrorMessage();
			case PROCESSING:
				assertTrue((System.currentTimeMillis()-startTime) < MAX_WAIT_MS, "Timed out waiting for job to complete");
				System.out.println("Waiting for job to fail");
				Thread.sleep(1000);
				break;
			case COMPLETE:
				fail("Expected the Job to fail but it completed.");
			}
		}
	}
}
