package org.sagebionetworks.repo.service.metadata;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class RecordSetMetadataProvider implements EntityValidator<RecordSet>, TypeSpecificCreateProvider<RecordSet>, TypeSpecificUpdateProvider<RecordSet>{

	private FileEntityMetadataProvider fileEntityMetadataProvider;
	
	public RecordSetMetadataProvider(FileEntityMetadataProvider fileEntityMetadataProvider) {
		this.fileEntityMetadataProvider = fileEntityMetadataProvider;	
	}

	@Override
	public void validateEntity(RecordSet entity, EntityEvent event) throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException {
		
		if (EventType.CREATE == event.getType() || EventType.UPDATE == event.getType()) {
			ValidateArgument.requiredNotEmpty(entity.getUpsertKey(), "The upsertKey");
			
			if (entity.getCsvDescriptor() != null) {
				ValidateArgument.requirement(Boolean.TRUE.equals(entity.getCsvDescriptor().getIsFirstLineHeader()), "The csvDescriptor.isFirstLineHeader must be true.");
			}
		}
		
		fileEntityMetadataProvider.validateEntity(entity, event);
	}
	
	@Override
	public void entityUpdated(UserInfo userInfo, RecordSet entity, boolean wasNewVersionCreated) {
		fileEntityMetadataProvider.entityUpdated(userInfo, entity, wasNewVersionCreated);
	}

	@Override
	public void entityCreated(UserInfo userInfo, RecordSet entity) {
		fileEntityMetadataProvider.entityCreated(userInfo, entity);
	}


}
