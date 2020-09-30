package org.sagebionetworks.repo.web.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Simplistic test to see if things are wired up correctly
 * All messages are retrieved oldest first
 */
@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
public class TeamControllerAutowiredTest extends AbstractAutowiredControllerJunit5TestBase {

	@Autowired
	public UserManager userManager;

	private Long adminUserId;
	private UserInfo adminUserInfo;
	
	private static final String TEAM_NAME = "MIS_CONTRL_AW_TEST";
	private Team teamToDelete;

	@BeforeEach
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

	@AfterEach
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
		String invalidTeamId = "404";
		String expectedResponse = "{\"reason\":\"Team does not exist for teamId: " + invalidTeamId + "\"}";
		NotFoundException exception = Assertions.assertThrows(NotFoundException.class, () -> {
			// Call under test
			servletTestHelper.getTeamMembersWithTeamId(dispatchServlet, adminUserId, invalidTeamId);
		});
		assertEquals(expectedResponse, exception.getMessage());
	}

	@Test
	public void testGetTeamMembersByValidId() throws Exception {
		String expectedResponse = "{\"totalNumberOfResults\":1,\"results\":[{\"teamId\":\"" + teamToDelete.getId() + "\",\"member\":" +
				"{\"ownerId\":\"1\",\"userName\":\"migrationAdmin\",\"isIndividual\":true},\"isAdmin\":true}]}";
		// Call under test
		MockHttpServletResponse response = servletTestHelper.getTeamMembersWithTeamId(dispatchServlet, adminUserId, teamToDelete.getId());
		assertEquals(expectedResponse, response.getContentAsString());
	}
}
