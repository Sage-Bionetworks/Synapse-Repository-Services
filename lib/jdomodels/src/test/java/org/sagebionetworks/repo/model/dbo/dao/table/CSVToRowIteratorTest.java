package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.SparseRowDto;

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.collect.Lists;

/**
 * Tests for CSVToRowIterator.
 * 
 * @author John
 * 
 */
public class CSVToRowIteratorTest {

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
		List<Map<String, String>> inputTranslate = TableModelTestUtils.mapInput(input, columns);
		// Create the iterator.
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, false, null, null);
		// Since no header was provide, the values should remain in the same
		// order.
		List<SparseRowDto> asList = readAll(iterator);
		assertEquals(3, asList.size());
		// zero
		SparseRowDto row = asList.get(0);
		assertNull(row.getRowId());
		assertNull(row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
		assertEquals(inputTranslate.get(0), row.getValues());
		// last
		row = asList.get(2);
		assertNull(row.getRowId());
		assertNull(row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
		assertEquals(inputTranslate.get(2), row.getValues());
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
		CSVReader reader = TableModelTestUtils.createReader(input);
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames(
				"a", "b", "c");
		columns.get(0).setColumnType(ColumnType.STRING);
		columns.get(1).setColumnType(ColumnType.INTEGER);
		columns.get(2).setColumnType(ColumnType.BOOLEAN);
		List<Map<String, String>> inputTranslate = TableModelTestUtils.mapInput(input, columns);
		boolean isFirstLineHeader= true;
		// Create the iterator.
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, isFirstLineHeader, null, null);
		List<SparseRowDto> asList = readAll(iterator);
		assertEquals("The header should not be included in the results", 3,
				asList.size());
		// zero
		SparseRowDto row = asList.get(0);
		assertNull(row.getRowId());
		assertNull(row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
		assertEquals(inputTranslate.get(1), row.getValues());
		// last
		row = asList.get(2);
		assertNull(row.getRowId());
		assertNull(row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
		assertEquals(inputTranslate.get(3), row.getValues());
	}
	
	@Test
	public void testCSVToRowIteratorWithColumnIds() throws IOException {
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
		List<Map<String, String>> inputTranslate = TableModelTestUtils.mapInput(input, columns);
		
		boolean isFirstLineHeader= false;
		List<String> columnIds = Lists.newArrayList(columns.get(0).getId(), columns.get(1).getId(), columns.get(2).getId());
		// Create the iterator.
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, isFirstLineHeader, columnIds, null);
		List<SparseRowDto> asList = readAll(iterator);
		assertEquals("The header should not be included in the results", 3,
				asList.size());
		// zero
		SparseRowDto row = asList.get(0);
		assertNull(row.getRowId());
		assertNull(row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
		assertEquals(inputTranslate.get(0), row.getValues());
		// last
		row = asList.get(2);
		assertNull(row.getRowId());
		assertNull(row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
		assertEquals(inputTranslate.get(2), row.getValues());
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
		List<Map<String, String>> inputTranslate = TableModelTestUtils.mapInput(input, columns);
		// Create the iterator.
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, true, Lists.newArrayList(columns.get(0).getId(), columns.get(1)
				.getId(), columns.get(2).getId()), null);
		List<SparseRowDto> asList = readAll(iterator);
		assertEquals("The header should not be included in the results", 3, asList.size());
		// zero
		SparseRowDto row = asList.get(0);
		assertNull(row.getRowId());
		assertNull(row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
		assertEquals(inputTranslate.get(1), row.getValues());
		// last
		row = asList.get(2);
		assertNull(row.getRowId());
		assertNull(row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
		assertEquals(inputTranslate.get(3), row.getValues());
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
		new CSVToRowIterator(columns, reader, true, null, null);
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
		new CSVToRowIterator(columns, reader, false, Lists.newArrayList("111", "222", "444"), null);
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
		CSVReader reader = TableModelTestUtils.createReader(input);
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames(
				"c", "b", "a");
		columns.get(0).setColumnType(ColumnType.BOOLEAN);
		columns.get(1).setColumnType(ColumnType.INTEGER);
		columns.get(2).setColumnType(ColumnType.STRING);
		// Create the iterator.
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, true, null, null);
		List<SparseRowDto> asList = readAll(iterator);
		assertEquals("The header should not be included in the results", 3,
				asList.size());
		// zero
		SparseRowDto row = asList.get(0);
		assertNull(row.getRowId());
		assertNull(row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());

		// last
		row = asList.get(2);
		assertNull(row.getRowId());
		assertNull(row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
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
		CSVReader reader = TableModelTestUtils.createReader(input);
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames(
				"c", "b", "a");
		columns.get(0).setColumnType(ColumnType.BOOLEAN);
		columns.get(1).setColumnType(ColumnType.INTEGER);
		columns.get(2).setColumnType(ColumnType.STRING);

		// Create the iterator.
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, true, null, null);
		List<SparseRowDto> asList = readAll(iterator);
		assertEquals("The header should not be included in the results", 3,
				asList.size());
		// zero
		SparseRowDto row = asList.get(0);
		assertEquals("RowId should have been set.", new Long(1), row.getRowId());
		assertEquals("RowVersion should have been set.",new Long(11), row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());

		// middle
		row = asList.get(1);
		assertEquals("RowId should have been set.", null, row.getRowId());
		assertEquals("RowVersion should have been set.",null, row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
		
		// last
		row = asList.get(2);
		assertEquals("RowId should have been set.", new Long(3), row.getRowId());
		assertEquals("RowVersion should have been set.",new Long(10), row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
	}

	@Test
	public void testCSVToRowIteratorWithHeadersIgnoreColumns() throws IOException {
		// Create a reader with some data.
		String csv = 
			"ROW_ID,ROW_VERSION,c,,a\n" +
			"1,11,AAA,2,\n" +
			",,CCC,3,false\n" +
			"3,10\n";
		CSVReader reader = new CSVReader(new StringReader(csv));
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames("a", "b", "c");
		// Create the iterator.
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, true, null, null);
		List<SparseRowDto> rows = readAll(iterator);

		assertEquals(3, rows.size());
		assertEquals(TableModelTestUtils.createSparseRow(1L, 11L, columns, null, null, "AAA"), rows.get(0));
		assertEquals(TableModelTestUtils.createSparseRow(null, null, columns, "false", null, "CCC"), rows.get(1));
		assertEquals(TableModelTestUtils.createDeletionSparseRow(3L, 10L), rows.get(2));
	}

	@Test
	public void testCSVToRowIteratorWithNotAllHeaders() throws IOException {
		// Create a reader with some data.
		String csv = 
			"ROW_ID,ROW_VERSION,c,a\n" +
			"1,11,AAA,\n" +
			",,CCC,false\n" +
			"3,10\n";
		CSVReader reader = new CSVReader(new StringReader(csv));
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames("a", "b", "c");
		// Create the iterator.
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, true, null, null);
		List<SparseRowDto> rows = readAll(iterator);

		assertEquals(3, rows.size());
		assertEquals(TableModelTestUtils.createSparseRow(1L, 11L, columns, null, null, "AAA"), rows.get(0));
		assertEquals(TableModelTestUtils.createSparseRow(null, null, columns, "false", null, "CCC"), rows.get(1));
		assertEquals(TableModelTestUtils.createDeletionSparseRow(3L, 10L), rows.get(2));
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
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, true, Lists.newArrayList("ROW_ID", "ROW_VERSION", columns.get(2)
				.getId(), null, columns.get(0).getId()), null);
		List<SparseRowDto> rows = readAll(iterator);

		assertEquals(3, rows.size());
		assertEquals(TableModelTestUtils.createSparseRow(1L, 11L, columns, null, null, "AAA"), rows.get(0));
		assertEquals(TableModelTestUtils.createSparseRow(null, null, columns, "false", null, "CCC"), rows.get(1));
		assertEquals(TableModelTestUtils.createDeletionSparseRow(3L, 10L), rows.get(2));
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
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, true, null, null);
		List<SparseRowDto> rows = readAll(iterator);

		assertEquals(3, rows.size());
		assertEquals(TableModelTestUtils.createSparseRow(1L, 11L, columns, null, "2", "AAA"), rows.get(0));
		assertEquals(TableModelTestUtils.createSparseRow(null, null, columns, "false", "3", "CCC"), rows.get(1));
		assertEquals(TableModelTestUtils.createDeletionSparseRow(3L, 10L), rows.get(2));
	}
	
	@Test
	public void testCSVToRowIteratorPLFM_3155() throws IOException {
		// Create a reader with some data.
		String csv =
			"ROW_ID,ROW_VERSION,bar\n" +
			"1,11,AAA\n"+
			"2,11,BBB\n";
		
		CSVReader reader = new CSVReader(new StringReader(csv));
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames("foo");
		boolean isFirstLineHeader = false;
		Long linesToSkip = 1L;
		 
		// Create the iterator.
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, isFirstLineHeader, null, linesToSkip);
		List<SparseRowDto> rows = readAll(iterator);

		/*
		 * For this case the first row should be skipped but the next two rows
		 * should be read correctly including ROW_ID and ROW_VERSION.
		 */
		assertEquals(2, rows.size());
		assertEquals(TableModelTestUtils.createSparseRow(1L, 11L, columns, "AAA"), rows.get(0));
		assertEquals(TableModelTestUtils.createSparseRow(2L, 11L, columns, "BBB"), rows.get(1));
	}

	/**
	 * Read all data from the iterator into a list.
	 * 
	 * @param iterator
	 * @return
	 */
	private static List<SparseRowDto> readAll(Iterator<SparseRowDto> iterator) {
		List<SparseRowDto> list = new LinkedList<SparseRowDto>();
		while (iterator.hasNext()) {
			list.add(iterator.next());
		}
		return list;
	}
}
