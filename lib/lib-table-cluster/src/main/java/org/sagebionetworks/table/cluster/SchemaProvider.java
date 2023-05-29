package org.sagebionetworks.table.cluster;

import java.util.List;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;

public interface SchemaProvider {
	
	/**
	 * Get the schema for the given table.
	 * @param tableId
	 * @return
	 */
	List<ColumnModel> getTableSchema(IdAndVersion tableId);
	
	/**
	 * Get the column model for the provided ID.
	 * @param id
	 * @return
	 */
	ColumnModel getColumnModel(String id);


}
