package org.sagebionetworks.repo.manager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserProfileFileHandleAssociationProvider implements FileHandleAssociationProvider{

	private UserProfileDAO userProfileDAO;
	
	private FileHandleDao fileHandleDao;
	
	@Autowired
	public UserProfileFileHandleAssociationProvider(UserProfileDAO userProfileDAO, FileHandleDao fileHandleDao) {
		this.userProfileDAO = userProfileDAO;
		this.fileHandleDao = fileHandleDao;
	}
	
	@Override
	public FileHandleAssociateType getAssociateType() {
		return FileHandleAssociateType.UserProfileAttachment;
	}

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		Set<String> result = new HashSet<String>();
		try {
			String handleId = userProfileDAO.getPictureFileHandleId(objectId);
			String previewId = fileHandleDao.getPreviewFileHandleId(handleId);
			if (fileHandleIds.contains(handleId)) {
				result.add(handleId);
			}
			if (fileHandleIds.contains(previewId)) {
				result.add(previewId);
			}
		} catch (NotFoundException e) {
			// the user does not have a profile picture or the preview hasn't been generated yet
			// just return what we've got.
		}
		return result;
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.USER_PROFILE;
	}

}
