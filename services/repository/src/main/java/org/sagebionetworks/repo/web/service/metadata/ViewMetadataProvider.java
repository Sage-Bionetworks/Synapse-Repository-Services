package org.sagebionetworks.repo.web.service.metadata;

import java.util.List;

import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.View;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.web.NotFoundException;

public abstract class ViewMetadataProvider<T extends View> implements TypeSpecificCreateProvider<T>, TypeSpecificUpdateProvider<T>, TypeSpecificMetadataProvider<T>{

	private TableViewManager viewManager;
	
	public ViewMetadataProvider(TableViewManager viewManager) {
		this.viewManager = viewManager;
	}

	@Override
	public void entityCreated(UserInfo userInfo, T entity) {
		ViewScope scope = createViewScope(userInfo, entity);
		
		viewManager.setViewSchemaAndScope(userInfo, entity.getColumnIds(), scope,  entity.getId());
	}

	@Override
	public void entityUpdated(UserInfo userInfo, T entity, boolean wasNewVersionCreated) {
		if(wasNewVersionCreated) {
			throw new IllegalArgumentException("A view version can only be created by creating a view snapshot.");
		}
		
		ViewScope scope = createViewScope(userInfo, entity);
		
		viewManager.setViewSchemaAndScope(userInfo, entity.getColumnIds(), scope, entity.getId());
	}

	@Override
	public void addTypeSpecificMetadata(T entity, UserInfo user, EventType eventType)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		IdAndVersion idAndVersion = KeyFactory.idAndVersion(entity.getId(), entity.getVersionNumber());
		
		List<String> tableSchema = viewManager.getViewSchemaIds(idAndVersion);
		
		entity.setColumnIds(tableSchema);
	}
	
	public abstract ViewScope createViewScope(UserInfo userInfo, T view);
}
