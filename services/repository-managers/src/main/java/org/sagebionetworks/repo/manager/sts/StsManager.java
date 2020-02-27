package org.sagebionetworks.repo.manager.sts;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.sts.StsCredentials;
import org.sagebionetworks.repo.model.sts.StsPermission;

/** Manages AWS Security Token Service (STS) calls and the restrictions around it. */
public interface StsManager {
	/** Gets the temporary S3 credentials from STS for the given entity. */
	StsCredentials getTemporaryCredentials(UserInfo userInfo, String entityId, StsPermission permission);

	/**
	 * Validates whether putting a FileEntity with the given file handle inside the given parent is valid with respect
	 * to our STS restrictions. Throws an IllegalArgumentException if it is invalid.
	 */
	void validateCanAddFile(UserInfo userInfo, String fileHandleId, String parentId);

	/**
	 * Validates whether moving the given folder from the old parent to the new parent is valid with respect to our STS
	 * restrictions. Throws an IllegalArgumentException if it is invalid.
	 */
	void validateCanMoveFolder(UserInfo userInfo, String folderId, String oldParentId, String newParentId);
}
