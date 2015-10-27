package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOVerificationDAOImplTest {
	
	@Autowired
	private VerificationDAO verificationDao;
	
	@Autowired
	private FileHandleDao fileHandleDao;
	
	// take advantage of these user IDs which are always present
	private static final String USER_1_ID = "1";
	private static final String USER_2_ID = "273950";
	
	private static final Date CREATED_ON = new Date();
	private static final String COMPANY = "company";
	private static final List<String> EMAILS = Arrays.asList("email1@foo.com", "email2@foo.com");
	private static final String FIRST_NAME = "fname";
	private static final String LAST_NAME = "lname";
	private static final String LOCATION = "location";
	private static final String ORCID = "http://orcid.org/0000-1111-2222-3333";
	
	private List<String> vsToDelete;
	private List<String> fhsToDelete;
	
	
	@Before
	public void before() throws Exception {
		vsToDelete = new ArrayList<String>();
		fhsToDelete = new ArrayList<String>();
	}
	
	@After
	public void after() throws Exception {
		for (String id : vsToDelete) {
			try {
				verificationDao.deleteVerificationSubmission(id);
			} catch (NotFoundException e) {
				// continue
			}
		}
		for (String id : fhsToDelete) {
			try {
				fileHandleDao.delete(id);
			} catch (NotFoundException e) {
				// continue
			}
		}
	}
	
	private static VerificationSubmission newVerificationSubmission(String createdBy, List<String> fileHandleIds) {
		VerificationSubmission dto = new VerificationSubmission();
		dto.setCreatedBy(createdBy);
		dto.setCreatedOn(CREATED_ON);
		dto.setCompany(COMPANY);
		dto.setEmails(EMAILS);
		dto.setFiles(fileHandleIds);
		dto.setFirstName(FIRST_NAME);
		dto.setLastName(LAST_NAME);
		dto.setLocation(LOCATION);
		dto.setOrcid(ORCID);
		// note, we don't set stateHistory
		return dto;
	}
	
	private FileHandle createFileHandle(String createdBy) throws Exception {
		S3FileHandle fh = new S3FileHandle();
		fh.setFileName("foo");
		fh.setCreatedBy(createdBy);
		fh.setBucketName(UUID.randomUUID().toString());
		fh.setKey(UUID.randomUUID().toString());
		fh = fileHandleDao.createFile(fh);
		fhsToDelete.add(fh.getId());
		return fh;
	}

	@Test
	public void testCreate() throws Exception {
		FileHandle fh = createFileHandle(USER_1_ID);
		List<String> fileHandleIds = Collections.singletonList(fh.getId());
		VerificationSubmission dto = newVerificationSubmission(USER_1_ID, fileHandleIds);
		VerificationSubmission created = verificationDao.createVerificationSubmission(dto);
		assertNotNull(created.getId());
		vsToDelete.add(created.getId());
		List<VerificationState> states = created.getStateHistory();
		assertEquals(1, states.size());
		VerificationState state = states.get(0);
		assertEquals(USER_1_ID, state.getCreatedBy());
		assertEquals(CREATED_ON, state.getCreatedOn());
		assertEquals(VerificationStateEnum.submitted, state.getState());
		// now 'null out' the history.  it should match the submitted object
		created.setStateHistory(null);
		assertEquals(dto, created);
	}
	
	@Test
	public void testListVerifications() throws Exception {
		FileHandle fh = createFileHandle(USER_1_ID);
		List<String> fileHandleIds = Collections.singletonList(fh.getId());
		VerificationSubmission dto = newVerificationSubmission(USER_1_ID, fileHandleIds);
		VerificationSubmission created = verificationDao.createVerificationSubmission(dto);
		vsToDelete.add(created.getId());
		
		// get all objects in the system
		List<VerificationSubmission> list = verificationDao.listVerificationSubmissions(null, null, 10, 0);
		assertEquals(1, list.size());
		assertEquals(created, list.get(0));
		
		// get all the objects for this user
		list = verificationDao.listVerificationSubmissions(null, Long.parseLong(USER_1_ID), 10, 0);
		assertEquals(1, list.size());
		assertEquals(created, list.get(0));
		
		// get all the objects for this state
		list = verificationDao.listVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.submitted), null, 10, 0);
		assertEquals(1, list.size());
		assertEquals(created, list.get(0));
		
		// get all the objects for this state and user
		list = verificationDao.listVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.submitted), Long.parseLong(USER_1_ID), 10, 0);
		assertEquals(1, list.size());
		assertEquals(created, list.get(0));
		
		// you can give several states to match
		list = verificationDao.listVerificationSubmissions(
				Arrays.asList(VerificationStateEnum.submitted, VerificationStateEnum.rejected), 
				Long.parseLong(USER_1_ID), 10, 0);
		assertEquals(1, list.size());
		assertEquals(created, list.get(0));
		
		// no objects for another user
		assertTrue(verificationDao.listVerificationSubmissions(null, Long.parseLong(USER_2_ID), 10, 0).isEmpty());
		
		// no objects in another state
		assertTrue(verificationDao.listVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.approved), null, 10, 0).isEmpty());

		// no objects in another state for another user
		assertTrue(verificationDao.listVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.approved), Long.parseLong(USER_2_ID), 10, 0).isEmpty());

		// make sure limit and offset are wired up right
		assertTrue(verificationDao.listVerificationSubmissions(null, null, 10, 1).isEmpty());
		assertTrue(verificationDao.listVerificationSubmissions(null, null, 0, 0).isEmpty());
	}

	
	@Test
	public void testListVerificationsFilterOther() throws Exception {
		FileHandle fh = createFileHandle(USER_1_ID);
		List<String> fileHandleIds = Collections.singletonList(fh.getId());
		VerificationSubmission dto = newVerificationSubmission(USER_1_ID, fileHandleIds);
		VerificationSubmission created = verificationDao.createVerificationSubmission(dto);
		vsToDelete.add(created.getId());
		
		// now create a verification submission for another user
		fh = createFileHandle(USER_2_ID);
		fileHandleIds = Collections.singletonList(fh.getId());
		dto = newVerificationSubmission(USER_2_ID, fileHandleIds);
		VerificationSubmission createdForOtherUser = verificationDao.createVerificationSubmission(dto);
		vsToDelete.add(createdForOtherUser.getId());
		
		// now create a verification submission for User-1 but with another state
		fh = createFileHandle(USER_1_ID);
		fileHandleIds = Collections.singletonList(fh.getId());
		dto = newVerificationSubmission(USER_1_ID, fileHandleIds);
		VerificationSubmission rejected = verificationDao.createVerificationSubmission(dto);
		vsToDelete.add(rejected.getId());
		VerificationState newState = new VerificationState();
		newState.setState(VerificationStateEnum.rejected);
		newState.setCreatedBy(USER_1_ID);
		newState.setCreatedOn(new Date());
		newState.setReason("my dog has fleas");
		verificationDao.appendVerificationSubmissionState(Long.parseLong(rejected.getId()), newState);
		// now update the expected object
		List<VerificationState> stateHistory = new ArrayList<VerificationState>(rejected.getStateHistory());
		stateHistory.add(newState);
		rejected.setStateHistory(stateHistory);
		
		// get all objects in the system
		List<VerificationSubmission> list = verificationDao.listVerificationSubmissions(null, null, 10, 0);
		assertEquals(3, list.size());
		assertTrue(list.contains(created));
		assertTrue(list.contains(createdForOtherUser));
		assertTrue(list.contains(rejected));
		
		// get all the objects for this user
		list = verificationDao.listVerificationSubmissions(null, Long.parseLong(USER_1_ID), 10, 0);
		assertEquals(2, list.size());
		assertTrue(list.contains(created));
		assertTrue(list.contains(rejected));
		
		// get all the objects for this state
		list = verificationDao.listVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.submitted), null, 10, 0);
		assertEquals(2, list.size());
		assertTrue(list.contains(created));
		assertTrue(list.contains(createdForOtherUser));
		
		// get all the objects for this state and user
		list = verificationDao.listVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.submitted), Long.parseLong(USER_1_ID), 10, 0);
		assertEquals(1, list.size());
		assertEquals(created, list.get(0));
		
		// the other state
		list = verificationDao.listVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.rejected), Long.parseLong(USER_1_ID), 10, 0);
		assertEquals(1, list.size());
		assertEquals(rejected, list.get(0));
		
		// searching for two states gives us both results
		list = verificationDao.listVerificationSubmissions(
				Arrays.asList(VerificationStateEnum.submitted, VerificationStateEnum.rejected), 
				Long.parseLong(USER_1_ID), 10, 0);
		assertEquals(2, list.size());
		assertTrue(list.contains(created));
		assertTrue(list.contains(rejected));
	}
}
