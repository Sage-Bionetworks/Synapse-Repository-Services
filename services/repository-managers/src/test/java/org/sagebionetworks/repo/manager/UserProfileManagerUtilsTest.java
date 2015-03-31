package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.repo.model.SchemaCache;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.schema.ObjectSchema;

public class UserProfileManagerUtilsTest {

	@Test
	public void testIsPublic() {
		ObjectSchema schema = SchemaCache.getSchema(UserProfile.class);
		assertFalse(PrivateFieldUtils.isPublic("rStudioUrl", schema));
	}
	
	@Test
	public void testIsOwnerOrAdmin() {
		UserInfo userInfo = new UserInfo(false/*not admin*/, 1001L);
		assertTrue(UserProfileManagerUtils.isOwnerOrAdmin(userInfo, "1001"));
		String otherId = "1002";
		assertFalse(UserProfileManagerUtils.isOwnerOrAdmin(userInfo, otherId));
		
		UserInfo adminInfo = new UserInfo(true/*is admin*/, 456L);
		assertTrue(UserProfileManagerUtils.isOwnerOrAdmin(adminInfo, otherId));

		assertFalse(UserProfileManagerUtils.isOwnerOrAdmin(null, otherId));
	}
	
	/**
	 * Test updated for PLFM-3317.
	 */
	@Test
	public void testClearPrivateFields() {
		UserInfo userInfo = new UserInfo(false);
		UserProfile up = new UserProfile();
		up.setProfilePicureFileHandleId("456");
		up.setRStudioUrl("http://rstudio");
		UserProfileManagerUtils.clearPrivateFields(userInfo, up);
		assertEquals("456", up.getProfilePicureFileHandleId());
		assertNull(up.getRStudioUrl());
	}

	@Test
	public void testClearPrivateFieldsAsAdmin() {
		UserInfo userInfo = new UserInfo(true);
		UserProfile up = new UserProfile();
		AttachmentData pic = new AttachmentData();
		pic.setPreviewId("a preview ID");
		up.setPic(pic);
		up.setRStudioUrl("http://rstudio");
		UserProfileManagerUtils.clearPrivateFields(userInfo, up);
	}

}
