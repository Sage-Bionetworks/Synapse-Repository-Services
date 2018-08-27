package org.sagebionetworks.repo.manager.doi;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

public interface DoiManager {

	/**
	 * Retrieves a DOI with all associated metadata.
	 * Note that this method calls an external API, which may affect responsiveness.
	 * @param userId The ID of the user making the call.
	 * @param objectId The ID of the object in Synapse
	 * @param objectType The type of the object
	 * @param versionNumber The version of the object. If null, refers to the most recent version.
	 * @return A DOI with all associated metadata.
	 * @throws RecoverableMessageException if the external API call failed. Consider retrying.
	 */
	Doi getDoi(final Long userId, final String objectId, final ObjectType objectType, final Long versionNumber) throws ServiceUnavailableException;

	/**
	 * Retrieves the data referring to the association of a DOI with an object in Synapse.
	 * @param userId The ID of the user making the call.
	 * @param objectId The ID of the object in Synapse
	 * @param objectType The type of the object
	 * @param versionNumber The version of the object. If null, refers to the most recent version.
	 * @return The data transfer object for the DOI association.
	 */
	DoiAssociation getDoiAssociation(final Long userId, final String objectId, final ObjectType objectType, final Long versionNumber);

	/**
	 * Mints or updates a DOI with all associated metadata. This method is idempotent.
	 * Note that this method calls an external API, which may affect responsiveness.
	 * @param userId The ID of the user making the call.
	 * @param dto A Doi object containing data needed to both mint a DOI and set the required metadata.
	 * @return A DOI with all associated metadata.
	 * @throws RecoverableMessageException if the external API call failed or a creation attempt failed. Consider retrying.
	 */
	Doi createOrUpdateDoi(final Long userId, final Doi dto) throws RecoverableMessageException;

	/**
	 * Deactivates an existing DOI. Note that this does not delete the DOI and the DOI will still be resolvable.
	 * The DOI will no longer be visible to users of external APIs, but it will still be visible to Synapse users.
	 * To reactivate, update the DOI and set the status to FINDABLE.
	 * Note that this method calls an external API, which may affect responsiveness.
	 * @param userId The ID of the user making the call.
	 * @param objectId The ID of the object in Synapse
	 * @param objectType The type of the object
	 * @param versionNumber The version of the object. If null, refers to the most recent version.
	 * @throws RecoverableMessageException if the external API call failed. Consider retrying.
	 */
	void deactivateDoi(final Long userId, final String objectId, final ObjectType objectType, final Long versionNumber) throws RecoverableMessageException;

	/**
	 * Retrieve the URL of an object in the Synapse web portal.
	 * @param objectId The ID of an object in Synapse
	 * @param objectType The type of the object
	 * @param versionNumber The version number of the object. If null, the URL will always refer to the current version.
	 * @return The URL of the object in the web portal
	 */
	String getLocation(final String objectId, final ObjectType objectType, final Long versionNumber);

	/**
	 * Create a URL for an object that points to the locator API, which will redirect to an object.
	 * @param objectId The ID of an object in Synapse
	 * @param objectType The type of the object
	 * @param versionNumber The version of the object. If null, the URL will always refer to the current version.
	 * @return A URL that points to the locator API that will redirect to the object in the web portal
	 */
	String generateLocationRequestUrl(final String objectId, final ObjectType objectType, final Long versionNumber);

}
