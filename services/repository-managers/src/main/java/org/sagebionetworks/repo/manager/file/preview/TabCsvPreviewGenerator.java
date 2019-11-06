package org.sagebionetworks.repo.manager.file.preview;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * Generates previews for text content types.
 * 
 * @author Jay & John
 *
 */
public class TabCsvPreviewGenerator implements PreviewGenerator {

	public static final String TEXT_CSV_SEPARATED_VALUES = "text/csv";

	public static final Set<String> TAB_SEPARATED_MIME_TYPES = ImmutableSet.<String>builder()
			.add("text/tab-separated-values", "text/tsv", "application/tab-separated-values", "application/tsv",
					"application/x-tsv")
			.build();
	public static final Set<String> COMMA_SEPARATED_MIME_TYPES = ImmutableSet.<String>builder()
			.add("text/comma-separated-values", "text/csv", "application/comma-separated-values", "application/csv",
					"application/x-csv")
			.build();
	public static final Set<String> EXCEL_MIME_TYPES = ImmutableSet.<String>builder()
			.add("application/xls", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
					"application/msexcel", "application/vnd.ms-excel")
			.build();

	public static final String TAB_EXTENSION = "tsv";
	public static final String COMMA_EXTENSION = "csv";

	public static final Character TAB = '\t';
	public static final Character COMMA = ',';
	public static final Character DOUBLE_QUOTE = '\"';
	public static final Character NEWLINE = '\n';
	public static final Character CR = '\r';
	public static final Character EOF = '\u001a';
	public static final String HTML_ELLIPSIS = "&hellip;";
	public static final String HTML_COMMA = "&#44;";
	public static final int MAX_ROW_COUNT = 30;
	public static final int MAX_COLUMN_COUNT = 20;
	public static final int MAX_CELL_CHARACTER_COUNT = 40;
	public static final long MAX_PREVIEW_CHARACTERS = MAX_ROW_COUNT
			* (MAX_COLUMN_COUNT * (MAX_CELL_CHARACTER_COUNT + 3) + 3);

	private final Character delimiter;
	private final String extension;
	private final Set<String> mimeTypes;

	public static Character getComma() {
		return COMMA;
	}

	public static Character getTab() {
		return TAB;
	}

	public TabCsvPreviewGenerator(Character delimiter) {
		this.delimiter = delimiter;
		if (this.delimiter.equals(COMMA)) {
			this.extension = COMMA_EXTENSION;
			this.mimeTypes = COMMA_SEPARATED_MIME_TYPES;
		} else if (this.delimiter.equals(TAB)) {
			this.extension = TAB_EXTENSION;
			this.mimeTypes = TAB_SEPARATED_MIME_TYPES;
		} else {
			throw new IllegalArgumentException("unknown delimiter: " + delimiter);
		}
	}

	@Override
	public PreviewOutputMetadata generatePreview(InputStream from, OutputStream to) throws IOException {
		generatePreview(delimiter, from, to);
		// always generates a csv preview
		return new PreviewOutputMetadata(TEXT_CSV_SEPARATED_VALUES, ".csv");
	}

	/**
	 * Generate a preview that does not exceed the maximum number of rows or
	 * columns, and each cell values is under a maximum number of characters.
	 * 
	 * @param delimiter
	 * @param from
	 * @param to
	 * @throws IOException
	 */
	public static void generatePreview(final Character delimiter, final InputStream from, final OutputStream to)
			throws IOException {
		try (CSVReader reader = new CSVReader(new InputStreamReader(from, "UTF-8"), delimiter);
				CSVWriter writer = new CSVWriter(new OutputStreamWriter(to, "UTF-8"), COMMA)) {
			// generate the preview from the input.
			List<String[]> previewRows = generatePreviewRows(reader);
			writer.writeAll(previewRows);
			writer.flush();
			writer.close();
		}
	}
	
	/**
	 * Generate a preview that does not exceed the maximum number of rows or
	 * columns, and each cell values is under a maximum number of characters.
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	public static List<String[]> generatePreviewRows(CSVReader reader) throws IOException{
		List<String[]> results = new LinkedList<>();
		String[] lastRow = null;
		int rowsRead = 0;
		int columnWidth = 0;
		// Read read a row from the csv.
		while ((lastRow = reader.readNext()) != null && rowsRead < MAX_ROW_COUNT + 1) {
			String[] previewRow = null;
			if (rowsRead > MAX_ROW_COUNT - 1) {
				// add a row of ellipsis
				previewRow = createEllipsisRow(columnWidth);
			} else {
				// create a preview from the current row.
				previewRow = createPreviewRow(lastRow);
			}
			results.add(previewRow);
			columnWidth = Math.max(columnWidth, previewRow.length);
			rowsRead++;
		}
		return results;
	}

	/**
	 * Create a preview row from the given row. If the input row includes more
	 * columns than the maximum allowed columns, the results will be truncated and
	 * the last column will contain an ellipsis. If any cell in the input is over
	 * the maximum number of characters the output cell will be truncated with an
	 * ellipsis suffix.
	 * 
	 * @param lastRow
	 * @return
	 */
	public static String[] createPreviewRow(String[] lastRow) {
		int resultNumberOfColumns = Math.min(lastRow.length, MAX_COLUMN_COUNT + 1);
		String[] resultRow = new String[resultNumberOfColumns];
		// Copy the data from the last row into the output row.
		for (int columnIndex = 0; columnIndex < resultNumberOfColumns; columnIndex++) {
			if (columnIndex > MAX_COLUMN_COUNT - 1) {
				// use an ellipsis when over the max number of columns.
				resultRow[columnIndex] = HTML_ELLIPSIS;
			} else {
				// create a preview for the cell value.
				resultRow[columnIndex] = createPreviewCell(lastRow[columnIndex]);
			}
		}
		return resultRow;
	}

	/**
	 * Create a preview cell from the input cell. If the input cell is null the output will be null.
	 * If the input is over the maximum number of characters the output will be truncated
	 * with an ellipsis suffix.
	 * 
	 * @param inputCell
	 * @return
	 */
	public static String createPreviewCell(String inputCell) {
		if (inputCell == null) {
			return null;
		}
		if (inputCell.length() <= MAX_CELL_CHARACTER_COUNT) {
			// cell under max.
			return inputCell;
		} else {
			// cell over max
			StringBuilder builder = new StringBuilder(inputCell.substring(0, MAX_CELL_CHARACTER_COUNT));
			builder.append(HTML_ELLIPSIS);
			return builder.toString();
		}
	}

	/**
	 * Create a row of ellipsis with the given number of columns.
	 * 
	 * @param numberColumns
	 * @return
	 */
	public static String[] createEllipsisRow(int numberOfColumns) {
		String[] row = new String[numberOfColumns];
		for (int i = 0; i < numberOfColumns; i++) {
			row[i] = HTML_ELLIPSIS;
		}
		return row;
	}

	@Override
	public long calculateNeededMemoryBytesForPreview(String mimeType, long contentSize) {
		return Math.min(contentSize, MAX_PREVIEW_CHARACTERS * 3);
	}

	@Override
	public boolean supportsContentType(String contentType, String extension) {
		if (mimeTypes.contains(contentType)) {
			return true;
		}
		if (EXCEL_MIME_TYPES.contains(contentType) && this.extension.equals(extension)) {
			return true;
		}
		return false;
	}
}
