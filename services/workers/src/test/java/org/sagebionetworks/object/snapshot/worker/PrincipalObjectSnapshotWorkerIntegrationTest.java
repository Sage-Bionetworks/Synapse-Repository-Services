package org.sagebionetworks.object.snapshot.worker;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
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
	private UserGroupDAO userGroupDAO;
	
	UserGroup ug;
	List<String> toRemove = new ArrayList<String>();
	

	@Before
	public void setup() throws Exception {
		assertNotNull(objectRecordDAO);
		assertNotNull(userGroupDAO);
		
		ug = new UserGroup();
		ug = new UserGroup();
		ug.setCreationDate(new Date());
		ug.setEtag("etag");
		ug.setId("635421");
		ug.setIsIndividual(true);
	}

	@After
	public void tearDown() throws Exception {
		for (String id : toRemove) {
			userGroupDAO.delete(id);
		}
	}
	
	@Test
	public void userGroupTest() throws Exception {
		Set<String> keys = objectRecordDAO.listAllKeys();
		
		// create a userGroup
		Long principalId = userGroupDAO.create(ug);
		toRemove.add(principalId.toString());
		UserGroup toLog = userGroupDAO.get(principalId);
		ObjectRecord expectedRecord = new ObjectRecord();
		expectedRecord.setObjectType(toLog.getClass().getSimpleName());
		expectedRecord.setJsonString(EntityFactory.createJSONStringForEntity(toLog));
		assertTrue(waitForObjects(keys, Arrays.asList(expectedRecord)));
		
		// update a userGroup
		userGroupDAO.update(toLog);
		toLog = userGroupDAO.get(principalId);
		expectedRecord = new ObjectRecord();
		expectedRecord.setObjectType(toLog.getClass().getSimpleName());
		expectedRecord.setJsonString(EntityFactory.createJSONStringForEntity(toLog));
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
		return compareRecords(new HashSet<ObjectRecord>(newRecords), expectedRecords);
	}

	private boolean compareRecords(HashSet<ObjectRecord> actualRecords,
			List<ObjectRecord> expectedRecords) {
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
