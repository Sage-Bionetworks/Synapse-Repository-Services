package org.sagebionetworks.repo.service.metadata;

import org.sagebionetworks.repo.manager.entity.RecordSetManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class RecordSetMetadataProvider implements EntityValidator<RecordSet>, TypeSpecificCreateProvider<RecordSet>, TypeSpecificUpdateProvider<RecordSet>, TypeSpecificMetadataProvider<RecordSet> {

	private final FileEntityMetadataProvider fileEntityMetadataProvider;
	private final RecordSetManager recordSetManager;

	public RecordSetMetadataProvider(FileEntityMetadataProvider fileEntityMetadataProvider, RecordSetManager recordSetManager) {
		this.fileEntityMetadataProvider = fileEntityMetadataProvider;
		this.recordSetManager = recordSetManager;
	}

	@Override
	public void validateEntity(RecordSet entity, EntityEvent event) throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException {
		recordSetManager.validateRecordSet(entity, event);
		fileEntityMetadataProvider.validateEntity(entity, event);
	}

	@Override
	public void entityUpdated(UserInfo userInfo, RecordSet entity, boolean wasNewVersionCreated) {
		fileEntityMetadataProvider.entityUpdated(userInfo, entity, wasNewVersionCreated);
		recordSetManager.inferSchemaAndBindToIndex(userInfo, entity);
	}

	@Override
	public void entityCreated(UserInfo userInfo, RecordSet entity) {
		fileEntityMetadataProvider.entityCreated(userInfo, entity);
		recordSetManager.inferSchemaAndBindToIndex(userInfo, entity);
	}

	@Override
	public void addTypeSpecificMetadata(RecordSet entity, UserInfo user, EventType eventType) throws DatastoreException, NotFoundException, UnauthorizedException {
		recordSetManager.updateWithValidationResults(entity);
	}


}
