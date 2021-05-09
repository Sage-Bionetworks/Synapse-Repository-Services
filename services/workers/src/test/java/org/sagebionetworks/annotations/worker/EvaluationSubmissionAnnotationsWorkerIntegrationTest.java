package org.sagebionetworks.annotations.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.evaluation.dao.AnnotationsDAO;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.EvaluationSubmissionsDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationSubmissions;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.evaluation.SubmissionManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.workers.util.aws.message.QueueCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This test validates that when a SubmissionStatus is created, the message propagates to the 
 * annotations queue, is processed by the annotations worker.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EvaluationSubmissionAnnotationsWorkerIntegrationTest {

	public static final long MAX_WAIT = 60*1000; // 60 seconds	
	
	@Autowired
	private AnnotationsDAO annotationsDAO;
	
	@Autowired
	private SubmissionManager submissionManager;
    
	@Autowired
    private EvaluationDAO evaluationDAO;
	
	@Autowired
    private EvaluationSubmissionsDAO evaluationSubmissionsDAO;
	
	@Autowired
	private NodeDAO nodeDAO;
	
	@Autowired
	private SemaphoreManager semphoreManager;
	
	@Autowired
	private QueueCleaner queueCleaner;
	
	@Autowired
	private AccessControlListDAO accessControlListDAO;
	
	@Autowired
	private UserManager userManager;
	
	private String nodeId;
    private String submissionId;
    private Long userId;
    private UserInfo userInfo;
    private String evalId;
    private final String name = "test submission";
    private final Long versionNumber = 1L;
	
	@Before
	public void before() throws Exception {
		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		userInfo = userManager.getUserInfo(userId);
		semphoreManager.releaseAllLocksAsAdmin(userInfo);
		queueCleaner.purgeQueue(StackConfigurationSingleton.singleton().getQueueName("EVALUATION_SUBMISSION_UPDATE"));
		
		// create a node
  		Node node = new Node();
		node.setName(name);
		node.setCreatedByPrincipalId(userId);
		node.setModifiedByPrincipalId(userId);
		node.setCreatedOn(new Date(System.currentTimeMillis()));
		node.setModifiedOn(node.getCreatedOn());
		node.setNodeType(EntityType.project);
		node.setVersionComment("This is the first version of the first node ever!");
		node.setVersionLabel("0.0.1");
		node = nodeDAO.createNewNode(node);
		nodeId = node.getId().substring(3);// trim "syn" from node ID
    	
		// add an acl.
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(node.getId(), userInfo, new Date());
		accessControlListDAO.create(acl, ObjectType.ENTITY);
    	
    	// create an evaluation
        Evaluation evaluation = new Evaluation();
        evaluation.setId("1234");
        evaluation.setEtag("etag");
        evaluation.setName("my eval");
        evaluation.setOwnerId(userId.toString());
        evaluation.setCreatedOn(new Date());
        evaluation.setContentSource(nodeId);
        evalId = evaluationDAO.create(evaluation, userId);
        
        EvaluationSubmissions evalSubs = evaluationSubmissionsDAO.createForEvaluation(Long.parseLong(evaluation.getId()));
        assertNotNull(evalSubs.getEtag());
        assertEquals("1234", evalSubs.getEvaluationId().toString());
        
        // create a submission
        Submission submission = new Submission();
        submission.setId("5678");
        submission.setName(name);
        submission.setEntityId(nodeId);
        submission.setVersionNumber(versionNumber);
        submission.setUserId(userId.toString());
        submission.setEvaluationId(evalId);
        submission.setCreatedOn(new Date());
        submission.setEntityBundleJSON("some bundle");
        Node created = nodeDAO.getNode(nodeId);
        EntityBundle bundle = new EntityBundle();
        bundle.setFileHandles(new ArrayList<FileHandle>());
        Submission createdSub = submissionManager.createSubmission(userInfo, submission, created.getETag(), null, bundle);
        submissionId = createdSub.getId();
        
        // create a submissionstatus
        SubmissionStatus status = submissionManager.getSubmissionStatus(userInfo, submissionId);
        status.setStatus(SubmissionStatusEnum.RECEIVED);
        status.setScore(0.1);
        status.setAnnotations(createDummyAnnotations());
        submissionManager.updateSubmissionStatus(userInfo, status);
	}
	
	@After
	public void after(){
    	try {
    		nodeDAO.delete(nodeId);
    	} catch (Exception e) {};
		try {
			submissionManager.deleteSubmission(userInfo, submissionId);
		} catch (Exception e)  {};
		try {
			evaluationDAO.delete(evalId);
		} catch (Exception e) {};
	}
	
	
	@Test
	public void testRoundTrip() throws Exception {
		long submissionIdLong = Long.parseLong(submissionId);
		Annotations annos = new Annotations();

		// check that Annotations are created
		long start = System.currentTimeMillis();
		while (annos.getObjectId() == null) {
			System.out.println("Waiting for Annotations to be populated for Submission: " + submissionIdLong);
			Thread.sleep(1000);
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Timed out waiting for Annotations to be populated", elapse < MAX_WAIT);
			annos = annotationsDAO.getAnnotationsFromBlob(submissionIdLong);
		}
		assertEquals(submissionId, annos.getObjectId());
		
		// check that Annotations are deleted
		submissionManager.deleteSubmission(userInfo, submissionId);
		start = System.currentTimeMillis();
		while (annos.getObjectId() != null) {
			System.out.println("Waiting for Annotations to be deleted for Submission: " + submissionIdLong);
			Thread.sleep(1000);
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Timed out waiting for Annotations to be deleted", elapse < MAX_WAIT);
			annos = annotationsDAO.getAnnotationsFromBlob(submissionIdLong);
		}		
	}
	
	/**
	 * Create a populated Annotations object.
	 * 
	 * @return
	 */
	public static Annotations createDummyAnnotations() {
		List<StringAnnotation> stringAnnos = new ArrayList<StringAnnotation>();
		StringAnnotation sa = new StringAnnotation();
		sa.setIsPrivate(false);
		sa.setKey("string_anno");
		sa.setValue("foo ");
		stringAnnos.add(sa);
		
		StringAnnotation sa2 = new StringAnnotation();
		sa2.setIsPrivate(false);
		sa2.setKey("string_anno_null");
		sa2.setValue(null);
		stringAnnos.add(sa2);
		
		List<LongAnnotation> longAnnos = new ArrayList<LongAnnotation>();
		LongAnnotation la = new LongAnnotation();
		la.setIsPrivate(true);
		la.setKey("long_anno");
		la.setValue(10L);
		longAnnos.add(la);
		
		List<DoubleAnnotation> doubleAnnos = new ArrayList<DoubleAnnotation>();
		DoubleAnnotation da = new DoubleAnnotation();
		da.setIsPrivate(false);
		da.setKey("double_anno");
		da.setValue(0.5);
		doubleAnnos.add(da);
		
		Annotations annos = new Annotations();
		annos.setStringAnnos(stringAnnos);
		annos.setLongAnnos(longAnnos);
		annos.setDoubleAnnos(doubleAnnos);
		annos.setObjectId("1");
		annos.setScopeId("2");
		return annos;
	}
}
