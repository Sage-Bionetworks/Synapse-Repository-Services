package org.sagebionetworks.table.cluster;

import java.util.List;

import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;

public interface SchemaProvider {
	
	/**
	 * @param tableId
	 * @return The type of table with the given id and version 
	 */
	TableType getTableType(IdAndVersion tableId);
	
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
