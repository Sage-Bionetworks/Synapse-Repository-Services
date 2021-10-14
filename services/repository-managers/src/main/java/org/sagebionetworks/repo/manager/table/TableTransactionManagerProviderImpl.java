package org.sagebionetworks.repo.manager.table;

import java.util.Map;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Simple mapping of entity types to transaction managers.
 *
 */
public class TableTransactionManagerProviderImpl implements TableTransactionManagerProvider {

	@Autowired
	Map<EntityType, TableTransactionManager> managerMap;

	@Override
	public TableTransactionManager getTransactionManagerForType(EntityType type) {
		ValidateArgument.required(type, "type");
		TableTransactionManager manager = managerMap.get(type);
		if (manager == null){
			throw new IllegalArgumentException("Unknown type: "+type);
		}
		return manager;
	}
	
}
