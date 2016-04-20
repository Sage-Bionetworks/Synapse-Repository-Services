package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;

public interface TableTruthManager {

	/**
	 * Get the current columns for the given table.
	 * @param tableId
	 * @return
	 */
	List<ColumnModel> getColumnModelsForObject(String tableId);

	/**
	 * Get the version of the given table.
	 * 
	 * @param tableId
	 * @return
	 */
	long getTableVersion(String tableId);

	/**
	 * Is the given table available.
	 * @param tableId
	 * @return
	 */
	boolean isTableAvailable(String tableId);

}
