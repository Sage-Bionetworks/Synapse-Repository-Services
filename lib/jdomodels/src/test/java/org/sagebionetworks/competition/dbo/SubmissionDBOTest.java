package org.sagebionetworks.competition.dbo;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.competition.model.SubmissionStatus;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class SubmissionDBOTest {
 
    @Autowired
    DBOBasicDao dboBasicDao;
	@Autowired
	NodeDAO nodeDAO;
 
    private String nodeId = null;
    private long submissionId = 2000;
    private long userId = 0;
    private long compId;
    private String name = "test submission";
    private Long score = 0L;
    
    @Before
    public void setUp() throws DatastoreException, InvalidModelException, NotFoundException {    	
    	// create a node
  		Node toCreate = NodeTestUtils.createNew(name, userId);
    	toCreate.setVersionComment("This is the first version of the first node ever!");
    	toCreate.setVersionLabel("0.0.1");
    	nodeId = nodeDAO.createNew(toCreate).substring(3); // trim "syn" from node ID
    	
        // Initialize a new competition
        CompetitionDBO competition = new CompetitionDBO();
        competition.setId(compId);
        competition.seteTag("etag");
        competition.setName("name");
        competition.setOwnerId(userId);
        competition.setContentSource("foobar");
        competition.setCreatedOn(System.currentTimeMillis());
        competition.setStatusEnum(CompetitionStatus.PLANNED);
        compId = dboBasicDao.createNew(competition).getId();
        
        // Initialize a new Participant
        ParticipantDBO participant = new ParticipantDBO();
        participant.setUserId(userId);
        participant.setCompId(compId);
        participant.setCreatedOn(System.currentTimeMillis());
        dboBasicDao.createNew(participant);
    }
    
    @After
    public void after() throws DatastoreException {
        if(dboBasicDao != null) {
        	// delete submission
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("id", submissionId);
            dboBasicDao.deleteObjectById(SubmissionDBO.class, params);
            
            // delete participant
            params = new MapSqlParameterSource();
            params.addValue("userId", userId);
            params.addValue("compId", compId);
            dboBasicDao.deleteObjectById(ParticipantDBO.class, params);
            
            // delete competition
            params = new MapSqlParameterSource();
            params.addValue("id", compId);
            dboBasicDao.deleteObjectById(CompetitionDBO.class, params);
        }
    	try {
    		nodeDAO.delete(nodeId);
    	} catch (NotFoundException e) {};
    }
    @Test
    public void testCRUD() throws Exception{
        // Initialize a new Submission
        SubmissionDBO submission = new SubmissionDBO();
        submission.setId(submissionId);
        submission.setName(name);
        submission.setEntityId(Long.parseLong(nodeId));
        submission.setVersionNumber(1L);
        submission.setStatusEnum(SubmissionStatus.OPEN);
        submission.setUserId(userId);
        submission.setCompId(compId);
        submission.setScore(score);
        submission.setCreatedOn(System.currentTimeMillis());
 
        // Create it
        SubmissionDBO clone = dboBasicDao.createNew(submission);
        assertNotNull(clone);
        assertEquals(submission, clone);
        
        // Fetch it
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id",submissionId);
        SubmissionDBO clone2 = dboBasicDao.getObjectById(SubmissionDBO.class, params);
        assertNotNull(clone2);
        assertEquals(submission, clone2);
        
		// Update it
        Long newScore = score + 100;
		clone.setScore(newScore);
		boolean result = dboBasicDao.update(clone);
		assertTrue(result);
		
		// Verify it
		params = new MapSqlParameterSource();
		params.addValue("id", submissionId);
		SubmissionDBO clone3 = dboBasicDao.getObjectById(SubmissionDBO.class, params);
		assertEquals(clone, clone3);      
        
        // Delete it
        result = dboBasicDao.deleteObjectById(SubmissionDBO.class,  params);
        assertTrue("Failed to delete the entry created", result); 
    }
 
}
