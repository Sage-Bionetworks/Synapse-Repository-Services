package org.sagebionetworks.evaluation.manager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.EvaluationFilter;
import org.sagebionetworks.evaluation.dao.EvaluationSubmissionsDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationRound;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.SubmissionQuota;
import org.sagebionetworks.evaluation.model.TeamSubmissionEligibility;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class EvaluationManagerTest {

	private Evaluation eval;
	private Evaluation evalWithId;
	private List<Evaluation> evaluations;
	Instant evaluationRoundStart;
	Instant evaluationRoundEnd;
	private EvaluationRound evaluationRound;


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

	@Spy
	@InjectMocks
	private EvaluationManager evaluationManager = new EvaluationManagerImpl();


	private final Long OWNER_ID = 123L;
	private final Long USER_ID = 456L;
	private UserInfo ownerInfo;
	private UserInfo userInfo;
	private String evaluationRoundId;

	private final String EVALUATION_NAME = "test-evaluation";
	private final String EVALUATION_ID = "1234";
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

		evaluationRoundStart = Instant.now();
		evaluationRoundEnd = evaluationRoundStart.plus(34, ChronoUnit.DAYS);
		evaluationRoundId = "98765";
		evaluationRound = new EvaluationRound();
		evaluationRound.setId(evaluationRoundId);
		evaluationRound.setEvaluationId(EVALUATION_ID);
		evaluationRound.setRoundStart(Date.from(evaluationRoundStart));
		evaluationRound.setRoundEnd(Date.from(evaluationRoundEnd));
	}

	@Test
	public void testCreateEvaluation() {
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
	public void testGetEvaluationWithNullId() throws DatastoreException, NotFoundException, UnauthorizedException {
		
		String evaluationId = null;
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			evaluationManager.getEvaluation(ownerInfo, evaluationId);
		}).getMessage();

		assertEquals("Evaluation ID cannot be null", errorMessage);
		verifyZeroInteractions(mockEvaluationDAO);
		verifyZeroInteractions(mockPermissionsManager);
	}
	
	@Test
	public void testGetEvaluationWithNotFoundException() throws DatastoreException, NotFoundException, UnauthorizedException {
		
		NotFoundException expected = new NotFoundException();
		
		when(mockEvaluationDAO.get(eq(EVALUATION_ID))).thenThrow(expected);

		NotFoundException ex = assertThrows(NotFoundException.class, () -> {
			// Call under test
			evaluationManager.getEvaluation(ownerInfo, EVALUATION_ID);
		});
		
		assertEquals(expected, ex);
		verify(mockEvaluationDAO).get(eq(EVALUATION_ID));
		verifyZeroInteractions(mockPermissionsManager);
	}


	@Test
	public void testGetEvaluationByContentSource() {

		ACCESS_TYPE accessType = ACCESS_TYPE.READ;
		evaluations= Collections.singletonList(evalWithId);
		List<Long> evaluationIds = null;
		long limit = 10L;
		long offset = 0;
		
		when(mockEvaluationDAO.getAccessibleEvaluations(any(), anyLong(), anyLong())).thenReturn(evaluations);
		
		EvaluationFilter expectedFilter = new EvaluationFilter(ownerInfo, accessType)
				.withTimeFilter(null)
				.withContentSourceFilter(EVALUATION_CONTENT_SOURCE)
				.withIdsFilter(evaluationIds);
		
		List<Evaluation> qr = evaluationManager.getEvaluationsByContentSource(ownerInfo, EVALUATION_CONTENT_SOURCE, accessType, false, evaluationIds, limit, offset);
		
		assertEquals(evaluations, qr);
		assertEquals(1L, qr.size());
	
		verify(mockEvaluationDAO).getAccessibleEvaluations(expectedFilter, limit, offset);
	}

	@Test
	public void testGetEvaluationByContentSourceActiveOnly() {
		
		ACCESS_TYPE accessType = ACCESS_TYPE.READ;
		evaluations= Collections.singletonList(evalWithId);
		List<Long> evaluationIds = null;
		long limit = 10L;
		long offset = 0;
		
		when(mockEvaluationDAO.getAccessibleEvaluations(any(), anyLong(), anyLong())).thenReturn(evaluations);

		List<Evaluation> qr = evaluationManager.getEvaluationsByContentSource(ownerInfo, EVALUATION_CONTENT_SOURCE, accessType, true, evaluationIds, limit, offset);
		
		assertEquals(evaluations, qr);
		
		ArgumentCaptor<EvaluationFilter> filterCaptor = ArgumentCaptor.forClass(EvaluationFilter.class);
		
		verify(mockEvaluationDAO).getAccessibleEvaluations(filterCaptor.capture(), eq(limit), eq(offset));
		
		assertNotNull(filterCaptor.getValue().getTimeFilter());
	}
	
	@Test
	public void testGetEvaluationByContentSourceWithEvaluationIdsFilter() {

		ACCESS_TYPE accessType = ACCESS_TYPE.READ;
		evaluations= Collections.singletonList(evalWithId);
		List<Long> evaluationIds = Collections.singletonList(Long.valueOf(EVALUATION_ID));
		long limit = 10L;
		long offset = 0;
		
		when(mockEvaluationDAO.getAccessibleEvaluations(any(), anyLong(), anyLong())).thenReturn(evaluations);
		
		EvaluationFilter expectedFilter = new EvaluationFilter(ownerInfo, accessType)
				.withTimeFilter(null)
				.withContentSourceFilter(EVALUATION_CONTENT_SOURCE)
				.withIdsFilter(evaluationIds);
		
		List<Evaluation> qr = evaluationManager.getEvaluationsByContentSource(ownerInfo, EVALUATION_CONTENT_SOURCE, accessType, false, evaluationIds, limit, offset);
		
		assertEquals(evaluations, qr);
		assertEquals(1L, qr.size());
	
		verify(mockEvaluationDAO).getAccessibleEvaluations(expectedFilter, limit, offset);
	}

	
	@Test
	public void testGetEvaluationByContentSourceWithNullAccessType() {

		ACCESS_TYPE accessType = null;
		evaluations= Collections.singletonList(evalWithId);
		List<Long> evaluationIds = null;
		long limit = 10L;
		long offset = 0;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			evaluationManager.getEvaluationsByContentSource(ownerInfo, EVALUATION_CONTENT_SOURCE, accessType, true, evaluationIds, limit, offset);
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
		//no 'quota' field, so we skip this check
		verify(mockEvaluationDAO, never()).hasEvaluationRounds(evalWithId.getId());
	}

	@Test
	public void updateEvaluationAsOwner__QuotaDefined_hasEvaluationRounds() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		when(mockEvaluationDAO.get(eq(EVALUATION_ID))).thenReturn(evalWithId);

		evaluations= Collections.singletonList(evalWithId);
		when(mockPermissionsManager.hasAccess(eq(ownerInfo), any(), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationStatus.authorized());

		assertNotNull(evalWithId.getCreatedOn());

		//an evaluation round was defined
		when(mockEvaluationDAO.hasEvaluationRounds(EVALUATION_ID)).thenReturn(true);
		//quota set
		evalWithId.setQuota(new SubmissionQuota());

		assertThrows(IllegalArgumentException.class, () ->{
			evaluationManager.updateEvaluation(ownerInfo, evalWithId);
		});

		verify(mockEvaluationDAO, never()).update(any());
		verify(mockEvaluationDAO).hasEvaluationRounds(EVALUATION_ID);
	}


	@Test
	public void updateEvaluationAsOwner__QuotaDefined_notHasEvaluationRounds() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		when(mockEvaluationDAO.get(eq(EVALUATION_ID))).thenReturn(evalWithId);

		evaluations= Collections.singletonList(evalWithId);
		when(mockPermissionsManager.hasAccess(eq(ownerInfo), any(), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationStatus.authorized());

		assertNotNull(evalWithId.getCreatedOn());

		//an evaluation round was defined
		when(mockEvaluationDAO.hasEvaluationRounds(any())).thenReturn(false);
		//quota set
		evalWithId.setQuota(new SubmissionQuota());


		//method under test
		evaluationManager.updateEvaluation(ownerInfo, evalWithId);

		verify(mockEvaluationDAO).update(eq(evalWithId));
		verify(mockEvaluationDAO).hasEvaluationRounds(EVALUATION_ID);
	}

	@Test
	public void updateEvaluationAsOwner__QuotaNull_hasEvaluationRounds() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		when(mockEvaluationDAO.get(eq(EVALUATION_ID))).thenReturn(evalWithId);

		evaluations= Collections.singletonList(evalWithId);
		when(mockPermissionsManager.hasAccess(eq(ownerInfo), any(), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationStatus.authorized());

		assertNotNull(evalWithId.getCreatedOn());

		//no quota set
		evalWithId.setQuota(null);

		//method under test
		evaluationManager.updateEvaluation(ownerInfo, evalWithId);

		verify(mockEvaluationDAO).update(eq(evalWithId));
		// never called because quota was null
		verify(mockEvaluationDAO, never()).hasEvaluationRounds(EVALUATION_ID);
	}



	@Test
	public void testUpdateEvaluationAsUser() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException {

		evaluations= Collections.singletonList(evalWithId);
		when(mockEvaluationDAO.get(EVALUATION_ID)).thenReturn(evalWithId);
		when(mockPermissionsManager.hasAccess(eq(userInfo), any(), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationStatus.accessDenied(""));

		assertThrows(UnauthorizedException.class, () -> {
			evaluationManager.updateEvaluation(userInfo, evalWithId);
		}, "User should not have permission to update evaluation");

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
	public void testGetAvailableEvaluations() {

		evaluations= Collections.singletonList(evalWithId);
		List<Long> evaluationIds = null;
		long limit = 10L;
		long offset = 0;
		
		when(mockEvaluationDAO.getAccessibleEvaluations(any(), anyLong(), anyLong())).thenReturn(evaluations);
		
		EvaluationFilter expectedFilter = new EvaluationFilter(ownerInfo, ACCESS_TYPE.SUBMIT)
				.withTimeFilter(null)
				.withContentSourceFilter(null)
				.withIdsFilter(evaluationIds);
		
		// availability is based on SUBMIT access, not READ
		List<Evaluation> qr = evaluationManager.getAvailableEvaluations(ownerInfo, false, evaluationIds, limit, offset);
		
		assertEquals(evaluations, qr);
		
		verify(mockEvaluationDAO).getAccessibleEvaluations(expectedFilter, limit, offset);
	}

	@Test
	public void testGetAvailableEvaluationsActiveOnly() {

		evaluations = Collections.singletonList(evalWithId);
		List<Long> evaluationIds = null;
		long limit = 10L;
		long offset = 0;
		
		when(mockEvaluationDAO.getAccessibleEvaluations(any(), anyLong(), anyLong())).thenReturn(evaluations);
		
		// availability is based on SUBMIT access, not READ
		List<Evaluation> qr = evaluationManager.getAvailableEvaluations(ownerInfo, true, evaluationIds, 10L, 0L);
		
		assertEquals(evaluations, qr);
		
		ArgumentCaptor<EvaluationFilter> filterCaptor = ArgumentCaptor.forClass(EvaluationFilter.class);
		
		verify(mockEvaluationDAO).getAccessibleEvaluations(filterCaptor.capture(), eq(limit), eq(offset));
		
		assertEquals(filterCaptor.getValue().getAccessType(), ACCESS_TYPE.SUBMIT);
		assertNotNull(filterCaptor.getValue().getTimeFilter());
	}
	
	@Test
	public void testGetAvailableEvaluationsWithEvaluationsIdsFilter() {

		evaluations= Collections.singletonList(evalWithId);
		List<Long> evaluationIds = Collections.singletonList(Long.valueOf(EVALUATION_ID));
		long limit = 10L;
		long offset = 0;
		
		when(mockEvaluationDAO.getAccessibleEvaluations(any(), anyLong(), anyLong())).thenReturn(evaluations);
		
		EvaluationFilter expectedFilter = new EvaluationFilter(ownerInfo, ACCESS_TYPE.SUBMIT)
				.withTimeFilter(null)
				.withContentSourceFilter(null)
				.withIdsFilter(evaluationIds);
		
		// availability is based on SUBMIT access, not READ
		List<Evaluation> qr = evaluationManager.getAvailableEvaluations(ownerInfo, false, evaluationIds, limit, offset);
		
		assertEquals(evaluations, qr);
		
		verify(mockEvaluationDAO).getAccessibleEvaluations(expectedFilter, limit, offset);
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
	public void testGetTeamSubmissionEligibility() {

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

	@Test
	public void validateEvaluationAccess_nullUserInfo (){
		UserInfo nullInfo = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			((EvaluationManagerImpl) evaluationManager).validateEvaluationAccess(nullInfo, EVALUATION_ID, ACCESS_TYPE.READ);
		}).getMessage();

		assertEquals("UserInfo cannot be null", message);
		verifyZeroInteractions(mockEvaluationDAO);
		verifyZeroInteractions(mockPermissionsManager);
	}

	@Test
	public void validateEvaluationAccess_noPermissionToAcccess (){
		when(mockEvaluationDAO.get(EVALUATION_ID)).thenReturn(evalWithId);
		when(mockPermissionsManager.hasAccess(userInfo,EVALUATION_ID,ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.accessDenied("nope"));

		String message = assertThrows(UnauthorizedException.class, ()->{
			((EvaluationManagerImpl) evaluationManager).validateEvaluationAccess(userInfo, EVALUATION_ID, ACCESS_TYPE.READ);
		}).getMessage();

		assertEquals("User 456 is not authorized to READ evaluation 1234 (test-evaluation)", message);
		verify(mockEvaluationDAO).get(EVALUATION_ID);
		verify(mockPermissionsManager).hasAccess(userInfo,EVALUATION_ID,ACCESS_TYPE.READ);
	}

	@Test
	public void validateEvaluationAccess_hasPermission (){
		when(mockPermissionsManager.hasAccess(userInfo,EVALUATION_ID,ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockEvaluationDAO.get(EVALUATION_ID)).thenReturn(evalWithId);


		Evaluation result = ((EvaluationManagerImpl) evaluationManager).validateEvaluationAccess(userInfo, EVALUATION_ID, ACCESS_TYPE.READ);

		assertEquals(evalWithId, result);
		verify(mockEvaluationDAO).get(EVALUATION_ID);
		verify(mockPermissionsManager).hasAccess(userInfo,EVALUATION_ID,ACCESS_TYPE.READ);
	}

	@Test
	public void validateNoDateRangeOverlap_hasOverlappingRounds(){
		String overlappingId = "890890";
		EvaluationRound overlappingRound = new EvaluationRound();
		overlappingRound.setId(overlappingId);
		when(mockEvaluationDAO.overlappingEvaluationRounds(EVALUATION_ID, evaluationRoundStart, evaluationRoundEnd))
				.thenReturn(Arrays.asList(overlappingRound));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			((EvaluationManagerImpl) evaluationManager).validateNoDateRangeOverlap(evaluationRound);
		}).getMessage();

		assertEquals("This round's date range overlaps with the following rounds: ["+overlappingId+"]", message);
		verify(mockEvaluationDAO).overlappingEvaluationRounds(EVALUATION_ID, evaluationRoundStart, evaluationRoundEnd);
	}

	@Test
	public void validateNoDateRangeOverlap_noOverlappingRounds(){
		when(mockEvaluationDAO.overlappingEvaluationRounds(EVALUATION_ID, evaluationRoundStart, evaluationRoundEnd))
				.thenReturn(Collections.emptyList());
		assertDoesNotThrow(() ->
			((EvaluationManagerImpl) evaluationManager).validateNoDateRangeOverlap(evaluationRound)
		);

		verify(mockEvaluationDAO).overlappingEvaluationRounds(EVALUATION_ID, evaluationRoundStart, evaluationRoundEnd);
	}

	@Test
	public void validateNoExistingQuotaDefined_hasQuota(){
		evalWithId.setQuota(new SubmissionQuota());

		String message = assertThrows(IllegalArgumentException.class, () ->
				((EvaluationManagerImpl) evaluationManager).validateNoExistingQuotaDefined(evalWithId)
		).getMessage();

		assertEquals("A SubmissionQuota must not be defined for an Evaluation." +
				" You must first remove your Evaluation's SubmisisonQuota in order to use EvaluationRounds", message);
	}

	@Test
	public void validateNoExistingQuotaDefined_noQuota(){
		evalWithId.setQuota(null);
		assertDoesNotThrow(() ->
				((EvaluationManagerImpl) evaluationManager).validateNoExistingQuotaDefined(evalWithId)
		);
	}


	@Test
	public void createEvaluationRound(){
		when(mockPermissionsManager.hasAccess(userInfo,EVALUATION_ID,ACCESS_TYPE.CREATE))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockEvaluationDAO.get(EVALUATION_ID)).thenReturn(evalWithId);
		//fake round that gets "created"
		EvaluationRound createdRound = new EvaluationRound();
		when(mockEvaluationDAO.createEvaluationRound(evaluationRound)).thenReturn(createdRound);


		EvaluationRound result = evaluationManager.createEvaluationRound(userInfo, evaluationRound);

		assertEquals(createdRound, result);

		verify(mockPermissionsManager).hasAccess(userInfo, EVALUATION_ID,ACCESS_TYPE.CREATE);
		verify(mockEvaluationDAO).get(EVALUATION_ID);
		verify(mockEvaluationDAO).overlappingEvaluationRounds(EVALUATION_ID, evaluationRoundStart, evaluationRoundEnd);
		verify(mockEvaluationDAO).createEvaluationRound(evaluationRound);
	}

	@Test
	public void updateEvaluationRound(){
		when(mockPermissionsManager.hasAccess(userInfo,EVALUATION_ID,ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockEvaluationDAO.get(EVALUATION_ID)).thenReturn(evalWithId);
		when(mockEvaluationDAO.getEvaluationRound(EVALUATION_ID, evaluationRoundId)).thenReturn(evaluationRound);

		EvaluationRound result = evaluationManager.updateEvaluationRound(userInfo, evaluationRound);

		assertEquals(evaluationRound, result);

		verify(mockPermissionsManager).hasAccess(userInfo, EVALUATION_ID,ACCESS_TYPE.UPDATE);
		verify(mockEvaluationDAO).get(EVALUATION_ID);
		verify(mockEvaluationDAO).overlappingEvaluationRounds(EVALUATION_ID, evaluationRoundStart, evaluationRoundEnd);
		verify(mockEvaluationDAO).updateEvaluationRound(evaluationRound);
		verify(mockEvaluationDAO).getEvaluationRound(EVALUATION_ID, evaluationRoundId);
	}

	@Test
	public void deleteEvaluationRound(){
		when(mockPermissionsManager.hasAccess(userInfo,EVALUATION_ID,ACCESS_TYPE.DELETE))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockEvaluationDAO.get(EVALUATION_ID)).thenReturn(evalWithId);

		evaluationManager.deleteEvaluationRound(userInfo ,EVALUATION_ID, evaluationRoundId);

		verify(mockPermissionsManager).hasAccess(userInfo, EVALUATION_ID,ACCESS_TYPE.DELETE);
		verify(mockEvaluationDAO).get(EVALUATION_ID);
		verify(mockEvaluationDAO).deleteEvaluationRound(EVALUATION_ID, evaluationRoundId);
	}

	@Test
	public void getEvaluationRound(){
		when(mockPermissionsManager.hasAccess(userInfo,EVALUATION_ID,ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockEvaluationDAO.get(EVALUATION_ID)).thenReturn(evalWithId);
		when(mockEvaluationDAO.getEvaluationRound(EVALUATION_ID, evaluationRoundId)).thenReturn(evaluationRound);


		EvaluationRound result = evaluationManager.getEvaluationRound(userInfo, EVALUATION_ID, evaluationRoundId);
		assertEquals(evaluationRound, result);


		verify(mockPermissionsManager).hasAccess(userInfo, EVALUATION_ID,ACCESS_TYPE.READ);
		verify(mockEvaluationDAO).get(EVALUATION_ID);
		verify(mockEvaluationDAO).getEvaluationRound(EVALUATION_ID, evaluationRoundId);
	}

	@Test
	public void getAllEvaluationRounds(){
		long limit = 50;
		long offset = 0;
		NextPageToken nextPageToken = new NextPageToken(limit, offset);
		List<EvaluationRound> rounds = Collections.singletonList(evaluationRound);

		when(mockPermissionsManager.hasAccess(userInfo,EVALUATION_ID,ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockEvaluationDAO.get(EVALUATION_ID)).thenReturn(evalWithId);
		when(mockEvaluationDAO.getAssociatedEvaluationRounds(EVALUATION_ID, limit+1, offset)).thenReturn(rounds);


		List<EvaluationRound> result = evaluationManager.getAllEvaluationRounds(userInfo, EVALUATION_ID, nextPageToken);

		assertEquals(rounds, result);

		verify(mockPermissionsManager).hasAccess(userInfo, EVALUATION_ID,ACCESS_TYPE.READ);
		verify(mockEvaluationDAO).get(EVALUATION_ID);
		verify(mockEvaluationDAO).getAssociatedEvaluationRounds(EVALUATION_ID, limit+1, offset);
	}
}
