package org.sagebionetworks.evaluation.dao;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.ParticipantDAO;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.dao.SubmissionStatusDAO;
import org.sagebionetworks.evaluation.dao.SubmissionStatusDAOImpl;
import org.sagebionetworks.evaluation.dbo.SubmissionStatusDBO;
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
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class SubmissionStatusDAOImplTest {
 
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
 
	private String nodeId = null;
    private String submissionId = null;
    private String userId = "0";
    private String evalId;
    private String name = "test submission";
    private Long versionNumber = 1L;
    
    @Before
    public void setUp() throws DatastoreException, InvalidModelException, NotFoundException, JSONObjectAdapterException {
    	// create a node
  		Node toCreate = NodeTestUtils.createNew(name, Long.parseLong(userId));
    	toCreate.setVersionComment("This is the first version of the first node ever!");
    	toCreate.setVersionLabel("0.0.1");
    	nodeId = nodeDAO.createNew(toCreate).substring(3); // trim "syn" from node ID
    	
    	// create an evaluation
        Evaluation evaluation = new Evaluation();
        evaluation.setId("1234");
        evaluation.setEtag("etag");
        evaluation.setName("name");
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
    }
    
    @After
    public void after() throws DatastoreException {
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
    public void testCRUD() throws Exception{
        // Initialize a new SubmissionStatus object for submissionId
        SubmissionStatus status = new SubmissionStatus();
        status.setModifiedOn(new Date());
        status.setId(submissionId);
        status.setEtag(null);
        status.setStatus(SubmissionStatusEnum.OPEN);
        status.setScore(0.1);
        long initialCount = submissionStatusDAO.getCount();
        
        // Create it
        submissionStatusDAO.create(status);
        assertEquals(initialCount + 1, submissionStatusDAO.getCount());
        
        // Fetch it
        SubmissionStatus clone = submissionStatusDAO.get(submissionId);
        assertNotNull(clone);
        assertNotNull(clone.getModifiedOn());        
        assertNotNull(clone.getEtag());
        status.setModifiedOn(clone.getModifiedOn());
        status.setEtag(clone.getEtag());
        assertEquals(status, clone);
        
        // Update it
        clone.setStatus(SubmissionStatusEnum.SCORED);
        clone.setScore(0.9);
        Thread.sleep(1L);
        submissionStatusDAO.update(clone);
        SubmissionStatus clone2 = submissionStatusDAO.get(submissionId);
        assertFalse("eTag was not updated.", clone.getEtag().equals(clone2.getEtag()));
        assertFalse("Modified date was not updated", clone.getModifiedOn().equals(clone2.getModifiedOn()));
        clone.setModifiedOn(clone2.getModifiedOn());
        clone.setEtag(clone2.getEtag());
        assertEquals(clone, clone2);

    	// Delete it
        submissionStatusDAO.delete(submissionId);
        assertEquals(initialCount, submissionStatusDAO.getCount());
        
        // Fetch it (should not exist)
        try {
        	status = submissionStatusDAO.get(submissionId);
        } catch (NotFoundException e) {
        	// expected
        }
    }
    
    @Test
    public void testCreateFromBackup() throws Exception{
        // Initialize a new SubmissionStatus object for submissionId
        SubmissionStatus status = new SubmissionStatus();
        status.setModifiedOn(new Date());
        status.setId(submissionId);
        status.setEtag("original-eTag");
        status.setStatus(SubmissionStatusEnum.OPEN);
        status.setScore(0.1);
        
        // Create it
        submissionStatusDAO.createFromBackup(status);
        SubmissionStatus restored = submissionStatusDAO.get(submissionId);
        assertEquals(status, restored);
    }
    
    @Test
    public void testDtoToDbo() {
    	SubmissionStatus subStatusDTO = new SubmissionStatus();
    	SubmissionStatus subStatusDTOclone = new SubmissionStatus();
    	SubmissionStatusDBO subStatusDBO = new SubmissionStatusDBO();
    	SubmissionStatusDBO subStatusDBOclone = new SubmissionStatusDBO();
    	
    	subStatusDTO.setEtag("eTag");
    	subStatusDTO.setId("123");
    	subStatusDTO.setModifiedOn(new Date());
    	subStatusDTO.setScore(0.42);
    	subStatusDTO.setStatus(SubmissionStatusEnum.CLOSED);
    	subStatusDTO.setReport("lorem ipsum");
    	    	
    	subStatusDBO = SubmissionStatusDAOImpl.convertDtoToDbo(subStatusDTO);
    	subStatusDTOclone = SubmissionStatusDAOImpl.convertDboToDto(subStatusDBO);
    	subStatusDBOclone = SubmissionStatusDAOImpl.convertDtoToDbo(subStatusDTOclone);
    	
    	assertEquals(subStatusDTO, subStatusDTOclone);
    	assertEquals(subStatusDBO, subStatusDBOclone);
    }
}
