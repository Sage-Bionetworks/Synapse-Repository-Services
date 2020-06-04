package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AccessRequirementStats;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.util.jrjc.CreatedIssue;
import org.sagebionetworks.repo.util.jrjc.ProjectInfo;

@ExtendWith(MockitoExtension.class)
public class RestrictionInformationManagerImplUnitTest {


	private static final String TEST_PRINCIPAL_ID = "1010101";
	private static final String TEST_ENTITY_ID = "syn98786543";

	@Mock
	private AccessRequirementDAO accessRequirementDAO;
	@Mock
	private AccessApprovalDAO accessApprovalDAO;
	@Mock
	private NodeDAO nodeDao;

	@InjectMocks
	private RestrictionInformationManagerImpl arm;

	private UserInfo userInfo;

	@Mock
	CreatedIssue mockProject;
	@Mock
	ProjectInfo mockProjectInfo;
	
	private Node testEntity;

	@BeforeEach
	public void setUp() throws Exception {
		userInfo = new UserInfo(false, TEST_PRINCIPAL_ID);
		testEntity = new Node();
		testEntity.setCreatedByPrincipalId(999L); // different from TEST_PRINCIPAL_ID
		testEntity.setNodeType(EntityType.file);
	}


	@Test
	public void testGetRestrictionInformationWithNullUserInfo() {
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.getRestrictionInformation(null, request);
		});
	}

	@Test
	public void testGetRestrictionInformationWithNullRequest() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.getRestrictionInformation(userInfo, null);
		});
	}

	@Test
	public void testGetRestrictionInformationWithNullObjectId() {
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.getRestrictionInformation(userInfo, request);
		});
	}

	@Test
	public void testGetRestrictionInformationWithNullObjectType() {
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.getRestrictionInformation(userInfo, request);
		});
	}

	@Test
	public void testGetRestrictionInformationForEvaluation() {
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.EVALUATION);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.getRestrictionInformation(userInfo, request);
		});
	}

	@Test
	public void testGetRestrictionInformationWithZeroAR() {
		when(nodeDao.getNode(TEST_ENTITY_ID)).thenReturn(testEntity);
		when(nodeDao.getEntityPathIds(eq(TEST_ENTITY_ID))).thenReturn(Arrays.asList(KeyFactory.stringToKey(TEST_ENTITY_ID)));
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		AccessRequirementStats stats = new AccessRequirementStats();
		stats.setRequirementIdSet(new HashSet<String>());
		when(accessRequirementDAO.getAccessRequirementStats(Arrays.asList(KeyFactory.stringToKey(TEST_ENTITY_ID)), RestrictableObjectType.ENTITY)).thenReturn(stats );
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.OPEN, info.getRestrictionLevel());
		assertFalse(info.getHasUnmetAccessRequirement());
		verify(nodeDao).getEntityPathIds(TEST_ENTITY_ID);
	}

	@Test
	public void testGetRestrictionInformationWithToU() {
		when(nodeDao.getNode(TEST_ENTITY_ID)).thenReturn(testEntity);
		when(nodeDao.getEntityPathIds(eq(TEST_ENTITY_ID))).thenReturn(Arrays.asList(KeyFactory.stringToKey(TEST_ENTITY_ID)));
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		AccessRequirementStats stats = new AccessRequirementStats();
		Set<String> set = new HashSet<String>();
		set.add("1");
		stats.setRequirementIdSet(set);
		stats.setHasToU(true);
		stats.setHasACT(false);
		stats.setHasLock(false);
		when(accessRequirementDAO.getAccessRequirementStats(Arrays.asList(KeyFactory.stringToKey(TEST_ENTITY_ID)), RestrictableObjectType.ENTITY)).thenReturn(stats );
		when(accessApprovalDAO.hasUnmetAccessRequirement(set, userInfo.getId().toString())).thenReturn(true);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, info.getRestrictionLevel());
		assertTrue(info.getHasUnmetAccessRequirement());
		verify(nodeDao).getEntityPathIds(TEST_ENTITY_ID);
	}

	@Test
	public void testGetRestrictionInformationWithLock() {
		when(nodeDao.getNode(TEST_ENTITY_ID)).thenReturn(testEntity);
		when(nodeDao.getEntityPathIds(eq(TEST_ENTITY_ID))).thenReturn(Arrays.asList(KeyFactory.stringToKey(TEST_ENTITY_ID)));
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		AccessRequirementStats stats = new AccessRequirementStats();
		Set<String> set = new HashSet<String>();
		set.add("1");
		stats.setRequirementIdSet(set);
		stats.setHasToU(false);
		stats.setHasACT(false);
		stats.setHasLock(true);
		when(accessRequirementDAO.getAccessRequirementStats(Arrays.asList(KeyFactory.stringToKey(TEST_ENTITY_ID)), RestrictableObjectType.ENTITY)).thenReturn(stats );
		when(accessApprovalDAO.hasUnmetAccessRequirement(set, userInfo.getId().toString())).thenReturn(true);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, info.getRestrictionLevel());
		assertTrue(info.getHasUnmetAccessRequirement());
		verify(nodeDao).getEntityPathIds(TEST_ENTITY_ID);
	}

	@Test
	public void testGetRestrictionInformationWithACT() {
		when(nodeDao.getNode(TEST_ENTITY_ID)).thenReturn(testEntity);
		when(nodeDao.getEntityPathIds(eq(TEST_ENTITY_ID))).thenReturn(Arrays.asList(KeyFactory.stringToKey(TEST_ENTITY_ID)));

		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		AccessRequirementStats stats = new AccessRequirementStats();
		Set<String> set = new HashSet<String>();
		set.add("1");
		stats.setRequirementIdSet(set);
		stats.setHasToU(false);
		stats.setHasACT(true);
		stats.setHasLock(false);
		when(accessRequirementDAO.getAccessRequirementStats(Arrays.asList(KeyFactory.stringToKey(TEST_ENTITY_ID)), RestrictableObjectType.ENTITY)).thenReturn(stats );
		when(accessApprovalDAO.hasUnmetAccessRequirement(set, userInfo.getId().toString())).thenReturn(false);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, info.getRestrictionLevel());
		assertFalse(info.getHasUnmetAccessRequirement());
		verify(nodeDao).getEntityPathIds(TEST_ENTITY_ID);
	}

	@Test
	public void testGetRestrictionInformationWithBoth() {
		when(nodeDao.getNode(TEST_ENTITY_ID)).thenReturn(testEntity);
		when(nodeDao.getEntityPathIds(eq(TEST_ENTITY_ID))).thenReturn(Arrays.asList(KeyFactory.stringToKey(TEST_ENTITY_ID)));
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		AccessRequirementStats stats = new AccessRequirementStats();
		Set<String> set = new HashSet<String>();
		set.add("1");
		set.add("2");
		stats.setRequirementIdSet(set);
		stats.setHasToU(true);
		stats.setHasACT(true);
		stats.setHasLock(false);
		when(accessRequirementDAO.getAccessRequirementStats(Arrays.asList(KeyFactory.stringToKey(TEST_ENTITY_ID)), RestrictableObjectType.ENTITY)).thenReturn(stats );
		when(accessApprovalDAO.hasUnmetAccessRequirement(set, userInfo.getId().toString())).thenReturn(false);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, info.getRestrictionLevel());
		assertFalse(info.getHasUnmetAccessRequirement());
		verify(nodeDao).getEntityPathIds(TEST_ENTITY_ID);
	}

	@Test
	public void testGetRestrictionInformationWithIllegalState() {
		when(nodeDao.getNode(TEST_ENTITY_ID)).thenReturn(testEntity);
		when(nodeDao.getEntityPathIds(eq(TEST_ENTITY_ID))).thenReturn(Arrays.asList(KeyFactory.stringToKey(TEST_ENTITY_ID)));
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		AccessRequirementStats stats = new AccessRequirementStats();
		Set<String> set = new HashSet<String>();
		set.add("1");
		set.add("2");
		stats.setRequirementIdSet(set);
		stats.setHasToU(false);
		stats.setHasACT(false);
		stats.setHasLock(false);
		when(accessRequirementDAO.getAccessRequirementStats(Arrays.asList(KeyFactory.stringToKey(TEST_ENTITY_ID)), RestrictableObjectType.ENTITY)).thenReturn(stats);
		Assertions.assertThrows(IllegalStateException.class, () -> {
			arm.getRestrictionInformation(userInfo, request);
		});
	}

	@Test
	public void testGetRestrictionInformationForTeam() {
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_PRINCIPAL_ID);
		request.setRestrictableObjectType(RestrictableObjectType.TEAM);
		AccessRequirementStats stats = new AccessRequirementStats();
		Set<String> set = new HashSet<String>();
		set.add("1");
		stats.setRequirementIdSet(set);
		stats.setHasToU(true);
		stats.setHasACT(false);
		stats.setHasLock(false);
		when(accessRequirementDAO.getAccessRequirementStats(Arrays.asList(KeyFactory.stringToKey(TEST_PRINCIPAL_ID)), RestrictableObjectType.TEAM)).thenReturn(stats);
		when(accessApprovalDAO.hasUnmetAccessRequirement(set, userInfo.getId().toString())).thenReturn(true);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, info.getRestrictionLevel());
		assertTrue(info.getHasUnmetAccessRequirement());
		verify(nodeDao, never()).getEntityPath(TEST_ENTITY_ID);
	}

}
