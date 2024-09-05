package org.sagebionetworks.repo.service;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

public interface DoiServiceV2 {

	/**
	 * Gets the DOI for the specified entity version, with all associated metadata.
	 */
	Doi getDoi(String objectId, ObjectType objectType, Long versionNumber)
			throws NotFoundException, UnauthorizedException, ServiceUnavailableException;


	/**
	 * Gets the DOI Association for the specified entity version.
	 */
	DoiAssociation getDoiAssociation(String objectId, ObjectType objectType, Long versionNumber)
			throws NotFoundException, UnauthorizedException;

	/**
	 * Redirect to the object in the Synapse web portal.
	 */
	String locate(String objectId, ObjectType objectType, Long versionNumber)
			throws NotFoundException, UnauthorizedException;


}
