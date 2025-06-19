package org.sagebionetworks.repo.service;

import org.sagebionetworks.repo.manager.doi.DoiManager;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.model.doi.v2.DoiObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DoiServiceV2Impl implements DoiServiceV2 {

	@Autowired
	private DoiManager doiManager;

	@Override
	public Doi getDoi(String portalId, String objectId, DoiObjectType objectType, Long versionNumber)
			throws NotFoundException, UnauthorizedException, ServiceUnavailableException {
		return doiManager.getDoi(portalId, objectId, objectType, versionNumber);
	}
	
	@Override
	public DoiAssociation getDoiAssociation(String portalId, String objectId, DoiObjectType objectType, Long versionNumber)
			throws NotFoundException, UnauthorizedException {
		return doiManager.getDoiAssociation(portalId, objectId, objectType, versionNumber);
	}

	/**
	 * Redirect to the object in the Synapse web portal.
	 */
	@Override
	public String locate(String portalId, String objectId, DoiObjectType objectType, Long versionNumber)
			throws NotFoundException, UnauthorizedException {
		if (objectId == null) {
			throw new IllegalArgumentException("Object ID cannot be null.");
		}
		if (objectType == null) {
			throw new IllegalArgumentException("Object type cannot be null.");
		}
		return doiManager.getLocation(portalId, objectId, objectType, versionNumber);
	}

}
