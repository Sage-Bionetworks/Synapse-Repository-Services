package org.sagebionetworks.repo.manager.entity.decider;

import static org.sagebionetworks.repo.model.AuthorizationConstants.*;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_CERTIFIED_USER_CONTENT;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_ENTITY_IN_TRASH_TEMPLATE;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_THERE_ARE_UNMET_ACCESS_REQUIREMENTS;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_THE_RESOURCE_YOU_ARE_ATTEMPTING_TO_ACCESS_CANNOT_BE_FOUND;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_YOU_HAVE_NOT_YET_AGREED_TO_THE_SYNAPSE_TERMS_OF_USE;

import java.util.Optional;

import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.NodeConstants.BOOTSTRAP_NODES;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.web.NotFoundException;

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
		if (BOOTSTRAP_NODES.TRASH.getId().equals(c.getPermissionState().getBenefactorId())) {
			return Optional.of(
					new UsersEntityAccessInfo(c, AuthorizationStatus.accessDenied(new EntityInTrashCanException(String
							.format(ERR_MSG_ENTITY_IN_TRASH_TEMPLATE, c.getPermissionState().getEntityIdAsString())))));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Denies access if the entity does not exist.
	 */
	DENY_IF_DOES_NOT_EXIST((c) -> {
		if (!c.getPermissionState().doesEntityExist()) {
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
		if (DataType.OPEN_DATA.equals(c.getPermissionState().getDataType()) && c.getPermissionState().hasRead()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Grants if the user has the DOWNLOAD permission on
	 * the entity.
	 */
	GRANT_IF_HAS_DOWNLOAD((c) -> {
		if (c.getPermissionState().hasDownload()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Grants if the user has the MODERATE permission on
	 * the entity.
	 */
	GRANT_IF_HAS_MODERATE((c) -> {
		if (c.getPermissionState().hasModerate()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.empty();
		}
	}),

	/**
	 * Grants if the user has the CHANGE_SETTINGS
	 * permission on the entity.
	 */
	GRANT_IF_HAS_CHANGE_SETTINGS((c) -> {
		if (c.getPermissionState().hasChangeSettings()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Grants if the user has the CHANGE_SETTINGS
	 * permission on the entity.
	 */
	GRANT_IF_HAS_CHANGE_PERMISSION((c) -> {
		if (c.getPermissionState().hasChangePermissions()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Grants if the user has the DELETE permission on the
	 * entity.
	 */
	GRANT_IF_HAS_DELETE((c) -> {
		if (c.getPermissionState().hasDelete()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Deny if the user is anonymous.
	 */
	DENY_IF_ANONYMOUS((c) -> {
		if (c.getUser().isUserAnonymous()) {
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
		if (!c.getUser().isCertifiedUser()) {
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
	 * Unconditional deny.
	 */
	DENY((c) -> {
		return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.accessDenied(ERR_MSG_ACCESS_DENIED)));
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
