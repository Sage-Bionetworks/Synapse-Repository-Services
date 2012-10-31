package org.sagebionetworks.competition.dbo;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class ParticipantDBOTest {
 
    @Autowired
    DBOBasicDao dboBasicDao;
 
    private long userId = 1;
    private long compId = 2;
    
    @Before
    public void setUp() {    	
        // Initialize a new competition
        CompetitionDBO competition = new CompetitionDBO();
        competition.setId(compId);
        competition.seteTag("etag");
        competition.setName("name");
        competition.setOwnerId(userId);
        competition.setCreatedOn(System.currentTimeMillis());
        competition.setContentSource("foobar");
        competition.setStatusEnum(CompetitionStatus.PLANNED);
        dboBasicDao.createNew(competition);
    }
    
    @After
    public void after() throws DatastoreException {
        if(dboBasicDao != null) {
        	// delete participant
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("userId", userId);
            params.addValue("compId", compId);
            dboBasicDao.deleteObjectById(ParticipantDBO.class, params);
            
            // delete competition
            params.addValue("id", compId);
            dboBasicDao.deleteObjectById(CompetitionDBO.class, params);
        }
    }
    @Test
    public void testCRD() throws Exception{
        // Initialize a new Participant
        ParticipantDBO participant = new ParticipantDBO();
        participant.setUserId(userId);
        participant.setCompId(compId);
        participant.setCreatedOn(System.currentTimeMillis());
 
        // Create it
        ParticipantDBO clone = dboBasicDao.createNew(participant);
        assertNotNull(clone);
        assertEquals(participant, clone);
        
        // Fetch it
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("compId", compId);
        clone = dboBasicDao.getObjectById(ParticipantDBO.class, params);
        assertNotNull(clone);
        assertEquals(participant.getUserId(), clone.getUserId());
        assertEquals(participant.getCompId(), clone.getCompId());
        
        // Delete it
        boolean result = dboBasicDao.deleteObjectById(ParticipantDBO.class,  params);
        assertTrue("Failed to delete the entry created", result); 
    }
 
}
