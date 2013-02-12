package org.sagebionetworks.evaluation.manager;

import static org.mockito.Mockito.*;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.manager.EvaluationManager;
import org.sagebionetworks.evaluation.manager.EvaluationManagerImpl;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.util.UserInfoUtils;
import org.sagebionetworks.repo.web.NotFoundException;

public class EvaluationManagerTest {
		
	private static EvaluationManager evaluationManager;
	private static Evaluation eval;
	
	private static EvaluationDAO mockEvaluationDAO;
	
	private static final Long OWNER_ID = 123L;
	private static final Long USER_ID = 456L;
	private static UserInfo ownerInfo;
	private static UserInfo userInfo;
	
	private static final String COMPETITION_NAME = "test-competition";
    private static final String COMPETITION_ID = "foo";
    private static final String COMPETITION_CONTENT_SOURCE = "Baz";
    private static final String COMPETITION_ETAG = "etag";
    
    @Before
    public void setUp() throws DatastoreException, NotFoundException, InvalidModelException {    	
    	// Competition DAO
    	mockEvaluationDAO = mock(EvaluationDAO.class);    	
    	
    	// UserInfo
    	ownerInfo = UserInfoUtils.createValidUserInfo(false);
    	ownerInfo.getIndividualGroup().setId(OWNER_ID.toString());
    	userInfo = UserInfoUtils.createValidUserInfo(false);
    	userInfo.getIndividualGroup().setId(USER_ID.toString());
    	
		// Competition
		eval = new Evaluation();
		eval.setName(COMPETITION_NAME);
		eval.setId(COMPETITION_ID);
		eval.setOwnerId(ownerInfo.getIndividualGroup().getId());
        eval.setContentSource(COMPETITION_CONTENT_SOURCE);
        eval.setStatus(EvaluationStatus.PLANNED);
        eval.setCreatedOn(new Date());
        eval.setEtag(COMPETITION_ETAG);
        
        // Competition Manager
    	evaluationManager = new EvaluationManagerImpl(mockEvaluationDAO);
		when(mockEvaluationDAO.create(eq(eval), eq(OWNER_ID))).thenReturn(eval.getId());
    	when(mockEvaluationDAO.get(eq(COMPETITION_ID))).thenReturn(eval);
    	when(mockEvaluationDAO.lookupByName(eq(COMPETITION_NAME))).thenReturn(COMPETITION_ID);
    }
	
	@Test
	public void testCreateCompetition() throws Exception {		
		Evaluation clone = evaluationManager.createEvaluation(ownerInfo, eval);
		assertEquals("'create' returned unexpected Competition ID", eval, clone);
		verify(mockEvaluationDAO).create(eq(eval), eq(OWNER_ID));
	}
	
	@Test
	public void testGetCompetition() throws DatastoreException, NotFoundException, UnauthorizedException {
		Evaluation comp2 = evaluationManager.getEvaluation(COMPETITION_ID);
		assertEquals(eval, comp2);
		verify(mockEvaluationDAO).get(eq(COMPETITION_ID));
	}
	
	@Test
	public void testUpdateCompetitionAsOwner() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		evaluationManager.updateEvaluation(ownerInfo, eval);
		verify(mockEvaluationDAO).update(eq(eval));
	}
	
	@Test
	public void testUpdateCompetitionAsUser() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException {
		try {
			evaluationManager.updateEvaluation(userInfo, eval);
			fail("User should not have permission to update competition");
		} catch (UnauthorizedException e) {
			// expected
		}
		verify(mockEvaluationDAO, times(0)).update(eq(eval));
	}

	@Test
	public void testFind() throws DatastoreException, UnauthorizedException, NotFoundException {
		Evaluation comp2 = evaluationManager.findEvaluation(COMPETITION_NAME);
		assertEquals(eval, comp2);
		verify(mockEvaluationDAO).lookupByName(eq(COMPETITION_NAME));
	}
	
	@Test(expected=NotFoundException.class)
	public void testFindDoesNotExist() throws DatastoreException, UnauthorizedException, NotFoundException {
		evaluationManager.findEvaluation(COMPETITION_NAME +  "2");
	}
	
	@Test
	public void testInvalidName() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException {
		// note that the Competition Manager relies on EntityNameValidation.java
		eval.setName("$ This is an invalid name");
		try {
			evaluationManager.createEvaluation(ownerInfo, eval);			
		} catch (IllegalArgumentException e) {
			// expected
			assertTrue(e.getMessage().toLowerCase().contains("name"));			
		}
		verify(mockEvaluationDAO, times(0)).update(eq(eval));
	}
	
	@Test
	public void testIsAdmin() throws DatastoreException, UnauthorizedException, NotFoundException {
		assertTrue("Owner should be an admin of their own Competition", 
				evaluationManager.isEvalAdmin(ownerInfo, COMPETITION_ID));
		assertFalse("Non-owner user should NOT be an admin of this Competition", 
				evaluationManager.isEvalAdmin(userInfo, COMPETITION_ID));
	}

}
