package org.sagebionetworks.repo.model.dao.table;

import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * This DAO provides status information about a table.
 * 
 * @author John
 *
 */
public interface TableStatusDAO {

	/**
	 * Create or update a table's status.
	 * @param status
	 */
	public TableStatus createOrUpdateTableStatus(TableStatus status);
	
	/**
	 * Get the current status of a table.
	 * 
	 * @param tableId
	 * @return
	 * @throws NotFoundException Thrown when there is no status information for the given table.
	 */
	public TableStatus getTableStatus(String tableId) throws NotFoundException;
}
