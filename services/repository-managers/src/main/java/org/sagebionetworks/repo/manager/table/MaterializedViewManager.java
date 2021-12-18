package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
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
	 * Extract and register all the tables referenced by the SQL defining a materialized view
	 * 
	 * @param idAndVersion The id and (optional) version of the materialized view, if a version is not
	 *                     supplied the source tables will be associated to the "current" version of the
	 *                     materialized view
	 * @param definingSql  The sql defining the materialized view
	 */
	void registerSourceTables(IdAndVersion idAndVersion, String definingSql);

}