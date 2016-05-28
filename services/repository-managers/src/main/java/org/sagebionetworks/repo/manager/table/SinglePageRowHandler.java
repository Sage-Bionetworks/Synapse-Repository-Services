package org.sagebionetworks.repo.manager.table;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.table.Row;

/**
 * A simple row handler that captures all all rows in memory.
 *
 */
public class SinglePageRowHandler implements RowHandler {
	
	List<Row> rows = new LinkedList<Row>();

	@Override
	public void nextRow(Row row) {
		rows.add(row);
	}
	
	/**
	 * Get the captured rows.
	 * @return
	 */
	public List<Row> getRows(){
		return rows;
	}


}
