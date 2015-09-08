package org.sagebionetworks.repo.manager.table;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.TableFileAssociationDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociationProvider;
import org.springframework.beans.factory.annotation.Autowired;

public class TableFileHandleAssociationProvider implements FileHandleAssociationProvider {
	
	@Autowired
	TableFileAssociationDao tableFileAssociationDao;

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.file.FileHandleAssociationProvider#getFileHandleIdsAssociatedWithObject(java.util.List, java.lang.String)
	 */
	@Override
	public Set<String> getFileHandleIdsAssociatedWithObject(
			List<String> fileHandleIds, String objectId) {
		return tableFileAssociationDao.getFileHandleIdsAssociatedWithTable(fileHandleIds, objectId);
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
