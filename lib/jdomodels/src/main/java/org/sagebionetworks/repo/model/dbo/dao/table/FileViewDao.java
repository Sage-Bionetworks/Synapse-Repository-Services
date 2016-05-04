package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.table.ColumnModel;

public interface FileViewDao {
	
	/**
	 * Calculate a Cyclic Redundancy Check (CRC) of all file within the given containers.
	 * The CRC is calculated as SUM(CRC23(CONCAT(ID, '-', ETAG)))
	 * given the ID and ETAG of each FileEntity with a parentId in the given container set.
	 * 
	 * Warning this call is not cheap.
	 * @param viewContainers
	 * @return
	 */
	public long calculateCRCForAllFilesWithinContainers(Set<Long> viewContainers);

	/**
	 * Stream over all FileEntities that are contained within the given containers.
	 * This method will read the minimum data from the database necessary to populate
	 * Rows matching the given schema.
	 *  
	 * @param containers
	 * @param schema
	 * @param rowHandler
	 */
	void streamOverFileEntities(Set<Long> containers, List<ColumnModel> schema,
			RowHandler rowHandler);

	/**
	 * Count the number of files contained within the given containers.
	 * 
	 * @param allContainersInScope
	 */
	public long countAllFilesInView(Set<Long> allContainersInScope);

}
