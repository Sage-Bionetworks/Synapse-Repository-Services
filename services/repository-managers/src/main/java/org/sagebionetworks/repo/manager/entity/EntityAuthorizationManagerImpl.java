package org.sagebionetworks.repo.manager.entity;

import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_ANONYMOUS;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_DOES_NOT_EXIST;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_HAS_NOT_ACCEPTED_TERMS_OF_USE;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_HAS_UNMET_ACCESS_RESTRICTIONS;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_IN_TRASH;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_NOT_CERTIFIED;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_IF_ADMIN;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_IF_OPEN_DATA_WITH_READ;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_OR_DENY_IF_HAS_CHANGE_PERMISSION;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_OR_DENY_IF_HAS_CHANGE_SETTINGS;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_OR_DENY_IF_HAS_DELETE;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_OR_DENY_IF_HAS_DOWNLOAD;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_OR_DENY_IF_HAS_MODERATE;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.makeAccessDecission;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.dataaccess.RestrictionInformationManager;
import org.sagebionetworks.repo.manager.entity.decider.AccessContext;
import org.sagebionetworks.repo.manager.entity.decider.UsersEntityAccessInfo;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
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

	@Autowired
	private RestrictionInformationManager restrictionInformationManager;
	@Autowired
	private UsersEntityPermissionsDao usersEntityPermissionsDao;

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
		List<UserEntityPermissionsState> permissionState = usersEntityPermissionsDao
				.getEntityPermissions(userInfo.getGroups(), entityIds);
		return determineAccess(userInfo, permissionState, accessType);
	}

	/**
	 * Determine the access information for a batch of entities for a given user.
	 * 
	 * @param userInfo
	 * @param permissionState
	 * @param accessType
	 * @return
	 */
	public List<UsersEntityAccessInfo> determineAccess(UserInfo userInfo,
			List<UserEntityPermissionsState> permissionState, ACCESS_TYPE accessType) {
		switch (accessType) {
		case CREATE:
			EntityType newEntityType = null;
			return permissionState.stream().map(t -> determineCreateAccess(userInfo, t, newEntityType))
					.collect(Collectors.toList());
		case READ:
			return permissionState.stream().map(t -> determineReadAccess(userInfo, t)).collect(Collectors.toList());
		case UPDATE:
			return permissionState.stream().map(t -> determineUpdateAccess(userInfo, t)).collect(Collectors.toList());
		case DELETE:
			return permissionState.stream().map(t -> determineDeleteAccess(userInfo, t)).collect(Collectors.toList());
		case CHANGE_PERMISSIONS:
			return permissionState.stream().map(t -> determineChangePermissionAccess(userInfo, t))
					.collect(Collectors.toList());
		case DOWNLOAD:
			return determineDownloadAccess(userInfo, permissionState);
		case UPLOAD:
			return permissionState.stream().map(t -> determineUpdateAccess(userInfo, t)).collect(Collectors.toList());
		case CHANGE_SETTINGS:
			return permissionState.stream().map(t -> determineChangeSettingsAccess(userInfo, t))
					.collect(Collectors.toList());
		case MODERATE:
			return permissionState.stream().map(t -> determineModerateAccess(userInfo, t)).collect(Collectors.toList());
		default:
			throw new IllegalArgumentException("Unknown access type: " + accessType);

		}
	}

	UsersEntityAccessInfo determineModerateAccess(UserInfo userInfo, UserEntityPermissionsState permissionState) {
		// @formatter:off
		return makeAccessDecission(new AccessContext().withUser(userInfo).withPermissionState(permissionState),
			DENY_IF_DOES_NOT_EXIST,
			GRANT_IF_ADMIN,
			DENY_IF_IN_TRASH,
			DENY_IF_ANONYMOUS,
			GRANT_OR_DENY_IF_HAS_MODERATE
		);
		// @formatter:on
	}

	UsersEntityAccessInfo determineChangeSettingsAccess(UserInfo userInfo, UserEntityPermissionsState permissionState) {
		// @formatter:off
		return makeAccessDecission(new AccessContext().withUser(userInfo).withPermissionState(permissionState),
			DENY_IF_DOES_NOT_EXIST,
			GRANT_IF_ADMIN,
			DENY_IF_IN_TRASH,
			DENY_IF_ANONYMOUS,
			DENY_IF_NOT_CERTIFIED,
			GRANT_OR_DENY_IF_HAS_CHANGE_SETTINGS
		);
		// @formatter:on
	}

	UsersEntityAccessInfo determineChangePermissionAccess(UserInfo userInfo,
			UserEntityPermissionsState permissionState) {
		// @formatter:off
		return makeAccessDecission(new AccessContext().withUser(userInfo).withPermissionState(permissionState),
			DENY_IF_DOES_NOT_EXIST,
			GRANT_IF_ADMIN,
			DENY_IF_IN_TRASH,
			DENY_IF_ANONYMOUS,
			GRANT_OR_DENY_IF_HAS_CHANGE_PERMISSION
		);
		// @formatter:on
	}

	UsersEntityAccessInfo determineDeleteAccess(UserInfo userInfo, UserEntityPermissionsState permissionState) {
		// @formatter:off
		return makeAccessDecission(new AccessContext().withUser(userInfo).withPermissionState(permissionState),
			DENY_IF_DOES_NOT_EXIST,
			GRANT_IF_ADMIN,
			DENY_IF_ANONYMOUS,
			GRANT_OR_DENY_IF_HAS_DELETE
		);
		// @formatter:on
	}

	/**
	 * For each entity in the list make a can download decision.
	 * 
	 * @param userInfo
	 * @param permissionState
	 * @return
	 */
	List<UsersEntityAccessInfo> determineDownloadAccess(UserInfo userInfo,
			List<UserEntityPermissionsState> permissionState) {
		// For each entity we need to fetch access restriction information.
		List<Long> entityIds = permissionState.stream().map(t -> t.getEntityId()).collect(Collectors.toList());
		List<UsersRestrictionStatus> restrictionStatus = restrictionInformationManager
				.getEntityRestrictionInformation(userInfo, entityIds);
		Map<Long, UsersRestrictionStatus> idToSubjectStatus = new HashMap<Long, UsersRestrictionStatus>(
				restrictionStatus.size());
		for (UsersRestrictionStatus status : restrictionStatus) {
			idToSubjectStatus.put(status.getSubjectId(), status);
		}
		return permissionState.stream()
				.map(t -> determineDownloadAccess(userInfo, t, idToSubjectStatus.get(t.getEntityId())))
				.collect(Collectors.toList());
	}

	/**
	 * Determine if the user can download a single entity.
	 * 
	 * @param userInfo
	 * @param permissionState
	 * @param restrictionStatus
	 * @return
	 */
	UsersEntityAccessInfo determineDownloadAccess(UserInfo userInfo, UserEntityPermissionsState permissionState,
			UsersRestrictionStatus restrictionStatus) {
		// @formatter:off
		return makeAccessDecission(new AccessContext().withUser(userInfo).withPermissionState(permissionState)
				.withRestrictionStatus(restrictionStatus),
			DENY_IF_DOES_NOT_EXIST,
			GRANT_IF_ADMIN,
			DENY_IF_IN_TRASH,
			DENY_IF_HAS_UNMET_ACCESS_RESTRICTIONS,
			GRANT_IF_OPEN_DATA_WITH_READ,
			DENY_IF_ANONYMOUS,
			DENY_IF_HAS_NOT_ACCEPTED_TERMS_OF_USE,
			GRANT_OR_DENY_IF_HAS_DOWNLOAD
		);
		// @formatter:on
	}

	UsersEntityAccessInfo determineUpdateAccess(UserInfo userInfo, UserEntityPermissionsState permissionState) {
		return null;
	}

	UsersEntityAccessInfo determineReadAccess(UserInfo userInfo, UserEntityPermissionsState permissionState) {
		return null;
	}

	UsersEntityAccessInfo determineCreateAccess(UserInfo userInfo, UserEntityPermissionsState permissionState,
			EntityType newEntityType) {
		return null;
	}

}
