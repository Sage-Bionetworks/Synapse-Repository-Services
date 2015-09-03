package org.sagebionetworks.repo.web.service.metadata;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class FileEntityMetadataProvider implements EntityValidator<FileEntity> {

	@Autowired
	FileHandleDao fileHandleDao;
	
	@Override
	public void validateEntity(FileEntity entity, EntityEvent event)
			throws InvalidModelException, NotFoundException,
			DatastoreException, UnauthorizedException {
		if(EventType.CREATE == event.getType() || EventType.UPDATE == event.getType() || EventType.NEW_VERSION == event.getType()) {
			// This is for PLFM-1754.
			if(entity.getDataFileHandleId() == null) throw new IllegalArgumentException("FileEntity.dataFileHandleId cannot be null");

			if (StringUtils.isEmpty(entity.getFileName())) {
				FileHandle fileHandle = fileHandleDao.get(entity.getDataFileHandleId());
				entity.setFileName(fileHandle.getFileName());
			}
		}	
	}


}
