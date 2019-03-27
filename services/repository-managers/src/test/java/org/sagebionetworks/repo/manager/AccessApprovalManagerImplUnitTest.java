package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessApprovalInfo;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoRequest;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoResponse;
import org.sagebionetworks.repo.model.HasAccessorRequirement;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.PostMessageContentAccessRequirement;
import org.sagebionetworks.repo.model.SelfSignAccessRequirement;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroup;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRevokeRequest;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Sets;

public class AccessApprovalManagerImplUnitTest {

	@Mock
	private AccessRequirementDAO mockAccessRequirementDAO;
	@Mock
	private AccessApprovalDAO mockAccessApprovalDAO;
	@Mock
	private AuthorizationManager mockAuthorizationManager;
	private AccessApprovalManager manager;
	UserInfo userInfo;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		manager = new AccessApprovalManagerImpl();
		ReflectionTestUtils.setField(manager, "accessRequirementDAO", mockAccessRequirementDAO);
		ReflectionTestUtils.setField(manager, "accessApprovalDAO", mockAccessApprovalDAO);
		ReflectionTestUtils.setField(manager, "authorizationManager", mockAuthorizationManager);
		userInfo = new UserInfo(false);
		userInfo.setId(3L);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testRevokeAccessApprovalsWithNullUserInfo() {
		manager.revokeAccessApprovals(null, "1", "1");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testRevokeAccessApprovalsWithNullAccessRequirementId() {
		manager.revokeAccessApprovals(userInfo, null, "1");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testRevokeAccessApprovalsWithNullAccessorId() {
		manager.revokeAccessApprovals(userInfo, "1", null);
	}

	@Test (expected = UnauthorizedException.class)
	public void testRevokeAccessApprovalsWithNonACTNorAdminUser() {
		String accessRequirementId = "1";
		String accessorId = "3";
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(false);
		manager.revokeAccessApprovals(userInfo, accessRequirementId, accessorId);
	}

	@Test (expected = NotFoundException.class)
	public void testRevokeAccessApprovalsWithNonExistingAccessRequirement() {
		String accessRequirementId = "1";
		String accessorId = "3";
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockAccessRequirementDAO.get(accessRequirementId)).thenThrow(new NotFoundException());
		manager.revokeAccessApprovals(userInfo, accessRequirementId, accessorId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testRevokeAccessApprovalsWithToUAccessRequirement() {
		String accessRequirementId = "1";
		String accessorId = "3";
		AccessRequirement accessRequirement = new TermsOfUseAccessRequirement();
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockAccessRequirementDAO.get(accessRequirementId)).thenReturn(accessRequirement);
		manager.revokeAccessApprovals(userInfo, accessRequirementId, accessorId);
	}

	@Test
	public void testRevokeAccessApprovalsWithACTAccessRequirement() {
		String accessRequirementId = "2";
		String accessorId = "3";
		AccessRequirement accessRequirement = new ACTAccessRequirement();
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockAccessRequirementDAO.get(accessRequirementId)).thenReturn(accessRequirement);
		manager.revokeAccessApprovals(userInfo, accessRequirementId, accessorId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testListAccessorGroupWithNullUserInfo() {
		AccessorGroupRequest request = new AccessorGroupRequest();
		manager.listAccessorGroup(null, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testListAccessorGroupWithNullRequest() {
		manager.listAccessorGroup(userInfo, null);
	}

	@Test (expected = UnauthorizedException.class)
	public void testListAccessorGroupUnauthorized() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(false);
		AccessorGroupRequest request = new AccessorGroupRequest();
		manager.listAccessorGroup(userInfo, request);
	}

	@Test
	public void testListAccessorGroupAuthorized() {
		AccessorGroupRequest request = new AccessorGroupRequest();
		List<AccessorGroup> result = new LinkedList<AccessorGroup>();
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockAccessApprovalDAO.listAccessorGroup(null, null, null, NextPageToken.DEFAULT_LIMIT+1, NextPageToken.DEFAULT_OFFSET)).thenReturn(result );
		AccessorGroupResponse response = manager.listAccessorGroup(userInfo, request);
		assertEquals(result, response.getResults());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testRevokeGroupWithNullUserInfo() {
		AccessorGroupRevokeRequest request = new AccessorGroupRevokeRequest();
		request.setAccessRequirementId("1");
		request.setSubmitterId("2");
		manager.revokeGroup(null, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testRevokeGroupWithNullRequest() {
		manager.revokeGroup(userInfo, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testRevokeGroupWithNullRequirementId() {
		AccessorGroupRevokeRequest request = new AccessorGroupRevokeRequest();
		request.setSubmitterId("2");
		manager.revokeGroup(userInfo, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testRevokeGroupWithNullSubmitterId() {
		AccessorGroupRevokeRequest request = new AccessorGroupRevokeRequest();
		request.setAccessRequirementId("1");
		manager.revokeGroup(userInfo, request);
	}

	@Test (expected = UnauthorizedException.class)
	public void testRevokeGroupUnauthorized() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(false);
		AccessorGroupRevokeRequest request = new AccessorGroupRevokeRequest();
		request.setAccessRequirementId("1");
		request.setSubmitterId("2");
		manager.revokeGroup(userInfo, request);
	}

	@Test
	public void testRevokeGroupAuthorized() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		AccessorGroupRevokeRequest request = new AccessorGroupRevokeRequest();
		request.setAccessRequirementId("1");
		request.setSubmitterId("2");
		manager.revokeGroup(userInfo, request);
		verify(mockAccessApprovalDAO).revokeGroup("1", "2","3");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateAccessApprovalWithNullUserInfo() {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setRequirementId(1L);
		manager.createAccessApproval(null, accessApproval);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateAccessApprovalWithNullUAccessApproval() {
		manager.createAccessApproval(userInfo, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateAccessApprovalWithNullRequirementId() {
		AccessApproval accessApproval = new AccessApproval();
		manager.createAccessApproval(userInfo, accessApproval);
	}

	@Test (expected = UnauthorizedException.class)
	public void testCreateAccessApprovalUnauthorized() {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setRequirementId(1L);
		when(mockAccessRequirementDAO.get("1")).thenReturn(new ACTAccessRequirement());
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(false);
		manager.createAccessApproval(userInfo, accessApproval);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateAccessApprovalWithoutAccessorId() {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setRequirementId(1L);
		when(mockAccessRequirementDAO.get("1")).thenReturn(new ACTAccessRequirement());
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		manager.createAccessApproval(userInfo, accessApproval);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateAccessApprovalForLockAR() {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setRequirementId(1L);
		accessApproval.setAccessorId("2");
		when(mockAccessRequirementDAO.get("1")).thenReturn(new LockAccessRequirement());
		manager.createAccessApproval(userInfo, accessApproval);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateAccessApprovalForPostMessageContentAR() {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setRequirementId(1L);
		accessApproval.setAccessorId("2");
		when(mockAccessRequirementDAO.get("1")).thenReturn(new PostMessageContentAccessRequirement());
		manager.createAccessApproval(userInfo, accessApproval);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateAccessApprovalAccessorRequirementNotSatisfied() {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setRequirementId(1L);
		accessApproval.setAccessorId("2");
		SelfSignAccessRequirement req = new SelfSignAccessRequirement();
		when(mockAccessRequirementDAO.get("1")).thenReturn(req);
		doThrow(new IllegalArgumentException()).when(mockAuthorizationManager)
				.validateHasAccessorRequirement(any(HasAccessorRequirement.class), anySet());
		manager.createAccessApproval(userInfo, accessApproval);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateAccessApprovalForAnonymous() {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setRequirementId(1L);
		accessApproval.setAccessorId(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString());
		when(mockAccessRequirementDAO.get("1")).thenReturn(new ACTAccessRequirement());
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		manager.createAccessApproval(userInfo, accessApproval);
	}

	@Test
	public void testCreateAccessApproval() {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setRequirementId(1L);
		SelfSignAccessRequirement req = new SelfSignAccessRequirement();
		when(mockAccessRequirementDAO.get("1")).thenReturn(req);
		manager.createAccessApproval(userInfo, accessApproval);
		ArgumentCaptor<AccessApproval> captor = ArgumentCaptor.forClass(AccessApproval.class);
		verify(mockAccessApprovalDAO).create(captor.capture());
		AccessApproval created = captor.getValue();
		assertNotNull(created);
		assertNotNull(created.getCreatedBy());
		assertNotNull(created.getCreatedOn());
		assertEquals(userInfo.getId().toString(), created.getAccessorId());
		assertEquals((Long)1L, created.getRequirementId());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAccessApprovalInfoWithNullUserInfo() {
		BatchAccessApprovalInfoRequest request = new BatchAccessApprovalInfoRequest();
		String requirementId = "1";
		request.setUserId(userInfo.getId().toString());
		request.setAccessRequirementIds(Arrays.asList(requirementId));
		manager.getAccessApprovalInfo(null, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAccessApprovalInfoWithNullRequest() {
		manager.getAccessApprovalInfo(userInfo, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAccessApprovalInfoWithNullUserId() {
		BatchAccessApprovalInfoRequest request = new BatchAccessApprovalInfoRequest();
		String requirementId = "1";
		request.setAccessRequirementIds(Arrays.asList(requirementId));
		manager.getAccessApprovalInfo(userInfo, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAccessApprovalInfoWithNullRequirementIds() {
		BatchAccessApprovalInfoRequest request = new BatchAccessApprovalInfoRequest();
		request.setUserId(userInfo.getId().toString());
		manager.getAccessApprovalInfo(userInfo, request);
	}

	@Test
	public void testGetAccessApprovalInfoWithEmptyRequirementIds() {
		BatchAccessApprovalInfoRequest request = new BatchAccessApprovalInfoRequest();
		request.setUserId(userInfo.getId().toString());
		request.setAccessRequirementIds(new LinkedList<String>());
		BatchAccessApprovalInfoResponse response = manager.getAccessApprovalInfo(userInfo, request);
		assertNotNull(response);
		assertNotNull(response.getResults());
		assertTrue(response.getResults().isEmpty());
		verifyZeroInteractions(mockAccessApprovalDAO);
	}

	@Test
	public void testGetAccessApprovalInfo() {
		BatchAccessApprovalInfoRequest request = new BatchAccessApprovalInfoRequest();
		request.setUserId(userInfo.getId().toString());
		request.setAccessRequirementIds(Arrays.asList("1", "2"));
		when(mockAccessApprovalDAO.getRequirementsUserHasApprovals(userInfo.getId().toString(), Arrays.asList("1", "2")))
				.thenReturn(Sets.newHashSet(Arrays.asList("1")));
		BatchAccessApprovalInfoResponse response = manager.getAccessApprovalInfo(userInfo, request);
		assertNotNull(response);
		assertNotNull(response.getResults());
		assertEquals(2, response.getResults().size());
		AccessApprovalInfo info1 = new AccessApprovalInfo();
		info1.setAccessRequirementId("1");
		info1.setUserId(userInfo.getId().toString());
		info1.setHasAccessApproval(true);
		AccessApprovalInfo info2 = new AccessApprovalInfo();
		info2.setAccessRequirementId("2");
		info2.setUserId(userInfo.getId().toString());
		info2.setHasAccessApproval(false);
		assertTrue(response.getResults().contains(info1));
		assertTrue(response.getResults().contains(info2));
	}
}
