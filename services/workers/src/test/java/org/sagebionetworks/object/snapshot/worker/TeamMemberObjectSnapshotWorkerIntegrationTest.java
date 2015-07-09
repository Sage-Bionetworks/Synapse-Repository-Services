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
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.workers.util.aws.message.QueueCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class TeamMemberObjectSnapshotWorkerIntegrationTest {

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
	NewUser user;
	Long userId;
	String type;

	@Before
	public void before() throws Exception {
		assertNotNull(objectRecordDAO);
		assertNotNull(teamManager);
		assertNotNull(userManager);
		queueCleaner.purgeQueue(StackConfiguration.singleton().getAsyncQueueName(QUEUE_NAME));
		
		// Create a user and a team.
		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		team = new Team();
		team.setName("team");
		team = teamManager.create(admin, team);
		teamId = Long.parseLong(team.getId());
		
		// Create a user
		user = new NewUser();
		user.setFirstName("FirstName");
		user.setLastName("LastName");
		user.setUserName("user");
		user.setEmail("employee@sagebase.org");
		userId = userManager.createUser(user);
		
		type = TeamMember.class.getSimpleName().toLowerCase();
		objectRecordDAO.deleteAllStackInstanceBatches(type);
	}
	
	@After
	public void after(){
		teamManager.removeMember(admin, teamId.toString(), userId.toString());
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
	public void addTeamMemberTest() throws Exception {
		Set<String> keys = ObjectSnapshotWorkerIntegrationTestUtils.listAllKeys(objectRecordDAO, type);
		
		teamManager.addMember(admin, teamId.toString(), userManager.getUserInfo(userId));
		
		TeamMember expectedTeamMember = teamManager.getMember(teamId.toString(), userId.toString());
		ObjectRecord expectedRecord = new ObjectRecord();
		expectedRecord.setJsonClassName(expectedTeamMember.getClass().getSimpleName().toLowerCase());
		expectedRecord.setJsonString(EntityFactory.createJSONStringForEntity(expectedTeamMember));
		assertTrue(ObjectSnapshotWorkerIntegrationTestUtils.waitForObjects(keys, Arrays.asList(expectedRecord), objectRecordDAO, type));
	}
}
