package org.sagebionetworks.repo.manager.file;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.web.NotFoundException;

public interface FileHandleAuthorizationManager {

	
	/**
	 * Is the user the creator of the given FileHnadle?
	 * 
	 * @param userInfo
	 * @param fileHandleId
	 * @return whether access is granted and, if not, a String giving the reason why
	 * @throws NotFoundException 
	 */
	AuthorizationStatus canAccessRawFileHandleById(UserInfo userInfo, String fileHandleId) throws NotFoundException;
}
