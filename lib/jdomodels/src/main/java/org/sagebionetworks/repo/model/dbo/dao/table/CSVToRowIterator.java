package org.sagebionetworks.repo.model.dbo.dao.table;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.SparseRowDto;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Provides an Iterator<Row> abstraction over a raw CSV reader.
 * 
 * @author jmhill
 * 
 */
public class CSVToRowIterator implements Iterator<SparseRowDto> {

	private final CSVReader reader;
	private final Map<Long, Integer> columnIdToCsvColumnIndexMap;
	private final List<ColumnModel> resultSchema;

	private String[] lastRow;
	private int rowLineNumber;
	private long rowsRead;
	private int linesToSkip;

	/**
	 * Create a new object for each use.
	 * 
	 * @param resultSchema Each row returned will match this schema.
	 * @param reader The CSV stream that contains the source data. Data will be read from this stream and translated
	 *        into rows. It is the job of the caller to close this stream when finished.
	 * @param progressReporter
	 * @throws IOException
	 */
	public CSVToRowIterator(List<ColumnModel> resultSchema, CSVReader reader, boolean isFirstLineHeader, Long linesToSkipLong)
			throws IOException {
		this.resultSchema = resultSchema;
		this.reader = reader;
		this.rowLineNumber = 1;
		rowsRead = 0;
		linesToSkip = 0;
		if(linesToSkipLong != null){
			linesToSkip = linesToSkipLong.intValue();
		}
		// skip any requested lines
		for(int i=0; i<linesToSkip; i++){
			lastRow = reader.readNext();
			rowLineNumber++;
		}
		
		// create the headers
		String[] headers = createHeader(isFirstLineHeader);
		
		lastRow = reader.readNext();
		rowLineNumber++;

		columnIdToCsvColumnIndexMap = TableModelUtils.createColumnIdToColumnIndexMapFromFirstRow(headers, resultSchema);
	}
	
	/**
	 * Create a CSV header by considering the lastRow, isFirstLineHeader and
	 * linesToSkip. When isFirstLineHeader=true the header is simply the last
	 * row. Since callers use isFirstLineHeader=true and linesToSkip > 0 when
	 * the headers do not match the names we still need to consider the last row
	 * for the case where the CSV includes ROW_ID and Row_VERSION. When
	 * isFirstLineHeader=true and linesToSkip > 0 then
	 * 
	 * @param lastRow
	 * @param isFirstLineHeader
	 * @param linesToSkip
	 * @param schema
	 * @return
	 * @throws IOException
	 */
	String[] createHeader(boolean isFirstLineHeader) throws IOException {
		// if the first line is a header then we simply read it.
		if (isFirstLineHeader) {
			// read the header
			rowLineNumber++;
			String[] header = reader.readNext();
			if (header == null) {
				throw new IllegalArgumentException("Expected the first line to be the header but was empty.");
			}
			return header;
		}
		boolean lastRowIncludesRowIdAndVersion = false;
		boolean includeEtag = false;
		if (lastRow != null && lastRow.length >= resultSchema.size() + 2) {
			if (TableConstants.ROW_ID.equals(lastRow[0])
					&& TableConstants.ROW_VERSION.equals(lastRow[1])) {
				lastRowIncludesRowIdAndVersion = true;
			}
			
			if(lastRow.length == resultSchema.size() + 3){
				if(TableConstants.ROW_ETAG.equals(lastRow[2])){
					includeEtag = true;
				}
			}
			
		}
		List<String> headerList = new LinkedList<>();
		/*
		 * For the case where firstLineHeader=false and linesToSkip > 0 and the
		 * lastRowIncludesRowIdAndVersion=true we need to create a header using
		 * the names of the columns in the schema
		 */
		if (linesToSkip > 0 && lastRowIncludesRowIdAndVersion) {
			// For this case the header needs to include ROW_ID and version
			headerList.add(TableConstants.ROW_ID);
			headerList.add(TableConstants.ROW_VERSION);
			if(includeEtag){
				headerList.add(TableConstants.ROW_ETAG);
			}
		}
		// Add the names of the columns from the schema
		for (ColumnModel cm : resultSchema) {
			headerList.add(cm.getName());
		}
		// convert to an array.
		return headerList.toArray(new String[headerList.size()]);
	}

	@Override
	public boolean hasNext() {
		return lastRow != null;
	}

	@Override
	public SparseRowDto next() {
		rowsRead++;
		// Convert the row.
		SparseRowDto row = new SparseRowDto();
		Map<String, String> values = new HashMap<>(resultSchema.size());
		boolean anyValues = false; // no values at all in a row denotes a deletion
		for (int i = 0; i < resultSchema.size(); i++) {
			Long columnId = Long.parseLong(resultSchema.get(i).getId());
			Integer csvColumnIndex = columnIdToCsvColumnIndexMap.get(columnId);
			String value = null;
			if (csvColumnIndex != null) {
				if (lastRow.length > csvColumnIndex) {
					anyValues = true;
					value = lastRow[csvColumnIndex];
					values.put(columnId.toString(), value);
				}
			}
		}
		if (anyValues) {
			row.setValues(values);
		}

		Integer csvColumnIndex = columnIdToCsvColumnIndexMap.get(TableConstants.ROW_ID_ID);
		if (csvColumnIndex != null) {
			if (lastRow.length > csvColumnIndex) {
				String value = lastRow[csvColumnIndex];
				if (!StringUtils.isEmpty(value)) {
					row.setRowId(Long.parseLong(value));
				}
			}
		}
		csvColumnIndex = columnIdToCsvColumnIndexMap.get(TableConstants.ROW_VERSION_ID);
		if (csvColumnIndex != null) {
			if (lastRow.length > csvColumnIndex) {
				String value = lastRow[csvColumnIndex];
				if (!StringUtils.isEmpty(value)) {
					row.setVersionNumber(Long.parseLong(value));
				}
			}
		}
		
		csvColumnIndex = columnIdToCsvColumnIndexMap.get(TableConstants.ROW_ETAG_ID);
		if (csvColumnIndex != null) {
			if (lastRow.length > csvColumnIndex) {
				String value = lastRow[csvColumnIndex];
				if (!StringUtils.isEmpty(value)) {
					row.setEtag(value);
				}
			}
		}

		// Net the next row
		try {
			lastRow = this.reader.readNext();
			rowLineNumber++;
		} catch (IOException e) {
			throw new RuntimeException("Line number " + rowLineNumber + ": " + e.getMessage(), e);
		}
		return row;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Not supported");
	}
	
	/**
	 * The number of rows read by this iterator.
	 * @return
	 */
	public long getRowsRead(){
		return rowsRead;
	}

}
