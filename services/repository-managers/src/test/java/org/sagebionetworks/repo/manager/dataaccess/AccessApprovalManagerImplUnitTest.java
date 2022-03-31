package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserCertificationRequiredException;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessApprovalInfo;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoRequest;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoResponse;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.HasAccessorRequirement;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PostMessageContentAccessRequirement;
import org.sagebionetworks.repo.model.SelfSignAccessRequirement;
import org.sagebionetworks.repo.model.TeamConstants;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroup;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRevokeRequest;
import org.sagebionetworks.repo.model.dbo.verification.VerificationDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.MessageToSend;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class AccessApprovalManagerImplUnitTest {

	@Mock
	private AccessRequirementDAO mockAccessRequirementDAO;
	@Mock
	private AccessApprovalDAO mockAccessApprovalDAO;
	@Mock
	private TransactionalMessenger mockTransactionMessenger;
	@Mock
	private VerificationDAO mockVerificationDao;
	@Mock
	private GroupMembersDAO mockgroupMembersDao;
	@Mock
	private NodeDAO nodeDao;
	
	@Mock
	private Set<String> accessors;
	
	@Spy
	@InjectMocks
	private AccessApprovalManagerImpl manager;
	
	private UserInfo userInfo;
	private UserInfo atcUser;

	@BeforeEach
	public void before() {
		userInfo = new UserInfo(false);
		userInfo.setId(4L);
		boolean isAdmin = false;
		atcUser = new UserInfo(isAdmin, 5L);
		atcUser.setGroups(Sets.newHashSet(TeamConstants.ACT_TEAM_ID));
	}

	@Test
	public void testRevokeAccessApprovalsWithNullUserInfo() {
		assertThrows(IllegalArgumentException.class, () -> {
			manager.revokeAccessApprovals(null, "1", "1");
		});
	}

	@Test
	public void testRevokeAccessApprovalsWithNullAccessRequirementId() {
		assertThrows(IllegalArgumentException.class, () -> {
			manager.revokeAccessApprovals(userInfo, null, "1");
		});
	}

	@Test
	public void testRevokeAccessApprovalsWithNullAccessorId() {
		assertThrows(IllegalArgumentException.class, () -> {
			manager.revokeAccessApprovals(userInfo, "1", null);
		});
	}
	

	@Test
	public void testRevokeAccessApprovalsWithNonACTNorAdminUser() {
		String accessRequirementId = "1";
		String accessorId = ""+userInfo.getId()+1L;
		assertThrows(UnauthorizedException.class, () -> {
			manager.revokeAccessApprovals(userInfo, accessRequirementId, accessorId);
		});
	}

	@Test
	public void testRevokeAccessApprovalsWithNonExistingAccessRequirement() {
		String accessRequirementId = "1";
		String accessorId = ""+userInfo.getId();
		when(mockAccessRequirementDAO.get(accessRequirementId)).thenThrow(new NotFoundException(""));
		assertThrows(NotFoundException.class, () -> {
			manager.revokeAccessApprovals(userInfo, accessRequirementId, accessorId);
		});
	}

	@Test
	public void testRevokeAccessApprovalsWithToUAccessRequirement() {
		String accessRequirementId = "1";
		String accessorId = ""+userInfo.getId();
		AccessRequirement accessRequirement = new TermsOfUseAccessRequirement();
		when(mockAccessRequirementDAO.get(accessRequirementId)).thenReturn(accessRequirement);
		assertThrows(IllegalArgumentException.class, () -> {
			manager.revokeAccessApprovals(userInfo, accessRequirementId, accessorId);
		});
	}

	@Test
	public void testRevokeAccessApprovalsWithACTAccessRequirement() {
		String accessRequirementId = "2";
		String accessorId = ""+userInfo.getId();
		List<Long> approvals = Arrays.asList(1L, 2L);
		AccessRequirement accessRequirement = new ACTAccessRequirement();
		when(mockAccessRequirementDAO.get(accessRequirementId)).thenReturn(accessRequirement);	
		when(mockAccessApprovalDAO.listApprovalsByAccessor(any(), any())).thenReturn(approvals);
		when(mockAccessApprovalDAO.revokeBatch(any(), any())).thenReturn(approvals);
		
		// Call under test
		manager.revokeAccessApprovals(userInfo, accessRequirementId, accessorId);
		
		verify(mockAccessApprovalDAO).listApprovalsByAccessor(accessRequirementId, accessorId);
		verify(mockAccessApprovalDAO).revokeBatch(userInfo.getId(), approvals);
		
		for (Long id : approvals) {
			
			MessageToSend expectedMessage = new MessageToSend()
					.withUserId(userInfo.getId())
					.withObjectType(ObjectType.ACCESS_APPROVAL)
					.withObjectId(id.toString())
					.withChangeType(ChangeType.UPDATE);
			
			verify(mockTransactionMessenger).sendMessageAfterCommit(expectedMessage);
		}
	}
	
	/**
	 * Added for PLFM-6922.
	 */
	@Test
	public void testRevokeAccessApprovalsWithOwnApproval() {
		String accessRequirementId = "2";
		String accessorId = userInfo.getId().toString();
		List<Long> approvals = Arrays.asList(1L, 2L);
		AccessRequirement accessRequirement = new ACTAccessRequirement();
		when(mockAccessRequirementDAO.get(accessRequirementId)).thenReturn(accessRequirement);	
		when(mockAccessApprovalDAO.listApprovalsByAccessor(any(), any())).thenReturn(approvals);
		when(mockAccessApprovalDAO.revokeBatch(any(), any())).thenReturn(approvals);
		
		// Call under test
		manager.revokeAccessApprovals(userInfo, accessRequirementId, accessorId);
		
		verify(mockAccessApprovalDAO).listApprovalsByAccessor(accessRequirementId, accessorId);
		verify(mockAccessApprovalDAO).revokeBatch(userInfo.getId(), approvals);
		
		for (Long id : approvals) {
			
			MessageToSend expectedMessage = new MessageToSend()
					.withUserId(userInfo.getId())
					.withObjectType(ObjectType.ACCESS_APPROVAL)
					.withObjectId(id.toString())
					.withChangeType(ChangeType.UPDATE);
			
			verify(mockTransactionMessenger).sendMessageAfterCommit(expectedMessage);
		}
	}

	@Test
	public void testListAccessorGroupWithNullUserInfo() {
		AccessorGroupRequest request = new AccessorGroupRequest();
		assertThrows(IllegalArgumentException.class, () -> {
			manager.listAccessorGroup(null, request);
		});
	}

	@Test
	public void testListAccessorGroupWithNullRequest() {
		assertThrows(IllegalArgumentException.class, () -> {
			manager.listAccessorGroup(userInfo, null);
		});
	}

	@Test
	public void testListAccessorGroupUnauthorized() {
		AccessorGroupRequest request = new AccessorGroupRequest();
		assertThrows(UnauthorizedException.class, () -> {
			manager.listAccessorGroup(userInfo, request);
		});
	}

	@Test
	public void testListAccessorGroupAuthorized() {
		AccessorGroupRequest request = new AccessorGroupRequest();
		List<AccessorGroup> result = new LinkedList<AccessorGroup>();
		when(mockAccessApprovalDAO.listAccessorGroup(null, null, null, null, NextPageToken.DEFAULT_LIMIT+1, NextPageToken.DEFAULT_OFFSET)).thenReturn(result );
		AccessorGroupResponse response = manager.listAccessorGroup(atcUser, request);
		assertEquals(result, response.getResults());
	}
	
	@Test
	public void testListAccessorGroup() {
		
		AccessorGroupRequest request = new AccessorGroupRequest()
				.setAccessorId("123")
				.setSubmitterId("456")
				.setAccessRequirementId("789")
				.setExpireBefore(new Date())
				.setNextPageToken(null);
		
		List<AccessorGroup> expected = Collections.emptyList();
		NextPageToken expectedToken = new NextPageToken(request.getNextPageToken());
		
		when(mockAccessApprovalDAO.listAccessorGroup(any(), any(), any(), any(), anyLong(), anyLong())).thenReturn(expected);
		
		AccessorGroupResponse response = manager.listAccessorGroup(atcUser, request);
		
		verify(mockAccessApprovalDAO).listAccessorGroup(request.getAccessRequirementId(), request.getSubmitterId(), request.getAccessorId(), request.getExpireBefore(), expectedToken.getLimitForQuery(), expectedToken.getOffset());
		
		assertEquals(expected, response.getResults());
	}

	@Test
	public void testRevokeGroupWithNullUserInfo() {
		AccessorGroupRevokeRequest request = new AccessorGroupRevokeRequest();
		request.setAccessRequirementId("1");
		request.setSubmitterId("2");
		assertThrows(IllegalArgumentException.class, () -> {
			manager.revokeGroup(null, request);
		});
	}

	@Test
	public void testRevokeGroupWithNullRequest() {
		assertThrows(IllegalArgumentException.class, () -> {
			manager.revokeGroup(userInfo, null);
		});
	}

	@Test
	public void testRevokeGroupWithNullRequirementId() {
		AccessorGroupRevokeRequest request = new AccessorGroupRevokeRequest();
		request.setSubmitterId("2");
		assertThrows(IllegalArgumentException.class, () -> {
			manager.revokeGroup(userInfo, request);
		});
	}

	@Test
	public void testRevokeGroupWithNullSubmitterId() {
		AccessorGroupRevokeRequest request = new AccessorGroupRevokeRequest();
		request.setAccessRequirementId("1");
		assertThrows(IllegalArgumentException.class, () -> {
			manager.revokeGroup(userInfo, request);
		});
	}

	@Test
	public void testRevokeGroupUnauthorized() {
		AccessorGroupRevokeRequest request = new AccessorGroupRevokeRequest();
		request.setAccessRequirementId("1");
		request.setSubmitterId("2");
		assertThrows(UnauthorizedException.class, () -> {
			manager.revokeGroup(userInfo, request);
		});
	}

	@Test
	public void testRevokeGroupAuthorized() {
		List<Long> approvals = Arrays.asList(1L, 2L);
		
		when(mockAccessApprovalDAO.listApprovalsBySubmitter(any(), any())).thenReturn(approvals);
		when(mockAccessApprovalDAO.revokeBatch(any(), any())).thenReturn(approvals);
		
		AccessorGroupRevokeRequest request = new AccessorGroupRevokeRequest();
		request.setAccessRequirementId("1");
		request.setSubmitterId("2");
		
		manager.revokeGroup(atcUser, request);
		
		verify(mockAccessApprovalDAO).listApprovalsBySubmitter("1", "2");
		verify(mockAccessApprovalDAO).revokeBatch(atcUser.getId(), approvals);
		
		for (Long id : approvals) {
			
			MessageToSend expectedMessage = new MessageToSend()
					.withUserId(atcUser.getId())
					.withObjectType(ObjectType.ACCESS_APPROVAL)
					.withObjectId(id.toString())
					.withChangeType(ChangeType.UPDATE);
			
			verify(mockTransactionMessenger).sendMessageAfterCommit(expectedMessage);
		}
		
	}

	@Test
	public void testCreateAccessApprovalWithNullUserInfo() {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setRequirementId(1L);
		assertThrows(IllegalArgumentException.class, () -> {
			manager.createAccessApproval(null, accessApproval);
		});
	}

	@Test
	public void testCreateAccessApprovalWithNullUAccessApproval() {
		assertThrows(IllegalArgumentException.class, () -> {
			manager.createAccessApproval(userInfo, null);
		});
	}

	@Test
	public void testCreateAccessApprovalWithNullRequirementId() {
		AccessApproval accessApproval = new AccessApproval();
		assertThrows(IllegalArgumentException.class, () -> {
			manager.createAccessApproval(userInfo, accessApproval);
		});
	}

	@Test
	public void testCreateAccessApprovalUnauthorized() {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setRequirementId(1L);
		when(mockAccessRequirementDAO.get("1")).thenReturn(new ACTAccessRequirement());
		assertThrows(UnauthorizedException.class, () -> {
			manager.createAccessApproval(userInfo, accessApproval);
		});
	}

	@Test
	public void testCreateAccessApprovalWithoutAccessorId() {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setRequirementId(1L);
		when(mockAccessRequirementDAO.get("1")).thenReturn(new ACTAccessRequirement());
		assertThrows(IllegalArgumentException.class, () -> {
			manager.createAccessApproval(atcUser, accessApproval);
		});
	}

	@Test
	public void testCreateAccessApprovalForLockAR() {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setRequirementId(1L);
		accessApproval.setAccessorId("2");
		when(mockAccessRequirementDAO.get("1")).thenReturn(new LockAccessRequirement());
		assertThrows(IllegalArgumentException.class, () -> {
			manager.createAccessApproval(userInfo, accessApproval);
		});
	}

	@Test
	public void testCreateAccessApprovalForPostMessageContentAR() {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setRequirementId(1L);
		accessApproval.setAccessorId("2");
		when(mockAccessRequirementDAO.get("1")).thenReturn(new PostMessageContentAccessRequirement());
		assertThrows(IllegalArgumentException.class, () -> {
			manager.createAccessApproval(userInfo, accessApproval);
		});
	}

	@Test
	public void testCreateAccessApprovalAccessorRequirementNotSatisfied() {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setRequirementId(1L);
		accessApproval.setAccessorId("2");
		SelfSignAccessRequirement req = new SelfSignAccessRequirement();
		when(mockAccessRequirementDAO.get("1")).thenReturn(req);
		doThrow(new IllegalArgumentException()).when(manager)
				.validateHasAccessorRequirement(any(HasAccessorRequirement.class), anySet());
		
		assertThrows(IllegalArgumentException.class, () -> {
			manager.createAccessApproval(userInfo, accessApproval);
		});
	}

	@Test
	public void testCreateAccessApprovalForAnonymous() {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setRequirementId(1L);
		accessApproval.setAccessorId(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString());
		when(mockAccessRequirementDAO.get("1")).thenReturn(new ACTAccessRequirement());
		
		assertThrows(IllegalArgumentException.class, () -> {
			manager.createAccessApproval(atcUser, accessApproval);
		});
	}

	@Test
	public void testCreateAccessApproval() {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setRequirementId(1L);
		SelfSignAccessRequirement req = new SelfSignAccessRequirement().setIsCertifiedUserRequired(false).setIsValidatedProfileRequired(false);
		when(mockAccessRequirementDAO.get(any())).thenReturn(req);
		// call under test
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

	@Test
	public void testGetAccessApprovalInfoWithNullUserInfo() {
		BatchAccessApprovalInfoRequest request = new BatchAccessApprovalInfoRequest();
		String requirementId = "1";
		request.setUserId(userInfo.getId().toString());
		request.setAccessRequirementIds(Arrays.asList(requirementId));
		assertThrows(IllegalArgumentException.class, () -> {
			manager.getAccessApprovalInfo(null, request);
		});
	}

	@Test
	public void testGetAccessApprovalInfoWithNullRequest() {
		assertThrows(IllegalArgumentException.class, () -> {
			manager.getAccessApprovalInfo(userInfo, null);
		});
	}

	@Test
	public void testGetAccessApprovalInfoWithNullUserId() {
		BatchAccessApprovalInfoRequest request = new BatchAccessApprovalInfoRequest();
		String requirementId = "1";
		request.setAccessRequirementIds(Arrays.asList(requirementId));
		assertThrows(IllegalArgumentException.class, () -> {
			manager.getAccessApprovalInfo(userInfo, request);
		});
	}

	@Test
	public void testGetAccessApprovalInfoWithNullRequirementIds() {
		BatchAccessApprovalInfoRequest request = new BatchAccessApprovalInfoRequest();
		request.setUserId(userInfo.getId().toString());
		assertThrows(IllegalArgumentException.class, () -> {
			manager.getAccessApprovalInfo(userInfo, request);
		});
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
	
	@Test
	public void testRevokeExpiredApprovalsWithNoUser() {
		UserInfo user = null;
		Instant expiredAfter = Instant.now().minus(1, ChronoUnit.DAYS);
		int maxBatchSize = 10;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.revokeExpiredApprovals(user, expiredAfter, maxBatchSize);
		}).getMessage();
		
		assertEquals("The user is required.", message);
	}
	
	@Test
	public void testRevokeExpiredApprovalsWithNoExpireAfter() {
		UserInfo user = userInfo;
		Instant expiredAfter = null;
		int maxBatchSize = 10;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.revokeExpiredApprovals(user, expiredAfter, maxBatchSize);
		}).getMessage();
	
		assertEquals("The expiredAfter is required.", message);
	}
	
	@Test
	public void testRevokeExpiredApprovalsWithFutureExpireAfter() {
		UserInfo user = userInfo;
		Instant expiredAfter = Instant.now().plus(1, ChronoUnit.DAYS);
		int maxBatchSize = 10;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.revokeExpiredApprovals(user, expiredAfter, maxBatchSize);
		}).getMessage();
		
		assertEquals("The expiredAfter must be a value in the past.", message);
	}
	
	@Test
	public void testRevokeExpiredApprovalsWithZeroBatchSize() {
		UserInfo user = userInfo;
		Instant expiredAfter = Instant.now().minus(1, ChronoUnit.DAYS);
		int maxBatchSize = 0;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.revokeExpiredApprovals(user, expiredAfter, maxBatchSize);
		}).getMessage();
		
		assertEquals("The maxBatchSize must be greater than 0.", message);
	}
	
	@Test
	public void testRevokeExpiredApprovalsWithNegativeBatchSize() {
		UserInfo user = userInfo;
		Instant expiredAfter = Instant.now().minus(1, ChronoUnit.DAYS);
		int maxBatchSize = -1;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.revokeExpiredApprovals(user, expiredAfter, maxBatchSize);
		}).getMessage();
		
		assertEquals("The maxBatchSize must be greater than 0.", message);
	}
	
	@Test
	public void testRevokeExpiredApprovals() {

		List<Long> expiredApprovals = Arrays.asList(1L, 2L);
		List<Long> revokedApprovals = Arrays.asList(2L);

		when(mockAccessApprovalDAO.listExpiredApprovals(any(), anyInt())).thenReturn(expiredApprovals);
		when(mockAccessApprovalDAO.revokeBatch(any(), any())).thenReturn(revokedApprovals);
		
		UserInfo user = atcUser;
		Instant expiredAfter = Instant.now().minus(1, ChronoUnit.DAYS);
		int maxBatchSize = 10;
		
		// Call under test
		int result = manager.revokeExpiredApprovals(user, expiredAfter, maxBatchSize);
		
		assertEquals(1, result);
		verify(mockAccessApprovalDAO).listExpiredApprovals(expiredAfter, maxBatchSize);
		verify(mockAccessApprovalDAO).revokeBatch(user.getId(), expiredApprovals);
		
		ArgumentCaptor<MessageToSend> messageCaptor = ArgumentCaptor.forClass(MessageToSend.class);
		
		verify(mockTransactionMessenger).sendMessageAfterCommit(messageCaptor.capture());
		
		List<MessageToSend> sentMessages = messageCaptor.getAllValues();
		
		// Despite 2 initial expired approvals, only one message was sent as only one was revoked
		assertEquals(1, sentMessages.size());
		
		MessageToSend message = sentMessages.get(0);
		
		assertEquals(user.getId(), message.getUserId());
		assertEquals(ObjectType.ACCESS_APPROVAL, message.getObjectType());
		assertEquals(revokedApprovals.get(0).toString(), message.getObjectId());
		assertEquals(ChangeType.UPDATE, message.getChangeType());
		
	}
	
	@Test
	public void testRevokeExpiredApprovalsNotAuthorized() {
		UserInfo user = userInfo;
		Instant expiredAfter = Instant.now().minus(1, ChronoUnit.DAYS);
		int maxBatchSize = 10;
		
		String message = assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			manager.revokeExpiredApprovals(user, expiredAfter, maxBatchSize);
		}).getMessage();
		
		assertEquals("Only ACT member can perform this action.", message);
	}
	
	@Test
	public void testRevokeExpiredApprovalsWithNoExpiredApprovals() {

		List<Long> expiredApprovals = Collections.emptyList();

		when(mockAccessApprovalDAO.listExpiredApprovals(any(), anyInt())).thenReturn(expiredApprovals);
		
		UserInfo user = atcUser;
		Instant expiredAfter = Instant.now().minus(1, ChronoUnit.DAYS);
		int maxBatchSize = 10;
		
		// Call under test
		int result = manager.revokeExpiredApprovals(user, expiredAfter, maxBatchSize);
		
		assertEquals(0, result);
		verify(mockAccessApprovalDAO).listExpiredApprovals(expiredAfter, maxBatchSize);
		verifyNoMoreInteractions(mockAccessApprovalDAO);
		verifyZeroInteractions(mockTransactionMessenger);
		
	}
	
	@Test
	public void testRevokeExpiredApprovalsWithNoRevokedApprovals() {

		List<Long> expiredApprovals = Arrays.asList(1L, 2L);
		List<Long> revokedApprovals = Collections.emptyList();
		
		when(mockAccessApprovalDAO.listExpiredApprovals(any(), anyInt())).thenReturn(expiredApprovals);
		when(mockAccessApprovalDAO.revokeBatch(any(), any())).thenReturn(revokedApprovals);
		
		UserInfo user = atcUser;
		Instant expiredAfter = Instant.now().minus(1, ChronoUnit.DAYS);
		int maxBatchSize = 10;
		
		// Call under test
		int result = manager.revokeExpiredApprovals(user, expiredAfter, maxBatchSize);
		
		assertEquals(0, result);
		verify(mockAccessApprovalDAO).listExpiredApprovals(expiredAfter, maxBatchSize);
		verify(mockAccessApprovalDAO).revokeBatch(user.getId(), expiredApprovals);
		verifyNoMoreInteractions(mockAccessApprovalDAO);
		verifyZeroInteractions(mockTransactionMessenger);
		
	}
	
	@Test
	public void testRevokeGroupAccessorsAuthorized() {
		List<Long> approvals = Arrays.asList(1L, 2L);
		
		when(mockAccessApprovalDAO.listApprovalsBySubmitter(any(), any(), any())).thenReturn(approvals);
		when(mockAccessApprovalDAO.revokeBatch(any(), any())).thenReturn(approvals);
		
		String accessRequirementId = "2";
		String submitterId = "1";
		List<String> accessorIds = Arrays.asList("1", "2");
		
		// Call under test
		manager.revokeGroup(atcUser, accessRequirementId, submitterId, accessorIds);
		
		verify(mockAccessApprovalDAO).listApprovalsBySubmitter(accessRequirementId, submitterId, accessorIds);
		verify(mockAccessApprovalDAO).revokeBatch(atcUser.getId(), approvals);
		
		for (Long id : approvals) {
			
			MessageToSend expectedMessage = new MessageToSend()
					.withUserId(atcUser.getId())
					.withObjectType(ObjectType.ACCESS_APPROVAL)
					.withObjectId(id.toString())
					.withChangeType(ChangeType.UPDATE);
			
			verify(mockTransactionMessenger).sendMessageAfterCommit(expectedMessage);
		}
		
	}
	
	@Test
	public void testRevokeGroupAccessorsUnauthorized() {
		
		String accessRequirementId = "2";
		String submitterId = "1";
		List<String> accessorIds = Arrays.asList("1", "2");
		
		assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			manager.revokeGroup(userInfo, accessRequirementId, submitterId, accessorIds);
		});
		
		verifyZeroInteractions(mockAccessApprovalDAO);
		verifyZeroInteractions(mockTransactionMessenger);
		
	}
	
	@Test
	public void testRevokeGroupAccessorsWithNullUser() {
		
		String accessRequirementId = "2";
		String submitterId = "1";
		List<String> accessorIds = Arrays.asList("1", "2");
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.revokeGroup(null, accessRequirementId, submitterId, accessorIds);
		}).getMessage();
		
		assertEquals("The user is required.", message);
	}
	
	@Test
	public void testRevokeGroupAccessorsWithNullRequirement() {
		
		String accessRequirementId = null;
		String submitterId = "1";
		List<String> accessorIds = Arrays.asList("1", "2");
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.revokeGroup(userInfo, accessRequirementId, submitterId, accessorIds);
		}).getMessage();
		
		assertEquals("The access requirement id is required.", message);
	}
	
	@Test
	public void testRevokeGroupAccessorsWithNullSubmitter() {
		
		String accessRequirementId = "2";
		String submitterId = null;
		List<String> accessorIds = Arrays.asList("1", "2");
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.revokeGroup(userInfo, accessRequirementId, submitterId, accessorIds);
		}).getMessage();
		
		assertEquals("The submitter id is required.", message);
	}
	
	@Test
	public void testRevokeGroupAccessorsWithNullAccessorIds() {
		
		String accessRequirementId = "2";
		String submitterId = "1";
		List<String> accessorIds = null;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.revokeGroup(userInfo, accessRequirementId, submitterId, accessorIds);
		}).getMessage();
		
		assertEquals("The list of accessor ids is required.", message);
	}
	
	@Test
	public void testValidateWithCertifiedUserRequiredNotSatisfied() {
		HasAccessorRequirement req = new SelfSignAccessRequirement();
		req.setIsCertifiedUserRequired(true);
		req.setIsValidatedProfileRequired(false);
		when(mockgroupMembersDao.areMemberOf(
				AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(),
				accessors))
				.thenReturn(false);
		assertThrows(UserCertificationRequiredException.class, ()-> {
			// call under test
			manager.validateHasAccessorRequirement(req, accessors);
		});
		verifyZeroInteractions(mockVerificationDao);
	}

	@Test
	public void testValidateWithValidatedProfileRequiredNotSatisfied() {
		HasAccessorRequirement req = new SelfSignAccessRequirement();
		req.setIsCertifiedUserRequired(false);
		req.setIsValidatedProfileRequired(true);
		when(mockVerificationDao.haveValidatedProfiles(accessors)).thenReturn(false);
		assertThrows(IllegalArgumentException.class, ()-> {
			// call under test
			manager.validateHasAccessorRequirement(req, accessors);
		});
		verifyZeroInteractions(mockgroupMembersDao);
	}

	@Test
	public void testValidateWithCertifiedUserRequiredAndValidatedProfileSatisfied() {
		HasAccessorRequirement req = new SelfSignAccessRequirement();
		req.setIsCertifiedUserRequired(true);
		req.setIsValidatedProfileRequired(true);
		when(mockgroupMembersDao.areMemberOf(
				AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(),
				accessors))
				.thenReturn(true);
		when(mockVerificationDao.haveValidatedProfiles(accessors)).thenReturn(true);
		// call under test
		manager.validateHasAccessorRequirement(req, accessors);
	}

	@Test
	public void testValidateWithoutRequirements() {
		HasAccessorRequirement req = new SelfSignAccessRequirement();
		req.setIsCertifiedUserRequired(false);
		req.setIsValidatedProfileRequired(false);
		// call under test
		manager.validateHasAccessorRequirement(req, accessors);
		verifyZeroInteractions(mockgroupMembersDao);
		verifyZeroInteractions(mockVerificationDao);
	}
	
}
