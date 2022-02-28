package org.sagebionetworks.repo.manager.file;

import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileHandleAuthorizationManagerImpl implements FileHandleAuthorizationManager {

	private final FileHandleDao fileHandleDao;

	@Autowired
	public FileHandleAuthorizationManagerImpl(FileHandleDao fileHandleDao) {
		super();
		this.fileHandleDao = fileHandleDao;
	}

	@Override
	public AuthorizationStatus canAccessRawFileHandleById(UserInfo userInfo, String fileHandleId)
			throws NotFoundException {
		// Admins can do anything
		if (userInfo.isAdmin()) {
			return AuthorizationStatus.authorized();
		}
		// Lookup the creator by
		String creator = fileHandleDao.getHandleCreator(fileHandleId);
		// Call the other methods
		return AuthorizationUtils.canAccessRawFileHandleByCreator(userInfo, fileHandleId, creator);
	}

}
