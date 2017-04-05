package org.sagebionetworks.repo.manager.dataaccess;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequestInterface;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessRequestDAO;
import org.sagebionetworks.repo.model.file.FileHandleAssociationProvider;
import org.springframework.beans.factory.annotation.Autowired;

public class DataAccessRequestFileHandleAssociationProvider implements FileHandleAssociationProvider{

	@Autowired
	DataAccessRequestDAO dataAccessRequestDao;

	@Override
	public Set<String> getFileHandleIdsAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		Set<String> associatedIds = new HashSet<String>();
		DataAccessRequestInterface request = dataAccessRequestDao.get(objectId);
		if (!request.getAttachments().isEmpty()) {
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
