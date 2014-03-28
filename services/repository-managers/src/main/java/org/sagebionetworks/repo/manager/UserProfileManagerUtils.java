package org.sagebionetworks.repo.manager;

import java.util.Map;

import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.schema.LinkDescription;
import org.sagebionetworks.schema.LinkDescription.LinkRel;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;

public class UserProfileManagerUtils {
	
	public static boolean isOwnerOrAdmin(UserInfo userInfo, String ownerId) {
		if (userInfo == null) return false;
		if (userInfo.isAdmin()) return true;
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
			boolean canSeePrivate = UserProfileManagerUtils.isOwnerOrAdmin(userInfo, userProfile.getOwnerId());
			if (!canSeePrivate) {
				PrivateFieldUtils.clearPrivateFields(userProfile);			
			}
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
 