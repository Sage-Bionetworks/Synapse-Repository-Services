package org.sagebionetworks.repo.web.service.metadata;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class EntityViewMetadataProvider implements TypeSpecificCreateProvider<EntityView>, TypeSpecificUpdateProvider<EntityView>, TypeSpecificMetadataProvider<EntityView> {

	
	@Autowired
	TableViewManager fileViewManager;

	@Override
	public void entityUpdated(UserInfo userInfo, EntityView entityView) {
		fileViewManager.setViewSchemaAndScope(userInfo, entityView.getColumnIds(), entityView.getScopeIds(),entityView.getType(), entityView.getId());
	}

	@Override
	public void entityCreated(UserInfo userInfo, EntityView entityView) {
		fileViewManager.setViewSchemaAndScope(userInfo, entityView.getColumnIds(), entityView.getScopeIds(),entityView.getType(),  entityView.getId());
	}

	@Override
	public void addTypeSpecificMetadata(EntityView entity,
			HttpServletRequest request, UserInfo user, EventType eventType)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		List<String> tableSchema = fileViewManager.getTableSchema(entity.getId());
		entity.setColumnIds(tableSchema);
	}
}
