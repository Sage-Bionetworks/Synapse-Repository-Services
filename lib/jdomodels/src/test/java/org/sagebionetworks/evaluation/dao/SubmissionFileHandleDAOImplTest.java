package org.sagebionetworks.evaluation.dao;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.FileHandleLinkedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class SubmissionFileHandleDAOImplTest {
 
    @Autowired
    private SubmissionDAO submissionDAO;
    
    @Autowired
    private SubmissionFileHandleDAO submissionFileHandleDAO;
    
    @Autowired
    private EvaluationDAO evaluationDAO;
	
    @Autowired
    private NodeDAO nodeDAO;
	
    @Autowired
    private FileHandleDao fileHandleDAO;

	@Autowired
	private IdGenerator idGenerator;
 
	private String userId;
	private String nodeId;
	
    private String submissionId1 = "206";
    private String submissionId2 = "207";
    private String evalId;
    private String name = "test submission";
    private String fileHandleId1;
    private String fileHandleId2;
    private String fileHandleId3;
    private Long versionNumber = 1L;
    
    @BeforeEach
    public void setUp() throws DatastoreException, InvalidModelException, NotFoundException {
    	userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();
    	
    	// create a file handle
		S3FileHandle meta1 = TestUtils.createS3FileHandle(userId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		S3FileHandle meta2 = TestUtils.createS3FileHandle(userId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		S3FileHandle meta3 = TestUtils.createS3FileHandle(userId, idGenerator.generateNewId(IdType.FILE_IDS).toString());

		List<FileHandle> fileHandleToCreate = new LinkedList<FileHandle>();
		fileHandleToCreate.add(meta1);
		fileHandleToCreate.add(meta2);
		fileHandleToCreate.add(meta3);
		fileHandleDAO.createBatch(fileHandleToCreate);

		fileHandleId1 = meta1.getId();
		fileHandleId2 = meta2.getId();
		fileHandleId3 = meta3.getId();
		
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
        evaluation.setContentSource(nodeId);
        evaluation.setStatus(EvaluationStatus.PLANNED);
        evalId = evaluationDAO.create(evaluation, Long.parseLong(userId));
        
        // create a Submission
        Submission submission = new Submission();
        submission.setCreatedOn(new Date());
        submission.setId(submissionId1);
        submission.setName(name);
        submission.setEvaluationId(evalId);
        submission.setEntityId(nodeId);
        submission.setVersionNumber(versionNumber);
        submission.setUserId(userId);
        submission.setEntityBundleJSON("some bundle");
        submissionId1 = submissionDAO.create(submission);
        submission.setId(submissionId2);
        submissionId2 = submissionDAO.create(submission);
    }
    
    @AfterEach
    public void tearDown() throws DatastoreException {
    	try {
    		submissionFileHandleDAO.delete(submissionId1, fileHandleId1);
    	} catch (NotFoundException e) {};
    	try {
    		submissionFileHandleDAO.delete(submissionId1, fileHandleId2);
    	} catch (NotFoundException e) {};
		try {
			submissionDAO.delete(submissionId1);
		} catch (NotFoundException e)  {};
		try {
			submissionDAO.delete(submissionId2);
		} catch (NotFoundException e)  {};
		try {
			evaluationDAO.delete(evalId);
		} catch (NotFoundException e) {};
    	try {
    		nodeDAO.delete(nodeId);
    	} catch (NotFoundException e) {};
    	fileHandleDAO.delete(fileHandleId1);
		fileHandleDAO.delete(fileHandleId2);
		fileHandleDAO.delete(fileHandleId3);
    }
    
    @Test
    public void testRoundTrip() throws Exception{
        long initialCount = submissionFileHandleDAO.getCount();
 
        // create two SubmissionFileHandles for Submission1
        submissionFileHandleDAO.create(submissionId1, fileHandleId1);
        submissionFileHandleDAO.create(submissionId1, fileHandleId2);
        // create one SubmissionFileHandle for Submission2
        submissionFileHandleDAO.create(submissionId2, fileHandleId3);
        
        // fetch
        assertEquals(initialCount + 3, submissionFileHandleDAO.getCount());
        List<String> ids = submissionFileHandleDAO.getAllBySubmission(submissionId1);
        assertEquals(2, ids.size());
        for (String id : ids) {
        	assertTrue(id.equals(fileHandleId1) || id.equals(fileHandleId2), "Unknown File Handle ID returned");
        }
        
        // delete
        submissionFileHandleDAO.delete(submissionId1, fileHandleId1);
        submissionFileHandleDAO.delete(submissionId1, fileHandleId2);
        submissionFileHandleDAO.delete(submissionId2, fileHandleId3);
        assertEquals(initialCount, submissionFileHandleDAO.getCount());
    }
    
    @Test
    public void testNoHandles() throws Exception{        
        // should return an empty list
        List<String> ids = submissionFileHandleDAO.getAllBySubmission(submissionId1);
        assertNotNull(ids);
        assertEquals(0, ids.size());
    }
    
    @Test
    public void testDeleteFileHandle() throws DatastoreException, NotFoundException {     
        // create SubmissionFileHandle
        submissionFileHandleDAO.create(submissionId1, fileHandleId1);
        long count = submissionFileHandleDAO.getCount();
        
        assertThrows(FileHandleLinkedException.class, () -> {
        	// delete should fail (due to ON DELETE RESTRICT constraint)
	        fileHandleDAO.delete(fileHandleId1);
        });
	        
    	assertNotNull(fileHandleDAO.get(fileHandleId1), "File handle should not have been deleted!");
    	assertEquals(count, submissionFileHandleDAO.getCount());
        	
    }
    
    @Test
    public void testDeleteSubmission() throws DatastoreException, NotFoundException {
        long initialCount = submissionFileHandleDAO.getCount();
        
        // create SubmissionFileHandles
        submissionFileHandleDAO.create(submissionId1, fileHandleId1);
        submissionFileHandleDAO.create(submissionId1, fileHandleId2);
        assertEquals(initialCount + 2, submissionFileHandleDAO.getCount());
        
        // delete Submission
        submissionDAO.delete(submissionId1);
        
        // submissionFileHandles should have been deleted
        assertEquals(initialCount, submissionFileHandleDAO.getCount());
        
        // FileHandles should not have been deleted
        assertNotNull(fileHandleDAO.get(fileHandleId1));
        assertNotNull(fileHandleDAO.get(fileHandleId2));
    }
    
}
