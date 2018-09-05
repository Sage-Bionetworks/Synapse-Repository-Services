package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.manager.doi.DoiManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;

public class DoiServiceV2Impl implements DoiServiceV2 {

	@Autowired
	private DoiManager doiManager;

	@Override
	public Doi getDoi(Long userId, String objectId, ObjectType objectType, Long versionNumber)
			throws NotFoundException, UnauthorizedException, ServiceUnavailableException {
		return doiManager.getDoi(objectId, objectType, versionNumber);
	}
	
	@Override
	public DoiAssociation getDoiAssociation(Long userId, String objectId, ObjectType objectType, Long versionNumber)
			throws NotFoundException, UnauthorizedException {
		return doiManager.getDoiAssociation(objectId, objectType, versionNumber);
	}

	/**
	 * Redirect to the object in the Synapse web portal.
	 */
	@Override
	public String locate(Long userId, String objectId, ObjectType objectType, Long versionNumber)
			throws NotFoundException, UnauthorizedException {
		if (objectId == null) {
			throw new IllegalArgumentException("Object ID cannot be null.");
		}
		if (objectType == null) {
			throw new IllegalArgumentException("Object type cannot be null.");
		}
		return doiManager.getLocation(objectId, objectType, versionNumber);
	}

}
