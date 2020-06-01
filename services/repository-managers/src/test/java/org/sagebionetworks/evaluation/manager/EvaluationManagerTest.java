package org.sagebionetworks.evaluation.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.TeamSubmissionEligibility;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.evaluation.EvaluationDAO;
import org.sagebionetworks.repo.model.evaluation.EvaluationSubmissionsDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class EvaluationManagerTest {

	private EvaluationManager evaluationManager;
	private Evaluation eval;
	private Evaluation evalWithId;
	private List<Evaluation> evaluations;

	@Mock
	private AuthorizationManager mockAuthorizationManager;

	@Mock
	private EvaluationPermissionsManager mockPermissionsManager;

	@Mock
	private IdGenerator mockIdGenerator;

	@Mock
	private EvaluationDAO mockEvaluationDAO;

	@Mock
	private EvaluationSubmissionsDAO mockEvaluationSubmissionsDAO;

	@Mock
	private SubmissionEligibilityManager mockSubmissionEligibilityManager;

	@Mock
	private NodeDAO mockNodeDAO;

	private final Long OWNER_ID = 123L;
	private final Long USER_ID = 456L;
	private UserInfo ownerInfo;
	private UserInfo userInfo;

	private final String EVALUATION_NAME = "test-evaluation";
	private final String EVALUATION_ID = "1234";
	private final Long EVALUATION_ID_LONG = Long.parseLong(EVALUATION_ID);
	private final String EVALUATION_CONTENT_SOURCE = "syn12358129748";
	private final String EVALUATION_ETAG = "etag";

	@BeforeEach
	public void setUp() throws DatastoreException, NotFoundException, InvalidModelException {

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
		ReflectionTestUtils.setField(evaluationManager, "nodeDAO", mockNodeDAO);

	}

	@Test
	public void testCreateEvaluation() throws Exception {		
		when(mockIdGenerator.generateNewId(IdType.EVALUATION_ID)).thenReturn(Long.parseLong(EVALUATION_ID));
		when(mockEvaluationDAO.create(any(Evaluation.class), eq(OWNER_ID))).thenReturn(EVALUATION_ID);
		when(mockEvaluationDAO.get(eq(EVALUATION_ID))).thenReturn(evalWithId);

		evaluations= Collections.singletonList(evalWithId);
		when(mockAuthorizationManager.canAccess(eq(ownerInfo), eq(EVALUATION_CONTENT_SOURCE), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.CREATE))).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDAO.getNodeTypeById(EVALUATION_CONTENT_SOURCE)).thenReturn(EntityType.project);

		Evaluation clone = evaluationManager.createEvaluation(ownerInfo, eval);
		assertNotNull(clone.getCreatedOn());
		evalWithId.setCreatedOn(clone.getCreatedOn());
		assertEquals(evalWithId, clone, "'create' returned unexpected Evaluation");
		verify(mockEvaluationDAO).create(eq(eval), eq(OWNER_ID));
	}

	@Test
	public void testGetEvaluation() throws DatastoreException, NotFoundException, UnauthorizedException {
		when(mockEvaluationDAO.get(eq(EVALUATION_ID))).thenReturn(evalWithId);
		evaluations= Collections.singletonList(evalWithId);
		when(mockPermissionsManager.hasAccess(eq(ownerInfo), any(), eq(ACCESS_TYPE.READ))).thenReturn(AuthorizationStatus.authorized());

		Evaluation eval2 = evaluationManager.getEvaluation(ownerInfo, EVALUATION_ID);

		assertEquals(evalWithId, eval2);
		verify(mockEvaluationDAO).get(eq(EVALUATION_ID));
	}

	@Test
	public void testGetEvaluationByContentSource() throws Exception {

		ACCESS_TYPE accessType = ACCESS_TYPE.READ;
		evaluations= Collections.singletonList(evalWithId);
		when(mockEvaluationDAO.getAccessibleEvaluationsForProject(eq(EVALUATION_CONTENT_SOURCE), (List<Long>)any(), eq(accessType), eq(null), anyLong(), anyLong())).thenReturn(evaluations);

		List<Evaluation> qr = evaluationManager.getEvaluationByContentSource(ownerInfo, EVALUATION_CONTENT_SOURCE, accessType, false, 10L, 0L);
		assertEquals(evaluations, qr);
		assertEquals(1L, qr.size());
	}

	@Test
	public void testGetEvaluationByContentSourceActiveOnly() throws Exception {

		ACCESS_TYPE accessType = ACCESS_TYPE.READ;
		evaluations= Collections.singletonList(evalWithId);
		when(mockEvaluationDAO.getAccessibleEvaluationsForProject(eq(EVALUATION_CONTENT_SOURCE), (List<Long>)any(), eq(accessType), anyLong(), anyLong(), anyLong())).
		thenReturn(Collections.EMPTY_LIST);

		List<Evaluation> qr = evaluationManager.getEvaluationByContentSource(ownerInfo, EVALUATION_CONTENT_SOURCE, accessType, true, 10L, 0L);
		assertTrue(qr.isEmpty());
		verify(mockEvaluationDAO).getAccessibleEvaluationsForProject(
				eq(EVALUATION_CONTENT_SOURCE), (List<Long>)any(), eq(ACCESS_TYPE.READ),  anyLong(), eq(10L), eq(0L));
	}
	
	@Test
	public void testGetEvaluationByContentSourceWithNullAccessType() throws Exception {

		ACCESS_TYPE accessType = null;
		evaluations= Collections.singletonList(evalWithId);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			evaluationManager.getEvaluationByContentSource(ownerInfo, EVALUATION_CONTENT_SOURCE, accessType, true, 10L, 0L);
		}).getMessage();
		
		assertEquals("The access type is required.", errorMessage);
	}

	@Test
	public void testUpdateEvaluationAsOwner() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		when(mockEvaluationDAO.get(eq(EVALUATION_ID))).thenReturn(evalWithId);

		evaluations= Collections.singletonList(evalWithId);
		when(mockPermissionsManager.hasAccess(eq(ownerInfo), any(), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationStatus.authorized());

		assertNotNull(evalWithId.getCreatedOn());

		evaluationManager.updateEvaluation(ownerInfo, evalWithId);
		verify(mockEvaluationDAO).update(eq(evalWithId));
	}

	@Test
	public void testUpdateEvaluationAsUser() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException {

		evaluations= Collections.singletonList(evalWithId);
		when(mockPermissionsManager.hasAccess(eq(userInfo), any(), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationStatus.accessDenied(""));

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
		when(mockEvaluationDAO.get(eq(EVALUATION_ID))).thenReturn(evalWithId);
		when(mockEvaluationDAO.lookupByName(eq(EVALUATION_NAME))).thenReturn(EVALUATION_ID);
		evaluations= Collections.singletonList(evalWithId);
		when(mockPermissionsManager.hasAccess(eq(ownerInfo), any(), eq(ACCESS_TYPE.READ))).thenReturn(AuthorizationStatus.authorized());

		Evaluation eval2 = evaluationManager.findEvaluation(ownerInfo, EVALUATION_NAME);
		assertEquals(evalWithId, eval2);
		verify(mockEvaluationDAO).lookupByName(eq(EVALUATION_NAME));
	}

	@Test
	public void testGetAvailableInRange() throws Exception {

		evaluations= Collections.singletonList(evalWithId);
		when(mockEvaluationDAO.getAccessibleEvaluations(any(List.class), eq(ACCESS_TYPE.SUBMIT), eq(null), anyLong(), anyLong(), eq(null))).thenReturn(evaluations);    	

		// availability is based on SUBMIT access, not READ
		List<Evaluation> qr = evaluationManager.getAvailableInRange(ownerInfo, false, 10L, 0L, null);
		assertEquals(evaluations, qr);
		assertEquals(1L, qr.size());
	}

	@Test
	public void testGetAvailableInRangeActiveOnly() throws Exception {

		evaluations= Collections.singletonList(evalWithId);
		when(mockEvaluationDAO.getAccessibleEvaluations(any(List.class), eq(ACCESS_TYPE.SUBMIT), anyLong(), anyLong(), anyLong(), eq(null))).thenReturn(Collections.EMPTY_LIST);    	

		// availability is based on SUBMIT access, not READ
		List<Evaluation> qr = evaluationManager.getAvailableInRange(ownerInfo, true, 10L, 0L, null);
		assertTrue(qr.isEmpty());
		verify(mockEvaluationDAO).getAccessibleEvaluations(
				(List<Long>)any(), eq(ACCESS_TYPE.SUBMIT), anyLong(), eq(10L), eq(0L), eq(null));
	}

	@Test
	public void testFindDoesNotExist() throws DatastoreException, UnauthorizedException, NotFoundException {
		when(mockEvaluationDAO.lookupByName(eq(EVALUATION_NAME +  "2"))).thenThrow(new NotFoundException());

		assertThrows(NotFoundException.class, ()->evaluationManager.findEvaluation(ownerInfo, EVALUATION_NAME +  "2"));
	}

	@Test
	public void testInvalidName() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException {

		evaluations= Collections.singletonList(evalWithId);
		when(mockAuthorizationManager.canAccess(eq(ownerInfo), eq(EVALUATION_CONTENT_SOURCE), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.CREATE))).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDAO.getNodeTypeById(EVALUATION_CONTENT_SOURCE)).thenReturn(EntityType.project);

		// note that the Evaluation Manager relies on NameValidation.java
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

		evaluations= Collections.singletonList(evalWithId);

		when(mockEvaluationDAO.get(eval.getId())).thenReturn(eval);
		when(mockPermissionsManager.canCheckTeamSubmissionEligibility(userInfo, eval.getId(), TEAM_ID)).
		thenReturn(AuthorizationStatus.authorized());
		TeamSubmissionEligibility tse = new TeamSubmissionEligibility();
		tse.setEvaluationId(eval.getId());
		tse.setTeamId(TEAM_ID);
		when(mockSubmissionEligibilityManager.getTeamSubmissionEligibility(eval, TEAM_ID)).
		thenReturn(tse);
		assertEquals(tse,
				evaluationManager.getTeamSubmissionEligibility(userInfo, eval.getId(), TEAM_ID));
	}

	@Test
	public void testCannotCreateEvaluationForNonProject() {
		// configure mocks
		when(mockIdGenerator.generateNewId(IdType.EVALUATION_ID)).thenReturn(Long.parseLong(EVALUATION_ID));
		when(mockEvaluationDAO.get(eq(EVALUATION_ID))).thenReturn(evalWithId);
		when(mockEvaluationDAO.create(eq(evalWithId), eq(OWNER_ID))).thenReturn(EVALUATION_ID);

		evaluations= Collections.singletonList(evalWithId);
		when(mockAuthorizationManager.canAccess(eq(ownerInfo), eq(EVALUATION_CONTENT_SOURCE), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.CREATE))).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDAO.getNodeTypeById(EVALUATION_CONTENT_SOURCE)).thenReturn(EntityType.project);

		for (EntityType t : EntityType.values()) {
			when(mockNodeDAO.getNodeTypeById(EVALUATION_CONTENT_SOURCE)).thenReturn(t);
			if (t.equals(EntityType.project)) { // Should succeed
				// Call under test, should not throw exception
				evaluationManager.createEvaluation(ownerInfo, evalWithId);
			} else { // Should get IllegalArgumentException
				try { // Call under test
					evaluationManager.createEvaluation(ownerInfo, evalWithId);
					fail("Expected exception");
				} catch (IllegalArgumentException e) {
					// As expected
				}
			}
		}
	}

}
