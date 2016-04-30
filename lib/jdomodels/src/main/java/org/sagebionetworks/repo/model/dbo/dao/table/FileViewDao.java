package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.table.ColumnModel;

public interface FileViewDao {

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

}
