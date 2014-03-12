package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.web.NotFoundException;

public interface DoiService {

	/**
	 * Creates a DOI for the specified entity version. If the version number is null,
	 * the DOI will associated with the most recent version if applicable.
	 */
	Doi createDoi(Long userId, String objectId, ObjectType objectType, Long versionNumber)
			throws NotFoundException, UnauthorizedException, DatastoreException;

	/**
	 * Gets the DOI for the specified entity version. If version number is null,
	 * the DOI associated with the most recent version will be retrieved.
	 */
	Doi getDoi(Long userId, String objectId, ObjectType objectType, Long versionNumber)
			throws NotFoundException, UnauthorizedException, DatastoreException;
}
