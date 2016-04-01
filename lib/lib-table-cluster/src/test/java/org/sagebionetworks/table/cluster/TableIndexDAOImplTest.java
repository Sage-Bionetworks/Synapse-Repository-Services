package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.table.cluster.SQLUtils.TableType;
import org.sagebionetworks.table.cluster.TableIndexDAO.ColumnDefinition;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.util.ReflectionStaticTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

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
	
	ProgressCallback<Void> mockProgressCallback;

	String tableId;

	@Before
	public void before() {
		mockProgressCallback = Mockito.mock(ProgressCallback.class);
		// Only run this test if the table feature is enabled.
		Assume.assumeTrue(config.getTableEnabled());
		tableId = "syn" + new Random().nextInt(Integer.MAX_VALUE);
		// First get a connection for this table
		tableIndexDAO = tableConnectionFactory.getConnection(tableId);
	}

	@After
	public void after() {
		// Drop the table
		if (tableId != null && tableIndexDAO != null) {
			tableIndexDAO.deleteTable(tableId);
			tableIndexDAO.deleteSecondayTables(tableId);
		}
	}

	@Test
	public void testGetCurrentTableColumnsDoesNotExist() {
		// There should not be any columns for this table as it does not exist
		List<ColumnDefinition> columns = tableIndexDAO
				.getCurrentTableColumns(tableId);
		assertNull(columns);
	}

	@Test
	public void testCRUD() {
		// Create a Simple table with only a few columns
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		// Create the table
		boolean updated = tableIndexDAO.createOrUpdateTable(allTypes, tableId);
		assertTrue(
				"The table should not have existed so update should be true",
				updated);
		updated = tableIndexDAO.createOrUpdateTable(allTypes, tableId);
		assertFalse(
				"The table already existed in that state so it should not have been updated",
				updated);
		// Now we should be able to see the columns that were created
		List<ColumnDefinition> columns = tableIndexDAO
				.getCurrentTableColumns(tableId);
		// There should be a column for each column in the schema plus one
		// ROW_ID and one ROW_VERSION plus one extra for doubles.
		assertEquals(allTypes.size() + 2 + 1, columns.size());
		for (int i = 0; i < allTypes.size(); i++) {
			// Now remove a column and update the table
			ColumnModel removed = allTypes.remove(0);
			tableIndexDAO.createOrUpdateTable(allTypes, tableId);
			// Now we should be able to see the columns that were created
			columns = tableIndexDAO.getCurrentTableColumns(tableId);
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
			tableIndexDAO.createOrUpdateTable(allTypes, tableId);
			// Now we should be able to see the columns that were created
			columns = tableIndexDAO.getCurrentTableColumns(tableId);
			// There should be a column for each column in the schema plus one
			// ROW_ID and one ROW_VERSION.
			assertEquals("readded " + removed, allTypes.size() + 2 + 1,
					columns.size());
		}
		// Now delete the table
		assertTrue(tableIndexDAO.deleteTable(tableId));
		columns = tableIndexDAO.getCurrentTableColumns(tableId);
		assertEquals(null, columns);
	}

	@Test
	public void testCreateOrUpdateRows() {
		// Create a Simple table with only a few columns
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		List<Row> rows = TableModelTestUtils.createRows(allTypes, 5);
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getSelectColumns(allTypes, false));
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Create the table
		tableIndexDAO.createOrUpdateTable(allTypes, tableId);
		// Now fill the table with data
		tableIndexDAO.createOrUpdateOrDeleteRows(set, allTypes);
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
						"syn123.3", "link2", "largeText"));
		rows.get(4).setVersionNumber(5L);
		// This should not fail
		tableIndexDAO.createOrUpdateOrDeleteRows(set, allTypes);
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
		assertEquals("syn123.3", row.get("_C6_"));
		assertEquals("largeText", row.get("_C8_"));
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
		tableIndexDAO.createOrUpdateTable(allTypes, tableId);
		// the table now exists
		count = tableIndexDAO.getRowCountForTable(tableId);
		assertEquals("The row count should be 0 when the table is empty",
				new Long(0), count);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(allTypes, 4);
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getSelectColumns(allTypes, false));
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		tableIndexDAO.createOrUpdateOrDeleteRows(set, allTypes);
		// Check again
		count = tableIndexDAO.getRowCountForTable(tableId);
		assertEquals(new Long(rows.size()), count);
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

		tableIndexDAO.deleteSecondayTables(tableId);
	}
	
	@Test
	public void testGetSchemaHashForTable(){
		tableIndexDAO.createSecondaryTables(tableId);
		// Before the table exists the max version should be -1L
		String hash = tableIndexDAO.getCurrentSchemaMD5Hex(tableId);
		assertEquals("DEFAULT", hash);
		
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
	public void testSimpleQuery() throws ParseException {
		// Create the table
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		tableIndexDAO.createOrUpdateTable(allTypes, tableId);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(allTypes, 2);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(allTypes,
				false);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		tableIndexDAO.createOrUpdateOrDeleteRows(set, allTypes);
		// This is our query
		SqlQuery query = new SqlQuery("select * from " + tableId, allTypes);
		// Now query for the results
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
		List<String> expectedValues = Arrays.asList("string0", "341003.12",
				"203000", "false", "404000", "505000", "syn606000.607000",
				"link708000", "largeText804000");
		assertEquals(expectedValues, row.getValues());
		// Second row
		row = results.getRows().get(1);
		assertNotNull(row);
		assertEquals(new Long(101), row.getRowId());
		assertEquals(new Long(3), row.getVersionNumber());
		expectedValues = Arrays.asList("string1", "341006.53", "203001",
				"true", "404001", "505001", "syn606001.607001", "link708001", "largeText804001");
		assertEquals(expectedValues, row.getValues());
		// progress should be made for each row.
		verify(mockProgressCallback, times(2)).progressMade(null);
		// must also be able to run the query with a null callback
		mockProgressCallback = null;
	}

	private StackConfiguration oldStackConfiguration = null;

	@Test
	@Ignore
	public void testLargeTableReverse() throws ParseException {
		oldStackConfiguration = StackConfiguration.singleton();
		StackConfiguration mockedStackConfiguration = Mockito
				.spy(oldStackConfiguration);
		stub(mockedStackConfiguration.getTableAllIndexedEnabled()).toReturn(!oldStackConfiguration
						.getTableAllIndexedEnabled());
		ReflectionTestUtils.setField(StackConfiguration.singleton(),
				"singleton", mockedStackConfiguration);

		testLargeTable();

		try {
		} finally {
			if (oldStackConfiguration != null) {
				ReflectionTestUtils.setField(StackConfiguration.singleton(),
						"singleton", oldStackConfiguration);
			}
		}
	}

	@Test
	@Ignore
	public void testLargeTableJustInTime() throws ParseException {
		oldStackConfiguration = StackConfiguration.singleton();
		StackConfiguration mockedStackConfiguration = Mockito
				.spy(oldStackConfiguration);
		// stub(mockedStackConfiguration.getTableJustInTimeIndexedEnabled()).toReturn(new
		// ImmutablePropertyAccessor<Boolean>(true));
		ReflectionTestUtils.setField(StackConfiguration.singleton(),
				"singleton", mockedStackConfiguration);

		testLargeTable();

		try {
		} finally {
			if (oldStackConfiguration != null) {
				ReflectionTestUtils.setField(StackConfiguration.singleton(),
						"singleton", oldStackConfiguration);
			}
		}
	}

	@Test
	@Ignore
	public void testLargeTable() throws ParseException {
		// Create the table
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		tableIndexDAO.createOrUpdateTable(allTypes, tableId);
		// Now add some data
		long startTime = System.currentTimeMillis();
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(allTypes,
				false);
		final int endgoal = 10000000;
		final int batchsize = 100000;
		final int distinctCount = 100;
		for (int i = 0; i < endgoal / batchsize; i++) {
			System.out.print(i);
			List<Row> rows = Lists.newArrayListWithCapacity(batchsize);
			for (int j = 0; j < batchsize; j += distinctCount) {
				rows.addAll(TableModelTestUtils.createRows(allTypes,
						distinctCount));
			}
			RowSet set = new RowSet();
			set.setRows(rows);
			set.setHeaders(headers);
			set.setTableId(tableId);
			IdRange range = new IdRange();
			range.setMinimumId(100L + i * batchsize);
			range.setMaximumId(100L + i * batchsize + batchsize);
			range.setVersionNumber(3L + i);
			TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
			// Now fill the table with data
			tableIndexDAO.createOrUpdateOrDeleteRows(set, allTypes);
		}
		System.out.println("");

		List<Object> times = Lists.newArrayList();

		times.add("Loading");
		times.add(System.currentTimeMillis() - startTime);

		runTest(allTypes, endgoal, distinctCount, times);
		runTest(allTypes, endgoal, distinctCount, times);

		System.err.println("All indexes: "
				+ StackConfiguration.singleton().getTableAllIndexedEnabled());
		// System.err.println("Just in time indexes: " +
		// StackConfiguration.singleton().getTableJustInTimeIndexedEnabled().get());
		for (int i = 0; i < times.size(); i += 2) {
			System.err.println(times.get(i) + ": "
					+ ((Long) times.get(i + 1) / 1000L));
		}
	}

	private void runTest(List<ColumnModel> allTypes, final int endgoal,
			final int distinctCount, List<Object> times) throws ParseException {
		long now;
		long startTime = System.currentTimeMillis();

		SqlQuery query;
		RowSet results;

		query = new SqlQuery("select distinct * from " + tableId, allTypes);
		results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		assertEquals(distinctCount, results.getRows().size());
		now = System.currentTimeMillis();
		times.add("Distinct");
		times.add(now - startTime);
		startTime = now;

		query = new SqlQuery("select distinct * from " + tableId, allTypes);
		results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		assertEquals(distinctCount, results.getRows().size());
		now = System.currentTimeMillis();
		times.add("Distinct2");
		times.add(now - startTime);
		startTime = now;

		// if
		// (StackConfiguration.singleton().getTableJustInTimeIndexedEnabled().get())
		// {
		// tableIndexDAO.addIndex(tableId, allTypes.get(0));
		// }
		query = new SqlQuery("select * from " + tableId + " where "
				+ allTypes.get(0).getName() + " = '"
				+ results.getRows().get(0).getValues().get(0) + "'", allTypes);
		// Now query for the results
		results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		now = System.currentTimeMillis();
		times.add("Select");
		times.add(now - startTime);
		startTime = now;

		query = new SqlQuery("select * from " + tableId + " limit 20", allTypes);
		// Now query for the results
		results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		now = System.currentTimeMillis();
		times.add("Limit");
		times.add(now - startTime);
		startTime = now;

		// if
		// (StackConfiguration.singleton().getTableJustInTimeIndexedEnabled().get())
		// {
		// tableIndexDAO.addIndex(tableId, allTypes.get(1));
		// }
		query = new SqlQuery("select * from " + tableId + " order by "
				+ allTypes.get(1).getName() + " asc limit 20", allTypes);
		// Now query for the results
		results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		now = System.currentTimeMillis();
		times.add("Limit sort asc");
		times.add(now - startTime);
		startTime = now;

		// if
		// (StackConfiguration.singleton().getTableJustInTimeIndexedEnabled().get())
		// {
		// tableIndexDAO.addIndex(tableId, allTypes.get(2));
		// }
		query = new SqlQuery("select * from " + tableId + " order by "
				+ allTypes.get(2).getName() + " desc limit 20", allTypes);
		// Now query for the results
		results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		now = System.currentTimeMillis();
		times.add("Limit sort desc");
		times.add(now - startTime);
		startTime = now;

		query = new SqlQuery("select count(*) from " + tableId, allTypes);
		// Now query for the results
		results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		assertEquals("" + endgoal, results.getRows().get(0).getValues().get(0));
		now = System.currentTimeMillis();
		times.add("Count");
		times.add(now - startTime);
		startTime = now;
	}

	@Test
	public void testDoubleQuery() throws ParseException {
		// Create the table
		List<ColumnModel> doubleColumn = Lists.newArrayList(TableModelTestUtils
				.createColumn(1L, "col1", ColumnType.DOUBLE));
		tableIndexDAO.createOrUpdateTable(doubleColumn, tableId);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(doubleColumn, 5);
		// insert special values
		rows.get(1).getValues().set(0, "0");
		rows.get(2).getValues().set(0, "NaN");
		rows.get(3).getValues().set(0, "Infinity");
		rows.get(4).getValues().set(0, "-Infinity");
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(
				doubleColumn, false);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		tableIndexDAO.createOrUpdateOrDeleteRows(set, doubleColumn);
		// This is our query
		SqlQuery query = new SqlQuery("select * from " + tableId, doubleColumn);
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
		tableIndexDAO.createOrUpdateTable(allTypes, tableId);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(allTypes, 4);
		Row deletion = new Row();
		rows.add(deletion);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(allTypes,
				false);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setMaximumUpdateId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		tableIndexDAO.createOrUpdateOrDeleteRows(set, allTypes);

		// now delete the second and fourth row
		set.getRows().remove(0);
		set.getRows().remove(1);
		set.getRows().get(0).getValues().clear();
		set.getRows().get(1).getValues().clear();
		range.setVersionNumber(4L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		tableIndexDAO.createOrUpdateOrDeleteRows(set, allTypes);
		// This is our query
		SqlQuery query = new SqlQuery("select * from " + tableId, allTypes);
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
		tableIndexDAO.createOrUpdateTable(allTypes, tableId);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createNullRows(allTypes, 2);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(allTypes,
				false);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		tableIndexDAO.createOrUpdateOrDeleteRows(set, allTypes);
		// Now query for the results
		SqlQuery query = new SqlQuery("select * from " + tableId, allTypes);
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
				null, null, null, null,  null);
		assertEquals(expectedValues, row.getValues());
		// Second row
		row = results.getRows().get(1);
		assertNotNull(row);
		assertEquals(new Long(101), row.getRowId());
		assertEquals(new Long(3), row.getVersionNumber());
		expectedValues = Arrays.asList(null, null, null, null, null, null,
				null, null, null);
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
		tableIndexDAO.createOrUpdateTable(allTypes, tableId);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(allTypes, 2);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(allTypes,
				false);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		tableIndexDAO.createOrUpdateOrDeleteRows(set, allTypes);
		// Now a count query
		SqlQuery query = new SqlQuery("select count(*) from " + tableId,
				allTypes);
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
		tableIndexDAO.createOrUpdateTable(schema, tableId);
		// Create some data
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 100);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(schema,
				false);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(4L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		tableIndexDAO.createOrUpdateOrDeleteRows(set, schema);
		// Now create the query
		SqlQuery query = new SqlQuery(
				"select foo, bar from "
						+ tableId
						+ " where foo is not null group by foo order by bar desc limit 1 offset 0",
				schema);
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		assertEquals(TableModelUtils.getSelectColumns(schema, true),
				results.getHeaders());
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
		tableIndexDAO.createOrUpdateTable(schema, tableId);
		// Create some data
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 100);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(schema,
				false);
		set.setHeaders(headers);
		set.setTableId(tableId);
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(4L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		tableIndexDAO.createOrUpdateOrDeleteRows(set, schema);
		// Now create the query
		SqlQuery query = new SqlQuery("select * from " + tableId
				+ " where ROW_ID = 104 AND Row_Version > 1 limit 1 offset 0",
				schema);
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
		tableIndexDAO.createOrUpdateTable(schema, tableId);
		if (StackConfiguration.singleton().getTableAllIndexedEnabled()) {
			checkIndexes(tableId, indexes.toArray(new String[0]));
		}
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
		tableIndexDAO.createOrUpdateTable(schema, tableId);
		if (StackConfiguration.singleton().getTableAllIndexedEnabled()) {
			checkIndexes(tableId, indexes.toArray(new String[0]));
		}

		// replace 10 columns
		for (int i = 30; i < 40; i++) {
			ColumnModel cm = schema.get(i);
			cm.setId("333" + i);
			indexes.set(i + 1, SQLUtils.getColumnNameForId(cm.getId()));
		}

		tableIndexDAO.createOrUpdateTable(schema, tableId);
		if (StackConfiguration.singleton().getTableAllIndexedEnabled()) {
			checkIndexes(tableId, indexes.toArray(new String[0]));
		}

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
		tableIndexDAO.createOrUpdateTable(schema, tableId);
		if (StackConfiguration.singleton().getTableAllIndexedEnabled()) {
			checkIndexes(tableId, indexes.toArray(new String[0]));
		}
	}

	private void checkIndexes(String tableId, final String... indexes)
			throws Exception {
		JdbcTemplate template = (JdbcTemplate) ReflectionStaticTestUtils
				.getField(tableIndexDAO, "template");
		String tableName = SQLUtils.getTableNameForId(tableId,
				SQLUtils.TableType.INDEX);

		// Bind variables do not seem to work here
		final AtomicInteger count = new AtomicInteger(0);
		template.query("show columns from " + tableName, new RowMapper<Void>() {
			@Override
			public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
				boolean hasIndex = !StringUtils.isEmpty(rs.getString("Key"));
				String column = rs.getString("Field");
				if (hasIndex) {
					count.incrementAndGet();
					assertTrue("Index on " + column, Arrays.asList(indexes)
							.contains(column));
				}
				return null;
			}
		});
		assertEquals(indexes.length, count.get());
	}
	
	@Test
	public void testCreateSecondaryTablesCreateDeleteIdempotent(){
		// ensure the secondary tables for this index exist
		this.tableIndexDAO.createSecondaryTables(tableId);
		// must be able to call this again.
		this.tableIndexDAO.createSecondaryTables(tableId);
		// The delete must also be idempotent
		this.tableIndexDAO.deleteSecondayTables(tableId);
		// must be able to call this again
		this.tableIndexDAO.deleteSecondayTables(tableId);
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
		this.tableIndexDAO.deleteSecondayTables(tableId);
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
}
