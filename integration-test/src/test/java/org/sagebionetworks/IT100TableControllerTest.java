package org.sagebionetworks;

import static org.junit.Assert.*;

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
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseConflictingUpdateException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseTableUnavailableException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableFileHandleResults;
import org.sagebionetworks.repo.model.table.TableState;

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
	public void testCreateTableEntity() throws SynapseException, InterruptedException{
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
		TableEntity table = new TableEntity();
		table.setName("Table");
		List<String> idList = new LinkedList<String>();
		idList.add(one.getId());
		idList.add(two.getId());
		table.setColumnIds(idList);
		table.setParentId(project.getId());
		table = synapse.createEntity(table);
		tablesToDelete.add(table);
		
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
		boolean isConsistent = true;
		boolean countOnly = false;
		RowSet queryResults = waitForQueryResults("select * from "+table.getId()+" limit 100", isConsistent, countOnly);
		assertNotNull(queryResults);
		assertNotNull(queryResults.getEtag());
		assertEquals(results1.getEtag(), queryResults.getEtag());
		assertEquals(table.getId(), queryResults.getTableId());
		assertNotNull(queryResults.getRows());
		assertEquals(2, queryResults.getRows().size());

		try {
			waitForQueryResults("select * from " + table.getId() + " where one = 'x'", isConsistent,
					countOnly);
			fail("Should have failed due to missing LIMIT");
		} catch (SynapseBadRequestException e) {
		}

		// get the rows direct
		RowSet directResults = synapse.getRowsFromTable(results1);
		// etag not set in direct case
		directResults.setEtag(queryResults.getEtag());
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
		countOnly = true;
		queryResults = waitForQueryResults("select * from "+table.getId()+" limit 2", isConsistent, countOnly);
		assertNotNull(queryResults);
		assertNotNull(queryResults.getEtag());
		assertEquals(results2.getEtag(), queryResults.getEtag());
		assertEquals(table.getId(), queryResults.getTableId());
		assertNotNull(queryResults.getRows());
		assertEquals(1, queryResults.getRows().size());
		Row onlyRow = queryResults.getRows().get(0);
		assertNotNull(onlyRow.getValues());
		assertEquals(1, onlyRow.getValues().size());
		assertEquals("There should be 4 rows in this table", "4", onlyRow.getValues().get(0));

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
		countOnly = true;
		queryResults = waitForQueryResults("select * from " + table.getId() + " limit 2", isConsistent, countOnly);
		assertNotNull(queryResults);
		assertNotNull(queryResults.getEtag());
		assertEquals(table.getId(), queryResults.getTableId());
		assertNotNull(queryResults.getRows());
		assertEquals(1, queryResults.getRows().size());
		onlyRow = queryResults.getRows().get(0);
		assertNotNull(onlyRow.getValues());
		assertEquals(1, onlyRow.getValues().size());
		assertEquals("There should be 3 rows in this table", "3", onlyRow.getValues().get(0));
		
		// run a bundled query
		isConsistent = true;
		int mask = 0x1 | 0x2 | 0x4 | 0x8;
		QueryResultBundle bundle = waitForBundleQueryResults("select one from " + table.getId() + " limit 2", isConsistent, mask);
		assertNotNull(bundle);
		assertNotNull(bundle.getQueryResults());
		assertNotNull(bundle.getQueryResults().getEtag());
		assertEquals(table.getId(), bundle.getQueryResults().getTableId());
		assertNotNull(bundle.getQueryResults().getRows());
		assertEquals(2, bundle.getQueryResults().getRows().size());
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
		Project project = new Project();
		project.setName(UUID.randomUUID().toString());
		project = synapse.createEntity(project);
		assertNotNull(project);
		entitiesToDelete.add(project);

		// now create a table entity
		TableEntity table = new TableEntity();
		table.setName("Table");
		table.setColumnIds(idList);
		table.setParentId(project.getId());
		table = synapse.createEntity(table);
		tablesToDelete.add(table);

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
	public void testQueryDoubles() throws SynapseException, InterruptedException {
		// Create a few columns to add to a table entity
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.DOUBLE);
		one = synapse.createColumnModel(one);

		// Create a project to contain it all
		Project project = new Project();
		project.setName(UUID.randomUUID().toString());
		project = synapse.createEntity(project);
		assertNotNull(project);
		entitiesToDelete.add(project);

		// now create a table entity
		TableEntity table = new TableEntity();
		table.setName("Table");
		table.setColumnIds(Lists.newArrayList(one.getId()));
		table.setParentId(project.getId());
		table = synapse.createEntity(table);
		tablesToDelete.add(table);

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
		boolean isConsistent = true;
		boolean countOnly = true;
		RowSet queryResults = waitForQueryResults("select * from " + table.getId() + " where one > -2.2 limit 100", isConsistent, countOnly);
		assertEquals("4", queryResults.getRows().get(0).getValues().get(0));
		queryResults = waitForQueryResults("select * from " + table.getId() + " where one > -1.3 limit 100", isConsistent, countOnly);
		assertEquals("4", queryResults.getRows().get(0).getValues().get(0));
		queryResults = waitForQueryResults("select * from " + table.getId() + " where one > -.3 limit 100", isConsistent, countOnly);
		assertEquals("3", queryResults.getRows().get(0).getValues().get(0));
		queryResults = waitForQueryResults("select * from " + table.getId() + " where one > -.1 limit 100", isConsistent, countOnly);
		assertEquals("2", queryResults.getRows().get(0).getValues().get(0));
		queryResults = waitForQueryResults("select * from " + table.getId() + " where one > .1 limit 100", isConsistent, countOnly);
		assertEquals("2", queryResults.getRows().get(0).getValues().get(0));
		queryResults = waitForQueryResults("select * from " + table.getId() + " where one > .3 limit 100", isConsistent, countOnly);
		assertEquals("1", queryResults.getRows().get(0).getValues().get(0));
		queryResults = waitForQueryResults("select * from " + table.getId() + " where one > 1.3 limit 100", isConsistent, countOnly);
		assertEquals("0", queryResults.getRows().get(0).getValues().get(0));
	}

	/**
	 * Wait for a consistent query results.
	 * 
	 * @param sql
	 * @return
	 * @throws InterruptedException
	 * @throws SynapseException
	 */
	public RowSet waitForQueryResults(String sql, boolean isConsistent, boolean countOnly) throws InterruptedException, SynapseException{
		long start = System.currentTimeMillis();
		while(true){
			try {
				RowSet queryResutls = synapse.queryTableEntity(sql, isConsistent, countOnly);
				return queryResutls;
			} catch (SynapseTableUnavailableException e) {
				// The table is not ready yet
				assertFalse("Table processing failed: "+e.getStatus().getErrorMessage(), TableState.PROCESSING_FAILED.equals(e.getStatus().getState()));
				System.out.println("Waiting for table index to be available: "+e.getStatus());
				Thread.sleep(2000);
				assertTrue("Timed out waiting for query results for sql: "+sql,System.currentTimeMillis()-start < MAX_QUERY_TIMEOUT_MS);
			}
		}
	}
	
	/**
	 * Wait for a consistent query results.
	 * 
	 * @param sql
	 * @return
	 * @throws InterruptedException
	 * @throws SynapseException
	 */
	public QueryResultBundle waitForBundleQueryResults(String sql, boolean isConsistent, int partsMask) throws InterruptedException, SynapseException{
		long start = System.currentTimeMillis();
		while(true){
			try {
				QueryResultBundle queryResutls = synapse.queryTableEntityBundle(sql, isConsistent, partsMask);
				return queryResutls;
			} catch (SynapseTableUnavailableException e) {
				// The table is not ready yet
				assertFalse("Table processing failed: "+e.getStatus().getErrorMessage(), TableState.PROCESSING_FAILED.equals(e.getStatus().getState()));
				System.out.println("Waiting for table index to be available: "+e.getStatus());
				Thread.sleep(2000);
				assertTrue("Timed out waiting for query results for sql: "+sql,System.currentTimeMillis()-start < MAX_QUERY_TIMEOUT_MS);
			}
		}
	}

	@Test
	public void testAddRetrieveFileHandle() throws Exception {
		// Create a few columns to add to a table entity
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.FILEHANDLEID);
		one = synapse.createColumnModel(one);
		// Create a project to contain it all
		Project project = new Project();
		project.setName(UUID.randomUUID().toString());
		project = synapse.createEntity(project);
		entitiesToDelete.add(project);

		// now create a table entity
		TableEntity table = new TableEntity();
		table.setName("Table");
		List<String> idList = Lists.newArrayList(one.getId());
		table.setColumnIds(idList);
		table.setParentId(project.getId());
		table = synapse.createEntity(table);
		tablesToDelete.add(table);

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
	public void testEmtpyTableRoundTrip() throws SynapseException, InterruptedException{
		// Create a project to contain it all
		Project project = new Project();
		project.setName(UUID.randomUUID().toString());
		project = synapse.createEntity(project);
		assertNotNull(project);
		entitiesToDelete.add(project);
		// now create a table entity
		TableEntity table = new TableEntity();
		table.setName("Table");
		table.setColumnIds(null);
		table.setParentId(project.getId());
		table = synapse.createEntity(table);
		tablesToDelete.add(table);
		// Now attempt to query for the table results
		boolean isConsistent = true;
		boolean countOnly = false;
		// This table has no rows and no columns
		RowSet queryResults = waitForQueryResults("select * from "+table.getId()+" limit 2", isConsistent, countOnly);
		assertNotNull(queryResults);
		assertNull(queryResults.getEtag());
		assertEquals(table.getId(), queryResults.getTableId());
		assertNull(queryResults.getRows());
		assertNull(queryResults.getHeaders());
	}
	
	@Test
	public void testConflictingResultS() throws SynapseException, InterruptedException{
		// Create a few columns to add to a table entity
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.STRING);
		one = synapse.createColumnModel(one);
		List<ColumnModel> columns = Arrays.asList(one);
		// Create a project to contain it all
		Project project = new Project();
		project.setName(UUID.randomUUID().toString());
		project = synapse.createEntity(project);
		entitiesToDelete.add(project);

		// now create a table entity
		TableEntity table = new TableEntity();
		table.setName("Table");
		List<String> idList = Lists.newArrayList(one.getId());
		table.setColumnIds(idList);
		table.setParentId(project.getId());
		table = synapse.createEntity(table);
		tablesToDelete.add(table);
		
		RowSet set = new RowSet();
		List<Row> rows = TableModelTestUtils.createRows(columns, 2);
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(columns));
		set.setTableId(table.getId());
		synapse.appendRowsToTable(set);
		// Query for the results
		boolean isConsistent = true;
		boolean countOnly = false;
		RowSet queryResults = waitForQueryResults("select * from "+table.getId()+" limit 2", isConsistent, countOnly);
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
			assertTrue(e.getMessage().contains("has been changes"));
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
		// Create a project to contain it all
		Project project = new Project();
		project.setName(UUID.randomUUID().toString());
		project = synapse.createEntity(project);
		entitiesToDelete.add(project);

		// now create a table entity
		TableEntity table = new TableEntity();
		table.setName("Table");
		List<String> idList = Lists.newArrayList(one.getId());
		table.setColumnIds(idList);
		table.setParentId(project.getId());
		table = synapse.createEntity(table);
		tablesToDelete.add(table);

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
		boolean isConsistent = true;
		boolean countOnly = false;
		RowSet queryResults = waitForQueryResults("select * from " + table.getId() + " order by row_id asc limit 2 offset 0", isConsistent,
				countOnly);
		// Change the data
		for (Row row : queryResults.getRows()) {
			String oldValue = row.getValues().get(0);
			row.setValues(Arrays.asList(oldValue + " changed"));
		}
		// Apply the changes
		synapse.appendRowsToTable(queryResults);

		queryResults = waitForQueryResults("select * from " + table.getId() + " order by row_id asc limit 2 offset 2", isConsistent,
				countOnly);
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

		queryResults = waitForQueryResults("select * from " + table.getId() + " order by row_id asc limit 200", isConsistent,
				countOnly);
		// Check that the changed data is there
		assertEquals(4, queryResults.getRows().size());
		for (Row row : queryResults.getRows()) {
			String value = row.getValues().get(0);
			assertEquals("test changed", value);
		}
	}
}
