package org.sagebionetworks.competition.dao;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.competition.dbo.SubmissionDBO;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.competition.model.Submission;
import org.sagebionetworks.competition.model.SubmissionStatus;
import org.sagebionetworks.competition.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class SubmissionDAOImplTest {
 
    @Autowired
    SubmissionDAO submissionDAO;
    @Autowired
    SubmissionStatusDAO submissionStatusDAO;
    @Autowired
    ParticipantDAO participantDAO;
    @Autowired
    CompetitionDAO competitionDAO;
	@Autowired
	NodeDAO nodeDAO;
 
	private String nodeId = null;
    private String submissionId = null;
    private String userId = "0";
    private String userId_does_not_exist = "2";
    private String compId;
    private String compId_does_not_exist = "456";
    private String name = "test submission";
    private Long versionNumber = 1L;
    private Submission submission;
    
    @Before
    public void setUp() throws DatastoreException, InvalidModelException, NotFoundException {
    	// create a node
  		Node toCreate = NodeTestUtils.createNew(name, Long.parseLong(userId));
    	toCreate.setVersionComment("This is the first version of the first node ever!");
    	toCreate.setVersionLabel("0.0.1");
    	nodeId = nodeDAO.createNew(toCreate).substring(3); // trim "syn" from node ID
    	
    	// create a Competition
        Competition competition = new Competition();
        competition.setEtag("etag");
        competition.setName("name");
        competition.setOwnerId(userId);
        competition.setCreatedOn(new Date());
        competition.setContentSource("foobar");
        competition.setStatus(CompetitionStatus.PLANNED);
        compId = competitionDAO.create(competition, Long.parseLong(userId));
        
        // create a Participant
        Participant participant = new Participant();
        participant.setUserId(userId);
        participant.setCompetitionId(compId);
        participantDAO.create(participant);
        
        // Initialize a Submission
        submission = new Submission();
        submission.setId(submissionId);
        submission.setName(name);
        submission.setCompetitionId(compId);
        submission.setEntityId(nodeId);
        submission.setVersionNumber(versionNumber);
        submission.setUserId(userId);
    }
    
    @After
    public void tearDown() throws DatastoreException {
    	try {
    		nodeDAO.delete(nodeId);
    	} catch (NotFoundException e) {};
		try {
			submissionDAO.delete(submissionId);
		} catch (NotFoundException e)  {};
		try {
			participantDAO.delete(userId, compId);
		} catch (NotFoundException e) {};
		try {
			competitionDAO.delete(compId);
		} catch (NotFoundException e) {};
    }
    
    @Test
    public void testCRD() throws Exception{
        long initialCount = submissionDAO.getCount();
 
        // create Submission
        submissionId = submissionDAO.create(submission);
        assertNotNull(submissionId);   
        
        // fetch it
        Submission clone = submissionDAO.get(submissionId);
        assertNotNull(clone);
        submission.setId(submissionId);
        submission.setCreatedOn(clone.getCreatedOn());
        assertEquals(initialCount + 1, submissionDAO.getCount());
        assertEquals(submission, clone);
        
        // delete it
        submissionDAO.delete(submissionId);
        try {
        	clone = submissionDAO.get(submissionId);
        } catch (NotFoundException e) {
        	// expected
        	assertEquals(initialCount, submissionDAO.getCount());
        	return;
        }
        fail("Failed to delete Participant");
    }
    
    @Test
    public void testGetAllByUser() throws DatastoreException, NotFoundException {
    	submissionId = submissionDAO.create(submission);
    	
    	// userId should have submissions
    	List<Submission> subs = submissionDAO.getAllByUser(userId);
    	assertEquals(1, subs.size());
    	submission.setCreatedOn(subs.get(0).getCreatedOn());
    	submission.setId(subs.get(0).getId());
    	assertEquals(subs.get(0), submission);
    	
    	// userId_does_not_exist should not have any submissions
    	subs = submissionDAO.getAllByUser(userId_does_not_exist);
    	assertEquals(0, subs.size());
    }
    
    @Test
    public void testGetAllByCompetition() throws DatastoreException, NotFoundException {
    	submissionId = submissionDAO.create(submission);
    	
    	// compId should have submissions
    	List<Submission> subs = submissionDAO.getAllByCompetition(compId);
    	assertEquals(1, subs.size());
    	assertEquals(1, submissionDAO.getCountByCompetition(compId));
    	submission.setCreatedOn(subs.get(0).getCreatedOn());
    	submission.setId(subs.get(0).getId());
    	assertEquals(subs.get(0), submission);
    	
    	// compId_does_not_exist should not have any submissions
    	subs = submissionDAO.getAllByCompetition(compId_does_not_exist);
    	assertEquals(0, subs.size());
    	assertEquals(0, submissionDAO.getCountByCompetition(compId_does_not_exist));
    }
    
    @Test
    public void testGetAllByCompetitionAndStatus() throws DatastoreException, NotFoundException {
    	submissionId = submissionDAO.create(submission);
    	
    	// create a SubmissionStatus object
    	SubmissionStatus subStatus = new SubmissionStatus();
    	subStatus.setId(submissionId);
    	subStatus.setStatus(SubmissionStatusEnum.OPEN);
    	submissionStatusDAO.create(subStatus);
    	
    	// hit compId and hit status => should find 1 submission
    	List<Submission> subs = submissionDAO.getAllByCompetitionAndStatus(compId, SubmissionStatusEnum.OPEN);
    	assertEquals(1, subs.size());
    	submission.setCreatedOn(subs.get(0).getCreatedOn());
    	submission.setId(subs.get(0).getId());
    	assertEquals(subs.get(0), submission);
    	
    	// miss compId and hit status => should find 0 submissions
    	subs = submissionDAO.getAllByCompetitionAndStatus(compId_does_not_exist, SubmissionStatusEnum.OPEN);
    	assertEquals(0, subs.size());
    	
    	// hit compId and miss status => should find 0 submissions
    	subs = submissionDAO.getAllByCompetitionAndStatus(compId, SubmissionStatusEnum.CLOSED);
    	assertEquals(0, subs.size());
    }
    
    @Test
    public void testDtoToDbo() {
    	Submission compDTO = new Submission();
    	Submission compDTOclone = new Submission();
    	SubmissionDBO compDBO = new SubmissionDBO();
    	SubmissionDBO compDBOclone = new SubmissionDBO();
    	
    	compDTO.setCompetitionId("123");
    	compDTO.setCreatedOn(new Date());
    	compDTO.setEntityId("456");
    	compDTO.setId("789");
    	compDTO.setName("name");
    	compDTO.setUserId("42");
    	compDTO.setVersionNumber(1L);
    	    	
    	SubmissionDAOImpl.copyDtoToDbo(compDTO, compDBO);
    	SubmissionDAOImpl.copyDboToDto(compDBO, compDTOclone);
    	SubmissionDAOImpl.copyDtoToDbo(compDTOclone, compDBOclone);
    	
    	assertEquals(compDTO, compDTOclone);
    	assertEquals(compDBO, compDBOclone);
    }
}
