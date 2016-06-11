package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.EntityView;
import org.springframework.beans.factory.annotation.Autowired;

public class EntityViewMetadataProvider implements TypeSpecificCreateProvider<EntityView>, TypeSpecificUpdateProvider<EntityView> {

	
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


}
