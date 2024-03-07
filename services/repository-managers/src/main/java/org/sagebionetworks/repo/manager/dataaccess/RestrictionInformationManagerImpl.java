package org.sagebionetworks.repo.manager.dataaccess;

import org.sagebionetworks.repo.manager.entity.EntityStateProvider;
import org.sagebionetworks.repo.manager.entity.LazyEntityStateProvider;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.AccessRestrictionStatusDao;
import org.sagebionetworks.repo.model.ar.UserRestrictionStatusWithHasUnmet;
import org.sagebionetworks.repo.model.dbo.entity.UsersEntityPermissionsDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
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

		EntityStateProvider stateProvider = new LazyEntityStateProvider(accessRestrictionStatusDao,
				usersEntityPermissionsDao, userInfo, KeyFactory.stringToKeySingletonList(request.getObjectId()));

		UserRestrictionStatusWithHasUnmet userRestrictionStatusWithHasUnmet =
				stateProvider.getUserRestrictionStatusWithHasUnmet((KeyFactory.stringToKey(request.getObjectId())));

		return userRestrictionStatusWithHasUnmet.getUsersRestrictionStatus().getAccessRestrictions().stream().findFirst().map((s) -> {
					RestrictionInformationResponse info = new RestrictionInformationResponse();
					info.setHasUnmetAccessRequirement(userRestrictionStatusWithHasUnmet.hasUnmet());
					info.setRestrictionLevel(s.getRequirementType().getRestrictionLevel());
					return info;
		}).orElseGet(() -> {
			// If there are no restrictions then the data is open and met.
			RestrictionInformationResponse info = new RestrictionInformationResponse();
			info.setHasUnmetAccessRequirement(false);
			info.setRestrictionLevel(RestrictionLevel.OPEN);
			return info;
		});
	}
}
