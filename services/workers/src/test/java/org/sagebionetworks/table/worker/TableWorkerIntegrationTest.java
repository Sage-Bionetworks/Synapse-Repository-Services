package org.sagebionetworks.table.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.sqs.MessageReceiver;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableWorkerIntegrationTest {

	/**
	 * 
	 */
	public static final int MAX_WAIT_MS = 1000*60;
	
	@Autowired
	StackConfiguration config;
	
	@Autowired
	EntityManager entityManager;
	@Autowired
	TableRowManager tableRowManager;
	@Autowired
	ColumnModelManager columnManager;
	@Autowired
	UserManager userManager;
	@Autowired
	MessageReceiver tableQueueMessageReveiver;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	
	private UserInfo adminUserInfo;
	RowReferenceSet referenceSet;
	List<ColumnModel> schema;
	private String tableId;
	
	@Before
	public void before() throws NotFoundException, DatastoreException, IOException, InterruptedException{
		// Only run this test if the table feature is enabled.
		Assume.assumeTrue(config.getTableEnabled());
		// Start with an empty queue.
		tableQueueMessageReveiver.emptyQueue();
		// Get the admin user
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		this.tableId = null;
	}
	
	@After
	public void after(){
		if(config.getTableEnabled()){
			// cleanup
			columnManager.truncateAllColumnData(adminUserInfo);
			
			if(tableId != null){
				try {
					entityManager.deleteEntity(adminUserInfo, tableId);
				} catch (Exception e) {	} 
				
				TableIndexDAO dao = tableConnectionFactory.getConnection(tableId);
				if(dao != null){
					try {
						dao.deleteTable(tableId);
					} catch (Exception e) {	}
				}
			}
		}
	}

	@Test
	public void testRoundTrip() throws NotFoundException, InterruptedException, DatastoreException, TableUnavilableException, IOException{
		// Create one column of each type
		List<ColumnModel> temp = TableModelTestUtils.createOneOfEachType();
		schema = new LinkedList<ColumnModel>();
		for(ColumnModel cm: temp){
			// Skip strings
			if(cm.getColumnType() == ColumnType.STRING) continue;
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
		List<String> headers = TableModelUtils.getHeaders(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(headers);
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, headers, tableId, true);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 2);
		// Add null rows
		rows.addAll(TableModelTestUtils.createNullRows(schema, 2));
		// Add empty rows
		rows.addAll(TableModelTestUtils.createEmptyRows(schema, 2));
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(headers);
		rowSet.setTableId(tableId);
		long start = System.currentTimeMillis();
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, schema, rowSet);
		System.out.println("Appended "+rowSet.getRows().size()+" rows in: "+(System.currentTimeMillis()-start)+" MS");
		// Wait for the table to become available
		TableStatus status = waitForTableToBeAvailable(tableId);
		// Validate that we can query the table
		boolean isConsistent = true;
		boolean countOnly = false;
		rowSet = tableRowManager.query(adminUserInfo, "select * from " + tableId + " limit 8", isConsistent, countOnly);
		assertNotNull(rowSet);
		assertEquals(tableId, rowSet.getTableId());
		assertNotNull(rowSet.getHeaders());
		assertEquals(schema.size(), rowSet.getHeaders().size());
		assertNotNull(rowSet.getRows());
		assertEquals(6, rowSet.getRows().size());
		assertNotNull(rowSet.getEtag());
		assertEquals("The etag for the last applied change set should be set for the status and the results",status.getLastTableChangeEtag(), rowSet.getEtag());
		assertEquals("The etag should also match the rereferenceSet.etag",referenceSet.getEtag(), rowSet.getEtag());
	}
	
	@Test
	public void testReplaceAndDeleteRoundTrip() throws NotFoundException, InterruptedException, DatastoreException, TableUnavilableException,
			IOException {
		// Create one column of each type
		schema = new LinkedList<ColumnModel>();
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.STRING);
		cm.setName("col1");
		cm = columnManager.createColumnModel(adminUserInfo, cm);
		schema.add(cm);

		List<String> headers = TableModelUtils.getHeaders(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(headers);
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, headers, tableId, true);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 4);
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(headers);
		rowSet.setTableId(tableId);
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, schema, rowSet);
		assertEquals(4, referenceSet.getRows().size());

		rowSet.setEtag(referenceSet.getEtag());
		for (int i = 0; i < 4; i++) {
			rows.get(i).setRowId(referenceSet.getRows().get(i).getRowId());
			rows.get(i).setVersionNumber(referenceSet.getRows().get(i).getVersionNumber());
		}

		List<Row> updateRows = Lists.newArrayList(rows);
		updateRows.remove(3);

		TableModelTestUtils.updateRow(schema, updateRows.get(0), 333);
		TableModelTestUtils.updateRow(schema, updateRows.get(1), 444);
		TableModelTestUtils.updateRow(schema, updateRows.get(2), 555);
		rowSet.setRows(updateRows);
		RowReferenceSet referenceSet2 = tableRowManager.appendRows(adminUserInfo, tableId, schema, rowSet);
		assertEquals(3, referenceSet2.getRows().size());

		RowSelection rowsToDelete = new RowSelection();
		rowsToDelete.setEtag(referenceSet2.getEtag());
		rowsToDelete.setRowIds(Lists.newArrayList(referenceSet2.getRows().get(1).getRowId(), referenceSet.getRows().get(3).getRowId()));

		referenceSet = tableRowManager.deleteRows(adminUserInfo, tableId, schema, rowsToDelete);
		assertEquals(2, referenceSet.getRows().size());

		// Wait for the table to become available
		waitForTableToBeAvailable(tableId);
		// Validate that we can query the table
		boolean isConsistent = true;
		boolean countOnly = false;
		rowSet = tableRowManager.query(adminUserInfo, "select * from " + tableId + " limit 100", isConsistent, countOnly);
		assertEquals("TableId: "+tableId, 2, rowSet.getRows().size());
		assertEquals("updatestring333", rowSet.getRows().get(0).getValues().get(0));
		assertEquals("updatestring555", rowSet.getRows().get(1).getValues().get(0));
	}

	@Ignore // This is a very slow test that pushes massive amounts of data so it is disabled.
	@Test
	public void testAppendRowsAtScale() throws NotFoundException, InterruptedException, DatastoreException, TableUnavilableException, IOException{
		// Create one column of each type
		List<ColumnModel> temp = TableModelTestUtils.createOneOfEachType();
		schema = new LinkedList<ColumnModel>();
		for(ColumnModel cm: temp){
			// Skip strings
			if(cm.getColumnType() == ColumnType.STRING) continue;
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
		List<String> headers = TableModelUtils.getHeaders(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(headers);
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, headers, tableId, true);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 500000);
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(headers);
		rowSet.setTableId(tableId);
		long start = System.currentTimeMillis();
		tableRowManager.appendRowsAsStream(adminUserInfo, tableId, schema, rowSet.getRows().iterator(), null, null);
		System.out.println("Appended "+rowSet.getRows().size()+" rows in: "+(System.currentTimeMillis()-start)+" MS");
		// Wait for the table to become available
		TableStatus status = waitForTableToBeAvailable(tableId);
		// Validate that we can query the table
		boolean isConsistent = true;
		boolean countOnly = false;
		rowSet = tableRowManager.query(adminUserInfo, "select * from "+tableId+" limit 2", isConsistent, countOnly);
		assertNotNull(rowSet);
		assertEquals(tableId, rowSet.getTableId());
		assertNotNull(rowSet.getHeaders());
		assertEquals(schema.size(), rowSet.getHeaders().size());
		assertNotNull(rowSet.getRows());
		assertEquals(2, rowSet.getRows().size());
		assertNotNull(rowSet.getEtag());
		assertEquals("The etag for the last applied change set should be set for the status and the results",status.getLastTableChangeEtag(), rowSet.getEtag());
		assertEquals("The etag should also match the rereferenceSet.etag",referenceSet.getEtag(), rowSet.getEtag());
	}

	@Test
	public void testColumnNameRange() throws NotFoundException, InterruptedException, DatastoreException, TableUnavilableException, IOException{
		// Create one column of each type
		String specialChars = "Specialchars~!@#$%^^&*()_+|}{:?></.,;'[]\'";
		List<ColumnModel> temp = TableModelTestUtils.createColumsWithNames("Has Space", "a", "A", specialChars);
		schema = new LinkedList<ColumnModel>();
		for(ColumnModel cm: temp){
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
		List<String> headers = TableModelUtils.getHeaders(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(headers);
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, headers, tableId, true);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 10);
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(headers);
		rowSet.setTableId(tableId);
		long start = System.currentTimeMillis();
		tableRowManager.appendRowsAsStream(adminUserInfo, tableId, schema, rowSet.getRows().iterator(), null, null);
		System.out.println("Appended "+rowSet.getRows().size()+" rows in: "+(System.currentTimeMillis()-start)+" MS");
		// Wait for the table to become available
		TableStatus status = waitForTableToBeAvailable(tableId);
		// Validate that we can query the table
		boolean isConsistent = true;
		boolean countOnly = false;
		rowSet = tableRowManager.query(adminUserInfo, "select A, a, \"Has Space\",\""+specialChars+"\" from "+tableId+" limit 2", isConsistent, countOnly);
		assertNotNull(rowSet);
		assertEquals(tableId, rowSet.getTableId());
		assertNotNull(rowSet.getHeaders());
		assertEquals(4, rowSet.getHeaders().size());
		assertEquals(headers.get(0), rowSet.getHeaders().get(2));
		assertEquals(headers.get(1), rowSet.getHeaders().get(1));
		assertEquals(headers.get(2), rowSet.getHeaders().get(0));
		assertEquals(headers.get(3), rowSet.getHeaders().get(3));
		assertNotNull(rowSet.getRows());
		assertEquals(2, rowSet.getRows().size());
		assertNotNull(rowSet.getEtag());
	}
	
	/**
	 * There were several issue related to creating tables with no columns an now rows.  This test validates that such tables are supported.
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 * @throws InvalidModelException 
	 * @throws Exception 
	 */
	@Test
	public void testNoColumnsNoRows() throws Exception {
		// Create a table with no columns.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(null);
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, null, tableId, true);
		// Wait for the table to be available
		TableStatus status = waitForTableToBeAvailable(tableId);
		assertNotNull(status);
		// We should be able to query
		String sql = "select * from "+tableId+" limit 1";
		boolean isConsistent = true;
		boolean countOnly = false;
		RowSet rowSet = tableRowManager.query(adminUserInfo, sql, isConsistent, countOnly);
		assertNotNull(rowSet);
		assertNull(rowSet.getEtag());
		assertEquals(tableId, rowSet.getTableId());
		assertTrue(rowSet.getHeaders() == null || rowSet.getHeaders().isEmpty());
		assertTrue(rowSet.getRows() == null || rowSet.getRows().isEmpty());
	}
	
	/**
	 * There were several issue related to creating tables with no columns an now rows.  This test validates that such tables are supported.
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 * @throws InvalidModelException 
	 * @throws Exception 
	 */
	@Test
	public void testNoRows() throws Exception {
		// Create one column of each type
		List<ColumnModel> temp = TableModelTestUtils.createOneOfEachType();
		schema = new LinkedList<ColumnModel>();
		for(ColumnModel cm: temp){
			// Skip strings
			if(cm.getColumnType() == ColumnType.STRING) continue;
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
		List<String> headers = TableModelUtils.getHeaders(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(headers);
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, headers, tableId, true);
		// Wait for the table to be available
		TableStatus status = waitForTableToBeAvailable(tableId);
		assertNotNull(status);
		// We should be able to query
		String sql = "select * from "+tableId+" limit 1";
		boolean isConsistent = true;
		boolean countOnly = false;
		RowSet rowSet = tableRowManager.query(adminUserInfo, sql, isConsistent, countOnly);
		assertNotNull(rowSet);
		assertNull(rowSet.getEtag());
		assertEquals(tableId, rowSet.getTableId());
		assertTrue("TableId: "+tableId, rowSet.getHeaders() == null || rowSet.getHeaders().isEmpty());
		assertTrue("TableId: "+tableId, rowSet.getRows() == null || rowSet.getRows().isEmpty());
	}
	

	/**
	 * Helper to wait for a table to become available.
	 * 
	 * @param tableId
	 * @return
	 * @throws NotFoundException
	 * @throws InterruptedException
	 */
	private TableStatus waitForTableToBeAvailable(String tableId) throws NotFoundException,
			InterruptedException {
		TableStatus status = tableRowManager.getTableStatus(tableId);
		assertNotNull(status);
		// wait for the status to become available.
		long start = System.currentTimeMillis();
		while(!TableState.AVAILABLE.equals(status.getState())){
			assertTrue("Timed out waiting for table index worker to make the table available.", (System.currentTimeMillis()-start) <  MAX_WAIT_MS);
			assertFalse("Failed to process table: "+status.getErrorMessage(), TableState.PROCESSING_FAILED.equals(status.getState()));
			System.out.println("Waiting for table index worker to build table..."+status.getProgressMessage()+ " current: "+status.getProgressCurrent()+" of: "+status.getProgressTotal());
			Thread.sleep(1000);
			status = tableRowManager.getTableStatus(tableId);
		}
		return status;
	}
	
}
