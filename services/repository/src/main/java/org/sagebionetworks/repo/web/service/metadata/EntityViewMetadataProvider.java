package org.sagebionetworks.repo.web.service.metadata;

import java.util.List;

import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class EntityViewMetadataProvider implements TypeSpecificCreateProvider<EntityView>, TypeSpecificUpdateProvider<EntityView>, TypeSpecificMetadataProvider<EntityView> {

	
	@Autowired
	TableViewManager fileViewManager;

	@Override
	public void entityUpdated(UserInfo userInfo, EntityView entityView, boolean wasNewVersionCreated) {
		if(wasNewVersionCreated) {
			throw new IllegalArgumentException("A view version can only be created by creating a view snapshot.");
		}
		ViewScope scope = createViewScope(entityView);
		fileViewManager.setViewSchemaAndScope(userInfo, entityView.getColumnIds(), scope, entityView.getId());
	}

	@Override
	public void entityCreated(UserInfo userInfo, EntityView entityView) {
		ViewScope scope = createViewScope(entityView);
		fileViewManager.setViewSchemaAndScope(userInfo, entityView.getColumnIds(), scope,  entityView.getId());
	}

	@Override
	public void addTypeSpecificMetadata(EntityView entity,
										UserInfo user, EventType eventType)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		List<String> tableSchema = fileViewManager.getTableSchema(entity.getId());
		entity.setColumnIds(tableSchema);
	}
	
	public static ViewScope createViewScope(EntityView view) {
		ViewScope scope = new ViewScope();
		scope.setScope(view.getScopeIds());
		scope.setViewType(view.getType());
		scope.setViewTypeMask(view.getViewTypeMask());
		return scope;
	}
}
