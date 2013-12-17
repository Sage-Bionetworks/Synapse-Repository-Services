package org.sagebionetworks.repo.manager.doi;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DoiAdminDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class DoiAdminManagerImpl implements DoiAdminManager {

	@Autowired private UserManager userManager;
	@Autowired private DoiAdminDao doiAdminDao;

	@Override
	public void clear(Long userId) throws NotFoundException, UnauthorizedException, DatastoreException {
		if (userId == null) {
			throw new IllegalArgumentException("User name cannot be null or empty.");
		}
		UserInfo currentUser = userManager.getUserInfo(userId);
		UserInfo.validateUserInfo(currentUser);
		if (!currentUser.isAdmin()) {
			throw new UnauthorizedException("User must be an administrator to clear the DOI table.");
		}
		doiAdminDao.clear();
	}
}
