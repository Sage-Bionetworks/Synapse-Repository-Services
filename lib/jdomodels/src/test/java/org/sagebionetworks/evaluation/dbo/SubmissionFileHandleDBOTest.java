package org.sagebionetworks.evaluation.dbo;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.dbo.EvaluationDBO;
import org.sagebionetworks.evaluation.dbo.ParticipantDBO;
import org.sagebionetworks.evaluation.dbo.SubmissionDBO;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class SubmissionFileHandleDBOTest {
 
    @Autowired
    DBOBasicDao dboBasicDao;
	@Autowired
	NodeDAO nodeDAO;
	@Autowired
	FileHandleDao fileHandleDAO;
    @Autowired
    IdGenerator idGenerator;
    
    private String nodeId = null;
    private long submissionId;
    private long userId = 0;
    private long evalId;
    private String fileHandleId;
    private String name = "test submission";
    
    @Before
    public void setUp() throws DatastoreException, InvalidModelException, NotFoundException, IOException {   
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
  		Node toCreate = NodeTestUtils.createNew(name, userId);
    	toCreate.setVersionComment("This is the first version of the first node ever!");
    	toCreate.setVersionLabel("0.0.1");
    	toCreate.setFileHandleId(fileHandleId);
    	nodeId = nodeDAO.createNew(toCreate).substring(3); // trim "syn" from node ID    
    	
        // Initialize a new Evaluation
        EvaluationDBO evaluation = new EvaluationDBO();
        evaluation.setId(evalId);
        evaluation.seteTag("etag");
        evaluation.setName("name");
        evaluation.setOwnerId(userId);
        evaluation.setContentSource(KeyFactory.ROOT_ID);
        evaluation.setCreatedOn(System.currentTimeMillis());
        evaluation.setStatusEnum(EvaluationStatus.PLANNED);
        evalId = dboBasicDao.createNew(evaluation).getId();
        
        // Initialize a new Participant
        ParticipantDBO participant = new ParticipantDBO();
        participant.setUserId(userId);
        participant.setEvalId(evalId);
        participant.setCreatedOn(System.currentTimeMillis());
        participant.setId(idGenerator.generateNewId(TYPE.PARTICIPANT_ID));
        dboBasicDao.createNew(participant);
        
        // Initialize a new Submission
        SubmissionDBO submission = new SubmissionDBO();
        submission.setId(submissionId);
        submission.setName(name);
        submission.setEntityId(Long.parseLong(nodeId));
        submission.setVersionNumber(1L);
        submission.setUserId(userId);
        submission.setEvalId(evalId);
        submission.setCreatedOn(System.currentTimeMillis());
        submission.setEntityBundle(JDOSecondaryPropertyUtils.compressObject(submission));
        submissionId = dboBasicDao.createNew(submission).getId();
        
    }
    
    @After
    public void tearDown() throws DatastoreException {
        if(dboBasicDao != null) {
        	// delete Submission
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("id", submissionId);
            dboBasicDao.deleteObjectByPrimaryKey(SubmissionDBO.class, params);
            
            // delete Participant
            params = new MapSqlParameterSource();
            params.addValue("userId", userId);
            params.addValue("evalId", evalId);
            dboBasicDao.deleteObjectByPrimaryKey(ParticipantDBO.class, params);
            
            // delete Evaluation
            params = new MapSqlParameterSource();
            params.addValue("id", evalId);
            dboBasicDao.deleteObjectByPrimaryKey(EvaluationDBO.class, params);
        }
    	try {
    		nodeDAO.delete(nodeId);
    	} catch (NotFoundException e) {};
    	fileHandleDAO.delete(fileHandleId);
    }
    
    @Test
    public void testCRD() throws Exception{
    	// Initialize a new SubmissionFileHandle
    	SubmissionFileHandleDBO handle = new SubmissionFileHandleDBO();
    	handle.setSubmissionId(submissionId);
    	handle.setFileHandleId(Long.parseLong(fileHandleId));
    	
        // Create it
        SubmissionFileHandleDBO clone = dboBasicDao.createNew(handle);
        assertNotNull(clone);
        assertEquals(handle, clone);
        
        // Fetch it
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(DBOConstants.PARAM_SUBFILE_SUBMISSION_ID, submissionId);
        params.addValue(DBOConstants.PARAM_SUBFILE_FILE_HANDLE_ID, fileHandleId);
        SubmissionFileHandleDBO clone2 = dboBasicDao.getObjectByPrimaryKey(SubmissionFileHandleDBO.class, params);
        assertNotNull(clone2);
        assertEquals(handle, clone2); 
        
        // Delete it
        boolean result = dboBasicDao.deleteObjectByPrimaryKey(SubmissionFileHandleDBO.class,  params);
        assertTrue("Failed to delete the entry created", result); 
    }
 
}
