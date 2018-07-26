package org.sagebionetworks.repo.manager.doi;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Manager for creating/updating/retrieving DOIs assigned to Entities.
 */
public interface EntityDoiManager {

	/**
	 * Create a DOI. If the DOI already exists without error, returns it.
	 * @param userId The user ID requesting creation.
	 * @param objectId The ID of the entity which will be assigned a DOI.
	 * @param versionNumber The version number of the entity, or null to
	 *                      create a DOI that always refers to the most
	 *                      recent version.
	 * @return A DOI DTO corresponding to the created DOI.
	 * @throws NotFoundException The entity could not be found.
	 * @throws UnauthorizedException The user is not authorized to create
	 * 								 a DOI for this object.
	 */
	Doi createDoi(Long userId, String objectId, Long versionNumber)
			throws NotFoundException, UnauthorizedException;

	/**
	 * Retrieve the DOI (if it exists) for the entity defined by the input parameters.
	 * @param userId The user ID requesting creation.
	 * @param objectId The ID of the entity.
	 * @param versionNumber The version number of the entity.
	 * @return A DOI DTO corresponding to the DOI for the given object.
	 */
	Doi getDoiForVersion(Long userId, String objectId, Long versionNumber)
			throws NotFoundException, UnauthorizedException;

	/**
	 * Retrieve the DOI (if it exists) for the current version of the entity
	 * defined by the input parameters.
	 * @param userId The user ID requesting creation.
	 * @param objectId The ID of the entity.
	 * @return A DOI DTO corresponding to the DOI for the given object.
	 */
	Doi getDoiForCurrentVersion(Long userId, String objectId)
			throws NotFoundException, UnauthorizedException;
}
