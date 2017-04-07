package org.sagebionetworks.evaluation.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.TeamSubmissionEligibility;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.evaluation.EvaluationDAO;
import org.sagebionetworks.repo.model.evaluation.EvaluationSubmissionsDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

public class EvaluationManagerTest {
		
	private EvaluationManager evaluationManager;
	private Evaluation eval;
	private Evaluation evalWithId;
	private List<Evaluation> evaluations;

	private AuthorizationManager mockAuthorizationManager;
	private EvaluationPermissionsManager mockPermissionsManager;
	private IdGenerator mockIdGenerator;
	private EvaluationDAO mockEvaluationDAO;
	private EvaluationSubmissionsDAO mockEvaluationSubmissionsDAO;
	private SubmissionEligibilityManager mockSubmissionEligibilityManager;
	
	private final Long OWNER_ID = 123L;
	private final Long USER_ID = 456L;
	private UserInfo ownerInfo;
	private UserInfo userInfo;
	
	private final String EVALUATION_NAME = "test-evaluation";
    private final String EVALUATION_ID = "1234";
    private final Long EVALUATION_ID_LONG = Long.parseLong(EVALUATION_ID);
    private final String EVALUATION_CONTENT_SOURCE = KeyFactory.SYN_ROOT_ID;
    private final String EVALUATION_ETAG = "etag";
    
    @Before
    public void setUp() throws DatastoreException, NotFoundException, InvalidModelException {
    	// ID Generator
    	mockIdGenerator = mock(IdGenerator.class);
    	
    	// Evaluation DAO
    	mockEvaluationDAO = mock(EvaluationDAO.class);

    	// Permissions manager
    	mockPermissionsManager = mock(EvaluationPermissionsManager.class);

    	// Authorization manager
    	mockAuthorizationManager = mock(AuthorizationManager.class);
    	
    	mockEvaluationSubmissionsDAO = mock(EvaluationSubmissionsDAO.class);

    	mockSubmissionEligibilityManager = mock(SubmissionEligibilityManager.class);

    	// UserInfo
    	ownerInfo = new UserInfo(false, OWNER_ID);
    	userInfo = new UserInfo(false, USER_ID);
    	
		// Evaluation
    	Date date = new Date();
		eval = new Evaluation();
		eval.setCreatedOn(date);
		eval.setName(EVALUATION_NAME);
		eval.setOwnerId(ownerInfo.getId().toString());
        eval.setContentSource(EVALUATION_CONTENT_SOURCE);
        eval.setStatus(EvaluationStatus.PLANNED);
        eval.setEtag(EVALUATION_ETAG);
        
		evalWithId = new Evaluation();
		evalWithId.setCreatedOn(date);
		evalWithId.setId(EVALUATION_ID);
		evalWithId.setName(EVALUATION_NAME);
		evalWithId.setOwnerId(ownerInfo.getId().toString());
		evalWithId.setContentSource(EVALUATION_CONTENT_SOURCE);
		evalWithId.setStatus(EvaluationStatus.PLANNED);
		evalWithId.setEtag(EVALUATION_ETAG);

        // Evaluation Manager
    	evaluationManager = new EvaluationManagerImpl();
    	ReflectionTestUtils.setField(evaluationManager, "evaluationDAO", mockEvaluationDAO);
    	ReflectionTestUtils.setField(evaluationManager, "idGenerator", mockIdGenerator);
    	ReflectionTestUtils.setField(evaluationManager, "authorizationManager", mockAuthorizationManager);
    	ReflectionTestUtils.setField(evaluationManager, "evaluationPermissionsManager", mockPermissionsManager);
    	ReflectionTestUtils.setField(evaluationManager, "evaluationSubmissionsDAO", mockEvaluationSubmissionsDAO);
    	ReflectionTestUtils.setField(evaluationManager, "submissionEligibilityManager", mockSubmissionEligibilityManager);

    	// configure mocks
    	when(mockIdGenerator.generateNewId(IdType.EVALUATION_ID)).thenReturn(Long.parseLong(EVALUATION_ID));
		when(mockEvaluationDAO.create(any(Evaluation.class), eq(OWNER_ID))).thenReturn(EVALUATION_ID);
    	when(mockEvaluationDAO.get(eq(EVALUATION_ID))).thenReturn(evalWithId);
    	when(mockEvaluationDAO.lookupByName(eq(EVALUATION_NAME))).thenReturn(EVALUATION_ID);
    	when(mockEvaluationDAO.create(eq(evalWithId), eq(OWNER_ID))).thenReturn(EVALUATION_ID);
    	evaluations=Arrays.asList(new Evaluation[]{evalWithId});
    	when(mockEvaluationDAO.getAccessibleEvaluationsForProject(eq(EVALUATION_CONTENT_SOURCE), (List<Long>)any(), eq(ACCESS_TYPE.READ), anyLong(), anyLong())).thenReturn(evaluations);
    	when(mockEvaluationDAO.getAccessibleEvaluations((List<Long>)any(), eq(ACCESS_TYPE.SUBMIT), anyLong(), anyLong(), any(List.class))).thenReturn(evaluations);
    	when(mockAuthorizationManager.canAccess(eq(ownerInfo), eq(KeyFactory.SYN_ROOT_ID), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.CREATE))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
    	when(mockPermissionsManager.hasAccess(eq(ownerInfo), anyString(), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
    	when(mockPermissionsManager.hasAccess(eq(ownerInfo), anyString(), eq(ACCESS_TYPE.READ))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
    	when(mockPermissionsManager.hasAccess(eq(userInfo), anyString(), eq(ACCESS_TYPE.READ))).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
    	when(mockPermissionsManager.hasAccess(eq(userInfo), anyString(), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
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
		Evaluation eval2 = evaluationManager.getEvaluation(ownerInfo, EVALUATION_ID);
		assertEquals(evalWithId, eval2);
		verify(mockEvaluationDAO).get(eq(EVALUATION_ID));
	}
	
	@Test
	public void testGetEvaluationByContentSource() throws Exception {
		List<Evaluation> qr = evaluationManager.getEvaluationByContentSource(ownerInfo, EVALUATION_CONTENT_SOURCE, 10, 0);
		assertEquals(evaluations, qr);
		assertEquals(1L, qr.size());
	}
	
	@Test
	public void testUpdateEvaluationAsOwner() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		assertNotNull(evalWithId.getCreatedOn());
		when(mockEvaluationSubmissionsDAO.lockAndGetForEvaluation(EVALUATION_ID_LONG)).thenThrow(new NotFoundException());
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
		Evaluation eval2 = evaluationManager.findEvaluation(ownerInfo, EVALUATION_NAME);
		assertEquals(evalWithId, eval2);
		verify(mockEvaluationDAO).lookupByName(eq(EVALUATION_NAME));
	}
	
	@Test
	public void testGetAvailableInRange() throws Exception {
		// availability is based on SUBMIT access, not READ
    	when(mockPermissionsManager.hasAccess(eq(ownerInfo), anyString(), eq(ACCESS_TYPE.READ))).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		List<Evaluation> qr = evaluationManager.getAvailableInRange(ownerInfo, 10L, 0L, null);
		assertEquals(evaluations, qr);
		assertEquals(1L, qr.size());
	}
	
	@Test(expected=NotFoundException.class)
	public void testFindDoesNotExist() throws DatastoreException, UnauthorizedException, NotFoundException {
		evaluationManager.findEvaluation(ownerInfo, EVALUATION_NAME +  "2");
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
	
	private static final String TEAM_ID = "101";
	
	@Test
	public void testGetTeamSubmissionEligibility() throws Exception {
		when(mockEvaluationDAO.get(eval.getId())).thenReturn(eval);
		when(mockPermissionsManager.canCheckTeamSubmissionEligibility(userInfo, eval.getId(), TEAM_ID)).
			thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		TeamSubmissionEligibility tse = new TeamSubmissionEligibility();
		tse.setEvaluationId(eval.getId());
		tse.setTeamId(TEAM_ID);
		when(mockSubmissionEligibilityManager.getTeamSubmissionEligibility(eval, TEAM_ID)).
			thenReturn(tse);
		assertEquals(tse,
				evaluationManager.getTeamSubmissionEligibility(userInfo, eval.getId(), TEAM_ID));
	}

}
