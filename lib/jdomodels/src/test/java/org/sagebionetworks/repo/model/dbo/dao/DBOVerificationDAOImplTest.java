package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.verification.AttachmentMetadata;
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

	@Autowired
	private IdGenerator idGenerator;
	
	// take advantage of these user IDs which are always present
	private static final String USER_1_ID = "1";
	private static final String USER_2_ID = "273950";
	
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
				verificationDao.deleteVerificationSubmission(Long.parseLong(id));
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
		dto.setCreatedOn(new Date());
		try {
			Thread.sleep(2L); // make sure time stamps are unique
		} catch (InterruptedException e) {
			// continue;
		}
		dto.setCompany(COMPANY);
		dto.setEmails(EMAILS);
		if (fileHandleIds!=null) {
			List<AttachmentMetadata> attachments = new ArrayList<AttachmentMetadata>();
			for (String fileHandleId : fileHandleIds) {
				AttachmentMetadata attachmentMetadata = new AttachmentMetadata();
				attachmentMetadata.setId(fileHandleId);
				attachments.add(attachmentMetadata);
			}
			dto.setAttachments(attachments);
		}
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
		fh.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		fh.setEtag(UUID.randomUUID().toString());
		fh = (S3FileHandle) fileHandleDao.createFile(fh);
		fhsToDelete.add(fh.getId());
		return fh;
	}

	@Test
	public void testCreate() throws Exception {
		FileHandle fh1 = createFileHandle(USER_1_ID);
		FileHandle fh2 = createFileHandle(USER_1_ID);
		List<String> fileHandleIds =Arrays.asList(fh1.getId(), fh2.getId());
		VerificationSubmission dto = newVerificationSubmission(USER_1_ID, fileHandleIds);
		VerificationSubmission created = verificationDao.createVerificationSubmission(dto);
		assertNotNull(created.getId());
		vsToDelete.add(created.getId());
		List<VerificationState> states = created.getStateHistory();
		assertEquals(1, states.size());
		VerificationState state = states.get(0);
		assertEquals(USER_1_ID, state.getCreatedBy());
		assertNotNull(state.getCreatedOn());
		assertEquals(VerificationStateEnum.SUBMITTED, state.getState());
		// now 'null out' the history.  it should match the submitted object
		created.setStateHistory(null);
		assertEquals(dto, created);
	}
	
	@Test
	public void testCreateNoFiles() throws Exception {
		VerificationSubmission dto = newVerificationSubmission(USER_1_ID, null);
		VerificationSubmission created = verificationDao.createVerificationSubmission(dto);
		assertNotNull(created.getId());
		vsToDelete.add(created.getId());
		created.setStateHistory(null);
		// now 'null out' the history.  it should match the submitted object
		assertEquals(dto, created);
	}
	
	@Test
	public void testDelete() throws Exception {
		VerificationSubmission dto = newVerificationSubmission(USER_1_ID, null);
		VerificationSubmission created = verificationDao.createVerificationSubmission(dto);
		assertNotNull(created.getId());
		vsToDelete.add(created.getId());
		assertEquals(1, verificationDao.countVerificationSubmissions(null, null));
		verificationDao.deleteVerificationSubmission(Long.parseLong(created.getId()));
		assertEquals(0, verificationDao.countVerificationSubmissions(null, null));
	}
	
	@Test
	public void testAppendVerificationSubmissionState() throws Exception {
		// now create a verification submission for User-1 but with another state
		VerificationSubmission dto = newVerificationSubmission(USER_1_ID, null);
		VerificationSubmission rejected = verificationDao.createVerificationSubmission(dto);
		vsToDelete.add(rejected.getId());
		List<VerificationSubmission> list = verificationDao.listVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.REJECTED), 
				Long.parseLong(USER_1_ID), 1, 0);
		// initally there are no rejected submissions for this user
		assertEquals(0, list.size());
		
		// now update the state
		VerificationState newState = new VerificationState();
		newState.setState(VerificationStateEnum.REJECTED);
		newState.setCreatedBy(USER_1_ID);
		newState.setCreatedOn(new Date());
		newState.setReason("your submission is invalid");
		verificationDao.appendVerificationSubmissionState(Long.parseLong(rejected.getId()), newState);
		list = verificationDao.listVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.REJECTED), 
				Long.parseLong(USER_1_ID), 1, 0);
		assertEquals(1, list.size());
		VerificationSubmission retrieved = list.get(0);
		assertEquals(rejected.getId(), retrieved.getId());
		List<VerificationState> stateHistory = retrieved.getStateHistory();
		assertEquals(VerificationStateEnum.SUBMITTED, stateHistory.get(0).getState());
		// check that the second (current) state is the one we set
		assertEquals(newState, stateHistory.get(1));
	}
	
	@Test
	public void testListVerifications() throws Exception {
		FileHandle fh1 = createFileHandle(USER_1_ID);
		FileHandle fh2 = createFileHandle(USER_1_ID);
		List<String> fileHandleIds =Arrays.asList(fh1.getId(), fh2.getId());
		VerificationSubmission dto = newVerificationSubmission(USER_1_ID, fileHandleIds);
		VerificationSubmission created = verificationDao.createVerificationSubmission(dto);
		vsToDelete.add(created.getId());
		
		// get all objects in the system
		List<VerificationSubmission> list = verificationDao.listVerificationSubmissions(null, null, 10, 0);
		assertEquals(1, list.size());
		assertEquals(created, list.get(0));
		assertEquals(1, verificationDao.countVerificationSubmissions(null, null));
		
		// get all the objects for this user
		list = verificationDao.listVerificationSubmissions(null, Long.parseLong(USER_1_ID), 10, 0);
		assertEquals(1, list.size());
		assertEquals(created, list.get(0));
		assertEquals(1, verificationDao.countVerificationSubmissions(null,  Long.parseLong(USER_1_ID)));
		
		// get all the objects for this state
		list = verificationDao.listVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.SUBMITTED), null, 10, 0);
		assertEquals(1, list.size());
		assertEquals(created, list.get(0));
		assertEquals(1, verificationDao.countVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.SUBMITTED), null));
		
		// get all the objects for this state and user
		list = verificationDao.listVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.SUBMITTED), Long.parseLong(USER_1_ID), 10, 0);
		assertEquals(1, list.size());
		assertEquals(created, list.get(0));
		assertEquals(1, verificationDao.countVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.SUBMITTED), Long.parseLong(USER_1_ID)));
		
		// you can give several states to match
		list = verificationDao.listVerificationSubmissions(
				Arrays.asList(VerificationStateEnum.SUBMITTED, VerificationStateEnum.REJECTED), 
				Long.parseLong(USER_1_ID), 10, 0);
		assertEquals(1, list.size());
		assertEquals(created, list.get(0));
		assertEquals(1, verificationDao.countVerificationSubmissions(
				Arrays.asList(VerificationStateEnum.SUBMITTED, VerificationStateEnum.REJECTED), 
				Long.parseLong(USER_1_ID)));
		
		// no objects for another user
		assertTrue(verificationDao.listVerificationSubmissions(null, Long.parseLong(USER_2_ID), 10, 0).isEmpty());
		assertEquals(0, verificationDao.countVerificationSubmissions(null,  Long.parseLong(USER_2_ID)));
		
		// no objects in another state
		assertTrue(verificationDao.listVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.APPROVED), null, 10, 0).isEmpty());
		assertEquals(0, verificationDao.countVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.APPROVED),  null));

		// no objects in another state for another user
		assertTrue(verificationDao.listVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.APPROVED), Long.parseLong(USER_2_ID), 10, 0).isEmpty());
		assertEquals(0, verificationDao.countVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.APPROVED),  Long.parseLong(USER_2_ID)));

		// make sure limit and offset are wired up right
		assertTrue(verificationDao.listVerificationSubmissions(null, null, 10, 1).isEmpty());
		assertTrue(verificationDao.listVerificationSubmissions(null, null, 0, 0).isEmpty());
	}

	
	@Test
	public void testListVerificationsWithFiltering() throws Exception {
		FileHandle fh1 = createFileHandle(USER_1_ID);
		FileHandle fh2 = createFileHandle(USER_1_ID);
		List<String> fileHandleIds =Arrays.asList(fh1.getId(), fh2.getId());
		VerificationSubmission dto = newVerificationSubmission(USER_1_ID, fileHandleIds);
		VerificationSubmission created = verificationDao.createVerificationSubmission(dto);
		vsToDelete.add(created.getId());
		
		// now create a verification submission for another user
		fh1 = createFileHandle(USER_2_ID);
		fileHandleIds = Collections.singletonList(fh1.getId());
		dto = newVerificationSubmission(USER_2_ID, fileHandleIds);
		VerificationSubmission createdForOtherUser = verificationDao.createVerificationSubmission(dto);
		vsToDelete.add(createdForOtherUser.getId());
		
		// now create a verification submission for User-1 but with another state
		dto = newVerificationSubmission(USER_1_ID, null);
		VerificationSubmission rejected = verificationDao.createVerificationSubmission(dto);
		vsToDelete.add(rejected.getId());
		VerificationState newState = new VerificationState();
		newState.setState(VerificationStateEnum.REJECTED);
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
		assertEquals(3, verificationDao.countVerificationSubmissions(null,  null));
		
		// get all the objects for this user
		list = verificationDao.listVerificationSubmissions(null, Long.parseLong(USER_1_ID), 10, 0);
		assertEquals(2, list.size());
		assertTrue(list.contains(created));
		assertTrue(list.contains(rejected));
		assertEquals(2, verificationDao.countVerificationSubmissions(null,  Long.parseLong(USER_1_ID)));
		
		// get all the objects for this state
		list = verificationDao.listVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.SUBMITTED), null, 10, 0);
		assertEquals(2, list.size());
		assertTrue(list.contains(created));
		assertTrue(list.contains(createdForOtherUser));
		assertEquals(2, verificationDao.countVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.SUBMITTED), null));
		
		// get all the objects for this state and user
		list = verificationDao.listVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.SUBMITTED), Long.parseLong(USER_1_ID), 10, 0);
		assertEquals(1, list.size());
		assertEquals(created, list.get(0));
		assertEquals(1, verificationDao.countVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.SUBMITTED), Long.parseLong(USER_1_ID)));
		
		// the other state
		list = verificationDao.listVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.REJECTED), Long.parseLong(USER_1_ID), 10, 0);
		assertEquals(1, list.size());
		assertEquals(rejected, list.get(0));
		assertEquals(1, verificationDao.countVerificationSubmissions(
				Collections.singletonList(VerificationStateEnum.REJECTED), Long.parseLong(USER_1_ID)));
		
		// searching for two states gives us both results
		list = verificationDao.listVerificationSubmissions(
				Arrays.asList(VerificationStateEnum.SUBMITTED, VerificationStateEnum.REJECTED), 
				Long.parseLong(USER_1_ID), 10, 0);
		assertEquals(2, list.size());
		assertTrue(list.contains(created));
		assertTrue(list.contains(rejected));
		assertEquals(2, verificationDao.countVerificationSubmissions(
				Arrays.asList(VerificationStateEnum.SUBMITTED, VerificationStateEnum.REJECTED), 
				Long.parseLong(USER_1_ID)));
	}
	
	@Test
	public void testListFileHandleIdsInVerificationSubmission() throws Exception {
		FileHandle fh1 = createFileHandle(USER_1_ID);
		FileHandle fh2 = createFileHandle(USER_1_ID);
		List<String> fileHandleIds =Arrays.asList(fh1.getId(), fh2.getId());
		VerificationSubmission dto = newVerificationSubmission(USER_1_ID, fileHandleIds);
		VerificationSubmission created = verificationDao.createVerificationSubmission(dto);
		assertNotNull(created.getId());
		vsToDelete.add(created.getId());
		
		long longId = Long.parseLong(created.getId());
		
		// make sure the file handle IDs appear in the query results
		List<Long> retrieved = verificationDao.listFileHandleIds(longId);
		assertEquals(2, retrieved.size());
		assertTrue(retrieved.contains(Long.parseLong(fh1.getId())));
		assertTrue(retrieved.contains(Long.parseLong(fh2.getId())));
		
		// no file handles for this ID
		assertTrue(verificationDao.listFileHandleIds(longId*13).isEmpty());
	}
	
	@Test
	public void testGetCurrentVerificationSubmissionForUser() {
		long user1long = Long.parseLong(USER_1_ID);
		// when there is no verification submission we should get null
		assertNull(verificationDao.getCurrentVerificationSubmissionForUser(user1long));

		VerificationSubmission dto = newVerificationSubmission(USER_1_ID, null);
		VerificationSubmission created = verificationDao.createVerificationSubmission(dto);
		vsToDelete.add(created.getId());
		
		VerificationSubmission current = verificationDao.getCurrentVerificationSubmissionForUser(user1long);
		assertEquals(created, current);
		
		dto = newVerificationSubmission(USER_1_ID, null); // this one will have a later time stamp
		VerificationSubmission created2 = verificationDao.createVerificationSubmission(dto);
		vsToDelete.add(created2.getId());
		// the new one is different!
		assertFalse(created.getId().equals(created2.getId()));

		current = verificationDao.getCurrentVerificationSubmissionForUser(user1long);
		assertEquals(created2, current);
	}
	
	@Test
	public void testGetVerificationState() {
		VerificationSubmission dto = newVerificationSubmission(USER_1_ID, null);
		VerificationSubmission created = verificationDao.createVerificationSubmission(dto);
		vsToDelete.add(created.getId());
		long createdIdLong = Long.parseLong(created.getId());
		assertEquals(VerificationStateEnum.SUBMITTED, verificationDao.getVerificationState(createdIdLong));

		VerificationState newState = new VerificationState();
		newState.setCreatedBy(USER_2_ID);
		newState.setCreatedOn(new Date());
		newState.setState(VerificationStateEnum.REJECTED);
		verificationDao.appendVerificationSubmissionState(createdIdLong, newState);
		
		assertEquals(VerificationStateEnum.REJECTED, verificationDao.getVerificationState(createdIdLong));

		// if we make a new one, there's no confusion about which state we're checking...
		dto = newVerificationSubmission(USER_1_ID, null);
		VerificationSubmission created2 = verificationDao.createVerificationSubmission(dto);
		vsToDelete.add(created2.getId());
		
		// ...still get the correct status, that of the first submission
		assertEquals(VerificationStateEnum.REJECTED, verificationDao.getVerificationState(createdIdLong));	
	}
	
	@Test
	public void testGetVerificationSubmitter() {
		VerificationSubmission dto = newVerificationSubmission(USER_1_ID, null);
		VerificationSubmission created = verificationDao.createVerificationSubmission(dto);
		vsToDelete.add(created.getId());
		long createdIdLong = Long.parseLong(created.getId());
		long user1Long = Long.parseLong(USER_1_ID);
		assertEquals(user1Long, verificationDao.getVerificationSubmitter(createdIdLong));

		// if we make a new one, there's no confusion about which object we're checking...
		dto = newVerificationSubmission(USER_2_ID, null);
		VerificationSubmission created2 = verificationDao.createVerificationSubmission(dto);
		vsToDelete.add(created2.getId());
		
		// ...still get the correct submitter, that of the first submission
		assertEquals(user1Long, verificationDao.getVerificationSubmitter(createdIdLong));
	}

	@Test
	public void testHaveValidatedProfiles() {
		assertFalse(verificationDao.haveValidatedProfiles(null));
		HashSet<String> userIds = new HashSet<String>();
		assertFalse(verificationDao.haveValidatedProfiles(userIds));

		VerificationSubmission dto = newVerificationSubmission(USER_1_ID, null);
		VerificationSubmission created = verificationDao.createVerificationSubmission(dto);
		vsToDelete.add(created.getId());
		userIds.add(USER_1_ID);
		assertFalse(verificationDao.haveValidatedProfiles(userIds));
		long createdIdLong = Long.parseLong(created.getId());

		VerificationState newState = new VerificationState();
		newState.setCreatedBy(USER_2_ID);
		newState.setCreatedOn(new Date());
		newState.setState(VerificationStateEnum.REJECTED);
		verificationDao.appendVerificationSubmissionState(createdIdLong, newState);
		assertFalse(verificationDao.haveValidatedProfiles(userIds));

		newState = new VerificationState();
		newState.setCreatedBy(USER_2_ID);
		newState.setCreatedOn(new Date());
		newState.setState(VerificationStateEnum.APPROVED);
		verificationDao.appendVerificationSubmissionState(createdIdLong, newState);
		assertTrue(verificationDao.haveValidatedProfiles(userIds));

		userIds.add(USER_2_ID);
		assertFalse(verificationDao.haveValidatedProfiles(userIds));
	}
}
