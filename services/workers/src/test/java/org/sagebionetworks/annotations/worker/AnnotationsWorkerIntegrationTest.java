package org.sagebionetworks.annotations.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.asynchronous.workers.sqs.MessageReceiver;
import org.sagebionetworks.evaluation.dao.AnnotationsDAO;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.ParticipantDAO;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.dao.SubmissionStatusDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.web.util.UserProvider;
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
public class AnnotationsWorkerIntegrationTest {

	public static final long MAX_WAIT = 30*1000; // 30 seconds	
	
	@Autowired
	AnnotationsDAO annotationsDAO;
	@Autowired
	SubmissionStatusDAO submissionStatusDAO;
    @Autowired
    SubmissionDAO submissionDAO;
    @Autowired
    ParticipantDAO participantDAO;
    @Autowired
    EvaluationDAO evaluationDAO;
	@Autowired
	NodeDAO nodeDAO;
	@Autowired
	private UserProvider userProvider;
	@Autowired
	private MessageReceiver annotationsQueueMessageReceiver;
	
	UserInfo userInfo;
	
	private String nodeId = null;
    private String submissionId = null;
    private String userId = "0";
    private String evalId;
    private String name = "test submission";
    private Long versionNumber = 1L;
	
	@Before
	public void before() throws Exception {
		// Before we start, make sure the queue is empty
		emptyQueue();
		
		// create a node
  		Node node = new Node();
		node.setName(name);
		node.setCreatedByPrincipalId(Long.parseLong(userId));
		node.setModifiedByPrincipalId(Long.parseLong(userId));
		node.setCreatedOn(new Date(System.currentTimeMillis()));
		node.setModifiedOn(node.getCreatedOn());
		node.setNodeType(EntityType.project.name());
		node.setVersionComment("This is the first version of the first node ever!");
		node.setVersionLabel("0.0.1");
    	nodeId = nodeDAO.createNew(node).substring(3); // trim "syn" from node ID
    	
    	// create an evaluation
        Evaluation evaluation = new Evaluation();
        evaluation.setId("1234");
        evaluation.setEtag("etag");
        evaluation.setName("my eval");
        evaluation.setOwnerId(userId);
        evaluation.setCreatedOn(new Date());
        evaluation.setContentSource(nodeId);
        evaluation.setStatus(EvaluationStatus.PLANNED);
        evalId = evaluationDAO.create(evaluation, Long.parseLong(userId));
        
        // create a participant
        Participant participant = new Participant();
        participant.setCreatedOn(new Date());
        participant.setUserId(userId);
        participant.setEvaluationId(evalId);
        participantDAO.create(participant);
        
        // create a submission
        Submission submission = new Submission();
        submission.setId("5678");
        submission.setName(name);
        submission.setEntityId(nodeId);
        submission.setVersionNumber(versionNumber);
        submission.setUserId(userId);
        submission.setEvaluationId(evalId);
        submission.setCreatedOn(new Date());
        submission.setEntityBundleJSON("some bundle");
        submissionId = submissionDAO.create(submission);
        
        // create a submissionstatus
        SubmissionStatus status = new SubmissionStatus();
        status.setModifiedOn(new Date());
        status.setId(submissionId);
        status.setEtag(null);
        status.setStatus(SubmissionStatusEnum.OPEN);
        status.setScore(0.1);
        status.setAnnotations(createDummyAnnotations());
        submissionStatusDAO.create(status);
	}
	
	@After
	public void after(){
    	try {
    		nodeDAO.delete(nodeId);
    	} catch (Exception e) {};
		try {
			submissionDAO.delete(submissionId);
		} catch (Exception e)  {};
		try {
			participantDAO.delete(userId, evalId);
		} catch (Exception e) {};
		try {
			evaluationDAO.delete(evalId);
		} catch (Exception e) {};
	}

	/**
	 * Empty the queue by processing all messages on the queue.
	 * @throws InterruptedException
	 */
	public void emptyQueue() throws InterruptedException {
		long start = System.currentTimeMillis();
		int count = 0;
		do {
			count = annotationsQueueMessageReceiver.triggerFired();
			System.out.println("Emptying the annotations message queue, there were at least: " + 
					count + " messages on the queue");
			Thread.yield();
			long elapse = System.currentTimeMillis() - start;
			if (elapse > MAX_WAIT*2) {
				throw new RuntimeException("Timed-out waiting process all messages that were on the queue before the tests started.");
			}
		} while(count > 0);
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
		submissionStatusDAO.delete(submissionId);
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
		sa.setKey("string anno");
		sa.setValue("foo ");
		stringAnnos.add(sa);
		
		StringAnnotation sa2 = new StringAnnotation();
		sa2.setIsPrivate(false);
		sa2.setKey("string anno_null");
		sa2.setValue(null);
		stringAnnos.add(sa2);
		
		List<LongAnnotation> longAnnos = new ArrayList<LongAnnotation>();
		LongAnnotation la = new LongAnnotation();
		la.setIsPrivate(true);
		la.setKey("long anno");
		la.setValue(10L);
		longAnnos.add(la);
		
		List<DoubleAnnotation> doubleAnnos = new ArrayList<DoubleAnnotation>();
		DoubleAnnotation da = new DoubleAnnotation();
		da.setIsPrivate(false);
		da.setKey("double anno");
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
