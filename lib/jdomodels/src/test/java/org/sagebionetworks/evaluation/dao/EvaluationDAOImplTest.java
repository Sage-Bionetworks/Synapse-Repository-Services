package org.sagebionetworks.evaluation.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
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
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })

public class EvaluationDAOImplTest {
	
	@Autowired
	private EvaluationDAO evaluationDAO;
	
	private Evaluation eval;	
	List<String> toDelete;
	
	private static final String EVALUATION_NAME = "test-evaluation";
	private static final String EVALUATION_NAME_2 = "test-evaluation-2";
    private static final Long EVALUATION_OWNER_ID = 0L;
    private static final String EVALUATION_CONTENT_SOURCE = "Baz";

	@Before
	public void setUp() throws Exception {
		toDelete = new ArrayList<String>();
		// Initialize Evaluation
		eval = new Evaluation();
		eval.setCreatedOn(new Date());
		eval.setId("123");
		eval.setName(EVALUATION_NAME);
        eval.setContentSource(EVALUATION_CONTENT_SOURCE);
        eval.setStatus(EvaluationStatus.PLANNED);
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
    public void testDtoToDbo() {
    	Evaluation evalDTO = new Evaluation();
    	Evaluation evalDTOclone = new Evaluation();
    	EvaluationDBO evalDBO = new EvaluationDBO();
    	EvaluationDBO evalDBOclone = new EvaluationDBO();
    	
    	evalDTO.setContentSource("contentSource");
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
