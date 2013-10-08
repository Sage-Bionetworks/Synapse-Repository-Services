package org.sagebionetworks.repo.web.controller.metadata;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.web.NotFoundException;


public class TableEntityMetadataProvider implements TypeSpecificMetadataProvider<TableEntity>{

	@Override
	public void validateEntity(TableEntity entity, EntityEvent event) throws InvalidModelException, NotFoundException,
			DatastoreException, UnauthorizedException {
		if(EventType.CREATE == event.getType() || EventType.UPDATE == event.getType() || EventType.NEW_VERSION == event.getType()){
			List<String> columnIds = entity.getColumnIds();
			if(columnIds == null || columnIds.size() < 1) throw new IllegalArgumentException("TableEntity.columnIds must contain at least one ColumnModel ID.");
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
