package org.sagebionetworks.repo.manager.file;

import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;

public interface FileHandleAuthorizationManager {

	/**
	 * Given a mixed list of FileHandleAssociation determine if the user is authorized to download each file.
	 * @see #canDownloadFile(UserInfo, List, String, FileHandleAssociateType)
	 * @param user
	 * @param associations
	 * @return
	 */
	public List<FileHandleAssociationAuthorizationStatus> canDownLoadFile(UserInfo user, List<FileHandleAssociation> associations);
}
