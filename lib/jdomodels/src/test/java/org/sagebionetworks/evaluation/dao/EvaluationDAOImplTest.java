package org.sagebionetworks.evaluation.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.EvaluationDAOImpl;
import org.sagebionetworks.evaluation.dbo.EvaluationDBO;
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

public class EvaluationDAOImplTest {
	
	@Autowired
	private EvaluationDAO evaluationDAO;
	
	@Autowired
	private ParticipantDAO participantDAO;
	
	private Evaluation eval;	
	private Participant participant;
	List<String> toDelete;
	
	private static final String EVALUATION_NAME = "test-evaluation";
	private static final String EVALUATION_NAME_2 = "test-evaluation-2";
    private static final Long EVALUATION_OWNER_ID = 0L;
    private static final String EVALUATION_CONTENT_SOURCE = KeyFactory.keyToString(KeyFactory.ROOT_ID);
    
    private static Evaluation newEvaluation(String id, String name, String contentSource, EvaluationStatus status) {
    	Evaluation evaluation = new Evaluation();
    	evaluation.setCreatedOn(new Date());
    	evaluation.setId(id);
    	evaluation.setName(name);
        evaluation.setContentSource(contentSource);
    	evaluation.setStatus(status);
    	return evaluation;
    }

	@Before
	public void setUp() throws Exception {
		toDelete = new ArrayList<String>();
		// Initialize Evaluation
		eval = newEvaluation("123", EVALUATION_NAME, EVALUATION_CONTENT_SOURCE, EvaluationStatus.PLANNED);
	}

	@After
	public void tearDown() throws Exception {
		if(toDelete != null && evaluationDAO != null){
			for(String id: toDelete){
				try {
					evaluationDAO.delete(id);
				} catch (NotFoundException e)  {
					// Already deleted; carry on
				}	
			}
		}
		if (participant!=null && participantDAO!=null) {
			participantDAO.delete(participant.getUserId(), participant.getEvaluationId());
			participant = null;
		}
	}
	
	@Test
	public void testCRUD() throws Exception {        
        // Create it
		long initialCount = evaluationDAO.getCount();
		String evalId = evaluationDAO.create(eval, EVALUATION_OWNER_ID);
		assertNotNull(evalId);
		toDelete.add(evalId);
		
		// Get it
		Evaluation created = evaluationDAO.get(evalId);
		assertEquals(evalId, created.getId());
		assertEquals(EVALUATION_NAME, created.getName());
		assertEquals(EVALUATION_OWNER_ID.toString(), created.getOwnerId());
		assertEquals(EVALUATION_CONTENT_SOURCE, created.getContentSource());
		assertEquals(EvaluationStatus.PLANNED, created.getStatus());
		assertNotNull(created.getEtag());
		assertEquals(1 + initialCount, evaluationDAO.getCount());
		
		// Update it
		created.setName(EVALUATION_NAME_2);
		evaluationDAO.update(created);
		Evaluation updated = evaluationDAO.get(evalId);
		assertEquals(evalId, updated.getId());
		assertFalse("Evaluation name update failed.", eval.getName().equals(updated.getName()));
		assertFalse("eTag was not updated.", created.getEtag().equals(updated.getEtag()));
		
		// Delete it
		assertNotNull(evaluationDAO.get(evalId));
		assertNotNull(evaluationDAO.lookupByName(updated.getName()));
		evaluationDAO.delete(evalId);
		try {
			evaluationDAO.get(evalId);
			fail("found a Evaluation that should have been deleted");
		} catch (NotFoundException e) {
			// Expected
		}
		assertEquals(initialCount, evaluationDAO.getCount());
		assertNull(evaluationDAO.lookupByName(updated.getName()));
	}

	@Test
	public void testFind() throws Exception {        
        // Create it
		String evalId = evaluationDAO.create(eval, EVALUATION_OWNER_ID);
		assertNotNull(evalId);
		toDelete.add(evalId);
		
		// Find it
		assertEquals(evalId, evaluationDAO.lookupByName(EVALUATION_NAME));		
		assertNull(evaluationDAO.lookupByName("" + (new Random()).nextLong()));
	}
	
    @Test
    public void testSameName() throws Exception{ 
        // Create it
		long initialCount = evaluationDAO.getCount();
		String evalId = evaluationDAO.create(eval, EVALUATION_OWNER_ID);
		assertNotNull(evalId);
		toDelete.add(evalId);
		
		// Get it
		Evaluation clone = evaluationDAO.get(evalId);
		assertEquals(evalId, clone.getId());
		assertEquals(EVALUATION_NAME, clone.getName());
		assertEquals(EVALUATION_OWNER_ID.toString(), clone.getOwnerId());
		assertEquals(EVALUATION_CONTENT_SOURCE, clone.getContentSource());
		assertEquals(EvaluationStatus.PLANNED, clone.getStatus());
		assertEquals(1 + initialCount, evaluationDAO.getCount());
		
		// Create clone with same name
		clone.setId(evalId + 1);
        try {
        	evaluationDAO.create(clone, EVALUATION_OWNER_ID);
        	fail("Should not be able to create two Evaluations with the same name");
        } catch (DatastoreException e) {
        	// Expected name conflict
        	assertTrue("Name conflict message should contain the requested name", 
        			e.getMessage().contains(EVALUATION_NAME));
        }
    }
    
    @Test
    public void testGetInRange() throws DatastoreException, NotFoundException {        
        // Create it
		String evalId = evaluationDAO.create(eval, EVALUATION_OWNER_ID);
		assertNotNull(evalId);
		toDelete.add(evalId);
		
		// Get it
		eval = evaluationDAO.get(evalId);
		List<Evaluation> evalList = evaluationDAO.getInRange(10, 0);
		assertEquals(1, evalList.size());
		assertEquals(eval, evalList.get(0));		
    }
    
    @Test
    public void testGetAvailableNoFilter() throws DatastoreException, NotFoundException {        
        // Create it
		String evalId = evaluationDAO.create(eval, EVALUATION_OWNER_ID);
		assertNotNull(evalId);
		toDelete.add(evalId);
		eval = evaluationDAO.get(evalId);
		
		// create another evaluation.  Make sure it doesn't appear in query results
		Evaluation e2 = newEvaluation("456", "rogue", EVALUATION_CONTENT_SOURCE, EvaluationStatus.PLANNED);
		String evalId2 = evaluationDAO.create(e2, 1L);
		assertNotNull(evalId2);
		toDelete.add(evalId2);
		
		// search for it
		// I can find my own evaluation...
		List<Long> pids;
		List<Evaluation> evalList;

		// those who have not joined do not get this result
		long participantId = 0L;
		pids = Arrays.asList(new Long[]{participantId,104L});
		evalList = evaluationDAO.getAvailableInRange(pids, null, 10, 0);
		assertTrue(evalList.isEmpty());
		assertEquals(0L, evaluationDAO.getAvailableCount(pids, null));
		// check that an empty principal list works too
		pids = Arrays.asList(new Long[]{});
		evalList = evaluationDAO.getAvailableInRange(pids, null, 10, 0);
		assertTrue(evalList.isEmpty());
		assertEquals(0L, evaluationDAO.getAvailableCount(pids, null));

		// Now join the Evaluation
		participant = new Participant();
		participant.setCreatedOn(new Date());
		participant.setUserId(""+participantId);
		participant.setEvaluationId(eval.getId());
		participantDAO.create(participant);
		
		// As a participant, I can find:
		pids = Arrays.asList(new Long[]{participantId,104L});
		evalList = evaluationDAO.getAvailableInRange(pids, null, 10, 0);
		assertEquals(1, evalList.size());
		assertEquals(eval, evalList.get(0));
		assertEquals(1L, evaluationDAO.getAvailableCount(pids, null));
		// non-participants  cannot find
		pids = Arrays.asList(new Long[]{110L,111L});
		evalList = evaluationDAO.getAvailableInRange(pids, null, 10, 0);
		assertTrue(evalList.isEmpty());
		assertEquals(0L, evaluationDAO.getAvailableCount(pids, null));
    }
    
    @Test
    public void testGetAvailableStatusFilter() throws DatastoreException, NotFoundException {        
        // Create it
		String evalId = evaluationDAO.create(eval, EVALUATION_OWNER_ID);
		assertNotNull(evalId);
		toDelete.add(evalId);
		eval = evaluationDAO.get(evalId);
		
		// create another evaluation.  Make sure it doesn't appear in query results
		Evaluation e2 = newEvaluation("456", "rogue", EVALUATION_CONTENT_SOURCE, EvaluationStatus.PLANNED);
		String evalId2 = evaluationDAO.create(e2, 1L);
		assertNotNull(evalId2);
		toDelete.add(evalId2);
		
		// search for it
		// I can find my own PLANNED evaluation...
		List<Long> pids;
		List<Evaluation> evalList;
		
		// those who have not joined cannot find it available
		pids = Arrays.asList(new Long[]{});
		evalList = evaluationDAO.getAvailableInRange(pids, EvaluationStatus.PLANNED, 10, 0);
		assertTrue(evalList.isEmpty());
		assertEquals(0L, evaluationDAO.getAvailableCount(pids, EvaluationStatus.PLANNED));
		long participantId = 0L;
		pids = Arrays.asList(new Long[]{participantId,104L});
		evalList = evaluationDAO.getAvailableInRange(pids, EvaluationStatus.PLANNED, 10, 0);
		assertTrue(evalList.isEmpty());
		assertEquals(0L, evaluationDAO.getAvailableCount(pids, EvaluationStatus.PLANNED));
		
		// join the Evaluation
		participant = new Participant();
		participant.setCreatedOn(new Date());
		participant.setUserId(""+participantId);
		participant.setEvaluationId(eval.getId());
		participantDAO.create(participant);
		
		// ... now participants can find it available:
		pids = Arrays.asList(new Long[]{participantId,104L});
		evalList = evaluationDAO.getAvailableInRange(pids, EvaluationStatus.PLANNED, 10, 0);
		assertEquals(1, evalList.size());
		assertEquals(eval, evalList.get(0));
		assertEquals(1L, evaluationDAO.getAvailableCount(pids, EvaluationStatus.PLANNED));
		// but not if they give some other status
		evalList = evaluationDAO.getAvailableInRange(pids, EvaluationStatus.OPEN, 10, 0);
		assertEquals(0, evalList.size());
		assertEquals(0L, evaluationDAO.getAvailableCount(pids, EvaluationStatus.OPEN));

		// non-participants cannot find
		pids = Arrays.asList(new Long[]{110L,111L});
		evalList = evaluationDAO.getAvailableInRange(pids, EvaluationStatus.PLANNED, 10, 0);
		assertTrue(evalList.isEmpty());
		assertEquals(0L, evaluationDAO.getAvailableCount(pids, EvaluationStatus.PLANNED));
    }
    
    @Test
    public void testGetInRangeByStatus() throws DatastoreException, NotFoundException {        
        // Create it
		String evalId = evaluationDAO.create(eval, EVALUATION_OWNER_ID);
		assertNotNull(evalId);
		toDelete.add(evalId);
		
		// Get it
		eval = evaluationDAO.get(evalId);
		List<Evaluation> evalList = evaluationDAO.getInRange(10, 0, EvaluationStatus.PLANNED);
		assertEquals(1, evalList.size());
		assertEquals(eval, evalList.get(0));
		
		// Verify filtering by status
		evalList = evaluationDAO.getInRange(10, 0, EvaluationStatus.OPEN);
		assertEquals(0, evalList.size());
    }
    
	@Test
	public void testCreateFromBackup() throws Exception {        
        // Create it
		eval.setOwnerId(EVALUATION_OWNER_ID.toString());
		eval.setEtag("original-etag");
		String evalId = evaluationDAO.createFromBackup(eval, EVALUATION_OWNER_ID);
		assertNotNull(evalId);
		toDelete.add(evalId);
		
		// Get it
		Evaluation created = evaluationDAO.get(evalId);
		assertEquals(eval, created);
	}
    
    @Test
    public void testDtoToDbo() {
    	Evaluation evalDTO = new Evaluation();
    	Evaluation evalDTOclone = new Evaluation();
    	EvaluationDBO evalDBO = new EvaluationDBO();
    	EvaluationDBO evalDBOclone = new EvaluationDBO();
    	
    	evalDTO.setContentSource("syn123");
    	evalDTO.setCreatedOn(new Date());
    	evalDTO.setDescription("description");
    	evalDTO.setEtag("eTag");
    	evalDTO.setId("123");
    	evalDTO.setName("name");
    	evalDTO.setOwnerId("456");
    	evalDTO.setStatus(EvaluationStatus.OPEN);
    	    	
    	EvaluationDAOImpl.copyDtoToDbo(evalDTO, evalDBO);
    	EvaluationDAOImpl.copyDboToDto(evalDBO, evalDTOclone);
    	EvaluationDAOImpl.copyDtoToDbo(evalDTOclone, evalDBOclone);
    	
    	assertEquals(evalDTO, evalDTOclone);
    	assertEquals(evalDBO, evalDBOclone);
    }
}
