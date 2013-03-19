package org.sagebionetworks.evaluation.dao;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.ParticipantDAO;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.dao.SubmissionDAOImpl;
import org.sagebionetworks.evaluation.dao.SubmissionStatusDAO;
import org.sagebionetworks.evaluation.dbo.SubmissionDBO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class SubmissionDAOImplTest {
 
    @Autowired
    SubmissionDAO submissionDAO;
    @Autowired
    SubmissionStatusDAO submissionStatusDAO;
    @Autowired
    ParticipantDAO participantDAO;
    @Autowired
    EvaluationDAO evaluationDAO;
	@Autowired
	NodeDAO nodeDAO;
 
	private String nodeId = null;
    private String submissionId = "206";
    private String userId = "0";
    private String userId_does_not_exist = "2";
    private String evalId;
    private String evalId_does_not_exist = "456";
    private String name = "test submission";
    private Long versionNumber = 1L;
    private Submission submission;
    
    @Before
    public void setUp() throws DatastoreException, InvalidModelException, NotFoundException {
    	// create a node
  		Node toCreate = NodeTestUtils.createNew(name, Long.parseLong(userId));
    	toCreate.setVersionComment("This is the first version of the first node ever!");
    	toCreate.setVersionLabel("0.0.1");
    	nodeId = nodeDAO.createNew(toCreate);
    	
    	// create a Evaluation
        Evaluation evaluation = new Evaluation();
        evaluation.setId("1234");
        evaluation.setEtag("etag");
        evaluation.setName("name");
        evaluation.setOwnerId(userId);
        evaluation.setCreatedOn(new Date());
        evaluation.setContentSource("foobar");
        evaluation.setStatus(EvaluationStatus.PLANNED);
        evalId = evaluationDAO.create(evaluation, Long.parseLong(userId));
        
        // create a Participant
        Participant participant = new Participant();
        participant.setCreatedOn(new Date());
        participant.setUserId(userId);
        participant.setEvaluationId(evalId);
        participantDAO.create(participant);
        
        // Initialize a Submission
        submission = new Submission();
        submission.setCreatedOn(new Date());
        submission.setId(submissionId);
        submission.setName(name);
        submission.setEvaluationId(evalId);
        submission.setEntityId(nodeId);
        submission.setVersionNumber(versionNumber);
        submission.setUserId(userId);
    }
    
    @After
    public void tearDown() throws DatastoreException {
    	try {
    		nodeDAO.delete(nodeId);
    	} catch (NotFoundException e) {};
		try {
			submissionDAO.delete(submissionId);
		} catch (NotFoundException e)  {};
		try {
			participantDAO.delete(userId, evalId);
		} catch (NotFoundException e) {};
		try {
			evaluationDAO.delete(evalId);
		} catch (NotFoundException e) {};
    }
    
    @Test
    public void testCRD() throws Exception{
        long initialCount = submissionDAO.getCount();
 
        // create Submission
        submissionId = submissionDAO.create(submission);
        assertNotNull(submissionId);   
        
        // fetch it
        Submission clone = submissionDAO.get(submissionId);
        assertNotNull(clone);
        submission.setId(submissionId);
        submission.setCreatedOn(clone.getCreatedOn());
        assertEquals(initialCount + 1, submissionDAO.getCount());
        assertEquals(submission, clone);
        
        // delete it
        submissionDAO.delete(submissionId);
        try {
        	clone = submissionDAO.get(submissionId);
        } catch (NotFoundException e) {
        	// expected
        	assertEquals(initialCount, submissionDAO.getCount());
        	return;
        }
        fail("Failed to delete Participant");
    }
    
    @Test
    public void testGetAllByUser() throws DatastoreException, NotFoundException {
    	submissionId = submissionDAO.create(submission);
    	
    	// userId should have submissions
    	List<Submission> subs = submissionDAO.getAllByUser(userId, 10, 0);
    	assertEquals(1, subs.size());
    	assertEquals(subs.size(), submissionDAO.getCountByUser(userId));
    	submission.setCreatedOn(subs.get(0).getCreatedOn());
    	submission.setId(subs.get(0).getId());
    	assertEquals(subs.get(0), submission);
    	
    	// userId_does_not_exist should not have any submissions
    	subs = submissionDAO.getAllByUser(userId_does_not_exist, 10, 0);
    	assertEquals(0, subs.size());
    }
    
    @Test
    public void testGetAllByEvaluation() throws DatastoreException, NotFoundException {
    	submissionId = submissionDAO.create(submission);
    	
    	// evalId should have submissions
    	List<Submission> subs = submissionDAO.getAllByEvaluation(evalId, 10, 0);
    	assertEquals(1, subs.size());
    	assertEquals(subs.size(), submissionDAO.getCountByEvaluation(evalId));
    	submission.setCreatedOn(subs.get(0).getCreatedOn());
    	submission.setId(subs.get(0).getId());
    	assertEquals(subs.get(0), submission);
    	
    	// evalId_does_not_exist should not have any submissions
    	subs = submissionDAO.getAllByEvaluation(evalId_does_not_exist, 10, 0);
    	assertEquals(0, subs.size());
    	assertEquals(0, submissionDAO.getCountByEvaluation(evalId_does_not_exist));
    }
    
    @Test
    public void testGetAllByEvaluationAndStatus() throws DatastoreException, NotFoundException {
    	submissionId = submissionDAO.create(submission);
    	
    	// create a SubmissionStatus object
    	SubmissionStatus subStatus = new SubmissionStatus();
    	subStatus.setId(submissionId);
    	subStatus.setStatus(SubmissionStatusEnum.OPEN);
    	subStatus.setModifiedOn(new Date());
    	submissionStatusDAO.create(subStatus);
    	
    	// hit evalId and hit status => should find 1 submission
    	List<Submission> subs = submissionDAO.getAllByEvaluationAndStatus(evalId, SubmissionStatusEnum.OPEN, 10, 0);
    	assertEquals(1, subs.size());
    	assertEquals(subs.size(), submissionDAO.getCountByEvaluationAndStatus(evalId, SubmissionStatusEnum.OPEN));
    	submission.setCreatedOn(subs.get(0).getCreatedOn());
    	submission.setId(subs.get(0).getId());
    	assertEquals(subs.get(0), submission);
    	
    	// miss evalId and hit status => should find 0 submissions
    	subs = submissionDAO.getAllByEvaluationAndStatus(evalId_does_not_exist, SubmissionStatusEnum.OPEN, 10, 0);
    	assertEquals(0, subs.size());
    	assertEquals(subs.size(), submissionDAO.getCountByEvaluationAndStatus(evalId_does_not_exist, SubmissionStatusEnum.OPEN));
    	
    	// hit evalId and miss status => should find 0 submissions
    	subs = submissionDAO.getAllByEvaluationAndStatus(evalId, SubmissionStatusEnum.CLOSED, 10, 0);
    	assertEquals(0, subs.size());
    	assertEquals(subs.size(), submissionDAO.getCountByEvaluationAndStatus(evalId, SubmissionStatusEnum.CLOSED));
    }
    
    @Test
    public void testGetAllByEvaluationAndUser() throws DatastoreException, NotFoundException {
    	submissionId = submissionDAO.create(submission);
    	
    	// hit evalId and hit user => should find 1 submission
    	List<Submission> subs = submissionDAO.getAllByEvaluationAndUser(evalId, userId, 10, 0);
    	assertEquals(1, subs.size());
    	assertEquals(subs.size(), submissionDAO.getCountByEvaluationAndUser(evalId, userId));
    	submission.setCreatedOn(subs.get(0).getCreatedOn());
    	submission.setId(subs.get(0).getId());
    	assertEquals(subs.get(0), submission);
    	
    	// miss evalId and hit user => should find 0 submissions
    	subs = submissionDAO.getAllByEvaluationAndUser(evalId_does_not_exist, userId, 10, 0);
    	assertEquals(0, subs.size());
    	assertEquals(subs.size(), submissionDAO.getCountByEvaluationAndUser(evalId_does_not_exist, userId));
    	
    	// hit evalId and miss user => should find 0 submissions
    	subs = submissionDAO.getAllByEvaluationAndUser(evalId, userId_does_not_exist, 10, 0);
    	assertEquals(0, subs.size());
    	assertEquals(subs.size(), submissionDAO.getCountByEvaluationAndUser(evalId, userId_does_not_exist));
    }
    
    @Test
    public void testDtoToDbo() {
    	Submission subDTO = new Submission();
    	Submission subDTOclone = new Submission();
    	SubmissionDBO subDBO = new SubmissionDBO();
    	SubmissionDBO subDBOclone = new SubmissionDBO();
    	
    	subDTO.setEvaluationId("123");
    	subDTO.setCreatedOn(new Date());
    	subDTO.setEntityId("syn456");
    	subDTO.setId("789");
    	subDTO.setName("name");
    	subDTO.setUserId("42");
    	subDTO.setVersionNumber(1L);
    	    	
    	SubmissionDAOImpl.copyDtoToDbo(subDTO, subDBO);
    	SubmissionDAOImpl.copyDboToDto(subDBO, subDTOclone);
    	SubmissionDAOImpl.copyDtoToDbo(subDTOclone, subDBOclone);
    	
    	assertEquals(subDTO, subDTOclone);
    	assertEquals(subDBO, subDBOclone);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testMissingVersionNumber() {
        submission.setVersionNumber(null);
        submissionDAO.create(submission);
    }
}
