package org.sagebionetworks.repo.manager;

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
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
}
