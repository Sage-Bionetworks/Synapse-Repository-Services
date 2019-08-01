package org.sagebionetworks.repo.manager.table;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationProvider;
import org.springframework.beans.factory.annotation.Autowired;


public class TableFileHandleAssociationProvider implements FileHandleAssociationProvider {
	

	@Autowired
	TableEntityManager tableEntityManager;
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.file.FileHandleAssociationProvider#getFileHandleIdsAssociatedWithObject(java.util.List, java.lang.String)
	 */
	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(
			List<String> fileHandleIds, String objectId) {
		return tableEntityManager.getFileHandleIdsAssociatedWithTable(objectId, fileHandleIds);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.file.FileHandleAssociationProvider#getObjectTypeForAssociationType()
	 */
	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.ENTITY;
	}

}
