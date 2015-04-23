package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This is a an integration test for the MembershipInvitationController.
 * 
 * @author jmhill, adapted by bhoff
 * 
 */
public class MembershipInvitationControllerAutowiredTest extends AbstractAutowiredControllerTestBase {

	@Autowired
	public UserManager userManager;

	private Long adminUserId;
	private UserInfo adminUserInfo;
	private UserInfo testInvitee;
	
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
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		testInvitee = userManager.getUserInfo(userManager.createUser(user));
		assertNotNull(testInvitee.getId().toString());
	}

	@After
	public void after() throws Exception {
		servletTestHelper.deleteTeam(dispatchServlet, adminUserId, teamToDelete);
		 teamToDelete = null;
		 
		 userManager.deletePrincipal(adminUserInfo, testInvitee.getId());
	}

	@Test
	public void testRoundTrip() throws Exception {
		// create an invitation
		MembershipInvtnSubmission mis = new MembershipInvtnSubmission();
		mis.setInviteeId(testInvitee.getId().toString());
		mis.setTeamId(teamToDelete.getId());
		MembershipInvtnSubmission created = servletTestHelper.createMembershipInvitation(dispatchServlet, adminUserId, mis);
		
		// get the invitation
		MembershipInvtnSubmission mis2 = servletTestHelper.getMembershipInvitation(dispatchServlet, adminUserId, created.getId());
		assertEquals(created, mis2);
		// get all invitations for the team
		PaginatedResults<MembershipInvtnSubmission> miss = servletTestHelper.
				getMembershipInvitationSubmissions(dispatchServlet, adminUserId, teamToDelete.getId());
		assertEquals(1L, miss.getTotalNumberOfResults());
		assertEquals(created, miss.getResults().get(0));
	}
}
