package org.sagebionetworks.repo.web.controller.metadata;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Analysis;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 *
 */
public class AnalysisMetadataProvider implements
		TypeSpecificMetadataProvider<Analysis> {

	@Override
	public void addTypeSpecificMetadata(Analysis entity,
			HttpServletRequest request, UserInfo userInfo, EventType eventType)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// TODO Auto-generated method stub
	}

	@Override
	public void validateEntity(Analysis entity, EntityEvent event)
			throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException {
		if (null == entity.getName()) {
			throw new InvalidModelException("name cannot be null");
		}
		if (null == entity.getDescription()) {
			throw new InvalidModelException("description cannot be null");
		}
	}

	@Override
	public void entityDeleted(Analysis deleted) {
		// TODO Auto-generated method stub
	}
	
}