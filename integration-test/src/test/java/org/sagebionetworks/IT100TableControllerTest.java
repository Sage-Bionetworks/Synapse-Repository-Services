package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
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
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseTableUnavilableException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableEntity;
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
		for (Entity entity : entitiesToDelete) {
			adminSynapse.deleteAndPurgeEntity(entity);
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
		RowSet queryResults = waitForQueryResults("select * from "+table.getId()+" limit 2", isConsistent, countOnly);
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
		results1.getRows().remove(1);
		RowReferenceSet results4 = synapse.deleteRowsFromTable(results1);
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
			} catch (SynapseTableUnavilableException e) {
				// The table is not ready yet
				assertFalse("Table processing failed: "+e.getStatus().getErrorMessage(), TableState.PROCESSING_FAILED.equals(e.getStatus().getState()));
				System.out.println("Waiting for table index to be available: "+e.getStatus());
				Thread.sleep(2000);
				assertTrue("Timed out waiting for query results for sql: "+sql,System.currentTimeMillis()-start < MAX_QUERY_TIMEOUT_MS);
			}
		}
	}

	@Test(expected = SynapseBadRequestException.class)
	public void testDuplicateRowUpdateFails() throws SynapseException, InterruptedException {
		// Create a few columns to add to a table entity
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.STRING);
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
		List<String> idList = new LinkedList<String>();
		idList.add(one.getId());
		table.setColumnIds(idList);
		table.setParentId(project.getId());
		table = synapse.createEntity(table);
		tablesToDelete.add(table);

		assertNotNull(table);
		assertNotNull(table.getId());
		// Now make sure we can get the columns for this entity.
		List<ColumnModel> columns = synapse.getColumnModelsForTableEntity(table.getId());
		assertNotNull(columns);
		assertEquals(1, columns.size());
		List<ColumnModel> expected = new LinkedList<ColumnModel>();
		expected.add(one);
		assertEquals(expected, columns);

		// Append some rows
		RowSet set = new RowSet();
		List<Row> rows = TableModelTestUtils.createRows(columns, 2);
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(columns));
		set.setTableId(table.getId());
		RowReferenceSet results1 = synapse.appendRowsToTable(set);

		// update one row twice
		RowSet update = new RowSet();
		Row row1 = new Row();
		row1.setRowId(results1.getRows().get(0).getRowId());
		row1.setVersionNumber(results1.getRows().get(0).getVersionNumber());
		row1.setValues(rows.get(0).getValues());
		Row row2 = new Row();
		row2.setRowId(results1.getRows().get(0).getRowId());
		row2.setVersionNumber(results1.getRows().get(0).getVersionNumber());
		row2.setValues(rows.get(0).getValues());
		update.setRows(Lists.newArrayList(row1, row2));
		update.setTableId(results1.getTableId());
		update.setHeaders(results1.getHeaders());
		update.setEtag(results1.getEtag());
		synapse.appendRowsToTable(update);
	}

	@Test(expected = SynapseBadRequestException.class)
	public void testNullRowUpdateFails() throws SynapseException, InterruptedException {
		// Create a few columns to add to a table entity
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.STRING);
		one = synapse.createColumnModel(one);
		// Create a project to contain it all
		Project project = new Project();
		project.setName(UUID.randomUUID().toString());
		project = synapse.createEntity(project);
		entitiesToDelete.add(project);

		// now create a table entity
		TableEntity table = new TableEntity();
		table.setName("Table");
		List<String> idList = new LinkedList<String>();
		idList.add(one.getId());
		table.setColumnIds(idList);
		table.setParentId(project.getId());
		table = synapse.createEntity(table);
		tablesToDelete.add(table);

		List<ColumnModel> columns = synapse.getColumnModelsForTableEntity(table.getId());

		// Append some rows
		RowSet set = new RowSet();
		List<Row> rows = TableModelTestUtils.createRows(columns, 2);
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(columns));
		set.setTableId(table.getId());
		RowReferenceSet results1 = synapse.appendRowsToTable(set);

		// update one row with null values
		RowSet update = new RowSet();
		Row row1 = new Row();
		row1.setRowId(results1.getRows().get(0).getRowId());
		row1.setVersionNumber(results1.getRows().get(0).getVersionNumber());
		row1.setValues(null);
		update.setRows(Lists.newArrayList(row1));
		update.setTableId(results1.getTableId());
		update.setHeaders(results1.getHeaders());
		update.setEtag(results1.getEtag());
		synapse.appendRowsToTable(update);
	}

	@Test(expected = SynapseBadRequestException.class)
	public void testEmptyRowUpdateFails() throws SynapseException, InterruptedException {
		// Create a few columns to add to a table entity
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.STRING);
		one = synapse.createColumnModel(one);
		// Create a project to contain it all
		Project project = new Project();
		project.setName(UUID.randomUUID().toString());
		project = synapse.createEntity(project);
		entitiesToDelete.add(project);

		// now create a table entity
		TableEntity table = new TableEntity();
		table.setName("Table");
		List<String> idList = new LinkedList<String>();
		idList.add(one.getId());
		table.setColumnIds(idList);
		table.setParentId(project.getId());
		table = synapse.createEntity(table);
		tablesToDelete.add(table);

		List<ColumnModel> columns = synapse.getColumnModelsForTableEntity(table.getId());

		// Append some rows
		RowSet set = new RowSet();
		List<Row> rows = TableModelTestUtils.createRows(columns, 2);
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(columns));
		set.setTableId(table.getId());
		RowReferenceSet results1 = synapse.appendRowsToTable(set);

		// update one row with empty values
		RowSet update = new RowSet();
		Row row1 = new Row();
		row1.setRowId(results1.getRows().get(0).getRowId());
		row1.setVersionNumber(results1.getRows().get(0).getVersionNumber());
		row1.setValues(Lists.<String> newArrayList());
		update.setRows(Lists.newArrayList(row1));
		update.setTableId(results1.getTableId());
		update.setHeaders(results1.getHeaders());
		update.setEtag(results1.getEtag());
		synapse.appendRowsToTable(update);
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
	
}
