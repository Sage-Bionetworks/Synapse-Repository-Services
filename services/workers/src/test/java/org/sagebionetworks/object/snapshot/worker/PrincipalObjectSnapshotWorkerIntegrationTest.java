package org.sagebionetworks.object.snapshot.worker;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class PrincipalObjectSnapshotWorkerIntegrationTest {

	private static final int TIME_OUT = 2 * 60 * 1000;

	@Autowired
	private ObjectRecordDAO objectRecordDAO;
	@Autowired
	private TeamDAO teamDAO;
	@Autowired
	private TeamManager teamManager;
	@Autowired
	private UserManager userManager;
	
	UserInfo admin;
	Team team;
	Long teamId;
	

	@Before
	public void before() throws Exception {
		assertNotNull(objectRecordDAO);
		assertNotNull(teamDAO);
		assertNotNull(teamManager);
		assertNotNull(userManager);
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
		Set<String> keys = objectRecordDAO.listAllKeys();
		
		// Create a user and a team.
		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		team = new Team();
		team.setName("team");
		team = teamManager.create(admin, team);
		teamId = Long.parseLong(team.getId());
		
		Team expectedTeam = teamManager.get(team.getId());
		ObjectRecord expectedRecord = new ObjectRecord();
		expectedRecord.setObjectType(expectedTeam.getClass().getSimpleName());
		expectedRecord.setJsonString(EntityFactory.createJSONStringForEntity(expectedTeam));
		assertTrue(waitForObjects(keys, Arrays.asList(expectedRecord)));
	}
	
	/**
	 * Helper method that keep looking for new ObjectRecord files in S3.
	 * 
	 * @return true if found the expectedObjectRecords in TIME_OUT milliseconds,
	 *         false otherwise.
	 */
	private boolean waitForObjects(Set<String> oldKeys, List<ObjectRecord> expectedRecords) throws Exception {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() < start + TIME_OUT) {
			Set<String> newKeys = objectRecordDAO.listAllKeys();
			newKeys.removeAll(oldKeys);
			if (newKeys.size() != 0) {
				return findRecords(expectedRecords, newKeys);
			}

			// wait for 1 second before calling the service again
			Thread.sleep(1000);
		}
		return false;
	}

	private boolean findRecords(List<ObjectRecord> expectedRecords, Set<String> newKeys) throws IOException {
		List<ObjectRecord> newRecords = new ArrayList<ObjectRecord>();
		for (String key : newKeys) {
			 newRecords.addAll(objectRecordDAO.getBatch(key));
		}
		return compareRecords(newRecords, expectedRecords);
	}

	private boolean compareRecords(List<ObjectRecord> actualRecords, List<ObjectRecord> expectedRecords) {
		for (ObjectRecord record: actualRecords) {
			record.setChangeNumber(null);
			record.setTimestamp(null);
		}
		for (ObjectRecord record : expectedRecords) {
			if (!actualRecords.contains(record)) {
				return false;
			}
		}
		return true;
	}

}
