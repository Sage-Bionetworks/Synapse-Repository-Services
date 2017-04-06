package org.sagebionetworks.evaluation.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.sagebionetworks.repo.model.evaluation.EvaluationDAO;
import org.sagebionetworks.repo.model.evaluation.SubmissionDAO;
import org.sagebionetworks.repo.model.evaluation.SubmissionFileHandleDAO;
import org.sagebionetworks.repo.model.file.FileHandle;
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
    
    @Before
    public void setUp() throws DatastoreException, InvalidModelException, NotFoundException {
    	userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();
    	
    	// create a file handle
		PreviewFileHandle meta1 = new PreviewFileHandle();
		meta1.setBucketName("bucketName");
		meta1.setKey("key");
		meta1.setContentType("content type");
		meta1.setContentSize(123l);
		meta1.setContentMd5("md5");
		meta1.setCreatedBy("" + userId);
		meta1.setFileName("preview.jpg");
		meta1.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		meta1.setEtag(UUID.randomUUID().toString());
		PreviewFileHandle meta2 = new PreviewFileHandle();
		meta2.setBucketName("bucketName");
		meta2.setKey("key");
		meta2.setContentType("content type");
		meta2.setContentSize(123l);
		meta2.setContentMd5("md5");
		meta2.setCreatedBy("" + userId);
		meta2.setFileName("preview.jpg");
		meta2.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		meta2.setEtag(UUID.randomUUID().toString());
		PreviewFileHandle meta3 = new PreviewFileHandle();
		meta3.setBucketName("bucketName");
		meta3.setKey("key");
		meta3.setContentType("content type");
		meta3.setContentSize(123l);
		meta3.setContentMd5("md5");
		meta3.setCreatedBy("" + userId);
		meta3.setFileName("preview.jpg");
		meta3.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		meta3.setEtag(UUID.randomUUID().toString());

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
    
    @After
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
        	assertTrue("Unknown File Handle ID returned", id.equals(fileHandleId1) || id.equals(fileHandleId2));
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
