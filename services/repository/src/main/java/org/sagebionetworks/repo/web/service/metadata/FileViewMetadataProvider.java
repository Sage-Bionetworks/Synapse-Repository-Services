package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.FileView;
import org.springframework.beans.factory.annotation.Autowired;

public class FileViewMetadataProvider implements TypeSpecificCreateProvider<FileView>, TypeSpecificUpdateProvider<FileView> {

	
	@Autowired
	TableViewManager fileViewManager;

	@Override
	public void entityUpdated(UserInfo userInfo, FileView fileView) {
		fileViewManager.setViewSchemaAndScope(userInfo, fileView.getColumnIds(), fileView.getScopeIds(), fileView.getId());
	}

	@Override
	public void entityCreated(UserInfo userInfo, FileView fileView) {
		fileViewManager.setViewSchemaAndScope(userInfo, fileView.getColumnIds(), fileView.getScopeIds(), fileView.getId());
	}


}
