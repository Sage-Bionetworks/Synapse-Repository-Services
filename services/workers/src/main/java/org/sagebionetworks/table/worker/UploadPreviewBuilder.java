package org.sagebionetworks.table.worker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewResult;
import org.sagebionetworks.util.csv.CsvNullReader;

/**
 * Builds a CSV upload preview for a file.
 * @author John
 *
 */
public class UploadPreviewBuilder {

	public static final int MAX_ROWS_IN_PARTIAL_SCAN = 1000;
	public static final int MAX_ROWS_IN_PREVIEW = 5;
	CsvNullReader reader;
	ProgressReporter reporter;
	boolean isFirstLineHeader;
	List<String[]> startingRow;
	ColumnModel[] testTypes;
	boolean fullScan;
	int maxRowsInpartialScan = MAX_ROWS_IN_PARTIAL_SCAN;
	String[] header;
	ColumnModel[] schema;
	int rowCount;
	
	public UploadPreviewBuilder(CsvNullReader reader, ProgressReporter reporter, UploadToTablePreviewRequest request){
		this.reader = reader;
		this.reporter = reporter;
		this.isFirstLineHeader = CSVUtils.isFirstRowHeader(request.getCsvTableDescriptor());
		this.startingRow = new ArrayList<String[]>(MAX_ROWS_IN_PREVIEW);
		this.fullScan = CSVUtils.doFullScan(request);
	}
	
	/**
	 * Override the max number of rows in partial scan.
	 * @param maxRowsInpartialScan
	 */
	public void setMaxRowsInpartialScan(int maxRowsInpartialScan) {
		this.maxRowsInpartialScan = maxRowsInpartialScan;
	}

	/**
	 * Build the preview.
	 * @return
	 * @throws IOException
	 */
	public UploadToTablePreviewResult buildResult() throws IOException{
		UploadToTablePreviewResult resutls = new UploadToTablePreviewResult();
		// Is the first row a header?
		header = null;
		if(isFirstLineHeader){
			header = reader.readNext();
		}
		scan();
		// Apply headers
		applyHeadersToSchema();
		// Add temp ColumnModel.IDs that are needed for CSVToRowIterator.
		setOrClearColumnIds(false);
		
		// Add temp ColumnModel.IDs that are needed for CSVToRowIterator.
		setOrClearColumnIds(true);
		resutls.setHeaderColumns(Arrays.asList(schema));
		resutls.setRowCount(new Long(rowCount));
		return resutls;
	}

	private void scan() throws IOException {
		schema = null;
		// Read the Entire file.
		while(reader.isHasNext()){
			String[] row = reader.readNext();
			if(row != null){
				if(schema == null){
					schema = new ColumnModel[row.length];
				}
				// Check the schema from this row
				CSVUtils.checkTypes(row, schema);
				rowCount++;
				reporter.tryReportProgress(rowCount);
				// Keep the first few rows
				if(rowCount <= MAX_ROWS_IN_PREVIEW){
					startingRow.add(row);
				}
				// break out if we are not doing a full scan
				if(!this.fullScan){
					if(rowCount > maxRowsInpartialScan){
						break;
					}
				}
			}
		}
	}

	/**
	  Copy the header strings into the ColumnModel.names.
	 * @param header
	 * @param schema
	 */
	private void applyHeadersToSchema() {
		// If the headers does not equal null then set the names
		if(header != null){
			if(header.length != schema.length){
				throw new IllegalArgumentException("The header column count of "+header.length+" does not match the number of columns found in the ");
			}
			// Set the header names.
			for(int i=0; i<header.length; i++){
				schema[i].setName(header[i]);
			}
		}
	}
	
	/**
	 * 
	 * @param schema
	 * @param clear If true then the column ids will be set to null.  When false the index will be assigned as a column Id.
	 */
	private void setOrClearColumnIds(boolean clear){
		for(int i=0; i<schema.length; i++){
			String value = null;
			if(!clear){
				value = ""+i;
			}
			schema[i].setId(value);
		}
	}
}
