package org.sagebionetworks.repo.manager.s3folder;

import com.amazonaws.services.securitytoken.model.Credentials;
import org.sagebionetworks.repo.model.Folder;

// todo doc
public interface S3FolderManager {
	// todo doc
	enum Permissions {
		READ_ONLY,
		READ_WRITE,
	}

	// todo doc
	Credentials getTemporaryCredentials(Folder folder, Permissions permissions);
}
