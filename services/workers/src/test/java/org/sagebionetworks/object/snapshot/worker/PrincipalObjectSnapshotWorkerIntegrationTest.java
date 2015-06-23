package org.sagebionetworks.object.snapshot.worker;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class PrincipalObjectSnapshotWorkerIntegrationTest {

	private static final int TIME_OUT = 60 * 1000;

	@Autowired
	private ObjectRecordDAO objectRecordDAO;
	@Autowired
	private TeamDAO teamDAO;
	@Autowired
	private AccessControlListDAO aclDAO;
	@Autowired
	private UserGroupDAO userGroupDAO;

	private Team team;
	
	@Before
	public void setup() throws Exception {
		assertNotNull(objectRecordDAO);
		assertNotNull(teamDAO);
		assertNotNull(aclDAO);
		assertNotNull(userGroupDAO);
		
		team = new Team();
		team.setId("345");
		team.setEtag("etag");
		team.setCreatedBy("678");
		team.setCreatedOn(new Date());
	}

	@After
	public void tearDown() throws Exception {
		
	}

	@Test
	public void teamTest() {
		// create a team
		
		// add a member
		
		// add another member
		
		// remove a member
		
		// remove a team
	}
	
	@Test
	public void userProfileTest() {
		// create a user profile
		
		// update the user profile
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
		for (String key : newKeys) {
			List<ObjectRecord> newRecords = objectRecordDAO.getBatch(key);
			if (compareRecords(new HashSet<ObjectRecord>(newRecords), expectedRecords)) {
				return true;
			}
		}
		return false;
	}

	private boolean compareRecords(HashSet<ObjectRecord> actualRecords,
			List<ObjectRecord> expectedRecords) {
		for (ObjectRecord record : expectedRecords) {
			if (!actualRecords.contains(record)) {
				return false;
			}
		}
		return true;
	}

}
