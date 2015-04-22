package org.sagebionetworks.principal.worker;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.asynchronous.workers.sqs.MessageQueue;
import org.sagebionetworks.asynchronous.workers.sqs.MessageReceiver;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class PrincipalPrefixWorkerIntegrationTest {
	
	private static final int MAX_WAIT_MS = 1000*60;

	String commonPrefix;
	
	@Autowired
	PrincipalPrefixDAO principalPrefixDao;
	
	@Autowired
	TeamManager teamManager;
	@Autowired
	UserManager userManager;
	@Autowired
	MessageReceiver principalPrefixQueueMessageReveiver;
	
	UserInfo admin;
	Team team;
	NewUser user;
	Long userId;
	Long teamId;
	
	@Before
	public void before() throws NotFoundException, InterruptedException{
		principalPrefixDao.truncateTable();
		principalPrefixQueueMessageReveiver.emptyQueue();
		commonPrefix = "PrincipalPrefixWorkerIntegrationTest";
		// Create a user and a team.
		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		team = new Team();
		team.setName(commonPrefix+"team");
		team = teamManager.create(admin, team);
		teamId = Long.parseLong(team.getId());
		// Create a user
		user = new NewUser();
		user.setFirstName("James");
		user.setLastName("Bond");
		user.setUserName(commonPrefix+"user");
		user.setEmail(commonPrefix+"@sagebase.org");
		userId = userManager.createUser(user);
	}
	
	@After
	public void after(){
		if(team != null){
			try {
				teamManager.delete(admin, team.getId());
			} catch (Exception e) {}
		}
		if(userId != null){
			try {
				userManager.deletePrincipal(admin, userId);
			} catch (Exception e) {}
		}
	}
	
	@Test
	public void testWorker() throws InterruptedException{
		// Wait until we can find both
		waitForPrefix(commonPrefix, 2);
		List<Long> results = principalPrefixDao.listPrincipalsForPrefix(commonPrefix, 100L, 0L);
		assertNotNull(results);
		assertEquals(2, results.size());
		assertEquals(teamId, results.get(0));
		assertEquals(userId, results.get(1));
		// team only
		results = principalPrefixDao.listPrincipalsForPrefix(team.getName(), 100L, 0L);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(teamId, results.get(0));
		// first name user only
		results = principalPrefixDao.listPrincipalsForPrefix(user.getFirstName(), 100L, 0L);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(userId, results.get(0));
		// last name user only
		results = principalPrefixDao.listPrincipalsForPrefix(user.getLastName(), 100L, 0L);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(userId, results.get(0));
	}
	
	/**
	 * Helper to wait for the worker
	 * @param prefix
	 * @param expectedCount
	 * @throws InterruptedException
	 */
	private void waitForPrefix(String prefix, long expectedCount) throws InterruptedException{
		long start = System.currentTimeMillis();
		while(true){
			long count = principalPrefixDao.countPrincipalsForPrefix(prefix);
			if(count == expectedCount){
				return;
			}else{
				System.out.println("Waiting for PrincipalPrefixWorker with count "+count);
				Thread.sleep(1000);
			}
			if(System.currentTimeMillis() - start > MAX_WAIT_MS){
				fail("Timed out waiting for worker");
			}
		}
	}

}
