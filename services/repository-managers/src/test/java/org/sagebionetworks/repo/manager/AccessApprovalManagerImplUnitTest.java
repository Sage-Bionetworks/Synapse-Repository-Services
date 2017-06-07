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
	public void testDeleteAccessApprovalWithNullUserInfo() {
		manager.deleteAccessApprovals(null, "1", "1");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testDeleteAccessApprovalWithNullAccessRequirementId() {
		manager.deleteAccessApprovals(new UserInfo(false), null, "1");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testDeleteAccessApprovalWithNullAccessorId() {
		manager.deleteAccessApprovals(new UserInfo(false), "1", null);
	}

	@Test (expected = UnauthorizedException.class)
	public void testDeleteAccessApprovalWithNonACTNorAdminUser() {
		UserInfo userInfo = new UserInfo(false);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(false);
		manager.deleteAccessApprovals(userInfo, "1", "1");
	}

	@Test (expected = NotFoundException.class)
	public void testDeleteAccessApprovalWithNonExistingAccessRequirement() {
		UserInfo userInfo = new UserInfo(false);
		String accessRequirementId = "1";
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockAccessRequirementDAO.get(accessRequirementId)).thenThrow(new NotFoundException());
		manager.deleteAccessApprovals(userInfo, accessRequirementId, "1");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testDeleteAccessApprovalWithNonACTAccessRequirement() {
		UserInfo userInfo = new UserInfo(false);
		String accessRequirementId = "1";
		AccessRequirement accessRequirement = new TermsOfUseAccessRequirement();
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockAccessRequirementDAO.get(accessRequirementId)).thenReturn(accessRequirement);
		manager.deleteAccessApprovals(userInfo, accessRequirementId, "1");
	}

	@Test
	public void testDeleteAccessApprovalWithACTAccessRequirement() {
		UserInfo userInfo = new UserInfo(false);
		String accessRequirementId = "1";
		AccessRequirement accessRequirement = new ACTAccessRequirement();
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockAccessRequirementDAO.get(accessRequirementId)).thenReturn(accessRequirement);
		manager.deleteAccessApprovals(userInfo, accessRequirementId, "1");
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
