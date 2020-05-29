package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("entityViewMetadataProvider")
public class EntityViewMetadataProvider extends ViewMetadataProvider<EntityView> {

	@Autowired
	public EntityViewMetadataProvider(TableViewManager viewManager) {
		super(viewManager);
	}
	
	public ViewScope createViewScope(UserInfo userInfo, EntityView view) {
		ViewScope scope = new ViewScope();
		scope.setViewEntityType(ViewEntityType.entityview);
		scope.setScope(view.getScopeIds());
		scope.setViewType(view.getType());
		scope.setViewTypeMask(view.getViewTypeMask());
		return scope;
	}
}
