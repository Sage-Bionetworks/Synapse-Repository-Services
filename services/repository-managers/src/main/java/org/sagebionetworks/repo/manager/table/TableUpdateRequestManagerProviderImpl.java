package org.sagebionetworks.repo.manager.table;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Simple mapping of entity types to transaction managers.
 *
 */
@Service
public class TableUpdateRequestManagerProviderImpl implements TableUpdateRequestManagerProvider {

	private Map<TableType, TableUpdateRequestManager> managerMap;
	
	@Autowired
	public void configureMapping(TableEntityUpdateRequestManager tableEntityUpdateManager, TableViewUpdateRequestManager tableViewUpdateManager) {
		managerMap = new HashMap<>();
		managerMap.put(TableType.table, tableEntityUpdateManager);
		managerMap.put(TableType.entityview, tableViewUpdateManager);
		managerMap.put(TableType.submissionview, tableViewUpdateManager);
		managerMap.put(TableType.dataset, tableViewUpdateManager);
		managerMap.put(TableType.datasetcollection, tableViewUpdateManager);
	}

	@Override
	public TableUpdateRequestManager getUpdateRequestManagerForType(TableType type) {
		ValidateArgument.required(type, "type");
		TableUpdateRequestManager manager = managerMap.get(type);
		if (manager == null){
			throw new IllegalArgumentException("Unsupported type: "+type);
		}
		return manager;
	}
	
}
