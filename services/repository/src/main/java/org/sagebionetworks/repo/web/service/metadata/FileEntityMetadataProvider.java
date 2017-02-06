package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class FileEntityMetadataProvider implements EntityValidator<FileEntity>{
	private static final String FILE_NAME_OVERRIDE_DEPRECATED_REASON = "fileNameOverride field is deprecated and should not be set.";
	@Autowired
	EntityManager manager;

	@Override
	public void validateEntity(FileEntity entity, EntityEvent event)
			throws InvalidModelException, NotFoundException,
			DatastoreException, UnauthorizedException {
		if(EventType.CREATE == event.getType() || EventType.UPDATE == event.getType() || EventType.NEW_VERSION == event.getType()){
			// This is for PLFM-1754.
			if(entity.getDataFileHandleId() == null) {
				throw new IllegalArgumentException("FileEntity.dataFileHandleId cannot be null");
			}
			// PLFM-4108 - deprecate fileNameOverride
			if (entity.getFileNameOverride() != null) {
				throw new IllegalArgumentException(FILE_NAME_OVERRIDE_DEPRECATED_REASON);
			}
		}
	}
}
