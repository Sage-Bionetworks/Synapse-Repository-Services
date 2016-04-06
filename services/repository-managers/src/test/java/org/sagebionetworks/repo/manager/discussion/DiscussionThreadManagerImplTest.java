package org.sagebionetworks.repo.manager.discussion;

import static org.sagebionetworks.repo.manager.discussion.DiscussionThreadManagerImpl.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.MessageURL;
import org.sagebionetworks.repo.model.discussion.ThreadCount;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

public class DiscussionThreadManagerImplTest {
	@Mock
	private DiscussionThreadDAO mockThreadDao;
	@Mock
	private DiscussionReplyDAO mockReplyDao;
	@Mock
	private ForumDAO mockForumDao;
	@Mock
	private UploadContentToS3DAO mockUploadDao;
	@Mock
	private SubscriptionDAO mockSubscriptionDao;
	@Mock
	private AuthorizationManager mockAuthorizationManager;
	@Mock
	private IdGenerator mockIdGenerator;
	@Mock
	private TransactionalMessenger mockTransactionalMessenger;

	private DiscussionThreadManager threadManager;
	private UserInfo userInfo = new UserInfo(false /*not admin*/);
	private CreateDiscussionThread createDto;
	private DiscussionThreadBundle dto;
	private Long forumId = 1L;
	private String projectId = "syn123";
	private Long userId = 2L;
	private Long threadId = 3L;
	private Forum forum;
	private String messageKey = "1/3/messageKey";
	private MessageURL messageUrl = new MessageURL();
	private UpdateThreadTitle newTitle = new UpdateThreadTitle();
	private UpdateThreadMessage newMessage = new UpdateThreadMessage();

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);

		threadManager = new DiscussionThreadManagerImpl();
		ReflectionTestUtils.setField(threadManager, "threadDao", mockThreadDao);
		ReflectionTestUtils.setField(threadManager, "forumDao", mockForumDao);
		ReflectionTestUtils.setField(threadManager, "uploadDao", mockUploadDao);
		ReflectionTestUtils.setField(threadManager, "authorizationManager", mockAuthorizationManager);
		ReflectionTestUtils.setField(threadManager, "idGenerator", mockIdGenerator);
		ReflectionTestUtils.setField(threadManager, "replyDao", mockReplyDao);
		ReflectionTestUtils.setField(threadManager, "subscriptionDao", mockSubscriptionDao);
		ReflectionTestUtils.setField(threadManager, "transactionalMessenger", mockTransactionalMessenger);

		createDto = new CreateDiscussionThread();
		createDto.setForumId(forumId.toString());
		createDto.setTitle("title");
		createDto.setMessageMarkdown("messageMarkdown");
		forum = new Forum();
		forum.setId(forumId.toString());
		forum.setProjectId(projectId);
		dto = new DiscussionThreadBundle();
		dto.setProjectId(projectId);
		dto.setMessageKey(messageKey);
		dto.setId(threadId.toString());
		dto.setEtag("etag");
		dto.setForumId(forumId.toString());
		userInfo.setId(userId);

		newTitle.setTitle("newTitle");
		newMessage.setMessageMarkdown("newMessageMarkdown");

		Mockito.when(mockForumDao.getForum(Long.parseLong(createDto.getForumId()))).thenReturn(forum);
		Mockito.when(mockThreadDao.getThread(threadId, DiscussionFilter.NO_FILTER)).thenReturn(dto);
		Mockito.when(mockThreadDao.getThread(threadId, DiscussionFilter.EXCLUDE_DELETED)).thenReturn(dto);
		Mockito.when(mockIdGenerator.generateNewId(TYPE.DISCUSSION_THREAD_ID)).thenReturn(threadId);
		messageUrl.setMessageUrl("messageUrl");
		Mockito.when(mockUploadDao.getThreadUrl(messageKey)).thenReturn(messageUrl);
		Mockito.when(mockReplyDao.getReplyCount(Mockito.anyLong(), Mockito.any(DiscussionFilter.class))).thenReturn(0L);
		Mockito.when(mockAuthorizationManager.isAnonymousUser(userInfo)).thenReturn(false);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullDTO() throws Exception {
		threadManager.createThread(userInfo, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullForumId() throws Exception {
		createDto.setForumId(null);
		threadManager.createThread(userInfo, createDto);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullTitle() throws Exception {
		createDto.setTitle(null);
		threadManager.createThread(userInfo, createDto);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullMessage() throws Exception {
		createDto.setMessageMarkdown(null);
		threadManager.createThread(userInfo, createDto);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNulluserInfo() throws Exception {
		threadManager.createThread(null, new CreateDiscussionThread());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithLongTitle() throws Exception {
		CreateDiscussionThread createThread = new CreateDiscussionThread();
		createThread.setTitle(RandomStringUtils.randomAlphanumeric(MAX_TITLE_LENGTH+1));
		threadManager.createThread(null, createThread);
	}

	@Test (expected = UnauthorizedException.class)
	public void testCreateAccessDenied() throws Exception {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		threadManager.createThread(userInfo, createDto);
	}

	@Test (expected = UnauthorizedException.class)
	public void testCreateByAnonymous() throws Exception {
		Mockito.when(mockAuthorizationManager.isAnonymousUser(userInfo)).thenReturn(true);
		threadManager.createThread(userInfo, createDto);
	}

	@Test
	public void testCreateAuthorized() throws Exception {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		Mockito.when(mockUploadDao.uploadThreadMessage(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn(messageKey);
		Mockito.when(mockThreadDao.createThread(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong()))
				.thenReturn(dto);
		DiscussionThreadBundle createdThread = threadManager.createThread(userInfo, createDto);
		assertNotNull(createdThread);
		assertEquals(createdThread, dto);
		Mockito.verify(mockReplyDao).getReplyCount(Long.parseLong(createdThread.getId()), DiscussionFilter.NO_FILTER);
		Mockito.verify(mockSubscriptionDao).create(userId.toString(), dto.getId(), SubscriptionObjectType.THREAD);
		Mockito.verify(mockTransactionalMessenger).sendMessageAfterCommit(createdThread.getId(), ObjectType.THREAD, dto.getEtag(), ChangeType.CREATE, userInfo.getId());
		Mockito.verify(mockSubscriptionDao).subscribeForumSubscriberToThread(dto.getForumId(), dto.getId());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetThreadWithNullThreadId() {
		threadManager.getThread(userInfo, null);
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetThreadUnauthorized() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		threadManager.getThread(userInfo, threadId.toString());
	}

	@Test
	public void testGetThreadAuthorized() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		assertEquals(dto, threadManager.getThread(userInfo, threadId.toString()));
		Mockito.verify(mockThreadDao).updateThreadView(Mockito.anyLong(), Mockito.anyLong());
		Mockito.verify(mockReplyDao).getReplyCount(threadId, DiscussionFilter.EXCLUDE_DELETED);
	}

	@Test (expected = NotFoundException.class)
	public void testGetThreadDeleted() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		Mockito.when(mockThreadDao.getThread(threadId, DiscussionFilter.EXCLUDE_DELETED)).thenThrow(new NotFoundException());
		threadManager.getThread(userInfo, threadId.toString());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateTitleWithNullTitle() {
		threadManager.updateTitle(userInfo, threadId.toString(), null);
	}

	@Test (expected = UnauthorizedException.class)
	public void testUpdateTitleUnauthorized() {
		Mockito.when(mockAuthorizationManager.isUserCreatorOrAdmin(Mockito.eq(userInfo), Mockito.anyString()))
				.thenReturn(false);
		threadManager.updateTitle(userInfo, threadId.toString(), newTitle);
	}

	@Test
	public void testUpdateTitleAuthorized() {
		Mockito.when(mockAuthorizationManager.isUserCreatorOrAdmin(Mockito.eq(userInfo), Mockito.anyString()))
				.thenReturn(true);
		Mockito.when(mockThreadDao.updateTitle(Mockito.anyLong(), Mockito.anyString())).thenReturn(dto);

		assertEquals(dto, threadManager.updateTitle(userInfo, threadId.toString(), newTitle));
		Mockito.verify(mockReplyDao).getReplyCount(threadId, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateMessageWithNullMessage() throws Exception {
		threadManager.updateMessage(userInfo, threadId.toString(), null);
	}

	@Test (expected = UnauthorizedException.class)
	public void testUpdateMessageUnauthorized() throws Exception {
		Mockito.when(mockAuthorizationManager.isUserCreatorOrAdmin(Mockito.eq(userInfo), Mockito.anyString()))
				.thenReturn(false);
		threadManager.updateMessage(userInfo, threadId.toString(), newMessage );
	}

	@Test
	public void testUpdateMessageAuthorized() throws Exception {
		Mockito.when(mockAuthorizationManager.isUserCreatorOrAdmin(Mockito.eq(userInfo), Mockito.anyString()))
				.thenReturn(true);
		Mockito.when(mockUploadDao.uploadThreadMessage(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn("newMessage");
		Mockito.when(mockThreadDao.updateMessageKey(Mockito.anyLong(), Mockito.anyString())).thenReturn(dto);
		assertEquals(dto, threadManager.updateMessage(userInfo, threadId.toString(), newMessage));
		Mockito.verify(mockReplyDao).getReplyCount(threadId, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = UnauthorizedException.class)
	public void testDeleteUnauthorized() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		threadManager.markThreadAsDeleted(userInfo, threadId.toString());
	}

	@Test
	public void testDeleteAuthorized() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		threadManager.markThreadAsDeleted(userInfo, threadId.toString());
		Mockito.verify(mockThreadDao).markThreadAsDeleted(threadId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetThreadsForForumWithNullForumId() {
		threadManager.getThreadsForForum(userInfo, null, 2L, 0L, DiscussionThreadOrder.LAST_ACTIVITY, null, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetThreadsForForumUnauthorizedWithNoFilter() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		threadManager.getThreadsForForum(userInfo, forumId.toString(), 2L, 0L, DiscussionThreadOrder.LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetThreadsForForumUnauthorized() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		threadManager.getThreadsForForum(userInfo, forumId.toString(), 2L, 0L, DiscussionThreadOrder.LAST_ACTIVITY, true, DiscussionFilter.EXCLUDE_DELETED);
	}

	@Test
	public void testGetThreadsForForum() {
		PaginatedResults<DiscussionThreadBundle> threads = new PaginatedResults<DiscussionThreadBundle>();
		threads.setResults(Arrays.asList(dto));
		Mockito.when(mockThreadDao.getThreads(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), (DiscussionThreadOrder) Mockito.any(), Mockito.anyBoolean(), Mockito.any(DiscussionFilter.class)))
				.thenReturn(threads);
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		assertEquals(threads, threadManager.getThreadsForForum(userInfo, forumId.toString(), 2L, 0L, DiscussionThreadOrder.LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER));
		Mockito.verify(mockReplyDao).getReplyCount(threadId, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetThreadURLUnauthorized() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		threadManager.getMessageUrl(userInfo, messageKey);
	}

	@Test
	public void testGetThreadURLAuthorized() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		MessageURL url = threadManager.getMessageUrl(userInfo, messageKey);
		assertNotNull(url);
		assertNotNull(url.getMessageUrl());
		Mockito.verify(mockThreadDao).updateThreadView(threadId, userId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetThreadCountForForumWithNullForumId() {
		threadManager.getThreadCountForForum(userInfo, null, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetThreadCountForForumUnauthorizedWithNoFilter() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		threadManager.getThreadCountForForum(userInfo, forumId.toString(), DiscussionFilter.NO_FILTER);
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetThreadCountForForumUnauthorized() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		threadManager.getThreadCountForForum(userInfo, forumId.toString(), DiscussionFilter.EXCLUDE_DELETED);
	}

	@Test
	public void testGetThreadCountForForum() {
		Long count = 3L;
		Mockito.when(mockThreadDao.getThreadCount(Mockito.anyLong(), Mockito.any(DiscussionFilter.class)))
				.thenReturn(count);
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		ThreadCount tc = threadManager.getThreadCountForForum(userInfo, forumId.toString(), DiscussionFilter.NO_FILTER);
		assertNotNull(tc);
		assertEquals(count, tc.getCount());
	}
}
