package org.sagebionetworks.repo.manager.dataaccess;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestUtils;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RequestFileHandleAssociationProvider implements FileHandleAssociationProvider {
	
	private RequestDAO requestDao;

	@Autowired
	public RequestFileHandleAssociationProvider(RequestDAO requestDao) {
		this.requestDao = requestDao;
	}
	
	@Override
	public FileHandleAssociateType getAssociateType() {
		return FileHandleAssociateType.DataAccessRequestAttachment;
	}

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		
		RequestInterface request = requestDao.get(objectId);
		
		Set<String> associatedIds = RequestUtils.extractAllFileHandleIds(request);
		
		associatedIds.retainAll(fileHandleIds);
		return associatedIds;
	}
	
	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.DATA_ACCESS_REQUEST;
	}

}
