package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.LoginRequest;

@ExtendWith(ITTestExtension.class)
public class ITUserInfoRemoval {

	private static SynapseClient client;

	private static Long userId;

	private static String username = UUID.randomUUID().toString();
	private static String password = "password" + UUID.randomUUID().toString();

	private SynapseAdminClient adminSynapse;
	
	public ITUserInfoRemoval(SynapseAdminClient adminSynapse) {
		this.adminSynapse = adminSynapse;
	}
	
	@BeforeAll
	public static void beforeClass(SynapseAdminClient adminSynapse) throws Exception {
		client = new SynapseClientImpl();

		SynapseClientHelper.setEndpoints(client);
		
		userId = SynapseClientHelper.createUser(adminSynapse, client, username, password, true);
	}

	@AfterAll
	public static void afterClass(SynapseAdminClient adminSynapse) throws Exception {
		try {
			adminSynapse.deleteUser(userId);
		} catch (SynapseException e) {
		}
	}

	@Test
	public void testRemoveUserInfo() throws SynapseException {
		UserProfile profile = client.getMyProfile();
		profile.setFirstName("First");
		profile.setLastName("Last");
		profile.setEmail("email@test.net");
		profile.setDisplayName("Some display name");
		profile.setFirstName("First");
		profile.setLastName("Last");
		profile.setPosition("Job");
		profile.setCompany("Organization");
		profile.setLocation("Seattle");
		profile.setOpenIds(Collections.singletonList("OpenID1"));

		client.updateMyProfile(profile);

		// Method under test
		adminSynapse.redactUserInformation(userId.toString());

		String expectedEmail = "gdpr-synapse+" + userId.toString() + "@sagebase.org";

		UserProfile clearedProfile = adminSynapse.getUserProfile(userId.toString());
		assertEquals(expectedEmail, clearedProfile.getEmail());
		assertEquals("", clearedProfile.getFirstName());
		assertEquals("", clearedProfile.getLastName());
		assertEquals(userId.toString(), clearedProfile.getUserName());
		assertEquals(Collections.emptyList(), clearedProfile.getOpenIds());
		assertFalse(clearedProfile.getNotificationSettings().getSendEmailNotifications());
		Assertions.assertNull(clearedProfile.getDisplayName());
		Assertions.assertNull(clearedProfile.getIndustry());
		Assertions.assertNull(clearedProfile.getProfilePicureFileHandleId());
		Assertions.assertNull(clearedProfile.getLocation());
		Assertions.assertNull(clearedProfile.getCompany());
		Assertions.assertNull(clearedProfile.getPosition());


		// Verify we cannot log in with the old username, user ID, or email address (the password should be changed)
		client.logoutForAccessToken();
		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setUsername(username);
		loginRequest.setPassword(password);
		assertThrows(SynapseUnauthorizedException.class, () -> client.login(loginRequest));
		loginRequest.setUsername(userId.toString());
		assertThrows(SynapseUnauthorizedException.class, () -> client.login(loginRequest));
		loginRequest.setUsername(expectedEmail);
		assertThrows(SynapseUnauthorizedException.class, () -> client.login(loginRequest));
	}
}
