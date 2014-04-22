package org.sagebionetworks.table.worker;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.bridge.model.dbo.dao.CsvNullReader;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;

/**
 * An iterator that streams over the rows of an input CSV and converts the results to Rows
 * that can be appended to a table. 
 * @author jmhill
 *
 */
public class CSVRowIterator implements Iterator<Row>{

	List<ColumnModel> tableSchema;
	List<String> headers;
	CsvNullReader reader;
	long progressIntervalMS;
	String[] lastRow;
	Map<String, Integer> idToIndexMap;
	ProgressReporter progressReporter;
	long lastProgressUpdateTime;
	int rowsProcessed;
	
	public CSVRowIterator(List<ColumnModel> tableSchema, CsvNullReader reader,
			long progressIntervalMS, ProgressReporter progressReporter) throws IOException {
		super();
		this.tableSchema = tableSchema;
		this.reader = reader;
		this.progressIntervalMS = progressIntervalMS;
		this.progressReporter = progressReporter;
		this.headers = TableModelUtils.getHeaders(tableSchema);
		this.lastProgressUpdateTime = System.currentTimeMillis();
		this.rowsProcessed = 0;
		// We need to read the first row to determine if it a header
		lastRow = reader.readNext();
		// We need a map of column ID to index.
		idToIndexMap = TableModelUtils.createColumnIdToIndexMapFromFirstRow(lastRow, tableSchema);
		if(idToIndexMap != null){
			// Since the first row was a header, we need to next row to start.
			lastRow = reader.readNext();
		}else{
			// This means the row is not a header.  So just map from the schema
			idToIndexMap = TableModelUtils.createColumnIdToIndexMap(this.headers);
		}
	}

	@Override
	public boolean hasNext() {
		return lastRow != null;
	}

	@Override
	public Row next() {
		// Convert the row.
		Row row = new Row();
		row.setValues(Arrays.asList(lastRow));
		// Convert the row as needed
		row = TableModelUtils.convertToSchemaAndMerge(row, this.idToIndexMap, this.tableSchema);
		// Net the next row
		try {
			lastRow = this.reader.readNext();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.rowsProcessed++;
		// Report progress if enough time has elapsed since the last update.
		reportProgressIfTime();
		return row;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Not supported");
	}
	
	private void reportProgressIfTime(){
		long now = System.currentTimeMillis();
		if((now - this.lastProgressUpdateTime) > this.progressIntervalMS){
			// Report progress
			this.progressReporter.reportProgress(this.rowsProcessed);
			// Reset the timer
			this.lastProgressUpdateTime = System.currentTimeMillis();
		}
	}
	
	
}
