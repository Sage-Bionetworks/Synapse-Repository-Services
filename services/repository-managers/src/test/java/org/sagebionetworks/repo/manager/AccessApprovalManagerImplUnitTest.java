package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroup;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRevokeRequest;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

public class AccessApprovalManagerImplUnitTest {

	@Mock
	private AccessRequirementDAO mockAccessRequirementDAO;
	@Mock
	private AccessApprovalDAO mockAccessApprovalDAO;
	@Mock
	private AuthorizationManager mockAuthorizationManager;
	private AccessApprovalManager manager;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		manager = new AccessApprovalManagerImpl();
		ReflectionTestUtils.setField(manager, "accessRequirementDAO", mockAccessRequirementDAO);
		ReflectionTestUtils.setField(manager, "accessApprovalDAO", mockAccessApprovalDAO);
		ReflectionTestUtils.setField(manager, "authorizationManager", mockAuthorizationManager);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testRevokeAccessApprovalsWithNullUserInfo() {
		manager.revokeAccessApprovals(null, "1", "1");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testRevokeAccessApprovalsWithNullAccessRequirementId() {
		manager.revokeAccessApprovals(new UserInfo(false), null, "1");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testRevokeAccessApprovalsWithNullAccessorId() {
		manager.revokeAccessApprovals(new UserInfo(false), "1", null);
	}

	@Test (expected = UnauthorizedException.class)
	public void testRevokeAccessApprovalsWithNonACTNorAdminUser() {
		UserInfo userInfo = new UserInfo(false);
		String accessRequirementId = "1";
		String accessorId = "3";
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(false);
		manager.revokeAccessApprovals(userInfo, accessRequirementId, accessorId);
	}

	@Test (expected = NotFoundException.class)
	public void testRevokeAccessApprovalsWithNonExistingAccessRequirement() {
		UserInfo userInfo = new UserInfo(false);
		String accessRequirementId = "1";
		String accessorId = "3";
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockAccessRequirementDAO.get(accessRequirementId)).thenThrow(new NotFoundException());
		manager.revokeAccessApprovals(userInfo, accessRequirementId, accessorId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testRevokeAccessApprovalsWithToUAccessRequirement() {
		UserInfo userInfo = new UserInfo(false);
		String accessRequirementId = "1";
		String accessorId = "3";
		AccessRequirement accessRequirement = new TermsOfUseAccessRequirement();
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockAccessRequirementDAO.get(accessRequirementId)).thenReturn(accessRequirement);
		manager.revokeAccessApprovals(userInfo, accessRequirementId, accessorId);
	}

	@Test
	public void testRevokeAccessApprovalsWithACTAccessRequirement() {
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(1L);
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
		UserInfo userInfo = new UserInfo(false);
		manager.listAccessorGroup(userInfo, null);
	}

	@Test (expected = UnauthorizedException.class)
	public void testListAccessorGroupUnauthorized() {
		UserInfo userInfo = new UserInfo(false);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(false);
		AccessorGroupRequest request = new AccessorGroupRequest();
		manager.listAccessorGroup(userInfo, request);
	}

	@Test
	public void testListAccessorGroupAuthorized() {
		UserInfo userInfo = new UserInfo(false);
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
		UserInfo userInfo = new UserInfo(false);
		manager.revokeGroup(userInfo, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testRevokeGroupWithNullRequirementId() {
		UserInfo userInfo = new UserInfo(false);
		AccessorGroupRevokeRequest request = new AccessorGroupRevokeRequest();
		request.setSubmitterId("2");
		manager.revokeGroup(userInfo, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testRevokeGroupWithNullSubmitterId() {
		UserInfo userInfo = new UserInfo(false);
		AccessorGroupRevokeRequest request = new AccessorGroupRevokeRequest();
		request.setAccessRequirementId("1");
		manager.revokeGroup(userInfo, request);
	}

	@Test (expected = UnauthorizedException.class)
	public void testRevokeGroupUnauthorized() {
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(3L);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(false);
		AccessorGroupRevokeRequest request = new AccessorGroupRevokeRequest();
		request.setAccessRequirementId("1");
		request.setSubmitterId("2");
		manager.revokeGroup(userInfo, request);
	}

	@Test
	public void testRevokeGroupAuthorized() {
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(3L);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		AccessorGroupRevokeRequest request = new AccessorGroupRevokeRequest();
		request.setAccessRequirementId("1");
		request.setSubmitterId("2");
		manager.revokeGroup(userInfo, request);
		verify(mockAccessApprovalDAO).revokeGroup("1", "2","3");
	}
}
