package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.manager.table.FileViewManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.FileView;
import org.springframework.beans.factory.annotation.Autowired;

public class FileViewMetadataProvider implements TypeSpecificCreateProvider<FileView>, TypeSpecificUpdateProvider<FileView> {

	
	@Autowired
	FileViewManager fileViewManager;

	@Override
	public void entityUpdated(UserInfo userInfo, FileView fileView) {
		fileViewManager.setViewSchemaAndScope(userInfo, fileView.getColumnIds(), fileView.getScopeIds(), fileView.getId());
	}

	@Override
	public void entityCreated(UserInfo userInfo, FileView fileView) {
		fileViewManager.setViewSchemaAndScope(userInfo, fileView.getColumnIds(), fileView.getScopeIds(), fileView.getId());
	}


}
