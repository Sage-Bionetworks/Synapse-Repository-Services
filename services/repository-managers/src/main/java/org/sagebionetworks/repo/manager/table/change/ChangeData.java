package org.sagebionetworks.repo.manager.table.change;

import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;

/**
 * Abstraction for all of the data associated with a single table change.
 *
 */
public interface ChangeData {

	/**
	 * Get the table's schema after this change has been applied has been applied.
	 * 
	 * @return
	 */
	List<ColumnModel> getChangeSchema();
}