package org.sagebionetworks.repo.model.dbo.dao.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Provides an Iterator<Row> abstraction over a raw CSV reader.
 * 
 * @author jmhill
 * 
 */
public class CSVToRowIterator implements Iterator<Row> {

	private final CSVReader reader;
	private final Map<Long, Integer> columnIdToCsvColumnIndexMap;
	private final List<ColumnModel> resultSchema;

	private String[] lastRow;
	private int rowLineNumber;

	/**
	 * Create a new object for each use.
	 * 
	 * @param resultSchema Each row returned will match this schema.
	 * @param reader The CSV stream that contains the source data. Data will be read from this stream and translated
	 *        into rows. It is the job of the caller to close this stream when finished.
	 * @param columnIds
	 * @param progressReporter
	 * @throws IOException
	 */
	public CSVToRowIterator(List<ColumnModel> resultSchema, CSVReader reader, boolean isFirstLineHeader, List<String> columnIds)
			throws IOException {
		this.resultSchema = resultSchema;
		this.reader = reader;
		this.rowLineNumber = 1;

		// We need to read the first row to determine if it a header
		lastRow = reader.readNext();

		// We need a map of column ID to index.
		String[] headers = null;
		if (isFirstLineHeader) {
			headers = lastRow;
			// Since the first row was a header, we need to next row to start.
			lastRow = reader.readNext();
			rowLineNumber++;
		}

		if (!CollectionUtils.isEmpty(columnIds)) {
			columnIdToCsvColumnIndexMap = TableModelUtils.createColumnIdToColumnIndexMapFromColumnIds(columnIds, resultSchema);
		} else if (headers != null) {
			columnIdToCsvColumnIndexMap = TableModelUtils.createColumnIdToColumnIndexMapFromFirstRow(headers, resultSchema);
		} else {
			// This means the row is not a header and no header was given. So just map from the schema
			columnIdToCsvColumnIndexMap = TableModelUtils.createColumnIdToSchemaIndexMap(resultSchema);
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
		ArrayList<String> values = Lists.<String> newArrayListWithCapacity(resultSchema.size());
		boolean anyValues = false; // no values at all in a row denotes a deletion
		for (int i = 0; i < resultSchema.size(); i++) {
			Long columnId = Long.parseLong(resultSchema.get(i).getId());
			Integer csvColumnIndex = columnIdToCsvColumnIndexMap.get(columnId);
			String value = null;
			if (csvColumnIndex != null) {
				if (lastRow.length > csvColumnIndex) {
					anyValues = true;
					value = lastRow[csvColumnIndex];
				}
			}
			values.add(value);
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

}
