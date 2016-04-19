package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.table.FileView;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class FileViewMetadataProvider implements EntityValidator<FileView> {

	
	@Autowired
	TableViewManager tableVeiwManager;


	@Override
	public void validateEntity(FileView entity, EntityEvent event)
			throws InvalidModelException, NotFoundException,
			DatastoreException, UnauthorizedException {
		// When the view is updated reset the schema and scope.
		if(EventType.CREATE == event.getType() || EventType.UPDATE == event.getType() || EventType.NEW_VERSION == event.getType()){
			tableVeiwManager.setViewSchemaAndScope(event.getUserInfo(), entity.getColumnIds(), entity.getContainerScope(), entity.getId());
		}	
	}

}
