package org.sagebionetworks.annotations.worker;

import static org.junit.Assert.assertFalse;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.SubmissionStatusAnnotationsAsyncManager;
import org.sagebionetworks.repo.model.evaluation.EvaluationDAO;
import org.sagebionetworks.repo.model.evaluation.SubmissionDAO;
import org.sagebionetworks.repo.model.evaluation.SubmissionStatusDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.sqs.model.Message;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:annot-worker-test-context.xml" })
public class AnnotationsWorkerAutowiredTest {
	@Autowired
	private SubmissionStatusDAO submissionStatusDAO;
    
	@Autowired
    private SubmissionDAO submissionDAO;
      
	@Autowired
    private EvaluationDAO evaluationDAO;
	
	@Autowired
	private NodeDAO nodeDAO;
	
	@Autowired
	SubmissionStatusAnnotationsAsyncManager ssAsyncMgr;
	
	private String nodeId;
    private String submissionId;
    private Long userId;
    private String evalId;
    private final String name = "test submission";
    private final Long versionNumber = 1L;

    @Before
	public void before() throws Exception {
		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		// create a node
  		Node node = new Node();
		node.setName(name);
		node.setCreatedByPrincipalId(userId);
		node.setModifiedByPrincipalId(userId);
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
        evaluation.setOwnerId(userId.toString());
        evaluation.setCreatedOn(new Date());
        evaluation.setContentSource(nodeId);
        evaluation.setStatus(EvaluationStatus.PLANNED);
        evalId = evaluationDAO.create(evaluation, userId);
        
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
        submissionId = submissionDAO.create(submission);
        
        // create a submissionstatus
        SubmissionStatus status = new SubmissionStatus();
        status.setModifiedOn(new Date());
        status.setId(submissionId);
        status.setEtag(null);
        status.setStatus(SubmissionStatusEnum.RECEIVED);
        status.setScore(0.1);
        submissionStatusDAO.create(status);
	}
	
	@After
	public void after(){
		try {
			submissionDAO.delete(submissionId);
		} catch (Exception e)  {};
		try {
			evaluationDAO.delete(evalId);
		} catch (Exception e) {};
    	try {
    		nodeDAO.delete(nodeId);
    	} catch (Exception e) {};
	}
	
	// test that a DeadlockLoserDataAccessException will cause the create/update to be retried
	@Test
	public void testDeadlockException() throws Exception {
		String etag = "";
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, ""+submissionId, ObjectType.SUBMISSION, etag);
		List<Message> messages = Collections.singletonList(message);
		AnnotationsWorker worker = new AnnotationsWorker(messages, ssAsyncMgr);
		List<Message> completedMessages = worker.call();
		// if the message is not in the list it means it is not completed and will be retried
		assertFalse(completedMessages.contains(message));
	}

}
