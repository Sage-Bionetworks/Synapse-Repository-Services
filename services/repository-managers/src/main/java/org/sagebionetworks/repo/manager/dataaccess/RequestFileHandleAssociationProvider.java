package org.sagebionetworks.repo.manager.dataaccess;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.file.FileHandleAssociationProvider;
import org.springframework.beans.factory.annotation.Autowired;

public class RequestFileHandleAssociationProvider implements FileHandleAssociationProvider{

	@Autowired
	RequestDAO requestDao;

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		Set<String> associatedIds = new HashSet<String>();
		RequestInterface request = requestDao.get(objectId);
		if (request.getAttachments()!= null && !request.getAttachments().isEmpty()) {
			associatedIds.addAll(request.getAttachments());
		}
		if (request.getDucFileHandleId() != null) {
			associatedIds.add(request.getDucFileHandleId());
		}
		if (request.getIrbFileHandleId() != null) {
			associatedIds.add(request.getIrbFileHandleId());
		}
		associatedIds.retainAll(fileHandleIds);
		return associatedIds;
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.DATA_ACCESS_REQUEST;
	}

}
