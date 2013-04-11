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
	public void clear(String userName) throws NotFoundException, UnauthorizedException, DatastoreException {
		if (userName == null || userName.isEmpty()) {
			throw new IllegalArgumentException("User name cannot be null or empty.");
		}
		UserInfo currentUser = userManager.getUserInfo(userName);
		UserInfo.validateUserInfo(currentUser);
		if (!currentUser.isAdmin()) {
			throw new UnauthorizedException("User must be an administrator to clear the DOI table.");
		}
		doiAdminDao.clear();
	}
}
