package org.sagebionetworks.table.cluster;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:table-spb.xml" })
public class TableIndexDAOImplTest {

	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	StackConfiguration config;
	// This is not a bean
	TableIndexDAO tableIndexDAO;
	
	String tableId;
	SimpleJdbcTemplate connection;
	
	@Before
	public void before(){
		// Only run this test if the table feature is enabled.
		Assume.assumeTrue(config.getTableEnabled());
		tableIndexDAO = new TableIndexDAOImpl();
		tableId = "syn123";
		// First get a connection for this table
		connection = tableConnectionFactory.getConnection(tableId);
		assertNotNull(connection);
	}
	
	@After
	public void after(){
		// Drop the table
		if(connection != null && tableId != null && tableIndexDAO != null){
			tableIndexDAO.deleteTable(connection, tableId);
		}
	}
	
	@Test
	public void testGetCurrentTableColumnsDoesNotExist(){
		// There should not be any columns for this table as it does not exist
		List<String> names = tableIndexDAO.getCurrentTableColumns(connection, tableId);
		assertEquals(null, names);
	}
	
	@Test
	public void testCRUD(){
		// Create a Simple table with only a few columns
		List<ColumnModel> allTypes = TableModelUtils.createOneOfEachType();
		// Create the table
		boolean updated = tableIndexDAO.createOrUpdateTable(connection, allTypes, tableId);
		assertTrue("The table should not have existed so update should be true",updated);
		updated = tableIndexDAO.createOrUpdateTable(connection, allTypes, tableId);
		assertFalse("The table already existed in that state so it should not have been updated",updated);
		// Now we should be able to see the columns that were created
		List<String> names = tableIndexDAO.getCurrentTableColumns(connection, tableId);
		// There should be a column for each column in the schema plus one ROW_ID and one ROW_VERSION.
		assertEquals(allTypes.size() + 2, names.size());
		// Now remove a column and update the table
		allTypes.remove(0);
		tableIndexDAO.createOrUpdateTable(connection, allTypes, tableId);
		// Now we should be able to see the columns that were created
		names = tableIndexDAO.getCurrentTableColumns(connection, tableId);
		// There should be a column for each column in the schema plus one ROW_ID and one ROW_VERSION.
		assertEquals(allTypes.size() + 2, names.size());
		// Now add a column
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.BOOLEAN);
		cm.setId("9");
		allTypes.add(cm);
		tableIndexDAO.createOrUpdateTable(connection, allTypes, tableId);
		// Now we should be able to see the columns that were created
		names = tableIndexDAO.getCurrentTableColumns(connection, tableId);
		// There should be a column for each column in the schema plus one ROW_ID and one ROW_VERSION.
		assertEquals(allTypes.size() + 2, names.size());
		// Now delete the table
		assertTrue(tableIndexDAO.deleteTable(connection, tableId));
		names = tableIndexDAO.getCurrentTableColumns(connection, tableId);
		assertEquals(null, names);
	}
	
	@Test
	public void testCreateOrUpdateRows(){
		// Create a Simple table with only a few columns
		List<ColumnModel> allTypes = TableModelUtils.createOneOfEachType();
		List<Row> rows = TableModelUtils.createRows(allTypes, 5);
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(allTypes));
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelUtils.assignRowIdsAndVersionNumbers(set, range);
		// Create the table
		tableIndexDAO.createOrUpdateTable(connection, allTypes, tableId);
		// Now fill the table with data
		tableIndexDAO.createOrUpdateRows(connection, set, allTypes);
		List<Map<String, Object>> result = connection.queryForList("SELECT * FROM "+SQLUtils.getTableNameForId("syn123"));
		assertNotNull(result);
		assertEquals(5, result.size());
		// Row zero
		Map<String, Object> row = result.get(0);
		assertEquals(100l, row.get(SQLUtils.ROW_ID));
		assertEquals(0l, row.get("C4"));
		// row four
		row = result.get(4);
		assertEquals(104l, row.get(SQLUtils.ROW_ID));
		assertEquals(13.64, row.get("C1"));
		assertEquals(4l, row.get("C4"));
		
		// We should be able to update all of the rows
		rows.get(4).setValues(Arrays.asList("update", "99.99", "3", "false", "123"));
		rows.get(4).setVersionNumber(5L);
		// This should not fail
		tableIndexDAO.createOrUpdateRows(connection, set, allTypes);
		// Check the update
		result = connection.queryForList("SELECT * FROM "+SQLUtils.getTableNameForId("syn123"));
		assertNotNull(result);
		assertEquals(5, result.size());
		// row four
		row = result.get(4);
		// Check all values on the updated row.
		assertEquals(104l, row.get(SQLUtils.ROW_ID));
		assertEquals(5L, row.get(SQLUtils.ROW_VERSION));
		assertEquals("update", row.get("C0"));
		assertEquals(99.99, row.get("C1"));
		assertEquals(3L, row.get("C2"));
		assertEquals(Boolean.FALSE, row.get("C3"));
		assertEquals(123L, row.get("C4"));
	}
	
	@Test
	public void testGetRowCountForTable(){
		// Before the table exists the max version should be null
		Long count = tableIndexDAO.getRowCountForTable(connection, tableId);
		assertEquals("The row count should be null when the table does not exist",null, count);
		// Create the table
		List<ColumnModel> allTypes = TableModelUtils.createOneOfEachType();
		tableIndexDAO.createOrUpdateTable(connection, allTypes, tableId);
		// the table now exists
		count = tableIndexDAO.getRowCountForTable(connection, tableId);
		assertEquals("The row count should be 0 when the table is empty", new Long(0), count);
		// Now add some data
		List<Row> rows = TableModelUtils.createRows(allTypes, 4);
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(allTypes));
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		tableIndexDAO.createOrUpdateRows(connection, set, allTypes);
		// Check again
		count = tableIndexDAO.getRowCountForTable(connection, tableId);
		assertEquals(new Long(rows.size()), count);
	}
	
	@Test
	public void testGetMaxVersionForTable(){
		// Before the table exists the max version should be null
		Long maxVersion = tableIndexDAO.getMaxVersionForTable(connection, tableId);
		assertEquals("The max version should be null when the table does not exist",null, maxVersion);
		// Create the table
		List<ColumnModel> allTypes = TableModelUtils.createOneOfEachType();
		tableIndexDAO.createOrUpdateTable(connection, allTypes, tableId);
		// The max version should now be -1
		maxVersion = tableIndexDAO.getMaxVersionForTable(connection, tableId);
		assertEquals("The max version should be -1 when the table is empty", new Long(-1), maxVersion);
		// Now add some data
		List<Row> rows = TableModelUtils.createRows(allTypes, 2);
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(allTypes));
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		tableIndexDAO.createOrUpdateRows(connection, set, allTypes);
		// Check again
		maxVersion = tableIndexDAO.getMaxVersionForTable(connection, tableId);
		assertEquals(new Long(3), maxVersion);

	}
}
