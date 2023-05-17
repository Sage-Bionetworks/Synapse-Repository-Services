package org.sagebionetworks.repo.manager.table;

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

}
