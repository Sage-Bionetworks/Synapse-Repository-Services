package org.sagebionetworks.table.cluster;

import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;

public interface TableAndColumnMapping {

	/**
	 * Get the union of the schemas from each table referenced in the query.
	 * @return
	 */
	List<ColumnModel> getUnionOfAllTableSchemas();

}
