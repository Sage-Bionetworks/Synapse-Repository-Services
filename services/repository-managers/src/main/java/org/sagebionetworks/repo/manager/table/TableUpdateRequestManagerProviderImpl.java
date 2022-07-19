package org.sagebionetworks.repo.manager.table;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Simple mapping of entity types to transaction managers.
 *
 */
@Service
public class TableUpdateRequestManagerProviderImpl implements TableUpdateRequestManagerProvider {

	private Map<EntityType, TableUpdateRequestManager> managerMap;
	
	@Autowired
	public void configureMapping(TableEntityUpdateRequestManager tableEntityUpdateManager, TableViewUpdateRequestManager tableViewUpdateManager) {
		managerMap = new HashMap<>();
		managerMap.put(EntityType.table, tableEntityUpdateManager);
		managerMap.put(EntityType.entityview, tableViewUpdateManager);
		managerMap.put(EntityType.submissionview, tableViewUpdateManager);
		managerMap.put(EntityType.dataset, tableViewUpdateManager);
		managerMap.put(EntityType.datasetcollection, tableViewUpdateManager);
	}

	@Override
	public TableUpdateRequestManager getUpdateRequestManagerForType(EntityType type) {
		ValidateArgument.required(type, "type");
		TableUpdateRequestManager manager = managerMap.get(type);
		if (manager == null){
			throw new IllegalArgumentException("Unknown type: "+type);
		}
		return manager;
	}
	
}
