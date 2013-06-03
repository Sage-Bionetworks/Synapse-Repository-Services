package org.sagebionetworks.evaluation.dbo;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.dbo.EvaluationDBO;
import org.sagebionetworks.evaluation.dbo.ParticipantDBO;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class ParticipantDBOTest {
 
    @Autowired
    DBOBasicDao dboBasicDao;
    @Autowired
    IdGenerator idGenerator;
 
    private long userId = 0;
    private long evalId = 2;
    
    @Before
    public void setUp() {    	
        // Initialize a new Evaluation
        EvaluationDBO evaluation = new EvaluationDBO();
        evaluation.setId(evalId);
        evaluation.seteTag("etag");
        evaluation.setName("name");
        evaluation.setOwnerId(userId);
        evaluation.setCreatedOn(System.currentTimeMillis());
        evaluation.setContentSource(KeyFactory.ROOT_ID);
        evaluation.setStatusEnum(EvaluationStatus.PLANNED);
        dboBasicDao.createNew(evaluation);
    }
    
    @After
    public void after() throws DatastoreException {
        if(dboBasicDao != null) {
        	// delete participant
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("userId", userId);
            params.addValue("evalId", evalId);
            dboBasicDao.deleteObjectByPrimaryKey(ParticipantDBO.class, params);
            
            // delete Evaluation
            params.addValue("id", evalId);
            dboBasicDao.deleteObjectByPrimaryKey(EvaluationDBO.class, params);
        }
    }
    @Test
    public void testCRD() throws Exception{
        // Initialize a new Participant
        ParticipantDBO participant = new ParticipantDBO();
        participant.setUserId(userId);
        participant.setEvalId(evalId);
        participant.setId(idGenerator.generateNewId(TYPE.PARTICIPANT_ID));
        participant.setCreatedOn(System.currentTimeMillis());
 
        // Create it
        ParticipantDBO clone = dboBasicDao.createNew(participant);
        assertNotNull(clone);
        assertEquals(participant, clone);
        
        // Fetch it
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("evalId", evalId);
        clone = dboBasicDao.getObjectByPrimaryKey(ParticipantDBO.class, params);
        assertNotNull(clone);
        assertEquals(participant.getUserId(), clone.getUserId());
        assertEquals(participant.getEvalId(), clone.getEvalId());
        
        // Delete it
        boolean result = dboBasicDao.deleteObjectByPrimaryKey(ParticipantDBO.class,  params);
        assertTrue("Failed to delete the entry created", result); 
    }
 
}
