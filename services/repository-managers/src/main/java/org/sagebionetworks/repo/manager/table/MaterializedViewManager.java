package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.MaterializedView;
import org.sagebionetworks.repo.model.table.TableUnavailableException;

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
	 * Validates the given defining SQL
	 * 
	 * @param definingSql
	 */
	void validateDefiningSql(String definingSql);

	/**
	 * Extract query by the SQL defining a materialized view and bind schema to view.
	 * 
	 * @param idAndVersion The id and (optional) version of the materialized view, if a version is not
	 *                     supplied the source tables will be associated to the "current" version of the
	 *                     materialized view
	 * @param definingSql  The sql defining the materialized view
	 */
	void bindSchemaToView(IdAndVersion idAndVersion, String definingSql);
	
	/**
	 * Refresh all the (non-snapshot) materialized views that depend on the entity with the given id
	 * 
	 * @param entityId The id and (optional) version of the entity that might have changed
	 */
	void refreshDependentMaterializedViews(IdAndVersion entityId);

	/**
	 * Get the bound schema for the given materialized view.
	 * @param idAndVersion
	 * @return
	 */
	List<String> getSchemaIds(IdAndVersion idAndVersion);

	/**
	 * Delete the index associated with the given materialized view.
	 * @param idAndVersion
	 */
	void deleteViewIndex(IdAndVersion idAndVersion);

	/**
	 * Create or update the index associated with the given materialized view.
	 * @param idAndVersion
	 * @throws TableUnavailableException
	 * @throws Exception 
	 */
	void createOrUpdateViewIndex(ProgressCallback callback, IdAndVersion idAndVersion) throws Exception;

}
