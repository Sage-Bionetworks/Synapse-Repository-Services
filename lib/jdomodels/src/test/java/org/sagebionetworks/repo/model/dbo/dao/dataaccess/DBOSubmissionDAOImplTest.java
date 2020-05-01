package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmission;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionInfo;
import org.sagebionetworks.repo.model.dataaccess.SubmissionOrder;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
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

import com.google.common.collect.ImmutableList;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOSubmissionDAOImplTest {

	@Autowired
	UserGroupDAO userGroupDAO;

	@Autowired
	NodeDAO nodeDao;

	@Autowired
	AccessRequirementDAO accessRequirementDAO;

	@Autowired
	private ResearchProjectDAO researchProjectDao;

	@Autowired
	private RequestDAO requestDao;

	@Autowired
	private SubmissionDAO submissionDao;

	@Autowired
	private TransactionTemplate transactionTemplate;

	private UserGroup user1 = null;
	private UserGroup user2 = null;
	private Node node = null;
	private ManagedACTAccessRequirement accessRequirement = null;
	private ManagedACTAccessRequirement accessRequirement2 = null;
	private ResearchProject researchProject = null;
	private ResearchProject researchProject2 = null;
	private Request request;
	
	private List<String> dtosToDelete;
	
	private static ManagedACTAccessRequirement createAccessRequirement(String userId, String entityId) {
		ManagedACTAccessRequirement accessRequirement = new ManagedACTAccessRequirement();
		accessRequirement.setCreatedBy(userId);
		accessRequirement.setCreatedOn(new Date());
		accessRequirement.setModifiedBy(userId);
		accessRequirement.setModifiedOn(new Date());
		accessRequirement.setEtag("10");
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		RestrictableObjectDescriptor rod = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(entityId);
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod, rod}));
		return accessRequirement;
	}

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

		// create two access requirements
		accessRequirement = createAccessRequirement(user1.getId(), node.getId());
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		accessRequirement2 = createAccessRequirement(user1.getId(), node.getId());
		accessRequirement2 = accessRequirementDAO.create(accessRequirement2);
		
		// create a ResearchProject
		researchProject = ResearchProjectTestUtils.createNewDto();
		researchProject.setAccessRequirementId(accessRequirement.getId().toString());
		researchProject = researchProjectDao.create(researchProject);
		
		// create second researchProject
		researchProject2 = ResearchProjectTestUtils.createNewDto("4", "projectLead2", "institution2", "idu2");
		researchProject2.setAccessRequirementId(accessRequirement.getId().toString());
		researchProject2 = researchProjectDao.create(researchProject2);

		// create request
		request = RequestTestUtils.createNewRequest();
		request.setAccessRequirementId(accessRequirement.getId().toString());
		request.setResearchProjectId(researchProject.getId());
		AccessorChange add = new AccessorChange();
		add.setUserId(user1.getId());
		add.setType(AccessType.GAIN_ACCESS);
		request.setAccessorChanges(Arrays.asList(add));
		request = requestDao.create(request);
		
		dtosToDelete = new ArrayList<String>();
	}

	@After
	public void after() {
		for (String id: dtosToDelete) {
			submissionDao.delete(id);
		}
		if (request != null) {
			requestDao.delete(request.getId());
		}
		if (researchProject != null) {
			researchProjectDao.delete(researchProject.getId());
		}
		if (researchProject2 != null) {
			researchProjectDao.delete(researchProject2.getId());
		}
		if (accessRequirement != null) {
			accessRequirementDAO.delete(accessRequirement.getId().toString());
		}
		if (accessRequirement2 != null) {
			accessRequirementDAO.delete(accessRequirement2.getId().toString());
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

	private Submission createSubmission(){
		return createSubmission(accessRequirement, this.researchProject, System.currentTimeMillis());
	}
		
	private Submission createSubmission(AccessRequirement accessRequirement, ResearchProject researchProject, long modifiedOn){
		Submission dto = new Submission();
		dto.setAccessRequirementId(accessRequirement.getId().toString());
		dto.setRequestId(request.getId());
		AccessorChange change = new AccessorChange();
		change.setType(AccessType.GAIN_ACCESS);
		change.setUserId(user1.getId());
		dto.setAccessorChanges(Arrays.asList(change));
		dto.setAttachments(Arrays.asList("1"));
		dto.setDucFileHandleId("2");
		dto.setIrbFileHandleId("3");
		dto.setIsRenewalSubmission(false);
		dto.setSubmittedBy(user1.getId());
		dto.setSubmittedOn(new Date());
		dto.setModifiedBy(user1.getId());
		dto.setModifiedOn(new Date(modifiedOn));
		dto.setResearchProjectSnapshot(researchProject);
		dto.setState(SubmissionState.SUBMITTED);
		return dto;
	}

	@Test
	public void testCRUD() {
		final Submission dto = createSubmission();

		SubmissionStatus status = submissionDao.createSubmission(dto);
		assertNotNull(status);
		assertEquals(user1.getId(), status.getSubmittedBy());
		assertEquals(SubmissionState.SUBMITTED, status.getState());
		assertEquals(dto.getModifiedOn(), status.getModifiedOn());
		assertEquals(dto.getId(), status.getSubmissionId());
		assertNull(status.getRejectedReason());

		assertTrue(submissionDao.isAccessor(status.getSubmissionId(), user1.getId()));
		assertFalse(submissionDao.isAccessor(status.getSubmissionId(), user2.getId()));

		assertEquals(dto, submissionDao.getSubmission(dto.getId()));
		Submission locked = transactionTemplate.execute(new TransactionCallback<Submission>() {
			@Override
			public Submission doInTransaction(TransactionStatus status) {
				return submissionDao.getForUpdate(dto.getId());
			}
		});
		assertEquals(dto, locked);

		String etag = UUID.randomUUID().toString();
		Long modifiedOn = System.currentTimeMillis();
		status = submissionDao.cancel(dto.getId(), user1.getId(), modifiedOn , etag);
		assertNotNull(status);
		assertEquals(SubmissionState.CANCELLED, status.getState());
		assertEquals(modifiedOn, (Long) status.getModifiedOn().getTime());
		assertEquals(dto.getId(), status.getSubmissionId());
		assertNull(status.getRejectedReason());

		Submission cancelled = submissionDao.getSubmission(dto.getId());
		assertEquals(SubmissionState.CANCELLED, cancelled.getState());
		assertEquals(modifiedOn, (Long) cancelled.getModifiedOn().getTime());
		assertEquals(user1.getId(), cancelled.getModifiedBy());
		assertNull(cancelled.getRejectedReason());
		assertEquals(etag, cancelled.getEtag());

		assertEquals(status, submissionDao.getStatusByRequirementIdAndPrincipalId(accessRequirement.getId().toString(), user1.getId().toString()));

		etag = UUID.randomUUID().toString();
		modifiedOn = System.currentTimeMillis();
		String reason = "no reason";
		Submission updated = submissionDao.updateSubmissionStatus(dto.getId(),
				SubmissionState.REJECTED, reason, user2.getId(), modifiedOn);
		assertEquals(SubmissionState.REJECTED, updated.getState());
		assertEquals(modifiedOn, (Long) updated.getModifiedOn().getTime());
		assertEquals(user2.getId(), updated.getModifiedBy());
		assertEquals(reason, updated.getRejectedReason());

		Submission dto2 = createSubmission();
		submissionDao.createSubmission(dto2);
		assertEquals(SubmissionState.SUBMITTED, submissionDao
				.getStatusByRequirementIdAndPrincipalId(accessRequirement.getId().toString(), user1.getId().toString()).getState());

		submissionDao.delete(dto.getId());
		submissionDao.delete(dto2.getId());
	}

	@Test
	public void testHasSubmissionWithState() {

		Submission dto1 = createSubmission();
		submissionDao.createSubmission(dto1);

		assertTrue(submissionDao.hasSubmissionWithState(user1.getId(),
				accessRequirement.getId().toString(), SubmissionState.SUBMITTED));
		assertFalse(submissionDao.hasSubmissionWithState(user2.getId(),
				accessRequirement.getId().toString(), SubmissionState.SUBMITTED));
		assertFalse(submissionDao.hasSubmissionWithState(user1.getId(),
				accessRequirement.getId().toString(), SubmissionState.CANCELLED));
		assertFalse(submissionDao.hasSubmissionWithState(user1.getId(),
				accessRequirement.getId().toString(), SubmissionState.APPROVED));
		assertFalse(submissionDao.hasSubmissionWithState(user1.getId(),
				accessRequirement.getId().toString(), SubmissionState.REJECTED));

		// PLFM-4355
		submissionDao.updateSubmissionStatus(dto1.getId(), SubmissionState.APPROVED, null, user2.getId(), System.currentTimeMillis());
		Submission dto2 = createSubmission();
		submissionDao.createSubmission(dto2);
		submissionDao.updateSubmissionStatus(dto2.getId(), SubmissionState.CANCELLED, null, user1.getId(), System.currentTimeMillis());
		assertTrue(submissionDao.hasSubmissionWithState(user1.getId(),
				accessRequirement.getId().toString(), SubmissionState.APPROVED));
		assertTrue(submissionDao.hasSubmissionWithState(user1.getId(),
				accessRequirement.getId().toString(), SubmissionState.CANCELLED));

		submissionDao.delete(dto1.getId());
		submissionDao.delete(dto2.getId());
	}

	@Test
	public void testListSubmissions() {
		Submission dto1 = createSubmission();
		Submission dto2 = createSubmission();
		dtosToDelete.add( submissionDao.createSubmission(dto1).getSubmissionId() );
		dtosToDelete.add( submissionDao.createSubmission(dto2).getSubmissionId() );

		List<Submission> submissions = submissionDao.getSubmissions(accessRequirement.getId().toString(),
				SubmissionState.SUBMITTED, SubmissionOrder.CREATED_ON,
				true, 10L, 0L);
		assertNotNull(submissions);
		assertEquals(2, submissions.size());
		assertEquals(dto1, submissions.get(0));
		assertEquals(dto2, submissions.get(1));

		assertEquals(new HashSet<Submission>(submissions),
				new HashSet<Submission>(submissionDao.getSubmissions(
				accessRequirement.getId().toString(), null, null, null, 10L, 0L)));

		submissions = submissionDao.getSubmissions(accessRequirement.getId().toString(),
				SubmissionState.APPROVED, SubmissionOrder.MODIFIED_ON,
				false, 10L, 0L);
		assertNotNull(submissions);
		assertEquals(0, submissions.size());
	}
	
	private static SubmissionInfo createSubmissionInfo(ResearchProject rp, long modifiedOn) {
		SubmissionInfo result = new SubmissionInfo();
		result.setInstitution(rp.getInstitution());
		result.setIntendedDataUseStatement(rp.getIntendedDataUseStatement());
		result.setProjectLead(rp.getProjectLead());
		result.setModifiedOn(new Date(modifiedOn));
		return result;
		
	}
	
	@Test
	public void testListInfoForApprovedSubmissions() {
		// create a submission for research project 1
		long modifiedOn = System.currentTimeMillis();
		Submission dto1 = createSubmission(accessRequirement, researchProject, modifiedOn);
		dtosToDelete.add( submissionDao.createSubmission(dto1).getSubmissionId() );
		modifiedOn += 60000L;
		submissionDao.updateSubmissionStatus(dto1.getId(), SubmissionState.APPROVED, null, user1.getId(), modifiedOn);
			
		// create a submission for research project 2
		modifiedOn += 60000L;
		Submission dto2 = createSubmission(accessRequirement, researchProject2, modifiedOn);
		dtosToDelete.add( submissionDao.createSubmission(dto2).getSubmissionId() );	
		modifiedOn += 60000L;
		submissionDao.updateSubmissionStatus(dto2.getId(), SubmissionState.APPROVED, null, user1.getId(), modifiedOn);
		
		// now create another, later submission for research project 2
		modifiedOn += 60000L;
		Submission dto3 = createSubmission(accessRequirement, researchProject2, modifiedOn);
		dtosToDelete.add( submissionDao.createSubmission(dto3).getSubmissionId() );	
		modifiedOn += 60000L;
		submissionDao.updateSubmissionStatus(dto3.getId(), SubmissionState.APPROVED, null, user1.getId(), modifiedOn);
		SubmissionInfo dto3Info = createSubmissionInfo(researchProject2, modifiedOn);
		
		// now create another, later submission for research project 1
		modifiedOn += 60000L;
		Submission dto4 = createSubmission(accessRequirement, researchProject, modifiedOn);
		dtosToDelete.add( submissionDao.createSubmission(dto4).getSubmissionId() );	
		modifiedOn += 60000L;
		submissionDao.updateSubmissionStatus(dto4.getId(), SubmissionState.APPROVED, null, user1.getId(), modifiedOn);
		SubmissionInfo dto4Info = createSubmissionInfo(researchProject, modifiedOn);
		
		// create another submission for some other access requirement.  (Shouldn't see it in the results.)
		modifiedOn += 60000L;
		Submission dto5 = createSubmission(accessRequirement2, researchProject, modifiedOn);
		dtosToDelete.add( submissionDao.createSubmission(dto5).getSubmissionId() );	
		modifiedOn += 60000L;
		submissionDao.updateSubmissionStatus(dto5.getId(), SubmissionState.APPROVED, null, user1.getId(), modifiedOn);
		
		// create a submission which is NOT approved.  (Shouldn't see it in the results.)
		modifiedOn += 60000L;
		Submission dto6 = createSubmission(accessRequirement, researchProject, modifiedOn);
		dtosToDelete.add( submissionDao.createSubmission(dto6).getSubmissionId() );	
		
		// we should get back dto3 , then dto4, in that order
		List<SubmissionInfo> expected = ImmutableList.of(dto3Info, dto4Info);

		// method under test
		List<SubmissionInfo> actual = submissionDao.listInfoForApprovedSubmissions(accessRequirement.getId().toString(), 10, 0);
		
		assertEquals(expected, actual);
		
		
		// check that pagination works right:  If I get the first page of size *one*, I should just get dto3
		actual = submissionDao.listInfoForApprovedSubmissions(accessRequirement.getId().toString(), 1, 0);
		assertEquals(ImmutableList.of(dto3Info), actual);
		// If I get the second page of size *one*, I should just get dto4
		actual = submissionDao.listInfoForApprovedSubmissions(accessRequirement.getId().toString(), 1, 1);
		assertEquals(ImmutableList.of(dto4Info), actual);
	}

	@Test (expected=NotFoundException.class)
	public void testGetByIdNotFound() {
		submissionDao.getSubmission("0");
	}

	@Test
	public void testGetStatusNotFound() {
		assertNull(submissionDao.getStatusByRequirementIdAndPrincipalId(accessRequirement.getId().toString(), user1.getId().toString()));
	}

	@Test (expected = IllegalTransactionStateException.class)
	public void testGetForUpdateWithoutTransaction() {
		submissionDao.getForUpdate("0");
	}

	@Test
	public void testAddOrderByClause() {
		String query = "";
		assertEquals("case null order",
				"", DBOSubmissionDAOImpl.addOrderByClause(null, null, query));
		assertEquals("case order by created on null asc",
				" ORDER BY DATA_ACCESS_SUBMISSION.CREATED_ON",
				DBOSubmissionDAOImpl.addOrderByClause(SubmissionOrder.CREATED_ON, null, query));
		assertEquals("case order by created on asc",
				" ORDER BY DATA_ACCESS_SUBMISSION.CREATED_ON",
				DBOSubmissionDAOImpl.addOrderByClause(SubmissionOrder.CREATED_ON, true, query));
		assertEquals("case order by created on desc",
				" ORDER BY DATA_ACCESS_SUBMISSION.CREATED_ON DESC",
				DBOSubmissionDAOImpl.addOrderByClause(SubmissionOrder.CREATED_ON, false, query));
		assertEquals("case order by modified on null asc",
				" ORDER BY DATA_ACCESS_SUBMISSION_STATUS.MODIFIED_ON",
				DBOSubmissionDAOImpl.addOrderByClause(SubmissionOrder.MODIFIED_ON, null, query));
		assertEquals("case order by modified on asc",
				" ORDER BY DATA_ACCESS_SUBMISSION_STATUS.MODIFIED_ON",
				DBOSubmissionDAOImpl.addOrderByClause(SubmissionOrder.MODIFIED_ON, true, query));
		assertEquals("case order by modified on desc",
				" ORDER BY DATA_ACCESS_SUBMISSION_STATUS.MODIFIED_ON DESC",
				DBOSubmissionDAOImpl.addOrderByClause(SubmissionOrder.MODIFIED_ON, false, query));
	}

	@Test
	public void testGetOpenSubmissions() {
		List<OpenSubmission> openSubmissions = submissionDao.getOpenSubmissions(10L, 0L);
		assertNotNull(openSubmissions);
		assertTrue(openSubmissions.isEmpty());

		Submission dto1 = createSubmission();
		Submission dto2 = createSubmission();
		submissionDao.createSubmission(dto1);
		submissionDao.createSubmission(dto2);

		openSubmissions = submissionDao.getOpenSubmissions(10L, 0L);
		assertNotNull(openSubmissions);
		assertEquals(1, openSubmissions.size());
		OpenSubmission openSubmission = openSubmissions.get(0);
		assertEquals(accessRequirement.getId().toString(), openSubmission.getAccessRequirementId());
		assertEquals((Long)2L, openSubmission.getNumberOfSubmittedSubmission());

		submissionDao.cancel(dto1.getId(), user1.getId(), System.currentTimeMillis() , "etag");

		openSubmissions = submissionDao.getOpenSubmissions(10L, 0L);
		assertNotNull(openSubmissions);
		assertEquals(1, openSubmissions.size());
		openSubmission = openSubmissions.get(0);
		assertEquals(accessRequirement.getId().toString(), openSubmission.getAccessRequirementId());
		assertEquals((Long)1L, openSubmission.getNumberOfSubmittedSubmission());

		submissionDao.updateSubmissionStatus(dto2.getId(),
				SubmissionState.REJECTED, "reason", user2.getId(), System.currentTimeMillis());

		openSubmissions = submissionDao.getOpenSubmissions(10L, 0L);
		assertNotNull(openSubmissions);
		assertTrue(openSubmissions.isEmpty());

		submissionDao.delete(dto1.getId());
		submissionDao.delete(dto2.getId());
	}
}
