package org.sagebionetworks.repo.manager.dataaccess;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.AccessRequirementUtils;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessRequirementFileHandleAssociationProvider implements FileHandleAssociationProvider {
	
	private AccessRequirementDAO accessRequirementDao;
	
	@Autowired
	public AccessRequirementFileHandleAssociationProvider(AccessRequirementDAO accessRequirementDao) {
		this.accessRequirementDao = accessRequirementDao;
	}
	
	@Override
	public FileHandleAssociateType getAssociateType() {
		return FileHandleAssociateType.AccessRequirementAttachment;
	}

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		AccessRequirement accessRequirement = accessRequirementDao.get(objectId);
		Set<String> associatedIds = AccessRequirementUtils.extractAllFileHandleIds(accessRequirement);
		associatedIds.retainAll(fileHandleIds);
		return associatedIds;
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.ACCESS_REQUIREMENT;
	}

}
