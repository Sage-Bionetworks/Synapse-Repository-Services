package org.sagebionetworks.repo.web.service.metadata;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Validation for TableEntities.
 * 
 * @author John
 *
 */
public class TableEntityMetadataProvider implements TypeSpecificDeleteProvider<TableEntity>, TypeSpecificCreateProvider<TableEntity>, TypeSpecificUpdateProvider<TableEntity>, TypeSpecificMetadataProvider<TableEntity> {
	
	@Autowired
	TableEntityManager tableEntityManager;		

	@Override
	public void entityDeleted(String deletedId) {
		tableEntityManager.setTableAsDeleted(deletedId);
	}

	@Override
	public void entityUpdated(UserInfo userInfo, TableEntity entity) {
		tableEntityManager.setTableSchema(userInfo, entity.getColumnIds(), entity.getId());
	}

	@Override
	public void entityCreated(UserInfo userInfo, TableEntity entity) {
		tableEntityManager.setTableSchema(userInfo, entity.getColumnIds(), entity.getId());
	}

	@Override
	public void addTypeSpecificMetadata(TableEntity entity,
			HttpServletRequest request, UserInfo user, EventType eventType)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		List<String> tableSchema = tableEntityManager.getTableSchema(entity.getId());
		entity.setColumnIds(tableSchema);
	}
}
