package org.sagebionetworks.repo.manager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.file.FileHandleAssociationProvider;
import org.springframework.beans.factory.annotation.Autowired;

public class UserProfileFileHandleAssociationProvider implements FileHandleAssociationProvider{

	@Autowired
	private UserProfileDAO userProfileDAO;
	@Autowired
	private FileHandleManager fileHandleManager;

	@Override
	public Set<String> getFileHandleIdsAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		String handleId = userProfileDAO.getPictureFileHandleId(objectId);
		String previewId = fileHandleManager.getPreviewFileHandleId(handleId);
		Set<String> result = new HashSet<String>();
		if (fileHandleIds.contains(handleId)) {
			result.add(handleId);
		}
		if (fileHandleIds.contains(previewId)) {
			result.add(previewId);
		}
		return result;
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.USER_PROFILE;
	}

}
