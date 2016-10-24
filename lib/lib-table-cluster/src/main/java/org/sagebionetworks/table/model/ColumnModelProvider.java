package org.sagebionetworks.table.model;

import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;

/**
 * Abstraction for a provider of ColumnModels.
 */
public interface ColumnModelProvider {

	/**
	 * Get the list of ColumnModels given a list of column IDs.
	 * @param columnIds
	 * @return
	 */
	public List<ColumnModel> getColumns(List<String> columnIds);
}
