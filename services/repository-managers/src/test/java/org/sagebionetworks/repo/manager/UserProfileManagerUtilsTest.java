package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.repo.model.SchemaCache;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.schema.ObjectSchema;

public class UserProfileManagerUtilsTest {

	@Test
	public void testIsPublic() {
		ObjectSchema schema = SchemaCache.getSchema(UserProfile.class);
		assertFalse(UserProfileManagerUtils.isPublic("rStudioUrl", schema));
		assertTrue(UserProfileManagerUtils.isPublic("displayName", schema));
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

		assertFalse(UserProfileManagerUtils.isOwnerOrAdmin(null, otherId));
	}
	
	@Test
	public void testClearPrivateFields() {
		String email = "test@example.com";
		UserInfo userInfo = new UserInfo(false);
		UserProfile up = new UserProfile();
		up.setEmail(email);
		up.setDisplayName("me");
		AttachmentData pic = new AttachmentData();
		pic.setPreviewId("a preview ID");
		up.setPic(pic);
		up.setRStudioUrl("http://rstudio");
		UserProfileManagerUtils.clearPrivateFields(userInfo, up);
		assertFalse(email.equals(up.getEmail()));
		assertTrue(up.getEmail().contains("..."));
		assertEquals("me", up.getDisplayName());
		assertEquals(pic, up.getPic());
		assertNull(up.getRStudioUrl());
	}

	@Test
	public void testClearPrivateFieldsAsAdmin() {
		String email = "test@example.com";
		UserInfo userInfo = new UserInfo(true);
		UserProfile up = new UserProfile();
		up.setEmail(email);
		up.setDisplayName("me");
		AttachmentData pic = new AttachmentData();
		pic.setPreviewId("a preview ID");
		up.setPic(pic);
		up.setRStudioUrl("http://rstudio");
		UserProfileManagerUtils.clearPrivateFields(userInfo, up);
		assertEquals(email, up.getEmail());
		assertEquals("me", up.getDisplayName());
	}

}
