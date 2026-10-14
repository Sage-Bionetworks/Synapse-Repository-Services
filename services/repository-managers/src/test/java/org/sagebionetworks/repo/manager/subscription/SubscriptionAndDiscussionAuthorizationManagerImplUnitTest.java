package org.sagebionetworks.repo.manager.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.dataaccess.DataAccessAuthorizationManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dbo.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.ForumObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;

@ExtendWith(MockitoExtension.class)
public class SubscriptionAndDiscussionAuthorizationManagerImplUnitTest {

	@Mock
	private EntityAuthorizationManager mockEntityAuthorizationManager;
	@Mock
	private ForumDAO mockForumDao;
	@Mock
	private DiscussionThreadDAO mockThreadDao;
	@Mock
	private org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO mockDataAccessSubmissionDao;
	@Mock
	private DataAccessAuthorizationManager mockDataAccessAuthManager;
	@InjectMocks
	private SubscriptionAndDiscussionAuthorizationManagerImpl subscriptionAuthorizationManager;

	private UserInfo userInfo;
	private UserInfo adminUser;
	private String forumId;
	private String threadId;
	private String projectId;
	private String submissionId;
	private Forum entityForum;
	private Forum arForum;
	private DiscussionThreadBundle entityThreadBundle;
	private DiscussionThreadBundle arThreadBundle;

	@BeforeEach
	public void setUp() {
		userInfo = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		adminUser = new UserInfo(true, 2L, AuthorizationConstants.DEFAULT_REALM_ID);

		forumId = "100";
		threadId = "200";
		projectId = "syn123";
		submissionId = "300";

		entityForum = new Forum();
		entityForum.setId(forumId);
		entityForum.setObjectId(projectId);
		entityForum.setObjectType(ForumObjectType.ENTITY);

		arForum = new Forum();
		arForum.setId(forumId);
		arForum.setObjectId("456");
		arForum.setObjectType(ForumObjectType.ACCESS_REQUIREMENT);

		entityThreadBundle = new DiscussionThreadBundle();
		entityThreadBundle.setId(threadId);
		entityThreadBundle.setForumId(forumId);
		entityThreadBundle.setObjectId(projectId);
		entityThreadBundle.setObjectType(ForumObjectType.ENTITY);

		arThreadBundle = new DiscussionThreadBundle();
		arThreadBundle.setId(threadId);
		arThreadBundle.setForumId(forumId);
		arThreadBundle.setObjectId("456");
		arThreadBundle.setObjectType(ForumObjectType.ACCESS_REQUIREMENT);
	}

	@Test
	public void testCanSubscribeWithAnonymousUser() {
		UserInfo anonUser = new UserInfo(false, BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId(), AuthorizationConstants.DEFAULT_REALM_ID);
		anonUser.setRealmAnonymousUserId(anonUser.getId());
		// call under test
		assertFalse(subscriptionAuthorizationManager.canSubscribe(anonUser, forumId, SubscriptionObjectType.FORUM).isAuthorized());
	}

	@Test
	public void testCanSubscribeForumEntityAuthorized() {
		when(mockForumDao.getForum(Long.parseLong(forumId))).thenReturn(entityForum);
		when(mockEntityAuthorizationManager.hasAccess(userInfo, projectId, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		// call under test
		assertEquals(AuthorizationStatus.authorized(),
				subscriptionAuthorizationManager.canSubscribe(userInfo, forumId, SubscriptionObjectType.FORUM));
	}

	@Test
	public void testCanSubscribeForumEntityUnauthorized() {
		when(mockForumDao.getForum(Long.parseLong(forumId))).thenReturn(entityForum);
		when(mockEntityAuthorizationManager.hasAccess(userInfo, projectId, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.accessDenied("no access"));
		// call under test
		assertFalse(subscriptionAuthorizationManager.canSubscribe(userInfo, forumId, SubscriptionObjectType.FORUM).isAuthorized());
	}

	@Test
	public void testCanSubscribeForumARAuthorized() {
		when(mockForumDao.getForum(Long.parseLong(forumId))).thenReturn(arForum);
		when(mockDataAccessAuthManager.canReviewAccessRequirementSubmissions(userInfo, "456"))
				.thenReturn(AuthorizationStatus.authorized());
		// call under test
		assertEquals(AuthorizationStatus.authorized(),
				subscriptionAuthorizationManager.canSubscribe(userInfo, forumId, SubscriptionObjectType.FORUM));
	}

	@Test
	public void testCanSubscribeForumARUnauthorized() {
		when(mockForumDao.getForum(Long.parseLong(forumId))).thenReturn(arForum);
		when(mockDataAccessAuthManager.canReviewAccessRequirementSubmissions(userInfo, "456"))
				.thenReturn(AuthorizationStatus.accessDenied("no permission"));
		// call under test
		assertFalse(subscriptionAuthorizationManager.canSubscribe(userInfo, forumId, SubscriptionObjectType.FORUM).isAuthorized());
	}

	@Test
	public void testCanSubscribeThreadEntityAuthorized() {
		when(mockThreadDao.getThread(Long.parseLong(threadId), DiscussionFilter.NO_FILTER)).thenReturn(entityThreadBundle);
		when(mockEntityAuthorizationManager.hasAccess(userInfo, projectId, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		// call under test
		assertEquals(AuthorizationStatus.authorized(),
				subscriptionAuthorizationManager.canSubscribe(userInfo, threadId, SubscriptionObjectType.THREAD));
	}

	@Test
	public void testCanSubscribeThreadEntityUnauthorized() {
		when(mockThreadDao.getThread(Long.parseLong(threadId), DiscussionFilter.NO_FILTER)).thenReturn(entityThreadBundle);
		when(mockEntityAuthorizationManager.hasAccess(userInfo, projectId, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.accessDenied("no access"));
		// call under test
		assertFalse(subscriptionAuthorizationManager.canSubscribe(userInfo, threadId, SubscriptionObjectType.THREAD).isAuthorized());
	}

	@Test
	public void testCanSubscribeThreadARAuthorized() {
		when(mockThreadDao.getThread(Long.parseLong(threadId), DiscussionFilter.NO_FILTER)).thenReturn(arThreadBundle);
		when(mockDataAccessAuthManager.canReviewAccessRequirementSubmissions(userInfo, "456"))
				.thenReturn(AuthorizationStatus.authorized());
		// call under test
		assertEquals(AuthorizationStatus.authorized(),
				subscriptionAuthorizationManager.canSubscribe(userInfo, threadId, SubscriptionObjectType.THREAD));
	}

	@Test
	public void testCanSubscribeThreadARUnauthorized() {
		when(mockThreadDao.getThread(Long.parseLong(threadId), DiscussionFilter.NO_FILTER)).thenReturn(arThreadBundle);
		when(mockDataAccessAuthManager.canReviewAccessRequirementSubmissions(userInfo, "456"))
				.thenReturn(AuthorizationStatus.accessDenied("no permission"));
		// call under test
		assertFalse(subscriptionAuthorizationManager.canSubscribe(userInfo, threadId, SubscriptionObjectType.THREAD).isAuthorized());
	}

	@Test
	public void testCanSubscribeDataAccessSubmissionUnauthorized() {
		// call under test
		assertFalse(subscriptionAuthorizationManager.canSubscribe(userInfo, submissionId, SubscriptionObjectType.DATA_ACCESS_SUBMISSION).isAuthorized());
	}

	@Test
	public void testCanSubscribeDataAccessSubmissionAdminAuthorized() {
		// adminUser is created with isAdmin=true, so isACTTeamMemberOrAdmin returns true
		// call under test
		assertTrue(subscriptionAuthorizationManager.canSubscribe(adminUser, submissionId, SubscriptionObjectType.DATA_ACCESS_SUBMISSION).isAuthorized());
	}

	@Test
	public void testCanSubscribeDataAccessSubmissionStatusUnauthorized() {
		when(mockDataAccessSubmissionDao.isAccessor(submissionId, userInfo.getId().toString())).thenReturn(false);
		// call under test
		assertFalse(subscriptionAuthorizationManager.canSubscribe(userInfo, submissionId, SubscriptionObjectType.DATA_ACCESS_SUBMISSION_STATUS).isAuthorized());
	}

	@Test
	public void testCanSubscribeDataAccessSubmissionStatusAuthorized() {
		when(mockDataAccessSubmissionDao.isAccessor(submissionId, userInfo.getId().toString())).thenReturn(true);
		// call under test
		assertTrue(subscriptionAuthorizationManager.canSubscribe(userInfo, submissionId, SubscriptionObjectType.DATA_ACCESS_SUBMISSION_STATUS).isAuthorized());
	}
}
