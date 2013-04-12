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
import org.sagebionetworks.evaluation.dao.SubmissionStatusDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.UnexpectedRollbackException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class SubmissionFileHandleDAOImplTest {
 
    @Autowired
    SubmissionDAO submissionDAO;
    @Autowired
    SubmissionStatusDAO submissionStatusDAO;
    @Autowired
    SubmissionFileHandleDAO submissionFileHandleDAO;
    @Autowired
    ParticipantDAO participantDAO;
    @Autowired
    EvaluationDAO evaluationDAO;
	@Autowired
	NodeDAO nodeDAO;
	@Autowired
	FileHandleDao fileHandleDAO;
 
	private String nodeId = null;
    private String submissionId = "206";
    private String userId = "0";
    private String evalId;
    private String name = "test submission";
    private String fileHandleId1;
    private String fileHandleId2;
    private Long versionNumber = 1L;
    
    @Before
    public void setUp() throws DatastoreException, InvalidModelException, NotFoundException {
    	// create a file handle
		PreviewFileHandle meta = new PreviewFileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy("" + userId);
		meta.setFileName("preview.jpg");
		fileHandleId1 = fileHandleDAO.createFile(meta).getId();		
		fileHandleId2 = fileHandleDAO.createFile(meta).getId();
		
    	// create a node
  		Node toCreate = NodeTestUtils.createNew(name, Long.parseLong(userId));
    	toCreate.setVersionComment("This is the first version of the first node ever!");
    	toCreate.setVersionLabel("0.0.1");
    	toCreate.setFileHandleId(fileHandleId1);
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
        
        // create a Submission
        Submission submission = new Submission();
        submission.setCreatedOn(new Date());
        submission.setId(submissionId);
        submission.setName(name);
        submission.setEvaluationId(evalId);
        submission.setEntityId(nodeId);
        submission.setVersionNumber(versionNumber);
        submission.setUserId(userId);
        submission.setEntityBundleJSON("some bundle");
        submissionId = submissionDAO.create(submission);
    }
    
    @After
    public void tearDown() throws DatastoreException {
    	try {
    		submissionFileHandleDAO.delete(submissionId, fileHandleId1);
    	} catch (NotFoundException e) {};
    	try {
    		submissionFileHandleDAO.delete(submissionId, fileHandleId2);
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
    	try {
    		nodeDAO.delete(nodeId);
    	} catch (NotFoundException e) {};
    	fileHandleDAO.delete(fileHandleId1);
		fileHandleDAO.delete(fileHandleId2);
    }
    
    @Test
    public void testRoundTrip() throws Exception{
        long initialCount = submissionFileHandleDAO.getCount();
 
        // create SubmissionFileHandles
        submissionFileHandleDAO.create(submissionId, fileHandleId1);
        submissionFileHandleDAO.create(submissionId, fileHandleId2);
        
        // fetch
        assertEquals(initialCount + 2, submissionFileHandleDAO.getCount());
        List<String> ids = submissionFileHandleDAO.getAllBySubmission(submissionId);
        assertEquals(2, ids.size());
        for (String id : ids) {
        	assertTrue("Unknown File Handle ID returned", id.equals(fileHandleId1) || id.equals(fileHandleId2));
        }
        
        // delete
        submissionFileHandleDAO.delete(submissionId, fileHandleId1);
        submissionFileHandleDAO.delete(submissionId, fileHandleId2);
        assertEquals(initialCount, submissionFileHandleDAO.getCount());
    }
    
    @Test
    public void testNoHandles() throws Exception{        
        // should return an empty list
        List<String> ids = submissionFileHandleDAO.getAllBySubmission(submissionId);
        assertNotNull(ids);
        assertEquals(0, ids.size());
    }
    
    @Test
    public void testDeleteFileHandle() throws DatastoreException, NotFoundException {     
        // create SubmissionFileHandle
        submissionFileHandleDAO.create(submissionId, fileHandleId1);
        long count = submissionFileHandleDAO.getCount();
        
        try {
	        // delete should fail (due to ON DELETE RESTRICT constraint)
	        fileHandleDAO.delete(fileHandleId1);
        } catch (UnexpectedRollbackException e) {
        	assertNotNull("File handle should not have been deleted!", fileHandleDAO.get(fileHandleId1));
        	assertEquals(count, submissionFileHandleDAO.getCount());
        	return;
        }
        fail("FileHandle deletion should not have succeeded!");
    }
    
    @Test
    public void testDeleteSubmission() throws DatastoreException, NotFoundException {
        long initialCount = submissionFileHandleDAO.getCount();
        
        // create SubmissionFileHandles
        submissionFileHandleDAO.create(submissionId, fileHandleId1);
        submissionFileHandleDAO.create(submissionId, fileHandleId2);
        assertEquals(initialCount + 2, submissionFileHandleDAO.getCount());
        
        // delete Submission
        submissionDAO.delete(submissionId);
        
        // submissionFileHandles should have been deleted
        assertEquals(initialCount, submissionFileHandleDAO.getCount());
        
        // FileHandles should not have been deleted
        assertNotNull(fileHandleDAO.get(fileHandleId1));
        assertNotNull(fileHandleDAO.get(fileHandleId2));
    }
    
}
