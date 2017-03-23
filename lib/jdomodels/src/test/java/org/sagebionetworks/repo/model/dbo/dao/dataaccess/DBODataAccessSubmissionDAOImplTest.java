package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequest;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionOrder;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionStatus;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dbo.dao.AccessRequirementUtilsTest;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBODataAccessSubmissionDAOImplTest {

	@Autowired
	UserGroupDAO userGroupDAO;

	@Autowired
	NodeDAO nodeDao;

	@Autowired
	AccessRequirementDAO accessRequirementDAO;

	@Autowired
	private ResearchProjectDAO researchProjectDao;

	@Autowired
	private DataAccessRequestDAO dataAccessRequestDao;

	@Autowired
	private DataAccessSubmissionDAO dataAccessSubmissionDao;

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private TransactionTemplate transactionTemplate;

	private UserGroup user1 = null;
	private UserGroup user2 = null;
	private Node node = null;
	private ACTAccessRequirement accessRequirement = null;
	private ResearchProject researchProject = null;
	private DataAccessRequest request;

	@Before
	public void before() {
		// create a user
		user1 = new UserGroup();
		user1.setIsIndividual(true);
		user1.setCreationDate(new Date());
		user1.setId(userGroupDAO.create(user1).toString());

		user2 = new UserGroup();
		user2.setIsIndividual(true);
		user2.setCreationDate(new Date());
		user2.setId(userGroupDAO.create(user2).toString());

		// create a node
		node = NodeTestUtils.createNew("foo", Long.parseLong(user1.getId()));
		node.setId(nodeDao.createNew(node));

		// create an ACTAccessRequirement
		accessRequirement = new ACTAccessRequirement();
		accessRequirement.setCreatedBy(user1.getId());
		accessRequirement.setCreatedOn(new Date());
		accessRequirement.setModifiedBy(user1.getId());
		accessRequirement.setModifiedOn(new Date());
		accessRequirement.setEtag("10");
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		RestrictableObjectDescriptor rod = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId());
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod, rod}));
		accessRequirement.setConcreteType("com.sagebionetworks.repo.model.ACTAccessRequirements");
		accessRequirement = accessRequirementDAO.create(accessRequirement);

		// create a ResearchProject
		researchProject = ResearchProjectTestUtils.createNewDto();
		researchProject.setId(idGenerator.generateNewId(TYPE.RESEARCH_PROJECT_ID).toString());
		researchProject.setAccessRequirementId(accessRequirement.getId().toString());
		researchProject = researchProjectDao.create(researchProject);

		// create request
		request = DataAccessRequestTestUtils.createNewDataAccessRequest();
		request.setAccessRequirementId(accessRequirement.getId().toString());
		request.setResearchProjectId(researchProject.getId());
		request.setId(idGenerator.generateNewId(TYPE.DATA_ACCESS_REQUEST_ID).toString());
		request = dataAccessRequestDao.create(request);
	}

	@After
	public void after() {
		if (request != null) {
			dataAccessRequestDao.delete(request.getId());
		}
		if (researchProject != null) {
			researchProjectDao.delete(researchProject.getId());
		}
		if (accessRequirement != null) {
			accessRequirementDAO.delete(accessRequirement.getId().toString());
		}
		if (node != null) {
			nodeDao.delete(node.getId());
			node = null;
		}
		if (user1 != null) {
			userGroupDAO.delete(user1.getId());
		}
		if (user2 != null) {
			userGroupDAO.delete(user2.getId());
		}
	}

	private DataAccessSubmission createSubmission(){
		DataAccessSubmission dto = new DataAccessSubmission();
		dto.setId(idGenerator.generateNewId(TYPE.DATA_ACCESS_SUBMISSION_ID).toString());
		dto.setAccessRequirementId(accessRequirement.getId().toString());
		dto.setDataAccessRequestId(request.getId());
		dto.setAccessors(Arrays.asList(user1.getId()));
		dto.setAttachments(Arrays.asList("1"));
		dto.setDucFileHandleId("2");
		dto.setEtag(UUID.randomUUID().toString());
		dto.setIrbFileHandleId("3");
		dto.setIsRenewalSubmission(false);
		dto.setSubmittedBy(user1.getId());
		dto.setSubmittedOn(new Date());
		dto.setModifiedBy(user1.getId());
		dto.setModifiedOn(new Date());
		dto.setResearchProjectSnapshot(researchProject);
		dto.setState(DataAccessSubmissionState.SUBMITTED);
		return dto;
	}

	@Test
	public void testCRUD() {
		final DataAccessSubmission dto = createSubmission();

		DataAccessSubmissionStatus status = dataAccessSubmissionDao.create(dto);
		assertNotNull(status);
		assertEquals(DataAccessSubmissionState.SUBMITTED, status.getState());
		assertEquals(dto.getModifiedOn(), status.getModifiedOn());
		assertEquals(dto.getId(), status.getSubmissionId());
		assertNull(status.getRejectedReason());

		assertEquals(dto, dataAccessSubmissionDao.getSubmission(dto.getId()));
		DataAccessSubmission locked = transactionTemplate.execute(new TransactionCallback<DataAccessSubmission>() {
			@Override
			public DataAccessSubmission doInTransaction(TransactionStatus status) {
				return dataAccessSubmissionDao.getForUpdate(dto.getId());
			}
		});
		assertEquals(dto, locked);

		String etag = UUID.randomUUID().toString();
		Long modifiedOn = System.currentTimeMillis();
		status = dataAccessSubmissionDao.cancel(dto.getId(), user1.getId(), modifiedOn , etag);
		assertNotNull(status);
		assertEquals(DataAccessSubmissionState.CANCELLED, status.getState());
		assertEquals(modifiedOn, (Long) status.getModifiedOn().getTime());
		assertEquals(dto.getId(), status.getSubmissionId());
		assertNull(status.getRejectedReason());

		DataAccessSubmission cancelled = dataAccessSubmissionDao.getSubmission(dto.getId());
		assertEquals(DataAccessSubmissionState.CANCELLED, cancelled.getState());
		assertEquals(modifiedOn, (Long) cancelled.getModifiedOn().getTime());
		assertEquals(user1.getId(), cancelled.getModifiedBy());
		assertNull(cancelled.getRejectedReason());
		assertEquals(etag, cancelled.getEtag());

		assertEquals(status, dataAccessSubmissionDao.getStatus(accessRequirement.getId().toString(), user1.getId().toString()));

		etag = UUID.randomUUID().toString();
		modifiedOn = System.currentTimeMillis();
		String reason = "no reason";
		DataAccessSubmission updated = dataAccessSubmissionDao.updateStatus(dto.getId(),
				DataAccessSubmissionState.REJECTED, reason, user2.getId(), modifiedOn, etag);
		assertEquals(DataAccessSubmissionState.REJECTED, updated.getState());
		assertEquals(modifiedOn, (Long) updated.getModifiedOn().getTime());
		assertEquals(user2.getId(), updated.getModifiedBy());
		assertEquals(reason, updated.getRejectedReason());
		assertEquals(etag, updated.getEtag());

		dataAccessSubmissionDao.delete(dto.getId());
	}

	@Test
	public void testHasSubmissionWithState() {

		final DataAccessSubmission dto = createSubmission();
		dataAccessSubmissionDao.create(dto);

		assertTrue(dataAccessSubmissionDao.hasSubmissionWithState(user1.getId(),
				accessRequirement.getId().toString(), DataAccessSubmissionState.SUBMITTED));
		assertFalse(dataAccessSubmissionDao.hasSubmissionWithState(user2.getId(),
				accessRequirement.getId().toString(), DataAccessSubmissionState.SUBMITTED));
		assertFalse(dataAccessSubmissionDao.hasSubmissionWithState(user1.getId(),
				accessRequirement.getId().toString(), DataAccessSubmissionState.CANCELLED));
		assertFalse(dataAccessSubmissionDao.hasSubmissionWithState(user1.getId(),
				accessRequirement.getId().toString(), DataAccessSubmissionState.APPROVED));
		assertFalse(dataAccessSubmissionDao.hasSubmissionWithState(user1.getId(),
				accessRequirement.getId().toString(), DataAccessSubmissionState.REJECTED));

		dataAccessSubmissionDao.delete(dto.getId());
	}

	@Test
	public void testListSubmissions() {
		final DataAccessSubmission dto1 = createSubmission();
		final DataAccessSubmission dto2 = createSubmission();
		dataAccessSubmissionDao.create(dto1);
		dataAccessSubmissionDao.create(dto2);

		List<DataAccessSubmission> submissions = dataAccessSubmissionDao.getSubmissions(accessRequirement.getId().toString(),
				DataAccessSubmissionState.SUBMITTED, DataAccessSubmissionOrder.CREATED_ON,
				true, 10L, 0L);
		assertNotNull(submissions);
		assertEquals(2, submissions.size());
		assertEquals(dto1, submissions.get(0));
		assertEquals(dto2, submissions.get(1));

		assertEquals(new HashSet<DataAccessSubmission>(submissions),
				new HashSet<DataAccessSubmission>(dataAccessSubmissionDao.getSubmissions(
				accessRequirement.getId().toString(), null, null, null, 10L, 0L)));

		submissions = dataAccessSubmissionDao.getSubmissions(accessRequirement.getId().toString(),
				DataAccessSubmissionState.APPROVED, DataAccessSubmissionOrder.MODIFIED_ON,
				false, 10L, 0L);
		assertNotNull(submissions);
		assertEquals(0, submissions.size());

		dataAccessSubmissionDao.delete(dto1.getId());
		dataAccessSubmissionDao.delete(dto2.getId());
	}

	@Test (expected=NotFoundException.class)
	public void testGetByIdNotFound() {
		dataAccessSubmissionDao.getSubmission(idGenerator.generateNewId(TYPE.DATA_ACCESS_SUBMISSION_ID).toString());
	}

	@Test (expected=NotFoundException.class)
	public void testGetStatusNotFound() {
		dataAccessSubmissionDao.getStatus(accessRequirement.getId().toString(), user1.getId().toString());
	}

	@Test (expected = IllegalTransactionStateException.class)
	public void testGetForUpdateWithoutTransaction() {
		dataAccessSubmissionDao.getForUpdate(idGenerator.generateNewId(TYPE.DATA_ACCESS_SUBMISSION_ID).toString());
	}
}
