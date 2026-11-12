package org.sagebionetworks.repo.manager.file;

import java.util.Collection;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.web.NotFoundException;

public interface FileHandleAuthorizationManager {

	/**
	 * Is the user an admin or the creator of the given FileHandle?
	 * 
	 * @return whether access is granted and, if not, a String giving the reason why
	 * @throws NotFoundException If the file handle does not exist.
	 */
	AuthorizationStatus canAccessRawFileHandleById(UserInfo userInfo, String fileHandleId) throws NotFoundException;

	/**
	 * Is the user an admin or the creator of every one of the given FileHandles? The file handles are
	 * looked up in a single batch.
	 * 
	 * @return Authorized when the collection is empty; otherwise a single status whose denial names
	 *         every file handle the user did not create.
	 * @throws NotFoundException If any of the file handles does not exist, including for admins.
	 */
	AuthorizationStatus canAccessRawFileHandlesByIds(UserInfo userInfo, Collection<String> fileHandleIds)
			throws NotFoundException;

	/**
	 * Is the user an admin or the creator of every one of the given, already loaded, FileHandles?
	 * 
	 * @return Authorized when the collection is empty; otherwise a single status whose denial names
	 *         every file handle the user did not create.
	 */
	AuthorizationStatus canAccessRawFileHandles(UserInfo userInfo, Collection<FileHandle> fileHandles);
}
