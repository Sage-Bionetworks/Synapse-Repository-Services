package org.sagebionetworks.repo.manager.file.preview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

import com.amazonaws.util.StringInputStream;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class TabCsvTextPreviewTest {
	TabCsvPreviewGenerator tabPreviewGenerator, csvPreviewGenerator;

	@Before
	public void before() throws IOException, ServiceUnavailableException {
		tabPreviewGenerator = new TabCsvPreviewGenerator(TabCsvPreviewGenerator.TAB);
		csvPreviewGenerator = new TabCsvPreviewGenerator(TabCsvPreviewGenerator.COMMA);
	}

	@Test
	public void testTabGeneratePreview() throws IOException {
		String tabTestInputString = "\"some quoted text\"\t \"second\tcolumn\"\t third column\n2nd row first column\t2nd row second column\t2nd row third column";
		InputStream from = IOUtils.toInputStream(tabTestInputString);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PreviewOutputMetadata type = tabPreviewGenerator.generatePreview(from, baos);
		assertEquals(TabCsvPreviewGenerator.TEXT_CSV_SEPARATED_VALUES, type.getContentType());
		String output = baos.toString();
		String[] lines = output.split("\n");
		// input csv has 2 rows
		assertEquals(2, lines.length);
		// 3 columns
		assertEquals(3, lines[0].split(",").length);
	}

	@Test
	public void testMaxColumnsTabGeneratePreview() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < TabCsvPreviewGenerator.MAX_COLUMN_COUNT + 10; i++) {
			sb.append(i + "\t");
		}
		InputStream from = IOUtils.toInputStream(sb.toString());
		PreviewOutputMetadata type = tabPreviewGenerator.generatePreview(from, baos);
		assertEquals(TabCsvPreviewGenerator.TEXT_CSV_SEPARATED_VALUES, type.getContentType());
		String output = baos.toString();
		String[] lines = output.split("\n");
		// input csv has 1 row
		assertEquals(1, lines.length);
		// input has more than MAX_COLUMN_COUNT, so output should have been truncated to
		// MAX_COLUMN_COUNT columns (with an additional column that has "...")
		assertEquals(TabCsvPreviewGenerator.MAX_COLUMN_COUNT + 1, lines[0].split(",").length);
	}

	@Test
	public void testMaxRowsTabGeneratePreview() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < TabCsvPreviewGenerator.MAX_ROW_COUNT + 10; i++) {
			sb.append(i + "\n");
		}
		InputStream from = IOUtils.toInputStream(sb.toString());
		PreviewOutputMetadata type = tabPreviewGenerator.generatePreview(from, baos);
		assertEquals(TabCsvPreviewGenerator.TEXT_CSV_SEPARATED_VALUES, type.getContentType());
		String output = baos.toString();
		String[] lines = output.split("\n");
		// input csv has more than MAX_ROW_COUNT, so output should have been truncated
		// to MAX_ROW_COUNT+1 rows
		assertEquals(TabCsvPreviewGenerator.MAX_ROW_COUNT + 1, lines.length);
		// input csv has 1 column
		assertEquals(1, lines[0].split(",").length);
	}

	@Test
	public void testMaxColumnsAndRowsTabGeneratePreview() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < TabCsvPreviewGenerator.MAX_ROW_COUNT + 10; i++) {
			for (int j = 0; j < TabCsvPreviewGenerator.MAX_COLUMN_COUNT + 10; j++) {
				sb.append("row " + i + " column " + j + "\t");
			}
			sb.append("\n");
		}
		InputStream from = IOUtils.toInputStream(sb.toString());
		PreviewOutputMetadata type = tabPreviewGenerator.generatePreview(from, baos);
		assertEquals(TabCsvPreviewGenerator.TEXT_CSV_SEPARATED_VALUES, type.getContentType());
		String output = baos.toString();
		String[] lines = output.split("\n");
		// input csv has more than MAX_ROW_COUNT, so output should have been truncated
		// to MAX_ROW_COUNT+1 rows
		assertEquals(TabCsvPreviewGenerator.MAX_ROW_COUNT + 1, lines.length);
		// input csv has more than MAX_COLUMN_COUNT
		assertEquals(TabCsvPreviewGenerator.MAX_COLUMN_COUNT + 1, lines[0].split(",").length);
	}

	@Test
	public void testVaryingColumnLengthGeneratePreview() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 5; i++) {
			sb.append("row 0 column " + i + "\t");
		}
		sb.append("\n");

		for (int i = 0; i < 10; i++) {
			sb.append("row 1 column " + i + "\t");
		}
		sb.append("\n");

		for (int i = 0; i < 2; i++) {
			sb.append("row 2 column " + i + "\t");
		}

		InputStream from = IOUtils.toInputStream(sb.toString());
		PreviewOutputMetadata type = tabPreviewGenerator.generatePreview(from, baos);
		assertEquals(TabCsvPreviewGenerator.TEXT_CSV_SEPARATED_VALUES, type.getContentType());
		String output = baos.toString();
		String[] lines = output.split("\n");
		// input tsv has 3 lines
		assertEquals(3, lines.length);
		// input tsv has varying column lengths
		assertEquals(5, lines[0].split(",").length);
		assertEquals(10, lines[1].split(",").length);
		assertEquals(2, lines[2].split(",").length);
	}

	@Test
	public void testContentType() throws IOException {
		for (String ct : TabCsvPreviewGenerator.TAB_SEPARATED_MIME_TYPES) {
			assertTrue(tabPreviewGenerator.supportsContentType(ct, null));
			assertFalse(csvPreviewGenerator.supportsContentType(ct, null));
		}
		for (String ct : TabCsvPreviewGenerator.COMMA_SEPARATED_MIME_TYPES) {
			assertTrue(csvPreviewGenerator.supportsContentType(ct, null));
			assertFalse(tabPreviewGenerator.supportsContentType(ct, null));
		}
		for (String ct : TabCsvPreviewGenerator.EXCEL_MIME_TYPES) {
			assertTrue(csvPreviewGenerator.supportsContentType(ct, "csv"));
			assertFalse(csvPreviewGenerator.supportsContentType(ct, "tsv"));
			assertFalse(csvPreviewGenerator.supportsContentType(ct, "other"));
			assertTrue(tabPreviewGenerator.supportsContentType(ct, "tsv"));
			assertFalse(tabPreviewGenerator.supportsContentType(ct, "csv"));
			assertFalse(tabPreviewGenerator.supportsContentType(ct, "other"));
		}

		assertFalse(tabPreviewGenerator.supportsContentType("text/xml", null));
	}

	@Test
	public void testCalculateMax() throws IOException {
		assertEquals(20L, tabPreviewGenerator.calculateNeededMemoryBytesForPreview(null, 20L));
		assertEquals(77670L, tabPreviewGenerator.calculateNeededMemoryBytesForPreview(null, 2000000L));
	}

	@Test
	public void testEmptyFile() throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		String s = "";
		InputStream is = IOUtils.toInputStream(s);
		csvPreviewGenerator.generatePreview(is, os);
		String out = os.toString();
		assertEquals("", out);
	}

	@Test
	public void testCreateEllipsisRow() {
		int numberOfColumns = 3;
		// call under test
		String[] results = TabCsvPreviewGenerator.createEllipsisRow(numberOfColumns);
		assertNotNull(results);
		assertEquals(numberOfColumns, results.length);
		assertEquals(TabCsvPreviewGenerator.HTML_ELLIPSIS, results[0]);
		assertEquals(TabCsvPreviewGenerator.HTML_ELLIPSIS, results[1]);
		assertEquals(TabCsvPreviewGenerator.HTML_ELLIPSIS, results[2]);
	}

	@Test
	public void testCreatePreviewCell() {
		String input = "foo";
		// call under test
		String result = TabCsvPreviewGenerator.createPreviewCell(input);
		assertEquals(input, result);
	}

	@Test
	public void testCreatePreviewCellNull() {
		String input = null;
		// call under test
		String result = TabCsvPreviewGenerator.createPreviewCell(input);
		assertEquals(null, result);
	}

	@Test
	public void testCreatePreviewCellAtMax() {
		String input = createStringOfLength(TabCsvPreviewGenerator.MAX_CELL_CHARACTER_COUNT);
		// call under test
		String result = TabCsvPreviewGenerator.createPreviewCell(input);
		assertEquals(input, result);
	}

	@Test
	public void testCreatePreviewCellAtOverMax() {
		String input = createStringOfLength(TabCsvPreviewGenerator.MAX_CELL_CHARACTER_COUNT + 1);
		// call under test
		String result = TabCsvPreviewGenerator.createPreviewCell(input);
		// expected
		String expected = createStringOfLength(TabCsvPreviewGenerator.MAX_CELL_CHARACTER_COUNT)
				+ TabCsvPreviewGenerator.HTML_ELLIPSIS;
		assertEquals(expected, result);
	}

	@Test
	public void testCreatePreviewRowCharsOverLimit() {
		int numberOfColumns = 1;
		int numberOfChars = TabCsvPreviewGenerator.MAX_CELL_CHARACTER_COUNT + 1;
		String[] row = createRow(numberOfColumns, numberOfChars);
		// call under test
		String[] result = TabCsvPreviewGenerator.createPreviewRow(row);
		assertNotNull(result);
		assertEquals(1, result.length);
		// the cell should be truncated
		assertTrue(result[0].endsWith(TabCsvPreviewGenerator.HTML_ELLIPSIS));
	}

	@Test
	public void testCreatePreviewRowAtMaxColumns() {
		int numberOfColumns = TabCsvPreviewGenerator.MAX_COLUMN_COUNT;
		int numberOfChars = 1;
		String[] row = createRow(numberOfColumns, numberOfChars);
		// call under test
		String[] result = TabCsvPreviewGenerator.createPreviewRow(row);
		// The results should be unchanged.
		assertEquals(Arrays.toString(row), Arrays.toString(result));
	}

	@Test
	public void testCreatePreviewRowOverMaxColumns() {
		int numberOfColumns = TabCsvPreviewGenerator.MAX_COLUMN_COUNT + 1;
		int numberOfChars = 1;
		String[] row = createRow(numberOfColumns, numberOfChars);
		// call under test
		String[] result = TabCsvPreviewGenerator.createPreviewRow(row);
		// the last value should be replaced by an ellipsis
		row[TabCsvPreviewGenerator.MAX_COLUMN_COUNT] = TabCsvPreviewGenerator.HTML_ELLIPSIS;
		assertEquals(Arrays.toString(row), Arrays.toString(result));
	}

	@Test
	public void testCreatePreviewRowOverMaxColumnsPlus() {
		int numberOfColumns = TabCsvPreviewGenerator.MAX_COLUMN_COUNT + 10;
		int numberOfChars = 1;
		String[] row = createRow(numberOfColumns, numberOfChars);
		// call under test
		String[] result = TabCsvPreviewGenerator.createPreviewRow(row);
		// the last value should be replaced by an ellipsis
		assertEquals(TabCsvPreviewGenerator.MAX_COLUMN_COUNT + 1, result.length);
		// last cell should be ellipsis
		assertEquals(TabCsvPreviewGenerator.HTML_ELLIPSIS, result[TabCsvPreviewGenerator.MAX_COLUMN_COUNT]);
	}

	/**
	 * For this case the preview is the same as the input.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGeneratePreviewRowsAtMax() throws IOException {
		int numberOfRows = TabCsvPreviewGenerator.MAX_ROW_COUNT;
		int numberOfColumns = 2;
		int numberOfChars = 2;
		char separator = ',';
		List<String[]> rows = createsRows(numberOfRows, numberOfColumns, numberOfChars);
		assertEquals(numberOfRows, rows.size());
		String csvString = writeToCSV(rows, separator);
		CSVReader reader = readCSV(csvString, separator);
		// call under test
		List<String[]> results = TabCsvPreviewGenerator.generatePreviewRows(reader);
		String resultCSV = writeToCSV(results, separator);
		assertEquals(csvString, resultCSV);
	}

	@Test
	public void testGeneratePreviewRowsJustOverMax() throws IOException {
		int numberOfRows = TabCsvPreviewGenerator.MAX_ROW_COUNT + 1;
		int numberOfColumns = 2;
		int numberOfChars = 2;
		char separator = ',';
		List<String[]> rows = createsRows(numberOfRows, numberOfColumns, numberOfChars);
		assertEquals(numberOfRows, rows.size());
		String csvString = writeToCSV(rows, separator);
		CSVReader reader = readCSV(csvString, separator);
		// call under test
		List<String[]> results = TabCsvPreviewGenerator.generatePreviewRows(reader);
		assertNotNull(results);
		assertEquals(numberOfRows, results.size());
		// Last row should be all ellipsis
		String[] expectedRow = TabCsvPreviewGenerator.createEllipsisRow(numberOfColumns);
		assertEquals(Arrays.toString(expectedRow), Arrays.toString(results.get(results.size() - 1)));
	}

	@Test
	public void testGeneratePreviewRowsWayOverMax() throws IOException {
		int numberOfRows = TabCsvPreviewGenerator.MAX_ROW_COUNT + 10;
		int numberOfColumns = 2;
		int numberOfChars = 2;
		char separator = ',';
		List<String[]> rows = createsRows(numberOfRows, numberOfColumns, numberOfChars);
		assertEquals(numberOfRows, rows.size());
		String csvString = writeToCSV(rows, separator);
		CSVReader reader = readCSV(csvString, separator);
		// call under test
		List<String[]> results = TabCsvPreviewGenerator.generatePreviewRows(reader);
		assertNotNull(results);
		assertEquals(TabCsvPreviewGenerator.MAX_ROW_COUNT + 1, results.size());
		// Last row should be all ellipsis
		String[] expectedRow = TabCsvPreviewGenerator.createEllipsisRow(numberOfColumns);
		assertEquals(Arrays.toString(expectedRow), Arrays.toString(results.get(results.size() - 1)));
	}

	/**
	 * Generate a preview where the number of rows and columns are over the maximum,
	 * and the number of characters in each cell is over the maximum.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGeneratePreviewCSV() throws IOException {
		int numberOfRows = 100;
		int numberOfColumns = 100;
		int numberOfChars = 100;
		char separator = ',';
		List<String[]> inputRows = createsRows(numberOfRows, numberOfColumns, numberOfChars);
		String csvString = writeToCSV(inputRows, separator);
		StringInputStream input = new StringInputStream(csvString);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		// call under test
		PreviewOutputMetadata meta = csvPreviewGenerator.generatePreview(input, output);
		assertNotNull(meta);
		assertEquals("text/csv", meta.getContentType());
		assertEquals(".csv", meta.getExtension());

		String resultCSV = new String(output.toByteArray(), "UTF-8");
		// System.out.println(resultCSV);
		CSVReader reader = readCSV(resultCSV, separator);
		List<String[]> rows = reader.readAll();
		assertNotNull(rows);
		assertEquals(TabCsvPreviewGenerator.MAX_ROW_COUNT + 1, rows.size());
		// first
		String[] firstRow = rows.get(0);
		assertEquals(TabCsvPreviewGenerator.MAX_COLUMN_COUNT + 1, firstRow.length);
		assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa&hellip;", firstRow[0]);
		assertEquals("&hellip;", firstRow[firstRow.length - 1]);
		// last
		String[] lastRow = rows.get(rows.size() - 1);
		assertEquals(TabCsvPreviewGenerator.MAX_COLUMN_COUNT + 1, lastRow.length);
		assertEquals("&hellip;", lastRow[0]);
		assertEquals("&hellip;", lastRow[lastRow.length - 1]);
	}

	@Test
	public void testGeneratePreviewTab() throws IOException {
		int numberOfRows = 100;
		int numberOfColumns = 100;
		int numberOfChars = 100;
		char separator = '\t';
		List<String[]> inputRows = createsRows(numberOfRows, numberOfColumns, numberOfChars);
		String csvString = writeToCSV(inputRows, separator);
		StringInputStream input = new StringInputStream(csvString);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		// call under test
		PreviewOutputMetadata meta = tabPreviewGenerator.generatePreview(input, output);
		assertNotNull(meta);
		assertEquals("text/csv", meta.getContentType());
		assertEquals(".csv", meta.getExtension());

		String resultCSV = new String(output.toByteArray(), "UTF-8");
		// System.out.println(resultCSV);
		// output should still be csv
		CSVReader reader = readCSV(resultCSV, ',');
		List<String[]> rows = reader.readAll();
		assertNotNull(rows);
		assertEquals(TabCsvPreviewGenerator.MAX_ROW_COUNT + 1, rows.size());
		// first
		String[] firstRow = rows.get(0);
		assertEquals(TabCsvPreviewGenerator.MAX_COLUMN_COUNT + 1, firstRow.length);
		assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa&hellip;", firstRow[0]);
		assertEquals("&hellip;", firstRow[firstRow.length - 1]);
		// last
		String[] lastRow = rows.get(rows.size() - 1);
		assertEquals(TabCsvPreviewGenerator.MAX_COLUMN_COUNT + 1, lastRow.length);
		assertEquals("&hellip;", lastRow[0]);
		assertEquals("&hellip;", lastRow[lastRow.length - 1]);
	}

	/**
	 * Validate a preview for the example CSV is generated as expected.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testPLFM_4236() throws IOException {
		String fileName = "PLFM_4236.csv";
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try (InputStream in = TabCsvTextPreviewTest.class.getClassLoader().getResourceAsStream(fileName)) {
			assertNotNull("Failed to find: " + fileName + " on classpath", in);
			// call under test
			csvPreviewGenerator.generatePreview(in, bos);
		}
		String resultCSV = new String(bos.toByteArray(), "UTF-8");
		// System.out.println(resultCSV);
		char separator = ',';
		CSVReader reader = readCSV(resultCSV, separator);
		List<String[]> rows = reader.readAll();
		assertNotNull(rows);
		assertEquals(7, rows.size());
		// first
		String[] firstRow = rows.get(0);
		assertEquals(20, firstRow.length);
		assertEquals("fileId", firstRow[0]);
		assertEquals("isTreated", firstRow[19]);
		// last
		String[] lastRow = rows.get(6);
		assertEquals(20, lastRow.length);
		assertEquals("3", lastRow[0]);
		assertEquals("FALSE", lastRow[19]);
	}

	/**
	 * Create a CSV reader to read the given CSV string.
	 * 
	 * @param csvString
	 * @param separator
	 * @return
	 */
	public static CSVReader readCSV(String csvString, char separator) {
		StringReader stringReader = new StringReader(csvString);
		return new CSVReader(stringReader, separator);
	}

	/**
	 * Write the given data to a CSV.
	 * 
	 * @param rows
	 * @param separator
	 * @return
	 * @throws IOException
	 */
	public static String writeToCSV(List<String[]> rows, char separator) throws IOException {
		StringWriter stringWriter = new StringWriter();
		CSVWriter writer = new CSVWriter(stringWriter, separator);
		writer.writeAll(rows);
		writer.flush();
		writer.close();
		return stringWriter.toString();
	}

	/**
	 * Helper to create a CSV with the given number of rows, columns and characters.
	 * 
	 * @param numberOfRows
	 * @param numberOfColumns
	 * @param numberOfChars
	 * @return
	 */
	public static List<String[]> createsRows(int numberOfRows, int numberOfColumns, int numberOfChars) {
		List<String[]> results = new LinkedList<>();
		for (int rowIndex = 0; rowIndex < numberOfRows; rowIndex++) {
			String[] row = createRow(numberOfColumns, numberOfChars);
			results.add(row);
		}
		return results;
	}

	/**
	 * Helper to create a row with the given number of columns. Each cell will
	 * contain a string with the given number of characters.
	 * 
	 * @param numberOfColumns
	 * @param numberOfChars
	 * @return
	 */
	public static String[] createRow(int numberOfColumns, int numberOfChars) {
		String[] row = new String[numberOfColumns];
		for (int i = 0; i < numberOfColumns; i++) {
			row[i] = createStringOfLength(numberOfChars);
		}
		return row;
	}

	/**
	 * Helper to create a string for the given size.
	 * 
	 * @param numberOfChars
	 * @return
	 */
	public static String createStringOfLength(int numberOfChars) {
		char[] chars = new char[numberOfChars];
		for (int i = 0; i < numberOfChars; i++) {
			chars[i] = 'a';
		}
		return new String(chars);
	}
}
