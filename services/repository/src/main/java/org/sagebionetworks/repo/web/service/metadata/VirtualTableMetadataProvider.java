package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.manager.table.VirtualTableManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.VirtualTable;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class VirtualTableMetadataProvider implements 
	EntityValidator<VirtualTable>, 
	TypeSpecificCreateProvider<VirtualTable>,
	TypeSpecificUpdateProvider<VirtualTable>, 
	TypeSpecificMetadataProvider<VirtualTable> {

	private VirtualTableManager manager;
	
	public VirtualTableMetadataProvider(VirtualTableManager manager) {
		this.manager = manager;
	}
	
	@Override
	public void validateEntity(VirtualTable entity, EntityEvent event)
			throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException {
		manager.validate(entity);		
	}

	@Override
	public void addTypeSpecificMetadata(VirtualTable entity, UserInfo user, EventType eventType)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		entity.setColumnIds(manager.getSchemaIds(KeyFactory.idAndVersion(entity.getId(), entity.getVersionNumber())));
	}

	@Override
	public void entityUpdated(UserInfo userInfo, VirtualTable entity, boolean wasNewVersionCreated) {
		if (wasNewVersionCreated) {
			throw new IllegalArgumentException("A VirtualTable version can only be created by creating a snapshot.");
		}
		registerDefiningSql(entity);
	}

	@Override
	public void entityCreated(UserInfo userInfo, VirtualTable entity) {
		registerDefiningSql(entity);
	}
	
	private void registerDefiningSql(VirtualTable entity) {
		// Note that the defining SQL is always bound to the "current" version of the entity (See https://sagebionetworks.jira.com/browse/PLFM-7963)
		manager.registerDefiningSql(IdAndVersion.parse(entity.getId()), entity.getDefiningSQL());
	}

}
