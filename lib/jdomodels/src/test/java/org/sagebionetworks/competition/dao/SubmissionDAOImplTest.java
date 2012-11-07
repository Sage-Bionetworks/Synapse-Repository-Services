package org.sagebionetworks.competition.dao;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.competition.model.Submission;
import org.sagebionetworks.competition.model.SubmissionStatus;
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
    CompetitionDAO competitionDAO;
	@Autowired
	NodeDAO nodeDAO;
 
	private String nodeId = null;
    private String submissionId = null;
    private String userId = "0";
    private String compId = "2";
    private String name = "test submission";
    private Long score = 0L;
    
    @Before
    public void setUp() throws DatastoreException, InvalidModelException, NotFoundException {
    	// create a node
  		Node toCreate = NodeTestUtils.createNew(name, Long.parseLong(userId));
    	toCreate.setVersionComment("This is the first version of the first node ever!");
    	toCreate.setVersionLabel("0.0.1");
    	nodeId = nodeDAO.createNew(toCreate).substring(3); // trim "syn" from node ID
    	
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
    		nodeDAO.delete(nodeId);
    	} catch (NotFoundException e) {};
		try {
			submissionDAO.delete(submissionId);
		} catch (NotFoundException e)  {};
		try {
			competitionDAO.delete(compId);
		} catch (NotFoundException e) {};
    }
    
    @Test
    public void testCRUD() throws Exception{
        // Initialize a new Submission
        Submission submission = new Submission();
        submission.setId(submissionId);
        submission.setName(name);
        submission.setEntityId(nodeId);
        submission.setStatus(SubmissionStatus.OPEN);
        submission.setUserId(userId);
        submission.setCompetitionId(compId);
        submission.setScore(score);
 
        // Create it
        Submission clone = submissionDAO.create(submission);
        assertNotNull(clone);        
        // copy the generated id
        submissionId = clone.getId();
        assertNotNull(submissionId);  
        submission.setId(submissionId);
        // copy the generated timestamp
        submission.setCreatedOn(clone.getCreatedOn());
        assertEquals(submission, clone);
        
        // Fetch it
        Submission clone2 = submissionDAO.get(submissionId);
        assertNotNull(clone2);
        assertEquals(submission, clone2);
        
		// Update it
        Long newScore = score + 100;
		clone.setScore(newScore);
		submissionDAO.update(clone);
		
		// Verify it
		Submission clone3 = submissionDAO.get(submissionId);
		assertEquals(clone, clone3);
		assertFalse(clone3.getScore().equals(clone2.getScore()));
        
        // Delete it
        submissionDAO.delete(submissionId);
        try {
        	clone = submissionDAO.get(submissionId);
        } catch (NotFoundException e) {
        	// expected
        	return;
        }
        fail("Failed to delete Participant");
    }
 
}
