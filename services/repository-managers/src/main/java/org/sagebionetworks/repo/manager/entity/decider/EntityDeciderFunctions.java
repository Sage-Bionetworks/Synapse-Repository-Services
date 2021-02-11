package org.sagebionetworks.repo.manager.entity.decider;

import static org.sagebionetworks.repo.model.AuthorizationConstants.ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ENTITY_IN_TRASH_TEMPLATE;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MESSAGE_CERTIFIED_USER_CONTENT;
import static org.sagebionetworks.repo.model.AuthorizationConstants.THERE_ARE_UNMET_ACCESS_REQUIREMENTS;
import static org.sagebionetworks.repo.model.AuthorizationConstants.THE_RESOURCE_YOU_ARE_ATTEMPTING_TO_ACCESS_CANNOT_BE_FOUND;
import static org.sagebionetworks.repo.model.AuthorizationConstants.YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE;
import static org.sagebionetworks.repo.model.AuthorizationConstants.YOU_HAVE_NOT_YET_AGREED_TO_THE_SYNAPSE_TERMS_OF_USE;

import java.util.Arrays;
import java.util.Optional;

import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationUtils;
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
			return Optional
					.of(new UsersEntityAccessInfo(c, AuthorizationStatus.accessDenied(new EntityInTrashCanException(
							String.format(ENTITY_IN_TRASH_TEMPLATE, c.getPermissionState().getEntityIdAsString())))));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Denies access if the entity does not exist.
	 */
	DENY_IF_DOES_NOT_EXIST((c) -> {
		if (!c.getPermissionState().doesEntityExist()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus
					.accessDenied(new NotFoundException(THE_RESOURCE_YOU_ARE_ATTEMPTING_TO_ACCESS_CANNOT_BE_FOUND))));
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
					AuthorizationStatus.accessDenied(THERE_ARE_UNMET_ACCESS_REQUIREMENTS)));
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
	 * Grants or denies access based on: if the user has the DOWNLOAD permission on
	 * the entity.
	 */
	GRANT_OR_DENY_IF_HAS_DOWNLOAD((c) -> {
		if (c.getPermissionState().hasDownload()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.of(new UsersEntityAccessInfo(c,
					AuthorizationStatus.accessDenied(String.format(YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE,
							ACCESS_TYPE.DOWNLOAD.name(), c.getPermissionState().getEntityIdAsString()))));
		}
	}),
	/**
	 * Grants or denies access based on: if the user has the MODERATE permission on
	 * the entity.
	 */
	GRANT_OR_DENY_IF_HAS_MODERATE((c) -> {
		if (c.getPermissionState().hasModerate()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.of(new UsersEntityAccessInfo(c,
					AuthorizationStatus.accessDenied(String.format(YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE,
							ACCESS_TYPE.MODERATE.name(), c.getPermissionState().getEntityIdAsString()))));
		}
	}),

	/**
	 * Grants or denies access based on: if the user has the CHANGE_SETTINGS
	 * permission on the entity.
	 */
	GRANT_OR_DENY_IF_HAS_CHANGE_SETTINGS((c) -> {
		if (c.getPermissionState().hasChangeSettings()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.of(new UsersEntityAccessInfo(c,
					AuthorizationStatus.accessDenied(String.format(YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE,
							ACCESS_TYPE.CHANGE_SETTINGS.name(), c.getPermissionState().getEntityIdAsString()))));
		}
	}),
	/**
	 * Grants or denies access based on if the user has the CHANGE_SETTINGS
	 * permission on the entity.
	 */
	GRANT_OR_DENY_IF_HAS_CHANGE_PERMISSION((c) -> {
		if (c.getPermissionState().hasChangePermissions()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.of(new UsersEntityAccessInfo(c,
					AuthorizationStatus.accessDenied(String.format(YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE,
							ACCESS_TYPE.CHANGE_PERMISSIONS.name(), c.getPermissionState().getEntityIdAsString()))));
		}
	}),
	/**
	 * Grants or denies access based on if the user has the DELETE permission on the
	 * entity.
	 */
	GRANT_OR_DENY_IF_HAS_DELETE((c) -> {
		if (c.getPermissionState().hasDelete()) {
			return Optional.of(new UsersEntityAccessInfo(c, AuthorizationStatus.authorized()));
		} else {
			return Optional.of(new UsersEntityAccessInfo(c,
					AuthorizationStatus.accessDenied(String.format(YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE,
							ACCESS_TYPE.DELETE.name(), c.getPermissionState().getEntityIdAsString()))));
		}
	}),

	/**
	 * Deny if the user is anonymous.
	 */
	DENY_IF_ANONYMOUS((c) -> {
		if (AuthorizationUtils.isUserAnonymous(c.getUser())) {
			return Optional.of(new UsersEntityAccessInfo(c,
					AuthorizationStatus.accessDenied(ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION)));
		} else {
			return Optional.empty();
		}
	}),
	/**
	 * Deny if the user is not certified.
	 */
	DENY_IF_NOT_CERTIFIED((c) -> {
		if (!AuthorizationUtils.isCertifiedUser(c.getUser())) {
			return Optional.of(
					new UsersEntityAccessInfo(c, AuthorizationStatus.accessDenied(ERR_MESSAGE_CERTIFIED_USER_CONTENT)));
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
					AuthorizationStatus.accessDenied(YOU_HAVE_NOT_YET_AGREED_TO_THE_SYNAPSE_TERMS_OF_USE)));
		} else {
			return Optional.empty();
		}
	});

	private AccessDecider decider;

	private EntityDeciderFunctions(AccessDecider decider) {
		this.decider = decider;
	}

	@Override
	public Optional<UsersEntityAccessInfo> deteremineAccess(AccessContext c) {
		return decider.deteremineAccess(c);
	}

	/**
	 * Make an access decision for the given context using the provided deciders. A
	 * decision is made by asking each decider, in order, to attempt to make a
	 * decision. The first non-empty decision that is found will be returned.
	 * 
	 * @param c        The context provides all of the information about the
	 *                 decision to be made.
	 * @param deciders The ordered AccessDeciders to ask.
	 * @return
	 */
	public static UsersEntityAccessInfo makeAccessDecission(AccessContext c, AccessDecider... deciders) {
		return Arrays.stream(deciders).map(d -> d.deteremineAccess(c)).filter(r -> r.isPresent()).map(r -> r.get())
				.findFirst().orElseThrow(() -> new IllegalStateException("Server Error: No Decision made"));
	}

}
