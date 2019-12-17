package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.SparseRowDto;

import com.google.common.collect.Lists;

import au.com.bytecode.opencsv.CSVReader;

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
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, false, null);
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
	public void testCSVToRowIteratorNullHeaders() throws IOException {
		List<String[]> input = new ArrayList<String[]>();
		
		input.add(null);
		
		CSVReader reader = TableModelTestUtils.createReader(input);
		
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames(
				"a", "b", "c");
		
		columns.get(0).setColumnType(ColumnType.STRING);
		columns.get(1).setColumnType(ColumnType.INTEGER);
		columns.get(2).setColumnType(ColumnType.BOOLEAN);
		
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new CSVToRowIterator(columns, reader, true, null);
		});
		
		assertEquals("Expected the first line to be the header but was empty.", ex.getMessage());
		
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
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, isFirstLineHeader, null);
		List<SparseRowDto> asList = readAll(iterator);
		assertEquals(3, asList.size(), "The header should not be included in the results");
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
		
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Should throw an exception since the headers are missing.
			new CSVToRowIterator(columns, reader, true, null);
		});
		
		assertEquals("The first line is expected to be a header but the values do not match the names of of the columns of the table (AAA is not a valid column name or id). Header row: AAA,2,true", ex.getMessage());
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
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, true, null);
		List<SparseRowDto> asList = readAll(iterator);
		assertEquals(3, asList.size(), "The header should not be included in the results");
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
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, true, null);
		List<SparseRowDto> asList = readAll(iterator);
		assertEquals(3, asList.size(), "The header should not be included in the results");
		// zero
		SparseRowDto row = asList.get(0);
		assertEquals(new Long(1), row.getRowId(), "RowId should have been set.");
		assertEquals(new Long(11), row.getVersionNumber(), "RowVersion should have been set.");
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());

		// middle
		row = asList.get(1);
		assertEquals(null, row.getRowId(), "RowId should have been set.");
		assertEquals(null, row.getVersionNumber(), "RowVersion should have been set.");
		assertNotNull(row.getValues());
		assertEquals(3, row.getValues().size());
		
		// last
		row = asList.get(2);
		assertEquals(new Long(3), row.getRowId(), "RowId should have been set.");
		assertEquals(new Long(10), row.getVersionNumber(), "RowVersion should have been set.");
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
		List<ColumnModel> csvColumns = Lists.newArrayList(columns.get(2), columns.get(0));
		// Create the iterator.
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, true, null);
		List<SparseRowDto> rows = readAll(iterator);

		assertEquals(3, rows.size());
		assertEquals(TableModelTestUtils.createSparseRow(1L, 11L, csvColumns, "AAA", null), rows.get(0));
		assertEquals(TableModelTestUtils.createSparseRow(null, null, csvColumns, "CCC", "false"), rows.get(1));
		assertEquals(TableModelTestUtils.createDeletionSparseRow(3L, 10L), rows.get(2));
	}

	/**
	 * This case
	 * @throws IOException
	 */
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
		List<ColumnModel> csvColumns = Lists.newArrayList(columns.get(2), columns.get(0));
		// Create the iterator.
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, true, null);
		List<SparseRowDto> rows = readAll(iterator);

		assertEquals(3, rows.size());
		assertEquals(TableModelTestUtils.createSparseRow(1L, 11L, csvColumns,"AAA", null), rows.get(0));
		assertEquals(TableModelTestUtils.createSparseRow(null, null, csvColumns, "CCC", "false"), rows.get(1));
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
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, true, null);
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
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, isFirstLineHeader, linesToSkip);
		List<SparseRowDto> rows = readAll(iterator);

		/*
		 * For this case the first row should be skipped but the next two rows
		 * should be read correctly including ROW_ID and ROW_VERSION.
		 */
		assertEquals(2, rows.size());
		assertEquals(TableModelTestUtils.createSparseRow(1L, 11L, columns, "AAA"), rows.get(0));
		assertEquals(TableModelTestUtils.createSparseRow(2L, 11L, columns, "BBB"), rows.get(1));
	}
	
	@Test
	public void testCSVIncludeEtag() throws IOException{
		// Create a reader with some data.
		String csv =
			"ROW_ID,ROW_VERSION,ROW_ETAG,foo\n" +
			"1,11,etag1,AAA\n"+
			"2,11,etag2,BBB\n";
		
		CSVReader reader = new CSVReader(new StringReader(csv));
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames("foo");
		boolean isFirstLineHeader = true;
		Long linesToSkip = null;
		 
		// Create the iterator.
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, isFirstLineHeader, linesToSkip);
		List<SparseRowDto> rows = readAll(iterator);

		/*
		 * For this case the first row should be skipped but the next two rows
		 * should be read correctly including ROW_ID and ROW_VERSION.
		 */
		assertEquals(2, rows.size());
		assertEquals(TableModelTestUtils.createSparseRow(1L, 11L, "etag1", columns, "AAA"), rows.get(0));
		assertEquals(TableModelTestUtils.createSparseRow(2L, 11L, "etag2", columns, "BBB"), rows.get(1));
	}
	
	@Test
	public void testCSVIncludeEtagSkipFirstLine() throws IOException{
		// Create a reader with some data.
		String csv =
			"ROW_ID,ROW_VERSION,ROW_ETAG,foo\n" +
			"1,11,etag1,AAA\n"+
			"2,11,etag2,BBB\n";
		
		CSVReader reader = new CSVReader(new StringReader(csv));
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames("foo");
		boolean isFirstLineHeader = false;
		Long linesToSkip = 1L;
		 
		// Create the iterator.
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, isFirstLineHeader, linesToSkip);
		List<SparseRowDto> rows = readAll(iterator);

		/*
		 * For this case the first row should be skipped but the next two rows
		 * should be read correctly including ROW_ID, ROW_VERSION, and ROW_ETAG.
		 */
		assertEquals(2, rows.size());
		assertEquals(TableModelTestUtils.createSparseRow(1L, 11L, "etag1", columns, "AAA"), rows.get(0));
		assertEquals(TableModelTestUtils.createSparseRow(2L, 11L, "etag2", columns, "BBB"), rows.get(1));
	}
	
	/**
	 * The file used for this test was generated by Excel as a CSV UTF-8 export. The
	 * first three bytes of the resulting file match the UTF-8 byte order marker (0xEF,0xBB,0xBF).
	 * The CSV parser was not ignoring the marker so the first header name 'a' was prefixed with the
	 * marker so it did not match the headers of the table. 
	 * @throws IOException
	 */
	@Test
	public void testPLFM_5989() throws IOException{
		String fileName = "PLFM-5989.csv";
		String csvString = null;
		try(InputStream in = CSVToRowIteratorTest.class.getClassLoader().getResourceAsStream(fileName)){
			assertNotNull(in, "Cannot find: "+fileName+" on classpath");
			csvString = IOUtils.toString(in, StandardCharsets.UTF_8);
		}
		
		CSVReader reader = new CSVReader(new StringReader(csvString));
		// Create some columns
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames("a","b");
		boolean isFirstLineHeader = true;
		Long linesToSkip = 0L;
		 
		// Create the iterator.
		Iterator<SparseRowDto> iterator = new CSVToRowIterator(columns, reader, isFirstLineHeader, linesToSkip);
		List<SparseRowDto> rows = readAll(iterator);

		assertEquals(1, rows.size());
		assertEquals(TableModelTestUtils.createSparseRow(null, null, null, columns, "alsdjfsldkjflwdjf","3.3"), rows.get(0));
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
