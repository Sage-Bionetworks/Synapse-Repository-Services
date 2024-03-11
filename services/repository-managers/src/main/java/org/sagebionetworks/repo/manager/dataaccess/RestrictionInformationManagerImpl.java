package org.sagebionetworks.repo.manager.dataaccess;

import java.util.List;
import java.util.Optional;

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
				return getEntityRestrictionInformationResponse(request, userInfo);
			case TEAM:
				return getTeamRestrictionInformationResponse(request, userInfo);
			default:
				throw new IllegalArgumentException("Unsupported type: " + request.getRestrictableObjectType());

		}
	}

	private RestrictionInformationResponse getEntityRestrictionInformationResponse(RestrictionInformationRequest request,
																							 UserInfo userInfo){
		long objectId = KeyFactory.stringToKey(request.getObjectId());
		EntityStateProvider stateProvider = new LazyEntityStateProvider(accessRestrictionStatusDao,
				usersEntityPermissionsDao, userInfo, KeyFactory.stringToKeySingletonList(request.getObjectId()));
		UsersRestrictionStatus usersRestrictionStatus = stateProvider.getRestrictionStatus(objectId);

		if(usersRestrictionStatus.getAccessRestrictions().isEmpty()){
			return getOpenAndMetRestrictionInformationResponse();
		}else {
			List<Long> unmetARIdList = UserAccessRestrictionUtils.usersUnmetAccessRestrictionsForEntity(
					stateProvider.getPermissionsState(objectId), usersRestrictionStatus);
			return new RestrictionInformationResponse().setHasUnmetAccessRequirement(!unmetARIdList.isEmpty())
					.setRestrictionLevel(usersRestrictionStatus.getMostRestrictiveLevel());
		}
	}

	private RestrictionInformationResponse getTeamRestrictionInformationResponse(RestrictionInformationRequest request,
																							 UserInfo userInfo){
		long objectId = KeyFactory.stringToKey(request.getObjectId());

		Optional<UsersRestrictionStatus> usersRestrictionStatus = accessRestrictionStatusDao.getNonEntityStatus(List.of(objectId),
				request.getRestrictableObjectType(), userInfo.getId()).stream().findFirst();
		if(usersRestrictionStatus.isEmpty() || usersRestrictionStatus.get().getAccessRestrictions().isEmpty()){
			return getOpenAndMetRestrictionInformationResponse();
		}else {
			List<Long> unmetARIdList = UserAccessRestrictionUtils.usersUnmetAccessRestrictionsForNonEntity(usersRestrictionStatus.get());
			return new RestrictionInformationResponse().setHasUnmetAccessRequirement(!unmetARIdList.isEmpty())
							.setRestrictionLevel(usersRestrictionStatus.get().getMostRestrictiveLevel());
		}
	}

	private RestrictionInformationResponse getOpenAndMetRestrictionInformationResponse(){
		// In case there is no restrictions then the data is open and met.
		return new  RestrictionInformationResponse().setHasUnmetAccessRequirement(false)
				.setRestrictionLevel(RestrictionLevel.OPEN);
	}
}
