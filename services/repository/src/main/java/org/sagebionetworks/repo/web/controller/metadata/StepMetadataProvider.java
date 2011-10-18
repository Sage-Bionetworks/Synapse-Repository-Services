package org.sagebionetworks.repo.web.controller.metadata;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Step;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 *
 */
public class StepMetadataProvider implements TypeSpecificMetadataProvider<Step> {

	@Override
	public void addTypeSpecificMetadata(Step entity,
			HttpServletRequest request, UserInfo userInfo, EventType eventType)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// TODO Auto-generated method stub
	}

	@Override
	public void validateEntity(Step entity, EntityEvent event)
			throws InvalidModelException, NotFoundException,
			DatastoreException, UnauthorizedException {
		if (null == entity.getStartDate()) {
			if (EventType.CREATE == event.getType()) {
				entity.setStartDate(new Date()); // set the startDate to now
			} else {
				throw new InvalidModelException(
						"startDate cannot changed to null");
			}
		}
		// TODO when no parentId is specified, these currently go under the root
		// folder. Instead do we want these to go in a folder owned by the user
		// that we create on their behalf?
	}

	@Override
	public void entityDeleted(Step deleted) {
		// TODO Auto-generated method stub
	}
}
