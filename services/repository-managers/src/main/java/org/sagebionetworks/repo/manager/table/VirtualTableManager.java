package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.VirtualTable;

/**
 * Manager for operations on {@link VirtualTable}s
 */
public interface VirtualTableManager {

	/**
	 * Validates the SQL defining the given virtual table
	 * 
	 * @param materializedView
	 */
	void validate(VirtualTable virtualTable);

	/**
	 * Get the column IDs for this VT's schema.
	 * @param idAndVersion
	 * @return
	 */
	List<String> getSchemaIds(IdAndVersion idAndVersion);

	void registerDefiningSql(IdAndVersion id, String definingSQL);

}
