package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.Count;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
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
	public void testRevokeAccessApprovalsWithNonACTAccessRequirement() {
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
	public void testDeleteBatchWithNullUserInfo() {
		IdList toDelete = new IdList();
		toDelete.setList(Arrays.asList(1L));
		manager.deleteBatch(null, toDelete);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testDeleteBatchWithNullIdList() {
		UserInfo userInfo = new UserInfo(false);
		manager.deleteBatch(userInfo, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testDeleteBatchWithEmptyIdList() {
		UserInfo userInfo = new UserInfo(false);
		IdList toDelete = new IdList();
		manager.deleteBatch(userInfo, toDelete);
	}

	@Test (expected = UnauthorizedException.class)
	public void testDeleteBatchUnauthorized() {
		UserInfo userInfo = new UserInfo(false);
		IdList toDelete = new IdList();
		toDelete.setList(Arrays.asList(1L));
		manager.deleteBatch(userInfo, toDelete);
	}

	@Test
	public void testDeleteBatchAuthorized() {
		UserInfo userInfo = new UserInfo(true);
		IdList toDelete = new IdList();
		toDelete.setList(Arrays.asList(1L));
		when(mockAccessApprovalDAO.deleteBatch(Arrays.asList(1L))).thenReturn(1);
		Count count = manager.deleteBatch(userInfo, toDelete);
		assertNotNull(count);
		assertEquals((Long)1L, count.getCount());
		verify(mockAccessApprovalDAO).deleteBatch(Arrays.asList(1L));
	}

}
