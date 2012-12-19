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
        submissionId = submissionDAO.create(submission).getId();
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
    public void testCRD() throws Exception{
        // Initialize a new SubmissionStatus object for submissionId
        SubmissionStatus status = new SubmissionStatus();
        status.setId(submissionId);
        status.setEtag(null);
        status.setStatus(SubmissionStatusEnum.OPEN);
        status.setScore(0L);
        
        // Create it
        SubmissionStatus clone = submissionStatusDAO.create(status);
        assertNotNull(clone);
        assertNotNull(clone.getModifiedOn());
        status.setModifiedOn(clone.getModifiedOn());
        assertNotNull(clone.getEtag());
        status.setEtag(clone.getEtag());
        assertEquals(status, clone);
        
        // Fetch it
        SubmissionStatus clone2 = submissionStatusDAO.get(submissionId);
        assertEquals(status, clone2);
        
        // Update it
        clone2.setStatus(SubmissionStatusEnum.SCORED);
        clone2.setScore(100L);
        Thread.sleep(1L);
        submissionStatusDAO.update(clone2);
        SubmissionStatus clone3 = submissionStatusDAO.get(submissionId);        
        assertFalse("Modified date was not updated", clone2.getModifiedOn().equals(clone3.getModifiedOn()));
        clone2.setModifiedOn(clone3.getModifiedOn());
        assertEquals(clone2, clone3);

    	// Delete it
        submissionStatusDAO.delete(submissionId);
        
        // Fetch it (should not exist)
        try {
        	status = submissionStatusDAO.get(submissionId);
        } catch (NotFoundException e) {
        	// expected
        }
    }
}
