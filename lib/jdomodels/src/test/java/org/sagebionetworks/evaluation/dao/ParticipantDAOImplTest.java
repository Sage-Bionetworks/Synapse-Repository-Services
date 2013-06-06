package org.sagebionetworks.evaluation.dao;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.ParticipantDAO;
import org.sagebionetworks.evaluation.dao.ParticipantDAOImpl;
import org.sagebionetworks.evaluation.dbo.ParticipantDBO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
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
    private EvaluationDAO evaluationDAO;
       
    private Long principalId = 0L;
    private String principalId_str = principalId.toString();
    private String evalId1;
    private String evalId2;
    private Participant part1;
    private Participant part2;
    
    @Before
    public void setUp() {    	
    	// create and persist Evaluations
        Evaluation evaluation = new Evaluation();
        evaluation.setId("1234");
        evaluation.setEtag("etag");
        evaluation.setName("name");
        evaluation.setCreatedOn(new Date());
        evaluation.setContentSource(KeyFactory.SYN_ROOT_ID);
        evaluation.setStatus(EvaluationStatus.PLANNED);
        evalId1 = evaluationDAO.create(evaluation, principalId);
        evaluation.setName("name2");
        evaluation.setId("5678");
        evaluation.setStatus(EvaluationStatus.CLOSED);
        evalId2 = evaluationDAO.create(evaluation, principalId);        
        
        // initialize Participants
        part1 = new Participant();
        part1.setUserId(principalId_str);
        part1.setEvaluationId(evalId1);
        part1.setCreatedOn(new Date());
        part2 = new Participant();
        part2.setUserId(principalId_str);
        part2.setEvaluationId(evalId2);
        part2.setCreatedOn(new Date());
    }
    
    @After
    public void tearDown() throws DatastoreException {
		try {
			participantDAO.delete(principalId_str, evalId1);
		} catch (NotFoundException e)  {};
		try {
			participantDAO.delete(principalId_str, evalId2);
		} catch (NotFoundException e)  {};
		try {
			evaluationDAO.delete(evalId1);
		} catch (NotFoundException e) {};
		try {
			evaluationDAO.delete(evalId2);
		} catch (NotFoundException e) {};
    }
    
    @Test
    public void testCRD() throws Exception{ 
        // Create and fetch Participant
        participantDAO.create(part1);
        Participant clone = participantDAO.get(principalId_str, evalId1);
        part1.setCreatedOn(clone.getCreatedOn());
        assertNotNull(clone);
        assertEquals(part1, clone);
        
        // Delete it
        participantDAO.delete(principalId_str, evalId1);
        try {
        	clone = participantDAO.get(principalId_str, evalId1);
        } catch (NotFoundException e) {
        	// expected
        	return;
        }
        fail("Failed to delete Participant");
    }
    
    @Test
    public void testGetByEvaluation() throws Exception{
        // Create two participants
        participantDAO.create(part1);
        participantDAO.create(part2);
        
        List<Participant> parts = participantDAO.getAllByEvaluation(evalId1, 10, 0);
        assertEquals(1, parts.size());
        for (Participant p : parts) {
        	assertNotNull(p.getCreatedOn());
        	p.setCreatedOn(part1.getCreatedOn());
        	assertEquals(part1, p);
        }
    }
    
    @Test
    public void testGetCountByEvaluation() throws Exception {
    	assertEquals(0, participantDAO.getCountByEvaluation(evalId1));
    	assertEquals(0, participantDAO.getCountByEvaluation(evalId2));
    	
        // create Participant for eval1
        participantDAO.create(part1);
        assertEquals(1, participantDAO.getCountByEvaluation(evalId1));
    	assertEquals(0, participantDAO.getCountByEvaluation(evalId2));
        
        // create Participant for eval2
        participantDAO.create(part2);
        assertEquals(1, participantDAO.getCountByEvaluation(evalId1));
    	assertEquals(1, participantDAO.getCountByEvaluation(evalId2));
        
        // delete Participant for eval1
    	participantDAO.delete(principalId_str, evalId1);
    	assertEquals(0, participantDAO.getCountByEvaluation(evalId1));
    	assertEquals(1, participantDAO.getCountByEvaluation(evalId2));
    	
    	// delete Participant for eval2
    	participantDAO.delete(principalId_str, evalId2);
    	assertEquals(0, participantDAO.getCountByEvaluation(evalId1));
    	assertEquals(0, participantDAO.getCountByEvaluation(evalId2));
    }
    
    @Test
    public void testGetInRange() throws DatastoreException, NotFoundException {
        // Create and fetch Participant
        participantDAO.create(part1);
        participantDAO.create(part2);
        part1 = participantDAO.get(part1.getUserId(), part1.getEvaluationId());
        part2 = participantDAO.get(part2.getUserId(), part2.getEvaluationId());
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
    	
    	partDTO.setEvaluationId("123");
    	partDTO.setCreatedOn(new Date());
    	partDTO.setUserId("456");
    	    	
    	ParticipantDAOImpl.copyDtoToDbo(partDTO, partDBO);
    	ParticipantDAOImpl.copyDboToDto(partDBO, partDTOclone);
    	ParticipantDAOImpl.copyDtoToDbo(partDTOclone, partDBOclone);
    	
    	assertEquals(partDTO, partDTOclone);
    	assertEquals(partDBO, partDBOclone);
    }
 
}
