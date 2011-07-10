package org.sagebionetworks.repo.web.controller.metadata;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Eula;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 *
 */
public class EulaMetadataProvider implements
		TypeSpecificMetadataProvider<Eula> {

	@Override
	public void addTypeSpecificMetadata(Eula entity,
			HttpServletRequest request, UserInfo user, EventType eventType) throws DatastoreException,
			NotFoundException {
		// TODO Auto-generated method stub
	}

	@Override
	public void validateEntity(Eula entity, EventType eventType)
			throws InvalidModelException {
		if (null == entity.getName()) {
			throw new InvalidModelException("name cannot be null");
		}
		if (null == entity.getAgreement()) {
			throw new InvalidModelException("agreement cannot be null");
		}
	}

	@Override
	public void entityDeleted(Eula deleted) {
		// TODO Auto-generated method stub
	}

}
