package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.util.csv.CsvNullReader;

import static org.sagebionetworks.repo.model.table.TableConstants.*;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Tests for CSVToRowIterator.
 * 
 * @author John
 * 
 */
public class CSVToRowIteratorTest {

	@Test
	public void testCSVToRowIteratorNoHeaderRow() throws IOException {
		// Create a reader with some data.
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "AAA", "2", "true" });
		input.add(new String[] { "CCC", "3", "false" });
		input.add(new String[] { "FFF", "4", "true" });
		CsvNullReader reader = TableModelTestUtils.createReader(input);
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames(
				"a", "b", "c");
		columns.get(0).setColumnType(ColumnType.STRING);
		columns.get(1).setColumnType(ColumnType.INTEGER);
		columns.get(2).setColumnType(ColumnType.BOOLEAN);
		// Create the iterator.
		Iterator<Row> iterator = new CSVToRowIterator(columns, reader, false);
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
		input.add(new String[] { "a", "b", "c" });
		input.add(new String[] { "AAA", "2", "true" });
		input.add(new String[] { "CCC", "3", "false" });
		input.add(new String[] { "FFF", "4", "true" });
		CsvNullReader reader = TableModelTestUtils.createReader(input);
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames(
				"a", "b", "c");
		columns.get(0).setColumnType(ColumnType.STRING);
		columns.get(1).setColumnType(ColumnType.INTEGER);
		columns.get(2).setColumnType(ColumnType.BOOLEAN);
		// Create the iterator.
		Iterator<Row> iterator = new CSVToRowIterator(columns, reader, true);
		List<Row> asList = readAll(iterator);
		assertEquals("The header should not be included in the results", 3,
				asList.size());
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
		CsvNullReader reader = TableModelTestUtils.createReader(input);
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames(
				"a", "b", "c");
		columns.get(0).setColumnType(ColumnType.STRING);
		columns.get(1).setColumnType(ColumnType.INTEGER);
		columns.get(2).setColumnType(ColumnType.BOOLEAN);
		// Should throw an exception since the headers are missing.
		new CSVToRowIterator(columns, reader, true);
	}

	@Test
	public void testCSVToRowIteratorWithHeaderDifferentOrder()
			throws IOException {
		// Create a reader with some data.
		List<String[]> input = new ArrayList<String[]>(3);
		// This time add a header row.
		input.add(new String[] { "a", "b", "c" });
		input.add(new String[] { "AAA", "2", "true" });
		input.add(new String[] { "CCC", "3", "false" });
		input.add(new String[] { "FFF", "4", "true" });
		CsvNullReader reader = TableModelTestUtils.createReader(input);
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames(
				"c", "b", "a");
		columns.get(0).setColumnType(ColumnType.BOOLEAN);
		columns.get(1).setColumnType(ColumnType.INTEGER);
		columns.get(2).setColumnType(ColumnType.STRING);
		// Create the iterator.
		Iterator<Row> iterator = new CSVToRowIterator(columns, reader, true);
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
		input.add(new String[] { "a", "b", "c", ROW_ID, ROW_VERSION });
		input.add(new String[] { "AAA", "2", null, "1", "11" });
		input.add(new String[] { "CCC", "3", "false", null, null });
		input.add(new String[] { "FFF", null, "true", "3", "10" });
		CsvNullReader reader = TableModelTestUtils.createReader(input);
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames(
				"c", "b", "a");
		columns.get(0).setColumnType(ColumnType.BOOLEAN);
		columns.get(1).setColumnType(ColumnType.INTEGER);
		columns.get(2).setColumnType(ColumnType.STRING);
		// Create the iterator.
		Iterator<Row> iterator = new CSVToRowIterator(columns, reader, true);
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
