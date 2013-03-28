package org.sagebionetworks.repo.web.controller.metadata;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public class FileEntityMetadataProvider implements TypeSpecificMetadataProvider<FileEntity>{

	@Override
	public void validateEntity(FileEntity entity, EntityEvent event)
			throws InvalidModelException, NotFoundException,
			DatastoreException, UnauthorizedException {
		if(EventType.CREATE == event.getType() || EventType.UPDATE == event.getType() || EventType.NEW_VERSION == event.getType()){
			// This is for PLFM-1754.
			if(entity.getDataFileHandleId() == null) throw new IllegalArgumentException("FileEntity.dataFileHandleId cannot be null");
		}	
	}

	@Override
	public void addTypeSpecificMetadata(FileEntity entity,
			HttpServletRequest request, UserInfo user, EventType eventType)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		
	}

	@Override
	public void entityDeleted(FileEntity deleted) {
		
	}

}
