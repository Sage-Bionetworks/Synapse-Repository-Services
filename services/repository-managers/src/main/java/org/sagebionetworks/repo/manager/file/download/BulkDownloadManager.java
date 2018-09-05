package org.sagebionetworks.repo.manager.file.download;

import org.sagebionetworks.repo.model.UserInfo;

public interface BulkDownloadManager {

	/**
	 * Add all of the files from the given folder to a user's download list.
	 * 
	 * @param user
	 * @param folderId
	 */
	public void addFilesFromFolder(UserInfo user, String folderId);

}
