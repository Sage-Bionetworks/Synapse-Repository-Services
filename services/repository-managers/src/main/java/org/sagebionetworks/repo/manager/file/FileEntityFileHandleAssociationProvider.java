package org.sagebionetworks.repo.manager.file;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.manager.util.CollectionUtils;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileEntityFileHandleAssociationProvider implements FileHandleAssociationProvider {

	private final NodeDAO nodeDao;
		
	@Autowired
	public FileEntityFileHandleAssociationProvider(NodeDAO nodeDao) {
		super();
		this.nodeDao = nodeDao;
	}

	@Override
	public FileHandleAssociateType getAssociateType() {
		return FileHandleAssociateType.FileEntity;
	}
	
	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String entityId) {
		ValidateArgument.required(fileHandleIds, "fileHandleIds");
		ValidateArgument.required(entityId, "entityId");
		List<Long> fileHandleIdsLong = new ArrayList<Long>();
		CollectionUtils.convertStringToLong(fileHandleIds, fileHandleIdsLong);
		Set<Long> returnedFileHandleIds = nodeDao.getFileHandleIdsAssociatedWithFileEntity(fileHandleIdsLong, KeyFactory.stringToKey(entityId));
		Set<String> results = new HashSet<String>();
		CollectionUtils.convertLongToString(returnedFileHandleIds, results);
		return results;
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.ENTITY;
	}

}
