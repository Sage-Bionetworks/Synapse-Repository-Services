package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.csv.CSVWriterStream;

/**
 * RowHandler that captures rows and write them to the 
 * provided CSV writer.
 *
 */
public class CSVWriterRowHandler implements RowHandler {
	
	CSVWriterStream writer;
	List<SelectColumn> selectColumns;
	boolean includeRowIdAndVersion;
	
	public CSVWriterRowHandler(CSVWriterStream writer,
			List<SelectColumn> selectColumns, boolean includeRowIdAndVersion) {
		super();
		this.writer = writer;
		this.selectColumns = selectColumns;
		this.includeRowIdAndVersion = includeRowIdAndVersion;
	}

	/**
	 * Write the header to the passed CSV writer.
	 */
	public void writeHeader(){
		// create the header row.
		String[] csvHeaders = TableModelUtils.createColumnNameHeader(
				selectColumns,
				includeRowIdAndVersion);
		writer.writeNext(csvHeaders);
	}

	@Override
	public void nextRow(Row row) {
		// translate the row
		String[] array = TableModelUtils.writeRowToStringArray(row,
				includeRowIdAndVersion);
		// write it to the stream.
		writer.writeNext(array);
	}


}
