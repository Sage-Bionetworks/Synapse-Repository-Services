package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

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
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.ar.UsersRequirementStatus;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

@ExtendWith(MockitoExtension.class)
public class RestrictionInformationManagerImplUnitTest {

	private static final String TEST_PRINCIPAL_ID = "1010101";
	private static final String TEST_ENTITY_ID = "syn98786543";

	@Mock
	private AccessRestrictionStatusDao mockRestrictionStatusDao;

	@InjectMocks
	private RestrictionInformationManagerImpl arm;

	private UserInfo userInfo;

	private Node testEntity;
	private Long entityIdAsLong;
	private Long teamIdAsLong;

	@BeforeEach
	public void setUp() throws Exception {
		userInfo = new UserInfo(false, TEST_PRINCIPAL_ID);
		testEntity = new Node();
		testEntity.setCreatedByPrincipalId(999L); // different from TEST_PRINCIPAL_ID
		testEntity.setNodeType(EntityType.file);
		entityIdAsLong = KeyFactory.stringToKey(TEST_ENTITY_ID);
		teamIdAsLong = KeyFactory.stringToKey(TEST_PRINCIPAL_ID);
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
	public void testGetRestrictionInformationWithZeroAR() {
		when(mockRestrictionStatusDao.getSubjectStatus(any(), any(), any())).thenReturn(Collections.emptyList());
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		AccessRequirementStats stats = new AccessRequirementStats();
		stats.setRequirementIdSet(new HashSet<String>());
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.OPEN, info.getRestrictionLevel());
		assertFalse(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getSubjectStatus(Arrays.asList(entityIdAsLong), RestrictableObjectType.ENTITY,
				userInfo.getId());
	}

	@Test
	public void testGetRestrictionInformationWithToU() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus(entityIdAsLong, userInfo.getId());
		touStatus.setHasUnmet(true);
		touStatus.addRestrictionStatus(
				new UsersRequirementStatus().withIsUnmet(true).withRequirementType(AccessRequirementType.SELF_SIGNED));
		when(mockRestrictionStatusDao.getSubjectStatus(any(), any(), any())).thenReturn(Arrays.asList(touStatus));
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, info.getRestrictionLevel());
		assertTrue(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getSubjectStatus(Arrays.asList(entityIdAsLong), RestrictableObjectType.ENTITY,
				userInfo.getId());
	}
	
	@Test
	public void testGetRestrictionInformationWithToUMet() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus(entityIdAsLong, userInfo.getId());
		touStatus.setHasUnmet(false);
		touStatus.addRestrictionStatus(
				new UsersRequirementStatus().withIsUnmet(true).withRequirementType(AccessRequirementType.SELF_SIGNED));
		when(mockRestrictionStatusDao.getSubjectStatus(any(), any(), any())).thenReturn(Arrays.asList(touStatus));
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, info.getRestrictionLevel());
		assertFalse(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getSubjectStatus(Arrays.asList(entityIdAsLong), RestrictableObjectType.ENTITY,
				userInfo.getId());
	}

	@Test
	public void testGetRestrictionInformationWithLock() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus(entityIdAsLong, userInfo.getId());
		touStatus.setHasUnmet(true);
		touStatus.addRestrictionStatus(
				new UsersRequirementStatus().withIsUnmet(true).withRequirementType(AccessRequirementType.LOCK));
		when(mockRestrictionStatusDao.getSubjectStatus(any(), any(), any())).thenReturn(Arrays.asList(touStatus));
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, info.getRestrictionLevel());
		assertTrue(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getSubjectStatus(Arrays.asList(entityIdAsLong), RestrictableObjectType.ENTITY,
				userInfo.getId());
	}

	@Test
	public void testGetRestrictionInformationWithACT() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus(entityIdAsLong, userInfo.getId());
		touStatus.setHasUnmet(true);
		touStatus.addRestrictionStatus(
				new UsersRequirementStatus().withIsUnmet(true).withRequirementType(AccessRequirementType.MANAGED_ATC));
		when(mockRestrictionStatusDao.getSubjectStatus(any(), any(), any())).thenReturn(Arrays.asList(touStatus));
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, info.getRestrictionLevel());
		assertTrue(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getSubjectStatus(Arrays.asList(entityIdAsLong), RestrictableObjectType.ENTITY,
				userInfo.getId());
	}

	@Test
	public void testGetRestrictionInformationWithBoth() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus(entityIdAsLong, userInfo.getId());
		touStatus.setHasUnmet(true);
		touStatus.addRestrictionStatus(
				new UsersRequirementStatus().withIsUnmet(true).withRequirementType(AccessRequirementType.MANAGED_ATC));
		touStatus.addRestrictionStatus(
				new UsersRequirementStatus().withIsUnmet(true).withRequirementType(AccessRequirementType.TOU));
		when(mockRestrictionStatusDao.getSubjectStatus(any(), any(), any())).thenReturn(Arrays.asList(touStatus));
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, info.getRestrictionLevel());
		assertTrue(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getSubjectStatus(Arrays.asList(entityIdAsLong), RestrictableObjectType.ENTITY,
				userInfo.getId());
	}


	@Test
	public void testGetRestrictionInformationForTeam() {
		UsersRestrictionStatus touStatus = new UsersRestrictionStatus(entityIdAsLong, userInfo.getId());
		touStatus.setHasUnmet(true);
		touStatus.addRestrictionStatus(
				new UsersRequirementStatus().withIsUnmet(true).withRequirementType(AccessRequirementType.TOU));
		when(mockRestrictionStatusDao.getSubjectStatus(any(), any(), any())).thenReturn(Arrays.asList(touStatus));
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_PRINCIPAL_ID);
		request.setRestrictableObjectType(RestrictableObjectType.TEAM);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, info.getRestrictionLevel());
		assertTrue(info.getHasUnmetAccessRequirement());
		verify(mockRestrictionStatusDao).getSubjectStatus(Arrays.asList(teamIdAsLong), RestrictableObjectType.TEAM,
				userInfo.getId());
	}
}
