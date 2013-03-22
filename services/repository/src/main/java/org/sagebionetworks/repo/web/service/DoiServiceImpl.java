package org.sagebionetworks.repo.web.service;

import java.util.Date;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiObjectType;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.web.NotFoundException;

public class DoiServiceImpl implements DoiService {

	@Override
	public Doi createDoi(String userId, String objectId, DoiObjectType objectType, Long versionNumber)
			throws NotFoundException, UnauthorizedException, DatastoreException {
		Doi doi = new Doi();
		doi.setCreatedBy("createdBy");
		doi.setCreatedOn(new Date());
		doi.setDoiStatus(DoiStatus.IN_PROCESS);
		doi.setId("id");
		doi.setObjectId("objectId");
		doi.setObjectType(DoiObjectType.ENTITY);
		doi.setObjectVersion(1L);
		doi.setUpdatedOn(new Date());
		return doi;
	}

	@Override
	public Doi getDoi(String userId, String objectId, DoiObjectType objectType, Long versionNumber)
			throws NotFoundException, UnauthorizedException, DatastoreException {
		Doi doi = new Doi();
		doi.setCreatedBy("createdBy");
		doi.setCreatedOn(new Date());
		doi.setDoiStatus(DoiStatus.READY);
		doi.setId("id");
		doi.setObjectId("objectId");
		doi.setObjectType(DoiObjectType.ENTITY);
		doi.setObjectVersion(1L);
		doi.setUpdatedOn(new Date());
		return doi;
	}
}
