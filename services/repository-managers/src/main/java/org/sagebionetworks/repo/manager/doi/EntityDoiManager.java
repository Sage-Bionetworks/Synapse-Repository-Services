package org.sagebionetworks.repo.manager.doi;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.web.NotFoundException;

public interface EntityDoiManager {

	Doi createDoi(Long userId, String objectId, Long versionNumber)
			throws NotFoundException, UnauthorizedException, DatastoreException;

	Doi getDoiForVersion(Long userId, String objectId, Long versionNumber)
			throws NotFoundException, UnauthorizedException, DatastoreException;
	
	Doi getDoiForCurrentVersion(Long userId, String objectId)
			throws NotFoundException, UnauthorizedException, DatastoreException;
}
