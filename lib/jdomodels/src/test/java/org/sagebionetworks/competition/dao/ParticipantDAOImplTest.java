package org.sagebionetworks.competition.dao;

import static org.junit.Assert.*;

import java.util.Date;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class ParticipantDAOImplTest {
 
    @Autowired
    private ParticipantDAO participantDAO;
    @Autowired
    private CompetitionDAO competitionDAO;
        
    private String userId = "0";
    private String compId = "2";
    
    @Before
    public void setUp() {    	
    	// create a competition
        Competition competition = new Competition();
        competition.setId(compId);
        competition.setEtag("etag");
        competition.setName("name");
        competition.setOwnerId(userId);
        competition.setCreatedOn(new Date());
        competition.setContentSource("foobar");
        competition.setStatus(CompetitionStatus.PLANNED);
        competitionDAO.create(competition);
    }
    
    @After
    public void after() throws DatastoreException {
		try {
			participantDAO.delete(userId, compId);
		} catch (NotFoundException e)  {};
		try {
			competitionDAO.delete(compId);
		} catch (NotFoundException e) {};
    }
    
    @Test
    public void testCRD() throws Exception{
        // Initialize a new Participant
        Participant participant = new Participant();
        participant.setUserId(userId);
        participant.setCompetitionId(compId);
 
        // Create and fetch it
        participantDAO.create(participant);
        Participant clone = participantDAO.get(userId, compId);
        participant.setCreatedOn(clone.getCreatedOn());
        assertNotNull(clone);
        assertEquals(participant, clone);
        
        // Delete it
        participantDAO.delete(userId, compId);
        try {
        	clone = participantDAO.get(userId, compId);
        } catch (NotFoundException e) {
        	// expected
        	return;
        }
        fail("Failed to delete Participant");
    }
 
}
