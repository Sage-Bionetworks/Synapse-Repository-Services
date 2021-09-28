package org.sagebionetworks.repo.web.service.metadata;

import java.util.List;

import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Validation for TableEntities.
 * 
 * @author John
 *
 */
@Service
public class TableEntityMetadataProvider implements TypeSpecificDeleteProvider<TableEntity>, TypeSpecificCreateProvider<TableEntity>, TypeSpecificUpdateProvider<TableEntity>, TypeSpecificMetadataProvider<TableEntity>, EntityValidator<TableEntity> {
	
	@Autowired
	TableEntityManager tableEntityManager;		

	@Override
	public void entityDeleted(String deletedId) {
		tableEntityManager.setTableAsDeleted(deletedId);
	}

	@Override
	public void entityUpdated(UserInfo userInfo, TableEntity entity, boolean wasNewVersionCreated) {
		if(wasNewVersionCreated) {
			throw new IllegalArgumentException("A table version can only be created by creating a table snapshot.");
		}
		tableEntityManager.setTableSchema(userInfo, entity.getColumnIds(), entity.getId());
	}

	@Override
	public void entityCreated(UserInfo userInfo, TableEntity entity) {
		if (entity.getIsSearchEnabled() != null && entity.getIsSearchEnabled()) {
			tableEntityManager.setSearchEnabled(userInfo, entity.getId());
		}
		tableEntityManager.setTableSchema(userInfo, entity.getColumnIds(), entity.getId());
	}

	@Override
	public void addTypeSpecificMetadata(TableEntity entity,
										UserInfo user, EventType eventType)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		List<String> tableSchema = tableEntityManager.getTableSchema(KeyFactory.idAndVersion(entity.getId(), entity.getVersionNumber()));
		entity.setColumnIds(tableSchema);
	}

	@Override
	public void validateEntity(TableEntity entity, EntityEvent event)
			throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException {
		if(entity.getVersionLabel() == null) {
			entity.setVersionLabel(TableConstants.IN_PROGRESS);
		}
		if(entity.getVersionComment() == null) {
			entity.setVersionComment(TableConstants.IN_PROGRESS);
		}
		if(entity.getIsSearchEnabled() == null) {
			entity.setIsSearchEnabled(false);
		}
	}
}
