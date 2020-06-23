package org.sagebionetworks.evaluation.dao;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.SubmissionQuota;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;



@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class EvaluationDAOImplTest {
	
	@Autowired
	private EvaluationDAO evaluationDAO;
	
	@Autowired
	private AccessControlListDAO aclDAO;
	
	private Evaluation eval;
	private AccessControlList aclToDelete = null;
	private Long futureTime;

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
    	SubmissionQuota quota = new SubmissionQuota();
    	quota.setFirstRoundStart(new Date(System.currentTimeMillis()-10L)); // started slightly in the past
    	quota.setNumberOfRounds(10L);
    	quota.setRoundDurationMillis(60000L); // one minute
    	quota.setSubmissionLimit(10L); // the challenge ends after ten minutes
    	evaluation.setQuota(quota);
    	return evaluation;
    }

	@BeforeEach
	public void setUp() throws Exception {
		toDelete = new ArrayList<String>();
		// Initialize Evaluation
		eval = newEvaluation("123", EVALUATION_NAME, EVALUATION_CONTENT_SOURCE, EvaluationStatus.PLANNED);
		aclToDelete = null;
		futureTime = System.currentTimeMillis()+60000L*1000L; // 1000 minutes in the future
	}

	@AfterEach
	public void tearDown() throws Exception {
		if(toDelete != null){
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
		String originalEtag = created.getEtag();
		
		// Update it
		created.setName(EVALUATION_NAME_2);
		evaluationDAO.update(created);
		Evaluation updated = evaluationDAO.get(evalId);
		assertEquals(evalId, updated.getId());
		assertFalse(eval.getName().equals(updated.getName()), "Evaluation name update failed.");
		assertFalse(originalEtag.equals(updated.getEtag()), "eTag was not updated.");
		
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
	public void testGetAccessibleEvaluationsForProject() throws Exception {
		Set<Long> principalIds = Collections.singleton(EVALUATION_OWNER_ID);
		
		EvaluationFilter filter = new EvaluationFilter(principalIds, ACCESS_TYPE.READ)
				.withContentSourceFilter(EVALUATION_CONTENT_SOURCE);
		
		// Get nothing
		List<Evaluation> retrieved = evaluationDAO.getAccessibleEvaluations(filter, 10, 0);
		assertEquals(0, retrieved.size());
		
		// test with timestamp param
		filter.withTimeFilter(System.currentTimeMillis());
		retrieved = evaluationDAO.getAccessibleEvaluations(filter, 10, 0);
		assertEquals(0, retrieved.size());
		
		// Create one
		String evalId = evaluationDAO.create(eval, EVALUATION_OWNER_ID);
		assertNotNull(evalId);
		toDelete.add(evalId);
		
		// no permission to access
		retrieved = evaluationDAO.getAccessibleEvaluations(filter, 10, 0);
		assertEquals(0, retrieved.size());
		
		// now provide the permission to READ
		AccessControlList acl = Util.createACL(evalId, EVALUATION_OWNER_ID, Collections.singleton(ACCESS_TYPE.READ), new Date());

		String aclId = aclDAO.create(acl, ObjectType.EVALUATION);
		acl.setId(aclId);
		aclToDelete = acl;
		
		// reset the time filter
		filter.withTimeFilter(null);
		
		// Get it
		retrieved = evaluationDAO.getAccessibleEvaluations(filter, 10, 0);
		assertEquals(1, retrieved.size());
		
		Evaluation created = retrieved.get(0);
		assertEquals(evalId, created.getId());
		assertEquals(EVALUATION_NAME, created.getName());
		assertEquals(EVALUATION_OWNER_ID.toString(), created.getOwnerId());
		assertEquals(EVALUATION_CONTENT_SOURCE, created.getContentSource());
		assertEquals(EvaluationStatus.PLANNED, created.getStatus());
		assertNotNull(created.getEtag());

		// test with timestamp param
		filter.withTimeFilter(System.currentTimeMillis());
		
		// currently the challenge is active...
		retrieved = evaluationDAO.getAccessibleEvaluations(filter, 10, 0);
		assertEquals(1, retrieved.size());
		
		// but in the future it won't be
		filter.withTimeFilter(futureTime);
		
		retrieved = evaluationDAO.getAccessibleEvaluations(filter, 10, 0);
		assertEquals(0, retrieved.size());
		
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
        	assertTrue(e.getMessage().contains(EVALUATION_NAME), 
        			"Name conflict message should contain the requested name");
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
		

		// those who have not joined do not get this result
		long participantId = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
		
		// search for it
		// I can find my own evaluation...
		Set<Long> pids = ImmutableSet.of(participantId, 104L);
		List<Evaluation> evalList;
		
		EvaluationFilter filter = new EvaluationFilter(pids, ACCESS_TYPE.SUBMIT);
		
		evalList = evaluationDAO.getAccessibleEvaluations(filter, 10, 0);
		assertTrue(evalList.isEmpty());
		
		// check that an empty principal list works too
		
		filter = new EvaluationFilter(Collections.emptySet(), ACCESS_TYPE.SUBMIT);
		
		evalList = evaluationDAO.getAccessibleEvaluations(filter, 10, 0);
		assertTrue(evalList.isEmpty());
		
		// check that the filter works
		
		filter = new EvaluationFilter(pids, ACCESS_TYPE.SUBMIT)
				.withIdsFilter(Arrays.asList(Long.parseLong(evalId)));
		
		evalList = evaluationDAO.getAccessibleEvaluations(filter, 10, 0);
		assertTrue(evalList.isEmpty());
		
		// and that the time filter works
		filter = new EvaluationFilter(pids, ACCESS_TYPE.SUBMIT)
			.withIdsFilter(null)
			.withTimeFilter(System.currentTimeMillis());

		evalList = evaluationDAO.getAccessibleEvaluations(filter, 10, 0);
		assertTrue(evalList.isEmpty());

		// Now join the Evaluation by
		// adding 'participantId' into the ACL with SUBMIT permission
		AccessControlList acl = Util.createACL(eval.getId(), participantId, Collections.singleton(ACCESS_TYPE.SUBMIT), new Date());
		String aclId = aclDAO.create(acl, ObjectType.EVALUATION);
		acl.setId(aclId);
		aclToDelete = acl;
		
		// As a participant, I can find:
		filter = new EvaluationFilter(pids, ACCESS_TYPE.SUBMIT);
		
		evalList = evaluationDAO.getAccessibleEvaluations(filter, 10, 0);
		assertEquals(1, evalList.size());
		assertEquals(eval, evalList.get(0));
		
		// make sure time filter works
		filter = new EvaluationFilter(pids, ACCESS_TYPE.SUBMIT)
				.withTimeFilter(System.currentTimeMillis());
		
		evalList = evaluationDAO.getAccessibleEvaluations(filter, 10, 0);
		assertEquals(1, evalList.size());
		assertEquals(eval, evalList.get(0));
		
		// the evaluation is omitted if the challenge is over:
		filter = new EvaluationFilter(pids, ACCESS_TYPE.SUBMIT)
				.withTimeFilter(futureTime);
		
		evalList = evaluationDAO.getAccessibleEvaluations(filter, 10, 0);
		assertTrue(evalList.isEmpty());		
		
		// make sure filter works
		filter = new EvaluationFilter(pids, ACCESS_TYPE.SUBMIT)
				.withIdsFilter(Arrays.asList(Long.parseLong(evalId)));
		
		evalList = evaluationDAO.getAccessibleEvaluations(filter, 10, 0);
		assertEquals(1, evalList.size());
		assertEquals(eval, evalList.get(0));
		
		// filtering with 'eval 2' causes no results to come back
		filter = new EvaluationFilter(pids, ACCESS_TYPE.SUBMIT)
				.withIdsFilter(Arrays.asList(Long.parseLong(evalId2)));
		
		evalList = evaluationDAO.getAccessibleEvaluations(filter, 10, 0);
		assertEquals(0, evalList.size());
		
		// non-participants  cannot find
		filter = new EvaluationFilter(ImmutableSet.of(110L, 111L), ACCESS_TYPE.SUBMIT)
				.withIdsFilter(Arrays.asList(Long.parseLong(evalId)));
		
		evalList = evaluationDAO.getAccessibleEvaluations(filter, 10, 0);
		assertTrue(evalList.isEmpty());
		
		// PLFM-2312 problem with repeated entries
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId());
		ra.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{ACCESS_TYPE.SUBMIT})));
		Set<ResourceAccess> ras = acl.getResourceAccess();
		ras.add(ra);
		aclDAO.update(acl, ObjectType.EVALUATION);
		
		// should still find just one result, even though I'm in the ACL twice
		pids = ImmutableSet.of(participantId, BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId());
		
		filter = new EvaluationFilter(pids, ACCESS_TYPE.SUBMIT);
		
		evalList = evaluationDAO.getAccessibleEvaluations(filter, 10, 0);
		assertEquals(1, evalList.size());
		assertEquals(eval, evalList.get(0));		
		
		// Note:  The evaluation isn't returned for the wrong access type
		filter = new EvaluationFilter(pids, ACCESS_TYPE.READ);
		
		assertTrue(evaluationDAO.getAccessibleEvaluations(filter, 10, 0).isEmpty());
   }
    
    @Test
    public void testGetAvailableEvaluations() {
    	
    	String evalId = evaluationDAO.create(eval, EVALUATION_OWNER_ID);

		toDelete.add(evalId);
    	
		Long evaluationId = Long.valueOf(evalId);
    	List<Long> ids = ImmutableList.of(evaluationId, 100L, 200L, 300L);
    	
    	Set<Long> result = evaluationDAO.getAvailableEvaluations(ids);
   
    	assertEquals(ImmutableSet.of(evaluationId), result);
    }
    
    @Test
    public void testGetAvailableEvaluationsWithEmptyInput() {
    	
    	List<Long> ids = Collections.emptyList();
    	
    	Set<Long> result = evaluationDAO.getAvailableEvaluations(ids);
    	   
    	assertEquals(Collections.emptySet(), result);
   
    }
    
    @Test
    public void testGetAvailableEvaluationsWithNullInput() {
    	
    	List<Long> ids = null;
    	
    	String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
    		// Call under test
    		evaluationDAO.getAvailableEvaluations(ids);
    	}).getMessage();
    	
    	assertEquals("ids is required.", errorMessage);
   
    }

}
