package org.sagebionetworks.repo.manager.table;

import java.util.Map;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Simple mapping of entity types to transaction managers.
 *
 */
public class TableTransactionManagerProviderImpl implements TableTransactionManagerProvider {
	
	/**
	 * Injected with spring.
	 */
	Map<EntityType, TableTransactionManager> managerMap;
	
	
	public void setManagerMap(Map<EntityType, TableTransactionManager> managerMap) {
		this.managerMap = managerMap;
	}

	@Override
	public TableTransactionManager getTransactionManagerForType(EntityType type) {
		ValidateArgument.required(type, "type");
		TableTransactionManager manager = managerMap.get(type);
		if(manager == null){
			throw new IllegalArgumentException("Unknown type: "+type);
		}
		return manager;
	}
	
}
