package org.sagebionetworks.competition.dao;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.competition.dbo.SubmissionStatusDBO;
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
public class SubmissionStatusDAOImplTest {
 
	@Autowired
	SubmissionStatusDAO submissionStatusDAO;
    @Autowired
    SubmissionDAO submissionDAO;
    @Autowired
    ParticipantDAO participantDAO;
    @Autowired
    CompetitionDAO competitionDAO;
	@Autowired
	NodeDAO nodeDAO;
 
	private String nodeId = null;
    private String submissionId = null;
    private String userId = "0";
    private String compId;
    private String name = "test submission";
    private Long versionNumber = 1L;
    
    @Before
    public void setUp() throws DatastoreException, InvalidModelException, NotFoundException {
    	// create a node
  		Node toCreate = NodeTestUtils.createNew(name, Long.parseLong(userId));
    	toCreate.setVersionComment("This is the first version of the first node ever!");
    	toCreate.setVersionLabel("0.0.1");
    	nodeId = nodeDAO.createNew(toCreate).substring(3); // trim "syn" from node ID
    	
    	// create a competition
        Competition competition = new Competition();
        competition.setEtag("etag");
        competition.setName("name");
        competition.setOwnerId(userId);
        competition.setCreatedOn(new Date());
        competition.setContentSource("foobar");
        competition.setStatus(CompetitionStatus.PLANNED);
        compId = competitionDAO.create(competition, userId);
        
        // create a participant
        Participant participant = new Participant();
        participant.setUserId(userId);
        participant.setCompetitionId(compId);
        participantDAO.create(participant);
        
        // create a submission
        Submission submission = new Submission();
        submission.setName(name);
        submission.setEntityId(nodeId);
        submission.setVersionNumber(versionNumber);
        submission.setUserId(userId);
        submission.setCompetitionId(compId);
        submission.setCreatedOn(new Date());
        submissionId = submissionDAO.create(submission);
    }
    
    @After
    public void after() throws DatastoreException {
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
    public void testCRUD() throws Exception{
        // Initialize a new SubmissionStatus object for submissionId
        SubmissionStatus status = new SubmissionStatus();
        status.setId(submissionId);
        status.setEtag(null);
        status.setStatus(SubmissionStatusEnum.OPEN);
        status.setScore(0L);
        long initialCount = submissionStatusDAO.getCount();
        
        // Create it
        submissionStatusDAO.create(status);
        assertEquals(initialCount + 1, submissionStatusDAO.getCount());
        
        // Fetch it
        SubmissionStatus clone = submissionStatusDAO.get(submissionId);
        assertNotNull(clone);
        assertNotNull(clone.getModifiedOn());        
        assertNotNull(clone.getEtag());
        status.setModifiedOn(clone.getModifiedOn());
        status.setEtag(clone.getEtag());
        assertEquals(status, clone);
        
        // Update it
        clone.setStatus(SubmissionStatusEnum.SCORED);
        clone.setScore(100L);
        Thread.sleep(1L);
        submissionStatusDAO.update(clone);
        SubmissionStatus clone2 = submissionStatusDAO.get(submissionId);
        assertFalse("eTag was not updated.", clone.getEtag().equals(clone2.getEtag()));
        assertFalse("Modified date was not updated", clone.getModifiedOn().equals(clone2.getModifiedOn()));
        clone.setModifiedOn(clone2.getModifiedOn());
        clone.setEtag(clone2.getEtag());
        assertEquals(clone, clone2);

    	// Delete it
        submissionStatusDAO.delete(submissionId);
        assertEquals(initialCount, submissionStatusDAO.getCount());
        
        // Fetch it (should not exist)
        try {
        	status = submissionStatusDAO.get(submissionId);
        } catch (NotFoundException e) {
        	// expected
        }
    }
    
    @Test
    public void testDtoToDbo() {
    	SubmissionStatus subStatusDTO = new SubmissionStatus();
    	SubmissionStatus subStatusDTOclone = new SubmissionStatus();
    	SubmissionStatusDBO subStatusDBO = new SubmissionStatusDBO();
    	SubmissionStatusDBO subStatusDBOclone = new SubmissionStatusDBO();
    	
    	subStatusDTO.setEtag("eTag");
    	subStatusDTO.setId("123");
    	subStatusDTO.setModifiedOn(new Date());
    	subStatusDTO.setScore(42L);
    	subStatusDTO.setStatus(SubmissionStatusEnum.CLOSED);
    	    	
    	SubmissionStatusDAOImpl.copyDtoToDbo(subStatusDTO, subStatusDBO);
    	SubmissionStatusDAOImpl.copyDboToDto(subStatusDBO, subStatusDTOclone);
    	SubmissionStatusDAOImpl.copyDtoToDbo(subStatusDTOclone, subStatusDBOclone);
    	
    	assertEquals(subStatusDTO, subStatusDTOclone);
    	assertEquals(subStatusDBO, subStatusDBOclone);
    }
}
