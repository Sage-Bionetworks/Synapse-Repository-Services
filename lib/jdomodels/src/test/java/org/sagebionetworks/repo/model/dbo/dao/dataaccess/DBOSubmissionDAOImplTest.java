package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmission;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.SortDirection;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionInfo;
import org.sagebionetworks.repo.model.dataaccess.SubmissionOrder;
import org.sagebionetworks.repo.model.dataaccess.SubmissionReviewerFilterType;
import org.sagebionetworks.repo.model.dataaccess.SubmissionSearchSort;
import org.sagebionetworks.repo.model.dataaccess.SubmissionSortField;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
import org.sagebionetworks.repo.model.dbo.dao.AccessRequirementUtilsTest;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOSubmissionDAOImplTest {

	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private AccessRequirementDAO accessRequirementDAO;

	@Autowired
	private ResearchProjectDAO researchProjectDao;

	@Autowired
	private RequestDAO requestDao;

	@Autowired
	private SubmissionDAO submissionDao;

	@Autowired
	private TransactionTemplate transactionTemplate;
	
	@Autowired
	private AccessControlListDAO aclDao;

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

	@BeforeEach
	public void before() {
		aclDao.truncateAll();
		submissionDao.truncateAll();
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

	@AfterEach
	public void after() {
		aclDao.truncateAll();
		submissionDao.truncateAll();
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
	
	private Submission createSubmission(String submitter){
		return createSubmission(accessRequirement, this.researchProject, System.currentTimeMillis(), submitter);
	}
		
	private Submission createSubmission(AccessRequirement accessRequirement, ResearchProject researchProject, long modifiedOn){
		return createSubmission(accessRequirement, researchProject, modifiedOn, user1.getId());
	}
	
	private Submission createSubmission(AccessRequirement accessRequirement, ResearchProject researchProject, long modifiedOn, String submitter){
		Submission dto = new Submission();
		dto.setAccessRequirementId(accessRequirement.getId().toString());
		dto.setRequestId(request.getId());
		AccessorChange change = new AccessorChange();
		change.setType(AccessType.GAIN_ACCESS);
		change.setUserId(submitter);
		dto.setAccessorChanges(new ArrayList<>(Arrays.asList(change)));
		dto.setAttachments(Arrays.asList("1"));
		dto.setDucFileHandleId("2");
		dto.setIrbFileHandleId("3");
		dto.setIsRenewalSubmission(false);
		dto.setSubmittedBy(submitter);
		dto.setSubmittedOn(new Date());
		dto.setModifiedBy(submitter);
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
	public void testListSubmissionsNoFilters() {
		Submission dto1 = createSubmission();
		Submission dto2 = createSubmission();
		Submission dto3 = createSubmission(user2.getId());
		
		dtosToDelete.add( submissionDao.createSubmission(dto1).getSubmissionId() );
		dtosToDelete.add( submissionDao.createSubmission(dto2).getSubmissionId() );
		dtosToDelete.add( submissionDao.createSubmission(dto3).getSubmissionId() );
		
		Set<Submission> expected = ImmutableSet.of(dto1, dto2, dto3);
		
		// Call under test
		List<Submission> submissions = submissionDao.getSubmissions(accessRequirement.getId().toString(), null, null, null, null, 10L, 0L);

		assertEquals(expected, new HashSet<Submission>(submissions));

	}

	@Test
	public void testListSubmissionsByState() {
		Submission dto1 = createSubmission();
		Submission dto2 = createSubmission();
		Submission dto3 = createSubmission(user2.getId());
		
		dtosToDelete.add( submissionDao.createSubmission(dto1).getSubmissionId() );
		dtosToDelete.add( submissionDao.createSubmission(dto2).getSubmissionId() );
		dtosToDelete.add( submissionDao.createSubmission(dto3).getSubmissionId() );
		
		List<Submission> expected = Arrays.asList(dto1, dto2, dto3);

		List<Submission> submissions = submissionDao.getSubmissions(accessRequirement.getId().toString(),
				SubmissionState.SUBMITTED, null, SubmissionOrder.CREATED_ON,
				true, 10L, 0L);
		
		assertEquals(expected, submissions);

		// Call under test
		submissions = submissionDao.getSubmissions(accessRequirement.getId().toString(),
				SubmissionState.APPROVED, null, SubmissionOrder.MODIFIED_ON,
				false, 10L, 0L);
		
		assertEquals(Collections.emptyList(), submissions);
	}
	
	@Test
	public void testListSubmissionsByAccessor() {
		Submission dto1 = createSubmission();
		Submission dto2 = createSubmission();
		// Another submission whose submitter and accessor is another user
		Submission dto3 = createSubmission(user2.getId());
		
		dtosToDelete.add( submissionDao.createSubmission(dto1).getSubmissionId() );
		dtosToDelete.add( submissionDao.createSubmission(dto2).getSubmissionId() );
		dtosToDelete.add( submissionDao.createSubmission(dto3).getSubmissionId() );
		
		List<Submission> expected = Arrays.asList(dto1, dto2);
		
		// We set the accessor filter to the submitter since createSubmission() automatically adds the submitter as an accessor
		String accessorId = dto1.getSubmittedBy();

		// Call under test
		List<Submission> submissions = submissionDao.getSubmissions(accessRequirement.getId().toString(), null, accessorId, SubmissionOrder.CREATED_ON, true, 10L, 0L);
		
		assertEquals(expected, submissions);

	}
	
	@Test
	public void testListSubmissionsByAccessorWithMultipleAccessors() {
		Submission dto1 = createSubmission();
		
		Submission dto2 = createSubmission();
		dto2.getAccessorChanges().add(new AccessorChange().setType(AccessType.GAIN_ACCESS).setUserId(user2.getId()));
		
		Submission dto3 = createSubmission(user2.getId());
		dto3.getAccessorChanges().add(new AccessorChange().setType(AccessType.GAIN_ACCESS).setUserId(user1.getId()));
		
		dtosToDelete.add( submissionDao.createSubmission(dto1).getSubmissionId() );
		dtosToDelete.add( submissionDao.createSubmission(dto2).getSubmissionId() );
		dtosToDelete.add( submissionDao.createSubmission(dto3).getSubmissionId() );
		
		List<Submission> expected = Arrays.asList(dto2, dto3);
		
		String accessorId = user2.getId();

		// Call under test
		List<Submission> submissions = submissionDao.getSubmissions(accessRequirement.getId().toString(), null, accessorId, SubmissionOrder.CREATED_ON, true, 10L, 0L);
		
		assertEquals(expected, submissions);

	}
	
	@Test
	public void testListSubmissionsByAccessorAndState() {
		Submission dto1 = createSubmission();
		Submission dto2 = createSubmission();
		
		dtosToDelete.add( submissionDao.createSubmission(dto1).getSubmissionId() );
		dtosToDelete.add( submissionDao.createSubmission(dto2).getSubmissionId() );
		
		List<Submission> expected = Arrays.asList(dto1, dto2);
		
		// We set the accessor filter to the submitter since createSubmission() automatically adds the submitter as an accessor
		String accessorId = dto1.getSubmittedBy();

		// Call under test
		List<Submission> submissions = submissionDao.getSubmissions(accessRequirement.getId().toString(), SubmissionState.SUBMITTED, accessorId, SubmissionOrder.CREATED_ON, true, 10L, 0L);
		
		assertEquals(expected, submissions);
		
		submissions = submissionDao.getSubmissions(accessRequirement.getId().toString(), SubmissionState.APPROVED, dto1.getSubmittedBy(), SubmissionOrder.CREATED_ON, true, 10L, 0L);
		
		assertEquals(Collections.emptyList(), submissions);

	}
	
	private static SubmissionInfo createSubmissionInfo(ResearchProject rp, long modifiedOn, Submission submission, boolean includeAccessorChqnges) {
		SubmissionInfo result = new SubmissionInfo();
		result.setInstitution(rp.getInstitution());
		result.setIntendedDataUseStatement(rp.getIntendedDataUseStatement());
		result.setProjectLead(rp.getProjectLead());
		result.setModifiedOn(new Date(modifiedOn));
		result.setSubmittedBy(submission.getSubmittedBy());
		if (includeAccessorChqnges) {
			result.setAccessorChanges(submission.getAccessorChanges());
		}
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
		SubmissionInfo dto3InfoWithAccessorChanges = createSubmissionInfo(researchProject2, modifiedOn, dto3, true);
		SubmissionInfo dto3InfoWithoutAccessorChanges = createSubmissionInfo(researchProject2, modifiedOn, dto3, false);
		
		// now create another, later submission for research project 1
		modifiedOn += 60000L;
		Submission dto4 = createSubmission(accessRequirement, researchProject, modifiedOn);
		dtosToDelete.add( submissionDao.createSubmission(dto4).getSubmissionId() );	
		modifiedOn += 60000L;
		submissionDao.updateSubmissionStatus(dto4.getId(), SubmissionState.APPROVED, null, user1.getId(), modifiedOn);
		SubmissionInfo dto4InfoWithAccessorChanges = createSubmissionInfo(researchProject, modifiedOn, dto4, true);
		SubmissionInfo dto4InfoWithoutAccessorChanges = createSubmissionInfo(researchProject, modifiedOn, dto4, false);
		
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
		List<SubmissionInfo> expectedWithAccessorChanges = ImmutableList.of(dto3InfoWithAccessorChanges, dto4InfoWithAccessorChanges);

		// method under test
		List<SubmissionInfo> actual = submissionDao.listInfoForApprovedSubmissions(accessRequirement.getId().toString(), 10, 0, true);
		assertEquals(expectedWithAccessorChanges, actual);
		
		// method under test
		List<SubmissionInfo> actualWithoutAccessorChanges = submissionDao.listInfoForApprovedSubmissions(accessRequirement.getId().toString(), 10, 0, false);
		
		List<SubmissionInfo> expectedWithoutAccessorChanges = ImmutableList.of(dto3InfoWithoutAccessorChanges, dto4InfoWithoutAccessorChanges);
		assertEquals(expectedWithoutAccessorChanges, actualWithoutAccessorChanges);
		
	
		// check that pagination works right:  If I get the first page of size *one*, I should just get dto3
		actual = submissionDao.listInfoForApprovedSubmissions(accessRequirement.getId().toString(), 1, 0, true);
		assertEquals(ImmutableList.of(dto3InfoWithAccessorChanges), actual);
		// If I get the second page of size *one*, I should just get dto4
		actual = submissionDao.listInfoForApprovedSubmissions(accessRequirement.getId().toString(), 1, 1, true);
		assertEquals(ImmutableList.of(dto4InfoWithAccessorChanges), actual);
	}

	@Test
	public void testGetByIdNotFound() {
		assertThrows(NotFoundException.class, () -> {			
			submissionDao.getSubmission("0");
		});
	}

	@Test
	public void testGetStatusNotFound() {
		assertNull(submissionDao.getStatusByRequirementIdAndPrincipalId(accessRequirement.getId().toString(), user1.getId().toString()));
	}

	@Test
	public void testGetForUpdateWithoutTransaction() {
		assertThrows(IllegalTransactionStateException.class, () -> {			
			submissionDao.getForUpdate("0");
		});
	}

	@Test
	public void testAddOrderByClause() {
		String query = "";
		assertEquals("", DBOSubmissionDAOImpl.addOrderByClause(null, null, query), "case null order");
		assertEquals(" ORDER BY DATA_ACCESS_SUBMISSION.CREATED_ON",
				DBOSubmissionDAOImpl.addOrderByClause(SubmissionOrder.CREATED_ON, null, query), "case order by created on null asc");
		assertEquals(" ORDER BY DATA_ACCESS_SUBMISSION.CREATED_ON",
				DBOSubmissionDAOImpl.addOrderByClause(SubmissionOrder.CREATED_ON, true, query), "case order by created on asc");
		assertEquals(" ORDER BY DATA_ACCESS_SUBMISSION.CREATED_ON DESC",
				DBOSubmissionDAOImpl.addOrderByClause(SubmissionOrder.CREATED_ON, false, query), "case order by created on desc");
		assertEquals(" ORDER BY DATA_ACCESS_SUBMISSION_STATUS.MODIFIED_ON",
				DBOSubmissionDAOImpl.addOrderByClause(SubmissionOrder.MODIFIED_ON, null, query), "case order by modified on null asc");
		assertEquals(" ORDER BY DATA_ACCESS_SUBMISSION_STATUS.MODIFIED_ON",
				DBOSubmissionDAOImpl.addOrderByClause(SubmissionOrder.MODIFIED_ON, true, query), "case order by modified on asc");
		assertEquals(" ORDER BY DATA_ACCESS_SUBMISSION_STATUS.MODIFIED_ON DESC",
				DBOSubmissionDAOImpl.addOrderByClause(SubmissionOrder.MODIFIED_ON, false, query), "case order by modified on desc");
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
	
	@Test
	public void testGetSubmissionWithNotFound() {
		String message = assertThrows(NotFoundException.class, ()->{
			submissionDao.getSubmission("-123");
		}).getMessage();
		assertEquals("Submission: '-123' does not exist", message);
	}
	
	@Test
	public void testGetAccessRequirementId() {
		SubmissionStatus status = submissionDao.createSubmission(createSubmission());
		
		// Call under test
		String result = submissionDao.getAccessRequirementId(status.getSubmissionId());
		
		assertEquals(accessRequirement.getId().toString(), result);
		
		submissionDao.delete(status.getSubmissionId());
	}
	
	@Test
	public void testGetAccessRequirementIdWithNonExisting() {
		String message = assertThrows(NotFoundException.class, () -> {
			// Call under test
			submissionDao.getAccessRequirementId("-123");
		}).getMessage();
		
		assertEquals("Submission: '-123' does not exist", message);
	}
	
	@Test
	public void testSearchAllSubmissions() {
		
		// both users can review the 1st AR
		addReviewers(accessRequirement.getId(), List.of(user1.getId(), user2.getId()));
		
		String s1 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis(), user1.getId())).getSubmissionId();
		String s2 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis() + 1000, user2.getId())).getSubmissionId();
		String s3 = submissionDao.createSubmission(createSubmission(accessRequirement2, researchProject, System.currentTimeMillis() + 2000, user2.getId())).getSubmissionId();
		
		String accessorId = null;
		String requirementId = null;
		List<SubmissionSearchSort> sort = List.of(new SubmissionSearchSort().setField(SubmissionSortField.MODIFIED_ON));
		SubmissionState state = null;
		String reviewerId = null;
		SubmissionReviewerFilterType reviewerFilterType = SubmissionReviewerFilterType.ALL;
		long limit = 10;
		long offset = 0;
		
		List<String> expected = List.of(s1, s2, s3);
		
		List<String> result = submissionDao.searchAllSubmissions(reviewerFilterType, sort, accessorId, requirementId, reviewerId, state, limit, offset)
			.stream().map( s-> s.getId()).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchSubmissionsWithACTOnly() {
		
		// both users can review the 1st AR
		addReviewers(accessRequirement.getId(), List.of(user1.getId(), user2.getId()));
		
		String s1 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis(), user1.getId())).getSubmissionId();
		String s2 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis() + 1000, user2.getId())).getSubmissionId();
		String s3 = submissionDao.createSubmission(createSubmission(accessRequirement2, researchProject, System.currentTimeMillis() + 2000, user2.getId())).getSubmissionId();
		
		String accessorId = null;
		String requirementId = null;
		List<SubmissionSearchSort> sort = List.of(new SubmissionSearchSort().setField(SubmissionSortField.MODIFIED_ON));
		SubmissionState state = null;
		String reviewerId = null;
		SubmissionReviewerFilterType reviewerFilterType = SubmissionReviewerFilterType.ACT_ONLY;
		long limit = 10;
		long offset = 0;
		
		List<String> expected = List.of(s3);
		
		List<String> result = submissionDao.searchAllSubmissions(reviewerFilterType, sort, accessorId, requirementId, reviewerId, state, limit, offset)
				.stream().map( s-> s.getId()).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchSubmissionsWithDelegatedOnly() {
		
		// both users can review the 1st AR
		addReviewers(accessRequirement.getId(), List.of(user1.getId(), user2.getId()));
		
		String s1 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis(), user1.getId())).getSubmissionId();
		String s2 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis() + 1000, user2.getId())).getSubmissionId();
		String s3 = submissionDao.createSubmission(createSubmission(accessRequirement2, researchProject, System.currentTimeMillis() + 2000, user2.getId())).getSubmissionId();
		
		String accessorId = null;
		String requirementId = null;
		List<SubmissionSearchSort> sort = List.of(new SubmissionSearchSort().setField(SubmissionSortField.MODIFIED_ON));
		SubmissionState state = null;
		String reviewerId = null;
		SubmissionReviewerFilterType reviewerFilterType = SubmissionReviewerFilterType.DELEGATED_ONLY;
		long limit = 10;
		long offset = 0;
		
		List<String> expected = List.of(s1, s2);
		
		List<String> result = submissionDao.searchAllSubmissions(reviewerFilterType, sort, accessorId, requirementId, reviewerId, state, limit, offset)
				.stream().map( s-> s.getId()).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchAllSubmissionsWithReviewer() {
		
		// both users can review the 1st AR
		addReviewers(accessRequirement.getId(), List.of(user1.getId(), user2.getId()));
		
		String s1 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis(), user1.getId())).getSubmissionId();
		String s2 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis() + 1000, user2.getId())).getSubmissionId();
		String s3 = submissionDao.createSubmission(createSubmission(accessRequirement2, researchProject, System.currentTimeMillis() + 2000, user2.getId())).getSubmissionId();
		
		String accessorId = null;
		String requirementId = null;
		List<SubmissionSearchSort> sort = List.of(new SubmissionSearchSort().setField(SubmissionSortField.MODIFIED_ON));
		SubmissionState state = null;
		String reviewerId = user2.getId();
		SubmissionReviewerFilterType reviewerFilterType = SubmissionReviewerFilterType.ALL;
		long limit = 10;
		long offset = 0;
		
		List<String> expected = List.of(s1, s2);
		
		List<String> result = submissionDao.searchAllSubmissions(reviewerFilterType, sort, accessorId, requirementId, reviewerId, state, limit, offset)
				.stream().map( s-> s.getId()).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchAllSubmissionsWithACTOnlyAndReviewer() {
		
		// both users can review the 1st AR
		addReviewers(accessRequirement.getId(), List.of(user1.getId(), user2.getId()));
		
		String s1 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis(), user1.getId())).getSubmissionId();
		String s2 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis() + 1000, user2.getId())).getSubmissionId();
		String s3 = submissionDao.createSubmission(createSubmission(accessRequirement2, researchProject, System.currentTimeMillis() + 2000, user2.getId())).getSubmissionId();
		
		String accessorId = null;
		String requirementId = null;
		List<SubmissionSearchSort> sort = List.of(new SubmissionSearchSort().setField(SubmissionSortField.MODIFIED_ON));
		SubmissionState state = null;
		String reviewerId = user2.getId();
		SubmissionReviewerFilterType reviewerFilterType = SubmissionReviewerFilterType.ACT_ONLY;
		long limit = 10;
		long offset = 0;
		
		List<String> expected = Collections.emptyList();
		
		List<String> result = submissionDao.searchAllSubmissions(reviewerFilterType, sort, accessorId, requirementId, reviewerId, state, limit, offset)
				.stream().map( s-> s.getId()).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchAllSubmissionsWithDelegatedOnlyAndReviewer() {
		
		// both users can review the 1st AR
		addReviewers(accessRequirement.getId(), List.of(user1.getId(), user2.getId()));
		
		String s1 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis(), user1.getId())).getSubmissionId();
		String s2 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis() + 1000, user2.getId())).getSubmissionId();
		String s3 = submissionDao.createSubmission(createSubmission(accessRequirement2, researchProject, System.currentTimeMillis() + 2000, user2.getId())).getSubmissionId();
		
		String accessorId = null;
		String requirementId = null;
		List<SubmissionSearchSort> sort = List.of(new SubmissionSearchSort().setField(SubmissionSortField.MODIFIED_ON));
		SubmissionState state = null;
		String reviewerId = user2.getId();
		SubmissionReviewerFilterType reviewerFilterType = SubmissionReviewerFilterType.DELEGATED_ONLY;
		long limit = 10;
		long offset = 0;
		
		List<String> expected = List.of(s1, s2);
		
		List<String> result = submissionDao.searchAllSubmissions(reviewerFilterType, sort, accessorId, requirementId, reviewerId, state, limit, offset)
				.stream().map( s-> s.getId()).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchAllSubmissionsWithAccessor() {
		
		// both users can review the 1st AR
		addReviewers(accessRequirement.getId(), List.of(user1.getId(), user2.getId()));
		
		String s1 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis(), user1.getId())).getSubmissionId();
		String s2 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis() + 1000, user2.getId())).getSubmissionId();
		String s3 = submissionDao.createSubmission(createSubmission(accessRequirement2, researchProject, System.currentTimeMillis() + 2000, user2.getId())).getSubmissionId();
		
		String accessorId = user1.getId();
		String requirementId = null;
		List<SubmissionSearchSort> sort = List.of(new SubmissionSearchSort().setField(SubmissionSortField.MODIFIED_ON));
		SubmissionState state = null;
		String reviewerId = null;
		SubmissionReviewerFilterType reviewerFilterType = SubmissionReviewerFilterType.ALL;
		long limit = 10;
		long offset = 0;
		
		List<String> expected = List.of(s1);
		
		List<String> result = submissionDao.searchAllSubmissions(reviewerFilterType, sort, accessorId, requirementId, reviewerId, state, limit, offset)
				.stream().map( s-> s.getId()).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchAllSubmissionsWithRequirement() {
		
		// both users can review the 1st AR
		addReviewers(accessRequirement.getId(), List.of(user1.getId(), user2.getId()));
		
		String s1 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis(), user1.getId())).getSubmissionId();
		String s2 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis() + 1000, user2.getId())).getSubmissionId();
		String s3 = submissionDao.createSubmission(createSubmission(accessRequirement2, researchProject, System.currentTimeMillis() + 2000, user2.getId())).getSubmissionId();
		
		String accessorId = null;
		String requirementId = accessRequirement2.getId().toString();
		List<SubmissionSearchSort> sort = List.of(new SubmissionSearchSort().setField(SubmissionSortField.MODIFIED_ON));
		SubmissionState state = null;
		String reviewerId = null;
		SubmissionReviewerFilterType reviewerFilterType = SubmissionReviewerFilterType.ALL;
		long limit = 10;
		long offset = 0;
		
		List<String> expected = List.of(s3);
		
		List<String> result = submissionDao.searchAllSubmissions(reviewerFilterType, sort, accessorId, requirementId, reviewerId, state, limit, offset)
				.stream().map( s-> s.getId()).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchAllSubmissionsWithState() {
		
		// both users can review the 1st AR
		addReviewers(accessRequirement.getId(), List.of(user1.getId(), user2.getId()));
		
		String s1 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis(), user1.getId())).getSubmissionId();
		String s2 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis() + 1000, user2.getId()).setState(SubmissionState.REJECTED)).getSubmissionId();
		String s3 = submissionDao.createSubmission(createSubmission(accessRequirement2, researchProject, System.currentTimeMillis() + 2000, user2.getId())).getSubmissionId();
		
		String accessorId = null;
		String requirementId = null;
		List<SubmissionSearchSort> sort = List.of(new SubmissionSearchSort().setField(SubmissionSortField.MODIFIED_ON));
		SubmissionState state = SubmissionState.REJECTED;
		String reviewerId = null;
		SubmissionReviewerFilterType reviewerFilterType = SubmissionReviewerFilterType.ALL;
		long limit = 10;
		long offset = 0;
		
		List<String> expected = List.of(s2);
		
		List<String> result = submissionDao.searchAllSubmissions(reviewerFilterType, sort, accessorId, requirementId, reviewerId, state, limit, offset)
				.stream().map( s-> s.getId()).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchAllSubmissionsWithMultiSort() {
		
		// both users can review the 1st AR
		addReviewers(accessRequirement.getId(), List.of(user1.getId(), user2.getId()));
		
		Date createdOn = new Date();
		
		String s1 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis(), user1.getId()).setSubmittedOn(createdOn)).getSubmissionId();
		String s2 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis() + 1000, user2.getId()).setSubmittedOn(createdOn)).getSubmissionId();
		String s3 = submissionDao.createSubmission(createSubmission(accessRequirement2, researchProject, System.currentTimeMillis() + 2000, user2.getId()).setSubmittedOn(createdOn)).getSubmissionId();
		
		String accessorId = null;
		String requirementId = null;
		
		List<SubmissionSearchSort> sort = List.of(
			new SubmissionSearchSort().setField(SubmissionSortField.CREATED_ON),
			new SubmissionSearchSort().setField(SubmissionSortField.MODIFIED_ON).setDirection(SortDirection.DESC)
		);
		
		SubmissionState state = null;
		String reviewerId = null;
		SubmissionReviewerFilterType reviewerFilterType = SubmissionReviewerFilterType.ALL;
		long limit = 10;
		long offset = 0;
		
		List<String> expected = List.of(s3, s2, s1);
		
		List<String> result = submissionDao.searchAllSubmissions(reviewerFilterType, sort, accessorId, requirementId, reviewerId, state, limit, offset)
			.stream().map( s-> s.getId()).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchAllSubmissionsWithLimitOffset() {
		
		// both users can review the 1st AR
		addReviewers(accessRequirement.getId(), List.of(user1.getId(), user2.getId()));
		
		String s1 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis(), user1.getId())).getSubmissionId();
		String s2 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis() + 1000, user2.getId())).getSubmissionId();
		String s3 = submissionDao.createSubmission(createSubmission(accessRequirement2, researchProject, System.currentTimeMillis() + 2000, user2.getId())).getSubmissionId();
		
		String accessorId = null;
		String requirementId = null;
		List<SubmissionSearchSort> sort = List.of(new SubmissionSearchSort().setField(SubmissionSortField.MODIFIED_ON));
		SubmissionState state = null;
		String reviewerId = null;
		SubmissionReviewerFilterType reviewerFilterType = SubmissionReviewerFilterType.ALL;
		long limit = 2;
		long offset = 0;
		
		List<String> expected = List.of(s1, s2);
		
		List<String> result = submissionDao.searchAllSubmissions(reviewerFilterType, sort, accessorId, requirementId, reviewerId, state, limit, offset)
			.stream().map( s-> s.getId()).collect(Collectors.toList());
		
		assertEquals(expected, result);
		
		limit = 2;
		offset = 1;
		
		expected = List.of(s2, s3);
		
		result = submissionDao.searchAllSubmissions(reviewerFilterType, sort, accessorId, requirementId, reviewerId, state, limit, offset)
			.stream().map( s-> s.getId()).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchPrincipalReviewableSubmissions() {
		
		// both users can review the 1st AR
		addReviewers(accessRequirement.getId(), List.of(user1.getId(), user2.getId()));
		
		String s1 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis(), user1.getId())).getSubmissionId();
		String s2 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis() + 1000, user2.getId())).getSubmissionId();
		String s3 = submissionDao.createSubmission(createSubmission(accessRequirement2, researchProject, System.currentTimeMillis() + 2000, user2.getId())).getSubmissionId();
		
		String accessorId = null;
		String requirementId = null;
		List<SubmissionSearchSort> sort = List.of(new SubmissionSearchSort().setField(SubmissionSortField.MODIFIED_ON));
		SubmissionState state = null;
		String reviewerId = null;
		long limit = 10;
		long offset = 0;
		
		List<String> expected = List.of(s1, s2);
		
		List<String> result = submissionDao.searchPrincipalReviewableSubmissions(user1.getId(), sort, accessorId, requirementId, reviewerId, state, limit, offset)
			.stream().map( s-> s.getId()).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchPrincipalReviewableSubmissionsWithReviewer() {
		
		// both users can review the 2nd AR
		addReviewers(accessRequirement2.getId(), List.of(user1.getId(), user2.getId()));
		// The first user can review the 1st AR
		addReviewers(accessRequirement.getId(), List.of(user1.getId()));
		
		String s1 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis(), user1.getId())).getSubmissionId();
		String s2 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis() + 1000, user2.getId())).getSubmissionId();
		String s3 = submissionDao.createSubmission(createSubmission(accessRequirement2, researchProject, System.currentTimeMillis() + 2000, user2.getId())).getSubmissionId();
		
		String accessorId = null;
		String requirementId = null;
		List<SubmissionSearchSort> sort = List.of(new SubmissionSearchSort().setField(SubmissionSortField.MODIFIED_ON));
		SubmissionState state = null;
		String reviewerId = user2.getId();
		long limit = 10;
		long offset = 0;
		
		List<String> expected = List.of(s3);
		
		List<String> result = submissionDao.searchPrincipalReviewableSubmissions(user1.getId(), sort, accessorId, requirementId, reviewerId, state, limit, offset)
			.stream().map( s-> s.getId()).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchPrincipalReviewableSubmissionsWithAccessor() {
		
		// 1st user can review the 1st AR
		addReviewers(accessRequirement.getId(), List.of(user1.getId()));
		
		String s1 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis(), user1.getId())).getSubmissionId();
		String s2 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis() + 1000, user2.getId())).getSubmissionId();
		String s3 = submissionDao.createSubmission(createSubmission(accessRequirement2, researchProject, System.currentTimeMillis() + 2000, user2.getId())).getSubmissionId();
		
		String accessorId = user2.getId();
		String requirementId = null;
		List<SubmissionSearchSort> sort = List.of(new SubmissionSearchSort().setField(SubmissionSortField.MODIFIED_ON));
		SubmissionState state = null;
		String reviewerId = null;
		long limit = 10;
		long offset = 0;
		
		List<String> expected = List.of(s2);
		
		List<String> result = submissionDao.searchPrincipalReviewableSubmissions(user1.getId(), sort, accessorId, requirementId, reviewerId, state, limit, offset)
			.stream().map( s-> s.getId()).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchPrincipalReviewableSubmissionsWithRequirement() {
		
		// both users can review the 1st AR
		addReviewers(accessRequirement.getId(), List.of(user1.getId(), user2.getId()));
		// 1st user can review the 2nd AR
		addReviewers(accessRequirement2.getId(), List.of(user1.getId()));
		
		String s1 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis(), user1.getId())).getSubmissionId();
		String s2 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis() + 1000, user2.getId())).getSubmissionId();
		String s3 = submissionDao.createSubmission(createSubmission(accessRequirement2, researchProject, System.currentTimeMillis() + 2000, user2.getId())).getSubmissionId();
		
		String accessorId = null;
		String requirementId = accessRequirement2.getId().toString();
		List<SubmissionSearchSort> sort = List.of(new SubmissionSearchSort().setField(SubmissionSortField.MODIFIED_ON));
		SubmissionState state = null;
		String reviewerId = null;
		long limit = 10;
		long offset = 0;
		
		List<String> expected = List.of(s3);
		
		List<String> result = submissionDao.searchPrincipalReviewableSubmissions(user1.getId(), sort, accessorId, requirementId, reviewerId, state, limit, offset)
			.stream().map( s-> s.getId()).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchPrincipalReviewableSubmissionsWithState() {
		
		// both users can review the 1st AR
		addReviewers(accessRequirement.getId(), List.of(user1.getId(), user2.getId()));
		
		String s1 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis(), user1.getId())).getSubmissionId();
		String s2 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis() + 1000, user2.getId()).setState(SubmissionState.REJECTED)).getSubmissionId();
		String s3 = submissionDao.createSubmission(createSubmission(accessRequirement2, researchProject, System.currentTimeMillis() + 2000, user2.getId())).getSubmissionId();
		
		String accessorId = null;
		String requirementId = null;
		List<SubmissionSearchSort> sort = List.of(new SubmissionSearchSort().setField(SubmissionSortField.MODIFIED_ON));
		SubmissionState state = SubmissionState.REJECTED;
		String reviewerId = null;
		long limit = 10;
		long offset = 0;
		
		List<String> expected = List.of(s2);
		
		List<String> result = submissionDao.searchPrincipalReviewableSubmissions(user1.getId(), sort, accessorId, requirementId, reviewerId, state, limit, offset)
			.stream().map( s-> s.getId()).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchPrincipalReviewableSubmissionsWithMultiSort() {
		
		// both users can review the 1st AR
		addReviewers(accessRequirement.getId(), List.of(user1.getId(), user2.getId()));
		
		Date createdOn = new Date();
		
		String s1 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis(), user1.getId()).setSubmittedOn(createdOn)).getSubmissionId();
		String s2 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis() + 1000, user2.getId()).setSubmittedOn(createdOn)).getSubmissionId();
		String s3 = submissionDao.createSubmission(createSubmission(accessRequirement2, researchProject, System.currentTimeMillis() + 2000, user2.getId()).setSubmittedOn(createdOn)).getSubmissionId();
		
		String accessorId = null;
		String requirementId = null;
		List<SubmissionSearchSort> sort = List.of(
			new SubmissionSearchSort().setField(SubmissionSortField.CREATED_ON),
			new SubmissionSearchSort().setField(SubmissionSortField.MODIFIED_ON).setDirection(SortDirection.DESC)
		);
		SubmissionState state = null;
		String reviewerId = null;
		long limit = 10;
		long offset = 0;
		
		List<String> expected = List.of(s2, s1);
		
		List<String> result = submissionDao.searchPrincipalReviewableSubmissions(user1.getId(), sort, accessorId, requirementId, reviewerId, state, limit, offset)
			.stream().map( s-> s.getId()).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchPrincipalReviewableSubmissionsWithLimitOffset() {
		
		// both users can review the 1st AR
		addReviewers(accessRequirement.getId(), List.of(user1.getId(), user2.getId()));
				
		String s1 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis(), user1.getId())).getSubmissionId();
		String s2 = submissionDao.createSubmission(createSubmission(accessRequirement, researchProject, System.currentTimeMillis() + 1000, user2.getId())).getSubmissionId();
		String s3 = submissionDao.createSubmission(createSubmission(accessRequirement2, researchProject, System.currentTimeMillis() + 2000, user2.getId())).getSubmissionId();
		
		String accessorId = null;
		String requirementId = null;
		List<SubmissionSearchSort> sort = List.of(new SubmissionSearchSort().setField(SubmissionSortField.MODIFIED_ON));
		SubmissionState state = null;
		String reviewerId = null;
		long limit = 1;
		long offset = 0;
		
		List<String> expected = List.of(s1);
		
		List<String> result = submissionDao.searchPrincipalReviewableSubmissions(user1.getId(), sort, accessorId, requirementId, reviewerId, state, limit, offset)
			.stream().map( s-> s.getId()).collect(Collectors.toList());
		
		assertEquals(expected, result);
		
		limit = 1;
		offset = 1;
		
		expected = List.of(s2);
		
		result = submissionDao.searchPrincipalReviewableSubmissions(user1.getId(), sort, accessorId, requirementId, reviewerId, state, limit, offset)
				.stream().map( s-> s.getId()).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	private void addReviewers(Long arId, List<String> reviewerIds) {
		AccessControlList acl = new AccessControlList()
			.setId(arId.toString())
			.setCreationDate(new Date())
			.setCreatedBy(user1.getId())
			.setModifiedBy(user1.getId())
			.setModifiedOn(new Date())
			.setResourceAccess(reviewerIds.stream().map(reviewerId -> 
				new ResourceAccess().setAccessType(Set.of(ACCESS_TYPE.REVIEW_SUBMISSIONS)).setPrincipalId(Long.valueOf(reviewerId))
			).collect(Collectors.toSet()));
		
		aclDao.create(acl, ObjectType.ACCESS_REQUIREMENT);
	}
	
}
