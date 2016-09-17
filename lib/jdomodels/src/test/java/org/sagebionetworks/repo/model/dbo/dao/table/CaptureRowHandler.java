package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.table.Row;

/**
 * Simple RowHandler used to capture data for test.
 *
 */
public class CaptureRowHandler implements RowHandler {
	
	List<Row> captured = new LinkedList<Row>();;

	@Override
	public void nextRow(Row row) {
		captured.add(row);
	}

	/**
	 * Get the rows captured by this handler.
	 * @return
	 */
	public List<Row> getCapturedRows() {
		return captured;
	}

	
}
