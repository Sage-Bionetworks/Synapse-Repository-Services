package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;

public class UserProfileManagerUtilsTest {

	@Test
	public void testIsPublic() {
		assertFalse(UserProfileManagerUtils.isPublic("rStudioUrl"));
		assertTrue(UserProfileManagerUtils.isPublic("displayName"));
	}
	
	@Test
	public void testIsOwnerOrAdmin() {
		UserInfo userInfo = new UserInfo(false/*not admin*/);
		UserGroup individualGroup=new UserGroup();
		individualGroup.setIsIndividual(true);
		String individualGroupId = "1001";
		individualGroup.setId(individualGroupId);
		userInfo.setIndividualGroup(individualGroup);
		User user = new User();
		user.setUserId("user@sagebase.org");
		userInfo.setUser(user);
		assertTrue(UserProfileManagerUtils.isOwnerOrAdmin(userInfo, individualGroupId));
		String otherId = "1002";
		assertFalse(UserProfileManagerUtils.isOwnerOrAdmin(userInfo, otherId));
		
		UserInfo adminInfo = new UserInfo(true/*is admin*/);
		adminInfo.setIndividualGroup(individualGroup);
		assertTrue(UserProfileManagerUtils.isOwnerOrAdmin(adminInfo, otherId));
	}
	
	@Test
	public void testClearPrivateFields() {
		UserProfile up = new UserProfile();
		up.setDisplayName("me");
		up.setRStudioUrl("http://rstudio");
		UserProfileManagerUtils.clearPrivateFields(up);
		assertEquals("me", up.getDisplayName());
		assertNull(up.getRStudioUrl());
	}

}
