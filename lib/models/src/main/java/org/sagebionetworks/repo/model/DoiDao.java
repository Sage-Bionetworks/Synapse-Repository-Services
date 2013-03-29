package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiObjectType;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Data operations for DOIs.
 */
public interface DoiDao {

	/**
	 * Creates a DOI for the specified entity version. If the version number is null,
	 * the DOI will be associated with the most recent version if applicable.
	 */
	Doi createDoi(String userGroupId, String objectId, DoiObjectType objectType,
			Long versionNumber, DoiStatus doiStatus) throws DatastoreException;

	/**
	 * Updates a DOI's status.
	 */
	Doi updateDoiStatus(String objectId, DoiObjectType objectType, Long versionNumber,
			DoiStatus doiStatus, String etag) throws NotFoundException,
			DatastoreException, ConflictingUpdateException;

	/**
	 * Gets the DOI for the specified entity version. If version number is null,
	 * the DOI will be associated with the most recent version will be retrieved.
	 */
	Doi getDoi(String objectId, DoiObjectType objectType, Long versionNumber)
			throws NotFoundException, DatastoreException;
}
