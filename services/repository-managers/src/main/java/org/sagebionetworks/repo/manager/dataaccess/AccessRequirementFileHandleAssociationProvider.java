package org.sagebionetworks.repo.manager.dataaccess;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationProvider;
import org.springframework.beans.factory.annotation.Autowired;

public class AccessRequirementFileHandleAssociationProvider implements FileHandleAssociationProvider{
	@Autowired
	AccessRequirementDAO accessRequirementDao;

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		Set<String> associatedIds = new HashSet<String>();
		AccessRequirement accessRequirement = accessRequirementDao.get(objectId);
		if (accessRequirement instanceof ManagedACTAccessRequirement) {
			ManagedACTAccessRequirement actAR = (ManagedACTAccessRequirement) accessRequirement;
			String ducFileHandleId = actAR.getDucTemplateFileHandleId();
			if (ducFileHandleId != null && fileHandleIds.contains(ducFileHandleId)) {
				associatedIds.add(ducFileHandleId);
			}
		}
		return associatedIds;
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.ACCESS_REQUIREMENT;
	}

}
