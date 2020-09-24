package org.sagebionetworks.repo.web.controller;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamSortOrder;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
		IdList idList = new IdList();
		idList.setList(Collections.EMPTY_LIST);
		List<Team> teams = servletTestHelper.listTeams(dispatchServlet, idList);
	}

	@Test
	public void testGetTeamIdsByMember() throws Exception {
		servletTestHelper.getTeamIdsByMember(dispatchServlet, 1L, TeamSortOrder.TEAM_NAME, true);
	}

	/**
	 * PLFM-6390
	 * @throws Exception
	 */
	@Test
	public void testGetTeamMembersByInvalidId() throws Exception {
		String invalidTeamId = "000";
		NotFoundException exception = Assertions.assertThrows(NotFoundException.class, () -> {
			// Call under test
			servletTestHelper.getTeamMembersWithTeamId(dispatchServlet, adminUserId, invalidTeamId);
		});
		Assert.assertEquals("{\"reason\":\"Team does not exist for teamId: " + invalidTeamId + "\"}", exception.getMessage());
	}

	@Test
	public void testGetTeamMembersByValidId() throws Exception {
		String expectedResponse = "{\"totalNumberOfResults\":1,\"results\":[{\"teamId\":\"" + teamToDelete.getId() + "\",\"member\":" +
				"{\"ownerId\":\"1\",\"userName\":\"migrationAdmin\",\"isIndividual\":true},\"isAdmin\":true}]}";
		// Call under test
		MockHttpServletResponse response = servletTestHelper.getTeamMembersWithTeamId(dispatchServlet, adminUserId, teamToDelete.getId());
		Assert.assertEquals(expectedResponse, response.getContentAsString());
	}
}
