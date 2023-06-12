package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.model.entity.IdAndVersion;

/**
 * DAO supporting extra data used for materialized views
 */
public interface MaterializedViewDao {
	
	/**
	 * Associates the given set of source table ids to the materialized view with the given id
	 * 
	 * @param viewId The id and version of the materialized view
	 * @param sourceTableIds The list of table ids associated with the view
	 */
	void addSourceTablesIds(IdAndVersion viewId, Set<IdAndVersion> sourceTableIds);
	
	/**
	 * Removes the association of the given set of source table ids from the materialized view with the given id 
	 * 
	 * @param viewId
	 * @param sourceTableIds
	 */
	void deleteSourceTablesIds(IdAndVersion viewId, Set<IdAndVersion> sourceTableIds);
	
	/**
	 * @param viewId The id of the materialized view
	 * @return The set of source table ids currently associated with the materialized view with the given id
	 */
	Set<IdAndVersion> getSourceTablesIds(IdAndVersion viewId);
	
	/**
	 * @param sourceTableId The id and (optional) version of a source table
	 * @return A page of ids and (optional) versions of the materialized views that are associated with the source table with the given id and (optional) version
	 */
	List<IdAndVersion> getMaterializedViewIdsPage(IdAndVersion sourceTableId, long limit, long offset);
	
}
