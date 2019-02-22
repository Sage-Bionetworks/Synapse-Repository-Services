package org.sagebionetworks.evaluation.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.dbo.EvaluationDBO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.evaluation.EvaluationDAO;
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
	private AccessControlListDAO aclDAO;
	
	private Evaluation eval;	
	private AccessControlList aclToDelete = null;

	List<String> toDelete;
	
	private static final String EVALUATION_NAME = "test-evaluation";
	private static final String EVALUATION_NAME_2 = "test-evaluation-2";
    private static final Long EVALUATION_OWNER_ID = 1L;
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
		aclToDelete = null;
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
		if (aclToDelete!=null && aclDAO!=null) {
			aclDAO.delete(aclToDelete.getId(), ObjectType.EVALUATION);
			aclToDelete = null;
		}
	}
	
	@Test
	public void testCRUD() throws Exception {        
        // Create it
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
		assertNull(evaluationDAO.lookupByName(updated.getName()));
	}
	
	@Test
	public void testgetAccessibleEvaluationsForProject() throws Exception {
		List<Long> principalIds = Collections.singletonList(EVALUATION_OWNER_ID);
		// Get nothing
		List<Evaluation> retrieved = evaluationDAO.getAccessibleEvaluationsForProject(EVALUATION_CONTENT_SOURCE, principalIds, ACCESS_TYPE.READ, 10, 0);
		assertEquals(0, retrieved.size());
		
		// Create one
		String evalId = evaluationDAO.create(eval, EVALUATION_OWNER_ID);
		assertNotNull(evalId);
		toDelete.add(evalId);
		
		// no permission to access
		retrieved = evaluationDAO.getAccessibleEvaluationsForProject(EVALUATION_CONTENT_SOURCE, principalIds, ACCESS_TYPE.READ, 10, 0);
		assertEquals(0, retrieved.size());

		// now provide the permission to READ
		AccessControlList acl = Util.createACL(evalId, EVALUATION_OWNER_ID, Collections.singleton(ACCESS_TYPE.READ), new Date());

		String aclId = aclDAO.create(acl, ObjectType.EVALUATION);
		acl.setId(aclId);
		aclToDelete = acl;
		
		// Get it
		retrieved = evaluationDAO.getAccessibleEvaluationsForProject(EVALUATION_CONTENT_SOURCE, principalIds, ACCESS_TYPE.READ, 10, 0);
		assertEquals(1, retrieved.size());
		
		Evaluation created = retrieved.get(0);
		assertEquals(evalId, created.getId());
		assertEquals(EVALUATION_NAME, created.getName());
		assertEquals(EVALUATION_OWNER_ID.toString(), created.getOwnerId());
		assertEquals(EVALUATION_CONTENT_SOURCE, created.getContentSource());
		assertEquals(EvaluationStatus.PLANNED, created.getStatus());
		assertNotNull(created.getEtag());
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
		
		// Create clone with same name
		clone.setId(evalId + 1);
        try {
        	evaluationDAO.create(clone, EVALUATION_OWNER_ID);
        	fail("Should not be able to create two Evaluations with the same name");
        } catch (NameConflictException e) {
        	// Expected name conflict
        	assertTrue("Name conflict message should contain the requested name", 
        			e.getMessage().contains(EVALUATION_NAME));
        }
    }
    
    @Test
    public void testGetAvailable() throws DatastoreException, NotFoundException {        
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
		long participantId = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
		pids = Arrays.asList(new Long[]{participantId,104L});
		evalList = evaluationDAO.getAccessibleEvaluations(pids, ACCESS_TYPE.SUBMIT, 10, 0, null);
		assertTrue(evalList.isEmpty());
		// check that an empty principal list works too
		pids = Arrays.asList(new Long[]{});
		evalList = evaluationDAO.getAccessibleEvaluations(pids, ACCESS_TYPE.SUBMIT, 10, 0, null);
		assertTrue(evalList.isEmpty());
		// check that the filter works
		evalList = evaluationDAO.getAccessibleEvaluations(pids, ACCESS_TYPE.SUBMIT, 10, 0, Arrays.asList(new Long[]{Long.parseLong(evalId)}));
		assertTrue(evalList.isEmpty());

		// Now join the Evaluation by
		// adding 'participantId' into the ACL with SUBMIT permission
		AccessControlList acl = Util.createACL(eval.getId(), participantId, Collections.singleton(ACCESS_TYPE.SUBMIT), new Date());
		String aclId = aclDAO.create(acl, ObjectType.EVALUATION);
		acl.setId(aclId);
		aclToDelete = acl;
		
		// As a participant, I can find:
		pids = Arrays.asList(new Long[]{participantId,104L});
		evalList = evaluationDAO.getAccessibleEvaluations(pids, ACCESS_TYPE.SUBMIT, 10, 0, null);
		assertEquals(1, evalList.size());
		assertEquals(eval, evalList.get(0));
		// make sure filter works
		evalList = evaluationDAO.getAccessibleEvaluations(pids, ACCESS_TYPE.SUBMIT, 10, 0, Arrays.asList(new Long[]{Long.parseLong(evalId)}));
		assertEquals(1, evalList.size());
		assertEquals(eval, evalList.get(0));
		// filtering with 'eval 2' causes no results to come back
		evalList = evaluationDAO.getAccessibleEvaluations(pids, ACCESS_TYPE.SUBMIT, 10, 0, Arrays.asList(new Long[]{Long.parseLong(evalId2)}));
		assertEquals(0, evalList.size());
		// non-participants  cannot find
		pids = Arrays.asList(new Long[]{110L,111L});
		evalList = evaluationDAO.getAccessibleEvaluations(pids, ACCESS_TYPE.SUBMIT, 10, 0, null);
		assertTrue(evalList.isEmpty());
		
		// PLFM-2312 problem with repeated entries
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId());
		ra.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{ACCESS_TYPE.SUBMIT})));
		Set<ResourceAccess> ras = acl.getResourceAccess();
		ras.add(ra);
		aclDAO.update(acl, ObjectType.EVALUATION);
		// should still find just one result, even though I'm in the ACL twice
		pids = Arrays.asList(new Long[] {
				participantId,
				BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId() });
		evalList = evaluationDAO.getAccessibleEvaluations(pids, ACCESS_TYPE.SUBMIT, 10, 0, null);
		assertEquals(1, evalList.size());
		assertEquals(eval, evalList.get(0));
		
		
		// Note:  The evaluation isn't returned for the wrong access type
		assertFalse(evaluationDAO.getAccessibleEvaluations(pids, ACCESS_TYPE.SUBMIT, 10, 0, null).isEmpty());
		assertTrue(evaluationDAO.getAccessibleEvaluations(pids, ACCESS_TYPE.READ, 10, 0, null).isEmpty());
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
