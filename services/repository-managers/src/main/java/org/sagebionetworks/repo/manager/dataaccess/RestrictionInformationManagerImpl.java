package org.sagebionetworks.repo.manager.dataaccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.manager.entity.EntityStateProvider;
import org.sagebionetworks.repo.manager.entity.LazyEntityStateProvider;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.AccessRestrictionStatusDao;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.dbo.entity.UsersEntityPermissionsDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.manager.util.UserAccessRestrictionUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RestrictionInformationManagerImpl implements RestrictionInformationManager {

	@Autowired
	private AccessRestrictionStatusDao accessRestrictionStatusDao;
	@Autowired
	private UsersEntityPermissionsDao usersEntityPermissionsDao;
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

		List<UsersRestrictionStatus> statusList = new ArrayList<>();
		boolean hasUnmet = populateUsersRestrictionStatusAndGetHasUnmetRestrictions(request, userInfo, statusList);

		return statusList.stream().findFirst().map((s) -> {
			RestrictionInformationResponse info = new RestrictionInformationResponse();
			info.setHasUnmetAccessRequirement(hasUnmet);
			info.setRestrictionLevel(s.getMostRestrictiveLevel());
			return info;
		}).orElseGet(() -> {
			// If there are no restrictions then the data is open and met.
			RestrictionInformationResponse info = new RestrictionInformationResponse();
			info.setHasUnmetAccessRequirement(hasUnmet);
			info.setRestrictionLevel(RestrictionLevel.OPEN);
			return info;
		});
	}

	private boolean populateUsersRestrictionStatusAndGetHasUnmetRestrictions(RestrictionInformationRequest request,
																			 UserInfo userInfo,
																			 List<UsersRestrictionStatus> statusList) {
		long objectId = KeyFactory.stringToKey(request.getObjectId());

		if (RestrictableObjectType.ENTITY.equals(request.getRestrictableObjectType())) {
			EntityStateProvider stateProvider = new LazyEntityStateProvider(accessRestrictionStatusDao,
					usersEntityPermissionsDao, userInfo, KeyFactory.stringToKeySingletonList(request.getObjectId()));
			statusList.add(stateProvider.getRestrictionStatus(objectId));
			return UserAccessRestrictionUtils.doesUserHaveUnmetAccessRestrictionsForEntity(
					stateProvider.getPermissionsState(objectId), stateProvider.getRestrictionStatus(objectId));
		}

		statusList.addAll(accessRestrictionStatusDao.getNonEntityStatus(Arrays.asList(objectId),
				request.getRestrictableObjectType(), userInfo.getId()));
		return UserAccessRestrictionUtils.doesUserHaveUnmetAccessRestrictionsForNonEntity(
				statusList.stream().findFirst().orElse(new UsersRestrictionStatus()));
	}
}
