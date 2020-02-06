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
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.LoginRequest;

public class ITUserInfoRemoval {

	private static SynapseAdminClient adminClient;
	private static SynapseClient client;

	private static Long userId;

	private static String username = UUID.randomUUID().toString();
	private static String password = "password" + UUID.randomUUID().toString();

	@BeforeAll
	public static void beforeClass() throws Exception {
		adminClient = new SynapseAdminClientImpl();
		client = new SynapseClientImpl();
		
		SynapseClientHelper.setEndpoints(adminClient);
		SynapseClientHelper.setEndpoints(client);

		adminClient.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminClient.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		adminClient.clearAllLocks();
		
		userId = SynapseClientHelper.createUser(adminClient, client, username, password);
	}

	@AfterAll
	public static void afterClass() throws Exception {
		try {
			adminClient.deleteUser(userId);
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
		adminClient.redactUserInformation(userId.toString());

		String expectedEmail = "gdpr-synapse+" + userId.toString() + "@sagebase.org";

		UserProfile clearedProfile = adminClient.getUserProfile(userId.toString());
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
		client.logout();
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
