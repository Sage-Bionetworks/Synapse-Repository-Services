package org.sagebionetworks;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseConflictingUpdateException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseResultNotReadyException;
import org.sagebionetworks.client.exceptions.UnknownSynapseServerException;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DataTypeResponse;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnModelPage;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.model.table.SnapshotResponse;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableFileHandleResults;
import org.sagebionetworks.repo.model.table.TableSchemaChangeRequest;
import org.sagebionetworks.repo.model.table.TableSchemaChangeResponse;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.TransformSqlWithFacetsRequest;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
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
	private static long MAX_APPEND_TIMEOUT = 30*1000;
	
	@BeforeAll
	public static void beforeClass() throws Exception {
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
	}
	
	@BeforeEach
	public void before() throws SynapseException{
		adminSynapse.clearAllLocks();
		entitiesToDelete = new LinkedList<Entity>();
		tablesToDelete = new ArrayList<TableEntity>();
	}
	
	@AfterEach
	public void after() throws Exception {
		for (Entity entity : tablesToDelete) {
			adminSynapse.deleteEntity(entity);
		}
		for (Entity entity : entitiesToDelete) {
			adminSynapse.deleteEntity(entity);
		}
		for (File tempFile : tempFiles) {
			tempFile.delete();
		}
	}
	
	@AfterAll
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
		String tableId = table.getId();
		
		// Set the table's type to sensitive (add with PLFM-5240)
		DataType dataType = DataType.SENSITIVE_DATA;
		DataTypeResponse typeResponse = synapse.changeEntitysDataType(tableId, dataType);
		assertNotNull(typeResponse);
		assertEquals(dataType, typeResponse.getDataType());
		
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
		set.setHeaders(TableModelUtils.getSelectColumns(columns));
		set.setTableId(table.getId());
		RowReferenceSet results1 = synapse.appendRowsToTable(set, MAX_APPEND_TIMEOUT, tableId);
		assertNotNull(results1);
		assertNotNull(results1.getRows());
		assertEquals(2, results1.getRows().size());
		assertEquals(table.getId(), results1.getTableId());
		assertEquals(TableModelUtils.getSelectColumns(columns), results1.getHeaders());
		
		// Now attempt to query for the table results
		RowSet queryResults = waitForQueryResults("select * from " + table.getId(), null, null, table.getId());
		assertNotNull(queryResults);
		assertNotNull(queryResults.getEtag());
		assertEquals(results1.getEtag(), queryResults.getEtag());
		assertEquals(table.getId(), queryResults.getTableId());
		assertNotNull(queryResults.getRows());
		assertEquals(2, queryResults.getRows().size());

		// Now use these results to update the table. By setting the row IDs to null, they should be treated as additions
		for(Row row: queryResults.getRows()){
			row.setRowId(null);
			row.setVersionNumber(null);
		}
		RowReferenceSet results2 = synapse.appendRowsToTable(queryResults, MAX_APPEND_TIMEOUT, tableId);
		assertNotNull(results2);
		assertNotNull(results2.getRows());
		// run the query again, but this time get the counts
		Long count = waitForCountResults("select * from " + table.getId(), tableId);
		assertNotNull(count);
		assertEquals(4L, count.longValue(), "There should be 4 rows in this table");

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
		count = waitForCountResults("select * from " + table.getId(), tableId);
		assertEquals(3L, count.longValue(), "There should be 3 rows in this table");
		
		// run a bundled query
		int mask = 0x1 | 0x2 | 0x4 | 0x8;
		QueryResultBundle bundle = waitForBundleQueryResults("select one from " + table.getId(), 0L, 2L, mask, tableId);
		assertNotNull(bundle);
		assertNotNull(bundle.getQueryResult().getQueryResults());
		assertNotNull(bundle.getQueryResult().getQueryResults().getEtag());
		assertEquals(table.getId(), bundle.getQueryResult().getQueryResults().getTableId());
		assertNotNull(bundle.getQueryResult().getQueryResults().getRows());
		assertEquals(2, bundle.getQueryResult().getQueryResults().getRows().size());
		assertEquals(new Long(3), bundle.getQueryCount(), "There should be 3 rows in this table");
		assertEquals(Arrays.asList(TableModelUtils.createSelectColumn(one)), bundle.getSelectColumns());
		assertNotNull(bundle.getMaxRowsPerPage());
		assertTrue(bundle.getMaxRowsPerPage() > 0);

		bundle = waitForBundleQueryResults("select one from " + table.getId(), 2L, 2L, mask, tableId);
		assertNotNull(bundle);
		assertNotNull(bundle.getQueryResult().getQueryResults());
		assertNotNull(bundle.getQueryResult().getQueryResults().getEtag());
		assertEquals(table.getId(), bundle.getQueryResult().getQueryResults().getTableId());
		assertNotNull(bundle.getQueryResult().getQueryResults().getRows());
		assertEquals(1, bundle.getQueryResult().getQueryResults().getRows().size());
		assertEquals(new Long(3), bundle.getQueryCount(), "There should be 3 rows in this table");
		assertEquals(Arrays.asList(TableModelUtils.createSelectColumn(one)), bundle.getSelectColumns());
		assertNotNull(bundle.getMaxRowsPerPage());
		assertTrue(bundle.getMaxRowsPerPage() > 0);
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
		set.setHeaders(TableModelUtils.getSelectColumns(columns));
		set.setTableId(table.getId());
		synapse.appendRowsToTable(set, MAX_APPEND_TIMEOUT, table.getId());

		// Now attempt to query for the table results
		Long count = waitForCountResults("select * from " + table.getId() + " where one > -2.2", table.getId());
		assertEquals(4L, count.longValue());
		count = waitForCountResults("select * from " + table.getId() + " where one > -1.3", table.getId());
		assertEquals(4L, count.longValue());
		count = waitForCountResults("select * from " + table.getId() + " where one > -.3", table.getId());
		assertEquals(3L, count.longValue());
		count = waitForCountResults("select * from " + table.getId() + " where one > -.1", table.getId());
		assertEquals(2L, count.longValue());
		count = waitForCountResults("select * from " + table.getId() + " where one > .1", table.getId());
		assertEquals(2L, count.longValue());
		count = waitForCountResults("select * from " + table.getId() + " where one > .3", table.getId());
		assertEquals(1L, count.longValue());
		count = waitForCountResults("select * from " + table.getId() + " where one > 1.3", table.getId());
		assertEquals(0L, count.longValue());
	}

	@Test
	public void testPermissionsFailure() throws Exception {
		// Create a few columns to add to a table entity
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.STRING);
		one = adminSynapse.createColumnModel(one);

		TableEntity table = createTable(Lists.newArrayList(one.getId()), adminSynapse);

		assertThrows(SynapseForbiddenException.class, () -> {
			// Now attempt to query for the table results
			waitForCountResults("select * from " + table.getId(), table.getId());
		});
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
			set.setHeaders(TableModelUtils.getSelectColumns(columns));
			set.setTableId(table.getId());
			synapse.appendRowsToTable(set, MAX_APPEND_TIMEOUT, table.getId());
		}

		// Now query for the table results
		Long count = waitForCountResults("select * from " + table.getId(), table.getId());
		assertEquals(40L, count.longValue());

		adminSynapse.rebuildTableCacheAndIndex(table.getId());
		final String asyncToken = synapse.queryTableEntityBundleAsyncStart("select * from " + table.getId(), null, null, true,
				SynapseClient.COUNT_PARTMASK, table.getId());
		try {
			synapse.queryTableEntityBundleAsyncGet(asyncToken, table.getId());
			fail("table should be invalid");
		} catch (SynapseResultNotReadyException e) {
		}
		final String tableId = table.getId();
		count = TimeUtils.waitFor(MAX_QUERY_TIMEOUT_MS, 500L, new Callable<Pair<Boolean, QueryResultBundle>>() {
			@Override
			public Pair<Boolean, QueryResultBundle> call() throws Exception {
				try {
					QueryResultBundle result = synapse.queryTableEntityBundleAsyncGet(asyncToken, tableId);
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
	public RowSet waitForQueryResults(String sql, Long offset, Long limit, String tableId) throws Exception {
		return waitForBundleQueryResults(sql, offset, limit, SynapseClient.QUERY_PARTMASK, tableId).getQueryResult().getQueryResults();
	}

	/**
	 * Wait for a count results.
	 * 
	 * @param sql
	 * @return
	 * @throws InterruptedException
	 * @throws SynapseException
	 */
	public long waitForCountResults(String sql, String tableId) throws Exception {
		return waitForBundleQueryResults(sql, null, null, SynapseClient.COUNT_PARTMASK, tableId).getQueryCount();
	}
	
	/**
	 * Wait for a consistent query results.
	 * 
	 * @param sql
	 * @return
	 * @throws InterruptedException
	 * @throws SynapseException
	 */
	public QueryResultBundle waitForBundleQueryResults(String sql, Long offset, Long limit, int partsMask, final String tableId) throws Exception {
		final String asyncToken = synapse.queryTableEntityBundleAsyncStart(sql, offset, limit, true, partsMask, tableId);
		return TimeUtils.waitFor(MAX_QUERY_TIMEOUT_MS, 500L, new Callable<Pair<Boolean, QueryResultBundle>>() {
			@Override
			public Pair<Boolean, QueryResultBundle> call() throws Exception {
				try {
					QueryResultBundle result = synapse.queryTableEntityBundleAsyncGet(asyncToken, tableId);
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

		FileHandle fileHandle = synapse.multipartUpload(tempFile, null, false, false);
		assertTrue(fileHandle instanceof S3FileHandle);

		// Append some rows
		RowSet set = new RowSet();
		List<Row> rows = Collections.singletonList(TableModelTestUtils.createRow(null, null, fileHandle.getId()));
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getSelectColumns(columns));
		set.setTableId(table.getId());
		RowReferenceSet results = synapse.appendRowsToTable(set, MAX_APPEND_TIMEOUT, table.getId());
		RowSet queryResults = waitForQueryResults("select * from " + table.getId(), null, null, table.getId());
		assertNotNull(queryResults);

		TableFileHandleResults fileHandles = synapse.getFileHandlesFromTable(results);
		assertEquals(fileHandle.getId(), fileHandles.getRows().get(0).getList().get(0).getId());

		URL url = synapse.getTableFileHandleTemporaryUrl(table.getId(), results.getRows().get(0), one.getId());
		assertTrue(url.toString().contains(((S3FileHandle) fileHandle).getKey()),
				"The temporary URL did not contain the expected file handle key");

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
	public void testgetDefaultColumnsForView() throws SynapseException{
		// test for the deprecated method.
		List<ColumnModel> defaults = synapse.getDefaultColumnsForView(ViewType.file);
		assertNotNull(defaults);
		assertTrue(defaults.size() > 1);
		ColumnModel cm = defaults.get(0);
		assertNotNull(cm);
		assertNotNull(cm.getName());
		assertNotNull(cm.getId());
	}
	
	@Test
	public void testgetDefaultColumnsForViewTypeMask() throws SynapseException{
		Long mask = ViewTypeMask.File.getMask();
		List<ColumnModel> defaults = synapse.getDefaultColumnsForView(mask);
		assertNotNull(defaults);
		assertTrue(defaults.size() > 1);
		ColumnModel cm = defaults.get(0);
		assertNotNull(cm);
		assertNotNull(cm.getName());
		assertNotNull(cm.getId());
	}
	
	@Test
	public void testEmtpyTableRoundTrip() throws Exception {
		TableEntity table = createTable(null);
		// Now attempt to query for the table results
		// This table has no rows and no columns
		RowSet queryResults = waitForQueryResults("select * from " + table.getId(), 0L, 2L, table.getId());
		assertNotNull(queryResults);
		assertNull(queryResults.getEtag());
		assertEquals(table.getId(), queryResults.getTableId());
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
		set.setHeaders(TableModelUtils.getSelectColumns(columns));
		set.setTableId(table.getId());
		synapse.appendRowsToTable(set, MAX_APPEND_TIMEOUT, table.getId());
		// Query for the results
		RowSet queryResults = waitForQueryResults("select * from " + table.getId(), 0L, 2L, table.getId());
		// Change the data
		for(Row row: queryResults.getRows()){
			String oldValue = row.getValues().get(0);
			row.setValues(Arrays.asList(oldValue+" changed"));
		}
		// Apply the changes
		synapse.appendRowsToTable(queryResults, MAX_APPEND_TIMEOUT, table.getId());
		
		// If we try to apply the same change again we should get a conflict
		try{
			synapse.appendRowsToTable(queryResults, MAX_APPEND_TIMEOUT, table.getId());
			fail("Should not be able to apply the same change twice.  It should result in a SynapseConflictingUpdateException update exception.");
		} catch (SynapseConflictingUpdateException e) {
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
		set.setHeaders(TableModelUtils.getSelectColumns(columns));
		set.setTableId(table.getId());
		synapse.appendRowsToTable(set, MAX_APPEND_TIMEOUT, table.getId());

		PartialRowSet partialSet = new PartialRowSet();
		List<PartialRow> partialRows = Lists.newArrayList(TableModelTestUtils.createPartialRow(null, one.getId(), "test"),
				TableModelTestUtils.createPartialRow(null, one.getId(), "test"));
		partialSet.setRows(partialRows);
		partialSet.setTableId(table.getId());
		synapse.appendRowsToTable(partialSet, MAX_APPEND_TIMEOUT, table.getId());

		// Query for the results
		RowSet queryResults = waitForQueryResults("select * from " + table.getId() + " order by row_id asc", 0L, 2L, table.getId());
		// Change the data
		for (Row row : queryResults.getRows()) {
			String oldValue = row.getValues().get(0);
			row.setValues(Arrays.asList(oldValue + " changed"));
		}
		// Apply the changes
		synapse.appendRowsToTable(queryResults, MAX_APPEND_TIMEOUT, table.getId());

		queryResults = waitForQueryResults("select * from " + table.getId() + " order by row_id asc", 2L, 2L, table.getId());
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
		synapse.appendRowsToTable(partialSet, MAX_APPEND_TIMEOUT, table.getId());

		queryResults = waitForQueryResults("select * from " + table.getId() + " order by row_id asc", null, null, table.getId());
		// Check that the changed data is there
		assertEquals(4, queryResults.getRows().size());
		for (Row row : queryResults.getRows()) {
			String value = row.getValues().get(0);
			assertEquals("test changed", value);
		}
	}
	
	/**
	 * Test an upload with a payload larger than the max size fails.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPLFM_4761() throws Exception {
		// Create a value that will be pushed to a table to exceed the max request size
		String sampleValue = "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
		long maxCharacters = sampleValue.length();
		long bytesPerRow = maxCharacters*3;
		long maxBytesPerRequest = 1024*1024*2;
		long maxRows = maxBytesPerRequest/bytesPerRow*10;
		// Create a column to add to a table entity
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.STRING);
		one.setMaximumSize(maxCharacters);
		one = synapse.createColumnModel(one);

		TableEntity table = createTable(Lists.newArrayList(one.getId()));

		// Add enough rows to exceed the maximum request size.
		PartialRowSet partialSet = new PartialRowSet();
		List<PartialRow> partialRows = new LinkedList<PartialRow>();
		for(long i=0; i<maxRows+10;i++) {
			PartialRow row = TableModelTestUtils.createPartialRow(null, one.getId(), sampleValue);
			partialRows.add(row);
		}
		partialSet.setRows(partialRows);
		partialSet.setTableId(table.getId());
		
		try {
			synapse.appendRowsToTable(partialSet, MAX_APPEND_TIMEOUT, table.getId());
		} catch (UnknownSynapseServerException e) {
			// this should result in a 413 "Payload Too Large"
			assertEquals(413, e.getStatusCode());
		}
	}

	@Test
	public void testQueryAsync() throws Exception {
		int columnCount = 16;
		int stringSize = 1000;
		int rowsNeeded = 40;

		// Create a few columns to add to a table entity
		List<String> columnIds = Lists.newArrayList();
		for (int i = 0; i < columnCount; i++) {
			ColumnModel one = new ColumnModel();
			one.setName("col-" + i);
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
			set.setHeaders(TableModelUtils.getSelectColumns(columns));
			set.setTableId(table.getId());
			synapse.appendRowsToTable(set, MAX_APPEND_TIMEOUT, table.getId());
		}

		final String tableId = table.getId();
		String queryString = "select * from " + table.getId();
		final String asyncToken = synapse.queryTableEntityBundleAsyncStart(queryString, null, null, true, 0xff, tableId);
		QueryResultBundle result = waitForAsync(new Callable<QueryResultBundle>() {
			@Override
			public QueryResultBundle call() throws Exception {
				return synapse.queryTableEntityBundleAsyncGet(asyncToken, tableId);
			}
		});
		
		assertTrue(result.getMaxRowsPerPage().intValue() >= result.getQueryResult().getQueryResults().getRows().size());
		assertEquals(rowsNeeded, result.getQueryCount().intValue());
		assertNotNull(result.getQueryResult().getNextPageToken());
		
		// Since the table has not changed running the same query again should return the same job id. See PLFM-3284.
		final String asyncToken2 = synapse.queryTableEntityBundleAsyncStart(queryString, null, null, true, 0xff, tableId);
		assertEquals(asyncToken, asyncToken2);

		final String nextPageAsyncToken = synapse.queryTableEntityNextPageAsyncStart(result.getQueryResult().getNextPageToken().getToken(), tableId);
		QueryResult nextPageResult = waitForAsync(new Callable<QueryResult>() {
			@Override
			public QueryResult call() throws Exception {
				return synapse.queryTableEntityNextPageAsyncGet(nextPageAsyncToken, tableId);
			}
		});
		assertEquals(rowsNeeded - result.getMaxRowsPerPage().intValue(), nextPageResult.getQueryResults().getRows().size());
		assertNull(nextPageResult.getNextPageToken());
	}
	
	@Test
	public void testTableTransaction() throws Exception{
		ColumnModel cm = new ColumnModel();
		cm.setName("aString");
		cm.setColumnType(ColumnType.STRING);
		cm.setMaximumSize(100L);
		cm = synapse.createColumnModel(cm);
		
		// create a table
		TableEntity table = createTable(null, synapse);
		
		TableSchemaChangeRequest request = new TableSchemaChangeRequest();
		ColumnChange change = new ColumnChange();
		change.setOldColumnId(null);
		change.setNewColumnId(cm.getId());
		request.setChanges(Lists.newArrayList(change));
		
		List<TableUpdateRequest> changes = new LinkedList();
		changes.add(request);
		
		final String jobId = synapse.startTableTransactionJob(changes, table.getId());
		final String tableId = table.getId();
		
		List<TableUpdateResponse> results = waitForAsync(new Callable<List<TableUpdateResponse>>() {
			@Override
			public List<TableUpdateResponse> call() throws Exception {
				return synapse.getTableTransactionJobResults(jobId, tableId);
			}
		});
		assertNotNull(results);
		assertEquals(1, results.size());
		assertTrue(results.get(0) instanceof TableSchemaChangeResponse);
		TableSchemaChangeResponse response = (TableSchemaChangeResponse) results.get(0);
		assertEquals(Lists.newArrayList(cm), response.getSchema());
	}
	
	@Test
	public void testCreateTableSnapshot() throws SynapseException {
		ColumnModel cm = new ColumnModel();
		cm.setName("aString");
		cm.setColumnType(ColumnType.STRING);
		cm.setMaximumSize(100L);
		cm = synapse.createColumnModel(cm);
		
		// create a table
		TableEntity table = createTable(Lists.newArrayList(cm.getId()), synapse);
		
		SnapshotRequest request = new SnapshotRequest();
		request.setSnapshotLabel("snapshot label");
		request.setSnapshotComment("snapshot comment");
		// call under test
		SnapshotResponse response = synapse.createTableSnapshot(table.getId(), request);
		assertNotNull(response);
		assertNotNull(response.getSnapshotVersionNumber());
		
		Entity version = synapse.getEntityByIdForVersion(table.getId(), response.getSnapshotVersionNumber());
		assertNotNull(version);
		assertTrue(version instanceof TableEntity);
		TableEntity tableVersion = (TableEntity) version;
		assertEquals(request.getSnapshotLabel(), tableVersion.getVersionLabel());
		assertEquals(request.getSnapshotComment(), tableVersion.getVersionComment());
	}
	
	
	@Test
	public void testGetPossibleColumnModelsForViewScope() throws Exception {
		assertTimeout(Duration.ofSeconds(60), () -> {
			// Create a project to contain it all
			Project project = new Project();
			project.setName(UUID.randomUUID().toString());
			project = synapse.createEntity(project);
			entitiesToDelete.add(project);
			// Add an entity
			Folder folder = new Folder();
			folder.setName(UUID.randomUUID().toString());
			folder.setParentId(project.getId());
			folder = synapse.createEntity(folder);
			Annotations annos = synapse.getAnnotationsV2(folder.getId());
			AnnotationsV2TestUtils.putAnnotations(annos, "keyA", "someValue", AnnotationsValueType.STRING);
			AnnotationsV2TestUtils.putAnnotations(annos, "keyB", "123456", AnnotationsValueType.STRING);
			AnnotationsV2TestUtils.putAnnotations(annos, "keyC", "45678", AnnotationsValueType.STRING);
			synapse.updateAnnotationsV2(folder.getId(), annos);

			// Now find the columns for this scope with mask
			ViewScope scope = new ViewScope();
			scope.setScope(Lists.newArrayList(project.getId()));
			scope.setViewTypeMask(ViewTypeMask.File.getMask());
			String nextPageToken = null;
			ColumnModelPage page = waitForColumnModelPage(scope, nextPageToken, 3);
			assertNotNull(page);
			assertNotNull(page.getResults());
			assertNull(page.getNextPageToken());
			assertEquals(3, page.getResults().size());

			// find the scope with the old type
			// Now find the columns for this scope
			scope = new ViewScope();
			scope.setScope(Lists.newArrayList(project.getId()));
			scope.setViewType(ViewType.file);
			nextPageToken = null;
			page = waitForColumnModelPage(scope, nextPageToken, 3);
			assertNotNull(page);
			assertNotNull(page.getResults());
			assertNull(page.getNextPageToken());
			assertEquals(3, page.getResults().size());

			// make another call with a next page token.
			long limit = 1;
			long offset = 1;
			nextPageToken = new NextPageToken(limit, offset).toToken();
			page = waitForColumnModelPage(scope, nextPageToken, 1);
			assertNotNull(page);
			assertNotNull(page.getResults());
			assertNotNull(page.getNextPageToken());
			assertEquals(1, page.getResults().size());
			ColumnModel cm = page.getResults().get(0);
			assertEquals("keyB", cm.getName());
		});
	}
	
	@Test
	public void tesSqlTransformRequest() throws SynapseException {
		TransformSqlWithFacetsRequest request = new TransformSqlWithFacetsRequest();
		request.setSqlToTransform("select * from syn123");
		FacetColumnRangeRequest facet = new FacetColumnRangeRequest();
		facet.setColumnName("foo");
		facet.setMax("100");
		facet.setMin("0");
		request.setSelectedFacets(Lists.newArrayList(facet));
		ColumnModel column = new ColumnModel();
		column.setName("foo");
		column.setFacetType(FacetType.range);
		column.setColumnType(ColumnType.INTEGER);
		request.setSchema(Lists.newArrayList(column));
		// Call under test
		String resultSql = synapse.transformSqlRequest(request);
		assertEquals("SELECT * FROM syn123 WHERE ( ( \"foo\" BETWEEN '0' AND '100' ) )", resultSql);
	}

	@Disabled("This test will not work because it has no mechanism to wait for the view to be consisted. Will be moved to the TableViewIntegrationTest PLFM-6043.")
	@Test
	public void testEntityView_multipleValueColumnRoundTrip() throws Exception {
		Project project = new Project();
		project.setName("testEntityView_multipleValueColumnRoundTrip" + UUID.randomUUID());

		project = synapse.createEntity(project);
		entitiesToDelete.add(project);

		String multiValueKey = "multiValueKey";

		// Add an entity
		Folder folder = new Folder();
		folder.setName(UUID.randomUUID().toString());
		folder.setParentId(project.getId());
		folder = synapse.createEntity(folder);
		Annotations annos = synapse.getAnnotationsV2(folder.getId());
		AnnotationsV2TestUtils.putAnnotations(annos, multiValueKey, Arrays.asList("val1", "val2"), AnnotationsValueType.STRING);
		synapse.updateAnnotationsV2(folder.getId(), annos);

		// Add another entity
		Folder folder2 = new Folder();
		folder2.setName(UUID.randomUUID().toString());
		folder2.setParentId(project.getId());
		folder2 = synapse.createEntity(folder2);
		Annotations annos2 = synapse.getAnnotationsV2(folder2.getId());
		AnnotationsV2TestUtils.putAnnotations(annos2, multiValueKey, Arrays.asList("val2", "val3"), AnnotationsValueType.STRING);
		synapse.updateAnnotationsV2(folder2.getId(), annos2);


		final long folderViewMask = 0x08L;

		//create schema for entity view
		ColumnModel listColumnModel = new ColumnModel();
		listColumnModel.setColumnType(ColumnType.STRING_LIST);
		listColumnModel.setName(multiValueKey);
		listColumnModel = synapse.createColumnModel(listColumnModel);
		List<ColumnModel> defaultViewColumns = synapse.getDefaultColumnsForView(folderViewMask);
		List<String> columnIds = new ArrayList<>(defaultViewColumns.size() + 1);
		for(ColumnModel cm : defaultViewColumns){
			columnIds.add(cm.getId());
		}
		columnIds.add(listColumnModel.getId());

		EntityView view = new EntityView();
		view.setParentId(project.getId());
		view.setName("ENTITY_VIEW_testEntityView_multipleValueColumnRoundTrip" + UUID.randomUUID());
		view.setScopeIds(Collections.singletonList(project.getId()));
		view.setViewTypeMask(folderViewMask);
		view.setColumnIds(columnIds);
		view = synapse.createEntity(view);

		//2 total rows should be in view
		assertEquals(2, waitForCountResults("select * from "+ view.getId(), view.getId()));

		//only 1 annotation has "val1" as a value
		assertEquals(1, waitForCountResults("select * from "+ view.getId() + " where multiValueKey HAS ('val1')", view.getId()));
		//both annotations have "val2" as a value
		assertEquals(2, waitForCountResults("select * from "+ view.getId() + " where multiValueKey HAS ('val2')", view.getId()));
		//HAS "val1" or "val3" should also cover both values
		assertEquals(2, waitForCountResults("select * from "+ view.getId() + " where multiValueKey HAS ('val1', 'val3')", view.getId()));

		//modify annotation values by using updates to table view
		RowSet result = waitForQueryResults("select * from "+ view.getId(), 0L, 10L,view.getId());
		//still expecting 2 rows
		assertEquals(2, result.getRows().size());

		//find index of "multiValueKey" column
		int multiValueKeyIndex = -1;
		int idKeyIndex = -1;
		List<SelectColumn> headers = result.getHeaders();
		for(int i = 0; i < headers.size(); i++){
			if (headers.get(i).getName().equals(multiValueKey)){
				multiValueKeyIndex = i;
			}else if (headers.get(i).getName().equalsIgnoreCase("id")){
				idKeyIndex = i;
			}
		}
		assertNotEquals(-1, multiValueKeyIndex, "the multiValueKey column is not included in the query results");

		//change multiValueKey to new values
		String firstChangeId = result.getRows().get(0).getValues().get(idKeyIndex);
		result.getRows().get(0).getValues().set(multiValueKeyIndex, "[\"newVal1\", \"newVal2\"]");
		String secondChangeId = result.getRows().get(1).getValues().get(idKeyIndex);
		result.getRows().get(1).getValues().set(multiValueKeyIndex, "[\"newVal4\", \"newVal5\", \"newVal6\"]");
		//push modified values to view
		synapse.appendRowSetToTableStart(result, view.getId());

		long startTime = System.currentTimeMillis();
		
		//check view is updated
		do {
			if(System.currentTimeMillis() - startTime >= MAX_QUERY_TIMEOUT_MS){
				fail("Timed out waiting for query results to be consistent");
			}
			result = waitForQueryResults("select * from "+ view.getId(), 0L, 10L, view.getId());
		} while(!result.getRows().get(0).getValues().get(multiValueKeyIndex).equals("[\"newVal1\", \"newVal2\"]")
		 && !result.getRows().get(1).getValues().get(multiValueKeyIndex).equals("[\"newVal4\", \"newVal5\", \"newVal6\"]")
		);

		//check Annotations on entities are updated
		assertEquals(Arrays.asList("newVal1", "newVal2"), synapse.getAnnotationsV2(firstChangeId).getAnnotations().get(multiValueKey).getValue());
		assertEquals(Arrays.asList("newVal4", "newVal5", "newVal6"), synapse.getAnnotationsV2(secondChangeId).getAnnotations().get(multiValueKey).getValue());
	}
	
	private TableEntity createTable(List<String> columns) throws SynapseException {
		return createTable(columns, synapse);
	}

	private TableEntity createTable(List<String> columns, SynapseClient synapse) throws SynapseException {
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
	
	/**
	 * Wait for the expected number of ColumnModels to found for a given scope.
	 * @param scope
	 * @param nextPageToken
	 * @param expectedCount
	 * @return
	 * @throws SynapseException
	 * @throws InterruptedException
	 */
	private ColumnModelPage waitForColumnModelPage(ViewScope scope, String nextPageToken, int expectedCount) throws SynapseException, InterruptedException{
		while(true){
			ColumnModelPage page = synapse.getPossibleColumnModelsForViewScope(scope, nextPageToken);
			if(page.getResults().size() >= expectedCount){
				return page;
			}
			System.out.println("Wait for entity replication...");
			Thread.sleep(2000);
		}
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
