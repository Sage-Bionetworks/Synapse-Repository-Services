package org.sagebionetworks.evaluation.dbo;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.dbo.EvaluationDBO;
import org.sagebionetworks.evaluation.dbo.ParticipantDBO;
import org.sagebionetworks.evaluation.dbo.SubmissionDBO;
import org.sagebionetworks.evaluation.dbo.SubmissionStatusDBO;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class SubmissionStatusDBOTest {
 
    @Autowired
    DBOBasicDao dboBasicDao;
	@Autowired
	NodeDAO nodeDAO;
    @Autowired
    IdGenerator idGenerator;
    
    private String nodeId = null;
    private long submissionId = 2000;
    private long userId = 0;
    private long evalId;
    private String name = "test submission";
    private String eTag = "foo";
    
    @Before
    public void setUp() throws DatastoreException, InvalidModelException, NotFoundException {    	
    	// create a node
  		Node toCreate = NodeTestUtils.createNew(name, userId);
    	toCreate.setVersionComment("This is the first version of the first node ever!");
    	toCreate.setVersionLabel("0.0.1");
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
        dboBasicDao.createNew(submission);
    }
    
    @After
    public void after() throws DatastoreException {
        if(dboBasicDao != null) {            
            // delete submission
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("id", submissionId);
            dboBasicDao.deleteObjectByPrimaryKey(SubmissionDBO.class, params);
            
            // delete participant
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
    }
    @Test
    public void testCRUD() throws Exception{
        // Initialize a new SubmissionStatus object for submissionId
        SubmissionStatusDBO status = new SubmissionStatusDBO();
        status.setId(submissionId);
        status.seteTag(eTag);
        status.setModifiedOn(System.currentTimeMillis());
        status.setStatusEnum(SubmissionStatusEnum.OPEN);
        status.setScore(0.0);
        status.setSerializedEntity("foo".getBytes());
        
        // Create it
        SubmissionStatusDBO clone = dboBasicDao.createNew(status);
        assertNotNull(clone);
        assertEquals(status, clone);
        
        // Fetch it
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id",submissionId);
        SubmissionStatusDBO clone2 = dboBasicDao.getObjectByPrimaryKey(SubmissionStatusDBO.class, params);
        assertEquals(status, clone2);
        
        // Update it
        clone2.setStatusEnum(SubmissionStatusEnum.SCORED);
        clone2.setScore(0.9);
        dboBasicDao.update(clone2);
        SubmissionStatusDBO clone3 = dboBasicDao.getObjectByPrimaryKey(SubmissionStatusDBO.class, params);
        assertEquals(clone2, clone3);

    	// Delete it
        boolean result = dboBasicDao.deleteObjectByPrimaryKey(SubmissionStatusDBO.class,  params);
        assertTrue("Failed to delete the entry created", result); 
    }
 
}
