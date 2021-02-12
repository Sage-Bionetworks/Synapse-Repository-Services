package org.sagebionetworks.repo.manager.dataaccess;

import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.AccessRestrictionStatusDao;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RestrictionInformationManagerImpl implements RestrictionInformationManager {

	@Autowired
	private AccessRestrictionStatusDao accessRestrictionStatusDao;

	@Override
	public RestrictionInformationResponse getRestrictionInformation(UserInfo userInfo,
			RestrictionInformationRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getObjectId(), "RestrictionInformationRequest.objectId");
		ValidateArgument.required(request.getRestrictableObjectType(),
				"RestrictionInformationRequest.restrictableObjectType");
		if (RestrictableObjectType.ENTITY != request.getRestrictableObjectType()
				&& RestrictableObjectType.TEAM != request.getRestrictableObjectType()) {
			throw new IllegalArgumentException("Unsupported type: " + request.getRestrictableObjectType());
		}

		List<UsersRestrictionStatus> statusList = accessRestrictionStatusDao.getSubjectStatus(
				Arrays.asList(KeyFactory.stringToKey(request.getObjectId())), request.getRestrictableObjectType(),
				userInfo.getId());

		return statusList.stream().findFirst().map((s) -> {
			RestrictionInformationResponse info = new RestrictionInformationResponse();
			info.setHasUnmetAccessRequirement(s.hasUnmet());
			info.setRestrictionLevel(s.getMostRestrictiveLevel());
			return info;
		}).orElseGet(() -> {
			// If there are no restrictions then the data is open and met.
			RestrictionInformationResponse info = new RestrictionInformationResponse();
			info.setHasUnmetAccessRequirement(false);
			info.setRestrictionLevel(RestrictionLevel.OPEN);
			return info;
		});
	}
	
	@Override
	public List<UsersRestrictionStatus> getEntityRestrictionInformation(UserInfo userInfo, List<Long> entityIds){
		return accessRestrictionStatusDao.getEntityStatus(entityIds, userInfo.getId());
	}

}
