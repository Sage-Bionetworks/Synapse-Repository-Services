package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.manager.table.TableRowManager;
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
	TableRowManager tableRowManager;		

	@Override
	public void entityDeleted(String deletedId) {
		tableRowManager.deleteTable(deletedId);
	}

	@Override
	public void entityUpdated(UserInfo userInfo, TableEntity entity) {
		tableRowManager.setTableSchema(userInfo, entity.getColumnIds(), entity.getId());
	}

	@Override
	public void entityCreated(UserInfo userInfo, TableEntity entity) {
		tableRowManager.setTableSchema(userInfo, entity.getColumnIds(), entity.getId());
	}
}
