package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.AnnotationDTO;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.EntityDTO;
import org.sagebionetworks.repo.model.table.EntityField;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.Table;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.table.cluster.SQLUtils.TableType;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.Grouping;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.util.SimpleAggregateQueryException;
import org.sagebionetworks.table.query.util.SqlElementUntils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:table-cluster-spb.xml" })
public class TableIndexDAOImplTest {

	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	StackConfiguration config;
	// These are not a bean
	TableIndexDAO tableIndexDAO;
	
	ProgressCallback mockProgressCallback;

	String tableId;
	boolean isView;

	@Before
	public void before() {
		mockProgressCallback = Mockito.mock(ProgressCallback.class);
		tableId = "syn123";
		// First get a connection for this table
		tableIndexDAO = tableConnectionFactory.getConnection(tableId);
		tableIndexDAO.deleteTable(tableId);
		tableIndexDAO.deleteSecondaryTables(tableId);
		isView = false;
	}

	@After
	public void after() {
		// Drop the table
		if (tableId != null && tableIndexDAO != null) {
			tableIndexDAO.deleteTable(tableId);
			tableIndexDAO.deleteSecondaryTables(tableId);
		}
	}
	
	/**
	 * Helper to setup a table with a new schema.
	 * 
	 * @param newSchema
	 * @param tableId
	 */
	public boolean createOrUpdateTable(List<ColumnModel> newSchema, String tableId, boolean isView){
		List<DatabaseColumnInfo> currentSchema = tableIndexDAO.getDatabaseInfo(tableId);
		List<ColumnChangeDetails> changes = SQLUtils.createReplaceSchemaChange(currentSchema, newSchema);
		tableIndexDAO.createTableIfDoesNotExist(tableId, isView);
		boolean alterTemp = false;
		// Ensure all all updated columns actually exist.
		changes = SQLUtils.matchChangesToCurrentInfo(currentSchema, changes);
		return tableIndexDAO.alterTableAsNeeded(tableId, changes, alterTemp);
	}

	/**
	 * Helper to alter the table as needed.
	 * @param tableId
	 * @param changes
	 * @param alterTemp
	 * @return
	 */
	boolean alterTableAsNeeded(String tableId, List<ColumnChangeDetails> changes, boolean alterTemp){
		// Lookup the current schema of the index.
		List<DatabaseColumnInfo> currentIndedSchema = tableIndexDAO.getDatabaseInfo(tableId);
		tableIndexDAO.provideIndexName(currentIndedSchema, tableId);
		// Ensure all all updated columns actually exist.
		changes = SQLUtils.matchChangesToCurrentInfo(currentIndedSchema, changes);
		return tableIndexDAO.alterTableAsNeeded(tableId, changes, alterTemp);
	}
	/**
	 * Helper to apply a change set to the index.s
	 * @param rowSet
	 * @param schema
	 */
	public void createOrUpdateOrDeleteRows(RowSet rowSet, List<ColumnModel> schema){
		SparseChangeSet sparse = TableModelUtils.createSparseChangeSet(rowSet, schema);
		for(Grouping grouping: sparse.groupByValidValues()){
			tableIndexDAO.createOrUpdateOrDeleteRows(grouping);
		}
	}
	
	@Test
	public void testTableEnityTypes(){
		TableEntity tableEntity = new TableEntity();
		assertTrue(tableEntity instanceof Table);
	}
	
	@Test
	public void testFileViewTypes(){
		EntityView entityView = new EntityView();
		assertTrue(entityView instanceof Table);
	}

	@Test
	public void testCRUD() {
		// Create a Simple table with only a few columns
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		// Create the table
		boolean updated = createOrUpdateTable(allTypes, tableId, isView);
		assertTrue(
				"The table should not have existed so update should be true",
				updated);
		updated = createOrUpdateTable(allTypes, tableId, isView);
		assertFalse(
				"The table already existed in that state so it should not have been updated",
				updated);
		// Now we should be able to see the columns that were created
		List<DatabaseColumnInfo> columns = getAllColumnInfo(tableId);
		// There should be a column for each column in the schema plus one
		// ROW_ID and one ROW_VERSION plus one extra for doubles.
		assertEquals(allTypes.size() + 2 + 1, columns.size());
		for (int i = 0; i < allTypes.size(); i++) {
			// Now remove a column and update the table
			ColumnModel removed = allTypes.remove(0);
			createOrUpdateTable(allTypes, tableId, isView);
			// Now we should be able to see the columns that were created
			columns = getAllColumnInfo(tableId);
			// There should be a column for each column in the schema plus one
			// ROW_ID and one ROW_VERSION.
			int extraColumns = 1;
			if (removed.getColumnType() == ColumnType.DOUBLE) {
				extraColumns = 0;
			}
			assertEquals("removed " + removed, allTypes.size() + 2
					+ extraColumns, columns.size());
			// Now add a column
			allTypes.add(removed);
			createOrUpdateTable(allTypes, tableId, isView);
			// Now we should be able to see the columns that were created
			columns = getAllColumnInfo(tableId);
			// There should be a column for each column in the schema plus one
			// ROW_ID and one ROW_VERSION.
			assertEquals("read " + removed, allTypes.size() + 2 + 1,
					columns.size());
		}
	}

	@Test
	public void testCreateOrUpdateRows() {
		// Create a Simple table with only a few columns
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		List<Row> rows = TableModelTestUtils.createRows(allTypes, 5);
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getSelectColumns(allTypes));
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Create the table
		createOrUpdateTable(allTypes, tableId, isView);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(set, allTypes);
		List<Map<String, Object>> result = tableIndexDAO.getConnection()
				.queryForList(
						"SELECT * FROM "
								+ SQLUtils.getTableNameForId(tableId,
										TableType.INDEX));
		assertNotNull(result);
		assertEquals(5, result.size());
		// Row zero
		Map<String, Object> row = result.get(0);
		assertEquals(100l, row.get(ROW_ID));
		assertEquals(404000l, row.get("_C4_"));
		// row four
		row = result.get(4);
		assertEquals(104l, row.get(ROW_ID));
		assertEquals(341016.76, row.get("_C1_"));
		assertEquals(404004l, row.get("_C4_"));

		// We should be able to update all of the rows
		rows.get(4).setValues(
				Arrays.asList("update", "99.99", "3", "false", "123", "123",
						"syn123", "link2", "largeText", "42"));
		rows.get(4).setVersionNumber(5L);
		rows.get(0).setVersionNumber(5L);
		// This should not fail
		createOrUpdateOrDeleteRows(set, allTypes);
		// Check the update
		result = tableIndexDAO.getConnection().queryForList(
				"SELECT * FROM "
						+ SQLUtils.getTableNameForId(tableId, TableType.INDEX));
		assertNotNull(result);
		assertEquals(5, result.size());
		// row four
		row = result.get(4);
		// Check all values on the updated row.
		assertEquals(104l, row.get(ROW_ID));
		assertEquals(5L, row.get(ROW_VERSION));
		assertEquals("update", row.get("_C0_"));
		assertEquals(99.99, row.get("_C1_"));
		assertEquals(3L, row.get("_C2_"));
		assertEquals(Boolean.FALSE, row.get("_C3_"));
		assertEquals(123L, row.get("_C4_"));
		assertEquals(123L, row.get("_C5_"));
		assertEquals(new Long(123), row.get("_C6_"));
		assertEquals("largeText", row.get("_C8_"));
		assertEquals(42L, row.get("_C9_"));
	}

	@Test
	public void testGetRowCountForTable() {
		// Before the table exists the max version should be null
		Long count = tableIndexDAO.getRowCountForTable(tableId);
		assertEquals(
				"The row count should be null when the table does not exist",
				null, count);
		// Create the table
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		createOrUpdateTable(allTypes, tableId, isView);
		// the table now exists
		count = tableIndexDAO.getRowCountForTable(tableId);
		assertEquals("The row count should be 0 when the table is empty",
				new Long(0), count);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(allTypes, 4);
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getSelectColumns(allTypes));
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(set, allTypes);
		// Check again
		count = tableIndexDAO.getRowCountForTable(tableId);
		assertEquals(new Long(rows.size()), count);
		
		// truncate and get the count again
		tableIndexDAO.truncateTable(tableId);
		count = tableIndexDAO.getRowCountForTable(tableId);
		assertEquals(new Long(0), count);
	}

	@Test
	public void testGetMaxVersionForTable() {
		tableIndexDAO.createSecondaryTables(tableId);
		// Before the table exists the max version should be -1L
		Long maxVersion = tableIndexDAO
				.getMaxCurrentCompleteVersionForTable(tableId);
		assertEquals(-1L, maxVersion.longValue());

		// Create the table
		tableIndexDAO.setMaxCurrentCompleteVersionForTable(tableId, 2L);

		maxVersion = tableIndexDAO
				.getMaxCurrentCompleteVersionForTable(tableId);
		assertEquals(2L, maxVersion.longValue());

		tableIndexDAO.setMaxCurrentCompleteVersionForTable(tableId, 4L);

		maxVersion = tableIndexDAO
				.getMaxCurrentCompleteVersionForTable(tableId);
		assertEquals(4L, maxVersion.longValue());

		tableIndexDAO.deleteSecondaryTables(tableId);
	}
	
	@Test
	public void testGetSchemaHashForTable(){
		tableIndexDAO.createSecondaryTables(tableId);
		// Before the table exists the max version should be -1L
		String hash = tableIndexDAO.getCurrentSchemaMD5Hex(tableId);
		assertEquals(TableIndexDAOImpl.EMPTY_SCHEMA_MD5, hash);
		
		hash = "some hash";
		tableIndexDAO.setCurrentSchemaMD5Hex(tableId, hash);
		String returnHash = tableIndexDAO.getCurrentSchemaMD5Hex(tableId);
		assertEquals(hash, returnHash);
		// setting the version should not change the hash
		tableIndexDAO.setMaxCurrentCompleteVersionForTable(tableId, 4L);
		// did it change?
		returnHash = tableIndexDAO.getCurrentSchemaMD5Hex(tableId);
		assertEquals(hash, returnHash);
	}

	@Test
	public void testSimpleQuery() throws ParseException, SimpleAggregateQueryException {
		// Create the table
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		createOrUpdateTable(allTypes, tableId, isView);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(allTypes, 2);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(allTypes);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(set, allTypes);
		// This is our query
		SqlQuery query = new SqlQueryBuilder("select * from " + tableId, allTypes).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		System.out.println(results);
		assertEquals(headers, results.getHeaders());
		assertNotNull(results.getRows());
		assertEquals(tableId, results.getTableId());
		assertEquals(2, results.getRows().size());
		// test the count
		String countSql = SqlElementUntils.createCountSql(query.getTransformedModel());
		Long count = tableIndexDAO.countQuery(countSql, query.getParameters());
		assertEquals(new Long(2), count);
		
		// the first row
		Row row = results.getRows().get(0);
		assertNotNull(row);
		assertEquals(new Long(100), row.getRowId());
		assertEquals(new Long(3), row.getVersionNumber());
		List<String> expectedValues = Arrays.asList("string0", "341003.12",
				"203000", "false", "404000", "505000", "syn606000",
				"link708000", "largeText804000", "903000");
		assertEquals(expectedValues, row.getValues());
		// Second row
		row = results.getRows().get(1);
		assertNotNull(row);
		assertEquals(new Long(101), row.getRowId());
		assertEquals(new Long(3), row.getVersionNumber());
		expectedValues = Arrays.asList("string1", "341006.53", "203001",
				"true", "404001", "505001", "syn606001", 
				"link708001", "largeText804001", "903001");
		assertEquals(expectedValues, row.getValues());
		// must also be able to run the query with a null callback
		mockProgressCallback = null;
	}

	@Test
	public void testDoubleQuery() throws ParseException {
		// Create the table
		List<ColumnModel> doubleColumn = Lists.newArrayList(TableModelTestUtils
				.createColumn(1L, "col1", ColumnType.DOUBLE));
		createOrUpdateTable(doubleColumn, tableId, isView);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(doubleColumn, 5);
		// insert special values
		rows.get(1).getValues().set(0, "0");
		rows.get(2).getValues().set(0, "NaN");
		rows.get(3).getValues().set(0, "Infinity");
		rows.get(4).getValues().set(0, "-Infinity");
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(doubleColumn);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(set, doubleColumn);
		// This is our query
		SqlQuery query = new SqlQueryBuilder("select * from " + tableId, doubleColumn).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		System.out.println(results);
		assertEquals(headers, results.getHeaders());
		assertNotNull(results.getRows());
		assertEquals(tableId, results.getTableId());
		assertEquals(5, results.getRows().size());
		assertEquals("3.12", results.getRows().get(0).getValues().get(0));
		assertEquals("0", results.getRows().get(1).getValues().get(0));
		assertEquals("NaN", results.getRows().get(2).getValues().get(0));
		assertEquals("Infinity", results.getRows().get(3).getValues().get(0));
		assertEquals("-Infinity", results.getRows().get(4).getValues().get(0));
	}

	@Test
	public void testSimpleQueryWithDeletedRows() throws ParseException {
		// Create the table
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		createOrUpdateTable(allTypes, tableId, isView);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(allTypes, 4);
		Row deletion = new Row();
		rows.add(deletion);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(allTypes);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setMaximumUpdateId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(set, allTypes);

		// now delete the second and fourth row
		set.getRows().remove(0);
		set.getRows().remove(1);
		set.getRows().get(0).getValues().clear();
		set.getRows().get(1).getValues().clear();
		range.setVersionNumber(4L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		createOrUpdateOrDeleteRows(set, allTypes);
		// This is our query
		SqlQuery query = new SqlQueryBuilder("select * from " + tableId, allTypes).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		System.out.println(results);
		assertEquals(2, results.getRows().size());
		assertEquals(100L, results.getRows().get(0).getRowId().longValue());
		assertEquals(102L, results.getRows().get(1).getRowId().longValue());
	}

	@Test
	public void testNullQuery() throws ParseException {
		// Create the table
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		createOrUpdateTable(allTypes, tableId, isView);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createNullRows(allTypes, 2);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(allTypes);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(set, allTypes);
		// Now query for the results
		SqlQuery query = new SqlQueryBuilder("select * from " + tableId, allTypes).build();
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		System.out.println(results);
		assertEquals(headers, results.getHeaders());
		assertNotNull(results.getRows());
		assertEquals(tableId, results.getTableId());
		assertEquals(2, results.getRows().size());
		// the first row
		Row row = results.getRows().get(0);
		assertNotNull(row);
		assertEquals(new Long(100), row.getRowId());
		assertEquals(new Long(3), row.getVersionNumber());
		List<String> expectedValues = Arrays.asList(null, null, null, null,
				null, null, null, null,  null, null);
		assertEquals(expectedValues, row.getValues());
		// Second row
		row = results.getRows().get(1);
		assertNotNull(row);
		assertEquals(new Long(101), row.getRowId());
		assertEquals(new Long(3), row.getVersionNumber());
		expectedValues = Arrays.asList(null, null, null, null, null, null,
				null, null, null, null);
		assertEquals(expectedValues, row.getValues());
	}

	@Test
	public void testQueryAggregate() throws ParseException {
		// Create the table
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		SelectColumn selectColumn = new SelectColumn();
		selectColumn.setColumnType(null);
		selectColumn.setId(null);
		selectColumn.setName("count(*)");
		createOrUpdateTable(allTypes, tableId, isView);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(allTypes, 2);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(allTypes);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(set, allTypes);
		// Now a count query
		SqlQuery query = new SqlQueryBuilder("select count(*) from " + tableId,
				allTypes).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		List<SelectColumn> expectedHeaders = Lists.newArrayList(TableModelUtils
				.createSelectColumn("COUNT(*)", ColumnType.INTEGER, null));
		assertEquals(expectedHeaders, results.getHeaders());
		assertNotNull(results.getRows());
		assertEquals(tableId, results.getTableId());
		assertEquals(1, results.getRows().size());
		// first and only row.
		Row row = results.getRows().get(0);
		assertNotNull(row);
		assertEquals("RowId should be null for an aggregate function.", null,
				row.getRowId());
		assertEquals("RowVersion should be null for an aggregate function",
				null, row.getVersionNumber());
		List<String> expectedValues = Arrays.asList("2");
		assertEquals(expectedValues, row.getValues());
	}

	@Test
	public void testQueryAllParts() throws ParseException {
		ColumnModel foo = new ColumnModel();
		foo.setColumnType(ColumnType.STRING);
		foo.setName("foo");
		foo.setId("111");
		foo.setMaximumSize(10L);
		ColumnModel bar = new ColumnModel();
		bar.setColumnType(ColumnType.INTEGER);
		bar.setId("222");
		bar.setName("bar");
		List<ColumnModel> schema = new LinkedList<ColumnModel>();
		schema.add(foo);
		schema.add(bar);
		// Create the table.
		createOrUpdateTable(schema, tableId, isView);
		// Create some data
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 100);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(schema);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(4L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(set, schema);
		// Now create the query
		SqlQuery query = new SqlQueryBuilder(
				"select foo, bar from "
						+ tableId
						+ " where foo is not null group by foo order by bar desc limit 1 offset 0",
				schema).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		assertEquals(schema.size(),
				results.getHeaders().size());
		assertNotNull(results.getRows());
		assertEquals(tableId, results.getTableId());
		assertEquals(1, results.getRows().size());
		// first and only row.
		Row row = results.getRows().get(0);
		assertNotNull(row);
		// is aggregate, so no row id and version
		assertEquals(null, row.getRowId());
		assertEquals(null, row.getVersionNumber());
		List<String> expectedValues = Arrays.asList("string99", "103099");
		assertEquals(expectedValues, row.getValues());
	}

	@Test
	public void testQueryRowIdAndRowVersion() throws ParseException {
		ColumnModel foo = new ColumnModel();
		foo.setColumnType(ColumnType.STRING);
		foo.setName("foo");
		foo.setId("111");
		foo.setMaximumSize(10L);
		ColumnModel bar = new ColumnModel();
		bar.setColumnType(ColumnType.INTEGER);
		bar.setId("222");
		bar.setName("bar");
		List<ColumnModel> schema = new LinkedList<ColumnModel>();
		schema.add(foo);
		schema.add(bar);
		// Create the table.
		createOrUpdateTable(schema, tableId, isView);
		// Create some data
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 100);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(schema);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(4L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(set, schema);
		// Now create the query
		SqlQuery query = new SqlQueryBuilder("select * from " + tableId
				+ " where ROW_ID = 104 AND Row_Version > 1 limit 1 offset 0",
				schema).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		assertNotNull(results.getRows());
		assertEquals(tableId, results.getTableId());
		assertEquals(1, results.getRows().size());
		// first and only row.
		Row row = results.getRows().get(0);
		assertNotNull(row);
		assertEquals(new Long(104), row.getRowId());
		assertEquals(new Long(4), row.getVersionNumber());
		List<String> expectedValues = Arrays.asList("string4", "103004");
		assertEquals(expectedValues, row.getValues());
	}

	@Test
	public void testTooManyColumns() throws Exception {
		List<ColumnModel> schema = Lists.newArrayList();
		List<String> indexes = Lists.newArrayList();
		indexes.add("ROW_ID");
		for (int i = 0; i < 100; i++) {
			ColumnModel cm = new ColumnModel();
			cm.setColumnType(ColumnType.STRING);
			cm.setName("foo" + i);
			cm.setId("111" + i);
			cm.setMaximumSize(30L);
			schema.add(cm);
			if (indexes.size() < 64) {
				indexes.add(SQLUtils.getColumnNameForId(cm.getId()));
			}
		}
		// Create the table.
		createOrUpdateTable(schema, tableId, isView);
	}

	@Test
	public void testTooManyColumnsAppended() throws Exception {
		List<ColumnModel> schema = new LinkedList<ColumnModel>();
		List<String> indexes = Lists.newArrayList();
		indexes.add("ROW_ID");
		for (int i = 0; i < 63; i++) {
			ColumnModel cm = new ColumnModel();
			cm.setColumnType(ColumnType.STRING);
			cm.setName("foo" + i);
			cm.setId("111" + i);
			cm.setMaximumSize(30L);
			schema.add(cm);
			if (indexes.size() < 64) {
				indexes.add(SQLUtils.getColumnNameForId(cm.getId()));
			}
		}

		// Create the table.
		createOrUpdateTable(schema, tableId, isView);

		// replace 10 columns
		for (int i = 30; i < 40; i++) {
			ColumnModel cm = schema.get(i);
			cm.setId("333" + i);
			indexes.set(i + 1, SQLUtils.getColumnNameForId(cm.getId()));
		}

		createOrUpdateTable(schema, tableId, isView);

		// replace 10 and add 10 columns
		for (int i = 20; i < 30; i++) {
			ColumnModel cm = schema.get(i);
			cm.setId("444" + i);
			indexes.set(i + 1, SQLUtils.getColumnNameForId(cm.getId()));
		}
		for (int i = 0; i < 10; i++) {
			ColumnModel cm = new ColumnModel();
			cm.setColumnType(ColumnType.STRING);
			cm.setName("foo" + i);
			cm.setId("222" + i);
			cm.setMaximumSize(30L);
			schema.add(cm);
			if (indexes.size() < 64) {
				indexes.add(SQLUtils.getColumnNameForId(cm.getId()));
			}
		}
		createOrUpdateTable(schema, tableId, isView);
	}
	
	@Test
	public void testCreateSecondaryTablesCreateDeleteIdempotent(){
		// ensure the secondary tables for this index exist
		this.tableIndexDAO.createSecondaryTables(tableId);
		// must be able to call this again.
		this.tableIndexDAO.createSecondaryTables(tableId);
		// The delete must also be idempotent
		this.tableIndexDAO.deleteSecondaryTables(tableId);
		// must be able to call this again
		this.tableIndexDAO.deleteSecondaryTables(tableId);
	}

	/**
	 * Test for the secondary table used to bind file handle IDs to a table.
	 * Once a file handle is bound to a table, any user with download permission
	 * will haver permission to download that file from the table.
	 */
	@Test
	public void testBindFileHandles() {
		// ensure the secondary tables for this index exist
		this.tableIndexDAO.createSecondaryTables(tableId);
		Set<Long> toBind = Sets.newHashSet(1L, 2L, 5L);
		// find the files
		this.tableIndexDAO.applyFileHandleIdsToTable(tableId, toBind);
		Set<Long> toTest = Sets.newHashSet(0L, 2L, 3L, 5L, 8L);
		// Expect to find the intersection of the toBound and toTest.
		Set<Long> expected = Sets.newHashSet(2L, 5L);

		Set<Long> results = this.tableIndexDAO.getFileHandleIdsAssociatedWithTable(toTest, tableId);
		assertEquals(expected, results);
	}
	
	@Test
	public void testBindFileHandlesWithOverlap() {
		// ensure the secondary tables for this index exist
		this.tableIndexDAO.createSecondaryTables(tableId);
		Set<Long> toBind1 = Sets.newHashSet(1L, 2L, 3L);
		this.tableIndexDAO.applyFileHandleIdsToTable(tableId, toBind1);
		Set<Long> toBind2 = Sets.newHashSet(2L, 3L, 4L);
		// must add 4 and ignore 2 & 3.
		this.tableIndexDAO.applyFileHandleIdsToTable(tableId, toBind2);
		Set<Long> toTest = Sets.newHashSet(5L,4L,3L,2L,1L,0L);
		// Expect to find the intersection of the toBound and toTest.
		Set<Long> expected = Sets.newHashSet(4L,3L,2L,1L);
		Set<Long> results = this.tableIndexDAO.getFileHandleIdsAssociatedWithTable(toTest, tableId);
		assertEquals(expected, results);
	}
	
	/**
	 * When the secondary table does not exist, an empty set should be returned.
	 */
	@Test
	public void testBindFileHandlesTableDoesNotExist() {
		// test with no secondary table.
		this.tableIndexDAO.deleteSecondaryTables(tableId);
		Set<Long> toTest = Sets.newHashSet(0L, 2L, 3L, 5L, 8L);
		Set<Long> results = this.tableIndexDAO.getFileHandleIdsAssociatedWithTable(toTest, tableId);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	@Test
	public void testDoesIndexStateMatch(){
		// ensure the secondary tables for this index exist
		this.tableIndexDAO.createSecondaryTables(tableId);
		
		String md5 = "md5hex";
		this.tableIndexDAO.setCurrentSchemaMD5Hex(tableId, md5);
		long version = 123;
		this.tableIndexDAO.setMaxCurrentCompleteVersionForTable(tableId, version);
		// call under test
		boolean match = this.tableIndexDAO.doesIndexStateMatch(tableId, version, md5);
		assertTrue(match);
		
		// call under test
		match = this.tableIndexDAO.doesIndexStateMatch(tableId, version+1, md5);
		assertFalse(match);
		
		// call under test
		match = this.tableIndexDAO.doesIndexStateMatch(tableId, version, md5+1);
		assertFalse(match);
	}
	
	@Test
	public void testSetIndexVersionAndSchemaMD5Hex(){
		// ensure the secondary tables for this index exist
		this.tableIndexDAO.createSecondaryTables(tableId);
		
		String md5 = "md5hex";
		Long version = 123L;
		// call under test.
		this.tableIndexDAO.setIndexVersionAndSchemaMD5Hex(tableId, version, md5);
		
		assertEquals(md5, this.tableIndexDAO.getCurrentSchemaMD5Hex(tableId));
		assertEquals(version, this.tableIndexDAO.getMaxCurrentCompleteVersionForTable(tableId));
	}
	
	@Test
	public void testDoesIndexStateMatchTableDoesNotExist(){
		// ensure the secondary tables for this index exist
		this.tableIndexDAO.createSecondaryTables(tableId);
		// the status table does not exist for this case.
		String md5 = "md5hex";
		long version = 123;
		// call under test
		boolean match = this.tableIndexDAO.doesIndexStateMatch(tableId, version, md5);
		assertFalse(match);
	}
	
	@Test
	public void testGetDistinctLongValues(){
		// create a table with a long column.
		ColumnModel column = new ColumnModel();
		column.setId("12");
		column.setName("foo");
		column.setColumnType(ColumnType.INTEGER);
		List<ColumnModel> schema = Lists.newArrayList(column);
		
		createOrUpdateTable(schema, tableId, isView);
		// create three rows.
		List<Row> rows = TableModelTestUtils.createRows(schema, 2);
		// add duplicate values
		rows.addAll(TableModelTestUtils.createRows(schema, 2));
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getSelectColumns(schema));
		set.setTableId(tableId);
		
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		
		createOrUpdateOrDeleteRows(set, schema);
		
		Set<Long> results = tableIndexDAO.getDistinctLongValues(tableId, SQLUtils.getColumnNameForId(column.getId()));
		Set<Long> expected = Sets.newHashSet(3000L, 3001L);
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetCurrentTableColumns(){
		// Create the table
		tableIndexDAO.createTableIfDoesNotExist(tableId, isView);
		// Call under test.
		List<DatabaseColumnInfo> schema = getAllColumnInfo(tableId);
		assertNotNull(schema);
		assertEquals(2, schema.size());
		DatabaseColumnInfo cd = schema.get(0);
		assertEquals(ROW_ID, cd.getColumnName());
		assertTrue("ROW_ID is the primary key so it should have an index.",cd.hasIndex());
		
		cd = schema.get(1);
		assertEquals(ROW_VERSION, cd.getColumnName());
		assertFalse(cd.hasIndex());
	}
	
	@Test
	public void testAlterTableAsNeeded(){
		// This will be an add, so the old is null.
		ColumnModel oldColumn = null;
		ColumnModel newColumn = new ColumnModel();
		newColumn.setColumnType(ColumnType.BOOLEAN);
		newColumn.setId("123");
		newColumn.setName("aBoolean");
		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, newColumn);
		// Create the table
		tableIndexDAO.createTableIfDoesNotExist(tableId, isView);
		boolean alterTemp = false;
		// call under test.
		boolean wasAltered = alterTableAsNeeded(tableId, Lists.newArrayList(change), alterTemp);
		assertTrue(wasAltered);
		// Check the results
		List<DatabaseColumnInfo> schema =  getAllColumnInfo(tableId);
		assertNotNull(schema);
		assertEquals(3, schema.size());
		DatabaseColumnInfo cd = schema.get(2);
		assertEquals("_C123_", cd.getColumnName());
		assertFalse(cd.hasIndex());
		
		// Another update of the same column with no change should not alter the table
		oldColumn = newColumn;
		change = new ColumnChangeDetails(oldColumn, newColumn);
		wasAltered = alterTableAsNeeded(tableId, Lists.newArrayList(change), alterTemp);
		assertFalse(wasAltered);
	}
	
	
	@Test
	public void testColumnInfoAndCardinality(){
		// create a table with a long column.
		ColumnModel intColumn = new ColumnModel();
		intColumn.setId("12");
		intColumn.setName("foo");
		intColumn.setColumnType(ColumnType.INTEGER);
		
		ColumnModel booleanColumn = new ColumnModel();
		booleanColumn.setId("13");
		booleanColumn.setName("bar");
		booleanColumn.setColumnType(ColumnType.BOOLEAN);
		
		List<ColumnModel> schema = Lists.newArrayList(intColumn, booleanColumn);
		
		createOrUpdateTable(schema, tableId, isView);
		// create three rows.
		List<Row> rows = TableModelTestUtils.createRows(schema, 5);
		// add duplicate values
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getSelectColumns(schema));
		set.setTableId(tableId);
		
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		
		createOrUpdateOrDeleteRows(set, schema);
		
		List<DatabaseColumnInfo> infoList = getAllColumnInfo(tableId);
		assertNotNull(infoList);
		tableIndexDAO.optimizeTableIndices(infoList, tableId, 4);
		infoList = getAllColumnInfo(tableId);
		
		assertEquals(4, infoList.size());
		
		DatabaseColumnInfo info = infoList.get(0);
		// ROW_ID
		assertEquals("ROW_ID", info.getColumnName());
		assertEquals(new Long(5), info.getCardinality());
		assertEquals("PRIMARY", info.getIndexName());
		assertTrue(info.hasIndex());
		assertEquals(MySqlColumnType.BIGINT, info.getType());
		assertEquals(new Integer(20), info.getMaxSize());
		assertEquals(null, info.getColumnType());
		
		// one
		info = infoList.get(2);
		assertEquals("_C12_", info.getColumnName());
		assertEquals(new Long(5), info.getCardinality());
		assertTrue(info.hasIndex());
		assertEquals("_C12_idx_", info.getIndexName());
		assertEquals(MySqlColumnType.BIGINT, info.getType());
		assertEquals(new Integer(20), info.getMaxSize());
		assertEquals(ColumnType.INTEGER, info.getColumnType());
		
		// two
		info = infoList.get(3);
		assertEquals("_C13_", info.getColumnName());
		assertEquals(new Long(2), info.getCardinality());
		assertTrue(info.hasIndex());
		assertEquals("_C13_idx_", info.getIndexName());
		assertEquals(MySqlColumnType.TINYINT, info.getType());
		assertEquals(new Integer(1), info.getMaxSize());
		assertEquals(ColumnType.BOOLEAN, info.getColumnType());
	}
	
	@Test
	public void testIndexAdd(){
		// create the table
		tableIndexDAO.createTableIfDoesNotExist(tableId, isView);
		ColumnModel oldColumn = null;
		
		// Create a column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("12");
		newColumn.setName("foo");
		newColumn.setColumnType(ColumnType.INTEGER);
		boolean alterTemp = false;
		// add the column
		alterTableAsNeeded(tableId, Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn)), alterTemp);
		int maxNumberOfIndices = 5;
		optimizeTableIndices(tableId, maxNumberOfIndices);
		// Get the latest table information
		List<DatabaseColumnInfo> infoList = getAllColumnInfo(tableId);
		assertNotNull(infoList);
		assertEquals(3, infoList.size());
		DatabaseColumnInfo info = infoList.get(2);
		assertEquals("_C12_idx_",info.getIndexName());
	}
	
	@Test
	public void testIndexRename(){
		// create the table
		tableIndexDAO.createTableIfDoesNotExist(tableId, isView);
		ColumnModel oldColumn = null;
		
		// Create a column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("12");
		newColumn.setName("foo");
		newColumn.setColumnType(ColumnType.INTEGER);
		boolean alterTemp = false;
		// add the column
		alterTableAsNeeded(tableId, Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn)), alterTemp);
		int maxNumberOfIndices = 5;
		optimizeTableIndices(tableId, maxNumberOfIndices);
		// Get the latest table information
		List<DatabaseColumnInfo> infoList = getAllColumnInfo(tableId);
		assertNotNull(infoList);
		assertEquals(3, infoList.size());
		DatabaseColumnInfo info = infoList.get(2);
		assertEquals("_C12_idx_",info.getIndexName());
		
		// Now change the column type
		oldColumn = newColumn;
		newColumn = new ColumnModel();
		newColumn.setId("13");
		newColumn.setName("bar");
		newColumn.setColumnType(ColumnType.DATE);
		
		alterTableAsNeeded(tableId, Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn)), alterTemp);
		// the index should get renamed
		optimizeTableIndices(tableId, maxNumberOfIndices);
		infoList = getAllColumnInfo(tableId);
		assertNotNull(infoList);
		assertEquals(3, infoList.size());
		info = infoList.get(2);
		assertEquals("_C13_idx_",info.getIndexName());
	}
	
	@Test
	public void testIndexDrop(){
		// create the table
		tableIndexDAO.createTableIfDoesNotExist(tableId, isView);
		ColumnModel oldColumn = null;
		
		// Create a column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("12");
		newColumn.setName("foo");
		newColumn.setColumnType(ColumnType.INTEGER);
		boolean alterTemp = false;
		// add the column
		alterTableAsNeeded(tableId, Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn)), alterTemp);
		int maxNumberOfIndices = 5;
		optimizeTableIndices(tableId, maxNumberOfIndices);
		// Get the latest table information
		List<DatabaseColumnInfo> infoList = getAllColumnInfo(tableId);
		assertNotNull(infoList);
		assertEquals(3, infoList.size());
		DatabaseColumnInfo info = infoList.get(2);
		assertEquals("_C12_idx_",info.getIndexName());
		
		// reduce the number of allowed indices
		maxNumberOfIndices = 1;
		// the index should get renamed
		optimizeTableIndices(tableId, maxNumberOfIndices);
		// the column should no longer have an index.
		infoList = getAllColumnInfo(tableId);
		assertNotNull(infoList);
		assertEquals(3, infoList.size());
		info = infoList.get(2);
		assertFalse(info.hasIndex());
		assertEquals(null,info.getIndexName());
	}
	
	/**
	 * Helper to get all of the DatabaseColumnInfo about a table. 
	 * @param tableId
	 * @return
	 */
	public List<DatabaseColumnInfo> getAllColumnInfo(String tableId){
		List<DatabaseColumnInfo> info = tableIndexDAO.getDatabaseInfo(tableId);
		tableIndexDAO.provideCardinality(info, tableId);
		tableIndexDAO.provideIndexName(info, tableId);
		return info;
	}
	
	/**
	 * Helper to optimize the indices for a table.
	 * 
	 * @param tableId
	 * @param maxNumberOfIndices
	 */
	public void optimizeTableIndices(String tableId, int maxNumberOfIndices){
		List<DatabaseColumnInfo> info = getAllColumnInfo(tableId);
		tableIndexDAO.optimizeTableIndices(info, tableId, maxNumberOfIndices);
	}
	
	@Test
	public void testGetDatabaseInfoEmpty(){
		// table does not exist
		List<DatabaseColumnInfo> info = tableIndexDAO.getDatabaseInfo(tableId);
		assertNotNull(info);
		assertTrue(info.isEmpty());
	}
	
	@Test
	public void testCreateTempTable(){
		ColumnModel intColumn = new ColumnModel();
		intColumn.setId("12");
		intColumn.setName("foo");
		intColumn.setColumnType(ColumnType.INTEGER);
		
		ColumnModel booleanColumn = new ColumnModel();
		booleanColumn.setId("13");
		booleanColumn.setName("bar");
		booleanColumn.setColumnType(ColumnType.BOOLEAN);
		
		List<ColumnModel> schema = Lists.newArrayList(intColumn, booleanColumn);
		
		createOrUpdateTable(schema, tableId, isView);
		// create three rows.
		List<Row> rows = TableModelTestUtils.createRows(schema, 5);
		// add duplicate values
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getSelectColumns(schema));
		set.setTableId(tableId);
		
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		
		createOrUpdateOrDeleteRows(set, schema);
		
		tableIndexDAO.deleteTemporaryTable(tableId);
		// Create a copy of the table
		tableIndexDAO.createTemporaryTable(tableId);
		// populate table with data
		tableIndexDAO.copyAllDataToTemporaryTable(tableId);
		
		long count = tableIndexDAO.getTempTableCount(tableId);
		assertEquals(5L, count);
		// delete the temp and get the count again
		tableIndexDAO.deleteTemporaryTable(tableId);
		count = tableIndexDAO.getTempTableCount(tableId);
		assertEquals(0L, count);
	}
	
	@Test
	public void testEntityReplication(){
		// delete all data
		tableIndexDAO.deleteEntityData(mockProgressCallback, Lists.newArrayList(1L,2L,3L));
		
		EntityDTO project = createEntityDTO(1L, EntityType.project, 0);
		EntityDTO folder = createEntityDTO(2L, EntityType.folder, 5);
		EntityDTO file = createEntityDTO(3L, EntityType.file, 10);
		tableIndexDAO.addEntityData(mockProgressCallback, Lists.newArrayList(file, folder, project));
		
		// lookup each
		EntityDTO fetched = tableIndexDAO.getEntityData(1L);
		assertEquals(project, fetched);
		fetched = tableIndexDAO.getEntityData(2L);
		assertEquals(folder, fetched);
		fetched = tableIndexDAO.getEntityData(3L);
		assertEquals(file, fetched);
	}
	
	@Test
	public void testEntityReplicationWithNulls(){
		// delete all data
		tableIndexDAO.deleteEntityData(mockProgressCallback, Lists.newArrayList(1L));
		
		EntityDTO project = createEntityDTO(1L, EntityType.project, 0);
		project.setParentId(null);
		project.setProjectId(null);
		project.setFileHandleId(null);
		tableIndexDAO.addEntityData(mockProgressCallback, Lists.newArrayList(project));
		
		// lookup each
		EntityDTO fetched = tableIndexDAO.getEntityData(1L);
		assertEquals(project, fetched);
	}
	
	@Test
	public void testEntityReplicationWithNullBenefactor(){
		// delete all data
		tableIndexDAO.deleteEntityData(mockProgressCallback, Lists.newArrayList(1L));
		
		EntityDTO project = createEntityDTO(1L, EntityType.project, 0);
		project.setBenefactorId(null);
		try {
			tableIndexDAO.addEntityData(mockProgressCallback, Lists.newArrayList(project));
			fail();
		} catch (Exception e) {
			// expected
		}
	}
	
	@Test
	public void testEntityReplicationUpdate(){
		// delete all data
		tableIndexDAO.deleteEntityData(mockProgressCallback, Lists.newArrayList(1L));
		
		EntityDTO file = createEntityDTO(1L, EntityType.file, 5);
		tableIndexDAO.addEntityData(mockProgressCallback, Lists.newArrayList(file));
		// delete before an update
		tableIndexDAO.deleteEntityData(mockProgressCallback, Lists.newArrayList(file.getId()));
		file = createEntityDTO(1L, EntityType.file, 3);
		tableIndexDAO.addEntityData(mockProgressCallback, Lists.newArrayList(file));
		
		// lookup each
		EntityDTO fetched = tableIndexDAO.getEntityData(1L);
		assertEquals(file, fetched);
	}

	@Test
	public void testCalculateCRC32ofEntityReplicationScopeFile(){
		// delete all data
		tableIndexDAO.deleteEntityData(mockProgressCallback, Lists.newArrayList(1L,2L,3L));
		
		// setup some hierarchy.
		EntityDTO file1 = createEntityDTO(2L, EntityType.file, 0);
		file1.setParentId(333L);
		EntityDTO file2 = createEntityDTO(3L, EntityType.file, 0);
		file2.setParentId(222L);
		
		tableIndexDAO.addEntityData(mockProgressCallback, Lists.newArrayList(file1, file2));
		// both parents
		Set<Long> scope = Sets.newHashSet(file1.getParentId(), file2.getParentId());
		// call under test
		Long crc = tableIndexDAO.calculateCRC32ofEntityReplicationScope(ViewTypeMask.File.getMask(), scope);
		assertEquals(new Long(381255304L), crc);
		// reduce the scope
		scope = Sets.newHashSet(file1.getParentId());
		// call under test
		crc = tableIndexDAO.calculateCRC32ofEntityReplicationScope(ViewTypeMask.File.getMask(), scope);
		assertEquals(new Long(3214398L), crc);
		// reduce the scope
		scope = Sets.newHashSet(file2.getParentId());
		// call under test
		crc = tableIndexDAO.calculateCRC32ofEntityReplicationScope(ViewTypeMask.File.getMask(), scope);
		assertEquals(new Long(378040906L), crc);
	}
	

	@Test
	public void testCalculateCRC32ofEntityReplicationScopeProject(){
		// delete all data
		tableIndexDAO.deleteEntityData(mockProgressCallback, Lists.newArrayList(1L,2L,3L));
		
		// setup some hierarchy.
		EntityDTO project1 = createEntityDTO(2L, EntityType.project, 0);
		project1.setParentId(111L);
		EntityDTO project2 = createEntityDTO(3L, EntityType.project, 0);
		project2.setParentId(111L);
		
		tableIndexDAO.addEntityData(mockProgressCallback, Lists.newArrayList(project1, project2));
		// both parents
		Set<Long> scope = Sets.newHashSet(project1.getId(), project2.getId());
		// call under test
		Long crc = tableIndexDAO.calculateCRC32ofEntityReplicationScope(ViewTypeMask.Project.getMask(), scope);
		assertEquals(new Long(381255304L), crc);
		// reduce the scope
		scope = Sets.newHashSet(project1.getId());
		// call under test
		crc = tableIndexDAO.calculateCRC32ofEntityReplicationScope(ViewTypeMask.Project.getMask(), scope);
		assertEquals(new Long(3214398L), crc);
		// reduce the scope
		scope = Sets.newHashSet(project2.getId());
		// call under test
		crc = tableIndexDAO.calculateCRC32ofEntityReplicationScope(ViewTypeMask.Project.getMask(), scope);
		assertEquals(new Long(378040906L), crc);
	}
	
	@Test
	public void testCalculateCRC32ofEntityReplicationNoRows(){
		// nothing should have this scope
		Set<Long> scope = Sets.newHashSet(99999L);
		// call under test
		Long crc = tableIndexDAO.calculateCRC32ofEntityReplicationScope(ViewTypeMask.File.getMask(), scope);
		assertEquals(new Long(-1), crc);
	}
	
	@Test
	public void testCalculateCRC32ofEntityReplicationScopeEmpty(){
		Set<Long> scope = new HashSet<Long>();
		// call under test
		Long crc = tableIndexDAO.calculateCRC32ofEntityReplicationScope(ViewTypeMask.File.getMask(), scope);
		assertEquals(new Long(-1), crc);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCalculateCRC32ofEntityReplicationNullType(){
		Set<Long> scope = new HashSet<Long>();
		// call under test
		tableIndexDAO.calculateCRC32ofEntityReplicationScope(null, scope);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCalculateCRC32ofEntityReplicationNullScope(){
		Set<Long> scope = null;
		// call under test
		tableIndexDAO.calculateCRC32ofEntityReplicationScope(ViewTypeMask.File.getMask(), scope);
	}
	
	@Test
	public void testCopyEntityReplicationToTable(){
		isView = true;
		// delete all data
		tableIndexDAO.deleteEntityData(mockProgressCallback, Lists.newArrayList(2L,3L));
		
		// setup some hierarchy.
		EntityDTO file1 = createEntityDTO(2L, EntityType.file, 2);
		file1.setParentId(333L);
		EntityDTO file2 = createEntityDTO(3L, EntityType.file, 3);
		file2.setParentId(222L);
		
		tableIndexDAO.addEntityData(mockProgressCallback, Lists.newArrayList(file1, file2));
		
		// both parents
		Set<Long> scope = Sets.newHashSet(file1.getParentId(), file2.getParentId());
		// Create the schema for this table
		List<ColumnModel> schema = createSchemaFromEntityDTO(file2);
		// Create the view index
		createOrUpdateTable(schema, tableId, isView);
		// Copy the entity data to the table
		tableIndexDAO.copyEntityReplicationToTable(tableId, ViewTypeMask.File.getMask(), scope, schema);
		// Query the results
		long count = tableIndexDAO.getRowCountForTable(tableId);
		assertEquals(2, count);
		// Check the CRC of the view
		long crc32 = tableIndexDAO.calculateCRC32ofTableView(tableId);
		assertEquals(381255304L, crc32);
	}
	
	/*
	 * PLFM-4336
	 */
	@Test
	public void testCopyEntityReplicationToTableScopeWithDoubleAnnotation(){
		isView = true;
		// delete all data
		tableIndexDAO.deleteEntityData(mockProgressCallback, Lists.newArrayList(2L,3L));
		
		// setup some hierarchy.
		EntityDTO file1 = createEntityDTO(2L, EntityType.file, 2);
		file1.setParentId(333L);
		AnnotationDTO double1 = new AnnotationDTO();
		double1.setKey("foo");
		double1.setValue("NaN");
		double1.setType(AnnotationType.DOUBLE);
		double1.setEntityId(2L);
		file1.setAnnotations(Arrays.asList(double1));
		EntityDTO file2 = createEntityDTO(3L, EntityType.file, 3);
		file2.setParentId(222L);
		AnnotationDTO double2 = new AnnotationDTO();
		double2.setKey("foo");
		double2.setValue("Infinity");
		double2.setType(AnnotationType.DOUBLE);
		double2.setEntityId(3L);
		file2.setAnnotations(Arrays.asList(double2));
		
		tableIndexDAO.addEntityData(mockProgressCallback, Lists.newArrayList(file1, file2));
		
		// both parents
		Set<Long> scope = Sets.newHashSet(file1.getParentId(), file2.getParentId());
		List<ColumnModel> schema = Lists.newArrayList(TableModelTestUtils
				.createColumn(1L, "foo", ColumnType.DOUBLE));
		// Create the view index
		createOrUpdateTable(schema, tableId, isView);
		// Copy the entity data to the table
		tableIndexDAO.copyEntityReplicationToTable(tableId, ViewTypeMask.File.getMask(), scope, schema);
		// Query the results
		long count = tableIndexDAO.getRowCountForTable(tableId);
		assertEquals(2, count);
	}

	@Test
	public void testCopyEntityReplicationToTableScopeEmpty(){
		isView = true;
		// delete all data
		tableIndexDAO.deleteEntityData(mockProgressCallback, Lists.newArrayList(2L,3L));
		
		// setup some hierarchy.
		EntityDTO file1 = createEntityDTO(2L, EntityType.file, 2);
		file1.setParentId(333L);
		EntityDTO file2 = createEntityDTO(3L, EntityType.file, 3);
		file2.setParentId(222L);
		
		tableIndexDAO.addEntityData(mockProgressCallback, Lists.newArrayList(file1, file2));
		
		// both parents
		Set<Long> scope = new HashSet<Long>();
		// Create the schema for this table
		List<ColumnModel> schema = createSchemaFromEntityDTO(file2);
		// Create the view index
		createOrUpdateTable(schema, tableId, isView);
		// Copy the entity data to the table
		tableIndexDAO.copyEntityReplicationToTable(tableId, ViewTypeMask.File.getMask(), scope, schema);
		// Query the results
		long count = tableIndexDAO.getRowCountForTable(tableId);
		assertEquals(0, count);
		// Check the CRC of the view
		long crc32 = tableIndexDAO.calculateCRC32ofTableView(tableId);
		assertEquals(-1L, crc32);
	}
	
	@Test
	public void testGetPossibleAnnotationsForContainers(){
		// delete all data
		tableIndexDAO.deleteEntityData(mockProgressCallback, Lists.newArrayList(2L,3L));
		
		// setup some hierarchy.
		EntityDTO file1 = createEntityDTO(2L, EntityType.file, 15);
		file1.setParentId(333L);
		EntityDTO file2 = createEntityDTO(3L, EntityType.file, 12);
		file2.setParentId(222L);
		
		tableIndexDAO.addEntityData(mockProgressCallback, Lists.newArrayList(file1, file2));
		
		Set<Long> containerIds = Sets.newHashSet(222L, 333L);
		long limit = 5;
		long offset = 0;
		List<ColumnModel> columns = tableIndexDAO.getPossibleColumnModelsForContainers(containerIds, ViewTypeMask.File.getMask(), limit, offset);
		assertNotNull(columns);
		assertEquals(limit, columns.size());
		// one
		ColumnModel cm = columns.get(0);
		assertEquals("key0", cm.getName());
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(new Long(1L), cm.getMaximumSize());
		// two
		cm = columns.get(1);
		assertEquals("key1", cm.getName());
		assertEquals(ColumnType.INTEGER, cm.getColumnType());
		assertEquals(null, cm.getMaximumSize());
		// three
		cm = columns.get(2);
		assertEquals("key10", cm.getName());
		assertEquals(ColumnType.DOUBLE, cm.getColumnType());
		assertEquals(null, cm.getMaximumSize());
	}
	
	/**
	 * Test added for PLFM-5034
	 */
	@Test
	public void testGetPossibleAnnotationsForContainersPLFM_5034(){
		// delete all data
		tableIndexDAO.deleteEntityData(mockProgressCallback, Lists.newArrayList(2L,3L));
		
		String duplicateName = "duplicate";
		
		// one
		EntityDTO file1 = createEntityDTO(2L, EntityType.file, 15);
		file1.getAnnotations().clear();
		file1.setParentId(333L);
		// add a string annotation with a size of 3
		AnnotationDTO annoDto = new AnnotationDTO();
		annoDto.setEntityId(file1.getId());
		annoDto.setKey(duplicateName);
		annoDto.setType(AnnotationType.STRING);
		annoDto.setValue("123");
		file1.getAnnotations().add(annoDto);
	
		// two
		EntityDTO file2 = createEntityDTO(3L, EntityType.file, 12);
		file2.getAnnotations().clear();
		file2.setParentId(222L);
		// add a long annotation with a size of 6
		annoDto = new AnnotationDTO();
		annoDto.setEntityId(file2.getId());
		annoDto.setKey(duplicateName);
		annoDto.setType(AnnotationType.LONG);
		annoDto.setValue("123456");
		file1.getAnnotations().add(annoDto);

		tableIndexDAO.addEntityData(mockProgressCallback, Lists.newArrayList(file1, file2));
		
		Set<Long> containerIds = Sets.newHashSet(222L, 333L);
		long limit = 5;
		long offset = 0;
		List<ColumnModel> columns = tableIndexDAO.getPossibleColumnModelsForContainers(containerIds, ViewTypeMask.File.getMask(), limit, offset);
		assertNotNull(columns);
		assertEquals(2, columns.size());
		// expected
		ColumnModel one = new ColumnModel();
		one.setName(duplicateName);
		one.setColumnType(ColumnType.STRING);
		one.setMaximumSize(6L);
		ColumnModel two = new ColumnModel();
		two.setName(duplicateName);
		two.setColumnType(ColumnType.INTEGER);
		two.setMaximumSize(null);
		Set<ColumnModel> expected = new HashSet<>(2);
		expected.add(one);
		expected.add(two);
		
		assertEquals(expected, new HashSet<>(columns));
	}
	
	@Test
	public void testExpandFromAggregation() {
		
		ColumnAggregation one = new ColumnAggregation();
		one.setColumnName("foo");
		one.setColumnTypeConcat(concatTypes(AnnotationType.STRING, AnnotationType.DOUBLE));
		one.setMaxSize(101L);
		
		ColumnAggregation two = new ColumnAggregation();
		two.setColumnName("bar");
		two.setColumnTypeConcat(concatTypes(AnnotationType.DOUBLE, AnnotationType.LONG));
		two.setMaxSize(0L);
		
		ColumnAggregation three = new ColumnAggregation();
		three.setColumnName("foobar");
		three.setColumnTypeConcat(concatTypes(AnnotationType.STRING));
		three.setMaxSize(202L);

		// call under test
		List<ColumnModel> results = TableIndexDAOImpl.expandFromAggregation(Lists.newArrayList(one,two,three));
		assertEquals(5, results.size());
		// zero
		ColumnModel cm = results.get(0);
		assertEquals("foo", cm.getName());
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(new Long(101), cm.getMaximumSize());
		// one
		cm = results.get(1);
		assertEquals("foo", cm.getName());
		assertEquals(ColumnType.DOUBLE, cm.getColumnType());
		assertEquals(null, cm.getMaximumSize());
		// two
		cm = results.get(2);
		assertEquals("bar", cm.getName());
		assertEquals(ColumnType.DOUBLE, cm.getColumnType());
		assertEquals(null, cm.getMaximumSize());
		// three
		cm = results.get(3);
		assertEquals("bar", cm.getName());
		assertEquals(ColumnType.INTEGER, cm.getColumnType());
		assertEquals(null, cm.getMaximumSize());
		// four
		cm = results.get(4);
		assertEquals("foobar", cm.getName());
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(new Long(202), cm.getMaximumSize());		
	}
	
	/**
	 * Helper to create a concatenated list of column types delimited with dot ('.')
	 * @param types
	 * @return
	 */
	public static String concatTypes(AnnotationType...types) {
		StringJoiner joiner = new StringJoiner(",");
		for(AnnotationType type: types) {
			joiner.add(type.name());
		}
		return joiner.toString();
	}
	
	@Test
	public void testGetPossibleAnnotationsForContainersProject(){
		// delete all data
		tableIndexDAO.deleteEntityData(mockProgressCallback, Lists.newArrayList(2L,3L));
		
		// setup some hierarchy.
		EntityDTO project1 = createEntityDTO(2L, EntityType.project, 15);
		project1.setParentId(111L);
		EntityDTO project2 = createEntityDTO(3L, EntityType.project, 12);
		project2.setParentId(111L);
		
		tableIndexDAO.addEntityData(mockProgressCallback, Lists.newArrayList(project1, project2));
		
		Set<Long> containerIds = Sets.newHashSet(2L, 3L);
		long limit = 5;
		long offset = 0;
		List<ColumnModel> columns = tableIndexDAO.getPossibleColumnModelsForContainers(containerIds, ViewTypeMask.Project.getMask(), limit, offset);
		assertNotNull(columns);
		assertEquals(limit, columns.size());
		// one
		ColumnModel cm = columns.get(0);
		assertEquals("key0", cm.getName());
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(new Long(1L), cm.getMaximumSize());
		// two
		cm = columns.get(1);
		assertEquals("key1", cm.getName());
		assertEquals(ColumnType.INTEGER, cm.getColumnType());
		assertEquals(null, cm.getMaximumSize());
		// three
		cm = columns.get(2);
		assertEquals("key10", cm.getName());
		assertEquals(ColumnType.DOUBLE, cm.getColumnType());
		assertEquals(null, cm.getMaximumSize());
	}
	
	@Test
	public void testGetSumOfChildCRCsForEachParent(){
		// delete all data
		tableIndexDAO.deleteEntityData(mockProgressCallback, Lists.newArrayList(2L,3L));
		
		Long parentOneId = 333L;
		Long parentTwoId = 222L;
		Long parentThreeId = 444L;
		// setup some hierarchy.
		EntityDTO file1 = createEntityDTO(2L, EntityType.file, 2);
		file1.setParentId(parentOneId);
		EntityDTO file2 = createEntityDTO(3L, EntityType.file, 3);
		file2.setParentId(parentTwoId);
		
		tableIndexDAO.addEntityData(mockProgressCallback, Lists.newArrayList(file1, file2));
		
		List<Long> parentIds = Lists.newArrayList(parentOneId,parentTwoId,parentThreeId);
		// call under test
		Map<Long, Long> results = tableIndexDAO.getSumOfChildCRCsForEachParent(parentIds);
		assertNotNull(results);
		assertEquals(2, results.size());
		assertEquals(new Long(122929132L), results.get(parentOneId));
		assertEquals(new Long(3592651982L), results.get(parentTwoId));
		assertEquals(null, results.get(parentThreeId));
	}
	
	@Test
	public void testGetSumOfChildCRCsForEachParentEmpty(){		
		List<Long> parentIds = new LinkedList<Long>();
		// call under test
		Map<Long, Long> results = tableIndexDAO.getSumOfChildCRCsForEachParent(parentIds);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	@Test
	public void testGetEntityChildren(){
		// delete all data
		tableIndexDAO.deleteEntityData(mockProgressCallback, Lists.newArrayList(2L,3L));
		
		Long parentOneId = 333L;
		Long parentTwoId = 222L;
		Long parentThreeId = 444L;
		// setup some hierarchy.
		EntityDTO file1 = createEntityDTO(2L, EntityType.file, 2);
		file1.setParentId(parentOneId);
		EntityDTO file2 = createEntityDTO(3L, EntityType.file, 3);
		file2.setParentId(parentTwoId);
		
		tableIndexDAO.addEntityData(mockProgressCallback, Lists.newArrayList(file1, file2));
		
		List<IdAndEtag> results = tableIndexDAO.getEntityChildren(parentOneId);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(new IdAndEtag(file1.getId(), file1.getEtag()), results.get(0));
		
		results = tableIndexDAO.getEntityChildren(parentTwoId);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(new IdAndEtag(file2.getId(), file2.getEtag()), results.get(0));
		
		results = tableIndexDAO.getEntityChildren(parentThreeId);
		assertNotNull(results);
		assertEquals(0, results.size());
	}
	
	@Test
	public void testReplicationExpiration() throws InterruptedException{
		tableIndexDAO.truncateReplicationSyncExpiration();
		Long one = 111L;
		Long two = 222L;
		Long three = 333L;
		List<Long> input = Lists.newArrayList(one,two,three);
		// call under test
		List<Long> expired = tableIndexDAO.getExpiredContainerIds(input);
		assertNotNull(expired);
		// all three should be expired
		assertEquals(Lists.newArrayList(one,two,three), expired);
		
		// Set two and three to expire in the future
		long now = System.currentTimeMillis();
		long timeout = 4 * 1000;
		long expires = now + timeout;
		// call under test
		tableIndexDAO.setContainerSynchronizationExpiration(Lists.newArrayList(two, three), expires);
		// set one to already be expired
		expires = now - 1;
		tableIndexDAO.setContainerSynchronizationExpiration(Lists.newArrayList(one), expires);
		// one should still be expired.
		expired = tableIndexDAO.getExpiredContainerIds(input);
		assertNotNull(expired);
		// all three should be expired
		assertEquals(Lists.newArrayList(one), expired);
		// wait for the two to expire
		Thread.sleep(timeout+1);
		// all three should be expired
		expired = tableIndexDAO.getExpiredContainerIds(input);
		assertNotNull(expired);
		// all three should be expired
		assertEquals(Lists.newArrayList(one,two,three), expired);
	}
	
	@Test
	public void testReplicationExpirationEmpty() throws InterruptedException{
		List<Long> empty = new LinkedList<Long>();
		// call under test
		List<Long> results  = tableIndexDAO.getExpiredContainerIds(empty);
		assertNotNull(results);
		assertTrue(results.isEmpty());
		Long expires = 0L;
		// call under test
		tableIndexDAO.setContainerSynchronizationExpiration(empty, expires);
	}
	
	@Test
	public void testArithmeticSelect() throws ParseException {
		// Create the table
		List<ColumnModel> doubleColumn = Lists.newArrayList(TableModelTestUtils
				.createColumn(1L, "col1", ColumnType.DOUBLE));
		createOrUpdateTable(doubleColumn, tableId, isView);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(doubleColumn, 1);
		// insert special values
		rows.get(0).getValues().set(0, "50");
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(doubleColumn);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(set, doubleColumn);
		// This is our query
		SqlQuery query = new SqlQueryBuilder("select 2 + 2, col1/10 from " + tableId, doubleColumn).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		System.out.println(results);
		assertNotNull(results.getRows());
		assertEquals(tableId, results.getTableId());
		assertEquals(1, results.getRows().size());
		assertEquals("4", results.getRows().get(0).getValues().get(0));
		assertEquals("5", results.getRows().get(0).getValues().get(1));
	}
	
	@Test
	public void testArithmeticPredicateRightHandSide() throws ParseException {
		// Create the table
		List<ColumnModel> doubleColumn = Lists.newArrayList(TableModelTestUtils
				.createColumn(1L, "col1", ColumnType.DOUBLE));
		createOrUpdateTable(doubleColumn, tableId, isView);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(doubleColumn, 1);
		// insert special values
		rows.get(0).getValues().set(0, "-50");
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(doubleColumn);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(set, doubleColumn);
		// This is our query
		SqlQuery query = new SqlQueryBuilder("select col1 from " + tableId+" where col1 = -5*10", doubleColumn).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		System.out.println(results);
		assertNotNull(results.getRows());
		assertEquals(tableId, results.getTableId());
		assertEquals(1, results.getRows().size());
		assertEquals("-50", results.getRows().get(0).getValues().get(0));
	}
	
	/**
	 * Test for PLFM-4575.
	 * @throws ParseException 
	 */
	@Test
	public void testDateTimeFunctions() throws ParseException{
		List<ColumnModel> schema = Lists.newArrayList(TableModelTestUtils
				.createColumn(1L, "aDate", ColumnType.DATE));
		createOrUpdateTable(schema, tableId, isView);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 2);
		// The first row is in the past
		long now = System.currentTimeMillis();
		long thritySecondsPast = now - (1000*30);
		long thritySecondsFuture = now + (1000*30);
		// first row is in the past
		rows.get(0).getValues().set(0, ""+thritySecondsPast);
		// second row is in the future
		rows.get(1).getValues().set(0, ""+thritySecondsFuture);
		// apply the rows
		createOrUpdateOrDeleteRows(rows, schema);
		
		// This is our query
		SqlQuery query = new SqlQueryBuilder("select aDate from " + tableId+" where aDate > unix_timestamp(CURRENT_TIMESTAMP - INTERVAL 1 SECOND)*1000", schema).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		System.out.println(results);
		assertNotNull(results.getRows());
		assertEquals(tableId, results.getTableId());
		assertEquals(1, results.getRows().size());
		assertEquals(""+thritySecondsFuture, results.getRows().get(0).getValues().get(0));
	}
	
	/**
	 * PLFM-4028 is an error that occurs when any type of column is changed 
	 * to a type of large text.
	 */
	@Test
	public void testPLFM_4028WithIndex(){
		ColumnModel oldColumn = TableModelTestUtils.createColumn(1L, "foo", ColumnType.INTEGER);
		List<ColumnModel> schema = Lists.newArrayList(oldColumn);
		createOrUpdateTable(schema, tableId, isView);
		int maxNumberOfIndices = 2;
		optimizeTableIndices(tableId, maxNumberOfIndices);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 2);
		// apply the rows
		createOrUpdateOrDeleteRows(rows, schema);
		
		// the new schema has a large text column with the same name
		ColumnModel newColumn = TableModelTestUtils.createColumn(1L, "foo", ColumnType.LARGETEXT);
		
		List<ColumnChangeDetails> changes = Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn));
		boolean alterTemp = false;
		alterTableAsNeeded(tableId, changes, alterTemp);
	}
	
	@Test
	public void testPLFM_4028WithoutIndex(){
		ColumnModel oldColumn = TableModelTestUtils.createColumn(1L, "foo", ColumnType.INTEGER);
		List<ColumnModel> schema = Lists.newArrayList(oldColumn);
		createOrUpdateTable(schema, tableId, isView);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 2);
		// apply the rows
		createOrUpdateOrDeleteRows(rows, schema);
		
		// the new schema has a large text column with the same name
		ColumnModel newColumn = TableModelTestUtils.createColumn(1L, "foo", ColumnType.LARGETEXT);
		
		List<ColumnChangeDetails> changes = Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn));
		boolean alterTemp = false;
		alterTableAsNeeded(tableId, changes, alterTemp);
	}
	
	/**
	 * Create update or delete the given rows in the current table.
	 * @param rows
	 * @param schema
	 */
	public void createOrUpdateOrDeleteRows(List<Row> rows, List<ColumnModel> schema){
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(schema);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(set, schema);
	}
	
	/**
	 * Create a view schema using an EntityDTO as a template.
	 * 
	 * @param dto
	 * @return
	 */
	public static List<ColumnModel> createSchemaFromEntityDTO(EntityDTO dto){
		List<ColumnModel> schema = new LinkedList<>();
		// add a column for each annotation
		if(dto.getAnnotations() != null){
			for(AnnotationDTO annoDto: dto.getAnnotations()){
				ColumnModel cm = new ColumnModel();
				cm.setColumnType(annoDto.getType().getColumnType());
				cm.setName(annoDto.getKey());
				if(ColumnType.STRING.equals(cm.getColumnType())){
					cm.setMaximumSize(50L);
				}
				schema.add(cm);
			}
		}
		// Add all of the default EntityFields
		schema.addAll(EntityField.getAllColumnModels());
		// assign each column an ID
		for(int i=0; i<schema.size(); i++){
			ColumnModel cm = schema.get(i);
			cm.setId(""+i);
		}
		return schema;
	}
	
	/**
	 * Helper to create populated EntityDTO.
	 * @param id
	 * @param type
	 * @param annotationCount
	 * @return
	 */
	private EntityDTO createEntityDTO(long id, EntityType type, int annotationCount){
		EntityDTO entityDto = new EntityDTO();
		entityDto.setId(id);
		entityDto.setCurrentVersion(2L);
		entityDto.setCreatedBy(222L);
		entityDto.setCreatedOn(new Date());
		entityDto.setEtag("etag"+id);
		entityDto.setName("name"+id);
		entityDto.setType(type);
		entityDto.setParentId(1L);
		entityDto.setBenefactorId(2L);
		entityDto.setProjectId(3L);
		entityDto.setModifiedBy(333L);
		entityDto.setModifiedOn(new Date());
		if(EntityType.file.equals(type)){
			entityDto.setFileHandleId(888L);
		}
		List<AnnotationDTO> annos = new LinkedList<AnnotationDTO>();
		for(int i=0; i<annotationCount; i++){
			AnnotationDTO annoDto = new AnnotationDTO();
			annoDto.setEntityId(id);
			annoDto.setKey("key"+i);
			annoDto.setType(AnnotationType.values()[i%AnnotationType.values().length]);
			annoDto.setValue(""+i);
			annos.add(annoDto);
		}
		if(!annos.isEmpty()){
			entityDto.setAnnotations(annos);
		}
		return entityDto;
	}
}
