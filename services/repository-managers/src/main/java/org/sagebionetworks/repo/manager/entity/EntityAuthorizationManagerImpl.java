package org.sagebionetworks.repo.manager.entity;

import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_ANONYMOUS;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_CREATE_TYPE_IS_NOT_PROJECT_AND_NOT_CERTIFIED;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_DOES_NOT_EXIST;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_HAS_NOT_ACCEPTED_TERMS_OF_USE;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_HAS_UNMET_ACCESS_RESTRICTIONS;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_IN_TRASH;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_NOT_CERTIFIED;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_NOT_PROJECT_AND_NOT_CERTIFIED;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_PARENT_IS_ROOT_OR_NULL;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_IF_ADMIN;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_IF_HAS_CHANGE_PERMISSION;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_IF_HAS_CHANGE_SETTINGS;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_IF_HAS_CREATE;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_IF_HAS_DELETE;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_IF_HAS_DOWNLOAD;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_IF_HAS_MODERATE;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_IF_HAS_READ;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_IF_HAS_UPDATE;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_IF_OPEN_DATA_WITH_READ;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.GRANT_IF_USER_IS_CREATOR;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_PERMISSIONS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_SETTINGS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.CREATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.MODERATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE;

import java.util.List;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.entity.decider.AccessContext;
import org.sagebionetworks.repo.manager.entity.decider.AccessDecider;
import org.sagebionetworks.repo.manager.entity.decider.UsersEntityAccessInfo;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationUtils;
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
	public AuthorizationStatus hasAccess(UserInfo userInfo, String entityId, ACCESS_TYPE... accessTypes)
			throws NotFoundException, DatastoreException {
		ValidateArgument.required(userInfo, "UserInfo");
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(accessTypes, "accessTypes");
		if (accessTypes.length < 1) {
			throw new IllegalArgumentException("At least one ACCESS_TYPE must be provided");
		}
		EntityStateProvider stateProvider = new LazyEntityStateProvider(accessRestrictionStatusDao,
				usersEntityPermissionsDao, userInfo, KeyFactory.stringToKeySingletonList(entityId));
		AuthorizationStatus lastResult = null;
		for (ACCESS_TYPE accessType : accessTypes) {
			lastResult = determineAccess(userInfo, KeyFactory.stringToKey(entityId), stateProvider, accessType)
					.getAuthroizationStatus();
			if (!lastResult.isAuthorized()) {
				return lastResult;
			}
		}
		return lastResult;
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
		return determineCreateAccess(userInfo, state, entityCreateType).getAuthroizationStatus();
	}

	@Override
	public AuthorizationStatus canCreateWiki(String entityId, UserInfo userInfo) {
		ValidateArgument.required(userInfo, "UserInfo");
		ValidateArgument.required(entityId, "entityId");
		UserEntityPermissionsState state = usersEntityPermissionsDao
				.getEntityPermissions(userInfo.getGroups(), KeyFactory.stringToKeySingletonList(entityId)).stream()
				.findFirst().get();
		return determineCreateAccess(userInfo, state, state.getEntityType()).getAuthroizationStatus();
	}

	@Override
	public UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo, String entityId)
			throws NotFoundException, DatastoreException {
		EntityStateProvider stateProvider = new LazyEntityStateProvider(accessRestrictionStatusDao,
				usersEntityPermissionsDao, userInfo, KeyFactory.stringToKeySingletonList(entityId));
		Long entityIdLong = KeyFactory.stringToKey(entityId);
		UserEntityPermissionsState permissionsState = stateProvider.getPermissionsState(entityIdLong);
		UserEntityPermissions permissions = new UserEntityPermissions();
		permissions.setCanAddChild(determineAccess(entityIdLong, stateProvider, CREATE, userInfo).isAuthorized());
		permissions.setCanCertifiedUserAddChild(permissionsState.hasCreate());
		permissions.setCanChangePermissions(
				determineAccess(entityIdLong, stateProvider, CHANGE_PERMISSIONS, userInfo).isAuthorized());
		permissions.setCanChangeSettings(
				determineAccess(entityIdLong, stateProvider, CHANGE_SETTINGS, userInfo).isAuthorized());
		permissions.setCanDelete(determineAccess(entityIdLong, stateProvider, DELETE, userInfo).isAuthorized());
		permissions.setCanEdit(determineAccess(entityIdLong, stateProvider, UPDATE, userInfo).isAuthorized());
		permissions.setCanCertifiedUserEdit(permissionsState.hasUpdate());
		permissions.setCanView(determineAccess(entityIdLong, stateProvider, READ, userInfo).isAuthorized());
		permissions.setCanDownload(
				determineAccess(entityIdLong, stateProvider, ACCESS_TYPE.DOWNLOAD, userInfo).isAuthorized());
		permissions.setCanUpload(userInfo.acceptsTermsOfUse());
		permissions.setCanModerate(determineAccess(entityIdLong, stateProvider, MODERATE, userInfo).isAuthorized());
		permissions.setIsCertificationRequired(true);

		permissions.setOwnerPrincipalId(permissionsState.getEntityCreatedBy());

		permissions.setIsCertifiedUser(AuthorizationUtils.isCertifiedUser(userInfo));
		permissions.setCanPublicRead(permissionsState.hasPublicRead());

		permissions.setCanEnableInheritance(
				determineCanDeleteACL(userInfo, stateProvider.getPermissionsState(entityIdLong))
						.getAuthroizationStatus().isAuthorized());
		return permissions;

	}

	/**
	 * Determine if the user has the given access to the given entity.
	 * 
	 * @param provider
	 * @param accessType
	 * @param user
	 * @return
	 */
	private AuthorizationStatus determineAccess(Long entityId, EntityStateProvider provider, ACCESS_TYPE accessType,
			UserInfo user) {
		return determineAccess(user, entityId, provider, accessType).getAuthroizationStatus();
	}

	@Override
	public List<UsersEntityAccessInfo> batchHasAccess(UserInfo userInfo, List<Long> entityIds, ACCESS_TYPE accessType) {
		ValidateArgument.required(userInfo, "UserInfo");
		ValidateArgument.required(entityIds, "entityId");
		ValidateArgument.required(accessType, "accessType");

		EntityStateProvider stateProvider = new LazyEntityStateProvider(accessRestrictionStatusDao,
				usersEntityPermissionsDao, userInfo, entityIds);
		return entityIds.stream().map(id -> determineAccess(userInfo, id, stateProvider, accessType))
				.collect(Collectors.toList());
	}

	public UsersEntityAccessInfo determineAccess(UserInfo userInfo, Long id, EntityStateProvider provider,
			ACCESS_TYPE accessType) {
		switch (accessType) {
		case CREATE:
			EntityType newEntityType = null;
			return determineCreateAccess(userInfo, provider.getPermissionsState(id), newEntityType);
		case READ:
			return determineReadAccess(userInfo, provider.getPermissionsState(id));
		case UPDATE:
			return determineUpdateAccess(userInfo, provider.getPermissionsState(id));
		case DELETE:
			return determineDeleteAccess(userInfo, provider.getPermissionsState(id));
		case CHANGE_PERMISSIONS:
			return determineChangePermissionAccess(userInfo, provider.getPermissionsState(id));
		case DOWNLOAD:
			return determineDownloadAccess(userInfo, provider.getPermissionsState(id),
					provider.getRestrictionStatus(id));
		case CHANGE_SETTINGS:
			return determineChangeSettingsAccess(userInfo, provider.getPermissionsState(id));
		case MODERATE:
			return determineModerateAccess(userInfo, provider.getPermissionsState(id));
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
	UsersEntityAccessInfo determineDownloadAccess(UserInfo userState, UserEntityPermissionsState permissionsState,
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

	UsersEntityAccessInfo determineUpdateAccess(UserInfo userInfo, UserEntityPermissionsState permissionsState) {
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

	UsersEntityAccessInfo determineCreateAccess(UserInfo userInfo, UserEntityPermissionsState parentPermissionsState,
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

	UsersEntityAccessInfo determineReadAccess(UserInfo userInfo, UserEntityPermissionsState permissionsState) {
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

	UsersEntityAccessInfo determineDeleteAccess(UserInfo userInfo, UserEntityPermissionsState permissionsState) {
		// @formatter:off
		return AccessDecider.makeAccessDecision(new AccessContext().withUser(userInfo).withPermissionsState(permissionsState)
				.withAccessType(ACCESS_TYPE.DELETE),
			DENY_IF_DOES_NOT_EXIST,
			GRANT_IF_ADMIN,
			DENY_IF_IN_TRASH,
			DENY_IF_ANONYMOUS,
			GRANT_IF_HAS_DELETE,
			DENY
		);
		// @formatter:on
	}

	UsersEntityAccessInfo determineModerateAccess(UserInfo userInfo, UserEntityPermissionsState permissionsState) {
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

	UsersEntityAccessInfo determineChangeSettingsAccess(UserInfo userInfo,
			UserEntityPermissionsState permissionsState) {
		// @formatter:off
		return AccessDecider.makeAccessDecision(new AccessContext().withUser(userInfo).withPermissionsState(permissionsState)
				.withAccessType(ACCESS_TYPE.CHANGE_SETTINGS),
			DENY_IF_DOES_NOT_EXIST,
			DENY_IF_IN_TRASH,
			GRANT_IF_ADMIN,
			DENY_IF_ANONYMOUS,
			DENY_IF_NOT_CERTIFIED,
			GRANT_IF_USER_IS_CREATOR,
			GRANT_IF_HAS_CHANGE_SETTINGS,
			DENY
		);
		// @formatter:on
	}

	UsersEntityAccessInfo determineChangePermissionAccess(UserInfo userInfo,
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

	@Override
	public AuthorizationStatus canDeleteACL(UserInfo userInfo, String entityId) {
		EntityStateProvider stateProvider = new LazyEntityStateProvider(accessRestrictionStatusDao,
				usersEntityPermissionsDao, userInfo, KeyFactory.stringToKeySingletonList(entityId));
		return determineCanDeleteACL(userInfo, stateProvider.getPermissionsState(KeyFactory.stringToKey(entityId)))
				.getAuthroizationStatus();
	}

	UsersEntityAccessInfo determineCanDeleteACL(UserInfo userInfo, UserEntityPermissionsState permissionsState) {
		// @formatter:off
		return AccessDecider.makeAccessDecision(new AccessContext().withUser(userInfo).withPermissionsState(permissionsState)
				.withAccessType(ACCESS_TYPE.CHANGE_PERMISSIONS),
			DENY_IF_DOES_NOT_EXIST,
			DENY_IF_IN_TRASH,
			DENY_IF_PARENT_IS_ROOT_OR_NULL,
			GRANT_IF_ADMIN,
			DENY_IF_ANONYMOUS,
			GRANT_IF_HAS_CHANGE_PERMISSION,
			DENY
		);
		// @formatter:on
	}



}
