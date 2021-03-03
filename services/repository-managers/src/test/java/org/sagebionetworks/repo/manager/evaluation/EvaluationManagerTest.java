package org.sagebionetworks.repo.manager.evaluation;

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

import org.json.JSONObject;
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
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.dbo.EvaluationRoundTranslationUtil;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationRound;
import org.sagebionetworks.evaluation.model.EvaluationRoundLimit;
import org.sagebionetworks.evaluation.model.EvaluationRoundLimitType;
import org.sagebionetworks.evaluation.model.EvaluationRoundListRequest;
import org.sagebionetworks.evaluation.model.EvaluationRoundListResponse;
import org.sagebionetworks.evaluation.model.SubmissionQuota;
import org.sagebionetworks.evaluation.model.TeamSubmissionEligibility;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.evaluation.EvaluationManagerImpl;
import org.sagebionetworks.repo.manager.evaluation.EvaluationPermissionsManager;
import org.sagebionetworks.repo.manager.evaluation.SubmissionEligibilityManager;
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
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

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
	private SubmissionDAO mockSubmissionDAO;

	@Mock
	private SubmissionEligibilityManager mockSubmissionEligibilityManager;

	@Mock
	private NodeDAO mockNodeDAO;

	@Spy
	@InjectMocks
	private EvaluationManagerImpl evaluationManager = new EvaluationManagerImpl();


	private final Long OWNER_ID = 123L;
	private final Long USER_ID = 456L;
	private UserInfo ownerInfo;
	private UserInfo userInfo;
	private String evaluationRoundId;

	private final String EVALUATION_NAME = "test-evaluation";
	private final String EVALUATION_ID = "1234";
	private final String EVALUATION_CONTENT_SOURCE = "syn12358129748";
	private final String EVALUATION_ETAG = "etag";

	private Date now;
	@BeforeEach
	public void setUp() throws DatastoreException, NotFoundException, InvalidModelException {

		// UserInfo
		ownerInfo = new UserInfo(false, OWNER_ID);
		userInfo = new UserInfo(false, USER_ID);

		// Evaluation
		now = new Date();
		eval = new Evaluation();
		eval.setCreatedOn(now);
		eval.setName(EVALUATION_NAME);
		eval.setOwnerId(ownerInfo.getId().toString());
		eval.setContentSource(EVALUATION_CONTENT_SOURCE);
		eval.setEtag(EVALUATION_ETAG);

		evalWithId = new Evaluation();
		evalWithId.setCreatedOn(now);
		evalWithId.setId(EVALUATION_ID);
		evalWithId.setName(EVALUATION_NAME);
		evalWithId.setOwnerId(ownerInfo.getId().toString());
		evalWithId.setContentSource(EVALUATION_CONTENT_SOURCE);
		evalWithId.setEtag(EVALUATION_ETAG);

		evaluationRoundStart = now.toInstant().plus(1, ChronoUnit.DAYS);
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

		assertEquals("Evaluation ID is required.", errorMessage);
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
	public void testUpdateEvaluationAsOwnerQuotaDefinedHasEvaluationRounds() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		when(mockEvaluationDAO.get(eq(EVALUATION_ID))).thenReturn(evalWithId);

		evaluations= Collections.singletonList(evalWithId);
		when(mockPermissionsManager.hasAccess(eq(ownerInfo), any(), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationStatus.authorized());

		assertNotNull(evalWithId.getCreatedOn());

		//an evaluation round was defined
		when(mockEvaluationDAO.hasEvaluationRounds(EVALUATION_ID)).thenReturn(true);
		//quota set
		SubmissionQuota quota = new SubmissionQuota();
		quota.setNumberOfRounds(123L);
		quota.setFirstRoundStart(new Date());
		quota.setRoundDurationMillis(64209L);
		evalWithId.setQuota(quota);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			evaluationManager.updateEvaluation(ownerInfo, evalWithId);
		}).getMessage();

		assertEquals("DEPRECATED! SubmissionQuota is a DEPRECATED feature and can not co-exist with EvaluationRounds. You must first delete your Evaluation's EvaluationRounds in order to use SubmissionQuota.", message);
		verify(mockEvaluationDAO, never()).update(any());
		verify(mockEvaluationDAO).hasEvaluationRounds(EVALUATION_ID);
	}


	@Test
	public void testUpdateEvaluationAsOwnerQuotaDefinedNotHasEvaluationRounds() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		when(mockEvaluationDAO.get(eq(EVALUATION_ID))).thenReturn(evalWithId);

		evaluations= Collections.singletonList(evalWithId);
		when(mockPermissionsManager.hasAccess(eq(ownerInfo), any(), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationStatus.authorized());

		assertNotNull(evalWithId.getCreatedOn());

		//an evaluation round was defined
		when(mockEvaluationDAO.hasEvaluationRounds(any())).thenReturn(false);
		//quota set
		SubmissionQuota quota = new SubmissionQuota();
		quota.setFirstRoundStart(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
		quota.setNumberOfRounds(2L);
		quota.setRoundDurationMillis(123123L);
		evalWithId.setQuota(quota);


		//method under test
		evaluationManager.updateEvaluation(ownerInfo, evalWithId);

		verify(mockEvaluationDAO).update(eq(evalWithId));
		verify(mockEvaluationDAO).hasEvaluationRounds(EVALUATION_ID);
	}

	@Test
	public void testUpdateEvaluationAsOwnerQuotaNullHasEvaluationRounds() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
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


	/**
	 * Helper for driving other tests that validate evaluation's submissionquota
	 * @param expectedErrorMessage
	 */
	private void helperTestCreateEvaluationSubmissionQuotaValidation(SubmissionQuota quota, String expectedErrorMessage){
		evalWithId.setQuota(quota);

		String errorMsg = assertThrows(IllegalArgumentException.class, () ->{
			//method under test
			evaluationManager.createEvaluation(ownerInfo, evalWithId);
		}).getMessage();

		assertEquals(expectedErrorMessage, errorMsg);

		verifyZeroInteractions(mockEvaluationDAO);
	}

	@Test
	public void testCreateEvaluationSubmissionQuotaNumberOfRoundsNull() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		SubmissionQuota quota = new SubmissionQuota();
		quota.setFirstRoundStart(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
		quota.setRoundDurationMillis(123123L);

		helperTestCreateEvaluationSubmissionQuotaValidation(quota, "numberOfRounds must be defined and be non-negative");
	}

	@Test
	public void testCreateEvaluationSubmissionQuotaNumberOfRoundsNegative() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		SubmissionQuota quota = new SubmissionQuota();
		quota.setNumberOfRounds(-1L);
		quota.setFirstRoundStart(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
		quota.setRoundDurationMillis(123123L);

		helperTestCreateEvaluationSubmissionQuotaValidation(quota, "numberOfRounds must be defined and be non-negative");

	}

	@Test
	public void testCreateEvaluationSubmissionQuotaFirstRoundStartNull() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		SubmissionQuota quota = new SubmissionQuota();
		quota.setNumberOfRounds(1L);
		quota.setRoundDurationMillis(123123L);

		helperTestCreateEvaluationSubmissionQuotaValidation(quota, "firstRoundStart is required.");

	}

	@Test
	public void testCreateEvaluationSubmissionQuotaRoundDurationMillisNull() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		SubmissionQuota quota = new SubmissionQuota();
		quota.setNumberOfRounds(1L);
		quota.setFirstRoundStart(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));

		helperTestCreateEvaluationSubmissionQuotaValidation(quota, "roundDurationMillis must be defined and non-negative");

	}

	@Test
	public void testCreateEvaluationSubmissionQuotaRoundDurationMillisNegative() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		SubmissionQuota quota = new SubmissionQuota();
		quota.setNumberOfRounds(1L);
		quota.setFirstRoundStart(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
		quota.setRoundDurationMillis(-123123L);

		helperTestCreateEvaluationSubmissionQuotaValidation(quota, "roundDurationMillis must be defined and non-negative");
	}


	@Test
	public void testCreateEvaluationSubmissionQuotaSubmissionLimitNegative() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		SubmissionQuota quota = new SubmissionQuota();
		quota.setNumberOfRounds(1L);
		quota.setFirstRoundStart(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
		quota.setRoundDurationMillis(123123L);
		quota.setSubmissionLimit(-123L);

		helperTestCreateEvaluationSubmissionQuotaValidation(quota, "submissionLimit must be non-negative");
	}



	/**
	 * Helper for driving other tests that validate evaluation's submissionquota
	 * @param expectedErrorMessage
	 */
	private void helperTestUpdateEvaluationSubmissionQuotaValidation(SubmissionQuota quota, String expectedErrorMessage){
		evalWithId.setQuota(quota);

		String errorMsg = assertThrows(IllegalArgumentException.class, () ->{
			//method under test
			evaluationManager.updateEvaluation(ownerInfo, evalWithId);
		}).getMessage();

		assertEquals(expectedErrorMessage, errorMsg);

		verifyZeroInteractions(mockEvaluationDAO);
	}

	@Test
	public void testUpdateEvaluationAsOwnerSubmissionQuotaNumberOfRoundsNull() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		SubmissionQuota quota = new SubmissionQuota();
		quota.setFirstRoundStart(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
		quota.setRoundDurationMillis(123123L);

		helperTestUpdateEvaluationSubmissionQuotaValidation(quota, "numberOfRounds must be defined and be non-negative");
	}

	@Test
	public void testUpdateEvaluationAsOwnerSubmissionQuotaNumberOfRoundsNegative() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		SubmissionQuota quota = new SubmissionQuota();
		quota.setNumberOfRounds(-1L);
		quota.setFirstRoundStart(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
		quota.setRoundDurationMillis(123123L);

		helperTestUpdateEvaluationSubmissionQuotaValidation(quota, "numberOfRounds must be defined and be non-negative");

	}

	@Test
	public void testUpdateEvaluationAsOwnerSubmissionQuotaFirstRoundStartNull() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		SubmissionQuota quota = new SubmissionQuota();
		quota.setNumberOfRounds(1L);
		quota.setRoundDurationMillis(123123L);

		helperTestUpdateEvaluationSubmissionQuotaValidation(quota, "firstRoundStart is required.");

	}

	@Test
	public void testUpdateEvaluationAsOwnerSubmissionQuotaRoundDurationMillisNull() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		SubmissionQuota quota = new SubmissionQuota();
		quota.setNumberOfRounds(1L);
		quota.setFirstRoundStart(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));

		helperTestUpdateEvaluationSubmissionQuotaValidation(quota, "roundDurationMillis must be defined and non-negative");

	}

	@Test
	public void testUpdateEvaluationAsOwnerSubmissionQuotaRoundDurationMillisNegative() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		SubmissionQuota quota = new SubmissionQuota();
		quota.setNumberOfRounds(1L);
		quota.setFirstRoundStart(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
		quota.setRoundDurationMillis(-123123L);

		helperTestUpdateEvaluationSubmissionQuotaValidation(quota, "roundDurationMillis must be defined and non-negative");
	}


	@Test
	public void testUpdateEvaluationAsOwnerSubmissionQuotaSubmissionLimitNegative() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		SubmissionQuota quota = new SubmissionQuota();
		quota.setNumberOfRounds(1L);
		quota.setFirstRoundStart(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
		quota.setRoundDurationMillis(123123L);
		quota.setSubmissionLimit(-123L);

		helperTestUpdateEvaluationSubmissionQuotaValidation(quota, "submissionLimit must be non-negative");
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
		when(mockSubmissionEligibilityManager.getTeamSubmissionEligibility(eq(eval), eq(TEAM_ID), any(Date.class))).
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
	public void validateEvaluationAccessNullUserInfo (){
		UserInfo nullInfo = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			((EvaluationManagerImpl) evaluationManager).validateEvaluationAccess(nullInfo, EVALUATION_ID, ACCESS_TYPE.READ);
		}).getMessage();

		assertEquals("UserInfo cannot be null", message);
		verifyZeroInteractions(mockEvaluationDAO);
		verifyZeroInteractions(mockPermissionsManager);
	}

	@Test
	public void validateEvaluationAccessNoPermissionToAcccess (){
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
	public void validateEvaluationAccessHasPermission (){
		when(mockPermissionsManager.hasAccess(userInfo,EVALUATION_ID,ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockEvaluationDAO.get(EVALUATION_ID)).thenReturn(evalWithId);


		Evaluation result = ((EvaluationManagerImpl) evaluationManager).validateEvaluationAccess(userInfo, EVALUATION_ID, ACCESS_TYPE.READ);

		assertEquals(evalWithId, result);
		verify(mockEvaluationDAO).get(EVALUATION_ID);
		verify(mockPermissionsManager).hasAccess(userInfo,EVALUATION_ID,ACCESS_TYPE.READ);
	}

	@Test
	public void validateNoDateRangeOverlapHasOverlappingRounds(){
		String overlappingId = "890890";
		EvaluationRound overlappingRound = new EvaluationRound();
		overlappingRound.setId(overlappingId);
		when(mockEvaluationDAO.overlappingEvaluationRounds(EVALUATION_ID, evaluationRoundId, evaluationRoundStart, evaluationRoundEnd))
				.thenReturn(Arrays.asList(overlappingRound));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			((EvaluationManagerImpl) evaluationManager).validateNoDateRangeOverlap(evaluationRound, evaluationRoundId);
		}).getMessage();

		assertEquals("This round's date range overlaps with the following round IDs: ["+overlappingId+"]", message);
		verify(mockEvaluationDAO).overlappingEvaluationRounds(EVALUATION_ID, evaluationRoundId, evaluationRoundStart, evaluationRoundEnd);
	}

	@Test
	public void validateNoDateRangeOverlapRoundEndEarlierThanRoundStart(){
		//swap start and end
		evaluationRound.setRoundStart(Date.from(evaluationRoundEnd));
		evaluationRound.setRoundEnd(Date.from(evaluationRoundStart));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			((EvaluationManagerImpl) evaluationManager).validateNoDateRangeOverlap(evaluationRound, evaluationRoundId);
		}).getMessage();

		assertEquals("EvaluationRound can not end before it starts", message);
		verifyZeroInteractions(mockEvaluationDAO);
	}

	@Test
	public void validateNoDateRangeOverlapNoOverlappingRounds(){
		when(mockEvaluationDAO.overlappingEvaluationRounds(EVALUATION_ID, evaluationRoundId, evaluationRoundStart, evaluationRoundEnd))
				.thenReturn(Collections.emptyList());
		assertDoesNotThrow(() ->
			((EvaluationManagerImpl) evaluationManager).validateNoDateRangeOverlap(evaluationRound, evaluationRoundId)
		);

		verify(mockEvaluationDAO).overlappingEvaluationRounds(EVALUATION_ID, evaluationRoundId, evaluationRoundStart, evaluationRoundEnd);
	}

	@Test
	public void validateNoExistingQuotaDefinedHasQuota(){
		evalWithId.setQuota(new SubmissionQuota());

		String message = assertThrows(IllegalArgumentException.class, () ->
				((EvaluationManagerImpl) evaluationManager).validateNoExistingQuotaDefined(evalWithId)
		).getMessage();

		assertEquals("A SubmissionQuota, which is deprecated, must not be defined for an Evaluation." +
						" You must first remove your Evaluation's SubmissionQuota or convert the SubmissionQuota" +
						" into EvaluationRounds automatically to via the EvaluationRound migration service",
				message);
	}

	@Test
	public void validateNoExistingQuotaDefinedNoQuota(){
		evalWithId.setQuota(null);
		assertDoesNotThrow(() ->
				((EvaluationManagerImpl) evaluationManager).validateNoExistingQuotaDefined(evalWithId)
		);
	}


	@Test
	public void createEvaluationRound(){
		when(mockPermissionsManager.hasAccess(userInfo,EVALUATION_ID,ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockEvaluationDAO.get(EVALUATION_ID)).thenReturn(evalWithId);
		//fake round that gets "created"
		EvaluationRound createdRound = new EvaluationRound();
		when(mockEvaluationDAO.createEvaluationRound(evaluationRound)).thenReturn(createdRound);

		EvaluationRound result = evaluationManager.createEvaluationRound(userInfo, evaluationRound);

		assertEquals(createdRound, result);

		verify(evaluationManager).validateEvaluationAccess(userInfo, evaluationRound.getEvaluationId(), ACCESS_TYPE.UPDATE);
//		verify(evaluationManager).validateNoExistingQuotaDefined(evalWithId);
		verify(evaluationManager).validateEvaluationRoundLimits(evaluationRound.getLimits());
		verify(evaluationManager).validateNoDateRangeOverlap(evaluationRound, EvaluationManagerImpl.NON_EXISTENT_ROUND_ID);

		verify(mockIdGenerator).generateNewId(IdType.EVALUATION_ROUND_ID);
		verify(mockPermissionsManager).hasAccess(userInfo, EVALUATION_ID,ACCESS_TYPE.UPDATE);
		verify(mockEvaluationDAO).get(EVALUATION_ID);
		verify(mockEvaluationDAO).overlappingEvaluationRounds(EVALUATION_ID, EvaluationManagerImpl.NON_EXISTENT_ROUND_ID, evaluationRoundStart, evaluationRoundEnd);
		verify(mockEvaluationDAO).createEvaluationRound(evaluationRound);
	}

	@Test
	public void createEvaluationRoundStartDateInPast(){
		when(mockPermissionsManager.hasAccess(userInfo,EVALUATION_ID,ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockEvaluationDAO.get(EVALUATION_ID)).thenReturn(evalWithId);

		evaluationRound.setRoundStart(Date.from(Instant.now().minus(3, ChronoUnit.SECONDS)));

		String message = assertThrows(IllegalArgumentException.class,
				() -> evaluationManager.createEvaluationRound(userInfo, evaluationRound)
		).getMessage();
		assertEquals("Can not create an EvaluationRound with a start date in the past.", message);
	}

	@Test
	public void createEvaluationRoundEndDateInPast(){
		when(mockPermissionsManager.hasAccess(userInfo,EVALUATION_ID,ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockEvaluationDAO.get(EVALUATION_ID)).thenReturn(evalWithId);

		evaluationRound.setRoundEnd(Date.from(Instant.now().minus(2, ChronoUnit.SECONDS)));

		String message = assertThrows(IllegalArgumentException.class,
				() -> evaluationManager.createEvaluationRound(userInfo, evaluationRound)
		).getMessage();
		assertEquals("Can not create an EvaluationRound with an end date in the past.", message);
	}

	@Test
	public void updateEvaluationRound(){
		when(mockPermissionsManager.hasAccess(userInfo,EVALUATION_ID,ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockEvaluationDAO.get(EVALUATION_ID)).thenReturn(evalWithId);
		when(mockEvaluationDAO.getEvaluationRound(EVALUATION_ID, evaluationRoundId)).thenReturn(evaluationRound);

		EvaluationRound result = evaluationManager.updateEvaluationRound(userInfo, evaluationRound);

		assertEquals(evaluationRound, result);

		verify(evaluationManager).validateEvaluationAccess(userInfo, evaluationRound.getEvaluationId(), ACCESS_TYPE.UPDATE);
//		verify(evaluationManager).validateNoExistingQuotaDefined(evalWithId);
		verify(evaluationManager).validateEvaluationRoundLimits(evaluationRound.getLimits());
		verify(evaluationManager).validateNoDateRangeOverlap(evaluationRound, evaluationRoundId);

		verify(mockPermissionsManager).hasAccess(userInfo, EVALUATION_ID,ACCESS_TYPE.UPDATE);
		verify(mockEvaluationDAO).get(EVALUATION_ID);
		verify(mockEvaluationDAO).overlappingEvaluationRounds(EVALUATION_ID, evaluationRoundId, evaluationRoundStart, evaluationRoundEnd);
		verify(mockEvaluationDAO).updateEvaluationRound(evaluationRound);
		verify(mockEvaluationDAO, times(2)).getEvaluationRound(EVALUATION_ID, evaluationRoundId);
	}

	@Test
	public void updateEvaluationRoundStartDateChangedToBeforeCurrentTime() throws JSONObjectAdapterException {
		when(mockPermissionsManager.hasAccess(userInfo,EVALUATION_ID,ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockEvaluationDAO.get(EVALUATION_ID)).thenReturn(evalWithId);
		when(mockEvaluationDAO.getEvaluationRound(EVALUATION_ID, evaluationRoundId)).thenReturn(evaluationRound);

		JSONObject jsonObject = EntityFactory.createJSONObjectForEntity(evaluationRound);
		EvaluationRound evaluationRoundModified = EntityFactory.createEntityFromJSONObject(jsonObject, EvaluationRound.class);
		evaluationRoundModified.setRoundEnd(Date.from(Instant.now().minus(2, ChronoUnit.SECONDS)));


		String message = assertThrows(IllegalArgumentException.class, () ->
				evaluationManager.updateEvaluationRound(userInfo, evaluationRoundModified)
		).getMessage();

		assertEquals("Can not update an EvaluationRound's end date to a time in the past.", message);
	}

	@Test
	public void updateEvaluationRoundEndDateChangedToBeforeCurrentTime() throws JSONObjectAdapterException {
		when(mockPermissionsManager.hasAccess(userInfo,EVALUATION_ID,ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockEvaluationDAO.get(EVALUATION_ID)).thenReturn(evalWithId);
		when(mockEvaluationDAO.getEvaluationRound(EVALUATION_ID, evaluationRoundId)).thenReturn(evaluationRound);

		JSONObject jsonObject = EntityFactory.createJSONObjectForEntity(evaluationRound);
		EvaluationRound evaluationRoundModified = EntityFactory.createEntityFromJSONObject(jsonObject, EvaluationRound.class);
		evaluationRoundModified.setRoundStart(Date.from(Instant.now().minus(2, ChronoUnit.SECONDS)));
		when(mockSubmissionDAO.hasSubmissionForEvaluationRound(EVALUATION_ID, evaluationRoundId)).thenReturn(true);

		String message = assertThrows(IllegalArgumentException.class, () ->
				evaluationManager.updateEvaluationRound(userInfo, evaluationRoundModified)
		).getMessage();

		assertEquals("Can not update an EvaluationRound's start date after it has already started and Submissions have been made", message);
	}

	@Test
	public void deleteEvaluationRoundAfterRoundStartedHasSubmissionRounds(){
		when(mockPermissionsManager.hasAccess(userInfo,EVALUATION_ID,ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockEvaluationDAO.get(EVALUATION_ID)).thenReturn(evalWithId);

		evaluationRound.setRoundStart(Date.from(Instant.now().minus(2, ChronoUnit.SECONDS)));
		when(mockEvaluationDAO.getEvaluationRound(EVALUATION_ID, evaluationRoundId)).thenReturn(evaluationRound);
		when(mockSubmissionDAO.hasSubmissionForEvaluationRound(EVALUATION_ID, evaluationRoundId)).thenReturn(true);


		String message = assertThrows(IllegalArgumentException.class, () ->
				evaluationManager.deleteEvaluationRound(userInfo, EVALUATION_ID, evaluationRoundId)
		).getMessage();

		assertEquals("Can not delete an EvaluationRound after it has already started and Submissions have been made", message);
	}
	@Test
	public void deleteEvaluationRoundAfterRoundStartedNoSubmissionRounds(){
		when(mockPermissionsManager.hasAccess(userInfo,EVALUATION_ID,ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockEvaluationDAO.get(EVALUATION_ID)).thenReturn(evalWithId);
		when(mockEvaluationDAO.getEvaluationRound(EVALUATION_ID, evaluationRoundId)).thenReturn(evaluationRound);

		evaluationRound.setRoundStart(Date.from(Instant.now().minus(2, ChronoUnit.SECONDS)));
		when(mockEvaluationDAO.getEvaluationRound(EVALUATION_ID, evaluationRoundId)).thenReturn(evaluationRound);
		when(mockSubmissionDAO.hasSubmissionForEvaluationRound(EVALUATION_ID, evaluationRoundId)).thenReturn(false);

		assertDoesNotThrow(() ->
			evaluationManager.deleteEvaluationRound(userInfo ,EVALUATION_ID, evaluationRoundId)
		);

		verify(evaluationManager).validateEvaluationAccess(userInfo, evaluationRound.getEvaluationId(), ACCESS_TYPE.UPDATE);
		verify(mockEvaluationDAO).getEvaluationRound(EVALUATION_ID, evaluationRoundId);
		verify(mockPermissionsManager).hasAccess(userInfo, EVALUATION_ID,ACCESS_TYPE.UPDATE);
		verify(mockEvaluationDAO).get(EVALUATION_ID);
		verify(mockEvaluationDAO).deleteEvaluationRound(EVALUATION_ID, evaluationRoundId);
	}

	@Test
	public void deleteEvaluationRound(){
		when(mockPermissionsManager.hasAccess(userInfo,EVALUATION_ID,ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockEvaluationDAO.get(EVALUATION_ID)).thenReturn(evalWithId);
		when(mockEvaluationDAO.getEvaluationRound(EVALUATION_ID, evaluationRoundId)).thenReturn(evaluationRound);

		evaluationManager.deleteEvaluationRound(userInfo ,EVALUATION_ID, evaluationRoundId);

		verify(evaluationManager).validateEvaluationAccess(userInfo, evaluationRound.getEvaluationId(), ACCESS_TYPE.UPDATE);
		verify(mockEvaluationDAO).getEvaluationRound(EVALUATION_ID, evaluationRoundId);
		verify(mockPermissionsManager).hasAccess(userInfo, EVALUATION_ID,ACCESS_TYPE.UPDATE);
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


		verify(evaluationManager).validateEvaluationAccess(userInfo, evaluationRound.getEvaluationId(), ACCESS_TYPE.READ);

		verify(mockPermissionsManager).hasAccess(userInfo, EVALUATION_ID,ACCESS_TYPE.READ);
		verify(mockEvaluationDAO).get(EVALUATION_ID);
		verify(mockEvaluationDAO).getEvaluationRound(EVALUATION_ID, evaluationRoundId);
	}


	@Test
	public void testGetAllEvaluationRoundsNullEmptyEvaluationId(){
		EvaluationRoundListRequest request = new EvaluationRoundListRequest();
		String message = assertThrows(IllegalArgumentException.class, () ->
				evaluationManager.getAllEvaluationRounds(userInfo, null, request)
		).getMessage();

		assertEquals("evaluationId is required and must not be the empty string.", message);

		message = assertThrows(IllegalArgumentException.class, () ->
				evaluationManager.getAllEvaluationRounds(userInfo, "", request)
		).getMessage();

		assertEquals("evaluationId is required and must not be the empty string.", message);

		message = assertThrows(IllegalArgumentException.class, () ->
				evaluationManager.getAllEvaluationRounds(userInfo, "       ", request)
		).getMessage();

		assertEquals("evaluationId is required and must not be a blank string.", message);
	}

	@Test
	public void testGetAllEvaluationRoundsNullRequest(){

		EvaluationRoundListRequest nullRequest = null;
		String message = assertThrows(IllegalArgumentException.class, () ->
				evaluationManager.getAllEvaluationRounds(userInfo, EVALUATION_ID, nullRequest)
		).getMessage();

		assertEquals("request is required.", message);
	}


	@Test
	public void testGetAllEvaluationRounds(){
		long limit = 50;
		long offset = 0;
		NextPageToken nextPageToken = new NextPageToken(limit, offset);
		EvaluationRoundListRequest request = new EvaluationRoundListRequest();
		request.setNextPageToken(nextPageToken.toToken());
		List<EvaluationRound> rounds = Collections.singletonList(evaluationRound);

		when(mockPermissionsManager.hasAccess(userInfo,EVALUATION_ID,ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockEvaluationDAO.get(EVALUATION_ID)).thenReturn(evalWithId);
		when(mockEvaluationDAO.getAssociatedEvaluationRounds(EVALUATION_ID, limit+1, offset)).thenReturn(rounds);


		EvaluationRoundListResponse result = evaluationManager.getAllEvaluationRounds(userInfo, EVALUATION_ID, request);

		EvaluationRoundListResponse expected = new EvaluationRoundListResponse();
		expected.setPage(rounds);

		assertEquals(expected, result);

		verify(evaluationManager).validateEvaluationAccess(userInfo, evaluationRound.getEvaluationId(), ACCESS_TYPE.READ);

		verify(mockPermissionsManager).hasAccess(userInfo, EVALUATION_ID,ACCESS_TYPE.READ);
		verify(mockEvaluationDAO).get(EVALUATION_ID);
		verify(mockEvaluationDAO).getAssociatedEvaluationRounds(EVALUATION_ID, limit+1, offset);
	}

	@Test
	public void testValidateEvaluationRoundLimitsNullOrEmpty(){
		assertDoesNotThrow(()->
			((EvaluationManagerImpl) evaluationManager).validateEvaluationRoundLimits(null)
		);
		assertDoesNotThrow(()->
			((EvaluationManagerImpl) evaluationManager).validateEvaluationRoundLimits(Collections.emptyList())
		);
	}

	@Test
	public void testValidateEvaluationRoundLimitsDuplicateLimitType(){
		List<EvaluationRoundLimit> duplicateTypeLimits = Arrays.asList(
				newLimit(EvaluationRoundLimitType.DAILY, 123),
				newLimit(EvaluationRoundLimitType.DAILY, 456)
		);
		String message = assertThrows(IllegalArgumentException.class, ()->
			((EvaluationManagerImpl) evaluationManager).validateEvaluationRoundLimits(duplicateTypeLimits)
		).getMessage();

		assertEquals("You may only have 1 limit of type: DAILY", message);
	}

	@Test
	public void testValidateEvaluationRoundLimitsNegativeMaxSubmissions(){
		List<EvaluationRoundLimit> negativeMaxSubmission = Arrays.asList(
				newLimit(EvaluationRoundLimitType.DAILY, 123),
				newLimit(EvaluationRoundLimitType.WEEKLY, -456),
				newLimit(EvaluationRoundLimitType.MONTHLY, 789)
		);
		String message = assertThrows(IllegalArgumentException.class, ()->
				((EvaluationManagerImpl) evaluationManager).validateEvaluationRoundLimits(negativeMaxSubmission)
		).getMessage();

		assertEquals("maxSubmissions must be a positive integer", message);
	}

	@Test
	public void testValidateEvaluationRoundLimitsHappy(){
		List<EvaluationRoundLimit> negativeMaxSubmission = Arrays.asList(
				newLimit(EvaluationRoundLimitType.DAILY, 123),
				newLimit(EvaluationRoundLimitType.WEEKLY, 456),
				newLimit(EvaluationRoundLimitType.MONTHLY, 789)
		);
		assertDoesNotThrow(()->
			((EvaluationManagerImpl) evaluationManager).validateEvaluationRoundLimits(negativeMaxSubmission)
		);
	}

	@Test
	public void testMigrateSubmissionQuotaUnauthorizedUser(){
		when(mockEvaluationDAO.get(EVALUATION_ID)).thenReturn(evalWithId);
		when(mockPermissionsManager.hasAccess(eq(userInfo), any(), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationStatus.accessDenied(""));

		assertThrows(UnauthorizedException.class,() ->
			evaluationManager.migrateSubmissionQuota(userInfo, EVALUATION_ID)
		);
	}

	@Test
	public void testMigrateSubmissionQuotaAuthorizedUser() throws JSONObjectAdapterException {
		when(mockEvaluationDAO.get(eq(EVALUATION_ID))).thenReturn(evalWithId);
		when(mockPermissionsManager.hasAccess(eq(ownerInfo), any(), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationStatus.authorized());
		when(mockIdGenerator.generateNewId(IdType.EVALUATION_ROUND_ID)).thenReturn(0L,1L);

		SubmissionQuota quota = new SubmissionQuota();
		quota.setNumberOfRounds(2L);
		quota.setFirstRoundStart(Date.from(evaluationRoundStart));
		//2 days in milliseconds
		quota.setRoundDurationMillis(172800000L);
		quota.setSubmissionLimit(123L);
		evalWithId.setQuota(quota);

		//create a copy of the Evaluation before it is modified by the tested function
		Evaluation expectedChangedEval = EntityFactory.createEntityFromJSONString(EntityFactory.createJSONStringForEntity(evalWithId), Evaluation.class);
		expectedChangedEval.setQuota(null);

		//method under test
		evaluationManager.migrateSubmissionQuota(ownerInfo, EVALUATION_ID);

		EvaluationRoundLimit expectedLimit = new EvaluationRoundLimit();
		expectedLimit.setLimitType(EvaluationRoundLimitType.TOTAL);
		expectedLimit.setMaximumSubmissions(123L);

		EvaluationRound expectedRound1 = new EvaluationRound();
		expectedRound1.setId("0");
		expectedRound1.setRoundStart(Date.from(evaluationRoundStart));
		expectedRound1.setRoundEnd(Date.from(evaluationRoundStart.plus(2, ChronoUnit.DAYS)));
		expectedRound1.setEvaluationId(EVALUATION_ID);
		expectedRound1.setLimits(Collections.singletonList(expectedLimit));

		EvaluationRound expectedRound2 = new EvaluationRound();
		expectedRound2.setRoundStart(Date.from(evaluationRoundStart.plus(2, ChronoUnit.DAYS)));
		expectedRound2.setRoundEnd(Date.from(evaluationRoundStart.plus(4, ChronoUnit.DAYS)));
		expectedRound2.setId("1");
		expectedRound2.setEvaluationId(EVALUATION_ID);
		expectedRound2.setLimits(Collections.singletonList(expectedLimit));

		//converted into 2 rounds and created both
		verify(mockEvaluationDAO).createEvaluationRound(expectedRound1);
		verify(mockEvaluationDAO).createEvaluationRound(expectedRound2);
		verify(mockEvaluationDAO).update(expectedChangedEval);
	}


	@Test
	public void testMigrateSubmissionQuotaAuthorizedUserNoQuota() throws JSONObjectAdapterException {
		when(mockEvaluationDAO.get(eq(EVALUATION_ID))).thenReturn(evalWithId);
		when(mockPermissionsManager.hasAccess(eq(ownerInfo), any(), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationStatus.authorized());

		evalWithId.setQuota(null);

		assertThrows(IllegalArgumentException.class, () -> {
			//method under test
			evaluationManager.migrateSubmissionQuota(ownerInfo, EVALUATION_ID);
		});

		verifyZeroInteractions(mockEvaluationDAO);
	}

	private EvaluationRoundLimit newLimit(EvaluationRoundLimitType type, long maxSubmission){
		EvaluationRoundLimit evaluationRoundLimit = new EvaluationRoundLimit();
		evaluationRoundLimit.setLimitType(type);
		evaluationRoundLimit.setMaximumSubmissions(maxSubmission);
		return evaluationRoundLimit;
	}
}
