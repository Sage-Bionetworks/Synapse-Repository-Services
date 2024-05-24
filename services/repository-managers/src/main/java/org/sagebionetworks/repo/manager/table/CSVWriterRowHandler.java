package org.sagebionetworks.repo.manager.table;

import java.io.IOException;
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
	boolean includeRowEtag;
	
	public CSVWriterRowHandler(CSVWriterStream writer,
			List<SelectColumn> selectColumns, boolean includeRowIdAndVersion, boolean includeRowEtag) {
		super();
		this.writer = writer;
		this.selectColumns = selectColumns;
		this.includeRowIdAndVersion = includeRowIdAndVersion;
		this.includeRowEtag = includeRowEtag;
	}

	/**
	 * Write the header to the passed CSV writer.
	 * @throws IOException 
	 */
	public void writeHeader() throws IOException{
		// create the header row.
		String[] csvHeaders = TableModelUtils.createColumnNameHeader(
				selectColumns,
				includeRowIdAndVersion,
				includeRowEtag);
		writer.writeNext(csvHeaders);
	}

	@Override
	public void nextRow(Row row) {
		// translate the row
		String[] array = TableModelUtils.writeRowToStringArray(row,
				includeRowIdAndVersion, includeRowEtag);
		// write it to the stream.
		try {
			writer.writeNext(array);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


}
