package org.sagebionetworks.competition.dao;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.competition.dbo.ParticipantDBO;
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
    private String compId1;
    private String compId2;
    private Participant part1;
    private Participant part2;
    
    @Before
    public void setUp() {    	
    	// create and persist competitions
        Competition competition = new Competition();
        competition.setEtag("etag");
        competition.setName("name");
        competition.setCreatedOn(new Date());
        competition.setContentSource("foobar");
        competition.setStatus(CompetitionStatus.PLANNED);
        compId1 = competitionDAO.create(competition, userId);
        competition.setName("name2");
        competition.setStatus(CompetitionStatus.CLOSED);
        compId2 = competitionDAO.create(competition, userId);        
        
        // initialize Participants
        part1 = new Participant();
        part1.setUserId(userId);
        part1.setCompetitionId(compId1);        
        part2 = new Participant();
        part2.setUserId(userId);
        part2.setCompetitionId(compId2);
    }
    
    @After
    public void after() throws DatastoreException {
		try {
			participantDAO.delete(userId, compId1);
		} catch (NotFoundException e)  {};
		try {
			participantDAO.delete(userId, compId2);
		} catch (NotFoundException e)  {};
		try {
			competitionDAO.delete(compId1);
		} catch (NotFoundException e) {};
		try {
			competitionDAO.delete(compId2);
		} catch (NotFoundException e) {};
    }
    
    @Test
    public void testCRD() throws Exception{ 
        // Create and fetch Participant
        participantDAO.create(part1);
        Participant clone = participantDAO.get(userId, compId1);
        part1.setCreatedOn(clone.getCreatedOn());
        assertNotNull(clone);
        assertEquals(part1, clone);
        
        // Delete it
        participantDAO.delete(userId, compId1);
        try {
        	clone = participantDAO.get(userId, compId1);
        } catch (NotFoundException e) {
        	// expected
        	return;
        }
        fail("Failed to delete Participant");
    }
    
    @Test
    public void testGetByCompetition() throws Exception{
        // Create two participants
        participantDAO.create(part1);
        participantDAO.create(part2);
        
        List<Participant> parts = participantDAO.getAllByCompetition(compId1);
        assertEquals(1, parts.size());
        for (Participant p : parts) {
        	assertNotNull(p.getCreatedOn());
        	p.setCreatedOn(part1.getCreatedOn());
        	assertEquals(part1, p);
        }
    }
    
    @Test
    public void testGetCountByCompetition() throws Exception {
    	assertEquals(0, participantDAO.getCountByCompetition(compId1));
    	assertEquals(0, participantDAO.getCountByCompetition(compId2));
    	
        // create Participant for comp1
        participantDAO.create(part1);
        assertEquals(1, participantDAO.getCountByCompetition(compId1));
    	assertEquals(0, participantDAO.getCountByCompetition(compId2));
        
        // create Participant for comp2
        participantDAO.create(part2);
        assertEquals(1, participantDAO.getCountByCompetition(compId1));
    	assertEquals(1, participantDAO.getCountByCompetition(compId2));
        
        // delete Participant for comp1
    	participantDAO.delete(userId, compId1);
    	assertEquals(0, participantDAO.getCountByCompetition(compId1));
    	assertEquals(1, participantDAO.getCountByCompetition(compId2));
    	
    	// delete Participant for comp2
    	participantDAO.delete(userId, compId2);
    	assertEquals(0, participantDAO.getCountByCompetition(compId1));
    	assertEquals(0, participantDAO.getCountByCompetition(compId2));
    }
    
    @Test
    public void testGetInRange() throws DatastoreException, NotFoundException {
        // Create and fetch Participant
        participantDAO.create(part1);
        participantDAO.create(part2);
        part1 = participantDAO.get(part1.getUserId(), part1.getCompetitionId());
        part2 = participantDAO.get(part2.getUserId(), part2.getCompetitionId());
        List<Participant> partList = participantDAO.getInRange(10, 0);
        assertEquals(2, partList.size());
        for (Participant p : partList) {
        	assertTrue("Unknown Participant returned", part1.equals(p) || part2.equals(p));
        }
    }
    
    @Test
    public void testDtoToDbo() {
    	Participant partDTO = new Participant();
    	Participant partDTOclone = new Participant();
    	ParticipantDBO partDBO = new ParticipantDBO();
    	ParticipantDBO partDBOclone = new ParticipantDBO();
    	
    	partDTO.setCompetitionId("123");
    	partDTO.setCreatedOn(new Date());
    	partDTO.setUserId("456");
    	    	
    	ParticipantDAOImpl.copyDtoToDbo(partDTO, partDBO);
    	ParticipantDAOImpl.copyDboToDto(partDBO, partDTOclone);
    	ParticipantDAOImpl.copyDtoToDbo(partDTOclone, partDBOclone);
    	
    	assertEquals(partDTO, partDTOclone);
    	assertEquals(partDBO, partDBOclone);
    }
 
}
