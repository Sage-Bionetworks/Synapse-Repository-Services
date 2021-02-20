package org.sagebionetworks.repo.manager.entity;

import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.*;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_DOES_NOT_EXIST;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_HAS_NOT_ACCEPTED_TERMS_OF_USE;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_HAS_UNMET_ACCESS_RESTRICTIONS;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_IN_TRASH;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_IF_ADMIN;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_IF_OPEN_DATA_WITH_READ;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sagebionetworks.repo.manager.entity.decider.AccessContext;
import org.sagebionetworks.repo.manager.entity.decider.AccessDecider;
import org.sagebionetworks.repo.manager.entity.decider.UserInfoState;
import org.sagebionetworks.repo.manager.entity.decider.UsersEntityAccessInfo;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.AccessRestrictionStatusDao;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;
import org.sagebionetworks.repo.model.dbo.entity.UsersEntityPermissionsDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntityAuthorizationManagerImpl implements EntityAuthorizationManager {

	private AccessRestrictionStatusDao accessRestrictionStatusDao;
	private UsersEntityPermissionsDao usersEntityPermissionsDao;

	@Autowired
	public EntityAuthorizationManagerImpl(AccessRestrictionStatusDao accessRestrictionStatusDao,
			UsersEntityPermissionsDao usersEntityPermissionsDao) {
		super();
		this.accessRestrictionStatusDao = accessRestrictionStatusDao;
		this.usersEntityPermissionsDao = usersEntityPermissionsDao;
	}

	@Override
	public AuthorizationStatus hasAccess(UserInfo userInfo, String entityId, ACCESS_TYPE accessType)
			throws NotFoundException, DatastoreException {
		ValidateArgument.required(userInfo, "UserInfo");
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(accessType, "accessType");
		long enityIdLong = KeyFactory.stringToKey(entityId);
		return batchHasAccess(userInfo, Arrays.asList(enityIdLong), accessType).stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("Unexpected empty results")).getAuthroizationStatus();
	}

	@Override
	public UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo, String entityId)
			throws NotFoundException, DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<UsersEntityAccessInfo> batchHasAccess(UserInfo userInfo, List<Long> entityIds, ACCESS_TYPE accessType) {
		ValidateArgument.required(userInfo, "UserInfo");
		ValidateArgument.required(entityIds, "entityId");
		ValidateArgument.required(accessType, "accessType");

		EntityStateProvider stateProvider = new LazyEntityStateProvider(accessRestrictionStatusDao,
				usersEntityPermissionsDao, userInfo, entityIds);

		return determineAccess(new UserInfoState(userInfo), stateProvider, accessType).collect(Collectors.toList());
	}

	/**
	 * Determine the access information for a batch of entities for a given user.
	 * 
	 * @param userInfo
	 * @param permissionState
	 * @param accessType
	 * @return
	 */
	public Stream<UsersEntityAccessInfo> determineAccess(UserInfoState userInfo, EntityStateProvider stateProvider,
			ACCESS_TYPE accessType) {
		List<UserEntityPermissionsState> permissionState = stateProvider.getUserEntityPermissionsState();
		switch (accessType) {
		case CREATE:
			EntityType newEntityType = null;
			return permissionState.stream().map(t -> determineCreateAccess(userInfo, t, newEntityType));
		case READ:
			return permissionState.stream().map(t -> determineReadAccess(userInfo, t));
		case UPDATE:
			return permissionState.stream().map(t -> determineUpdateAccess(userInfo, t));
		case DELETE:
			return permissionState.stream().map(t -> determineDeleteAccess(userInfo, t));
		case CHANGE_PERMISSIONS:
			return permissionState.stream().map(t -> determineChangePermissionAccess(userInfo, t));
		case DOWNLOAD:
			return permissionState.stream().map(
					t -> determineDownloadAccess(userInfo, t, stateProvider.getUserRestrictionStatus(t.getEntityId())));
		case UPLOAD:
			return permissionState.stream().map(t -> determineUpdateAccess(userInfo, t));
		case CHANGE_SETTINGS:
			return permissionState.stream().map(t -> determineChangeSettingsAccess(userInfo, t));
		case MODERATE:
			return permissionState.stream().map(t -> determineModerateAccess(userInfo, t));
		default:
			throw new IllegalArgumentException("Unknown access type: " + accessType);

		}
	}

	/**
	 * Determine if the user can download a single entity.
	 * 
	 * @param userState
	 * @param permissionState
	 * @param restrictionStatus
	 * @return
	 */
	UsersEntityAccessInfo determineDownloadAccess(UserInfoState userState, UserEntityPermissionsState permissionState,
			UsersRestrictionStatus restrictionStatus) {
		// @formatter:off
		return AccessDecider.makeAccessDecision(new AccessContext().withUser(userState).withPermissionState(permissionState)
				.withRestrictionStatus(restrictionStatus),
			DENY_IF_DOES_NOT_EXIST,
			GRANT_IF_ADMIN,
			DENY_IF_IN_TRASH,
			DENY_IF_HAS_UNMET_ACCESS_RESTRICTIONS,
			GRANT_IF_OPEN_DATA_WITH_READ,
			DENY_IF_ANONYMOUS,
			DENY_IF_HAS_NOT_ACCEPTED_TERMS_OF_USE,
			GRANT_IF_HAS_DOWNLOAD,
			DENY
		);
		// @formatter:on
	}

	UsersEntityAccessInfo determineUpdateAccess(UserInfoState userInfo, UserEntityPermissionsState permissionState) {
		return null;
	}

	UsersEntityAccessInfo determineReadAccess(UserInfoState userInfo, UserEntityPermissionsState permissionState) {
		return null;
	}

	UsersEntityAccessInfo determineCreateAccess(UserInfoState userInfo, UserEntityPermissionsState permissionState,
			EntityType newEntityType) {
		return null;
	}

	UsersEntityAccessInfo determineModerateAccess(UserInfoState userInfo, UserEntityPermissionsState permissionState) {
		return null;
	}

	UsersEntityAccessInfo determineChangeSettingsAccess(UserInfoState userInfo,
			UserEntityPermissionsState permissionState) {
		return null;
	}

	UsersEntityAccessInfo determineChangePermissionAccess(UserInfoState userInfo,
			UserEntityPermissionsState permissionState) {
		return null;
	}

	UsersEntityAccessInfo determineDeleteAccess(UserInfoState UserInfoState,
			UserEntityPermissionsState permissionState) {
		return null;
	}

}
