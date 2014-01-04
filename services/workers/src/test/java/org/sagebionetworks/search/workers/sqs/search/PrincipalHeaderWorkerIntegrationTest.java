package org.sagebionetworks.search.workers.sqs.search;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.asynchronous.workers.sqs.MessageReceiver;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.PrincipalHeaderDAO;
import org.sagebionetworks.repo.model.PrincipalHeaderDAO.MATCH_TYPE;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This test validates that change messages related to users and teams get
 * propagated to the PrincipalHeader table
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class PrincipalHeaderWorkerIntegrationTest {
	
	private static final long MAX_WAIT = 60 * 1000; // one minute
	
	/**
	 * Some randomly typed string to match by
	 */
	private static final String PREFIX = "urmckwjfmejucmekf";
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private TeamManager teamManager;
	
	@Autowired
	private PrincipalHeaderDAO prinHeadDAO;
	
	@Autowired
	private MessageReceiver principalHeaderQueueMessageReceiver;
	
	private UserInfo adminUserInfo;
	
	private String username;
	private String teamName;
	
	private long userId;
	private long teamId;
	
	@Before
	public void before() throws Exception {
		// Before we start, make sure the queue and table is empty
		emptyQueue();
		emptyPrefixTable();
		
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		username = "user name " + PREFIX + UUID.randomUUID().toString();
		teamName = "team name " + PREFIX + UUID.randomUUID().toString();
		
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setDisplayName(username);
		userId = userManager.createUser(user);
		
		Team team = new Team();
		team.setName(teamName);
		team = teamManager.create(adminUserInfo, team);
		teamId = Long.parseLong(team.getId());
	}

	/**
	 * Empty the queue by processing all messages on the queue.
	 */
	private void emptyQueue() throws InterruptedException {
		long start = System.currentTimeMillis();
		int count = 0;
		do {
			count = principalHeaderQueueMessageReceiver.triggerFired();
			System.out.println("Emptying the principal header queue, there were at least: "
							+ count + " messages on the queue");
			Thread.yield();
			long elapse = System.currentTimeMillis() - start;
			if (elapse > MAX_WAIT * 2)
				throw new RuntimeException(
						"Timed out waiting process all messages that were on the queue before the tests started.");
		} while (count > 0);
	}
	
	/**
	 * Deletes all entries in the PrincipalHeader table
	 */
	private void emptyPrefixTable() throws Exception {
		List<Long> results;
		do {
			results = prinHeadDAO.query(null, MATCH_TYPE.PREFIX, null, null, 10, 0);
			for (Long id : results) {
				prinHeadDAO.delete(id);
			}
		} while (results.size() > 0);
	}

	@After
	public void after() throws Exception {
		teamManager.delete(adminUserInfo, "" + teamId);
		
		userManager.deletePrincipal(adminUserInfo, userId);
		userManager.deletePrincipal(adminUserInfo, teamId);
		
		emptyPrefixTable();
	}
	
	@Test
	public void testRoundTrip() throws Exception {
		List<Long> results = null;
		
		long start = System.currentTimeMillis();
		while (results == null || results.size() < 2) {
			// Query for the two principals
			results = prinHeadDAO.query(PREFIX, MATCH_TYPE.PREFIX, null, null, 10, 0);
			
			Thread.sleep(1000);
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Timed out waiting for message to be sent", elapse < MAX_WAIT);
		}
		assertTrue(results.contains(userId));
		assertTrue(results.contains(teamId));
	}
	
}
