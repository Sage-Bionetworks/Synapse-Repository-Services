package org.sagebionetworks.repo.web.service.metadata;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class FileEntityMetadataProvider implements EntityValidator<FileEntity>, TypeSpecificMetadataProvider<FileEntity> {

	@Autowired
	FileHandleManager fileHandleManager;
	
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
		// upon CREATE, fill in fileName field if empty
		if (eventType!=EventType.CREATE) return;
		if (!StringUtils.isEmpty(entity.getFileName())) return;
		String fileHandleId = entity.getDataFileHandleId();
		if (fileHandleId==null) throw new InvalidModelException("dataFileHandleId is required.");
		FileHandle fileHandle = fileHandleManager.getRawFileHandle(user, fileHandleId);
		entity.setFileName(fileHandle.getFileName());
	}
}
