package org.sagebionetworks.repo.manager.table;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TableFileHandleAssociationProvider implements FileHandleAssociationProvider {
	
	private TableEntityManager tableEntityManager;
	
	@Autowired
	public TableFileHandleAssociationProvider(TableEntityManager tableEntityManager) {
		this.tableEntityManager = tableEntityManager;
	}	
	
	@Override
	public FileHandleAssociateType getAssociateType() {
		return FileHandleAssociateType.TableEntity;
	}
	
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
