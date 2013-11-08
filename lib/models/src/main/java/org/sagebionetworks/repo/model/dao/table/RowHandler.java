package org.sagebionetworks.repo.model.dao.table;

import java.util.List;

import org.sagebionetworks.repo.model.table.Row;

/**
 * Used to scan over RowSets without loading the full set in memory.
 * 
 * @author jmhill
 *
 */
public interface RowHandler {

	/**
	 * Called for each row of the set.
	 * @param row
	 */
	public void nextRow(List<String> headers, Row row);
}
