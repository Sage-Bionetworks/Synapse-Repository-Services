package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.model.AccessRequirementStats;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionFulfillment;
import org.sagebionetworks.repo.model.RestrictionInformationBatchRequest;
import org.sagebionetworks.repo.model.RestrictionInformationBatchResponse;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.AccessRequirementType;
import org.sagebionetworks.repo.model.ar.AccessRestrictionStatusDao;
import org.sagebionetworks.repo.model.ar.UsersRequirementStatus;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;
import org.sagebionetworks.repo.model.dbo.entity.UsersEntityPermissionsDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

@ExtendWith(MockitoExtension.class)
public class RestrictionInformationManagerImplUnitTest {

	private static final String TEST_PRINCIPAL_ID = "1010101";
	private static final String TEST_ENTITY_ID = "syn98786543";

	@Mock
	private AccessRestrictionStatusDao mockRestrictionStatusDao;
	@Mock
	private UsersEntityPermissionsDao mockUsersEntityPermissionsDao;
	@Mock
	private EntityAuthorizationManager mockEntityAuthorizationManager;
	@Mock
	private UserEntityPermissions mockUserEntityPermissions;

	@InjectMocks
	private RestrictionInformationManagerImpl arm;

	private UserInfo userInfo;

	private Node testEntity;
	private Long entityIdAsLong;
	private Long teamIdAsLong;
	private Map<Long, UsersRestrictionStatus> mapIdToAccess = new LinkedHashMap<Long, UsersRestrictionStatus>();
	private Map<Long, UserEntityPermissionsState> userEntityPermissionsState = new LinkedHashMap<Long,UserEntityPermissionsState>();

	@Mock
	private Supplier<List<Long>> mockUnmetArIdsSupplier;
	
	@BeforeEach
	public void setUp() throws Exception {
		entityIdAsLong = KeyFactory.stringToKey(TEST_ENTITY_ID);
		teamIdAsLong = KeyFactory.stringToKey(TEST_PRINCIPAL_ID);
		
		userInfo = new UserInfo(false, teamIdAsLong);
		
		testEntity = new Node();
		testEntity.setCreatedByPrincipalId(999L); // different from TEST_PRINCIPAL_ID
		testEntity.setNodeType(EntityType.file);
		
		userEntityPermissionsState.put(entityIdAsLong,
				new UserEntityPermissionsState(entityIdAsLong)
					.withHasUpdate(false)
					.withHasRead(true)
					.withHasDelete(true)
					.withHasDownload(true));
	}

	@Test
	public void testGetRestrictionInformationWithNullUserInfo() {
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		assertThrows(IllegalArgumentException.class, () -> {
			arm.getRestrictionInformation(null, request);
		});
	}

	@Test
	public void testGetRestrictionInformationWithNullRequest() {
		assertThrows(IllegalArgumentException.class, () -> {
			arm.getRestrictionInformation(userInfo, null);
		});
	}

	@Test
	public void testGetRestrictionInformationWithNullObjectId() {
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		assertThrows(IllegalArgumentException.class, () -> {
			arm.getRestrictionInformation(userInfo, request);
		});
	}

	@Test
	public void testGetRestrictionInformationWithNullObjectType() {
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		assertThrows(IllegalArgumentException.class, () -> {
			arm.getRestrictionInformation(userInfo, request);
		});
	}

	@Test
	public void testGetRestrictionInformationWithEvaluation() {
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.EVALUATION);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			arm.getRestrictionInformation(userInfo, request);
		}).getMessage();
		assertEquals("Unsupported type: EVALUATION", message);
	}

	@Test
	public void testGetRestrictionInformationWithZeroARForEntity() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(entityIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(Collections.emptyList());
		
		mapIdToAccess.put(entityIdAsLong, touStatus);
		
		when(mockRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(userEntityPermissionsState);
		when(mockEntityAuthorizationManager.getUserPermissionsForEntity(eq(userInfo), eq(TEST_ENTITY_ID), any())).thenReturn(mockUserEntityPermissions);
		
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		
		RestrictionInformationResponse expected = new RestrictionInformationResponse()
			.setObjectId(entityIdAsLong)
			.setHasUnmetAccessRequirement(false)
			.setUserEntityPermissions(mockUserEntityPermissions)
			.setRestrictionDetails(Collections.emptyList())
			.setRestrictionLevel(RestrictionLevel.OPEN);
		
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		
		assertEquals(expected, info);
		
		verify(mockRestrictionStatusDao).getEntityStatusAsMap(Arrays.asList(entityIdAsLong), userInfo.getId(), userInfo.getGroups());
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), List.of(entityIdAsLong));
	}

	@Test
	public void testGetRestrictionInformationBatchWithZeroARForTeam() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(teamIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(Collections.emptyList());
		when(mockRestrictionStatusDao.getNonEntityStatus(any(), any(), any())).thenReturn(List.of(touStatus));
		long teamId = Long.parseLong(TEST_PRINCIPAL_ID);
		RestrictionInformationBatchRequest request = new RestrictionInformationBatchRequest();
		request.setObjectIds(List.of(TEST_PRINCIPAL_ID));
		request.setRestrictableObjectType(RestrictableObjectType.TEAM);
		AccessRequirementStats stats = new AccessRequirementStats();
		stats.setRequirementIdSet(new HashSet<String>());
		
		RestrictionInformationBatchResponse expected = new RestrictionInformationBatchResponse().setRestrictionInformation(List.of(
			new RestrictionInformationResponse()
				.setHasUnmetAccessRequirement(false)
				.setRestrictionLevel(RestrictionLevel.OPEN)
				.setUserEntityPermissions(null)
				.setObjectId(teamIdAsLong)
				.setRestrictionDetails(Collections.emptyList())
		));
		
		RestrictionInformationBatchResponse info = arm.getTeamRestrictionInformationBatchResponse(userInfo, request);
		
		assertEquals(expected, info);
		
		verify(mockRestrictionStatusDao).getNonEntityStatus(Arrays.asList(teamId), RestrictableObjectType.TEAM, userInfo.getId());
	}

	@Test
	public void testGetRestrictionInformationBatchWithToUForEntity() {
		when(mockEntityAuthorizationManager.getUserPermissionsForEntity(eq(userInfo), eq(TEST_ENTITY_ID), any())).thenReturn(mockUserEntityPermissions);
		when(mockUserEntityPermissions.getIsDataContributor()).thenReturn(false);
		
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(entityIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus()
							.withRequirementId(123L)
							.withIsUnmet(true)
							.withRequirementType(AccessRequirementType.SELF_SIGNED)));
		mapIdToAccess.put(entityIdAsLong, touStatus);
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(userEntityPermissionsState);
		when(mockRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		RestrictionInformationBatchRequest request = new RestrictionInformationBatchRequest();
		
		request.setObjectIds(List.of(TEST_ENTITY_ID));
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		
		RestrictionInformationBatchResponse expected = new RestrictionInformationBatchResponse().setRestrictionInformation(List.of(
			new RestrictionInformationResponse()
				.setHasUnmetAccessRequirement(true)
				.setRestrictionLevel(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE)
				.setUserEntityPermissions(mockUserEntityPermissions)
				.setObjectId(entityIdAsLong)
				.setRestrictionDetails(List.of(new RestrictionFulfillment().setAccessRequirementId(123L).setIsApproved(false).setIsExempt(false).setIsMet(false)))
		));
		
		RestrictionInformationBatchResponse info = arm.getEntityRestrictionInformationBatchResponse(userInfo, request);
		
		assertEquals(expected, info);
				
		verify(mockRestrictionStatusDao).getEntityStatusAsMap(Arrays.asList(entityIdAsLong), userInfo.getId(), userInfo.getGroups());
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), List.of(entityIdAsLong));
	}

	@Test
	public void testGetRestrictionInformationBatchWithToUForTeam() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(teamIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus()
							.withRequirementId(123L)
							.withIsUnmet(true)
							.withRequirementType(AccessRequirementType.SELF_SIGNED)));
		
		when(mockRestrictionStatusDao.getNonEntityStatus(any(), any(), any())).thenReturn(List.of(touStatus));
		long teamId = Long.parseLong(TEST_PRINCIPAL_ID);
		RestrictionInformationBatchRequest request = new RestrictionInformationBatchRequest();
		
		request.setObjectIds(List.of(TEST_PRINCIPAL_ID));
		request.setRestrictableObjectType(RestrictableObjectType.TEAM);
		
		RestrictionInformationBatchResponse expected = new RestrictionInformationBatchResponse().setRestrictionInformation(List.of(
			new RestrictionInformationResponse()
				.setHasUnmetAccessRequirement(true)
				.setRestrictionLevel(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE)
				.setUserEntityPermissions(null)
				.setObjectId(teamIdAsLong)
				.setRestrictionDetails(List.of(new RestrictionFulfillment().setAccessRequirementId(123L).setIsApproved(false).setIsExempt(false).setIsMet(false)))
		));
		
		RestrictionInformationBatchResponse info = arm.getTeamRestrictionInformationBatchResponse(userInfo, request);

		assertEquals(expected, info);
		
		verify(mockRestrictionStatusDao).getNonEntityStatus(Arrays.asList(teamId), RestrictableObjectType.TEAM, userInfo.getId());
	}
	
	@Test
	public void testGetRestrictionInformationBatchWithToUMetForEntity() {
		when(mockEntityAuthorizationManager.getUserPermissionsForEntity(eq(userInfo), eq(TEST_ENTITY_ID), any())).thenReturn(mockUserEntityPermissions);
		when(mockUserEntityPermissions.getIsDataContributor()).thenReturn(false);

		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(entityIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus()
							.withRequirementId(123L)
							.withIsUnmet(false)
							.withRequirementType(AccessRequirementType.SELF_SIGNED)));
		
		mapIdToAccess.put(entityIdAsLong, touStatus);
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(userEntityPermissionsState);
		when(mockRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		RestrictionInformationBatchRequest request = new RestrictionInformationBatchRequest();
		
		request.setObjectIds(List.of(TEST_ENTITY_ID));
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		
		RestrictionInformationBatchResponse expected = new RestrictionInformationBatchResponse().setRestrictionInformation(List.of(
			new RestrictionInformationResponse()
				.setHasUnmetAccessRequirement(false)
				.setRestrictionLevel(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE)
				.setUserEntityPermissions(mockUserEntityPermissions)
				.setObjectId(entityIdAsLong)
				.setRestrictionDetails(List.of(new RestrictionFulfillment().setAccessRequirementId(123L).setIsApproved(true).setIsExempt(false).setIsMet(true)))
		));
		
		RestrictionInformationBatchResponse info = arm.getEntityRestrictionInformationBatchResponse(userInfo, request);
		
		assertEquals(expected, info);
		
		verify(mockRestrictionStatusDao).getEntityStatusAsMap(Arrays.asList(entityIdAsLong), userInfo.getId(), userInfo.getGroups());
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), List.of(entityIdAsLong));
	}

	@Test
	public void testGetRestrictionInformationBatchWithToUMetForTeam() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(teamIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus()
							.withRequirementId(123L)
							.withIsUnmet(false)
							.withRequirementType(AccessRequirementType.SELF_SIGNED)));
		
		when(mockRestrictionStatusDao.getNonEntityStatus(any(), any(), any())).thenReturn(List.of(touStatus));
		long teamId = Long.parseLong(TEST_PRINCIPAL_ID);
		RestrictionInformationBatchRequest request = new RestrictionInformationBatchRequest();
		request.setObjectIds(List.of(TEST_PRINCIPAL_ID));
		request.setRestrictableObjectType(RestrictableObjectType.TEAM);
		
		RestrictionInformationBatchResponse expected = new RestrictionInformationBatchResponse().setRestrictionInformation(List.of(
			new RestrictionInformationResponse()
				.setHasUnmetAccessRequirement(false)
				.setRestrictionLevel(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE)
				.setUserEntityPermissions(null)
				.setObjectId(teamIdAsLong)
				.setRestrictionDetails(List.of(new RestrictionFulfillment().setAccessRequirementId(123L).setIsApproved(true).setIsExempt(false).setIsMet(true)))
		));
		
		RestrictionInformationBatchResponse info = arm.getTeamRestrictionInformationBatchResponse(userInfo, request);
		
		assertEquals(expected, info);

		verify(mockRestrictionStatusDao).getNonEntityStatus(Arrays.asList(teamId), RestrictableObjectType.TEAM, userInfo.getId());
	}

	@Test
	public void testGetRestrictionInformationBatchWithLockForEntity() {
		when(mockEntityAuthorizationManager.getUserPermissionsForEntity(eq(userInfo), eq(TEST_ENTITY_ID), any())).thenReturn(mockUserEntityPermissions);
		when(mockUserEntityPermissions.getIsDataContributor()).thenReturn(false);

		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(entityIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus().withRequirementId(123L).withIsUnmet(true).withRequirementType(AccessRequirementType.LOCK)));
		mapIdToAccess.put(entityIdAsLong, touStatus);
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(userEntityPermissionsState);
		when(mockRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		RestrictionInformationBatchRequest request = new RestrictionInformationBatchRequest();
		request.setObjectIds(List.of(TEST_ENTITY_ID));
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		
		RestrictionInformationBatchResponse expected = new RestrictionInformationBatchResponse().setRestrictionInformation(List.of(
			new RestrictionInformationResponse()
				.setHasUnmetAccessRequirement(true)
				.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT)
				.setUserEntityPermissions(mockUserEntityPermissions)
				.setObjectId(entityIdAsLong)
				.setRestrictionDetails(List.of(new RestrictionFulfillment().setAccessRequirementId(123L).setIsApproved(false).setIsExempt(false).setIsMet(false)))
		));
		
		RestrictionInformationBatchResponse info = arm.getEntityRestrictionInformationBatchResponse(userInfo, request);
		
		assertEquals(expected, info);
		
		verify(mockRestrictionStatusDao).getEntityStatusAsMap(Arrays.asList(entityIdAsLong), userInfo.getId(), userInfo.getGroups());
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), List.of(entityIdAsLong));
	}

	@Test
	public void testGetRestrictionInformationBatchWithLockForTeam() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(teamIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus()
							.withRequirementId(123L)
							.withIsUnmet(true)
							.withRequirementType(AccessRequirementType.LOCK)));
		
		when(mockRestrictionStatusDao.getNonEntityStatus(any(), any(), any())).thenReturn(List.of(touStatus));
		long teamId = Long.parseLong(TEST_PRINCIPAL_ID);
		RestrictionInformationBatchRequest request = new RestrictionInformationBatchRequest();
		request.setObjectIds(List.of(TEST_PRINCIPAL_ID));
		request.setRestrictableObjectType(RestrictableObjectType.TEAM);
		
		RestrictionInformationBatchResponse expected = new RestrictionInformationBatchResponse().setRestrictionInformation(List.of(
			new RestrictionInformationResponse()
				.setHasUnmetAccessRequirement(true)
				.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT)
				.setUserEntityPermissions(null)
				.setObjectId(teamIdAsLong)
				.setRestrictionDetails(List.of(new RestrictionFulfillment().setAccessRequirementId(123L).setIsApproved(false).setIsExempt(false).setIsMet(false)))
		));
		
		RestrictionInformationBatchResponse info = arm.getTeamRestrictionInformationBatchResponse(userInfo, request);
		
		assertEquals(expected, info);

		verify(mockRestrictionStatusDao).getNonEntityStatus(Arrays.asList(teamId), RestrictableObjectType.TEAM, userInfo.getId());
	}

	@Test
	public void testGetRestrictionInformationBatchWithACTForEntity() {
		when(mockEntityAuthorizationManager.getUserPermissionsForEntity(eq(userInfo), eq(TEST_ENTITY_ID), any())).thenReturn(mockUserEntityPermissions);
		when(mockUserEntityPermissions.getIsDataContributor()).thenReturn(false);
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(entityIdAsLong)
				.withUserId(userInfo.getId())
						.withRestrictionStatus(List.of(
								new UsersRequirementStatus()
									.withRequirementId(123L)
									.withIsUnmet(true)
									.withRequirementType(AccessRequirementType.MANAGED_ATC)));
		mapIdToAccess.put(entityIdAsLong, touStatus);
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(userEntityPermissionsState);
		when(mockRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		RestrictionInformationBatchRequest request = new RestrictionInformationBatchRequest();
		request.setObjectIds(List.of(TEST_ENTITY_ID));
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		
		RestrictionInformationBatchResponse expected = new RestrictionInformationBatchResponse().setRestrictionInformation(List.of(
			new RestrictionInformationResponse()
				.setHasUnmetAccessRequirement(true)
				.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT)
				.setUserEntityPermissions(mockUserEntityPermissions)
				.setObjectId(entityIdAsLong)
				.setRestrictionDetails(List.of(new RestrictionFulfillment().setAccessRequirementId(123L).setIsApproved(false).setIsExempt(false).setIsMet(false)))
		));
		
		RestrictionInformationBatchResponse info = arm.getEntityRestrictionInformationBatchResponse(userInfo, request);
		
		assertEquals(expected, info);
		
		verify(mockRestrictionStatusDao).getEntityStatusAsMap(Arrays.asList(entityIdAsLong), userInfo.getId(), userInfo.getGroups());
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), List.of(entityIdAsLong));
	}

	@Test
	public void testGetRestrictionInformationBatchWithACTForTeam() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(teamIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus()
							.withRequirementId(123L)
							.withIsUnmet(true)
							.withRequirementType(AccessRequirementType.MANAGED_ATC)));
		
		when(mockRestrictionStatusDao.getNonEntityStatus(any(), any(), any())).thenReturn(List.of(touStatus));
		long teamId = Long.parseLong(TEST_PRINCIPAL_ID);
		RestrictionInformationBatchRequest request = new RestrictionInformationBatchRequest();
		request.setObjectIds(List.of(TEST_PRINCIPAL_ID));
		request.setRestrictableObjectType(RestrictableObjectType.TEAM);
		
		RestrictionInformationBatchResponse expected = new RestrictionInformationBatchResponse().setRestrictionInformation(List.of(
			new RestrictionInformationResponse()
				.setHasUnmetAccessRequirement(true)
				.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT)
				.setUserEntityPermissions(null)
				.setObjectId(teamIdAsLong)
				.setRestrictionDetails(List.of(new RestrictionFulfillment().setAccessRequirementId(123L).setIsApproved(false).setIsExempt(false).setIsMet(false)))
		));
		
		RestrictionInformationBatchResponse info = arm.getTeamRestrictionInformationBatchResponse(userInfo, request);
		
		assertEquals(expected, info);
		
		verify(mockRestrictionStatusDao).getNonEntityStatus(Arrays.asList(teamId), RestrictableObjectType.TEAM, userInfo.getId());
	}

	@Test
	public void testGetRestrictionInformationBatchForEntityWithMultipleAR() {
		when(mockEntityAuthorizationManager.getUserPermissionsForEntity(eq(userInfo), eq(TEST_ENTITY_ID), any())).thenReturn(mockUserEntityPermissions);
		when(mockUserEntityPermissions.getIsDataContributor()).thenReturn(false);

		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(entityIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus().withRequirementId(123L).withIsUnmet(true).withRequirementType(AccessRequirementType.MANAGED_ATC),
						new UsersRequirementStatus().withRequirementId(456L).withIsUnmet(true).withRequirementType(AccessRequirementType.TOU)));
		mapIdToAccess.put(entityIdAsLong, touStatus);
		when(mockRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(userEntityPermissionsState);
		RestrictionInformationBatchRequest request = new RestrictionInformationBatchRequest();
		request.setObjectIds(List.of(TEST_ENTITY_ID));
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		
		RestrictionInformationBatchResponse expected = new RestrictionInformationBatchResponse().setRestrictionInformation(List.of(
			new RestrictionInformationResponse()
				.setHasUnmetAccessRequirement(true)
				.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT)
				.setUserEntityPermissions(mockUserEntityPermissions)
				.setObjectId(entityIdAsLong)
				.setRestrictionDetails(List.of(
						new RestrictionFulfillment().setAccessRequirementId(123L).setIsApproved(false).setIsExempt(false).setIsMet(false),
						new RestrictionFulfillment().setAccessRequirementId(456L).setIsApproved(false).setIsExempt(false).setIsMet(false)
				))
		));
		
		RestrictionInformationBatchResponse info = arm.getEntityRestrictionInformationBatchResponse(userInfo, request);
		
		assertEquals(expected, info);
		
		verify(mockRestrictionStatusDao).getEntityStatusAsMap(Arrays.asList(entityIdAsLong), userInfo.getId(), userInfo.getGroups());
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), List.of(entityIdAsLong));
	}

	@Test
	public void testGetRestrictionInformationWithEntityWithMetAndLock() {
		when(mockEntityAuthorizationManager.getUserPermissionsForEntity(eq(userInfo), eq(TEST_ENTITY_ID), any())).thenReturn(mockUserEntityPermissions);
		when(mockUserEntityPermissions.getIsDataContributor()).thenReturn(false);

		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(entityIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus().withRequirementId(123L).withIsUnmet(false).withRequirementType(AccessRequirementType.LOCK)));
		mapIdToAccess.put(entityIdAsLong, touStatus);
		when(mockRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(userEntityPermissionsState);
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		
		RestrictionInformationResponse expected = new RestrictionInformationResponse()
			.setHasUnmetAccessRequirement(false)
			.setUserEntityPermissions(mockUserEntityPermissions)
			.setObjectId(entityIdAsLong)
			.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT)
			.setRestrictionDetails(List.of(new RestrictionFulfillment().setAccessRequirementId(123L).setIsApproved(true).setIsExempt(false).setIsMet(true)));
		
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		
		assertEquals(expected, info);
		
		verify(mockRestrictionStatusDao).getEntityStatusAsMap(Arrays.asList(entityIdAsLong), userInfo.getId(), userInfo.getGroups());
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), List.of(entityIdAsLong));
	}

	@Test
	public void testGetRestrictionInformationBatchForTeamWithMetAndLock() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(teamIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
					new UsersRequirementStatus().withRequirementId(123L).withIsUnmet(false).withRequirementType(AccessRequirementType.LOCK))
				);
		
		long teamId = Long.parseLong(TEST_PRINCIPAL_ID);
		mapIdToAccess.put(teamId, touStatus);
		when(mockRestrictionStatusDao.getNonEntityStatus(any(), any(), any())).thenReturn(List.of(touStatus));
		RestrictionInformationBatchRequest request = new RestrictionInformationBatchRequest();
		request.setObjectIds(List.of(TEST_PRINCIPAL_ID));
		request.setRestrictableObjectType(RestrictableObjectType.TEAM);
		
		RestrictionInformationBatchResponse expected = new RestrictionInformationBatchResponse().setRestrictionInformation(List.of(
			new RestrictionInformationResponse()
				.setHasUnmetAccessRequirement(false)
				.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT)
				.setUserEntityPermissions(null)
				.setObjectId(teamIdAsLong)
				.setRestrictionDetails(List.of(new RestrictionFulfillment().setAccessRequirementId(123L).setIsApproved(true).setIsExempt(false).setIsMet(true)))
		));
		
		RestrictionInformationBatchResponse info = arm.getTeamRestrictionInformationBatchResponse(userInfo, request);

		assertEquals(expected, info);
		
		verify(mockRestrictionStatusDao).getNonEntityStatus(Arrays.asList(teamId), RestrictableObjectType.TEAM, userInfo.getId());
	}


	@Test
	public void testGetRestrictionInformationBatchForTeam() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(teamIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus().withRequirementId(123L).withIsUnmet(true).withRequirementType(AccessRequirementType.TOU)));
		long teamId = Long.parseLong(TEST_PRINCIPAL_ID);
		mapIdToAccess.put(teamId, touStatus);
		when(mockRestrictionStatusDao.getNonEntityStatus(any(), any(), any())).thenReturn(List.of(touStatus));
		RestrictionInformationBatchRequest request = new RestrictionInformationBatchRequest();
		request.setObjectIds(List.of(TEST_PRINCIPAL_ID));
		request.setRestrictableObjectType(RestrictableObjectType.TEAM);
		
		RestrictionInformationBatchResponse expected = new RestrictionInformationBatchResponse().setRestrictionInformation(List.of(
			new RestrictionInformationResponse()
				.setHasUnmetAccessRequirement(true)
				.setRestrictionLevel(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE)
				.setUserEntityPermissions(null)
				.setObjectId(teamIdAsLong)
				.setRestrictionDetails(List.of(new RestrictionFulfillment().setAccessRequirementId(123L).setIsApproved(false).setIsExempt(false).setIsMet(false)))
		));
		
		RestrictionInformationBatchResponse info = arm.getTeamRestrictionInformationBatchResponse(userInfo, request);
		
		assertEquals(expected, info);
		
		verify(mockRestrictionStatusDao).getNonEntityStatus(Arrays.asList(teamId), RestrictableObjectType.TEAM, userInfo.getId());
	}

	@Test
	public void testGetRestrictionInformationBatchForTeamWithMultipleAR() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(teamIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus().withRequirementId(123L).withIsUnmet(true).withRequirementType(AccessRequirementType.TOU),
						new UsersRequirementStatus().withRequirementId(456L).withIsUnmet(true).withRequirementType(AccessRequirementType.LOCK)
						));
		long teamId = Long.parseLong(TEST_PRINCIPAL_ID);
		when(mockRestrictionStatusDao.getNonEntityStatus(any(), any(), any())).thenReturn(List.of(touStatus));
		RestrictionInformationBatchRequest request = new RestrictionInformationBatchRequest();
		request.setObjectIds(List.of(TEST_PRINCIPAL_ID));
		request.setRestrictableObjectType(RestrictableObjectType.TEAM);
		
		RestrictionInformationBatchResponse expected = new RestrictionInformationBatchResponse().setRestrictionInformation(List.of(
			new RestrictionInformationResponse()
				.setHasUnmetAccessRequirement(true)
				.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT)
				.setUserEntityPermissions(null)
				.setObjectId(teamIdAsLong)
				.setRestrictionDetails(List.of(
					new RestrictionFulfillment().setAccessRequirementId(123L).setIsApproved(false).setIsExempt(false).setIsMet(false),
					new RestrictionFulfillment().setAccessRequirementId(456L).setIsApproved(false).setIsExempt(false).setIsMet(false)
				))
		));
			
		RestrictionInformationBatchResponse info = arm.getTeamRestrictionInformationBatchResponse(userInfo, request);
		
		assertEquals(expected, info);		
		
		verify(mockRestrictionStatusDao).getNonEntityStatus(Arrays.asList(teamId), RestrictableObjectType.TEAM, userInfo.getId());
	}

	@Test
	public void testGetRestrictionInformationWithTeamAndNoRestrictions() {
		long teamId = Long.parseLong(TEST_PRINCIPAL_ID);
		when(mockRestrictionStatusDao.getNonEntityStatus(any(), any(), any())).thenReturn(List.of(new UsersRestrictionStatus().withSubjectId(teamIdAsLong).withUserId(userInfo.getId())));
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_PRINCIPAL_ID);
		request.setRestrictableObjectType(RestrictableObjectType.TEAM);
		
		RestrictionInformationResponse expected = new RestrictionInformationResponse()
			.setHasUnmetAccessRequirement(false)
			.setUserEntityPermissions(null)
			.setObjectId(teamIdAsLong)
			.setRestrictionLevel(RestrictionLevel.OPEN)
			.setRestrictionDetails(Collections.emptyList());
				
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		
		assertEquals(expected, info);

		verify(mockRestrictionStatusDao).getNonEntityStatus(Arrays.asList(teamId), RestrictableObjectType.TEAM, userInfo.getId());
	}
	
	@Test
	public void testGetRestrictionInformationWithUnexpectedSize() {
		
		when(mockRestrictionStatusDao.getNonEntityStatus(any(), any(), any())).thenReturn(Collections.emptyList());
		
		String message = assertThrows(IllegalStateException.class, () -> {			
			arm.getRestrictionInformation(userInfo, new RestrictionInformationRequest().setObjectId("123").setRestrictableObjectType(RestrictableObjectType.TEAM));
		}).getMessage();
		
		assertEquals("Could not fetch restriction information for object 123 of type TEAM", message);
	}
	
	@Test
	public void testGetRestrictionInformationBatchWithNoRequest() {
		
		RestrictionInformationBatchRequest request = null;
		
		String result = assertThrows(IllegalArgumentException.class, () ->  {			
			arm.getRestrictionInformationBatch(userInfo, request);
		}).getMessage();
		
		assertEquals("The request is required.", result);
		
	}
	
	@Test
	public void testGetRestrictionInformationBatchWithNoUser() {
		
		RestrictionInformationBatchRequest request = new RestrictionInformationBatchRequest()
			.setObjectIds(List.of("syn123"))
			.setRestrictableObjectType(RestrictableObjectType.ENTITY);;
		
		String result = assertThrows(IllegalArgumentException.class, () ->  {			
			arm.getRestrictionInformationBatch(null, request);
		}).getMessage();
		
		assertEquals("The userInfo is required.", result);
		
	}
	
	@Test
	public void testGetRestrictionInformationBatchWithNoRestrictableObjectType() {
		
		RestrictionInformationBatchRequest request = new RestrictionInformationBatchRequest()
			.setObjectIds(List.of("syn123"))
			.setRestrictableObjectType(null);
		
		String result = assertThrows(IllegalArgumentException.class, () ->  {			
			arm.getRestrictionInformationBatch(userInfo, request);
		}).getMessage();
		
		assertEquals("The restrictableObjectType is required.", result);
		
	}
	
	@Test
	public void testGetRestrictionInformationBatchWithEmptyObjectIds() {
		
		RestrictionInformationBatchRequest request = new RestrictionInformationBatchRequest()
			.setObjectIds(Collections.emptyList())
			.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		
		String result = assertThrows(IllegalArgumentException.class, () ->  {			
			arm.getRestrictionInformationBatch(userInfo, request);
		}).getMessage();
		
		assertEquals("The objectIds is required and must not be empty.", result);
		
	}
	
	@Test
	public void testGetRestrictionInformationBatchWithTooManyObjectIds() {
		
		
		RestrictionInformationBatchRequest request = new RestrictionInformationBatchRequest()
			.setObjectIds(IntStream.range(0, RestrictionInformationManager.MAX_BATCH_SIZE + 1)
				      .boxed()
				      .map( i -> i.toString())
				      .collect(Collectors.toList()))			
			.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		
		String result = assertThrows(IllegalArgumentException.class, () ->  {			
			arm.getRestrictionInformationBatch(userInfo, request);
		}).getMessage();
		
		assertEquals("The maximum number of allowed object ids is 50.", result);
		
	}
	
	@Test
	public void testGetRestrictionInformationBatchWithEntityList() {
		UserEntityPermissions mockPermissionsFor123 = mock(UserEntityPermissions.class);
		UserEntityPermissions mockPermissionsFor456 = mock(UserEntityPermissions.class);
		UserEntityPermissions mockPermissionsFor789 = mock(UserEntityPermissions.class);

		when(mockEntityAuthorizationManager.getUserPermissionsForEntity(eq(userInfo), eq("syn123"), any())).thenReturn(mockPermissionsFor123);
		when(mockEntityAuthorizationManager.getUserPermissionsForEntity(eq(userInfo), eq("syn456"), any())).thenReturn(mockPermissionsFor456);
		when(mockEntityAuthorizationManager.getUserPermissionsForEntity(eq(userInfo), eq("syn789"), any())).thenReturn(mockPermissionsFor789);
		when(mockPermissionsFor123.getIsDataContributor()).thenReturn(false);
		when(mockPermissionsFor456.getIsDataContributor()).thenReturn(false);
		when(mockPermissionsFor789.getIsDataContributor()).thenReturn(true);
		
		mapIdToAccess.put(123L, new UsersRestrictionStatus()
				.withSubjectId(123L)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
					new UsersRequirementStatus().withRequirementId(1234L).withIsUnmet(false).withRequirementType(AccessRequirementType.LOCK))
				));
		
		mapIdToAccess.put(456L, new UsersRestrictionStatus()
				.withSubjectId(456L)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
					new UsersRequirementStatus().withRequirementId(4567L).withIsUnmet(true).withRequirementType(AccessRequirementType.MANAGED_ATC))
				));
		
		// Exemption Eligible and not approved
		mapIdToAccess.put(789L, new UsersRestrictionStatus()
			.withSubjectId(789L)
			.withUserId(userInfo.getId())
			.withRestrictionStatus(List.of(
				new UsersRequirementStatus().withRequirementId(7890L).withIsUnmet(true).withRequirementType(AccessRequirementType.MANAGED_ATC).withIsExemptionEligible(true))
			));
		
		// No ARs, no download access
		mapIdToAccess.put(321L, new UsersRestrictionStatus()
				.withSubjectId(321L)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(Collections.emptyList()));
		
		// Data contributor
		userEntityPermissionsState.put(123L,
				new UserEntityPermissionsState(123L)
					.withHasUpdate(true)
					.withHasRead(true)
					.withHasDelete(true)
					.withHasDownload(true));
		
		userEntityPermissionsState.put(456L,
				new UserEntityPermissionsState(456L)
					.withHasUpdate(false)
					.withHasRead(true)
					.withHasDelete(true)
					.withHasDownload(true));
		
		// Data contributor
		userEntityPermissionsState.put(789L,
				new UserEntityPermissionsState(789L)
					.withHasUpdate(true)
					.withHasRead(true)
					.withHasDelete(true)
					.withHasDownload(true));

		when(mockRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(userEntityPermissionsState);
		
		RestrictionInformationBatchRequest request = new RestrictionInformationBatchRequest()
			.setObjectIds(List.of("syn123", "syn456", "789")).setRestrictableObjectType(RestrictableObjectType.ENTITY);
		
		RestrictionInformationBatchResponse expected = new RestrictionInformationBatchResponse().setRestrictionInformation(List.of(
			new RestrictionInformationResponse()
				.setHasUnmetAccessRequirement(false)
				.setUserEntityPermissions(mockPermissionsFor123)
				.setObjectId(123L)
				.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT)
				.setRestrictionDetails(List.of(new RestrictionFulfillment().setAccessRequirementId(1234L).setIsApproved(true).setIsExempt(false).setIsMet(true))),
			new RestrictionInformationResponse()
				.setHasUnmetAccessRequirement(true)
				.setUserEntityPermissions(mockPermissionsFor456)
				.setObjectId(456L)
				.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT)
				.setRestrictionDetails(List.of(new RestrictionFulfillment().setAccessRequirementId(4567L).setIsApproved(false).setIsExempt(false).setIsMet(false))),
			new RestrictionInformationResponse()
				.setHasUnmetAccessRequirement(false)
				.setUserEntityPermissions(mockPermissionsFor789)
				.setObjectId(789L)
				.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT)
				.setRestrictionDetails(List.of(new RestrictionFulfillment().setAccessRequirementId(7890L).setIsApproved(false).setIsExempt(true).setIsMet(true)))
		));
		
		RestrictionInformationBatchResponse response = arm.getRestrictionInformationBatch(userInfo, request);
		
		assertEquals(expected, response);
		
		verify(mockRestrictionStatusDao).getEntityStatusAsMap(List.of(123L, 456L, 789L), userInfo.getId(), userInfo.getGroups());
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), List.of(123L, 456L, 789L));
		
	}
	
	@Test
	public void testGetRestrictionInformationBatchWithTeamList() {
				
		when(mockRestrictionStatusDao.getNonEntityStatus(any(), any(), any())).thenReturn(List.of(
			new UsersRestrictionStatus()
				.withSubjectId(123L)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
					new UsersRequirementStatus()
						.withRequirementId(1234L)
						.withIsUnmet(false)
						.withRequirementType(AccessRequirementType.MANAGED_ATC))),
			new UsersRestrictionStatus()
				.withSubjectId(456L)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
					new UsersRequirementStatus()
						.withRequirementId(4567L)
						.withIsUnmet(true)
						.withRequirementType(AccessRequirementType.MANAGED_ATC)))
		));
		
		
		RestrictionInformationBatchRequest request = new RestrictionInformationBatchRequest()
			.setObjectIds(List.of("123", "456")).setRestrictableObjectType(RestrictableObjectType.TEAM);
		
		RestrictionInformationBatchResponse expected = new RestrictionInformationBatchResponse().setRestrictionInformation(List.of(
			new RestrictionInformationResponse()
				.setHasUnmetAccessRequirement(false)
				.setUserEntityPermissions(null)
				.setObjectId(123L)
				.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT)
				.setRestrictionDetails(List.of(new RestrictionFulfillment().setAccessRequirementId(1234L).setIsApproved(true).setIsExempt(false).setIsMet(true))),
			new RestrictionInformationResponse()
				.setHasUnmetAccessRequirement(true)
				.setUserEntityPermissions(null)
				.setObjectId(456L)
				.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT)
				.setRestrictionDetails(List.of(new RestrictionFulfillment().setAccessRequirementId(4567L).setIsApproved(false).setIsExempt(false).setIsMet(false)))
		)); 
		
		RestrictionInformationBatchResponse response = arm.getRestrictionInformationBatch(userInfo, request);
		
		assertEquals(expected, response);
		
		verify(mockRestrictionStatusDao).getNonEntityStatus(List.of(123L, 456L), RestrictableObjectType.TEAM, userInfo.getId());
		
	}
	
	@Test
	public void testBuildRestrictionInformationResponseWithNoRestrictions() {
		UsersRestrictionStatus restrictionStatus = new UsersRestrictionStatus()
			.withSubjectId(123L)
			.withRestrictionStatus(Collections.emptyList());

		RestrictionInformationResponse expected = new RestrictionInformationResponse()
			.setHasUnmetAccessRequirement(false)
			.setUserEntityPermissions(mockUserEntityPermissions)
			.setObjectId(123L)
			.setRestrictionDetails(Collections.emptyList())
			.setRestrictionLevel(RestrictionLevel.OPEN);
		
		RestrictionInformationResponse result = RestrictionInformationManagerImpl.buildRestrictionInformationResponse(restrictionStatus, mockUserEntityPermissions, mockUnmetArIdsSupplier);
		
		assertEquals(expected, result);

		verifyZeroInteractions(mockUnmetArIdsSupplier);
	}
	
	@Test
	public void testBuildRestrictionInformationResponseWithRestrictions() {
		UsersRestrictionStatus restrictionStatus = new UsersRestrictionStatus()
			.withSubjectId(123L)
			.withRestrictionStatus(List.of(
				new UsersRequirementStatus()
					.withRequirementId(1L)
					.withRequirementType(AccessRequirementType.MANAGED_ATC)
					.withIsExemptionEligible(false)
					.withIsUnmet(true),
				new UsersRequirementStatus()
					.withRequirementId(2L)
					.withRequirementType(AccessRequirementType.MANAGED_ATC)
					.withIsExemptionEligible(false)
					.withIsUnmet(false)
			));
		
		when(mockUnmetArIdsSupplier.get()).thenReturn(List.of(1L));
		
		RestrictionInformationResponse expected = new RestrictionInformationResponse()
			.setHasUnmetAccessRequirement(true)
			.setUserEntityPermissions(mockUserEntityPermissions)
			.setObjectId(123L)
			.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT)
			.setRestrictionDetails(List.of(
				new RestrictionFulfillment()
					.setAccessRequirementId(1L)
					.setIsApproved(false)
					.setIsExempt(false)
					.setIsMet(false),
				new RestrictionFulfillment()
					.setAccessRequirementId(2L)
					.setIsApproved(true)
					.setIsExempt(false)
					.setIsMet(true)
			));
		
		RestrictionInformationResponse result = RestrictionInformationManagerImpl.buildRestrictionInformationResponse(restrictionStatus, mockUserEntityPermissions, mockUnmetArIdsSupplier);
		
		assertEquals(expected, result);
		
		verify(mockUnmetArIdsSupplier).get();
	}
	
	@Test
	public void testBuildRestrictionInformationResponseWithRestrictionsAndExemptionsAndNotDataContributor() {
		when(mockUserEntityPermissions.getIsDataContributor()).thenReturn(false);

		UsersRestrictionStatus restrictionStatus = new UsersRestrictionStatus()
			.withSubjectId(123L)
			.withRestrictionStatus(List.of(
				new UsersRequirementStatus()
					.withRequirementId(1L)
					.withRequirementType(AccessRequirementType.MANAGED_ATC)
					.withIsExemptionEligible(false)
					.withIsUnmet(true),
				new UsersRequirementStatus()
					.withRequirementId(2L)
					.withRequirementType(AccessRequirementType.MANAGED_ATC)
					.withIsExemptionEligible(true)
					.withIsUnmet(true)
			));
		
		when(mockUnmetArIdsSupplier.get()).thenReturn(List.of(1L, 2L));
		
		RestrictionInformationResponse expected = new RestrictionInformationResponse()
			.setHasUnmetAccessRequirement(true)
			.setUserEntityPermissions(mockUserEntityPermissions)
			.setObjectId(123L)
			.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT)
			.setRestrictionDetails(List.of(
				new RestrictionFulfillment()
					.setAccessRequirementId(1L)
					.setIsApproved(false)
					.setIsExempt(false)
					.setIsMet(false),
				new RestrictionFulfillment()
					.setAccessRequirementId(2L)
					.setIsApproved(false)
					.setIsExempt(false)
					.setIsMet(false)
			));
		
		RestrictionInformationResponse result = RestrictionInformationManagerImpl.buildRestrictionInformationResponse(restrictionStatus, mockUserEntityPermissions, mockUnmetArIdsSupplier);
		
		assertEquals(expected, result);
		
		verify(mockUnmetArIdsSupplier).get();
	}
	
	@Test
	public void testBuildRestrictionInformationResponseWithRestrictionsAndExemptionsAndDataContributor() {
		when(mockUserEntityPermissions.getIsDataContributor()).thenReturn(true);

		UsersRestrictionStatus restrictionStatus = new UsersRestrictionStatus()
			.withSubjectId(123L)
			.withRestrictionStatus(List.of(
				new UsersRequirementStatus()
					.withRequirementId(1L)
					.withRequirementType(AccessRequirementType.MANAGED_ATC)
					.withIsExemptionEligible(false)
					.withIsUnmet(true),
				new UsersRequirementStatus()
					.withRequirementId(2L)
					.withRequirementType(AccessRequirementType.MANAGED_ATC)
					.withIsExemptionEligible(true)
					.withIsUnmet(true)
			));
		
		when(mockUnmetArIdsSupplier.get()).thenReturn(List.of(1L));
		
		RestrictionInformationResponse expected = new RestrictionInformationResponse()
			.setHasUnmetAccessRequirement(true)
			.setUserEntityPermissions(mockUserEntityPermissions)
			.setObjectId(123L)
			.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT)
			.setRestrictionDetails(List.of(
				new RestrictionFulfillment()
					.setAccessRequirementId(1L)
					.setIsApproved(false)
					.setIsExempt(false)
					.setIsMet(false),
				new RestrictionFulfillment()
					.setAccessRequirementId(2L)
					.setIsApproved(false)
					.setIsExempt(true)
					.setIsMet(true)
			));
		
		RestrictionInformationResponse result = RestrictionInformationManagerImpl.buildRestrictionInformationResponse(restrictionStatus, mockUserEntityPermissions, mockUnmetArIdsSupplier);
		
		assertEquals(expected, result);
		
		verify(mockUnmetArIdsSupplier).get();
	}
}
