package org.sagebionetworks.repo.manager.doi;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.model.doi.v2.DoiObjectType;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

public interface DoiManager {

	/**
	 * Retrieves a DOI with all associated metadata.
	 * Note that this method calls an external API, which may affect responsiveness.
	 * @param portalId The id of the portal that the DOI belongs to, if omitted will default to the Synapse.org portal
	 * @param objectId The ID of the object in Synapse
	 * @param objectType The type of the object
	 * @param versionNumber The version of the object. If null, refers to the most recent version.
	 * @return A DOI with all associated metadata.
	 * @throws RecoverableMessageException if the external API call failed. Consider retrying.
	 */
	Doi getDoi(String portalId, String objectId, DoiObjectType objectType, Long versionNumber) throws ServiceUnavailableException;

	/**
	 * Retrieves the data referring to the association of a DOI with an object in Synapse.
	 * @param portalId The id of the portal that the DOI belongs to, if omitted will default to the Synapse.org portal
	 * @param objectId The ID of the object in Synapse
	 * @param objectType The type of the object
	 * @param versionNumber The version of the object. If null, refers to the most recent version.
	 * @return The data transfer object for the DOI association.
	 */
	DoiAssociation getDoiAssociation(String portalId, String objectId, DoiObjectType objectType, Long versionNumber);

	/**
	 * Mints or updates a DOI with all associated metadata. This method is idempotent.
	 * Note that this method calls an external API, which may affect responsiveness.
	 * @param user The UserInfo of the user making the call.
	 * @param dto A Doi object containing data needed to both mint a DOI and set the required metadata.
	 * @return A DOI with all associated metadata.
	 * @throws RecoverableMessageException if the external API call failed or a creation attempt failed. Consider retrying.
	 */
	Doi createOrUpdateDoi(UserInfo user, Doi dto) throws RecoverableMessageException;

	/**
	 * Deactivates an existing DOI. Note that this does not delete the DOI and the DOI will still be resolvable.
	 * The DOI will no longer be visible to users of external APIs, but it will still be visible to Synapse users.
	 * To reactivate, update the DOI and set the status to FINDABLE.
	 * Note that this method calls an external API, which may affect responsiveness.
	 * 
	 * @param user The UserInfo of the user making the call.
	 * @param portalId The id of the portal that the DOI belongs to, if omitted will default to the Synapse.org portal
	 * @param objectId The ID of the object in Synapse
	 * @param objectType The type of the object
	 * @param versionNumber The version of the object. If null, refers to the most recent version.
	 * @throws RecoverableMessageException if the external API call failed. Consider retrying.
	 */
	void deactivateDoi(UserInfo user, String portalId,  String objectId, DoiObjectType objectType, Long versionNumber) throws RecoverableMessageException;

	/**
	 * Retrieve the URL of an object in the Synapse web portal.
	 * @param portalId The id of the portal that the DOI belongs to, if omitted will default to the Synapse.org portal
	 * @param objectId The ID of an object in Synapse
	 * @param objectType The type of the object
	 * @param versionNumber The version number of the object. If null, the URL will always refer to the current version.
	 * @return The URL of the object in the web portal
	 */
	String getLocation(String portalId, String objectId, DoiObjectType objectType, Long versionNumber);

}
