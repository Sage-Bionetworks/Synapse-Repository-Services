package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.EntityType;

public interface TableTransactionManagerProvider {
	
	/**
	 * Get the transaction manager to be used for a given type.
	 * 
	 * @param type
	 * @return
	 */
	public TableTransactionManager getTransactionManagerForType(EntityType type);

}
