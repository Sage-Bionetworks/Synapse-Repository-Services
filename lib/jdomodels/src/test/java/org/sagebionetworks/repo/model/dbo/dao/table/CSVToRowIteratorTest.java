package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sagebionetworks.csv.utils.CSVReader;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;

import com.google.common.collect.Lists;

/**
 * Tests for CSVToRowIterator.
 * 
 * @author John
 * 
 */
@RunWith(Parameterized.class)
public class CSVToRowIteratorTest {

	@Parameters
	public static List<Object[]> parameters() {
		return Lists.newArrayList(new Object[] { true }, new Object[] { false });
	}

	private final boolean useColumnIds;

	public CSVToRowIteratorTest(boolean useColumnIds) {
		this.useColumnIds = useColumnIds;
	}

	@Test
	public void testCSVToRowIteratorNoHeaders() throws IOException {
		// Create a reader with some data.
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "AAA", "2", "true" });
		input.add(new String[] { "CCC", "3", "false" });
		input.add(new String[] { "FFF", "4", "true" });
		CSVReader reader = TableModelTestUtils.createReader(input);
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames(
				"a", "b", "c");
		columns.get(0).setColumnType(ColumnType.STRING);
		columns.get(1).setColumnType(ColumnType.INTEGER);
		columns.get(2).setColumnType(ColumnType.BOOLEAN);
		// Create the iterator.
		Iterator<Row> iterator = new CSVToRowIterator(columns, reader, false, null);
		// Since no header was provide, the values should remain in the same
		// order.
		List<Row> asList = readAll(iterator);
		assertEquals(3, asList.size());
		// zero
		Row row = asList.get(0);
		assertNull(row.getRowId());
		assertNull(row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
		assertEquals(Arrays.asList(input.get(0)), row.getValues());
		// last
		row = asList.get(2);
		assertNull(row.getRowId());
		assertNull(row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
		assertEquals(Arrays.asList(input.get(2)), row.getValues());
	}

	@Test
	public void testCSVToRowIteratorWithHeaderSameOrder() throws IOException {
		// Create a reader with some data.
		List<String[]> input = new ArrayList<String[]>(3);
		// This time add a header row.
		if (!useColumnIds) {
			input.add(new String[] { "a", "b", "c" });
		}
		input.add(new String[] { "AAA", "2", "true" });
		input.add(new String[] { "CCC", "3", "false" });
		input.add(new String[] { "FFF", "4", "true" });
		CSVReader reader = TableModelTestUtils.createReader(input);
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames(
				"a", "b", "c");
		columns.get(0).setColumnType(ColumnType.STRING);
		columns.get(1).setColumnType(ColumnType.INTEGER);
		columns.get(2).setColumnType(ColumnType.BOOLEAN);
		// Create the iterator.
		Iterator<Row> iterator = new CSVToRowIterator(columns, reader, !useColumnIds, useColumnIds ? Lists.newArrayList(columns.get(0)
				.getId(), columns.get(1).getId(), columns.get(2).getId()) : null);
		List<Row> asList = readAll(iterator);
		assertEquals("The header should not be included in the results", 3,
				asList.size());
		// zero
		Row row = asList.get(0);
		assertNull(row.getRowId());
		assertNull(row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
		assertEquals(Arrays.asList(input.get(useColumnIds ? 0 : 1)), row.getValues());
		// last
		row = asList.get(2);
		assertNull(row.getRowId());
		assertNull(row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
		assertEquals(Arrays.asList(input.get(useColumnIds ? 2 : 3)), row.getValues());
	}

	@Test
	public void testCSVToRowIteratorWithBrokenHeaderAndColumnsInSameOrder() throws IOException {
		// Create a reader with some data.
		List<String[]> input = new ArrayList<String[]>(3);
		// This time add a header row.
		input.add(new String[] { "a-not", "b-not", "c-not" });
		input.add(new String[] { "AAA", "2", "true" });
		input.add(new String[] { "CCC", "3", "false" });
		input.add(new String[] { "FFF", "4", "true" });
		CSVReader reader = TableModelTestUtils.createReader(input);
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames("a", "b", "c");
		columns.get(0).setColumnType(ColumnType.STRING);
		columns.get(1).setColumnType(ColumnType.INTEGER);
		columns.get(2).setColumnType(ColumnType.BOOLEAN);
		// Create the iterator.
		Iterator<Row> iterator = new CSVToRowIterator(columns, reader, true, Lists.newArrayList(columns.get(0).getId(), columns.get(1)
				.getId(), columns.get(2).getId()));
		List<Row> asList = readAll(iterator);
		assertEquals("The header should not be included in the results", 3, asList.size());
		// zero
		Row row = asList.get(0);
		assertNull(row.getRowId());
		assertNull(row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
		assertEquals(Arrays.asList(input.get(1)), row.getValues());
		// last
		row = asList.get(2);
		assertNull(row.getRowId());
		assertNull(row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
		assertEquals(Arrays.asList(input.get(3)), row.getValues());
	}

	@Test (expected=IllegalArgumentException.class)
	public void testCSVToRowIteratorWithHeadersMissing() throws IOException {
		// Create a reader with some data.
		List<String[]> input = new ArrayList<String[]>(3);
		// This time add a header row.
		input.add(new String[] { "AAA", "2", "true" });
		input.add(new String[] { "CCC", "3", "false" });
		input.add(new String[] { "FFF", "4", "true" });
		CSVReader reader = TableModelTestUtils.createReader(input);
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames(
				"a", "b", "c");
		columns.get(0).setColumnType(ColumnType.STRING);
		columns.get(1).setColumnType(ColumnType.INTEGER);
		columns.get(2).setColumnType(ColumnType.BOOLEAN);
		// Should throw an exception since the headers are missing.
		new CSVToRowIterator(columns, reader, true, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCSVToRowIteratorWithColumnIdsWrong() throws IOException {
		// Create a reader with some data.
		List<String[]> input = new ArrayList<String[]>(3);
		// This time add a header row.
		input.add(new String[] { "AAA", "2", "true" });
		input.add(new String[] { "CCC", "3", "false" });
		input.add(new String[] { "FFF", "4", "true" });
		CSVReader reader = TableModelTestUtils.createReader(input);
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames("a", "b", "c");
		columns.get(0).setColumnType(ColumnType.STRING);
		columns.get(1).setColumnType(ColumnType.INTEGER);
		columns.get(2).setColumnType(ColumnType.BOOLEAN);
		// Should throw an exception since the column ids are incorrect.
		new CSVToRowIterator(columns, reader, false, Lists.newArrayList("111", "222", "444"));
	}

	@Test
	public void testCSVToRowIteratorWithHeaderDifferentOrder()
			throws IOException {
		// Create a reader with some data.
		List<String[]> input = new ArrayList<String[]>(3);
		// This time add a header row.
		if (!useColumnIds) {
			input.add(new String[] { "a", "b", "c" });
		}
		input.add(new String[] { "AAA", "2", "true" });
		input.add(new String[] { "CCC", "3", "false" });
		input.add(new String[] { "FFF", "4", "true" });
		CSVReader reader = TableModelTestUtils.createReader(input);
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames(
				"c", "b", "a");
		columns.get(0).setColumnType(ColumnType.BOOLEAN);
		columns.get(1).setColumnType(ColumnType.INTEGER);
		columns.get(2).setColumnType(ColumnType.STRING);
		// Create the iterator.
		Iterator<Row> iterator = new CSVToRowIterator(columns, reader, !useColumnIds, useColumnIds ? Lists.newArrayList(columns.get(2)
				.getId(), columns.get(1).getId(), columns.get(0).getId()) : null);
		List<Row> asList = readAll(iterator);
		assertEquals("The header should not be included in the results", 3,
				asList.size());
		// zero
		Row row = asList.get(0);
		assertNull(row.getRowId());
		assertNull(row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
		assertEquals("The column order should match the input schema",
				Arrays.asList("true", "2", "AAA"), row.getValues());
		// last
		row = asList.get(2);
		assertNull(row.getRowId());
		assertNull(row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
		assertEquals("The column order should match the input schema",
				Arrays.asList("true", "4", "FFF"), row.getValues());
	}

	@Test
	public void testCSVToRowIteratorWithHeaderAndRowIdAndVersionId()
			throws IOException {
		// Create a reader with some data.
		List<String[]> input = new ArrayList<String[]>(3);
		// This time add a header row.
		if (!useColumnIds) {
			input.add(new String[] { "a", "b", "c", ROW_ID, ROW_VERSION });
		}
		input.add(new String[] { "AAA", "2", null, "1", "11" });
		input.add(new String[] { "CCC", "3", "false", null, null });
		input.add(new String[] { "FFF", null, "true", "3", "10" });
		CSVReader reader = TableModelTestUtils.createReader(input);
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames(
				"c", "b", "a");
		columns.get(0).setColumnType(ColumnType.BOOLEAN);
		columns.get(1).setColumnType(ColumnType.INTEGER);
		columns.get(2).setColumnType(ColumnType.STRING);
		// Create the iterator.
		Iterator<Row> iterator = new CSVToRowIterator(columns, reader, !useColumnIds, useColumnIds ? Lists.newArrayList(columns.get(2)
				.getId(), columns.get(1).getId(), columns.get(0).getId(), ROW_ID, ROW_VERSION) : null);
		List<Row> asList = readAll(iterator);
		assertEquals("The header should not be included in the results", 3,
				asList.size());
		// zero
		Row row = asList.get(0);
		assertEquals("RowId should have been set.", new Long(1), row.getRowId());
		assertEquals("RowVersion should have been set.",new Long(11), row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
		assertEquals("The column order should match the input schema",
				Arrays.asList(null, "2", "AAA"), row.getValues());
		// middle
		row = asList.get(1);
		assertEquals("RowId should have been set.", null, row.getRowId());
		assertEquals("RowVersion should have been set.",null, row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
		assertEquals("The column order should match the input schema",
				Arrays.asList("false", "3", "CCC"), row.getValues());
		
		// last
		row = asList.get(2);
		assertEquals("RowId should have been set.", new Long(3), row.getRowId());
		assertEquals("RowVersion should have been set.",new Long(10), row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
		assertEquals("The column order should match the input schema",
				Arrays.asList("true", null, "FFF"), row.getValues());
	}

	@Test
	public void testCSVToRowIteratorWithHeadersIgnoreColumns() throws IOException {
		// Create a reader with some data.
		String csv = useColumnIds ? "" :
			"ROW_ID,ROW_VERSION,c,,a\n";
		csv +=
			"1,11,AAA,2,\n" +
			",,CCC,3,false\n" +
			"3,10\n";
		CSVReader reader = new CSVReader(new StringReader(csv));
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames("a", "b", "c");
		// Create the iterator.
		Iterator<Row> iterator = new CSVToRowIterator(columns, reader, !useColumnIds, useColumnIds ? Lists.newArrayList("ROW_ID",
				"ROW_VERSION", columns.get(2).getId(), null, columns.get(0).getId()) : null);
		List<Row> rows = readAll(iterator);

		assertEquals(3, rows.size());
		assertEquals(TableModelTestUtils.createRow(1L, 11L, null, null, "AAA"), rows.get(0));
		assertEquals(TableModelTestUtils.createRow(null, null, "false", null, "CCC"), rows.get(1));
		assertEquals(TableModelTestUtils.createDeletionRow(3L, 10L), rows.get(2));
	}

	@Test
	public void testCSVToRowIteratorWithNotAllHeaders() throws IOException {
		// Create a reader with some data.
		String csv = useColumnIds ? "" :
			"ROW_ID,ROW_VERSION,c,a\n";
		csv +=
			"1,11,AAA,\n" +
			",,CCC,false\n" +
			"3,10\n";
		CSVReader reader = new CSVReader(new StringReader(csv));
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames("a", "b", "c");
		// Create the iterator.
		Iterator<Row> iterator = new CSVToRowIterator(columns, reader, !useColumnIds, useColumnIds ? Lists.newArrayList("ROW_ID",
				"ROW_VERSION", columns.get(2).getId(), columns.get(0).getId()) : null);
		List<Row> rows = readAll(iterator);

		assertEquals(3, rows.size());
		assertEquals(TableModelTestUtils.createRow(1L, 11L, null, null, "AAA"), rows.get(0));
		assertEquals(TableModelTestUtils.createRow(null, null, "false", null, "CCC"), rows.get(1));
		assertEquals(TableModelTestUtils.createDeletionRow(3L, 10L), rows.get(2));
	}

	@Test
	public void testCSVToRowIteratorWithColumnIdsIgnoreColumns() throws IOException {
		// Create a reader with some data.
		String csv =
			"ROW_ID,ROW_VERSION,aa,bb,cc\n" +
			"1,11,AAA,2,\n" +
			",,CCC,3,false\n" +
			"3,10\n";
		CSVReader reader = new CSVReader(new StringReader(csv));
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames("a", "b", "c");
		// Create the iterator.
		Iterator<Row> iterator = new CSVToRowIterator(columns, reader, true, Lists.newArrayList("ROW_ID", "ROW_VERSION", columns.get(2)
				.getId(), null, columns.get(0).getId()));
		List<Row> rows = readAll(iterator);

		assertEquals(3, rows.size());
		assertEquals(TableModelTestUtils.createRow(1L, 11L, null, null, "AAA"), rows.get(0));
		assertEquals(TableModelTestUtils.createRow(null, null, "false", null, "CCC"), rows.get(1));
		assertEquals(TableModelTestUtils.createDeletionRow(3L, 10L), rows.get(2));
	}

	@Test
	public void testCSVToRowIteratorReadDeletion() throws IOException {
		// Create a reader with some data.
		String csv =
			"ROW_ID,ROW_VERSION,a,b,c\n" +
			"1,11,AAA,2,\n"+
			",,CCC,3,false\n" +
			"3,10\n";
		CSVReader reader = new CSVReader(new StringReader(csv));
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames("c", "b", "a");
		// Create the iterator.
		Iterator<Row> iterator = new CSVToRowIterator(columns, reader, true, null);
		List<Row> rows = readAll(iterator);

		assertEquals(3, rows.size());
		assertEquals(TableModelTestUtils.createRow(1L, 11L, null, "2", "AAA"), rows.get(0));
		assertEquals(TableModelTestUtils.createRow(null, null, "false", "3", "CCC"), rows.get(1));
		assertEquals(TableModelTestUtils.createDeletionRow(3L, 10L), rows.get(2));
	}

	/**
	 * Read all data from the iterator into a list.
	 * 
	 * @param iterator
	 * @return
	 */
	private static List<Row> readAll(Iterator<Row> iterator) {
		List<Row> list = new LinkedList<Row>();
		while (iterator.hasNext()) {
			list.add(iterator.next());
		}
		return list;
	}
}
