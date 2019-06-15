package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.S3TestUtils;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is a an integration test for the MembershipInvitationController.
 * 
 * @author jmhill, adapted by bhoff
 * 
 */
public class MembershipInvitationControllerAutowiredTest extends AbstractAutowiredControllerTestBase {

	@Autowired
	public UserManager userManager;
	
	@Autowired
	private SynapseS3Client s3Client;

	private Long adminUserId;
	private UserInfo adminUserInfo;
	private UserInfo testInvitee;
	private String userEmail;
	
	private static final String TEAM_NAME = "MIS_CONTRL_AW_TEST";
	private Team teamToDelete;

	@Before
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		adminUserInfo = userManager.getUserInfo(adminUserId);
		
		// create a Team
		Team team = new Team();
		team.setName(TEAM_NAME);
		teamToDelete = servletTestHelper.createTeam(dispatchServlet, adminUserId, team);
		
		NewUser user = new NewUser();
		userEmail = UUID.randomUUID().toString() + "@test.com";
		user.setEmail(userEmail);
		user.setUserName(UUID.randomUUID().toString());
		testInvitee = userManager.getUserInfo(userManager.createUser(user));
		assertNotNull(testInvitee.getId().toString());
	}

	@After
	public void after() throws Exception {
		 // creating invitations generates messages. We have to delete
		 // the file handles as part of cleaning up
		 PaginatedResults<MessageToUser> messages = 
				 servletTestHelper.getOutbox(adminUserId, MessageSortBy.SEND_DATE, false, (long)Integer.MAX_VALUE, 0L);
		 for (MessageToUser mtu : messages.getResults()) {
		 	servletTestHelper.deleteMessage(dispatchServlet, adminUserId, mtu.getId());
		 	servletTestHelper.deleteFile(adminUserId, mtu.getFileHandleId());
		 }
		 
		S3TestUtils.doDeleteAfter(s3Client);
		
		servletTestHelper.deleteTeam(dispatchServlet, adminUserId, teamToDelete);
		teamToDelete = null;

		userManager.deletePrincipal(adminUserInfo, testInvitee.getId());
	}

	@Test
	public void testRoundTrip() throws Exception {
		String key = userEmail+".json"; // this is the target for 'sent' email messages to the invitee
		assertFalse(S3TestUtils.doesFileExist(StackConfigurationSingleton.singleton().getS3Bucket(), key, s3Client, 2000L));
		S3TestUtils.addObjectToDelete(StackConfigurationSingleton.singleton().getS3Bucket(), key);
		
		// create an invitation
		String acceptInvitationEndpoint = "https://synapse.org/#acceptInvitationEndpoint:";
		String notificationUnsubscribeEndpoint = "https://synapse.org/#notificationUnsubscribeEndpoint:";
		MembershipInvitation mis = new MembershipInvitation();
		mis.setInviteeId(testInvitee.getId().toString());
		mis.setTeamId(teamToDelete.getId());
		MembershipInvitation created = servletTestHelper.createMembershipInvitation(dispatchServlet, adminUserId, mis,
				acceptInvitationEndpoint, notificationUnsubscribeEndpoint);
		
		// get the invitation
		MembershipInvitation mis2 = servletTestHelper.getMembershipInvitation(dispatchServlet, adminUserId, created.getId());
		assertEquals(created, mis2);
		// get all invitations for the team
		PaginatedResults<MembershipInvitation> miss = servletTestHelper.
				getMembershipInvitationSubmissions(dispatchServlet, adminUserId, teamToDelete.getId());
		assertEquals(1L, miss.getTotalNumberOfResults());
		assertEquals(created, miss.getResults().get(0));
		
		assertTrue(S3TestUtils.doesFileExist(StackConfigurationSingleton.singleton().getS3Bucket(), key, s3Client, 60000L));
	}
}
