package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalResult;
import org.sagebionetworks.repo.model.dataaccess.BatchAccessApprovalRequest;
import org.sagebionetworks.repo.model.dataaccess.BatchAccessApprovalResult;
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

	@Test (expected = IllegalArgumentException.class)
	public void testGetApprovalInfoWithNullUserInfo() {
		BatchAccessApprovalRequest batchRequest = new BatchAccessApprovalRequest();
		AccessApprovalRequest request = new AccessApprovalRequest();
		request.setUserId("1L");
		request.setAccessRequirementId("2L");
		List<AccessApprovalRequest> requests = Arrays.asList(request);
		batchRequest.setRequests(requests);
		manager.getApprovalInfo(null, batchRequest);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetApprovalInfoWithNullRequest() {
		UserInfo userInfo = new UserInfo(false);
		manager.getApprovalInfo(userInfo, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetApprovalInfoWithNullList() {
		UserInfo userInfo = new UserInfo(false);
		BatchAccessApprovalRequest batchRequest = new BatchAccessApprovalRequest();
		manager.getApprovalInfo(userInfo, batchRequest);
	}

	@Test
	public void testGetApprovalInfoWithEmptyList() {
		UserInfo userInfo = new UserInfo(true);
		BatchAccessApprovalRequest batchRequest = new BatchAccessApprovalRequest();
		batchRequest.setRequests(new LinkedList<AccessApprovalRequest>());
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		BatchAccessApprovalResult result = manager.getApprovalInfo(userInfo, batchRequest);
		assertNotNull(result);
		assertNotNull(result.getResults());
		assertTrue(result.getResults().isEmpty());
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetApprovalInfoUnauthorized() {
		UserInfo userInfo = new UserInfo(false);
		BatchAccessApprovalRequest batchRequest = new BatchAccessApprovalRequest();
		AccessApprovalRequest request = new AccessApprovalRequest();
		request.setUserId("1L");
		request.setAccessRequirementId("2L");
		List<AccessApprovalRequest> requests = Arrays.asList(request);
		batchRequest.setRequests(requests);
		manager.getApprovalInfo(userInfo, batchRequest);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetApprovalInfoWithNullUserId() {
		UserInfo userInfo = new UserInfo(true);
		BatchAccessApprovalRequest batchRequest = new BatchAccessApprovalRequest();
		AccessApprovalRequest request = new AccessApprovalRequest();
		request.setAccessRequirementId("2L");
		List<AccessApprovalRequest> requests = Arrays.asList(request);
		batchRequest.setRequests(requests);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		manager.getApprovalInfo(userInfo, batchRequest);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetApprovalInfoWithNullAccessRequirementId() {
		UserInfo userInfo = new UserInfo(true);
		BatchAccessApprovalRequest batchRequest = new BatchAccessApprovalRequest();
		AccessApprovalRequest request = new AccessApprovalRequest();
		request.setUserId("1L");
		List<AccessApprovalRequest> requests = Arrays.asList(request);
		batchRequest.setRequests(requests);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		manager.getApprovalInfo(userInfo, batchRequest);
	}

	@Test
	public void testGetApprovalInfo() {
		UserInfo userInfo = new UserInfo(true);
		BatchAccessApprovalRequest batchRequest = new BatchAccessApprovalRequest();
		AccessApprovalRequest request1 = new AccessApprovalRequest();
		request1.setUserId("1L");
		request1.setAccessRequirementId("2L");
		AccessApprovalRequest request2 = new AccessApprovalRequest();
		request2.setUserId("3L");
		request2.setAccessRequirementId("4L");
		List<AccessApprovalRequest> requests = Arrays.asList(request1, request2);
		batchRequest.setRequests(requests);
		Set<String> userIds = new HashSet<String>();
		userIds.add("1L");
		userIds.add("3L");
		Set<String> requirementIds = new HashSet<String>();
		requirementIds.add("2L");
		requirementIds.add("4L");
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		map.put("2L", Arrays.asList("1L", "3L"));
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockAccessApprovalDAO.getAccessApprovalInfo(userIds, requirementIds)).thenReturn(map);
		BatchAccessApprovalResult result = manager.getApprovalInfo(userInfo, batchRequest);
		assertNotNull(result);
		assertNotNull(result.getResults());
		assertEquals(2, result.getResults().size());
		AccessApprovalResult result1 = result.getResults().get(0);
		AccessApprovalResult result2 = result.getResults().get(1);
		assertEquals(request1.getUserId(), result1.getUserId());
		assertEquals(request2.getUserId(), result2.getUserId());
		assertEquals(request1.getAccessRequirementId(), result1.getAccessRequirementId());
		assertEquals(request2.getAccessRequirementId(), result2.getAccessRequirementId());
		assertTrue(result1.getHasApproval());
		assertFalse(result2.getHasApproval());
	}
}
