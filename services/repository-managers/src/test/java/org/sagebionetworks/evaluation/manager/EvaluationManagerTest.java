package org.sagebionetworks.evaluation.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.util.UserInfoUtils;
import org.sagebionetworks.repo.web.NotFoundException;

public class EvaluationManagerTest {
		
	private static EvaluationManager evaluationManager;
	private static Evaluation eval;
	private static Evaluation evalWithId;
	private static List<Evaluation> evaluations;
	
	private static AuthorizationManager mockAuthorizationManager;
	private static IdGenerator mockIdGenerator;
	private static EvaluationDAO mockEvaluationDAO;
	
	private static final Long OWNER_ID = 123L;
	private static final Long USER_ID = 456L;
	private static UserInfo ownerInfo;
	private static UserInfo userInfo;
	
	private static final String EVALUATION_NAME = "test-evaluation";
    private static final String EVALUATION_ID = "1234";
    private static final String EVALUATION_CONTENT_SOURCE = KeyFactory.SYN_ROOT_ID;
    private static final String EVALUATION_ETAG = "etag";
    
    @Before
    public void setUp() throws DatastoreException, NotFoundException, InvalidModelException {
    	// ID Generator
    	mockIdGenerator = mock(IdGenerator.class);
    	
    	// Evaluation DAO
    	mockEvaluationDAO = mock(EvaluationDAO.class);    
    	
    	// Authorization Manager
    	mockAuthorizationManager = mock(AuthorizationManager.class);
    	
    	// UserInfo
    	ownerInfo = UserInfoUtils.createValidUserInfo(false);
    	ownerInfo.getIndividualGroup().setId(OWNER_ID.toString());
    	userInfo = UserInfoUtils.createValidUserInfo(false);
    	userInfo.getIndividualGroup().setId(USER_ID.toString());
    	
		// Evaluation
    	Date date = new Date();
		eval = new Evaluation();
		eval.setCreatedOn(date);
		eval.setName(EVALUATION_NAME);
		eval.setOwnerId(ownerInfo.getIndividualGroup().getId());
        eval.setContentSource(EVALUATION_CONTENT_SOURCE);
        eval.setStatus(EvaluationStatus.PLANNED);
        eval.setEtag(EVALUATION_ETAG);
        
		evalWithId = new Evaluation();
		evalWithId.setCreatedOn(date);
		evalWithId.setId(EVALUATION_ID);
		evalWithId.setName(EVALUATION_NAME);
		evalWithId.setOwnerId(ownerInfo.getIndividualGroup().getId());
		evalWithId.setContentSource(EVALUATION_CONTENT_SOURCE);
		evalWithId.setStatus(EvaluationStatus.PLANNED);
		evalWithId.setEtag(EVALUATION_ETAG);
        
        // Evaluation Manager
    	evaluationManager = new EvaluationManagerImpl(mockAuthorizationManager, mockEvaluationDAO, mockIdGenerator);
    	
    	// configure mocks
    	when(mockIdGenerator.generateNewId()).thenReturn(Long.parseLong(EVALUATION_ID));
		when(mockEvaluationDAO.create(any(Evaluation.class), eq(OWNER_ID))).thenReturn(EVALUATION_ID);
    	when(mockEvaluationDAO.get(eq(EVALUATION_ID))).thenReturn(evalWithId);
    	when(mockEvaluationDAO.lookupByName(eq(EVALUATION_NAME))).thenReturn(EVALUATION_ID);
    	when(mockEvaluationDAO.create(eq(evalWithId), eq(OWNER_ID))).thenReturn(EVALUATION_ID);
    	evaluations=Arrays.asList(new Evaluation[]{evalWithId});
    	when(mockEvaluationDAO.getAvailableInRange((List<Long>)any(), (EvaluationStatus)any(), anyLong(), anyLong())).thenReturn(evaluations);
    	when(mockEvaluationDAO.getAvailableCount((List<Long>)any(), (EvaluationStatus)any())).thenReturn(1L);
       	when(mockAuthorizationManager.canAccess(eq(ownerInfo), eq(EVALUATION_ID), eq(ObjectType.EVALUATION), eq(ACCESS_TYPE.UPDATE))).thenReturn(true);
            }
	
	@Test
	public void testCreateEvaluation() throws Exception {		
		Evaluation clone = evaluationManager.createEvaluation(ownerInfo, eval);
		assertNotNull(clone.getCreatedOn());
		evalWithId.setCreatedOn(clone.getCreatedOn());
		assertEquals("'create' returned unexpected Evaluation", evalWithId, clone);
		verify(mockEvaluationDAO).create(eq(eval), eq(OWNER_ID));
	}
	
	@Test
	public void testGetEvaluation() throws DatastoreException, NotFoundException, UnauthorizedException {
		Evaluation eval2 = evaluationManager.getEvaluation(EVALUATION_ID);
		assertEquals(evalWithId, eval2);
		verify(mockEvaluationDAO).get(eq(EVALUATION_ID));
	}
	
	@Test
	public void testUpdateEvaluationAsOwner() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		assertNotNull(evalWithId.getCreatedOn());
		evaluationManager.updateEvaluation(ownerInfo, evalWithId);
		verify(mockEvaluationDAO).update(eq(evalWithId));
	}
	
	@Test
	public void testUpdateEvaluationAsUser() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException {
		try {
			evaluationManager.updateEvaluation(userInfo, evalWithId);
			fail("User should not have permission to update evaluation");
		} catch (UnauthorizedException e) {
			// expected
		}
		verify(mockEvaluationDAO, never()).update(eq(eval));
	}

	@Test
	public void testFind() throws DatastoreException, UnauthorizedException, NotFoundException {
		Evaluation eval2 = evaluationManager.findEvaluation(EVALUATION_NAME);
		assertEquals(evalWithId, eval2);
		verify(mockEvaluationDAO).lookupByName(eq(EVALUATION_NAME));
	}
	
	@Test
	public void testGetAvailableInRange() throws Exception {
		QueryResults<Evaluation> qr = evaluationManager.getAvailableInRange(ownerInfo, null, 10L, 0L);
		assertEquals(evaluations, qr.getResults());
		assertEquals(1L, qr.getTotalNumberOfResults());
	}
	
	@Test(expected=NotFoundException.class)
	public void testFindDoesNotExist() throws DatastoreException, UnauthorizedException, NotFoundException {
		evaluationManager.findEvaluation(EVALUATION_NAME +  "2");
	}
	
	@Test
	public void testInvalidName() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException {
		// note that the Evaluation Manager relies on EntityNameValidation.java
		eval.setName("$ This is an invalid name");
		try {
			evaluationManager.createEvaluation(ownerInfo, eval);			
		} catch (IllegalArgumentException e) {
			// expected
			assertTrue(e.getMessage().toLowerCase().contains("name"));			
		}
		verify(mockEvaluationDAO, times(0)).update(eq(eval));
	}

}
