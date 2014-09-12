package org.sagebionetworks.table.worker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.dao.table.CSVToRowIterator;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.Row;
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
	List<ColumnModel> currentSchema;
	ProgressReporter reporter;
	boolean isFirstLineHeader;
	List<String[]> startingRow;
	ColumnModel[] testTypes;
	
	public UploadPreviewBuilder(CsvNullReader reader, List<ColumnModel> currentSchema, ProgressReporter reporter, CsvTableDescriptor descriptor){
		this.reader = reader;
		this.currentSchema = currentSchema;
		this.reporter = reporter;
		this.isFirstLineHeader = CSVUtils.isFirstRowHeader(descriptor);
		this.startingRow = new ArrayList<String[]>(MAX_ROWS_IN_PREVIEW);
	}
	
	public UploadToTablePreviewResult buildResult() throws IOException{
		UploadToTablePreviewResult resutls = new UploadToTablePreviewResult();
		// Is the first row a header?
		if(isFirstLineHeader){
			String[] header = reader.readNext();
		}
		int rowCount = 0;
		// Read the Entire file.
		while(reader.isHasNext()){
			String[] row = reader.readNext();
			rowCount++;
			reporter.tryReportProgress(rowCount);
			// Keep the first few rows
			if(rowCount <= MAX_ROWS_IN_PREVIEW){
				startingRow.add(row);
			}else{
				break;
			}
		}
		
		// Create a reader for the starter rows
//		CsvNullReader reader = TableModelTestUtils.createReader(this.startingRow);
//		CSVToRowIterator iterator = new CSVToRowIterator(tableSchema, reader, false);
//		List<Row> previewRows = new ArrayList<Row>(MAX_ROWS_IN_PREVIEW);
//		while(iterator.hasNext()){
//			previewRows.add(iterator.next());
//		}
//		resutls.setSampleRows(previewRows);
		resutls.setRowCount(new Long(rowCount));
		return resutls;
	}
}
