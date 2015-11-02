package org.sagebionetworks;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.UserBundle;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.verification.VerificationPagedResults;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

public class ITVerification {
	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapseOne;
	private static Long user1ToDelete;
	private static String repoEndpoint;
	private static String username;
	private static String password;

	@BeforeClass
	public static void beforeClass() throws Exception {
		// Create 2 users
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse
				.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapseOne = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapseOne);
		user1ToDelete = SynapseClientHelper
				.createUser(adminSynapse, synapseOne);
		repoEndpoint = StackConfiguration.getRepositoryServiceEndpoint();
		username = StackConfiguration.getCloudMailInUser();
		password = StackConfiguration.getCloudMailInPassword();

	}

	@Before
	public void before() throws Exception {
	}

	@AfterClass
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(user1ToDelete);
		} catch (SynapseException e) {
		}

	}
	
	@Test
	public void testGetBundle() throws Exception {
		UserProfile userProfile = synapseOne.getMyProfile();
		UserBundle bundle = synapseOne.getMyOwnUserBundle(63/*everything*/);
		assertEquals(userProfile, bundle.getUserProfile());
		
		bundle = synapseOne.getUserBundle(Long.parseLong(userProfile.getOwnerId()), 63/*everything*/);
		assertEquals(userProfile, bundle.getUserProfile());	
	}

	@Test
	public void testVerification() throws Exception {
		
		VerificationSubmission verificationSubmission = new VerificationSubmission();
		// this can't work since I can't make an ORCID programmatically
		synapseOne.createVerificationSubmission(verificationSubmission);
		
		Long submitterId = Long.parseLong(synapseOne.getMyProfile().getOwnerId());
		VerificationStateEnum state = VerificationStateEnum.APPROVED;
		VerificationPagedResults list = adminSynapse.listVerificationSubmissions(state, submitterId, 10L, 0L);
		
		VerificationState stateObject = new VerificationState();
		// this can't work since I can't make an ORCID programmatically
		adminSynapse.updateVerificationState(101L, stateObject);
	}





}
