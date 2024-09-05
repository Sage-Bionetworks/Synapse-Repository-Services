package org.sagebionetworks.repo.service.metadata;

import org.sagebionetworks.repo.manager.table.MaterializedViewManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.MaterializedView;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MaterializedViewMetadataProvider implements 
	EntityValidator<MaterializedView>, 
	TypeSpecificCreateProvider<MaterializedView>,
	TypeSpecificUpdateProvider<MaterializedView>,
	TypeSpecificMetadataProvider<MaterializedView>, 
	TypeSpecificDefiningSqlProvider<MaterializedView> {

	private MaterializedViewManager manager;

	@Autowired
	public MaterializedViewMetadataProvider(MaterializedViewManager manager) {
		this.manager = manager;
	}

	@Override
	public void validateEntity(MaterializedView entity, EntityEvent event)
			throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException {
		manager.validate(entity);
	}

	@Override
	public void entityCreated(UserInfo userInfo, MaterializedView entity) {
		manager.registerSourceTables(IdAndVersion.parse(entity.getId()), entity.getDefiningSQL());
	}

	@Override
	public void entityUpdated(UserInfo userInfo, MaterializedView entity, boolean wasNewVersionCreated) {
		if (wasNewVersionCreated) {
			throw new IllegalStateException("A materialized view version can only be created by creating a snapshot.");
		}
		manager.registerSourceTables(IdAndVersion.parse(entity.getId()), entity.getDefiningSQL());
	}

	@Override
	public void addTypeSpecificMetadata(MaterializedView entity, UserInfo user, EventType eventType)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		IdAndVersion idAndVersion = KeyFactory.idAndVersion(entity.getId(), entity.getVersionNumber());
		entity.setColumnIds(manager.getSchemaIds(idAndVersion));
	}

	@Override
	public void validateDefiningSql(String definingSql) {
		manager.validateDefiningSql(definingSql);
	}

}
