package org.sagebionetworks.repo.manager.dataaccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.AccessRequirementStats;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.AccessRequirementType;
import org.sagebionetworks.repo.model.ar.AccessRestrictionStatusDao;
import org.sagebionetworks.repo.model.ar.UsersRequirementStatus;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;
import org.sagebionetworks.repo.model.dbo.entity.UsersEntityPermissionsDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RestrictionInformationManagerImplUnitTest {

	private static final String TEST_PRINCIPAL_ID = "1010101";
	private static final String TEST_ENTITY_ID = "syn98786543";

	@Mock
	private AccessRestrictionStatusDao mockRestrictionStatusDao;
	@Mock
	private UsersEntityPermissionsDao mockUsersEntityPermissionsDao;

	@InjectMocks
	private RestrictionInformationManagerImpl arm;

	private UserInfo userInfo;

	private Node testEntity;
	private Long entityIdAsLong;
	private Long teamIdAsLong;
	private Map<Long, UsersRestrictionStatus> mapIdToAccess = new LinkedHashMap<Long, UsersRestrictionStatus>();
	private Map<Long, UserEntityPermissionsState> userEntityPermissionsState = new LinkedHashMap<Long,UserEntityPermissionsState>();

	@BeforeEach
	public void setUp() throws Exception {
		userInfo = new UserInfo(false, TEST_PRINCIPAL_ID);
		testEntity = new Node();
		testEntity.setCreatedByPrincipalId(999L); // different from TEST_PRINCIPAL_ID
		testEntity.setNodeType(EntityType.file);
		entityIdAsLong = KeyFactory.stringToKey(TEST_ENTITY_ID);
		teamIdAsLong = KeyFactory.stringToKey(TEST_PRINCIPAL_ID);
		userEntityPermissionsState.put(entityIdAsLong,
				new UserEntityPermissionsState(entityIdAsLong)
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
	public void testGetRestrictionInformationForEvaluation() {
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
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(userEntityPermissionsState);
		when(mockRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		AccessRequirementStats stats = new AccessRequirementStats();
		stats.setRequirementIdSet(new HashSet<String>());
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.OPEN, info.getRestrictionLevel());
		assertFalse(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getEntityStatusAsMap(Arrays.asList(entityIdAsLong), userInfo.getId(), userInfo.getGroups());
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), List.of(entityIdAsLong));
	}

	@Test
	public void testGetRestrictionInformationWithZeroARForTeam() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(entityIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(Collections.emptyList());
		when(mockRestrictionStatusDao.getNonEntityStatus(any(), any(), any())).thenReturn(List.of(touStatus));
		long teamId = Long.parseLong(TEST_PRINCIPAL_ID);
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_PRINCIPAL_ID);
		request.setRestrictableObjectType(RestrictableObjectType.TEAM);
		AccessRequirementStats stats = new AccessRequirementStats();
		stats.setRequirementIdSet(new HashSet<String>());
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.OPEN, info.getRestrictionLevel());
		assertFalse(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getNonEntityStatus(Arrays.asList(teamId), RestrictableObjectType.TEAM, userInfo.getId());
	}

	@Test
	public void testGetRestrictionInformationWithToUForEntity() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(entityIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus()
								.withIsUnmet(true)
								.withRequirementType(AccessRequirementType.SELF_SIGNED)));
		mapIdToAccess.put(entityIdAsLong, touStatus);
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(userEntityPermissionsState);
		when(mockRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, info.getRestrictionLevel());
		assertTrue(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getEntityStatusAsMap(Arrays.asList(entityIdAsLong), userInfo.getId(), userInfo.getGroups());
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), List.of(entityIdAsLong));
	}

	@Test
	public void testGetRestrictionInformationWithToUForTeam() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(entityIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus()
								.withIsUnmet(true)
								.withRequirementType(AccessRequirementType.SELF_SIGNED)));
		when(mockRestrictionStatusDao.getNonEntityStatus(any(), any(), any())).thenReturn(List.of(touStatus));
		long teamId = Long.parseLong(TEST_PRINCIPAL_ID);
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_PRINCIPAL_ID);
		request.setRestrictableObjectType(RestrictableObjectType.TEAM);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, info.getRestrictionLevel());
		assertTrue(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getNonEntityStatus(Arrays.asList(teamId), RestrictableObjectType.TEAM, userInfo.getId());
	}
	
	@Test
	public void testGetRestrictionInformationWithToUMetForEntity() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(entityIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus()
								.withIsUnmet(false)
								.withRequirementType(AccessRequirementType.SELF_SIGNED)));
		mapIdToAccess.put(entityIdAsLong, touStatus);
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(userEntityPermissionsState);
		when(mockRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, info.getRestrictionLevel());
		assertFalse(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getEntityStatusAsMap(Arrays.asList(entityIdAsLong), userInfo.getId(), userInfo.getGroups());
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), List.of(entityIdAsLong));
	}

	@Test
	public void testGetRestrictionInformationWithToUMetForTeam() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(entityIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus()
								.withIsUnmet(false)
								.withRequirementType(AccessRequirementType.SELF_SIGNED)));
		when(mockRestrictionStatusDao.getNonEntityStatus(any(), any(), any())).thenReturn(List.of(touStatus));
		long teamId = Long.parseLong(TEST_PRINCIPAL_ID);
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_PRINCIPAL_ID);
		request.setRestrictableObjectType(RestrictableObjectType.TEAM);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, info.getRestrictionLevel());
		assertFalse(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getNonEntityStatus(Arrays.asList(teamId), RestrictableObjectType.TEAM, userInfo.getId());
	}

	@Test
	public void testGetRestrictionInformationWithLockForEntity() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(entityIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus().withIsUnmet(true).withRequirementType(AccessRequirementType.LOCK)));
		mapIdToAccess.put(entityIdAsLong, touStatus);
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(userEntityPermissionsState);
		when(mockRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, info.getRestrictionLevel());
		assertTrue(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getEntityStatusAsMap(Arrays.asList(entityIdAsLong), userInfo.getId(), userInfo.getGroups());
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), List.of(entityIdAsLong));
	}

	@Test
	public void testGetRestrictionInformationWithLockForTeam() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(entityIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus()
								.withIsUnmet(true)
								.withRequirementType(AccessRequirementType.LOCK)));
		when(mockRestrictionStatusDao.getNonEntityStatus(any(), any(), any())).thenReturn(List.of(touStatus));
		long teamId = Long.parseLong(TEST_PRINCIPAL_ID);
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_PRINCIPAL_ID);
		request.setRestrictableObjectType(RestrictableObjectType.TEAM);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, info.getRestrictionLevel());
		assertTrue(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getNonEntityStatus(Arrays.asList(teamId), RestrictableObjectType.TEAM, userInfo.getId());
	}

	@Test
	public void testGetRestrictionInformationWithACTForEntity() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(entityIdAsLong)
				.withUserId(userInfo.getId())
						.withRestrictionStatus(List.of(
								new UsersRequirementStatus()
										.withIsUnmet(true)
										.withRequirementType(AccessRequirementType.MANAGED_ATC)));
		mapIdToAccess.put(entityIdAsLong, touStatus);
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(userEntityPermissionsState);
		when(mockRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, info.getRestrictionLevel());
		assertTrue(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getEntityStatusAsMap(Arrays.asList(entityIdAsLong), userInfo.getId(), userInfo.getGroups());
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), List.of(entityIdAsLong));
	}

	@Test
	public void testGetRestrictionInformationWithACTForTeam() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(entityIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus()
								.withIsUnmet(true)
								.withRequirementType(AccessRequirementType.MANAGED_ATC)));
		when(mockRestrictionStatusDao.getNonEntityStatus(any(), any(), any())).thenReturn(List.of(touStatus));
		long teamId = Long.parseLong(TEST_PRINCIPAL_ID);
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_PRINCIPAL_ID);
		request.setRestrictableObjectType(RestrictableObjectType.TEAM);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, info.getRestrictionLevel());
		assertTrue(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getNonEntityStatus(Arrays.asList(teamId), RestrictableObjectType.TEAM, userInfo.getId());
	}

	@Test
	public void testGetRestrictionInformationForEntityWithMultipleAR() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(entityIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus().withIsUnmet(true).withRequirementType(AccessRequirementType.MANAGED_ATC),
						new UsersRequirementStatus().withIsUnmet(true).withRequirementType(AccessRequirementType.TOU)));
		mapIdToAccess.put(entityIdAsLong, touStatus);
		when(mockRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(userEntityPermissionsState);
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, info.getRestrictionLevel());
		assertTrue(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getEntityStatusAsMap(Arrays.asList(entityIdAsLong), userInfo.getId(), userInfo.getGroups());
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), List.of(entityIdAsLong));
	}


	@Test
	public void testGetRestrictionInformationForTeam() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(entityIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus().withIsUnmet(true).withRequirementType(AccessRequirementType.TOU)));
		long teamId = Long.parseLong(TEST_PRINCIPAL_ID);
		mapIdToAccess.put(teamId, touStatus);
		when(mockRestrictionStatusDao.getNonEntityStatus(any(), any(), any())).thenReturn(List.of(touStatus));
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_PRINCIPAL_ID);
		request.setRestrictableObjectType(RestrictableObjectType.TEAM);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, info.getRestrictionLevel());
		assertTrue(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getNonEntityStatus(Arrays.asList(teamId), RestrictableObjectType.TEAM, userInfo.getId());
	}

	@Test
	public void testGetRestrictionInformationForTeamWithMultipleAR() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(entityIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus().withIsUnmet(true).withRequirementType(AccessRequirementType.TOU),
						new UsersRequirementStatus().withIsUnmet(true).withRequirementType(AccessRequirementType.LOCK)
						));
		long teamId = Long.parseLong(TEST_PRINCIPAL_ID);
		when(mockRestrictionStatusDao.getNonEntityStatus(any(), any(), any())).thenReturn(List.of(touStatus));
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_PRINCIPAL_ID);
		request.setRestrictableObjectType(RestrictableObjectType.TEAM);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, info.getRestrictionLevel());
		assertTrue(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getNonEntityStatus(Arrays.asList(teamId), RestrictableObjectType.TEAM, userInfo.getId());
	}

	@Test
	public void testGetNoRestrictionInformationForTeam() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(entityIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(Collections.emptyList());
		long teamId = Long.parseLong(TEST_PRINCIPAL_ID);
		when(mockRestrictionStatusDao.getNonEntityStatus(any(), any(), any())).thenReturn(List.of(touStatus));
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_PRINCIPAL_ID);
		request.setRestrictableObjectType(RestrictableObjectType.TEAM);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.OPEN, info.getRestrictionLevel());
		assertFalse(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getNonEntityStatus(Arrays.asList(teamId), RestrictableObjectType.TEAM, userInfo.getId());
	}

	@Test
	public void testGetNoRestrictionInformationForEntity() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus()
				.withSubjectId(entityIdAsLong)
				.withUserId(userInfo.getId())
				.withRestrictionStatus(Collections.emptyList());
		mapIdToAccess.put(entityIdAsLong, touStatus);
		when(mockRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(userEntityPermissionsState);
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.OPEN, info.getRestrictionLevel());
		assertFalse(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getEntityStatusAsMap(Arrays.asList(entityIdAsLong), userInfo.getId(), userInfo.getGroups());
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), List.of(entityIdAsLong));
	}
}
