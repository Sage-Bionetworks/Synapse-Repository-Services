package org.sagebionetworks.table.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.sqs.MessagePollingReceiverImpl;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.TableRowCache;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableCurrentCacheWorkerIntegrationTest {

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
	MessagePollingReceiverImpl tableCurrentCacheQueueMessageReveiver;
	@Autowired
	TableRowCache tableRowCache;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	SemaphoreManager semphoreManager;
	
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
		tableCurrentCacheQueueMessageReveiver.emptyQueue();
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
			tableRowCache.truncateAllData();
		}
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

		assertEquals(0, tableRowCache.getCurrentVersionNumbers(KeyFactory.stringToKey(tableId), 0L, 1000L).size());

		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, schema, rowSet);

		assertTrue(TimeUtils.waitFor(20000, 200, null, new Predicate<String>() {
			@Override
			public boolean apply(String input) {
				long status = tableRowCache.getLatestCurrentVersionNumber(KeyFactory.stringToKey(tableId));
				return status == 0;
			}
		}));
		ImmutableMap<Long, Long> expected = ImmutableMap.<Long, Long>builder().put(0L, 0L).put(1L, 0L).put(2L, 0L).put(3L, 0L).put(4L, 0L).put(5L, 0L).build();
		Map<Long, Long> actual = tableRowCache.getCurrentVersionNumbers(KeyFactory.stringToKey(tableId), 0L, 1000L);
		assertEquals(expected, actual);

		expected = ImmutableMap.<Long, Long> builder().put(0L, 0L).put(1L, 0L).put(2L, 0L).put(3L, 0L).build();
		actual = tableRowCache.getCurrentVersionNumbers(KeyFactory.stringToKey(tableId), 0L, 4L);
		assertEquals(expected, actual);

		expected = ImmutableMap.<Long, Long> builder().put(4L, 0L).put(5L, 0L).build();
		actual = tableRowCache.getCurrentVersionNumbers(KeyFactory.stringToKey(tableId), 4L, 2L);
		assertEquals(expected, actual);
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

		assertTrue(TimeUtils.waitFor(20000, 200, null, new Predicate<String>() {
			@Override
			public boolean apply(String input) {
				long status = tableRowCache.getLatestCurrentVersionNumber(KeyFactory.stringToKey(tableId));
				return status == 2L;
			}
		}));
		ImmutableMap<Long, Long> expected = ImmutableMap.<Long, Long> builder().put(0L, 1L).put(1L, 2L).put(2L, 1L).put(3L, 2L).build();
		Map<Long, Long> actual = tableRowCache.getCurrentVersionNumbers(KeyFactory.stringToKey(tableId), 0L, 100L);
		assertEquals(expected, actual);
		assertEquals(2L, tableRowCache.getLatestCurrentVersionNumber(KeyFactory.stringToKey(tableId)));
	}
}
