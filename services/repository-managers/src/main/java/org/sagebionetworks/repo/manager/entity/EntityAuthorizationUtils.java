package org.sagebionetworks.repo.manager.entity;

import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_ANONYMOUS;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_CREATE_TYPE_IS_NOT_PROJECT_AND_NOT_CERTIFIED;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_DOES_NOT_EXIST;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_IN_TRASH;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_NOT_CERTIFIED;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_NOT_EXEMPT_AND_HAS_UNMET_ACCESS_RESTRICTIONS;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_NOT_PROJECT_AND_NOT_CERTIFIED;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_PARENT_IS_ROOT_OR_NULL;
import static org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions.DENY_IF_TWO_FA_REQUIREMENT_NOT_MET;
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
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DOWNLOAD;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.MODERATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE;

import org.sagebionetworks.repo.manager.entity.decider.AccessContext;
import org.sagebionetworks.repo.manager.entity.decider.AccessDecider;
import org.sagebionetworks.repo.manager.entity.decider.UsersEntityAccessInfo;
import org.sagebionetworks.repo.manager.util.UserAccessRestrictionUtils;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;

public class EntityAuthorizationUtils {

    /**
     * Calculates the permissions that the calling user has for a given entity.
     * @param userInfo the UserInfo of the caller
     * @param entityId the ID of the entity
     * @param stateProvider the EntityStateProvider for the entity
     * @return a UserEntityPermissions object representing the permissions the user has for the entity
     * @throws NotFoundException
     * @throws DatastoreException
     */
    public static UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo, String entityId, EntityStateProvider stateProvider)
            throws NotFoundException, DatastoreException {
        ValidateArgument.required(userInfo, "userInfo");
        ValidateArgument.required(entityId, "entityId");
        ValidateArgument.required(stateProvider, "stateProvider");

        Long entityIdLong = KeyFactory.stringToKey(entityId);
        UserEntityPermissionsState permissionsState = stateProvider.getPermissionsState(entityIdLong);
        boolean canUpdate = determineAccess(userInfo, entityIdLong, stateProvider, UPDATE).getAuthorizationStatus().isAuthorized();
        boolean canChangePermissions = determineAccess(userInfo, entityIdLong, stateProvider, CHANGE_PERMISSIONS).getAuthorizationStatus().isAuthorized();
        UserEntityPermissions permissions = new UserEntityPermissions();
        permissions.setCanAddChild(determineAccess(userInfo, entityIdLong, stateProvider, CREATE).getAuthorizationStatus().isAuthorized());
        permissions.setCanCertifiedUserAddChild(permissionsState.hasCreate());
        permissions.setCanChangePermissions(canChangePermissions);
        permissions.setCanMove(canUpdate && canChangePermissions);
        permissions.setCanChangeSettings(
                determineAccess(userInfo, entityIdLong, stateProvider, CHANGE_SETTINGS).getAuthorizationStatus().isAuthorized());
        permissions.setCanDelete(determineAccess(userInfo, entityIdLong, stateProvider, DELETE).getAuthorizationStatus().isAuthorized());
        permissions.setCanEdit(canUpdate);
        permissions.setCanCertifiedUserEdit(permissionsState.hasUpdate());
        permissions.setCanView(determineAccess(userInfo, entityIdLong, stateProvider, READ).getAuthorizationStatus().isAuthorized());
        permissions.setCanDownload(
                determineAccess(userInfo, entityIdLong, stateProvider, DOWNLOAD).getAuthorizationStatus().isAuthorized());
        // We have a tos filter that won't allow to get here, the canUpload historically only checked if the user accepted the tos
        permissions.setCanUpload(!AuthorizationUtils.isUserAnonymous(userInfo.getId()));
        permissions.setCanModerate(determineAccess(userInfo, entityIdLong, stateProvider, MODERATE).getAuthorizationStatus().isAuthorized());
        permissions.setIsCertificationRequired(true);

        permissions.setOwnerPrincipalId(permissionsState.getEntityCreatedBy());

        permissions.setIsCertifiedUser(AuthorizationUtils.isCertifiedUser(userInfo));
        permissions.setCanPublicRead(permissionsState.hasPublicRead());

        permissions.setCanEnableInheritance(
                determineCanDeleteACL(userInfo, stateProvider.getPermissionsState(entityIdLong))
                        .getAuthorizationStatus().isAuthorized());
        permissions.setIsEntityOpenData(DataType.OPEN_DATA.equals(permissionsState.getDataType()));
        permissions.setIsDataContributor(UserAccessRestrictionUtils.isUserDataContributor(permissionsState));
        return permissions;

    }


    public static UsersEntityAccessInfo determineAccess(UserInfo userInfo, Long id, EntityStateProvider provider, ACCESS_TYPE accessType) {
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
    private static UsersEntityAccessInfo determineDownloadAccess(UserInfo userState, UserEntityPermissionsState permissionsState, UsersRestrictionStatus restrictionStatus) {

        // @formatter:off
        return AccessDecider.makeAccessDecision(new AccessContext().withUser(userState).withPermissionsState(permissionsState)
                .withRestrictionStatus(restrictionStatus).withAccessType(DOWNLOAD),
            DENY_IF_DOES_NOT_EXIST,
            DENY_IF_IN_TRASH,
            GRANT_IF_ADMIN,
            DENY_IF_NOT_EXEMPT_AND_HAS_UNMET_ACCESS_RESTRICTIONS,
            DENY_IF_TWO_FA_REQUIREMENT_NOT_MET,
            GRANT_IF_OPEN_DATA_WITH_READ,
            DENY_IF_ANONYMOUS,
            GRANT_IF_HAS_DOWNLOAD,
            DENY
        );
        // @formatter:on
    }

    private static UsersEntityAccessInfo determineUpdateAccess(UserInfo userInfo, UserEntityPermissionsState permissionsState) {
        // @formatter:off
        return AccessDecider.makeAccessDecision(new AccessContext().withUser(userInfo).withPermissionsState(permissionsState)
                .withAccessType(ACCESS_TYPE.UPDATE),
            DENY_IF_DOES_NOT_EXIST,
            DENY_IF_IN_TRASH,
            GRANT_IF_ADMIN,
            DENY_IF_ANONYMOUS,
            DENY_IF_NOT_PROJECT_AND_NOT_CERTIFIED,
            GRANT_IF_HAS_UPDATE,
            DENY
        );
        // @formatter:on
    }

    public static UsersEntityAccessInfo determineCreateAccess(UserInfo userInfo, UserEntityPermissionsState parentPermissionsState, EntityType entityCreateType) {
        // @formatter:off
        return AccessDecider.makeAccessDecision(new AccessContext().withUser(userInfo).withPermissionsState(parentPermissionsState)
                .withAccessType(ACCESS_TYPE.CREATE).withEntityCreateType(entityCreateType),
            DENY_IF_DOES_NOT_EXIST,
            DENY_IF_IN_TRASH,
            GRANT_IF_ADMIN,
            DENY_IF_ANONYMOUS,
            DENY_IF_CREATE_TYPE_IS_NOT_PROJECT_AND_NOT_CERTIFIED,
            GRANT_IF_HAS_CREATE,
            DENY
        );
        // @formatter:on
    }

    private static UsersEntityAccessInfo determineReadAccess(UserInfo userInfo, UserEntityPermissionsState permissionsState) {
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

    private static UsersEntityAccessInfo determineDeleteAccess(UserInfo userInfo, UserEntityPermissionsState permissionsState) {
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

    private static UsersEntityAccessInfo determineModerateAccess(UserInfo userInfo, UserEntityPermissionsState permissionsState) {
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

    private static UsersEntityAccessInfo determineChangeSettingsAccess(UserInfo userInfo, UserEntityPermissionsState permissionsState) {
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

    private static UsersEntityAccessInfo determineChangePermissionAccess(UserInfo userInfo, UserEntityPermissionsState permissionsState) {
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

    public static UsersEntityAccessInfo determineCanDeleteACL(UserInfo userInfo, UserEntityPermissionsState permissionsState) {
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
