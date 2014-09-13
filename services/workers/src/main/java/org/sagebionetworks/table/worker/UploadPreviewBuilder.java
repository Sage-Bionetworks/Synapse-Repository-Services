package org.sagebionetworks.table.worker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewResult;
import org.sagebionetworks.util.csv.CsvNullReader;

/**
 * Builds a CSV upload preview for a file.
 * @author John
 *
 */
public class UploadPreviewBuilder {

	public static final int MAX_ROWS_IN_PREVIEW = 5;
	CsvNullReader reader;
	ProgressReporter reporter;
	boolean isFirstLineHeader;
	List<String[]> startingRow;
	ColumnModel[] testTypes;
	boolean fullScan;
	
	public UploadPreviewBuilder(CsvNullReader reader, ProgressReporter reporter, UploadToTablePreviewRequest request){
		this.reader = reader;
		this.reporter = reporter;
		this.isFirstLineHeader = CSVUtils.isFirstRowHeader(request.getCsvTableDescriptor());
		this.startingRow = new ArrayList<String[]>(MAX_ROWS_IN_PREVIEW);
		this.fullScan = request.getDoFullFileScan();;
	}
	
	/**
	 * Build the preview.
	 * @return
	 * @throws IOException
	 */
	public UploadToTablePreviewResult buildResult() throws IOException{
		UploadToTablePreviewResult resutls = new UploadToTablePreviewResult();
		// Is the first row a header?
		String[] header = null;
		if(isFirstLineHeader){
			header = reader.readNext();
		}
		int rowCount = 0;
		ColumnModel[] schema = null;
		// Read the Entire file.
		while(reader.isHasNext()){
			String[] row = reader.readNext();
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
			}else{
				break;
			}
		}
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
		resutls.setHeaderColumns(Arrays.asList(schema));
		resutls.setRowCount(new Long(rowCount));
		return resutls;
	}
}
