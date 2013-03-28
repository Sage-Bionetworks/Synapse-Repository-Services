package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.manager.doi.EntityDoiManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class DoiServiceImpl implements DoiService {

	@Autowired private EntityDoiManager entityDoiManager;

	@Override
	public Doi createDoi(String userId, String objectId, DoiObjectType objectType, Long versionNumber)
			throws NotFoundException, UnauthorizedException, DatastoreException {

		if (objectType == null) {
			throw new IllegalArgumentException("Object type cannot be null.");
		}

		if (DoiObjectType.ENTITY.equals(objectType)) {
			return entityDoiManager.createDoi(userId, objectId, versionNumber);
		} else {
			throw new IllegalArgumentException(objectType + " does not support DOIs.");
		}
	}

	@Override
	public Doi getDoi(String userId, String objectId, DoiObjectType objectType, Long versionNumber)
			throws NotFoundException, UnauthorizedException, DatastoreException {

		if (objectType == null) {
			throw new IllegalArgumentException("Object type cannot be null.");
		}

		if (DoiObjectType.ENTITY.equals(objectType)) {
			return entityDoiManager.getDoi(userId, objectId, versionNumber);
		} else {
			throw new IllegalArgumentException(objectType + " does not support DOIs.");
		}
	}
}
