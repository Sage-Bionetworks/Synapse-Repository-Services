package org.sagebionetworks.repo.web.service.metadata;

import java.util.List;

import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
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
	public void entityUpdated(UserInfo userInfo, TableEntity entity, boolean wasNewVersionCreated) {
		tableEntityManager.setTableSchema(userInfo, entity.getColumnIds(), entity.getId());
		if(wasNewVersionCreated) {
			tableEntityManager.bindCurrentEntityVersionToLatestTransaction(entity.getId());
		}
	}

	@Override
	public void entityCreated(UserInfo userInfo, TableEntity entity) {
		tableEntityManager.setTableSchema(userInfo, entity.getColumnIds(), entity.getId());
	}

	@Override
	public void addTypeSpecificMetadata(TableEntity entity,
										UserInfo user, EventType eventType)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		List<String> tableSchema = tableEntityManager.getTableSchema(IdAndVersion.newBuilder()
				.setId(KeyFactory.stringToKey(entity.getId())).setVersion(entity.getVersionNumber()).build());
		entity.setColumnIds(tableSchema);
	}
}
