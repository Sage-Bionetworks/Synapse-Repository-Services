package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.EntityService;
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

	private String userName = AuthorizationConstants.TEST_USER_NAME;
	
	private static final String TEAM_NAME = "MIS_CONTRL_AW_TEST";
	private Team teamToDelete;

	@BeforeClass
	public static void beforeClass() throws ServletException {
		dispatchServlet = DispatchServletSingleton.getInstance();
	}
	private String inviteeUserName = AuthorizationConstants.ADMIN_USER_NAME;
	private UserInfo testInvitee;

	@Before
	public void before() throws Exception {		
		// create a Team
		Team team = new Team();
		team.setName(TEAM_NAME);
		teamToDelete = ServletTestHelper.createTeam(dispatchServlet, userName, team);
		testInvitee = userManager.getUserInfo(inviteeUserName);
		assertNotNull(testInvitee.getIndividualGroup().getId());
	}

	@After
	public void after() throws Exception {
		 ServletTestHelper.deleteTeam(dispatchServlet, userName, teamToDelete);
		 teamToDelete = null;
	}

	@Test
	public void testRoundTrip() throws Exception {
		// create an invitation
		MembershipInvtnSubmission mis = new MembershipInvtnSubmission();
		mis.setInviteeId(testInvitee.getIndividualGroup().getId());
		mis.setTeamId(teamToDelete.getId());
		MembershipInvtnSubmission created = ServletTestHelper.createMembershipInvitation(dispatchServlet, userName, mis);
		
		// get the invitation
		MembershipInvtnSubmission mis2 = ServletTestHelper.getMembershipInvitation(dispatchServlet, userName, created.getId());
		assertEquals(created, mis2);
		// get all invitations for the team
		PaginatedResults<MembershipInvtnSubmission> miss = ServletTestHelper.
				getMembershipInvitationSubmissions(dispatchServlet, userName, teamToDelete.getId());
		assertEquals(1L, miss.getTotalNumberOfResults());
		assertEquals(created, miss.getResults().get(0));
	}

	


}
