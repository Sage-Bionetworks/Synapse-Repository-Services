package org.sagebionetworks.repo.web.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMemberTypeFilterOptions;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ContextConfiguration(locations = { "classpath:test-context.xml" })
@ExtendWith(SpringExtension.class)
public class TeamServiceAutowireTest {
	
	private static final int THREADS_N = 4;

	@Autowired
	private TeamService teamService;

	@Autowired
	private UserManager userManager;

	private String teamEndpoint = "http://localhost/teamEndpoint";
	private ExecutorService executor;
	private UserInfo userInfo;
	private UserInfo adminUserInfo;
	private String teamId;

	@BeforeEach
	public void before() {
		NewUser user = new NewUser();
		user.setFirstName("Test");
		user.setLastName("Test");
		user.setUserName(UUID.randomUUID().toString());
		user.setEmail(UUID.randomUUID().toString() + "@test.org");

		userInfo = userManager.getUserInfo(userManager.createUser(user));
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		Team team = new Team();
		team.setName("teamName");

		teamId = teamService.create(adminUserInfo.getId(), team).getId();
		executor = Executors.newFixedThreadPool(THREADS_N);
	}

	@AfterEach
	public void after() {
		teamService.delete(adminUserInfo.getId(), teamId);
		userManager.deletePrincipal(adminUserInfo, userInfo.getId());
		executor.shutdown();
	}

	// Test for PLFM-5766
	@Test
	public void testAddMemberConcurrent() throws Exception {
		int maxTasks = 10;
		
		List<Future<Boolean>> tasks = new ArrayList<>(maxTasks);
		
		for (int i = 0; i < maxTasks; i++) {
			Future<Boolean> task = executor.submit(() -> {
				return teamService.addMember(adminUserInfo.getId(), teamId, userInfo.getId().toString(), teamEndpoint, null);
			});
			tasks.add(task);
		}
		
		int count = 0;
		
		for (Future<Boolean> task : tasks) {
			if (task.get()) {
				count++;
			}
		}
		
		// Even though multiple add member calls were made in parallel the user should have been added only once
		assertEquals(1, count);

	}

	/**
	 * PLFM-6390
	 * @throws Exception
	 */
	@Test
	public void testGetMembersInvalidTeamId() throws Exception {
		String invalidTeamId = "404";
		String expectedResponse = "Team does not exist for teamId: " + invalidTeamId;
		NotFoundException exception = Assertions.assertThrows(NotFoundException.class, () -> {
			// Call under test
			teamService.getMembers(invalidTeamId, null, TeamMemberTypeFilterOptions.ALL, 1, 0 );
		});
		assertEquals(expectedResponse, exception.getMessage());
	}

	/**
	 * PLFM-6390
	 * @throws Exception
	 */
	@Test
	public void testGetMembersValidTeamId() throws Exception {
		teamService.getMembers(teamId, null, TeamMemberTypeFilterOptions.ALL, 1, 0 );
	}

}
