package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.PermissionDao;
import org.sagebionetworks.repo.model.evaluation.EvaluationDAO;
import org.sagebionetworks.repo.model.evaluation.SubmissionDAO;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class PermissionDaoImplTest {
	@Autowired
	private PermissionDao permissionDao;

	@Autowired
	private SubmissionDAO submissionDAO;

	@Autowired
	private EvaluationDAO evaluationDAO;

	@Autowired
	private NodeDAO nodeDAO;

	@Autowired
	private FileHandleDao fileHandleDAO;

	@Autowired
	AccessControlListDAO aclDAO;

	private static final String SUBMISSION_ID = "206";
	private static final String SUBMISSION_NAME = "test submission";
	private static final Long VERSION_NUMBER = 1L;
	private static final long CREATION_TIME_STAMP = System.currentTimeMillis();

	private String nodeId;
	private String userId;
	private Submission submission;

	private String evalId;
	private String fileHandleId;

	private static Random random = new Random();

	private static Submission newSubmission(String submissionId, String userId, Date createdDate, String evalId, String nodeId) {
		Submission submission = new Submission();
		submission.setCreatedOn(createdDate);
		submission.setId(submissionId);
		submission.setName(SUBMISSION_NAME+"_"+random.nextInt());
		submission.setEvaluationId(evalId);
		submission.setEntityId(nodeId);
		submission.setVersionNumber(VERSION_NUMBER);
		submission.setUserId(userId);
		submission.setSubmitterAlias("Team Awesome_"+random.nextInt());
		submission.setEntityBundleJSON("some bundle"+random.nextInt());
		return submission;
	}

	@Before
	public void setUp() throws DatastoreException, InvalidModelException, NotFoundException {
		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();

		// create a file handle
		PreviewFileHandle meta = new PreviewFileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy("" + userId);
		meta.setFileName("preview.jpg");
		fileHandleId = fileHandleDAO.createFile(meta).getId();

		// create a node
		Node toCreate = NodeTestUtils.createNew(SUBMISSION_NAME, Long.parseLong(userId));
		toCreate.setVersionComment("This is the first version of the first node ever!");
		toCreate.setVersionLabel("0.0.1");
		toCreate.setFileHandleId(fileHandleId);
		nodeId = nodeDAO.createNew(toCreate);

		// create an Evaluation
		Evaluation evaluation = new Evaluation();
		evaluation.setId("1234");
		evaluation.setEtag("etag");
		evaluation.setName("name");
		evaluation.setOwnerId(userId);
		evaluation.setCreatedOn(new Date());
		evaluation.setContentSource(nodeId);
		evaluation.setStatus(EvaluationStatus.PLANNED);
		evalId = evaluationDAO.create(evaluation, Long.parseLong(userId));
		
		AccessControlList acl = new AccessControlList();
		acl.setId(evalId);
		acl.setCreationDate(new Date(System.currentTimeMillis()));
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(Long.parseLong(userId));
		ra.setAccessType(new HashSet<ACCESS_TYPE>(
				Arrays.asList(new ACCESS_TYPE[]{
						ACCESS_TYPE.READ_PRIVATE_SUBMISSION
				})));
		ras.add(ra);
		acl.setResourceAccess(ras);
		aclDAO.create(acl, ObjectType.EVALUATION);

		// Initialize Submissions
		// submission has no team and no contributors
		submission = newSubmission(SUBMISSION_ID, userId, new Date(CREATION_TIME_STAMP), evalId, nodeId);
		submissionDAO.create(submission);
	}

	@After
	public void tearDown() throws DatastoreException, NotFoundException  {
		for (String id : new String[]{SUBMISSION_ID}) {
			try {
				submissionDAO.delete(id);
			} catch (NotFoundException e)  {};
		}

		try {
			aclDAO.delete(evalId, ObjectType.EVALUATION);
		} catch (NotFoundException e) {};

		try {
			evaluationDAO.delete(evalId);
		} catch (NotFoundException e) {};

		try {
			nodeDAO.delete(nodeId);
		} catch (NotFoundException e) {};
		fileHandleDAO.delete(fileHandleId);
	}

	@Test
	public void testIsEntityInEvaluationWithAccessHappyCase() {
		List<Long> principalIds = Collections.singletonList(Long.parseLong(userId));
		boolean b = permissionDao.isEntityInEvaluationWithAccess(
				nodeId, principalIds, ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
		assertTrue(b);
	}

	@Test
	public void testIsEntityInEvaluationWithAccessWrongPrincipal() {
		List<Long> principalIds = Collections.singletonList(99999L);
		boolean b = permissionDao.isEntityInEvaluationWithAccess(
				nodeId, principalIds, ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
		assertFalse(b);
	}

	@Test
	public void testIsEntityInEvaluationWithAccessWrongAccessType() {
		List<Long> principalIds = Collections.singletonList(Long.parseLong(userId));
		boolean b = permissionDao.isEntityInEvaluationWithAccess(
				nodeId, principalIds, ACCESS_TYPE.READ);
		assertFalse(b);
	}

	@Test
	public void testIsEntityInEvaluationWithAccessNoPrincipals() {
		List<Long> principalIds = new ArrayList<Long>();
		boolean b = permissionDao.isEntityInEvaluationWithAccess(
				nodeId, principalIds, ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
		assertFalse(b);
	}

	@Test
	public void testIsEntityInEvaluationWithAccessWrongEntity() {
		List<Long> principalIds = Collections.singletonList(Long.parseLong(userId));
		boolean b = permissionDao.isEntityInEvaluationWithAccess(
				"syn99999", principalIds, ACCESS_TYPE.READ);
		assertFalse(b);
	}


}
