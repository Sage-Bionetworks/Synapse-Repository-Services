package org.sagebionetworks.repo.manager.entity;

import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_ANONYMOUS;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_CREATE_TYPE_IS_NOT_PROJECT_AND_NOT_CERTIFIED;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_DOES_NOT_EXIST;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_HAS_NOT_ACCEPTED_TERMS_OF_USE;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_HAS_UNMET_ACCESS_RESTRICTIONS;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_IN_TRASH;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_NOT_PROJECT_AND_NOT_CERTIFIED;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_IF_ADMIN;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_IF_HAS_CREATE;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_IF_HAS_DOWNLOAD;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_IF_HAS_UPDATE;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.*;

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
		return batchHasAccess(userInfo, KeyFactory.stringToKeySingletonList(entityId), accessType).stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("Unexpected empty results")).getAuthroizationStatus();
	}
	
	@Override
	public AuthorizationStatus canCreate(String parentId, EntityType entityCreateType, UserInfo userInfo)
			throws DatastoreException, NotFoundException {
		ValidateArgument.required(userInfo, "UserInfo");
		ValidateArgument.required(parentId, "parentId");
		ValidateArgument.required(entityCreateType, "entityCreateType");
		UserEntityPermissionsState state = usersEntityPermissionsDao
				.getEntityPermissions(userInfo.getGroups(), KeyFactory.stringToKeySingletonList(parentId)).stream()
				.findFirst().get();
		return determineCreateAccess(new UserInfoState(userInfo), state, entityCreateType).getAuthroizationStatus();
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
	 * @param permissionsState
	 * @param accessType
	 * @return
	 */
	public Stream<UsersEntityAccessInfo> determineAccess(UserInfoState userInfo, EntityStateProvider provider,
			ACCESS_TYPE accessType) {
		List<Long> ids = provider.getEntityIds();
		switch (accessType) {
		case CREATE:
			EntityType newEntityType = null;
			return ids.stream()
					.map(id -> determineCreateAccess(userInfo, provider.getPermissionsState(id), newEntityType));
		case READ:
			return ids.stream().map(id -> determineReadAccess(userInfo, provider.getPermissionsState(id)));
		case UPDATE:
			return ids.stream().map(id -> determineUpdateAccess(userInfo, provider.getPermissionsState(id)));
		case DELETE:
			return ids.stream().map(id -> determineDeleteAccess(userInfo, provider.getPermissionsState(id)));
		case CHANGE_PERMISSIONS:
			return ids.stream().map(id -> determineChangePermissionAccess(userInfo, provider.getPermissionsState(id)));
		case DOWNLOAD:
			return ids.stream().map(id -> determineDownloadAccess(userInfo, provider.getPermissionsState(id),
					provider.getRestrictionStatus(id)));
		case CHANGE_SETTINGS:
			return ids.stream().map(id -> determineChangeSettingsAccess(userInfo, provider.getPermissionsState(id)));
		case MODERATE:
			return ids.stream().map(id -> determineModerateAccess(userInfo, provider.getPermissionsState(id)));
		default:
			throw new IllegalArgumentException("Unknown access type: " + accessType);
		}
	}

	/**
	 * Determine if the user can download a single entity.
	 * 
	 * @param userState
	 * @param permissionsState
	 * @param restrictionStatus
	 * @return
	 */
	UsersEntityAccessInfo determineDownloadAccess(UserInfoState userState, UserEntityPermissionsState permissionsState,
			UsersRestrictionStatus restrictionStatus) {
		// @formatter:off
		return AccessDecider.makeAccessDecision(new AccessContext().withUser(userState).withPermissionsState(permissionsState)
				.withRestrictionStatus(restrictionStatus).withAccessType(ACCESS_TYPE.DOWNLOAD),
			DENY_IF_DOES_NOT_EXIST,
			DENY_IF_IN_TRASH,
			GRANT_IF_ADMIN,
			DENY_IF_HAS_UNMET_ACCESS_RESTRICTIONS,
			GRANT_IF_OPEN_DATA_WITH_READ,
			DENY_IF_ANONYMOUS,
			DENY_IF_HAS_NOT_ACCEPTED_TERMS_OF_USE,
			GRANT_IF_HAS_DOWNLOAD,
			DENY
		);
		// @formatter:on
	}

	UsersEntityAccessInfo determineUpdateAccess(UserInfoState userInfo, UserEntityPermissionsState permissionsState) {
		// @formatter:off
		return AccessDecider.makeAccessDecision(new AccessContext().withUser(userInfo).withPermissionsState(permissionsState)
				.withAccessType(ACCESS_TYPE.UPDATE),
			DENY_IF_DOES_NOT_EXIST,
			DENY_IF_IN_TRASH,
			GRANT_IF_ADMIN,
			DENY_IF_ANONYMOUS,
			DENY_IF_NOT_PROJECT_AND_NOT_CERTIFIED,
			DENY_IF_HAS_NOT_ACCEPTED_TERMS_OF_USE,
			GRANT_IF_HAS_UPDATE,
			DENY
		);
		// @formatter:on
	}
	
	UsersEntityAccessInfo determineCreateAccess(UserInfoState userInfo, UserEntityPermissionsState parentPermissionsState,
			EntityType entityCreateType) {
		// @formatter:off
		return AccessDecider.makeAccessDecision(new AccessContext().withUser(userInfo).withPermissionsState(parentPermissionsState)
				.withAccessType(ACCESS_TYPE.CREATE).withEntityCreateType(entityCreateType),
			DENY_IF_DOES_NOT_EXIST,
			DENY_IF_IN_TRASH,
			GRANT_IF_ADMIN,
			DENY_IF_ANONYMOUS,
			DENY_IF_CREATE_TYPE_IS_NOT_PROJECT_AND_NOT_CERTIFIED,
			DENY_IF_HAS_NOT_ACCEPTED_TERMS_OF_USE,
			GRANT_IF_HAS_CREATE,
			DENY
		);
		// @formatter:on
	}

	UsersEntityAccessInfo determineReadAccess(UserInfoState userInfo, UserEntityPermissionsState permissionsState) {
		// @formatter:off
		return AccessDecider.makeAccessDecision(new AccessContext().withUser(userInfo).withPermissionsState(permissionsState)
				.withAccessType(ACCESS_TYPE.READ),
			DENY_IF_DOES_NOT_EXIST,
			DENY_IF_IN_TRASH,
			GRANT_IF_ADMIN,
			GRANT_IF_HAS_READ,
			DENY
		);
		// @formatter:on
	}
	
	UsersEntityAccessInfo determineDeleteAccess(UserInfoState userInfo,
			UserEntityPermissionsState permissionsState) {
		// @formatter:off
		return AccessDecider.makeAccessDecision(new AccessContext().withUser(userInfo).withPermissionsState(permissionsState)
				.withAccessType(ACCESS_TYPE.DELETE),
			DENY_IF_DOES_NOT_EXIST,
			DENY_IF_IN_TRASH,
			GRANT_IF_ADMIN,
			DENY_IF_ANONYMOUS,
			GRANT_IF_HAS_DELETE,
			DENY
		);
		// @formatter:on
	}

	UsersEntityAccessInfo determineModerateAccess(UserInfoState userInfo, UserEntityPermissionsState permissionsState) {
		// @formatter:off
		return AccessDecider.makeAccessDecision(new AccessContext().withUser(userInfo).withPermissionsState(permissionsState)
				.withAccessType(ACCESS_TYPE.MODERATE),
			DENY_IF_DOES_NOT_EXIST,
			DENY_IF_IN_TRASH,
			GRANT_IF_ADMIN,
			DENY_IF_ANONYMOUS,
			GRANT_IF_HAS_MODERATE,
			DENY
		);
		// @formatter:on
	}

	UsersEntityAccessInfo determineChangeSettingsAccess(UserInfoState userInfo,
			UserEntityPermissionsState permissionsState) {
		// @formatter:off
		return AccessDecider.makeAccessDecision(new AccessContext().withUser(userInfo).withPermissionsState(permissionsState)
				.withAccessType(ACCESS_TYPE.CHANGE_SETTINGS),
			DENY_IF_DOES_NOT_EXIST,
			DENY_IF_IN_TRASH,
			GRANT_IF_ADMIN,
			DENY_IF_ANONYMOUS,
			GRANT_IF_HAS_CHANGE_SETTINGS,
			DENY
		);
		// @formatter:on
	}

	UsersEntityAccessInfo determineChangePermissionAccess(UserInfoState userInfo,
			UserEntityPermissionsState permissionsState) {
		// @formatter:off
		return AccessDecider.makeAccessDecision(new AccessContext().withUser(userInfo).withPermissionsState(permissionsState)
				.withAccessType(ACCESS_TYPE.CHANGE_PERMISSIONS),
			DENY_IF_DOES_NOT_EXIST,
			DENY_IF_IN_TRASH,
			GRANT_IF_ADMIN,
			DENY_IF_ANONYMOUS,
			GRANT_IF_HAS_CHANGE_PERMISSION,
			DENY
		);
		// @formatter:on
	}

}
