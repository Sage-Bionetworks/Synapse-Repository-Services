package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.table.MaterializedView;

/**
 * Manager for operations on {@link MaterializedView}s
 */
public interface MaterializedViewManager {
	
	/**
	 * Validates the SQL defining the given materialized view
	 * 
	 * @param materializedView
	 */
	void validate(MaterializedView materializedView);
	
	/**
	 * Extract and register all the tables referenced by the SQL defining the given materialized view 
	 * 
	 * @param materializedView The materialized view
	 */
	void registerSourceTables(MaterializedView materializedView);
	
}
