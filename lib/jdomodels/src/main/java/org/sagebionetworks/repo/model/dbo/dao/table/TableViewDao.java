package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ViewType;

public interface TableViewDao {
	
	/**
	 * Calculate a Cyclic Redundancy Check (CRC) of all entities of the given type within the given containers.
	 * The CRC is calculated as SUM(CRC23(CONCAT(ID, '-', ETAG)))
	 * given the ID and ETAG of each FileEntity with a parentId in the given container set.
	 * 
	 * Warning this call is not cheap.
	 * @param viewContainers
	 * @param type The type of entity to filter.
	 * @return
	 */
	public long calculateCRCForAllEntitiesWithinContainers(Set<Long> viewContainers, ViewType type);

	/**
	 * Stream over all entities of the given type that are contained within the given containers.
	 * This method will read the minimum data from the database necessary to populate
	 * Rows matching the given schema.
	 *  
	 * @param containers
	 * @param type The type of entity to filter.
	 * @param schema
	 * @param rowHandler
	 */
	void streamOverEntities(Set<Long> containers, ViewType type, List<ColumnModel> schema,
			RowHandler rowHandler);

	/**
	 * Count the number of entities of the given type contained within the given containers.
	 * 
	 * @param allContainersInScope
	 * @param type The type of entity to filter.
	 */
	public long countAllEntitiesInView(Set<Long> allContainersInScope, ViewType type);

}
