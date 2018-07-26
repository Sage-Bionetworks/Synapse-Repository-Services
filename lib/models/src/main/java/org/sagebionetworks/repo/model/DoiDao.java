package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Data operations for DOIs.
 */
public interface DoiDao {

	/**
	 * Creates a DOI for the specified entity version. If the version number is null,
	 * the DOI will be associated with the most recent version if applicable.
	 * @param dto The DTO for the DOI entry to create, containing object id,
	 *            object type, object version (if applicable), and createdBy.
	 * @return A DTO that corresponds to the new database entry matching the input DTO.
	 */
	Doi createDoi(Doi dto) throws DatastoreException;

	/**
	 * Updates a DOI status.
 	 * @param dto A DOI DTO containing object ID, object type, object version
	 *            (if applicable), etag, and a new DoiStatus.
	 * @return A DTO that corresponds to the updated database entry.
	 */
	Doi updateDoiStatus(Doi dto) throws NotFoundException,
			DatastoreException, ConflictingUpdateException;

	/**
	 * Gets the DOI for the specified entity version. If version number is null,
	 * the DOI will be associated with the most recent version will be retrieved.
	 * @param dto A DTO that corresponds to an existing database entry based on
	 *            object ID, object type, and object version (if applicable).
	 * @return A matching DTO containing additional information stored in the database.
	 */
	Doi getDoi(Doi dto)
			throws NotFoundException, DatastoreException;
}
