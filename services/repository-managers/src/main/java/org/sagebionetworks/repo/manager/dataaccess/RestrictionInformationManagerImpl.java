package org.sagebionetworks.repo.manager.dataaccess;

import java.util.List;

import org.sagebionetworks.repo.manager.entity.EntityStateProvider;
import org.sagebionetworks.repo.manager.entity.LazyEntityStateProvider;
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
	public static final RestrictionInformationResponse DEFAULT_RESPONSE =
			new RestrictionInformationResponse().setHasUnmetAccessRequirement(false).setRestrictionLevel(RestrictionLevel.OPEN);

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

		switch (request.getRestrictableObjectType()) {
			case ENTITY:
				return getEntityRestrictionInformationResponse(userInfo, request);
			case TEAM:
				return getTeamRestrictionInformationResponse(userInfo, request);
			default:
				throw new IllegalArgumentException("Unsupported type: " + request.getRestrictableObjectType());

		}
	}

	RestrictionInformationResponse getEntityRestrictionInformationResponse(UserInfo userInfo,
																		   RestrictionInformationRequest request) {
		long objectId = KeyFactory.stringToKey(request.getObjectId());
		EntityStateProvider stateProvider = new LazyEntityStateProvider(accessRestrictionStatusDao,
				usersEntityPermissionsDao, userInfo, KeyFactory.stringToKeySingletonList(request.getObjectId()));
		UsersRestrictionStatus usersRestrictionStatus = stateProvider.getRestrictionStatus(objectId);

		if (usersRestrictionStatus.getAccessRestrictions().isEmpty()) {
			// If there are no restrictions then the data is open and met.
			return DEFAULT_RESPONSE;
		} else {
			List<Long> unmetARIdList = UserAccessRestrictionUtils.getUsersUnmetAccessRestrictionsForEntity(
					stateProvider.getPermissionsState(objectId), usersRestrictionStatus);
			return new RestrictionInformationResponse().setHasUnmetAccessRequirement(!unmetARIdList.isEmpty())
					.setRestrictionLevel(usersRestrictionStatus.getMostRestrictiveLevel());
		}
	}

	RestrictionInformationResponse getTeamRestrictionInformationResponse(UserInfo userInfo,
																		 RestrictionInformationRequest request) {
		long objectId = KeyFactory.stringToKey(request.getObjectId());

		return accessRestrictionStatusDao.getNonEntityStatus(List.of(objectId),
				request.getRestrictableObjectType(), userInfo.getId()).stream().findFirst().map((restrictionStatus) -> {
			List<Long> unmetARIdList = UserAccessRestrictionUtils.getUsersUnmetAccessRestrictionsForNonEntity(restrictionStatus);
			RestrictionInformationResponse info = new RestrictionInformationResponse();
			info.setHasUnmetAccessRequirement(!unmetARIdList.isEmpty());
			info.setRestrictionLevel(restrictionStatus.getMostRestrictiveLevel());
			return info;
		}).orElseGet(() -> {
			// If there are no restrictions then the data is open and met.
			return DEFAULT_RESPONSE;
		});
	}
}
