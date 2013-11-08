package org.sagebionetworks.repo.model.dao.table;

import org.sagebionetworks.repo.model.table.RowSet;

/**
 * Abstraction for Querying a TableEntity.
 * 
 * @author John
 *
 */
public interface TableRowQueryDAO extends SecondaryTableRowDAO {

	/**
	 * Query a TableEntity using the SQL-Like query.
	 * 
	 * @param tableId
	 * @param queryString
	 * @return
	 */
	public RowSet queryRows(String tableId, String queryString);
}
