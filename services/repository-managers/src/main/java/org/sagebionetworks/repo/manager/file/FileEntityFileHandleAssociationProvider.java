package org.sagebionetworks.repo.manager.file;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileEntityFileHandleAssociationProvider implements FileHandleAssociationProvider {

	private NodeManager nodeManager;
		
	@Autowired
	public FileEntityFileHandleAssociationProvider(NodeManager nodeManager) {
		this.nodeManager = nodeManager;
	}
	
	@Override
	public FileHandleAssociateType getAssociateType() {
		return FileHandleAssociateType.FileEntity;
	}
	
	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		return nodeManager.getFileHandleIdsAssociatedWithFileEntity(fileHandleIds, objectId);
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.ENTITY;
	}

}
