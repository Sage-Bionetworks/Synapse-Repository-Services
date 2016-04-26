package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Validation for TableEntities.
 * 
 * @author John
 *
 */
public class TableEntityMetadataProvider implements TypeSpecificDeleteProvider<TableEntity>, TypeSpecificCreateProvider<TableEntity>, TypeSpecificUpdateProvider<TableEntity> {
	
	@Autowired
	TableEntityManager tableEntityManager;		

	@Override
	public void entityDeleted(String deletedId) {
		tableEntityManager.deleteTable(deletedId);
	}

	@Override
	public void entityUpdated(UserInfo userInfo, TableEntity entity) {
		tableEntityManager.setTableSchema(userInfo, entity.getColumnIds(), entity.getId());
	}

	@Override
	public void entityCreated(UserInfo userInfo, TableEntity entity) {
		tableEntityManager.setTableSchema(userInfo, entity.getColumnIds(), entity.getId());
	}
}
