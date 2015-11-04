package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.manager.team.TeamConstants;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

public class UserProfileManagerUtils {
	
	public static boolean isOwnerOrAdmin(UserInfo userInfo, String ownerId) {
		if (userInfo == null) return false;
		if (userInfo.isAdmin()) return true;
		if (ownerId != null && ownerId.equals(userInfo.getId().toString())) return true;
		return false;
	}

	public static boolean isOwnerACTOrAdmin(UserInfo userInfo, String ownerId) {
		if (userInfo == null) return false;
		if (userInfo.isAdmin()) return true;
		if (userInfo.getGroups().contains(TeamConstants.ACT_TEAM_ID)) return true;
		if (ownerId != null && ownerId.equals(userInfo.getId().toString())) return true;
		return false;
	}

	/**
	 * 
	 * @param userInfo
	 * @param userProfile Note this is treated as MUTABLE
	 */
	public static void clearPrivateFields(UserInfo userInfo, UserProfile userProfile) {		
		if (userProfile != null) {
			boolean canSeePrivate = isOwnerOrAdmin(userInfo, userProfile.getOwnerId());
			if (!canSeePrivate) {
				PrivateFieldUtils.clearPrivateFields(userProfile);			
			}
		}
	}
	
	/**
	 * 
	 * @param userInfo
	 * @param userProfile Note this is treated as MUTABLE
	 */
	public static void clearPrivateFields(VerificationSubmission verificationSubmission) {		
		if (verificationSubmission != null) {
			PrivateFieldUtils.clearPrivateFields(verificationSubmission);			
		}
	}
	
	/**
	 * 
	 * @param userInfo
	 * @param userGroupHeader Note this is treated as MUTABLE
	 */
	public static void clearPrivateFields(UserInfo userInfo, UserGroupHeader userGroupHeader) {		
		if (userGroupHeader != null) {
			boolean canSeePrivate = UserProfileManagerUtils.isOwnerOrAdmin(userInfo, userGroupHeader.getOwnerId());
			if (!canSeePrivate) {
				PrivateFieldUtils.clearPrivateFields(userGroupHeader);		
			}
		}
	}
	

}
 