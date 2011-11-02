package org.sagebionetworks.repo.web.controller.metadata;

import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Eula;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 *
 */
public class EulaMetadataProvider implements TypeSpecificMetadataProvider<Eula> {

	@Override
	public void addTypeSpecificMetadata(Eula entity,
			HttpServletRequest request, UserInfo user, EventType eventType)
			throws DatastoreException, NotFoundException {
		// Clear the blob and set the string
	}

	@Override
	public void validateEntity(Eula entity, EntityEvent event)
			throws InvalidModelException {
		// Convert the blob value to the string value
	}

	@Override
	public void entityDeleted(Eula deleted) {
		// TODO Auto-generated method stub
	}

}
