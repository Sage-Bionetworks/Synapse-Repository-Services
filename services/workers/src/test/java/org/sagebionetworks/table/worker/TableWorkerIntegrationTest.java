package org.sagebionetworks.table.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.sqs.MessageReceiver;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.TableRowCache;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.CSVToRowIterator;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.AsynchDownloadResponseBody;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.TimeUtils;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.sagebionetworks.util.csv.CSVWriterStreamProxy;
import org.sagebionetworks.util.csv.CsvNullReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import au.com.bytecode.opencsv.CSVWriter;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableWorkerIntegrationTest {

	/**
	 * 
	 */
	public static final int MAX_WAIT_MS = 1000 * 60 * 100;
	
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
	TableRowCache tableRowCache;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	SemaphoreManager semphoreManager;
	
	@Autowired
	TableStatusDAO tableStatusDAO;
	@Autowired
	DBOChangeDAO changeDAO;
	@Autowired
	RepositoryMessagePublisher repositoryMessagePublisher;
	@Autowired
	private IdGenerator idGenerator;

	private UserInfo adminUserInfo;
	RowReferenceSet referenceSet;
	List<ColumnModel> schema;
	private String tableId;
	
	@Before
	public void before() throws NotFoundException, DatastoreException, IOException, InterruptedException{
		// Only run this test if the table feature is enabled.
		Assume.assumeTrue(config.getTableEnabled());
		semphoreManager.releaseAllLocksAsAdmin(new UserInfo(true));
		// Start with an empty queue.
		tableQueueMessageReveiver.emptyQueue();
		// Get the admin user
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		this.tableId = null;
		// Start with an empty database
		this.tableConnectionFactory.dropAllTablesForAllConnections();
		tableRowCache.truncateAllData();
	}
	
	@After
	public void after(){
		if(config.getTableEnabled()){
			// cleanup
			columnManager.truncateAllColumnData(adminUserInfo);
			// Drop all data in the index database
			this.tableConnectionFactory.dropAllTablesForAllConnections();
		}
		tableRowCache.truncateAllData();
	}

	@Test
	public void testRoundTrip() throws NotFoundException, InterruptedException, DatastoreException, TableUnavilableException, IOException{
		// Create one column of each type
		List<ColumnModel> columnModels = TableModelTestUtils.createOneOfEachType();
		schema = new LinkedList<ColumnModel>();
		for(ColumnModel cm: columnModels){
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
		String sql = "select * from " + tableId + " order by row_id limit 8";
		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		System.out.println("testRoundTrip");
		System.out.println(rowSet);
		assertNotNull(rowSet);
		assertEquals(tableId, rowSet.getTableId());
		assertNotNull(rowSet.getHeaders());
		assertEquals(schema.size(), rowSet.getHeaders().size());
		assertNotNull(rowSet.getRows());
		assertEquals(6, rowSet.getRows().size());
		assertNotNull(rowSet.getEtag());
		assertEquals("The etag for the last applied change set should be set for the status and the results",referenceSet.getEtag(), rowSet.getEtag());
		assertEquals("The etag should also match the rereferenceSet.etag",referenceSet.getEtag(), rowSet.getEtag());

		@SuppressWarnings("unchecked")
		Set<Long> all = mock(Set.class);
		when(all.contains(any())).thenReturn(true);
		RowSet expectedRowSet = tableRowManager.getRowSet(tableId, referenceSet.getRows().get(0).getVersionNumber(), all);
		assertEquals(expectedRowSet, rowSet);
	}

	/**
	 * Test if things work if the table index is not being build, which can happen for example after a migration
	 */
	@Test
	public void testRoundTripAfterMigrate() throws NotFoundException, InterruptedException, DatastoreException, TableUnavilableException,
			IOException {
		schema = new LinkedList<ColumnModel>();
		for (ColumnModel cm : TableModelTestUtils.createOneOfEachType()) {
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
		RowSet rowSet = new RowSet();
		rowSet.setRows(TableModelTestUtils.createRows(schema, 2));
		rowSet.setHeaders(headers);
		rowSet.setTableId(tableId);
		tableRowManager.appendRows(adminUserInfo, tableId, schema, rowSet);
		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id limit 8";
		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		assertEquals(2, rowSet.getRows().size());

		// reset table index
		tableStatusDAO.clearAllTableState();
		tableConnectionFactory.dropAllTablesForAllConnections();

		// now we still should get the index taken care of
		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		assertEquals(2, rowSet.getRows().size());
	}

	/**
	 * Test if things work after a migration where there is no table status, but the index and current index have to be
	 * built
	 */
	@Test
	public void testAfterMigrate() throws NotFoundException, InterruptedException, DatastoreException, TableUnavilableException, IOException {
		schema = new LinkedList<ColumnModel>();
		for (ColumnModel cm : TableModelTestUtils.createOneOfEachType()) {
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
		RowSet rowSet = new RowSet();
		rowSet.setRows(TableModelTestUtils.createRows(schema, 2));
		rowSet.setHeaders(headers);
		rowSet.setTableId(tableId);
		tableRowManager.appendRows(adminUserInfo, tableId, schema, rowSet);
		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id limit 8";
		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		assertEquals(2, rowSet.getRows().size());

		// reset table index
		tableStatusDAO.clearAllTableState();
		tableConnectionFactory.dropAllTablesForAllConnections();
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.CREATE);
		message.setObjectType(ObjectType.TABLE);
		message.setObjectId(KeyFactory.stringToKey(tableId).toString());
		message.setObjectEtag(UUID.randomUUID().toString());
		message = changeDAO.replaceChange(message);
		// and pretend we just created it
		repositoryMessagePublisher.publishToTopic(message);

		final TableIndexDAO tableIndexDAO = tableConnectionFactory.getConnection(tableId);
		assertTrue("Index table was not created", TimeUtils.waitFor(20000, 500, null, new Predicate<Void>() {
			@Override
			public boolean apply(Void input) {
				return tableIndexDAO.getRowCountForTable(tableId) != null;
			}
		}));
		assertTrue("Current index was not created", TimeUtils.waitFor(20000, 500, null, new Predicate<Void>() {
			@Override
			public boolean apply(Void input) {
				return tableRowCache.getCurrentVersionNumbers(KeyFactory.stringToKey(tableId), 0, 1000).size() == 2;
			}
		}));

		// now we still should get the index taken care of
		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		assertEquals(2, rowSet.getRows().size());
	}

	@Test
	public void testPartialUpdateRoundTrip() throws NotFoundException, InterruptedException, DatastoreException, TableUnavilableException,
			IOException {
		// Create one column of each type
		List<ColumnModel> columnModels = TableModelTestUtils.createOneOfEachType(true);
		schema = new LinkedList<ColumnModel>();
		for (ColumnModel cm : columnModels) {
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
		// Add null rows
		rows.addAll(TableModelTestUtils.createNullRows(schema, 3));
		// Add empty rows
		rows.addAll(TableModelTestUtils.createEmptyRows(schema, 3));
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(headers);
		rowSet.setTableId(tableId);
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, schema, rowSet);

		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id limit 100";
		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		assertEquals(16, rowSet.getRows().size());

		@SuppressWarnings("unchecked")
		Set<Long> all = mock(Set.class);
		when(all.contains(any())).thenReturn(true);
		RowSet expectedRowSet = tableRowManager.getRowSet(tableId, referenceSet.getRows().get(0).getVersionNumber(), all);

		// apply updates to expected and actual
		List<PartialRow> partialRows = Lists.newArrayList(); 
		for(int i = 0; i < 16; i++){
			partialRows.add(TableModelTestUtils.updatePartialRow(schema, expectedRowSet.getRows().get(i), i));
			expectedRowSet.getRows().get(i).setVersionNumber(1L);
		}
		rows = TableModelTestUtils.createExpectedFullRows(schema, 5);
		for (int i = 0; i < rows.size(); i++) {
			rows.get(i).setRowId(16L + i);
			rows.get(i).setVersionNumber(1L);
		}
		expectedRowSet.getRows().addAll(rows);
		partialRows.addAll(TableModelTestUtils.createPartialRows(schema, 5));

		PartialRowSet partialRowSet = new PartialRowSet();
		partialRowSet.setRows(partialRows);
		partialRowSet.setTableId(tableId);
		tableRowManager.appendPartialRows(adminUserInfo, tableId, schema, partialRowSet);

		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		// we couldn't know the etag in advance
		expectedRowSet.setEtag(rowSet.getEtag());
		assertEquals(expectedRowSet.toString(), rowSet.toString());
		assertEquals(expectedRowSet, rowSet);
	}

	@Test
	public void testDates() throws Exception {
		schema = Lists.newArrayList(columnManager.createColumnModel(adminUserInfo,
				TableModelTestUtils.createColumn(0L, "coldate", ColumnType.DATE)));
		List<String> headers = TableModelUtils.getHeaders(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(headers);
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, headers, tableId, true);
		DateFormat dateTimeInstance = new SimpleDateFormat("yyy-M-d h:mm");
		dateTimeInstance.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date[] dates = new Date[] { dateTimeInstance.parse("2014-2-3 2:12"), dateTimeInstance.parse("2014-2-3 3:41"),
				dateTimeInstance.parse("2015-2-3 3:41"), dateTimeInstance.parse("2016-2-3 3:41") };
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, dates.length);
		for (int i = 0; i < dates.length; i++) {
			rows.get(i).getValues().set(0, "" + dates[i].getTime());
		}

		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(headers);
		rowSet.setTableId(tableId);
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, schema, rowSet);

		// Wait for the table to become available
		String sql = "select * from " + tableId + " where coldate between '2014-2-3 3:00' and '2016-1-1' order by coldate asc limit 8";
		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		assertNotNull(rowSet);
		assertEquals(2, rowSet.getRows().size());
		assertEquals("2014-2-3 3:41", dateTimeInstance.format(new Date(Long.parseLong(rowSet.getRows().get(0).getValues().get(0)))));
		assertEquals("2015-2-3 3:41", dateTimeInstance.format(new Date(Long.parseLong(rowSet.getRows().get(1).getValues().get(0)))));

		// Again, but now with longs
		sql = "select * from " + tableId + " where coldate between " + dateTimeInstance.parse("2014-2-3 3:00").getTime() + " and "
				+ dateTimeInstance.parse("2016-1-1 0:00").getTime() + " order by coldate asc limit 8";
		RowSet rowSet2 = waitForConsistentQuery(adminUserInfo, sql);
		assertEquals(rowSet, rowSet2);
	}

	@Test
	public void testBooleans() throws Exception {
		schema = Lists.newArrayList(columnManager.createColumnModel(adminUserInfo,
				TableModelTestUtils.createColumn(0L, "colbool", ColumnType.BOOLEAN)));
		List<String> headers = TableModelUtils.getHeaders(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(headers);
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, headers, tableId, true);

		String[] booleans = new String[] { null, "", "true", "false", "True", "False", "TRUE", "FALSE", Boolean.TRUE.toString(),
				Boolean.FALSE.toString(), Boolean.FALSE.toString() };
		String[] expectedOut = new String[] { null, null, "true", "false", "true", "false", "true", "false", "true", "false", "false" };
		int expectedTrueCount = 4;
		int expectedFalseCount = 5;
		int expectedNullCount = 2;
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, booleans.length);
		for (int i = 0; i < booleans.length; i++) {
			rows.get(i).getValues().set(0, booleans[i]);
		}

		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(headers);
		rowSet.setTableId(tableId);
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, schema, rowSet);

		String[] failingBooleans = new String[] { "1", "0", "2", "falseish", "nottrue" };
		for (String failingBoolean : failingBooleans) {
			List<Row> failRow = TableModelTestUtils.createRows(schema, 1);
			failRow.get(0).getValues().set(0, failingBoolean);

			RowSet failRowSet = new RowSet();
			failRowSet.setRows(failRow);
			failRowSet.setHeaders(headers);
			failRowSet.setTableId(tableId);
			try {
				tableRowManager.appendRows(adminUserInfo, tableId, schema, failRowSet);
				fail("Should have rejected as boolean: " + failingBoolean);
			} catch (IllegalArgumentException e) {
			}
		}

		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id asc limit 20";
		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		assertNotNull(rowSet);
		assertEquals(expectedOut.length, rowSet.getRows().size());
		for (int i = 0; i < expectedOut.length; i++) {
			assertEquals(expectedOut[i], rowSet.getRows().get(i).getValues().get(0));
		}

		sql = "select * from " + tableId + " where colbool is true order by row_id asc limit 20";
		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		assertEquals(expectedTrueCount, rowSet.getRows().size());

		sql = "select * from " + tableId + " where colbool is false order by row_id asc limit 20";
		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		assertEquals(expectedFalseCount, rowSet.getRows().size());

		sql = "select * from " + tableId + " where colbool is not true order by row_id asc limit 20";
		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		assertEquals(expectedFalseCount + expectedNullCount, rowSet.getRows().size());

		sql = "select * from " + tableId + " where colbool is not false order by row_id asc limit 20";
		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		assertEquals(expectedTrueCount + expectedNullCount, rowSet.getRows().size());

		sql = "select * from " + tableId + " where colbool = true order by row_id asc limit 20";
		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		assertEquals(expectedTrueCount, rowSet.getRows().size());

		sql = "select * from " + tableId + " where colbool = false order by row_id asc limit 20";
		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		assertEquals(expectedFalseCount, rowSet.getRows().size());

		sql = "select * from " + tableId + " where colbool <> true order by row_id asc limit 20";
		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		assertEquals(expectedFalseCount, rowSet.getRows().size());

		sql = "select * from " + tableId + " where colbool <> false order by row_id asc limit 20";
		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		assertEquals(expectedTrueCount, rowSet.getRows().size());
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
		String sql = "select * from " + tableId + " limit 100";
		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		assertEquals("TableId: "+tableId, 2, rowSet.getRows().size());
		assertEquals("updatestring333", rowSet.getRows().get(0).getValues().get(0));
		assertEquals("updatestring555", rowSet.getRows().get(1).getValues().get(0));
	}

	@Test
	public void testBreakAndFixRoundTrip() throws NotFoundException, InterruptedException, DatastoreException, TableUnavilableException,
			IOException {
		schema = new LinkedList<ColumnModel>();
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.LONG);
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

		// Now add valid data
		RowSet rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(TableModelTestUtils.createRow(null, null, "123")));
		rowSet.setHeaders(headers);
		rowSet.setTableId(tableId);

		// cheat by passing in a different column model that will allow us to insert an invalid value
		List<ColumnModel> invalidSchema = new LinkedList<ColumnModel>();
		ColumnModel cheatingColumnModel = new ColumnModel();
		cheatingColumnModel.setColumnType(ColumnType.STRING);
		cheatingColumnModel.setMaximumSize(100L);
		cheatingColumnModel.setName("col1");
		cheatingColumnModel.setId(cm.getId());
		invalidSchema.add(cheatingColumnModel);

		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, schema, rowSet);

		String sql = "select * from " + tableId + " limit 100";
		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		assertEquals("TableId: " + tableId, 1, rowSet.getRows().size());

		// Now add invalid data using the invalid schema
		rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(TableModelTestUtils.createRow(null, null, Long.toString(Long.MAX_VALUE) + "234")));
		rowSet.setHeaders(headers);
		rowSet.setTableId(tableId);
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, invalidSchema, rowSet);

		waitForConsistentQueryError(adminUserInfo, sql);
		try {
			// should fail immediately
			tableRowManager.query(adminUserInfo, sql, true, false);
			fail("should not have succeeded");
		} catch (TableUnavilableException e) {
			assertNotNull(e.getStatus().getErrorMessage());
		}

		// Now fix the error
		rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(TableModelTestUtils.createRow(referenceSet.getRows().get(0).getRowId(),
				referenceSet.getRows().get(0).getVersionNumber(), "456")));
		rowSet.setHeaders(headers);
		rowSet.setTableId(tableId);
		rowSet.setEtag(referenceSet.getEtag());
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, schema, rowSet);

		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		assertEquals("TableId: " + tableId, 2, rowSet.getRows().size());
	}

	@Test
	public void testPartialUpdate() throws NotFoundException, InterruptedException, DatastoreException, TableUnavilableException,
			IOException {
		// four columns, two with default value
		schema = new LinkedList<ColumnModel>();
		ColumnModel cm = new ColumnModel();
		cm.setName("col1");
		cm.setColumnType(ColumnType.STRING);
		schema.add(columnManager.createColumnModel(adminUserInfo, cm));
		cm.setName("col2");
		schema.add(columnManager.createColumnModel(adminUserInfo, cm));
		cm.setDefaultValue("default");
		cm.setName("col3");
		schema.add(columnManager.createColumnModel(adminUserInfo, cm));
		cm.setName("col4");
		schema.add(columnManager.createColumnModel(adminUserInfo, cm));
		List<String> headers = TableModelUtils.getHeaders(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(headers);
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		columnManager.bindColumnToObject(adminUserInfo, headers, tableId, true);

		// Now add some data
		List<Row> rows = Lists.newArrayList();
		for (int i = 0; i < 4; i++) {
			rows.add(TableModelTestUtils.createRow(null, null, "something", null, "something", null));
		}
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(headers);
		rowSet.setTableId(tableId);
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, schema, rowSet);

		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id limit 20";
		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		assertEquals(4, rowSet.getRows().size());
		for (int i = 0; i < 4; i++) {
			assertEquals(Lists.newArrayList("something", null, "something", "default"), rowSet.getRows().get(i).getValues());
		}

		// append
		PartialRow partialRowAppend = new PartialRow();
		partialRowAppend.setRowId(null);
		partialRowAppend.setValues(ImmutableMap.<String, String> builder().put(schema.get(0).getId(), "something")
				.put(schema.get(2).getId(), "something").build());

		// update null columns
		PartialRow partialRowUpdateNull = new PartialRow();
		partialRowUpdateNull.setRowId(rowSet.getRows().get(0).getRowId());
		partialRowUpdateNull.setValues(ImmutableMap.<String, String> builder().put(schema.get(1).getId(), "other")
				.put(schema.get(3).getId(), "other").build());

		// update non null columns
		PartialRow partialRowUpdateNonNull = new PartialRow();
		partialRowUpdateNonNull.setRowId(rowSet.getRows().get(1).getRowId());
		partialRowUpdateNonNull.setValues(ImmutableMap.<String, String> builder().put(schema.get(0).getId(), "other")
				.put(schema.get(2).getId(), "other").build());

		// update nothing
		PartialRow partialRowUpdateNothing = new PartialRow();
		partialRowUpdateNothing.setRowId(rowSet.getRows().get(2).getRowId());
		partialRowUpdateNothing.setValues(ImmutableMap.<String, String> builder().build());

		// update with nulls
		PartialRow partialRowUpdateNulls = new PartialRow();
		partialRowUpdateNulls.setRowId(rowSet.getRows().get(3).getRowId());
		Map<String, String> values = Maps.newHashMap();
		values.put(schema.get(0).getId(), null);
		values.put(schema.get(1).getId(), null);
		values.put(schema.get(2).getId(), null);
		values.put(schema.get(3).getId(), null);
		partialRowUpdateNulls.setValues(values);

		PartialRowSet partialRowSet = new PartialRowSet();
		partialRowSet.setTableId(tableId);
		partialRowSet.setRows(Lists.newArrayList(partialRowAppend, partialRowUpdateNull, partialRowUpdateNonNull, partialRowUpdateNothing,
				partialRowUpdateNulls));
		tableRowManager.appendPartialRows(adminUserInfo, tableId, schema, partialRowSet);

		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		assertEquals(5, rowSet.getRows().size());
		// update null columns
		assertEquals(Lists.newArrayList("something", "other", "something", "other"), rowSet.getRows().get(0).getValues());
		// update non null columns
		assertEquals(Lists.newArrayList("other", null, "other", "default"), rowSet.getRows().get(1).getValues());
		// update nothing
		assertEquals(Lists.newArrayList("something", null, "something", "default"), rowSet.getRows().get(2).getValues());
		// update with nulls
		assertEquals(Lists.newArrayList(null, null, "default", "default"), rowSet.getRows().get(3).getValues());
		// append
		assertEquals(Lists.newArrayList("something", null, "something", "default"), rowSet.getRows().get(4).getValues());
	}

	@Ignore // This is a very slow test that pushes massive amounts of data so it is disabled.
	@Test
	public void testAppendRowsAtScale() throws NotFoundException, InterruptedException, DatastoreException, TableUnavilableException, IOException{
		// Create one column of each type
		List<ColumnModel> temp = TableModelTestUtils.createOneOfEachType();
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
		List<Row> rows = TableModelTestUtils.createRows(schema, 500000);
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(headers);
		rowSet.setTableId(tableId);
		long start = System.currentTimeMillis();
		String etag = tableRowManager.appendRowsAsStream(adminUserInfo, tableId, schema, rowSet.getRows().iterator(), null, null);
		System.out.println("Appended "+rowSet.getRows().size()+" rows in: "+(System.currentTimeMillis()-start)+" MS");
		// Wait for the table to become available
		String sql = "select * from "+tableId+" limit 2";
		rowSet = waitForConsistentQuery(adminUserInfo, sql);
		assertNotNull(rowSet);
		assertEquals(tableId, rowSet.getTableId());
		assertNotNull(rowSet.getHeaders());
		assertEquals(schema.size(), rowSet.getHeaders().size());
		assertNotNull(rowSet.getRows());
		assertEquals(2, rowSet.getRows().size());
		assertNotNull(rowSet.getEtag());
		assertEquals("The etag for the last applied change set should be set for the status and the results",etag, rowSet.getEtag());
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
		// Query for the results
		String sql = "select A, a, \"Has Space\",\""+specialChars+"\" from "+tableId+" limit 2";
		rowSet = waitForConsistentQuery(adminUserInfo, sql);
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
		// We should be able to query
		String sql = "select * from "+tableId+" limit 1";
		RowSet rowSet = waitForConsistentQuery(adminUserInfo, sql);
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
		// We should be able to query
		String sql = "select * from "+tableId+" limit 1";
		RowSet rowSet = waitForConsistentQuery(adminUserInfo, sql);
		System.out.println("testNoRows");
		System.out.println(rowSet);
		assertNotNull(rowSet);
		assertNull(rowSet.getEtag());
		assertEquals(tableId, rowSet.getTableId());
		assertTrue("TableId: "+tableId, rowSet.getHeaders() == null || rowSet.getHeaders().isEmpty());
		assertTrue("TableId: "+tableId, rowSet.getRows() == null || rowSet.getRows().isEmpty());
	}
	
	/**
	 * This test will first create a table from an input CSV, then stream all of the data from the table
	 * to an output CSV.  The output CSV is then updated and then used to update the table.
	 * The output date is then stream again, but without the headers so that it can be used to create
	 * a copy of original table.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCopyAndUpdateTableFromCSV() throws Exception {
		// Create one column of each type
		List<ColumnModel> temp = new LinkedList<ColumnModel>();
		temp.add(TableModelTestUtils.createColumn(0L, "a", ColumnType.STRING));
		temp.add(TableModelTestUtils.createColumn(0L, "b", ColumnType.LONG));
		temp.add(TableModelTestUtils.createColumn(0L, "c", ColumnType.DOUBLE));
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
		// Create some CSV data
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "a", "b", "c" });
		input.add(new String[] { "AAA", "2", "1.1" });
		input.add(new String[] { null, "3", "1.2" });
		input.add(new String[] { "FFF", "4", null });
		input.add(new String[] { "ZZZ", null, "1.3" });
		// This is the starting input stream
		CsvNullReader reader = TableModelTestUtils.createReader(input);
		// Write the CSV to the table
		CSVToRowIterator iterator = new CSVToRowIterator(schema, reader);
		tableRowManager.appendRowsAsStream(adminUserInfo, tableId, schema, iterator, null, null);
		// Now wait for the table index to be ready
		RowSet rowSet = waitForConsistentQuery(adminUserInfo, "select * from "+tableId+" limit 100");
		assertNotNull(rowSet);
		// Now stream the query results to a CSV
		StringWriter stringWriter = new StringWriter();
		CSVWriter csvWriter = new CSVWriter(stringWriter);
		CSVWriterStreamProxy proxy = new CSVWriterStreamProxy(csvWriter);
		// Downlaod the data to a csv
		boolean includeRowIdAndVersion = true;
		AsynchDownloadResponseBody response = waitForConsistentStreamQuery("select * from "+tableId, proxy, includeRowIdAndVersion);
		assertNotNull(response);
		assertNotNull(response.getEtag());
		// Read the results
		CsvNullReader copyReader = new CsvNullReader(new StringReader(stringWriter.toString()));
		List<String[]> copy = copyReader.readAll();
		assertNotNull(copy);
		// the results should include a header.
		assertEquals(input.size(),  copy.size());
		// the first two columns should include the rowId can verionNumber
		assertEquals(Arrays.asList(TableConstants.ROW_ID, TableConstants.ROW_VERSION, "a", "b", "c").toString(), Arrays.toString(copy.get(0)));
		assertEquals(Arrays.asList("0", "0", "AAA", "2", "1.1").toString(), Arrays.toString(copy.get(1)));
		assertEquals(Arrays.asList("1", "0",  null, "3", "1.2" ).toString(), Arrays.toString(copy.get(2)));
		// make some changes
		copy.get(1)[2] = "DDD";
		copy.get(2)[2] = "EEE";
		copy.get(3)[2] = "FFF";
		reader = TableModelTestUtils.createReader(copy);
		// Use the data to update the table
		iterator = new CSVToRowIterator(schema, reader);
		tableRowManager.appendRowsAsStream(adminUserInfo, tableId, schema, iterator, response.getEtag(), null);
		// Fetch the results again but this time without row id and version so it can be used to create a new table.
		stringWriter = new StringWriter();
		csvWriter = new CSVWriter(stringWriter);
		proxy = new CSVWriterStreamProxy(csvWriter);
		includeRowIdAndVersion = false;
		response = waitForConsistentStreamQuery("select c, a, b from "+tableId, proxy, includeRowIdAndVersion);
		// read the results
		copyReader = new CsvNullReader(new StringReader(stringWriter.toString()));
		copy = copyReader.readAll();
		assertNotNull(copy);
		// As long as the updated data does not includes rowIds and row version we can use it to create a new table.
		assertEquals(Arrays.asList( "c", "a", "b").toString(), Arrays.toString(copy.get(0)));
		assertEquals(Arrays.asList("1.1","DDD", "2").toString(), Arrays.toString(copy.get(1)));
		assertEquals(Arrays.asList("1.2","EEE", "3").toString(), Arrays.toString(copy.get(2)));
	}
	
	/**
	 * Attempt to run a query. If the table is unavailable, it will continue to try until successful or the timeout is exceeded.
	 * 
	 * @param user
	 * @param sql
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InterruptedException
	 */
	private RowSet waitForConsistentQuery(UserInfo user, String sql) throws DatastoreException, NotFoundException, InterruptedException{
		long start = System.currentTimeMillis();
		while(true){
			try {
				return  tableRowManager.query(adminUserInfo, sql, true, false);
			} catch (TableUnavilableException e) {
				assertTrue("Timed out waiting for table index worker to make the table available.", (System.currentTimeMillis()-start) <  MAX_WAIT_MS);
				assertNotNull(e.getStatus());
				assertFalse("Failed: "+e.getStatus().getErrorMessage(),TableState.PROCESSING_FAILED.equals(e.getStatus().getState()));
				System.out.println("Waiting for table index worker to build table. Status: "+e.getStatus());
				Thread.sleep(1000);
			}
		}
	}
	
	private void waitForConsistentQueryError(UserInfo user, String sql) {
		assertTrue(TimeUtils.waitFor(MAX_WAIT_MS, 250, sql, new Predicate<String>() {
			@Override
			public boolean apply(String sql) {
				try {
					tableRowManager.query(adminUserInfo, sql, true, false);
					fail("should not have succeeded");
				} catch (TableUnavilableException e) {
					if (e.getStatus().getErrorMessage() != null) {
						return true;
					}
				} catch (Exception e) {
					fail("unexpected exception " + e.getMessage());
				}
				return false;
			}
		}));
	}

	/**
	 * Attempt to run a query as a stream.  If the table is unavailable, it will continue to try until successful or the timeout is exceeded.
	 * @param sql
	 * @param writer
	 * @param includeRowIdAndVersion
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InterruptedException
	 */
	private AsynchDownloadResponseBody waitForConsistentStreamQuery(String sql, CSVWriterStream writer, boolean includeRowIdAndVersion) throws DatastoreException, NotFoundException, InterruptedException{
		long start = System.currentTimeMillis();
		while(true){
			try {
				return  tableRowManager.runConsistentQueryAsStream(sql, writer, includeRowIdAndVersion);
			} catch (TableUnavilableException e) {
				assertTrue("Timed out waiting for table index worker to make the table available.", (System.currentTimeMillis()-start) <  MAX_WAIT_MS);
				assertNotNull(e.getStatus());
				assertFalse("Failed: "+e.getStatus().getErrorMessage(),TableState.PROCESSING_FAILED.equals(e.getStatus().getState()));
				System.out.println("Waiting for table index worker to build table. Status: "+e.getStatus());
				Thread.sleep(1000);
			}
		}
	}
	
}
