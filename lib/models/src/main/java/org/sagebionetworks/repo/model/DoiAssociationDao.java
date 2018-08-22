package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Data operations for DOIs.
 */
public interface DoiAssociationDao {

	/**
	 * Creates a DOI for the specified entity version. If the version number is null,
	 * the DOI will be associated with the most recent version if applicable.
	 * @param dto The DTO for the DOI entry to create, containing object id,
	 *            object type, object version (if applicable), and createdBy.
	 * @return A DTO that corresponds to the new database entry matching the input DTO.
	 */
	DoiAssociation createDoiAssociation(DoiAssociation dto);

	/**
	 * Updates a DOI for the specified entity version. If the version number is null,
	 * the DOI will be associated with the most recent version if applicable.
	 * @param dto The DTO for the existing DOI entry to update, containing object id,
	 *            object type, object version (if applicable), and createdBy.
	 * @return A DTO that corresponds to the updated database entry matching the input DTO.
	 */
	DoiAssociation updateDoiAssociation(DoiAssociation dto);


	/**
	 * Gets the DOI that has the specified ID.
	 * @param id The ID of the DOI object
	 * @return The DTO for the specified DOI.
	 * @throws NotFoundException An existing DOI was not found
	 */
	DoiAssociation getDoiAssociation(String id) throws NotFoundException;

	/**
	 * Gets the DOI for the specified object. If version number is null,
	 * the DOI associated with the most recent version will be retrieved.
	 * @param objectId The ID of the object to which a DOI refers
	 * @param objectType The type of the object.
	 * @param versionNumber The version number of the object. Null refers to
	 *                      the most recent version
	 * @return The DTO for the specified DOI.
	 * @throws NotFoundException An existing DOI was not found
	 */
	DoiAssociation getDoiAssociation(String objectId, ObjectType objectType, Long versionNumber) throws NotFoundException;

	/**
	 * Gets the Etag of the DOI for the specified object. If version number
	 * is null, the etag for the DOI associated with the most recent version
	 * will be retrieved. Uses SELECT ... FOR UPDATE
	 * @param objectId The ID of the object to which a DOI refers
	 * @param objectType The type of the object.
	 * @param versionNumber The version number of the object. Null refers to
	 *                      the most recent version
	 * @return The Etag of the specified DOI object.
	 * @throws NotFoundException An existing DOI was not found
	 */
	String getEtagForUpdate(String objectId, ObjectType objectType, Long versionNumber) throws NotFoundException;
}
