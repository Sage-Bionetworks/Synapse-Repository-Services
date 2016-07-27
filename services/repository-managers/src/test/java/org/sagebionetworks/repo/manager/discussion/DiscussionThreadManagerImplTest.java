package org.sagebionetworks.repo.manager.discussion;

import static org.sagebionetworks.repo.manager.discussion.DiscussionThreadManagerImpl.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import static org.mockito.Mockito.*;

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
		dto.setIsDeleted(false);
		userInfo.setId(userId);

		newTitle.setTitle("newTitle");
		newMessage.setMessageMarkdown("newMessageMarkdown");

		when(mockForumDao.getForum(Long.parseLong(createDto.getForumId()))).thenReturn(forum);
		when(mockThreadDao.getThread(threadId, DiscussionFilter.NO_FILTER)).thenReturn(dto);
		when(mockThreadDao.getThread(threadId, DiscussionFilter.EXCLUDE_DELETED)).thenReturn(dto);
		when(mockIdGenerator.generateNewId(TYPE.DISCUSSION_THREAD_ID)).thenReturn(threadId);
		messageUrl.setMessageUrl("messageUrl");
		when(mockUploadDao.getThreadUrl(messageKey)).thenReturn(messageUrl);
		when(mockReplyDao.getReplyCount(Mockito.anyLong(), Mockito.any(DiscussionFilter.class))).thenReturn(0L);
		when(mockAuthorizationManager.isAnonymousUser(userInfo)).thenReturn(false);
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
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		threadManager.createThread(userInfo, createDto);
	}

	@Test (expected = UnauthorizedException.class)
	public void testCreateByAnonymous() throws Exception {
		when(mockAuthorizationManager.isAnonymousUser(userInfo)).thenReturn(true);
		threadManager.createThread(userInfo, createDto);
	}

	@Test
	public void testCreateAuthorized() throws Exception {
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockUploadDao.uploadThreadMessage(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn(messageKey);
		when(mockThreadDao.createThread(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong()))
				.thenReturn(dto);
		DiscussionThreadBundle createdThread = threadManager.createThread(userInfo, createDto);
		assertNotNull(createdThread);
		assertEquals(createdThread, dto);
		verify(mockReplyDao).getReplyCount(Long.parseLong(createdThread.getId()), DiscussionFilter.EXCLUDE_DELETED);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(createdThread.getId(), ObjectType.THREAD, dto.getEtag(), ChangeType.CREATE, userInfo.getId());
		verify(mockSubscriptionDao).create(eq(userId.toString()), eq(dto.getId()), eq(SubscriptionObjectType.THREAD));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetThreadWithNullThreadId() {
		threadManager.getThread(userInfo, null);
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetThreadUnauthorized() {
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		threadManager.getThread(userInfo, threadId.toString());
	}

	@Test
	public void testGetThreadAuthorized() {
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		assertEquals(dto, threadManager.getThread(userInfo, threadId.toString()));
		verify(mockThreadDao).updateThreadView(Mockito.anyLong(), Mockito.anyLong());
		verify(mockReplyDao).getReplyCount(threadId, DiscussionFilter.EXCLUDE_DELETED);
	}

	@Test
	public void testGetDeletedThreadAuthorized() {
		dto.setIsDeleted(true);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		assertEquals(dto, threadManager.getThread(userInfo, threadId.toString()));
		verify(mockThreadDao).updateThreadView(Mockito.anyLong(), Mockito.anyLong());
		verify(mockReplyDao).getReplyCount(threadId, DiscussionFilter.EXCLUDE_DELETED);
	}

	@Test (expected = NotFoundException.class)
	public void testGetDeletedThreadUnauthorized() {
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockThreadDao.getThread(threadId, DiscussionFilter.NO_FILTER)).thenThrow(new NotFoundException());
		threadManager.getThread(userInfo, threadId.toString());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCheckPermissionWithNullUserInfo() {
		threadManager.checkPermission(null, threadId.toString(), ACCESS_TYPE.READ);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCheckPermissionWithNullThreadId() {
		threadManager.checkPermission(userInfo, null, ACCESS_TYPE.READ);
	}

	@Test (expected = UnauthorizedException.class)
	public void testCheckPermissionUnauthorized() {
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		threadManager.checkPermission(userInfo, threadId.toString(), ACCESS_TYPE.READ);
	}

	@Test
	public void testCheckPermissionAuthorized() {
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		threadManager.checkPermission(userInfo, threadId.toString(), ACCESS_TYPE.READ);
		verify(mockThreadDao, Mockito.never()).updateThreadView(Mockito.anyLong(), Mockito.anyLong());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateTitleWithNullTitle() {
		threadManager.updateTitle(userInfo, threadId.toString(), null);
	}

	@Test (expected = UnauthorizedException.class)
	public void testUpdateTitleUnauthorized() {
		when(mockAuthorizationManager.isUserCreatorOrAdmin(Mockito.eq(userInfo), Mockito.anyString()))
				.thenReturn(false);
		threadManager.updateTitle(userInfo, threadId.toString(), newTitle);
	}

	@Test (expected = NotFoundException.class)
	public void testUpdateTitleDeletedThread() {
		when(mockThreadDao.getAuthorForUpdate(threadId.toString())).thenThrow(new NotFoundException());
		threadManager.updateTitle(userInfo, threadId.toString(), newTitle);
	}

	@Test
	public void testUpdateTitleAuthorized() {
		when(mockAuthorizationManager.isUserCreatorOrAdmin(Mockito.eq(userInfo), Mockito.anyString()))
				.thenReturn(true);
		when(mockThreadDao.updateTitle(Mockito.anyLong(), Mockito.anyString())).thenReturn(dto);

		assertEquals(dto, threadManager.updateTitle(userInfo, threadId.toString(), newTitle));
		verify(mockReplyDao).getReplyCount(threadId, DiscussionFilter.EXCLUDE_DELETED);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateMessageWithNullMessage() throws Exception {
		threadManager.updateMessage(userInfo, threadId.toString(), null);
	}

	@Test (expected = UnauthorizedException.class)
	public void testUpdateMessageUnauthorized() throws Exception {
		when(mockAuthorizationManager.isUserCreatorOrAdmin(Mockito.eq(userInfo), Mockito.anyString()))
				.thenReturn(false);
		threadManager.updateMessage(userInfo, threadId.toString(), newMessage );
	}

	@Test (expected = NotFoundException.class)
	public void testUpdateMessageForDeletedThread() throws Exception {
		when(mockThreadDao.getThread(threadId, DiscussionFilter.EXCLUDE_DELETED)).thenThrow(new NotFoundException());
		threadManager.updateMessage(userInfo, threadId.toString(), newMessage );
	}

	@Test
	public void testUpdateMessageAuthorized() throws Exception {
		when(mockAuthorizationManager.isUserCreatorOrAdmin(Mockito.eq(userInfo), Mockito.anyString()))
				.thenReturn(true);
		when(mockUploadDao.uploadThreadMessage(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn("newMessage");
		when(mockThreadDao.updateMessageKey(Mockito.anyLong(), Mockito.anyString())).thenReturn(dto);
		assertEquals(dto, threadManager.updateMessage(userInfo, threadId.toString(), newMessage));
		verify(mockReplyDao).getReplyCount(threadId, DiscussionFilter.EXCLUDE_DELETED);
	}

	@Test (expected = UnauthorizedException.class)
	public void testDeleteUnauthorized() {
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		threadManager.markThreadAsDeleted(userInfo, threadId.toString());
	}

	@Test
	public void testDeleteAuthorized() {
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		threadManager.markThreadAsDeleted(userInfo, threadId.toString());
		verify(mockThreadDao).markThreadAsDeleted(threadId);
	}

	@Test (expected = UnauthorizedException.class)
	public void testPinThreadUnauthorized() {
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		threadManager.pinThread(userInfo, threadId.toString());
	}

	@Test (expected = NotFoundException.class)
	public void testPinNotExistingThread() {
		when(mockThreadDao.isThreadDeleted(threadId.toString())).thenThrow(new NotFoundException());
		threadManager.pinThread(userInfo, threadId.toString());
	}

	@Test (expected = NotFoundException.class)
	public void testPinDeletedThread() {
		when(mockThreadDao.isThreadDeleted(threadId.toString())).thenReturn(true);
		threadManager.pinThread(userInfo, threadId.toString());
	}

	@Test
	public void testPinThreadAuthorized() {
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		threadManager.pinThread(userInfo, threadId.toString());
		verify(mockThreadDao).pinThread(threadId);
	}

	@Test (expected = UnauthorizedException.class)
	public void testUnpinThreadUnauthorized() {
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		threadManager.unpinThread(userInfo, threadId.toString());
	}

	@Test (expected = NotFoundException.class)
	public void testUnpinNonExistingThread() {
		when(mockThreadDao.isThreadDeleted(threadId.toString())).thenThrow(new NotFoundException());
		threadManager.unpinThread(userInfo, threadId.toString());
	}

	@Test (expected = NotFoundException.class)
	public void testUnpinDeletedThread() {
		when(mockThreadDao.isThreadDeleted(threadId.toString())).thenReturn(true);
		threadManager.unpinThread(userInfo, threadId.toString());
	}

	@Test
	public void testUnpinThreadAuthorized() {
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		threadManager.unpinThread(userInfo, threadId.toString());
		verify(mockThreadDao).unpinThread(threadId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetThreadsForForumWithNullForumId() {
		threadManager.getThreadsForForum(userInfo, null, 2L, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, null, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetThreadsForForumUnauthorizedWithNoFilter() {
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		threadManager.getThreadsForForum(userInfo, forumId.toString(), 2L, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetThreadsForForumUnauthorized() {
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		threadManager.getThreadsForForum(userInfo, forumId.toString(), 2L, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.EXCLUDE_DELETED);
	}

	@Test
	public void testGetThreadsForForum() {
		PaginatedResults<DiscussionThreadBundle> threads = new PaginatedResults<DiscussionThreadBundle>();
		threads.setResults(Arrays.asList(dto));
		when(mockThreadDao.getThreads(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), (DiscussionThreadOrder) Mockito.any(), Mockito.anyBoolean(), Mockito.any(DiscussionFilter.class)))
				.thenReturn(threads);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		assertEquals(threads, threadManager.getThreadsForForum(userInfo, forumId.toString(), 2L, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER));
		verify(mockReplyDao).getReplyCount(threadId, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetThreadURLUnauthorized() {
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		threadManager.getMessageUrl(userInfo, messageKey);
	}

	@Test
	public void testGetThreadURLAuthorized() {
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		MessageURL url = threadManager.getMessageUrl(userInfo, messageKey);
		assertNotNull(url);
		assertNotNull(url.getMessageUrl());
		verify(mockThreadDao).updateThreadView(threadId, userId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetThreadCountForForumWithNullForumId() {
		threadManager.getThreadCountForForum(userInfo, null, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetThreadCountForForumUnauthorizedWithNoFilter() {
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		threadManager.getThreadCountForForum(userInfo, forumId.toString(), DiscussionFilter.NO_FILTER);
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetThreadCountForForumUnauthorized() {
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		threadManager.getThreadCountForForum(userInfo, forumId.toString(), DiscussionFilter.EXCLUDE_DELETED);
	}

	@Test
	public void testGetThreadCountForForum() {
		Long count = 3L;
		when(mockThreadDao.getThreadCountForForum(Mockito.anyLong(), Mockito.any(DiscussionFilter.class)))
				.thenReturn(count);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		ThreadCount tc = threadManager.getThreadCountForForum(userInfo, forumId.toString(), DiscussionFilter.NO_FILTER);
		assertNotNull(tc);
		assertEquals(count, tc.getCount());
	}

}
