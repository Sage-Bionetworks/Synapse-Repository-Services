package org.sagebionetworks.table.worker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewResult;
import org.sagebionetworks.table.cluster.utils.CSVUtils;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Builds a CSV upload preview for a file.
 * 
 * @author John
 *
 */
public class UploadPreviewBuilder {

	public static final int MAX_ROWS_IN_PARTIAL_SCAN = 1000;
	public static final int MAX_ROWS_IN_PREVIEW = 5;
	CSVReader reader;
	ProgressCallback<Integer> progressCallback;
	boolean isFirstLineHeader;
	List<String[]> startingRow;
	ColumnModel[] testTypes;
	boolean fullScan;
	int maxRowsInpartialScan = MAX_ROWS_IN_PARTIAL_SCAN;
	String[] header;
	ColumnModel[] schema;
	int rowsScanned;
	List<ColumnModel> suggestedColumns;

	public UploadPreviewBuilder(CSVReader reader,
			ProgressCallback<Integer> progressCallback, UploadToTablePreviewRequest request) {
		this.reader = reader;
		this.progressCallback = progressCallback;
		this.isFirstLineHeader = CSVUtils.isFirstRowHeader(request
				.getCsvTableDescriptor());
		this.startingRow = new ArrayList<String[]>(MAX_ROWS_IN_PREVIEW);
		this.fullScan = CSVUtils.doFullScan(request);
	}

	/**
	 * Override the max number of rows in partial scan.
	 * 
	 * @param maxRowsInpartialScan
	 */
	public void setMaxRowsInpartialScan(int maxRowsInpartialScan) {
		this.maxRowsInpartialScan = maxRowsInpartialScan;
	}

	/**
	 * Build the preview.
	 * 
	 * @return
	 * @throws IOException
	 */
	public UploadToTablePreviewResult buildResult() throws IOException {
		UploadToTablePreviewResult results = new UploadToTablePreviewResult();
		// Is the first row a header?
		header = null;
		if (isFirstLineHeader) {
			header = reader.readNext();
			schema = new ColumnModel[header.length];
		}
		// First gather data by scanning the rows
		scanRows();
		// fix the schema as needed
		checkSchemaAfterScan();
		// Apply headers
		applyHeadersToSchema();
		// Make sure the names are unique
		makeColumnNamesUnique();
		results.setSuggestedColumns(extractSuggestedColumns());
		results.setRowsScanned(new Long(rowsScanned));
		results.setSampleRows(extractSampleRows());
		return results;
	}

	/**
	 * Extract the sample rows.
	 * 
	 * @return
	 */
	private List<Row> extractSampleRows() {
		List<Row> rows = new ArrayList<Row>(startingRow.size());
		for (String[] rowValues : startingRow) {
			Row row = new Row();
			if (header == null) {
				// Just copy it over when there are no headers.
				row.setValues(Arrays.asList(rowValues));
			} else {
				List<String> values = new ArrayList<String>();
				for (int i = 0; i < header.length; i++) {
					String thisValue = rowValues[i];
					if (TableConstants.ROW_ID.equals(header[i])) {
						row.setRowId(Long.parseLong(thisValue));
					} else if (TableConstants.ROW_VERSION.equals(header[i])) {
						row.setVersionNumber(Long.parseLong(thisValue));
					} else {
						values.add(thisValue);
					}
				}
				row.setValues(values);
			}
			rows.add(row);
		}
		return rows;
	}

	/**
	 * Extract the columns that are part of the suggested columns.
	 * 
	 * @param resutls
	 */
	private List<ColumnModel> extractSuggestedColumns() {
		suggestedColumns = new ArrayList<ColumnModel>(schema.length);
		for (ColumnModel cm : schema) {
			// Filter out ROW_ID and ROW_VERSION
			if (!TableConstants.isReservedColumnName(cm.getName())) {
				suggestedColumns.add(cm);
			}
		}
		return suggestedColumns;
	}

	/**
	 * Scan through the rows of the CSV. checking the types and counting rows.
	 * 
	 * @throws IOException
	 */
	private void scanRows() throws IOException {
		rowsScanned = 0;
		// Read the Entire file.
		String[] row;
		while ((row = reader.readNext()) != null) {
			if (schema == null) {
				schema = new ColumnModel[row.length];
			}
			// Check the schema from this row
			CSVUtils.checkTypes(row, schema);
			rowsScanned++;
			progressCallback.progressMade(rowsScanned);
			// Keep the first few rows
			if (rowsScanned <= MAX_ROWS_IN_PREVIEW) {
				startingRow.add(row);
			}
			// break out if we are not doing a full scan
			if (!this.fullScan) {
				if (rowsScanned >= maxRowsInpartialScan) {
					break;
				}
			}
		}
	}

	/**
	 * After a scan check each schema and fill in values that are missing with
	 * some reasonable defaults
	 */
	private void checkSchemaAfterScan() {
		// Check each column after
		for (int i = 0; i < schema.length; i++) {
			// Add missing columns
			if (schema[i] == null) {
				schema[i] = new ColumnModel();
				schema[i].setColumnType(ColumnType.STRING);
			}
			/*
			 * Start each column with a default name. These names will be
			 * replaced if the table has headers.
			 */
			if (schema[i].getName() == null || "".equals(schema[i].getName())) {
				schema[i].setName("col" + (i+1));
			}
		}
	}

	/**
	 * Copy the header strings into the ColumnModel.names.
	 * 
	 * @param header
	 * @param schema
	 */
	private void applyHeadersToSchema() {
		// If the headers does not equal null then set the names
		if (header != null) {
			if (header.length != schema.length) {
				throw new IllegalArgumentException(
						"The header column count of "
								+ header.length
								+ " does not match the number of columns found in the file ");
			}
			// Set the header names.
			for (int i = 0; i < header.length; i++) {
				// Use the processed from of the header
				schema[i].setName(processHeader(header[i]));
			}
		}
	}
	
	/**
	 * Scan all name and change duplicates to be unique.
	 */
	private void makeColumnNamesUnique(){
		Map<String, Integer> nameCount = new HashMap<String, Integer>(schema.length);
		for (int i = 0; i < schema.length; i++) {
			String name = schema[i].getName();
			Integer count = nameCount.get(name);
			if(count == null){
				count = 0;
			}else{
				String uniqueName = name+count;
				schema[i].setName(uniqueName);
			}
			count++;
			nameCount.put(name, count);
		}
	}
	
	/**
	 * Process the header and fix anything that we cannot handle.
	 * 
	 * @param originalHeader
	 * @return
	 */
	public static String processHeader(String originalHeader){
		if(originalHeader == null){
			return "col";
		}
		// Replace all space with underscore
		String newHeader = originalHeader.trim().replaceAll("\\s", "_");
		// Replace all non-alpha-numeric-underscores with empty string.
		newHeader = newHeader.replaceAll("[^\\w_]", "").trim();
		// If there is nothing left use 'col'
		if("".equals(newHeader)){
			newHeader = "col";
		}
		// must have at least one alpha numeric
		if(!(Pattern.compile("[a-zA-Z0-9]").matcher(newHeader).find())){
			newHeader = "col";
		}
		// key words cannot be used as column names.
		if(TableConstants.isKeyWord(newHeader)){
			newHeader = "_"+newHeader;
		}
		return newHeader;
	}

}
