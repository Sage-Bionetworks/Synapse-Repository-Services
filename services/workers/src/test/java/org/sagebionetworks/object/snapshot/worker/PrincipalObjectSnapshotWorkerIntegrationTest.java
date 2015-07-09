package org.sagebionetworks.object.snapshot.worker;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.workers.util.aws.message.QueueCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class PrincipalObjectSnapshotWorkerIntegrationTest {

	private static final String QUEUE_NAME = "OBJECT";
	@Autowired
	private ObjectRecordDAO objectRecordDAO;
	@Autowired
	private TeamManager teamManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private QueueCleaner queueCleaner;
	
	UserInfo admin;
	Team team;
	Long teamId;
	String type;

	@Before
	public void before() throws Exception {
		assertNotNull(objectRecordDAO);
		assertNotNull(teamManager);
		assertNotNull(userManager);
		queueCleaner.purgeQueue(StackConfiguration.singleton().getAsyncQueueName(QUEUE_NAME));
		
		type = Team.class.getSimpleName().toLowerCase();
		objectRecordDAO.deleteAllStackInstanceBatches(type);
	}
	
	@After
	public void after(){
		if(team != null){
			try {
				teamManager.delete(admin, team.getId());
			} catch (Exception e) {}
		}
	}
	
	@Test
	public void teamTest() throws Exception {
		Set<String> keys = ObjectSnapshotWorkerIntegrationTestUtils.listAllKeys(objectRecordDAO, type);
		
		// Create a user and a team.
		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		team = new Team();
		team.setName("team");
		team = teamManager.create(admin, team);
		teamId = Long.parseLong(team.getId());
		
		Team expectedTeam = teamManager.get(team.getId());
		ObjectRecord expectedRecord = new ObjectRecord();
		expectedRecord.setJsonClassName(expectedTeam.getClass().getSimpleName().toLowerCase());
		expectedRecord.setJsonString(EntityFactory.createJSONStringForEntity(expectedTeam));
		assertTrue(ObjectSnapshotWorkerIntegrationTestUtils.waitForObjects(keys, Arrays.asList(expectedRecord), objectRecordDAO, type));
	}
}
