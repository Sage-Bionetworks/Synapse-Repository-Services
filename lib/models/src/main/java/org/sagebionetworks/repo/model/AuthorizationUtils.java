package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;

public class AuthorizationUtils {

	private static final String FILE_HANDLE_UNAUTHORIZED_TEMPLATE = "Only the creator of a FileHandle can access it directly by its ID.  FileHandleId = '%1$s', UserId = '%2$s'";

	public static boolean isUserAnonymous(UserInfo userInfo) {
		return isUserAnonymous(userInfo.getId());
	}

	public static boolean isUserAnonymous(UserGroup ug) {
		return isUserAnonymous(Long.parseLong(ug.getId()));
	}

	public static boolean isUserAnonymous(Long id) {
		return id == null || BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().equals(id);
	}

	/**
	 * returns true iff the user is a certified user
	 * 
	 * @param userInfo
	 * @return
	 */
	public static boolean isCertifiedUser(UserInfo userInfo) {
		if (userInfo.isAdmin()) {
			return true;
		}
		return userInfo.getGroups() != null && userInfo.getGroups()
				.contains(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
	}

	/**
	 * Throws UnauthorizedException if the passed user is anonymous.
	 * 
	 * @param user
	 * @throws UnauthorizedException if the user is anonymous.
	 */
	public static void disallowAnonymous(UserInfo user) throws UnauthorizedException {
		if (AuthorizationUtils.isUserAnonymous(user)) {
			throw new UnauthorizedException("Must login to perform this action");
		}
	}

	/**
	 * Is the user an admin or does the userId match the passed creatorId.
	 * 
	 * @param userInfo
	 * @param creatorId
	 * @return
	 */
	public static boolean isUserCreatorOrAdmin(UserInfo userInfo, String creatorId) {
		// Admins can see anything.
		if (userInfo.isAdmin()) {
			return true;
		}
		// Only the creator can see the raw file handle
		return userInfo.getId().toString().equals(creatorId);
	}

	/**
	 * Is the user either an admin or the creator of the passed fileHandleId.
	 * @param userInfo
	 * @param fileHandleId
	 * @param creator
	 * @return
	 */
	public static AuthorizationStatus canAccessRawFileHandleByCreator(UserInfo userInfo, String fileHandleId, String creator) {
		if (AuthorizationUtils.isUserCreatorOrAdmin(userInfo, creator)) {
			return AuthorizationStatus.authorized();
		} else {
			return AuthorizationStatus.accessDenied(
					String.format(FILE_HANDLE_UNAUTHORIZED_TEMPLATE, fileHandleId, userInfo.getId().toString()));
		}
	}
	
	public static boolean isACTTeamMemberOrAdmin(UserInfo userInfo) throws DatastoreException, UnauthorizedException {
		if (userInfo.isAdmin()) {
			return true;
		}
		if(userInfo.getGroups() != null) {
			if(userInfo.getGroups().contains(TeamConstants.ACT_TEAM_ID)) return true;
		}
		return false;
	}
	
	public static boolean isReportTeamMemberOrAdmin(UserInfo userInfo) throws DatastoreException, UnauthorizedException {
		if (userInfo.isAdmin()) {
			return true;
		}
		if(userInfo.getGroups() != null) {
			if(userInfo.getGroups().contains(TeamConstants.SYNAPSE_REPORT_TEAM_ID)) return true;
		}
		return false;
	}
	
	public static boolean isSageEmployeeOrAdmin(UserInfo userInfo) {
		if (userInfo.isAdmin()) {
			return true;
		}
		if(userInfo.getGroups() != null) {
			if(userInfo.getGroups().contains(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId())) return true;
		}
		return false;
	}
}
