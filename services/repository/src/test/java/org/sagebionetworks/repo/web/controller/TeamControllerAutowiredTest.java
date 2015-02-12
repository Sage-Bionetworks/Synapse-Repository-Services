package org.sagebionetworks.repo.web.controller;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.IdSet;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Simplistic test to see if things are wired up correctly
 * All messages are retrieved oldest first
 */
public class TeamControllerAutowiredTest extends AbstractAutowiredControllerTestBase {

	@Autowired
	public UserManager userManager;

	private Long adminUserId;
	private UserInfo adminUserInfo;
	
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
	}

	@After
	public void after() throws Exception {
		servletTestHelper.deleteTeam(dispatchServlet, adminUserId, teamToDelete);
		 teamToDelete = null;
		 
	}

	@Test
	public void testListTeams() throws Exception {
		IdSet idSet = new IdSet();
		idSet.setSet(Collections.EMPTY_SET);
		List<Team> teams = servletTestHelper.listTeams(dispatchServlet, idSet);
	}
}
