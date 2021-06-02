package org.sagebionetworks.repo.manager.entity.decider;

import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_ACCESS_DENIED;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_CANNOT_REMOVE_ACL_OF_PROJECT;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_CERTIFIED_USER_CONTENT;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_ENTITY_IN_TRASH_TEMPLATE;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_THERE_ARE_UNMET_ACCESS_REQUIREMENTS;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_THE_RESOURCE_YOU_ARE_ATTEMPTING_TO_ACCESS_CANNOT_BE_FOUND;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_YOU_HAVE_NOT_YET_AGREED_TO_THE_SYNAPSE_TERMS_OF_USE;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_YOU_LACK_ACCESS_TO_REQUESTED_ENTITY_TEMPLATE;
import static org.sagebionetworks.repo.model.NodeConstants.BOOTSTRAP_NODES.ROOT;

import java.util.Optional;

import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeConstants.BOOTSTRAP_NODES;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.web.NotFoundException;;

/**
 * The set of functions that can be used in making entity access decisions.
 *
 */
public enum EntityDeciderFunctions implements AccessDecider {

	/**
	 * Grants access if the user is an administrator.
	 */
	GRANT_IF_ADMIN((c) -> {
		if (c.getUser().isAdmin()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Denies access if the the entity is in the trash.
	 */
	DENY_IF_IN_TRASH((c) -> {
		if (BOOTSTRAP_NODES.TRASH.getId().equals(c.getPermissionsState().getBenefactorId())) {
			return Optional.of(new UsersEntityAccessInfo(c,
					AuthorizationStatus
							.accessDenied(new EntityInTrashCanException(String.format(ERR_MSG_ENTITY_IN_TRASH_TEMPLATE,
									c.getPermissionsState().getEntityIdAsString())))));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Denies access if the entity does not exist.
	 */
	DENY_IF_DOES_NOT_EXIST((c) -> {
		if (!c.getPermissionsState().doesEntityExist()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.accessDenied(
					new NotFoundException(ERR_MSG_THE_RESOURCE_YOU_ARE_ATTEMPTING_TO_ACCESS_CANNOT_BE_FOUND))));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Denies access if the user has unmet access restrictions on this entity.
	 */
	DENY_IF_HAS_UNMET_ACCESS_RESTRICTIONS((c) -> {
		if (c.getRestrictionStatus().hasUnmet()) {
			return Optional.of(new UsersEntityAccessInfo(c,
					AuthorizationStatus.accessDenied(ERR_MSG_THERE_ARE_UNMET_ACCESS_REQUIREMENTS)));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Grants access if the data is "OPEN" and the user has the READ permission on
	 * the entity.
	 */
	GRANT_IF_OPEN_DATA_WITH_READ((c) -> {
		if (DataType.OPEN_DATA.equals(c.getPermissionsState().getDataType()) && c.getPermissionsState().hasRead()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Grants if the user has the DOWNLOAD permission on the entity.
	 */
	GRANT_IF_HAS_DOWNLOAD((c) -> {
		if (c.getPermissionsState().hasDownload()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Grants if the user has the MODERATE permission on the entity.
	 */
	GRANT_IF_HAS_MODERATE((c) -> {
		if (c.getPermissionsState().hasModerate()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Grants if the user has the CHANGE_SETTINGS permission on the entity.
	 */
	GRANT_IF_HAS_CHANGE_SETTINGS((c) -> {
		if (c.getPermissionsState().hasChangeSettings()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Grants if the user is the creator of the entity.
	 */
	GRANT_IF_USER_IS_CREATOR((c) -> {
		if (c.getUser().getId().equals(c.getPermissionsState().getEntityCreatedBy())) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Grants if the user has the CHANGE_SETTINGS permission on the entity.
	 */
	GRANT_IF_HAS_CHANGE_PERMISSION((c) -> {
		if (c.getPermissionsState().hasChangePermissions()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Grants if the user has the DELETE permission on the entity.
	 */
	GRANT_IF_HAS_DELETE((c) -> {
		if (c.getPermissionsState().hasDelete()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Grants if the user has the UPDATE permission on the entity.
	 */
	GRANT_IF_HAS_UPDATE((c) -> {
		if (c.getPermissionsState().hasUpdate()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Grants if the user has the CREATE permission on the entity.
	 */
	GRANT_IF_HAS_CREATE((c) -> {
		if (c.getPermissionsState().hasCreate()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Grants if the user has the READ permission on the entity.
	 */
	GRANT_IF_HAS_READ((c) -> {
		if (c.getPermissionsState().hasRead()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Deny if the user is anonymous.
	 */
	DENY_IF_ANONYMOUS((c) -> {
		if (AuthorizationUtils.isUserAnonymous(c.getUser())) {
			return Optional.of(new UsersEntityAccessInfo(c,
					AuthorizationStatus.accessDenied(ERR_MSG_ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION)));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Deny if the user is not certified.
	 */
	DENY_IF_NOT_CERTIFIED((c) -> {
		if (!AuthorizationUtils.isCertifiedUser(c.getUser())) {
			return Optional
					.of(new UsersEntityAccessInfo(c, AuthorizationStatus.accessDenied(ERR_MSG_CERTIFIED_USER_CONTENT)));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Deny if the user has not accepted the terms of use.
	 */
	DENY_IF_HAS_NOT_ACCEPTED_TERMS_OF_USE((c) -> {
		if (!c.getUser().acceptsTermsOfUse()) {
			return Optional.of(new UsersEntityAccessInfo(c,
					AuthorizationStatus.accessDenied(ERR_MSG_YOU_HAVE_NOT_YET_AGREED_TO_THE_SYNAPSE_TERMS_OF_USE)));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Deny if the entity to create type is not a project and the user is not
	 * certified.
	 */
	DENY_IF_CREATE_TYPE_IS_NOT_PROJECT_AND_NOT_CERTIFIED((c) -> {
		if (!EntityType.project.equals(c.getEntityCreateType()) && !AuthorizationUtils.isCertifiedUser(c.getUser())) {
			return Optional
					.of(new UsersEntityAccessInfo(c, AuthorizationStatus.accessDenied(ERR_MSG_CERTIFIED_USER_CONTENT)));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Deny if the EntityType is not a project and the user is not certified.
	 */
	DENY_IF_NOT_PROJECT_AND_NOT_CERTIFIED((c) -> {
		if (!EntityType.project.equals(c.getPermissionsState().getEntityType()) && !AuthorizationUtils.isCertifiedUser(c.getUser())) {
			return Optional
					.of(new UsersEntityAccessInfo(c, AuthorizationStatus.accessDenied(ERR_MSG_CERTIFIED_USER_CONTENT)));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Deny if parent is ROOT or null
	 */
	DENY_IF_PARENT_IS_ROOT_OR_NULL((c) -> {
		if (ROOT.getId().equals(c.getPermissionsState().getEntityParentId()) || c.getPermissionsState().getEntityParentId() == null) {
			return Optional.of(new UsersEntityAccessInfo(c,
					AuthorizationStatus.accessDenied(ERR_MSG_CANNOT_REMOVE_ACL_OF_PROJECT)));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Unconditional deny.
	 */
	DENY((c) -> {
		if (c.getAccessType() != null) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.accessDenied(
					String.format(ERR_MSG_YOU_LACK_ACCESS_TO_REQUESTED_ENTITY_TEMPLATE, c.getAccessType().name()))));
		} else {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.accessDenied(ERR_MSG_ACCESS_DENIED)));
		}
	});

	private AccessDecider decider;

	private EntityDeciderFunctions(AccessDecider decider) {
		this.decider = decider;
	}

	@Override
	public Optional<UsersEntityAccessInfo> determineAccess(AccessContext c) {
		return decider.determineAccess(c);
	}

}
