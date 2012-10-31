package org.sagebionetworks.competition.dbo;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.competition.model.SubmissionStatus;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class SubmissionDBOTest {
 
    @Autowired
    DBOBasicDao dboBasicDao;
 
    private long submissionId = 2000;
    private long userId = 1;
    private long compId = 2;
    private long entityId = 4;
    private String name = "test submission";
    private Long score = 0L;
    
    @Before
    public void setUp() {    	
        // Initialize a new competition
        CompetitionDBO competition = new CompetitionDBO();
        competition.setId(compId);
        competition.seteTag("etag");
        competition.setName("name");
        competition.setOwnerId(userId);
        competition.setCreatedOn(new Date(System.currentTimeMillis()));
        competition.setContentSource("foobar");
        competition.setStatusEnum(CompetitionStatus.PLANNED);
        dboBasicDao.createNew(competition);        
    }
    
    @After
    public void after() throws DatastoreException {
        if(dboBasicDao != null) {
        	// delete submission
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("id", submissionId);
            dboBasicDao.deleteObjectById(SubmissionDBO.class, params);            
            
            // delete competition
            params = new MapSqlParameterSource();
            params.addValue("id", compId);
            dboBasicDao.deleteObjectById(CompetitionDBO.class, params);
        }
    }
    @Test
    public void testCRUD() throws Exception{
        // Initialize a new Submission
        SubmissionDBO submission = new SubmissionDBO();
        submission.setId(submissionId);
        submission.setName(name);
        submission.setEntityId(entityId);
        submission.setStatusEnum(SubmissionStatus.OPEN);
        submission.setUserId(userId);
        submission.setCompetitionId(compId);
        submission.setScore(score);
        submission.setCreatedOn(new Date(System.currentTimeMillis()));
 
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
		assertEquals(newScore, clone3.getScore());
		clone.setScore(newScore);
		assertEquals(clone, clone3);      
        
        // Delete it
        result = dboBasicDao.deleteObjectById(SubmissionDBO.class,  params);
        assertTrue("Failed to delete the entry created", result); 
    }
 
}
