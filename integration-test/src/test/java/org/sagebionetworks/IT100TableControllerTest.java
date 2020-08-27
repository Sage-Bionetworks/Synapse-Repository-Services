package org.sagebionetworks;


import com.google.common.collect.Lists;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.AsyncJobHelper.AsyncJobResponse;
import org.sagebionetworks.client.AsynchJobType;
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
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.model.table.SnapshotResponse;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableFileHandleResults;
import org.sagebionetworks.repo.model.table.TableSchemaChangeRequest;
import org.sagebionetworks.repo.model.table.TableSchemaChangeResponse;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.repo.model.table.TransformSqlWithFacetsRequest;
import org.sagebionetworks.repo.model.table.ViewColumnModelRequest;
import org.sagebionetworks.repo.model.table.ViewColumnModelResponse;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for TableEntity and ColumnModel services.
 * 
 * @author jmhill
 *
 */
public class IT100TableControllerTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userId;

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
		userId = SynapseClientHelper.createUser(adminSynapse, synapse);

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
		// This means proper cleanup was not done by the test 
		try {
			adminSynapse.deleteUser(userId);
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
		ColumnModel newColumnModel = new ColumnModel();
		newColumnModel.setName("one");
		newColumnModel.setColumnType(ColumnType.STRING);
		
		final ColumnModel one = synapse.createColumnModel(newColumnModel);
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
		
		Consumer<RowSet> rowSetConsumer = (queryResults) -> {
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
		};
		
		// Now attempt to query for the table results
		RowSet queryResults = assertQueryResults("select * from " + table.getId(), null, null, table.getId(), rowSetConsumer);
		
		RowReferenceSet results2 = synapse.appendRowsToTable(queryResults, MAX_APPEND_TIMEOUT, tableId);
		assertNotNull(results2);
		assertNotNull(results2.getRows());
		
		// run the query again, but this time get the counts
		assertCountResults("select * from " + table.getId(), tableId, 4L);
		
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
		assertCountResults("select * from " + table.getId(), tableId, 3L);
		
		// run a bundled query
		int mask = 0x1 | 0x2 | 0x4 | 0x8;
		
		Consumer<QueryResultBundle> resultConsumer = (bundle) -> {
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
		};
		
		AsyncJobHelper.assertQueryBundleResults(synapse, tableId, "select one from " + table.getId(), 0L, 2L, mask, resultConsumer, MAX_QUERY_TIMEOUT_MS);

		resultConsumer = (bundle) -> {
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
		};
		
		AsyncJobHelper.assertQueryBundleResults(synapse, tableId, "select one from " + table.getId(), 2L, 2L, mask, resultConsumer, MAX_QUERY_TIMEOUT_MS);
		
	}

	@Test
	public void testCurrentUserFunction() throws Exception{
		// Create a few columns to add to a table entity
		ColumnModel one = new ColumnModel();
		one.setName("userId");
		one.setColumnType(ColumnType.DOUBLE);
		one = synapse.createColumnModel(one);

		TableEntity table = createTable(Lists.newArrayList(one.getId()));

		List<ColumnModel> columns = synapse.getColumnModelsForTableEntity(table.getId());

		// Append some rows
		RowSet set = new RowSet();
		List<Row> rows = Lists.newArrayList(TableModelTestUtils.createRow(null, null, userId.toString()),
				TableModelTestUtils.createRow(null, null, "2"),
				TableModelTestUtils.createRow(null, null, "3"),
				TableModelTestUtils.createRow(null, null, "4"));
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getSelectColumns(columns));
		set.setTableId(table.getId());
		synapse.appendRowsToTable(set, MAX_APPEND_TIMEOUT, table.getId());

		assertCountResults("select userId from " + table.getId() + " where userId = CURRENT_USER()", table.getId(), 1L);
		assertQueryResults("select userId from " + table.getId() + " where userId = CURRENT_USER()", null, null, table.getId(), (queryResults) -> {
			assertEquals(1, queryResults.getRows().size());
			assertEquals(1, queryResults.getRows().get(0).getValues().size());
			assertEquals(userId.toString(), queryResults.getRows().get(0).getValues().get(0));
		});
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
		assertCountResults("select * from " + table.getId() + " where one > -2.2", table.getId(), 4L);
		assertCountResults("select * from " + table.getId() + " where one > -1.3", table.getId(), 4L);
		assertCountResults("select * from " + table.getId() + " where one > -.3", table.getId(), 3L);
		assertCountResults("select * from " + table.getId() + " where one > -.1", table.getId(), 2L);
		assertCountResults("select * from " + table.getId() + " where one > .1", table.getId(), 2L);
		assertCountResults("select * from " + table.getId() + " where one > .3", table.getId(), 1L);
		assertCountResults("select * from " + table.getId() + " where one > 1.3", table.getId(), 0L);
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
			assertCountResults("select * from " + table.getId(), table.getId(), 0L);
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
		assertCountResults("select * from " + table.getId(), table.getId(), 40L);
		
		adminSynapse.rebuildTableCacheAndIndex(table.getId());
		
		final String asyncToken = synapse.queryTableEntityBundleAsyncStart("select * from " + table.getId(), null, null,
				SynapseClient.COUNT_PARTMASK, table.getId());
		
		assertThrows(SynapseResultNotReadyException.class, () -> {
			synapse.queryTableEntityBundleAsyncGet(asyncToken, table.getId());
		});
		
		// Now query for the table results
		assertCountResults("select * from " + table.getId(), table.getId(), 40L);
	}

	/**
	 * Wait for a query results.
	 * 
	 * @param sql
	 * @return
	 * @throws Exception
	 */
	public RowSet assertQueryResults(String sql, Long offset, Long limit, String tableId, Consumer<RowSet> rowSetConsumer) throws Exception {
		
		Consumer<QueryResultBundle> resultConsumer = (bundle) -> {
			rowSetConsumer.accept(bundle.getQueryResult().getQueryResults());
		};
		
		return AsyncJobHelper.assertQueryBundleResults(synapse, tableId, sql, offset, limit, SynapseClient.QUERY_PARTMASK, resultConsumer, MAX_QUERY_TIMEOUT_MS)
				.getResponse().getQueryResult().getQueryResults();
	
	}

	/**
	 * Wait for a count results.
	 * 
	 * @param sql
	 * @return
	 * @throws InterruptedException
	 * @throws SynapseException
	 */
	public void assertCountResults(String sql, String tableId, long count) throws Exception {
		
		Consumer<QueryResultBundle> resultConsumer = (bundle) -> {
			assertEquals(count, bundle.getQueryCount(), "There should be " + count + " rows in this table");
		};
		
		AsyncJobHelper.assertQueryBundleResults(synapse, tableId, sql, null, null, SynapseClient.COUNT_PARTMASK, resultConsumer, MAX_QUERY_TIMEOUT_MS);
		
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
		
		assertQueryResults("select * from " + table.getId(), null, null, table.getId(), (queryResults) -> {			
			assertNotNull(queryResults);
		});

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
	public void testGetDefaultColumnsForView() throws SynapseException{
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
		ViewEntityType viewEntityType = ViewEntityType.entityview;
		Long mask = ViewTypeMask.File.getMask();
		List<ColumnModel> defaults = synapse.getDefaultColumnsForView(viewEntityType, mask);
		assertNotNull(defaults);
		assertTrue(defaults.size() > 1);
		ColumnModel cm = defaults.get(0);
		assertNotNull(cm);
		assertNotNull(cm.getName());
		assertNotNull(cm.getId());
	}
	
	@Test
	public void testGetDefaultColumnsForSubmissionView() throws SynapseException{
		ViewEntityType viewEntityType = ViewEntityType.submissionview;
		Long mask = null;
		List<ColumnModel> defaults = synapse.getDefaultColumnsForView(viewEntityType, mask);
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
		assertQueryResults("select * from " + table.getId(), 0L, 2L, table.getId(), (queryResults) -> {			
			assertNotNull(queryResults);
			assertNull(queryResults.getEtag());
			assertEquals(table.getId(), queryResults.getTableId());
		});
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
		RowSet queryResults = assertQueryResults("select * from " + table.getId(), 0L, 2L, table.getId(), (rowSet) -> {
			assertNotNull(rowSet);
		});
		// Change the data
		for(Row row: queryResults.getRows()){
			String oldValue = row.getValues().get(0);
			row.setValues(Arrays.asList(oldValue+" changed"));
		}
		// Apply the changes
		synapse.appendRowsToTable(queryResults, MAX_APPEND_TIMEOUT, table.getId());
		
		// If we try to apply the same change again we should get a conflict
		SynapseConflictingUpdateException e = assertThrows(SynapseConflictingUpdateException.class, () -> {
			synapse.appendRowsToTable(queryResults, MAX_APPEND_TIMEOUT, table.getId());
		}, "Should not be able to apply the same change twice.  It should result in a SynapseConflictingUpdateException update exception.");

		// expected
		System.out.println(e.getMessage());
		assertTrue(e.getMessage().contains("Row id:"));
		assertTrue(e.getMessage().contains("has been changed"));
		
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
		RowSet queryResults = assertQueryResults("select * from " + table.getId() + " order by row_id asc", 0L, 2L, table.getId(), (rowSet) -> {
			assertNotNull(rows);
		});
		
		// Change the data
		for (Row row : queryResults.getRows()) {
			String oldValue = row.getValues().get(0);
			row.setValues(Arrays.asList(oldValue + " changed"));
		}
		// Apply the changes
		synapse.appendRowsToTable(queryResults, MAX_APPEND_TIMEOUT, table.getId());

		queryResults = assertQueryResults("select * from " + table.getId() + " order by row_id asc", 2L, 2L, table.getId(), (rowSet) -> {
			assertNotNull(rows);
		});
		
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

		assertQueryResults("select * from " + table.getId() + " order by row_id asc", null, null, table.getId(), (newQueryResults) -> {
			// Check that the changed data is there
			assertEquals(4, newQueryResults.getRows().size());
			for (Row row : newQueryResults.getRows()) {
				String value = row.getValues().get(0);
				assertEquals("test changed", value);
			}	
		});
		
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
		
		AsyncJobResponse<QueryResultBundle> jobResponse = AsyncJobHelper.assertQueryBundleResults(synapse, tableId, queryString, null, null, 0xff, (result) -> {
		 	assertTrue(result.getMaxRowsPerPage().intValue() >= result.getQueryResult().getQueryResults().getRows().size());
			assertEquals(rowsNeeded, result.getQueryCount().intValue());
			assertNotNull(result.getQueryResult().getNextPageToken());	
		}, MAX_QUERY_TIMEOUT_MS);
		
		// Since the table has not changed running the same query again should return the same job id. See PLFM-3284.
		final String asyncToken2 = synapse.queryTableEntityBundleAsyncStart(queryString, null, null, 0xff, tableId);
		
		assertEquals(jobResponse.getJobToken(), asyncToken2);		
		
		QueryResultBundle queryResult = jobResponse.getResponse();
		QueryNextPageToken nextPageToken = queryResult.getQueryResult().getNextPageToken();
		nextPageToken.setEntityId(tableId);
		
		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.TableQueryNextPage, nextPageToken, (QueryResult nextPageResult) -> {
			assertEquals(rowsNeeded - queryResult.getMaxRowsPerPage().intValue(), nextPageResult.getQueryResults().getRows().size());
			assertNull(nextPageResult.getNextPageToken());
		}, MAX_QUERY_TIMEOUT_MS);

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
		
		TableSchemaChangeRequest schemaChangeRequest = new TableSchemaChangeRequest();
		
		ColumnChange change = new ColumnChange();
		change.setOldColumnId(null);
		change.setNewColumnId(cm.getId());
		
		schemaChangeRequest.setChanges(Collections.singletonList(change));
		
		List<TableUpdateRequest> changes = Collections.singletonList(schemaChangeRequest);
		
		TableUpdateTransactionRequest request = new TableUpdateTransactionRequest();
		
		request.setEntityId(table.getId());
		request.setChanges(changes);
		
		List<ColumnModel> expectedSchema = Collections.singletonList(cm);
		
		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.TableTransaction, request, (TableUpdateTransactionResponse response) -> {
			List<TableUpdateResponse> results = response.getResults();
			assertNotNull(results);
			assertEquals(1, results.size());
			assertTrue(results.get(0) instanceof TableSchemaChangeResponse);
			TableSchemaChangeResponse schemaChange = (TableSchemaChangeResponse) results.get(0);
			assertEquals(expectedSchema, schemaChange.getSchema());
		}, MAX_APPEND_TIMEOUT);
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
		scope.setViewTypeMask(ViewTypeMask.Folder.getMask());
		String nextPageToken = null;
		
		assertColumnModelPage(scope, nextPageToken, (response) -> {
			assertNotNull(response);
			assertNotNull(response.getResults());
			assertNull(response.getNextPageToken());
			assertEquals(3, response.getResults().size());
		});
		
		// make another call with a next page token.
		long limit = 1;
		long offset = 1;
		nextPageToken = new NextPageToken(limit, offset).toToken();
		
		assertColumnModelPage(scope, nextPageToken, (response) -> {
			assertNotNull(response);
			assertNotNull(response.getResults());
			assertNotNull(response.getNextPageToken());
			assertEquals(1, response.getResults().size());
			ColumnModel cm = response.getResults().get(0);
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
	
	private static void assertColumnModelPage(ViewScope scope, String nextPageToken, Consumer<ViewColumnModelResponse> responseConsumer) throws Exception {
		ViewColumnModelRequest request = new ViewColumnModelRequest();
		request.setViewScope(scope);
		request.setNextPageToken(nextPageToken);
		
		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.ViewColumnModelRequest, request, responseConsumer, MAX_QUERY_TIMEOUT_MS, AsyncJobHelper.INFINITE_RETRIES);
	}
}
