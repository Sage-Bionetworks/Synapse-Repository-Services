package org.sagebionetworks.repo.web.controller.metadata;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Step;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.util.ReferenceUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 */
public class StepMetadataProvider implements TypeSpecificMetadataProvider<Step> {
	
	@Autowired
	ReferenceUtil referenceUtil;

	@Override
	public void addTypeSpecificMetadata(Step entity,
			HttpServletRequest request, UserInfo userInfo, EventType eventType)
			throws NotFoundException, DatastoreException, UnauthorizedException {
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
		// For steps validate that all references have a version number.
		if(referenceUtil != null){
			referenceUtil.replaceNullVersionNumbersWithCurrent(entity.getCode());
			referenceUtil.replaceNullVersionNumbersWithCurrent(entity.getInput());
			referenceUtil.replaceNullVersionNumbersWithCurrent(entity.getOutput());
		}
		
	}

	@Override
	public void entityDeleted(Step deleted) {
		// TODO Auto-generated method stub
	}
}
