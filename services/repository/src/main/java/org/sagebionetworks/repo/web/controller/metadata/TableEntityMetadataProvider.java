package org.sagebionetworks.repo.web.controller.metadata;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
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
public class TableEntityMetadataProvider implements TypeSpecificMetadataProvider<TableEntity>{
	
	@Autowired
	ColumnModelManager columnModelManager;

	@Override
	public void validateEntity(TableEntity entity, EntityEvent event) throws InvalidModelException, NotFoundException,
			DatastoreException, UnauthorizedException {
		// For create/update/new version we need to bind the columns to the entity.
		if(EventType.CREATE == event.getType() || EventType.UPDATE == event.getType() || EventType.NEW_VERSION == event.getType()){
			List<String> columnIds = entity.getColumnIds();
			if(columnIds == null || columnIds.size() < 1) throw new IllegalArgumentException("TableEntity.columnIds must contain at least one ColumnModel ID.");
			// Bind the entity to these columns
			columnModelManager.bindColumnToObject(event.getUserInfo(), columnIds, entity.getId());
		}	
	}
	
	@Override
	public void addTypeSpecificMetadata(TableEntity entity,
			HttpServletRequest request, UserInfo user, EventType eventType)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void entityDeleted(TableEntity deleted) {
		// TODO Auto-generated method stub
		
	}
	
	

}
