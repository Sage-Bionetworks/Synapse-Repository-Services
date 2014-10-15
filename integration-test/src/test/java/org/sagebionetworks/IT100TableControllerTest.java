package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseConflictingUpdateException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseResultNotReadyException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableFileHandleResults;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;

import com.google.common.collect.Lists;

/**
 * Tests for TableEntity and ColumnModel services.
 * 
 * @author jmhill
 *
 */
public class IT100TableControllerTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;

	private List<Entity> entitiesToDelete;
	private List<TableEntity> tablesToDelete;
	private List<File> tempFiles = Lists.newArrayList();
	
	private static long MAX_QUERY_TIMEOUT_MS = 1000*60*5;
	
	@BeforeClass 
	public static void beforeClass() throws Exception {
		// Only run this test if the table feature is enabled.
		Assume.assumeTrue(new StackConfiguration().getTableEnabled());
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
	}
	
	@Before
	public void before(){
		entitiesToDelete = new LinkedList<Entity>();
		tablesToDelete = new ArrayList<TableEntity>();
	}
	
	@After
	public void after() throws Exception {
		for (Entity entity : tablesToDelete) {
			adminSynapse.deleteAndPurgeEntity(entity);
		}
		for (Entity entity : entitiesToDelete) {
			adminSynapse.deleteAndPurgeEntity(entity);
		}
		for (File tempFile : tempFiles) {
			tempFile.delete();
		}
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		//TODO This delete should not need to be surrounded by a try-catch
		// This means proper cleanup was not done by the test 
		try {
			adminSynapse.deleteUser(userToDelete);
		} catch (Exception e) { }
	}

	@Test
	public void testCreateGetColumn() throws SynapseException{
		ColumnModel cm = new ColumnModel();
		cm.setName("ExampleColumn");
		cm.setColumnType(ColumnType.STRING);
		cm = synapse.createColumnModel(cm);
		assertNotNull(cm);
		assertNotNull(cm.getId());
		ColumnModel clone = synapse.getColumnModel(cm.getId());
		assertNotNull(clone);
		assertEquals(cm, clone);
	}
	
	@Test
	public void testCreateGetColumns() throws SynapseException {
		List<ColumnModel> cms = Lists.newArrayList();
		for (int i = 0; i < 2; i++) {
			ColumnModel cm = new ColumnModel();
			cm.setName("ExampleColumn" + i);
			cm.setColumnType(ColumnType.STRING);
			cms.add(cm);
		}
		List<ColumnModel> results = synapse.createColumnModels(cms);
		assertNotNull(results);
		assertEquals(2, results.size());
		for (int i = 0; i < 2; i++) {
			assertNotNull(results.get(i).getId());
			ColumnModel clone = synapse.getColumnModel(results.get(i).getId());
			assertNotNull(clone);
			assertEquals(results.get(i), clone);
			assertEquals(cms.get(i).getName(), clone.getName());
		}
	}

	@Test
	public void testCreateTableEntity() throws Exception {
		// Create a few columns to add to a table entity
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.STRING);
		one = synapse.createColumnModel(one);
		// two
		ColumnModel two = new ColumnModel();
		two.setName("two");
		two.setColumnType(ColumnType.STRING);
		two = synapse.createColumnModel(two);
		// Create a project to contain it all
		Project project = new Project();
		project.setName(UUID.randomUUID().toString());
		project = synapse.createEntity(project);
		assertNotNull(project);
		entitiesToDelete.add(project);
		
		// now create a table entity
		TableEntity table = createTable(Lists.newArrayList(one.getId(), two.getId()));
		
		assertNotNull(table);
		assertNotNull(table.getId());
		// Now make sure we can get the columns for this entity.
		List<ColumnModel> columns = synapse.getColumnModelsForTableEntity(table.getId());
		assertNotNull(columns);
		assertEquals(2, columns.size());
		List<ColumnModel> expected = new LinkedList<ColumnModel>();
		expected.add(one);
		expected.add(two);
		assertEquals(expected, columns);
		
		// Append some rows
		RowSet set = new RowSet();
		List<Row> rows = TableModelTestUtils.createRows(columns, 2);
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(columns));
		set.setTableId(table.getId());
		RowReferenceSet results1 = synapse.appendRowsToTable(set);
		assertNotNull(results1);
		assertNotNull(results1.getRows());
		assertEquals(2, results1.getRows().size());
		assertEquals(table.getId(), results1.getTableId());
		assertEquals(TableModelUtils.getHeaders(columns), results1.getHeaders());
		
		// Now attempt to query for the table results
		RowSet queryResults = waitForQueryResults("select * from " + table.getId(), null, null);
		assertNotNull(queryResults);
		assertNotNull(queryResults.getEtag());
		assertEquals(results1.getEtag(), queryResults.getEtag());
		assertEquals(table.getId(), queryResults.getTableId());
		assertNotNull(queryResults.getRows());
		assertEquals(2, queryResults.getRows().size());

		// get the rows direct
		RowSet directResults = synapse.getRowsFromTable(results1);
		assertEquals(queryResults, directResults);

		// Now use these results to update the table. By setting the row IDs to null, they should be treated as additions
		for(Row row: queryResults.getRows()){
			row.setRowId(null);
			row.setVersionNumber(null);
		}
		RowReferenceSet results2 = synapse.appendRowsToTable(queryResults);
		assertNotNull(results2);
		assertNotNull(results2.getRows());
		// run the query again, but this time get the counts
		Long count = waitForCountResults("select * from " + table.getId());
		assertNotNull(count);
		assertEquals("There should be 4 rows in this table", 4L, count.longValue());

		// Now use these results to delete a row using the row delete api
		RowSelection toDelete = new RowSelection();
		toDelete.setEtag(results1.getEtag());
		toDelete.setTableId(results1.getTableId());
		toDelete.setRowIds(Lists.newArrayList(results1.getRows().get(0).getRowId()));
		RowReferenceSet results4 = synapse.deleteRowsFromTable(toDelete);
		assertNotNull(results4);
		assertNotNull(results4.getRows());
		assertEquals(1, results4.getRows().size());

		// run the query again, to get the counts
		count = waitForCountResults("select * from " + table.getId());
		assertEquals("There should be 3 rows in this table", 3L, count.longValue());
		
		// run a bundled query
		int mask = 0x1 | 0x2 | 0x4 | 0x8;
		QueryResultBundle bundle = waitForBundleQueryResults("select one from " + table.getId(), 0L, 2L, mask);
		assertNotNull(bundle);
		assertNotNull(bundle.getQueryResult().getQueryResults());
		assertNotNull(bundle.getQueryResult().getQueryResults().getEtag());
		assertEquals(table.getId(), bundle.getQueryResult().getQueryResults().getTableId());
		assertNotNull(bundle.getQueryResult().getQueryResults().getRows());
		assertEquals(2, bundle.getQueryResult().getQueryResults().getRows().size());
		assertEquals("There should be 3 rows in this table", new Long(3), bundle.getQueryCount());
		assertEquals(Arrays.asList(one), bundle.getSelectColumns());
		assertNotNull(bundle.getMaxRowsPerPage());
		assertTrue(bundle.getMaxRowsPerPage() > 0);

		bundle = waitForBundleQueryResults("select one from " + table.getId(), 2L, 2L, mask);
		assertNotNull(bundle);
		assertNotNull(bundle.getQueryResult().getQueryResults());
		assertNotNull(bundle.getQueryResult().getQueryResults().getEtag());
		assertEquals(table.getId(), bundle.getQueryResult().getQueryResults().getTableId());
		assertNotNull(bundle.getQueryResult().getQueryResults().getRows());
		assertEquals(1, bundle.getQueryResult().getQueryResults().getRows().size());
		assertEquals("There should be 3 rows in this table", new Long(3), bundle.getQueryCount());
		assertEquals(Arrays.asList(one), bundle.getSelectColumns());
		assertNotNull(bundle.getMaxRowsPerPage());
		assertTrue(bundle.getMaxRowsPerPage() > 0);
	}

	@Test
	public void testColumnOrdering() throws SynapseException, InterruptedException {
		// Create a few columns to add to a table entity
		List<ColumnModel> columns = Lists.newArrayList();
		List<String> idList = Lists.newArrayList();
		for (int i = 0; i < 3; i++) {
			ColumnModel cm = new ColumnModel();
			cm.setName("col" + i);
			cm.setColumnType(ColumnType.STRING);
			cm = synapse.createColumnModel(cm);
			columns.add(cm);
			idList.add(cm.getId());
		}

		// Create a project to contain it all
		TableEntity table = createTable(idList);

		// Append some rows
		List<Row> rows = Lists.newArrayList();
		for (int i = 0; i < 2; i++) {
			String[] values = new String[columns.size()];
			for (int j = 0; j < columns.size(); j++) {
				values[j] = "val-" + i + "-" + j;
			}
			Row row = TableModelTestUtils.createRow(null, null, values);
			row.setValues(Lists.reverse(row.getValues()));
			rows.add(row);
		}
		List<PartialRow> partialRows = Lists.newArrayList();
		for (int i = 2; i < 4; i++) {
			String[] keysAndValues = new String[columns.size() * 2];
			for (int j = 0; j < columns.size(); j++) {
				keysAndValues[2 * j] = columns.get(j).getId();
				keysAndValues[2 * j + 1] = "val-" + i + "-" + j;
			}
			PartialRow row = TableModelTestUtils.createPartialRow(null, keysAndValues);
			partialRows.add(row);
		}

		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(Lists.reverse(TableModelUtils.getHeaders(columns)));
		set.setTableId(table.getId());
		RowReferenceSet newRows = synapse.appendRowsToTable(set);

		PartialRowSet toAppend = new PartialRowSet();
		toAppend.setTableId(table.getId());
		toAppend.setRows(partialRows);
		RowReferenceSet newRows2 = synapse.appendPartialRowsToTable(toAppend);

		// get in original order
		RowReferenceSet toGet = new RowReferenceSet();
		toGet.setTableId(table.getId());
		toGet.setHeaders(TableModelUtils.getHeaders(columns));
		toGet.setRows(newRows.getRows());
		RowSet rowsFromTable = synapse.getRowsFromTable(toGet);
		assertEquals(toGet.getHeaders(), rowsFromTable.getHeaders());

		for (int i = 0; i < 2; i++) {
			String[] values = new String[columns.size()];
			for (int j = 0; j < columns.size(); j++) {
				values[j] = "val-" + i + "-" + j;
			}
			assertEquals(Lists.newArrayList(values), rowsFromTable.getRows().get(i).getValues());
		}

		// get in original order
		toGet.setRows(newRows2.getRows());
		rowsFromTable = synapse.getRowsFromTable(toGet);
		assertEquals(toGet.getHeaders(), rowsFromTable.getHeaders());

		for (int i = 2; i < 4; i++) {
			String[] values = new String[columns.size()];
			for (int j = 0; j < columns.size(); j++) {
				values[j] = "val-" + i + "-" + j;
			}
			assertEquals(Lists.newArrayList(values), rowsFromTable.getRows().get(i - 2).getValues());
		}

		// get in reverse order
		toGet.setHeaders(Lists.reverse(TableModelUtils.getHeaders(columns)));
		toGet.setRows(newRows.getRows());
		rowsFromTable = synapse.getRowsFromTable(toGet);
		assertEquals(toGet.getHeaders(), rowsFromTable.getHeaders());

		for (int i = 0; i < 2; i++) {
			String[] values = new String[columns.size()];
			for (int j = 0; j < columns.size(); j++) {
				values[j] = "val-" + i + "-" + j;
			}
			assertEquals(Lists.reverse(Lists.newArrayList(values)), rowsFromTable.getRows().get(i).getValues());
		}
	}

	@Test
	public void testQueryDoubles() throws Exception {
		// Create a few columns to add to a table entity
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.DOUBLE);
		one = synapse.createColumnModel(one);

		TableEntity table = createTable(Lists.newArrayList(one.getId()));

		List<ColumnModel> columns = synapse.getColumnModelsForTableEntity(table.getId());

		// Append some rows
		RowSet set = new RowSet();
		List<Row> rows = Lists.newArrayList(TableModelTestUtils.createRow(null, null, "-1.2"),
				TableModelTestUtils.createRow(null, null, "-.2"), TableModelTestUtils.createRow(null, null, ".2"),
				TableModelTestUtils.createRow(null, null, "1.2"));
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(columns));
		set.setTableId(table.getId());
		synapse.appendRowsToTable(set);

		// Now attempt to query for the table results
		Long count = waitForCountResults("select * from " + table.getId() + " where one > -2.2");
		assertEquals(4L, count.longValue());
		count = waitForCountResults("select * from " + table.getId() + " where one > -1.3");
		assertEquals(4L, count.longValue());
		count = waitForCountResults("select * from " + table.getId() + " where one > -.3");
		assertEquals(3L, count.longValue());
		count = waitForCountResults("select * from " + table.getId() + " where one > -.1");
		assertEquals(2L, count.longValue());
		count = waitForCountResults("select * from " + table.getId() + " where one > .1");
		assertEquals(2L, count.longValue());
		count = waitForCountResults("select * from " + table.getId() + " where one > .3");
		assertEquals(1L, count.longValue());
		count = waitForCountResults("select * from " + table.getId() + " where one > 1.3");
		assertEquals(0L, count.longValue());
	}

	@Test
	public void testAdminRebuild() throws Exception {
		// Create a few columns to add to a table entity
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.DOUBLE);
		one = synapse.createColumnModel(one);

		TableEntity table = createTable(Lists.newArrayList(one.getId()));

		List<ColumnModel> columns = synapse.getColumnModelsForTableEntity(table.getId());

		// Append some rows
		for (int i = 0; i < 10; i++) {
			RowSet set = new RowSet();
			set.setRows(TableModelTestUtils.createRows(columns, 4));
			set.setHeaders(TableModelUtils.getHeaders(columns));
			set.setTableId(table.getId());
			synapse.appendRowsToTable(set);
		}

		// Now query for the table results
		Long count = waitForCountResults("select * from " + table.getId());
		assertEquals(40L, count.longValue());

		adminSynapse.rebuildTableCacheAndIndex(table.getId());
		final String asyncToken = synapse.queryTableEntityBundleAsyncStart("select * from " + table.getId(), null, null, true,
				SynapseClient.COUNT_PARTMASK);
		try {
			synapse.queryTableEntityBundleAsyncGet(asyncToken);
			fail("table should be invalid");
		} catch (SynapseResultNotReadyException e) {
		}
		count = TimeUtils.waitFor(MAX_QUERY_TIMEOUT_MS, 500L, new Callable<Pair<Boolean, QueryResultBundle>>() {
			@Override
			public Pair<Boolean, QueryResultBundle> call() throws Exception {
				try {
					QueryResultBundle result = synapse.queryTableEntityBundleAsyncGet(asyncToken);
					return Pair.create(true, result);
				} catch (SynapseResultNotReadyException e) {
					return Pair.create(false, null);
				}
			}
		}).getQueryCount();
		assertEquals(40L, count.longValue());
	}

	/**
	 * Wait for a query results.
	 * 
	 * @param sql
	 * @return
	 * @throws Exception
	 */
	public RowSet waitForQueryResults(String sql, Long offset, Long limit) throws Exception {
		return waitForBundleQueryResults(sql, offset, limit, SynapseClient.QUERY_PARTMASK).getQueryResult().getQueryResults();
	}

	/**
	 * Wait for a count results.
	 * 
	 * @param sql
	 * @return
	 * @throws InterruptedException
	 * @throws SynapseException
	 */
	public long waitForCountResults(String sql) throws Exception {
		return waitForBundleQueryResults(sql, null, null, SynapseClient.COUNT_PARTMASK).getQueryCount();
	}
	
	/**
	 * Wait for a consistent query results.
	 * 
	 * @param sql
	 * @return
	 * @throws InterruptedException
	 * @throws SynapseException
	 */
	public QueryResultBundle waitForBundleQueryResults(String sql, Long offset, Long limit, int partsMask) throws Exception {
		final String asyncToken = synapse.queryTableEntityBundleAsyncStart(sql, offset, limit, true, partsMask);
		return TimeUtils.waitFor(MAX_QUERY_TIMEOUT_MS, 500L, new Callable<Pair<Boolean, QueryResultBundle>>() {
			@Override
			public Pair<Boolean, QueryResultBundle> call() throws Exception {
				try {
					QueryResultBundle result = synapse.queryTableEntityBundleAsyncGet(asyncToken);
					return Pair.create(true, result);
				} catch (SynapseResultNotReadyException e) {
					return Pair.create(false, null);
				}
			}
		});
	}

	@Test
	public void testAddRetrieveFileHandle() throws Exception {
		// Create a few columns to add to a table entity
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.FILEHANDLEID);
		one = synapse.createColumnModel(one);

		TableEntity table = createTable(Lists.newArrayList(one.getId()));

		List<ColumnModel> columns = synapse.getColumnModelsForTableEntity(table.getId());

		File tempFile = File.createTempFile("temp", ".txt");
		tempFiles.add(tempFile);
		Writer writer = new FileWriter(tempFile);
		writer.write("a temporary string");
		writer.close();

		FileHandle fileHandle = synapse.createFileHandle(tempFile, "text/plain", table.getId());
		assertTrue(fileHandle instanceof S3FileHandle);

		// Append some rows
		RowSet set = new RowSet();
		List<Row> rows = Collections.singletonList(TableModelTestUtils.createRow(null, null, fileHandle.getId()));
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(columns));
		set.setTableId(table.getId());
		RowReferenceSet results = synapse.appendRowsToTable(set);

		TableFileHandleResults fileHandles = synapse.getFileHandlesFromTable(results);
		assertEquals(fileHandle.getId(), fileHandles.getRows().get(0).getList().get(0).getId());

		URL url = synapse.getTableFileHandleTemporaryUrl(table.getId(), results.getRows().get(0), one.getId());
		assertTrue("The temporary URL did not contain the expected file handle key",
				url.toString().indexOf(URLEncoder.encode(((S3FileHandle) fileHandle).getKey(), "UTF-8")) > 0);

		File tempFile2 = File.createTempFile("temp", ".txt");
		tempFiles.add(tempFile2);
		synapse.downloadFromTableFileHandleTemporaryUrl(table.getId(), results.getRows().get(0), one.getId(), tempFile2);

		BufferedReader reader = new BufferedReader(new FileReader(tempFile2));
		String fileContent = reader.readLine();
		reader.close();

		assertEquals("a temporary string", fileContent);
	}

	@Test
	public void testListColumnModels() throws Exception{
		ColumnModel one = new ColumnModel();
		String prefix = UUID.randomUUID().toString();
		one.setName(prefix+"a");
		one.setColumnType(ColumnType.STRING);
		one = synapse.createColumnModel(one);
		// two
		ColumnModel two = new ColumnModel();
		two.setName(prefix+"b");
		two.setColumnType(ColumnType.STRING);
		two = synapse.createColumnModel(two);
		// three
		ColumnModel three = new ColumnModel();
		three.setName(prefix+"bb");
		three.setColumnType(ColumnType.STRING);
		three = synapse.createColumnModel(three);
		// Now make sure we can find our columns
		PaginatedColumnModels pcm = synapse.listColumnModels(null, null, null);
		assertNotNull(pcm);
		assertTrue(pcm.getTotalNumberOfResults() >= 3);
		// filter by our prefix
		pcm = synapse.listColumnModels(prefix, null, null);
		assertNotNull(pcm);
		List<ColumnModel> expected = new LinkedList<ColumnModel>();
		expected.add(one);
		expected.add(two);
		expected.add(three);
		assertEquals(new Long(3), pcm.getTotalNumberOfResults());
		assertEquals(expected, pcm.getResults());
		// Now try pagination.
		pcm = synapse.listColumnModels(prefix, 1l, 2l);
		assertNotNull(pcm);
		assertEquals(new Long(3), pcm.getTotalNumberOfResults());
		expected.clear();
		expected.add(three);
		assertEquals(expected, pcm.getResults());
	}
	
	@Test
	public void testEmtpyTableRoundTrip() throws Exception {
		TableEntity table = createTable(null);
		// Now attempt to query for the table results
		// This table has no rows and no columns
		RowSet queryResults = waitForQueryResults("select * from " + table.getId(), 0L, 2L);
		assertNotNull(queryResults);
		assertNull(queryResults.getEtag());
		assertEquals(table.getId(), queryResults.getTableId());
		assertNull(queryResults.getRows());
		assertNull(queryResults.getHeaders());
	}
	
	@Test
	public void testConflictingResultS() throws Exception {
		// Create a few columns to add to a table entity
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.STRING);
		one = synapse.createColumnModel(one);
		List<ColumnModel> columns = Arrays.asList(one);

		TableEntity table = createTable(Lists.newArrayList(one.getId()));
		
		RowSet set = new RowSet();
		List<Row> rows = TableModelTestUtils.createRows(columns, 2);
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(columns));
		set.setTableId(table.getId());
		synapse.appendRowsToTable(set);
		// Query for the results
		RowSet queryResults = waitForQueryResults("select * from " + table.getId(), 0L, 2L);
		// Change the data
		for(Row row: queryResults.getRows()){
			String oldValue = row.getValues().get(0);
			row.setValues(Arrays.asList(oldValue+" changed"));
		}
		// Apply the changes
		synapse.appendRowsToTable(queryResults);
		
		// If we try to apply the same change again we should get a conflict
		try{
			synapse.appendRowsToTable(queryResults);
			fail("Should not be able to apply the same change twice.  It should result in a SynapseConflictingUpdateException update exception.");
		}catch(SynapseConflictingUpdateException e){
			// expected
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().contains("Row id:"));
			assertTrue(e.getMessage().contains("has been changed"));
		}
	}
	
	@Test
	public void testUpdate() throws Exception {
		// Create a column to add to a table entity
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.STRING);
		one = synapse.createColumnModel(one);
		List<ColumnModel> columns = Arrays.asList(one);

		TableEntity table = createTable(Lists.newArrayList(one.getId()));

		RowSet set = new RowSet();
		List<Row> rows = Lists.newArrayList(TableModelTestUtils.createRow(null, null, "test"),
				TableModelTestUtils.createRow(null, null, "test"));
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(columns));
		set.setTableId(table.getId());
		synapse.appendRowsToTable(set);

		PartialRowSet partialSet = new PartialRowSet();
		List<PartialRow> partialRows = Lists.newArrayList(TableModelTestUtils.createPartialRow(null, one.getId(), "test"),
				TableModelTestUtils.createPartialRow(null, one.getId(), "test"));
		partialSet.setRows(partialRows);
		partialSet.setTableId(table.getId());
		synapse.appendPartialRowsToTable(partialSet);

		// Query for the results
		RowSet queryResults = waitForQueryResults("select * from " + table.getId() + " order by row_id asc", 0L, 2L);
		// Change the data
		for (Row row : queryResults.getRows()) {
			String oldValue = row.getValues().get(0);
			row.setValues(Arrays.asList(oldValue + " changed"));
		}
		// Apply the changes
		synapse.appendRowsToTable(queryResults);

		queryResults = waitForQueryResults("select * from " + table.getId() + " order by row_id asc", 2L, 2L);
		// Change the data
		partialRows = Lists.newArrayList();
		for (int i = 0; i < 2; i++) {
			PartialRow partialRow = new PartialRow();
			partialRow.setRowId(queryResults.getRows().get(i).getRowId());
			partialRow.setValues(Collections.singletonMap(one.getId(), queryResults.getRows().get(i).getValues().get(0) + " changed"));
			partialRows.add(partialRow);
		}
		// Apply the changes using partial
		partialSet.setRows(partialRows);
		partialSet.setTableId(table.getId());
		synapse.appendPartialRowsToTable(partialSet);

		queryResults = waitForQueryResults("select * from " + table.getId() + " order by row_id asc", null, null);
		// Check that the changed data is there
		assertEquals(4, queryResults.getRows().size());
		for (Row row : queryResults.getRows()) {
			String value = row.getValues().get(0);
			assertEquals("test changed", value);
		}
	}

	@Test
	public void testQueryAsync() throws Exception {
		int columnCount = 20;
		int stringSize = 1000;
		int rowsNeeded = 40;

		// Create a few columns to add to a table entity
		List<String> columnIds = Lists.newArrayList();
		for (int i = 0; i < columnCount; i++) {
			ColumnModel one = new ColumnModel();
			one.setName("col" + i);
			one.setColumnType(ColumnType.STRING);
			one.setMaximumSize((long) stringSize);
			one = synapse.createColumnModel(one);
			columnIds.add(one.getId());
		}

		TableEntity table = createTable(columnIds);

		List<ColumnModel> columns = synapse.getColumnModelsForTableEntity(table.getId());

		String[] data = new String[columnCount];
		for (int i = 0; i < columnCount; i++) {
			data[i] = "x";
		}
		// Append some rows
		RowSet set = new RowSet();
		List<Row> rows = Lists.newArrayList();
		for (int i = 0; i < 10; i++) {
			rows.add(TableModelTestUtils.createRow(null, null, data));
		}
		for (int i = 0; i < rowsNeeded; i += rows.size()) {
			set.setRows(rows);
			set.setHeaders(TableModelUtils.getHeaders(columns));
			set.setTableId(table.getId());
			synapse.appendRowsToTable(set);
		}

		final String asyncToken = synapse.queryTableEntityBundleAsyncStart("select * from " + table.getId(), null, null, true, 0xff);
		QueryResultBundle result = waitForAsync(new Callable<QueryResultBundle>() {
			@Override
			public QueryResultBundle call() throws Exception {
				return synapse.queryTableEntityBundleAsyncGet(asyncToken);
			}
		});
		assertEquals(result.getMaxRowsPerPage().intValue(), result.getQueryResult().getQueryResults().getRows().size());
		assertEquals(rowsNeeded, result.getQueryCount().intValue());
		assertNotNull(result.getQueryResult().getNextPageToken());

		final String nextPageAsyncToken = synapse.queryTableEntityNextPageAsyncStart(result.getQueryResult().getNextPageToken().getToken());
		QueryResult nextPageResult = waitForAsync(new Callable<QueryResult>() {
			@Override
			public QueryResult call() throws Exception {
				return synapse.queryTableEntityNextPageAsyncGet(nextPageAsyncToken);
			}
		});
		assertEquals(rowsNeeded - result.getMaxRowsPerPage().intValue(), nextPageResult.getQueryResults().getRows().size());
		assertNull(nextPageResult.getNextPageToken());
	}

	private TableEntity createTable(List<String> columns) throws SynapseException {
		// Create a project to contain it all
		Project project = new Project();
		project.setName(UUID.randomUUID().toString());
		project = synapse.createEntity(project);
		entitiesToDelete.add(project);

		// now create a table entity
		TableEntity table = new TableEntity();
		table.setName("Table");
		table.setColumnIds(columns);
		table.setParentId(project.getId());
		table = synapse.createEntity(table);
		tablesToDelete.add(table);
		return table;
	}

	private <T> T waitForAsync(final Callable<T> callable) throws Exception {
		return TimeUtils.waitFor(80000, 500, new Callable<Pair<Boolean, T>>() {
			@Override
			public Pair<Boolean, T> call() throws Exception {
				try {
					T result = callable.call();
					return Pair.create(true, result);
				} catch (SynapseResultNotReadyException e) {
					return Pair.<Boolean, T> create(false, null);
				}
			}
		});
	}
}
