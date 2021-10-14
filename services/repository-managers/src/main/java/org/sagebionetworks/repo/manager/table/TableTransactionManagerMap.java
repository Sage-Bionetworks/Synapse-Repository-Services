package org.sagebionetworks.repo.manager.table;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.EntityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Simple mapping of entity types to transaction managers.
 *
 */
@Configuration
public class TableTransactionManagerMap {

	@Autowired
	TableEntityTransactionManager tableEntityTransactionManager;

	@Autowired
	TableViewTransactionManager tableViewTransactionManager;

	@Bean
	public Map<EntityType, TableTransactionManager> getMapping() {
		Map<EntityType, TableTransactionManager> map = new HashMap<>();
		map.put(EntityType.table, tableEntityTransactionManager);
		map.put(EntityType.entityview, tableViewTransactionManager);
		map.put(EntityType.submissionview, tableViewTransactionManager);
		map.put(EntityType.dataset, tableViewTransactionManager);
		return map;
	};
}
