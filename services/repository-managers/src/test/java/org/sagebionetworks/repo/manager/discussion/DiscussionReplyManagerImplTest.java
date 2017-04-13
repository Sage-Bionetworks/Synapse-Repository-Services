package org.sagebionetworks.repo.manager.discussion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
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
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.MessageURL;
import org.sagebionetworks.repo.model.discussion.UpdateReplyMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

public class DiscussionReplyManagerImplTest {

	@Mock
	private DiscussionThreadManager mockThreadManager;
	@Mock
	private DiscussionThreadDAO mockThreadDao;
	@Mock
	private DiscussionReplyDAO mockReplyDao;
	@Mock
	private UploadContentToS3DAO mockUploadDao;
	@Mock
	private SubscriptionDAO mockSubscriptionDao;
	@Mock
	private AuthorizationManager mockAuthorizationManager;
	@Mock
	private DiscussionThreadBundle mockThread;
	@Mock
	private IdGenerator mockIdGenerator;
	@Mock
	private TransactionalMessenger mockTransactionalMessenger;

	private DiscussionReplyManager replyManager;
	private UserInfo userInfo = new UserInfo(false /*not admin*/);
	private Long userId = 765L;
	private String threadId = "123";
	private String projectId = "syn456";
	private String forumId = "789";
	private Long replyId = 222L;
	private DiscussionReplyBundle bundle;
	private String messageKey;
	private MessageURL messageUrl = new MessageURL();

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);

		replyManager = new DiscussionReplyManagerImpl();
		ReflectionTestUtils.setField(replyManager, "threadManager", mockThreadManager);
		ReflectionTestUtils.setField(replyManager, "threadDao", mockThreadDao);
		ReflectionTestUtils.setField(replyManager, "replyDao", mockReplyDao);
		ReflectionTestUtils.setField(replyManager, "uploadDao", mockUploadDao);
		ReflectionTestUtils.setField(replyManager, "authorizationManager", mockAuthorizationManager);
		ReflectionTestUtils.setField(replyManager, "idGenerator", mockIdGenerator);
		ReflectionTestUtils.setField(replyManager, "subscriptionDao", mockSubscriptionDao);
		ReflectionTestUtils.setField(replyManager, "transactionalMessenger", mockTransactionalMessenger);

		when(mockThreadManager.getThread(userInfo, threadId)).thenReturn(mockThread);
		when(mockThread.getProjectId()).thenReturn(projectId);
		when(mockThread.getForumId()).thenReturn(forumId);
		when(mockThread.getId()).thenReturn(threadId);
		messageUrl.setMessageUrl("messageUrl");
		when(mockUploadDao.getReplyUrl(Mockito.anyString())).thenReturn(messageUrl);

		when(mockIdGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID)).thenReturn(replyId);

		bundle = new DiscussionReplyBundle();
		bundle.setThreadId(threadId);
		bundle.setForumId(forumId);
		bundle.setProjectId(projectId);
		bundle.setId(replyId.toString());
		bundle.setThreadId(threadId);
		bundle.setEtag("etag");
		bundle.setIsDeleted(false);
		messageKey = forumId + "/" + threadId + "/" + replyId +"/" + UUID.randomUUID().toString();
		bundle.setMessageKey(messageKey);
		userInfo.setId(userId);
		bundle.setCreatedBy(userInfo.getId().toString());
		when(mockReplyDao.getReply(Mockito.anyLong(), Mockito.any(DiscussionFilter.class))).thenReturn(bundle);
		when(mockAuthorizationManager.isAnonymousUser(userInfo)).thenReturn(false);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateReplyWithInvalidUserInfo() throws IOException {
		replyManager.createReply(null, new CreateDiscussionReply());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateReplyWithNullDTO() throws IOException {
		replyManager.createReply(userInfo, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateReplyWithNullThreadId() throws IOException {
		CreateDiscussionReply createReply = new CreateDiscussionReply();
		createReply.setThreadId(null);
		createReply.setMessageMarkdown("messageMarkdown");
		replyManager.createReply(userInfo, createReply);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateReplyWithNullMessage() throws IOException {
		CreateDiscussionReply createReply = new CreateDiscussionReply();
		createReply.setThreadId(threadId);
		createReply.setMessageMarkdown(null);
		replyManager.createReply(userInfo, createReply);
	}

	@Test (expected = UnauthorizedException.class)
	public void testCreateReplyUnauthorized() throws IOException {
		CreateDiscussionReply createReply = new CreateDiscussionReply();
		createReply.setThreadId(threadId);
		createReply.setMessageMarkdown("messageMarkdown");
		when(mockThreadManager.getThread(userInfo, threadId)).thenThrow(new UnauthorizedException());
		replyManager.createReply(userInfo, createReply);
	}

	@Test (expected = UnauthorizedException.class)
	public void testCreateReplyByAnonymous() throws IOException {
		when(mockAuthorizationManager.isAnonymousUser(userInfo)).thenReturn(true);
		CreateDiscussionReply createReply = new CreateDiscussionReply();
		createReply.setThreadId(threadId);
		createReply.setMessageMarkdown("messageMarkdown");
		replyManager.createReply(userInfo, createReply);
	}

	@Test
	public void testCreateReplyAuthorized() throws IOException {
		String message = "messageMarkdown";
		CreateDiscussionReply createReply = new CreateDiscussionReply();
		createReply.setThreadId(threadId);
		createReply.setMessageMarkdown(message);
		when(mockUploadDao.uploadReplyMessage(message, forumId, threadId, replyId.toString()))
				.thenReturn(messageKey);
		when(mockReplyDao.createReply(threadId, replyId.toString(), messageKey, userInfo.getId()))
				.thenReturn(bundle);
		DiscussionReplyBundle reply = replyManager.createReply(userInfo, createReply);
		assertEquals(bundle, reply);
		verify(mockSubscriptionDao).create(eq(userId.toString()), eq(reply.getThreadId()), eq(SubscriptionObjectType.THREAD));
		verify(mockTransactionalMessenger).sendMessageAfterCommit(replyId.toString(), ObjectType.REPLY, bundle.getEtag(), ChangeType.CREATE, userInfo.getId());
		verify(mockThreadDao).insertEntityReference(any(List.class));
		verify(mockTransactionalMessenger).sendMessageAfterCommit(eq(threadId), eq(ObjectType.THREAD), anyString(), eq(ChangeType.UPDATE), eq(userInfo.getId()));
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetReplyUnauthorized() {
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		replyManager.getReply(userInfo, replyId.toString());
	}

	@Test
	public void testGetReplyAuthorized() {
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		DiscussionReplyBundle reply = replyManager.getReply(userInfo, replyId.toString());
		assertEquals(bundle, reply);
	}

	@Test (expected = NotFoundException.class)
	public void testGetDeletedReplyUnauthorized() {
		bundle.setIsDeleted(true);
		when(mockReplyDao.getReply(Mockito.anyLong(), Mockito.any(DiscussionFilter.class))).thenReturn(bundle);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		replyManager.getReply(userInfo, replyId.toString());
	}

	@Test
	public void testGetDeletedReplyAuthorized() {
		bundle.setIsDeleted(true);
		when(mockReplyDao.getReply(Mockito.anyLong(), Mockito.any(DiscussionFilter.class))).thenReturn(bundle);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		assertEquals(bundle, replyManager.getReply(userInfo, replyId.toString()));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateReplyMessageWithNullMessageMarkdown() throws IOException {
		when(mockAuthorizationManager.isUserCreatorOrAdmin(userInfo, replyId.toString())).thenReturn(false);
		UpdateReplyMessage newMessage = new UpdateReplyMessage();
		replyManager.updateReplyMessage(userInfo, replyId.toString(), newMessage);
	}

	@Test (expected = UnauthorizedException.class)
	public void testUpdateReplyMessageUnauthorized() throws IOException {
		when(mockAuthorizationManager.isUserCreatorOrAdmin(userInfo, bundle.getCreatedBy())).thenReturn(false);
		UpdateReplyMessage newMessage = new UpdateReplyMessage();
		newMessage.setMessageMarkdown("messageMarkdown");
		replyManager.updateReplyMessage(userInfo, replyId.toString(), newMessage);
	}

	@Test (expected = NotFoundException.class)
	public void testUpdateReplyMessageForDeletedReply() throws IOException {
		when(mockReplyDao.getReply(replyId, DiscussionFilter.EXCLUDE_DELETED)).thenThrow(new NotFoundException());
		UpdateReplyMessage newMessage = new UpdateReplyMessage();
		newMessage.setMessageMarkdown("messageMarkdown");
		replyManager.updateReplyMessage(userInfo, replyId.toString(), newMessage);
	}

	@Test
	public void testUpdateReplyMessageAuthorized() throws IOException {
		when(mockAuthorizationManager.isUserCreatorOrAdmin(userInfo, bundle.getCreatedBy())).thenReturn(true);
		when(mockUploadDao.uploadReplyMessage("messageMarkdown", forumId, threadId, replyId.toString()))
				.thenReturn(messageKey);
		when(mockReplyDao.updateMessageKey(replyId, messageKey)).thenReturn(bundle);
		UpdateReplyMessage newMessage = new UpdateReplyMessage();
		newMessage.setMessageMarkdown("messageMarkdown");
		replyManager.updateReplyMessage(userInfo, replyId.toString(), newMessage);
		verify(mockReplyDao).updateMessageKey(replyId, messageKey);
		verify(mockThreadDao).insertEntityReference(any(List.class));
	}

	@Test (expected = UnauthorizedException.class)
	public void testMarkReplyAsDeletedUnauthorized() {
		when(mockReplyDao.getProjectId(replyId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		replyManager.markReplyAsDeleted(userInfo, replyId.toString());
		verifyZeroInteractions(mockReplyDao);
	}

	@Test
	public void testMarkReplyAsDeletedAuthorized() {
		when(mockReplyDao.getProjectId(replyId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		replyManager.markReplyAsDeleted(userInfo, replyId.toString());
		verify(mockReplyDao).markReplyAsDeleted(replyId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRepliesForThreadWithNullUserInfo() {
		Mockito.doThrow(new IllegalArgumentException()).when(mockThreadManager).checkPermission(null, threadId, ACCESS_TYPE.READ);
		replyManager.getRepliesForThread(null, threadId, 2L, 0L, DiscussionReplyOrder.CREATED_ON, false, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRepliesForThreadWithNullThreadId() {
		replyManager.getRepliesForThread(userInfo, null, 2L, 0L, DiscussionReplyOrder.CREATED_ON, false, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetRepliesForThreadUnauthorized() {
		Mockito.doThrow(new UnauthorizedException()).when(mockThreadManager).checkPermission(userInfo, threadId, ACCESS_TYPE.READ);
		replyManager.getRepliesForThread(userInfo, threadId, 2L, 0L, DiscussionReplyOrder.CREATED_ON, true, DiscussionFilter.NO_FILTER);
	}

	@Test
	public void testGetRepliesForThread() {
		PaginatedResults<DiscussionReplyBundle> replies = PaginatedResults.createWithLimitAndOffset(Arrays.asList(bundle), 2L, 0L);
		when(mockReplyDao.getRepliesForThread(Mockito.anyLong(), Mockito.anyLong(),
				Mockito.anyLong(), (DiscussionReplyOrder) Mockito.any(), Mockito.anyBoolean(),
				Mockito.any(DiscussionFilter.class))).thenReturn(Arrays.asList(bundle));
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		assertEquals(replies, replyManager.getRepliesForThread(userInfo, threadId, 2L, 0L, DiscussionReplyOrder.CREATED_ON, true, DiscussionFilter.NO_FILTER));
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetReplyUrlUnauthorized() {
		when(mockReplyDao.getProjectId(replyId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		replyManager.getMessageUrl(userInfo, messageKey);
	}

	@Test
	public void testGetReplyUrlAuthorized() {
		when(mockReplyDao.getProjectId(replyId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		MessageURL url = replyManager.getMessageUrl(userInfo, messageKey);
		assertNotNull(url);
		assertNotNull(url.getMessageUrl());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetReplyCountForThreadWithNullUserInfo() {
		Mockito.doThrow(new IllegalArgumentException()).when(mockThreadManager).checkPermission(null, threadId, ACCESS_TYPE.READ);
		replyManager.getReplyCountForThread(null, threadId, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetReplyCountForThreadWithNullThreadId() {
		replyManager.getReplyCountForThread(userInfo, null, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetReplyCountForThreadUnauthorized() {
		Mockito.doThrow(new UnauthorizedException()).when(mockThreadManager).checkPermission(userInfo, threadId, ACCESS_TYPE.READ);
		replyManager.getReplyCountForThread(userInfo, threadId, DiscussionFilter.NO_FILTER);
	}

	@Test
	public void testGetReplyCountForThread() {
		Long count = 3L;
		when(mockReplyDao.getReplyCount(Mockito.anyLong(), Mockito.any(DiscussionFilter.class))).thenReturn(count);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		assertEquals((Long)3L, replyManager.getReplyCountForThread(userInfo, threadId, DiscussionFilter.NO_FILTER).getCount());
	}

}
