package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.dao.table.TableType;

public interface TableUpdateRequestManagerProvider {
	
	/**
	 * Get the table update request manager to be used for a given type.
	 * 
	 * @param type
	 * @return
	 */
	public TableUpdateRequestManager getUpdateRequestManagerForType(TableType type);

}
