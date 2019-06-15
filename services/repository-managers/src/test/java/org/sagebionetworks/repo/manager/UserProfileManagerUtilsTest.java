package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.manager.team.TeamConstants;
import org.sagebionetworks.repo.model.SchemaCache;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.verification.AttachmentMetadata;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
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
	
	@Test
	public void testIsOwnerACTOrAdmin() {
		UserInfo userInfo = new UserInfo(false/*not admin*/, 1001L);
		assertTrue(UserProfileManagerUtils.isOwnerACTOrAdmin(userInfo, "1001"));
		String otherId = "1002";
		assertFalse(UserProfileManagerUtils.isOwnerACTOrAdmin(userInfo, otherId));
		
		UserInfo adminInfo = new UserInfo(true/*is admin*/, 456L);
		assertTrue(UserProfileManagerUtils.isOwnerACTOrAdmin(adminInfo, otherId));

		assertFalse(UserProfileManagerUtils.isOwnerACTOrAdmin(null, otherId));
		
		userInfo.setGroups(Collections.singleton(TeamConstants.ACT_TEAM_ID));
		assertTrue(UserProfileManagerUtils.isOwnerACTOrAdmin(userInfo, otherId));
		
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
	
	/**
	 * Test for PLFM-3925.
	 */
	@Test
	public void testClearPrivateFieldsCreatedOn() {
		UserInfo userInfo = new UserInfo(false);
		UserProfile up = new UserProfile();
		Date createdOn = new Date(123L);
		up.setCreatedOn(createdOn);
		UserProfileManagerUtils.clearPrivateFields(userInfo, up);
		assertEquals(createdOn, up.getCreatedOn());
	}

	@Test
	public void testClearPrivateFieldsAsAdmin() {
		UserInfo userInfo = new UserInfo(true);
		UserProfile up = new UserProfile();
		UserProfileManagerUtils.clearPrivateFields(userInfo, up);
	}

	@Test
	public void testClearPrivateFieldsVerificationSubmission() {
		VerificationSubmission v = new VerificationSubmission();
		v.setFirstName("fname");
		v.setLastName("lname");
		v.setOrcid("http://orcid.org/foo");
		v.setCreatedBy("102");
		List<AttachmentMetadata> attachments = new ArrayList<AttachmentMetadata>();
		for (String fileHandleId : new String[]{"101", "102"}) {
			AttachmentMetadata attachmentMetadata = new AttachmentMetadata();
			attachmentMetadata.setId(fileHandleId);
			attachments.addAll(attachments);
		}
		v.setAttachments(attachments);
		VerificationState state = new VerificationState();
		state.setCreatedBy("act");
		state.setCreatedOn(new Date());
		state.setReason("just because");
		state.setState(VerificationStateEnum.APPROVED);
		v.setStateHistory(Collections.singletonList(state));
		
		UserProfileManagerUtils.clearPrivateFields(v);
		
		assertEquals("fname", v.getFirstName());
		assertEquals("lname", v.getLastName());
		assertEquals("http://orcid.org/foo", v.getOrcid());
		assertEquals("102", v.getCreatedBy());
		assertNull(v.getAttachments());
		assertEquals(1, v.getStateHistory().size());
		VerificationState scrubbedState = v.getStateHistory().get(0);
		assertNull(scrubbedState.getCreatedBy());
		assertNotNull(scrubbedState.getCreatedOn());
		assertNull(scrubbedState.getReason());
		assertEquals(VerificationStateEnum.APPROVED, scrubbedState.getState());
	}


}
