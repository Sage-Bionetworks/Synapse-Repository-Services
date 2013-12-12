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
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.PaginatedResults;
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
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MembershipInvitationControllerAutowiredTest {

	@Autowired
	public UserManager userManager;

	private static HttpServlet dispatchServlet;

	private UserInfo adminUserInfo;
	private String adminUsername;
	private UserInfo testInvitee;
	
	private static final String TEAM_NAME = "MIS_CONTRL_AW_TEST";
	private Team teamToDelete;

	@BeforeClass
	public static void beforeClass() throws ServletException {
		dispatchServlet = DispatchServletSingleton.getInstance();
	}

	@Before
	public void before() throws Exception {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		adminUsername = adminUserInfo.getIndividualGroup().getName();
		
		// create a Team
		Team team = new Team();
		team.setName(TEAM_NAME);
		teamToDelete = ServletTestHelper.createTeam(dispatchServlet, adminUsername, team);
		
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@");
		testInvitee = userManager.getUserInfo(userManager.createUser(user));
		assertNotNull(testInvitee.getIndividualGroup().getId());
	}

	@After
	public void after() throws Exception {
		 ServletTestHelper.deleteTeam(dispatchServlet, adminUsername, teamToDelete);
		 teamToDelete = null;
		 
		 userManager.deletePrincipal(adminUserInfo, Long.parseLong(testInvitee.getIndividualGroup().getId()));
	}

	@Test
	public void testRoundTrip() throws Exception {
		// create an invitation
		MembershipInvtnSubmission mis = new MembershipInvtnSubmission();
		mis.setInviteeId(testInvitee.getIndividualGroup().getId());
		mis.setTeamId(teamToDelete.getId());
		MembershipInvtnSubmission created = ServletTestHelper.createMembershipInvitation(dispatchServlet, adminUsername, mis);
		
		// get the invitation
		MembershipInvtnSubmission mis2 = ServletTestHelper.getMembershipInvitation(dispatchServlet, adminUsername, created.getId());
		assertEquals(created, mis2);
		// get all invitations for the team
		PaginatedResults<MembershipInvtnSubmission> miss = ServletTestHelper.
				getMembershipInvitationSubmissions(dispatchServlet, adminUsername, teamToDelete.getId());
		assertEquals(1L, miss.getTotalNumberOfResults());
		assertEquals(created, miss.getResults().get(0));
	}
}
