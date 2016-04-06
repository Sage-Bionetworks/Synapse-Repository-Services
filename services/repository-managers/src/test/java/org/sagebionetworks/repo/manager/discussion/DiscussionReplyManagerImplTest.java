package org.sagebionetworks.repo.manager.discussion;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

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
		ReflectionTestUtils.setField(replyManager, "replyDao", mockReplyDao);
		ReflectionTestUtils.setField(replyManager, "uploadDao", mockUploadDao);
		ReflectionTestUtils.setField(replyManager, "authorizationManager", mockAuthorizationManager);
		ReflectionTestUtils.setField(replyManager, "idGenerator", mockIdGenerator);
		ReflectionTestUtils.setField(replyManager, "subscriptionDao", mockSubscriptionDao);
		ReflectionTestUtils.setField(replyManager, "transactionalMessenger", mockTransactionalMessenger);

		Mockito.when(mockThreadManager.getThread(userInfo, threadId)).thenReturn(mockThread);
		Mockito.when(mockThread.getProjectId()).thenReturn(projectId);
		Mockito.when(mockThread.getForumId()).thenReturn(forumId);
		Mockito.when(mockThread.getId()).thenReturn(threadId);
		messageUrl.setMessageUrl("messageUrl");
		Mockito.when(mockUploadDao.getReplyUrl(Mockito.anyString())).thenReturn(messageUrl);

		Mockito.when(mockIdGenerator.generateNewId(TYPE.DISCUSSION_REPLY_ID)).thenReturn(replyId);

		bundle = new DiscussionReplyBundle();
		bundle.setThreadId(threadId);
		bundle.setForumId(forumId);
		bundle.setProjectId(projectId);
		bundle.setId(replyId.toString());
		bundle.setThreadId(threadId);
		bundle.setEtag("etag");
		messageKey = forumId + "/" + threadId + "/" + replyId +"/" + UUID.randomUUID().toString();
		bundle.setMessageKey(messageKey);
		userInfo.setId(765L);
		bundle.setCreatedBy(userInfo.getId().toString());
		Mockito.when(mockReplyDao.getReply(Mockito.anyLong(), Mockito.any(DiscussionFilter.class))).thenReturn(bundle);
		Mockito.when(mockAuthorizationManager.isAnonymousUser(userInfo)).thenReturn(false);
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
		Mockito.when(mockThreadManager.getThread(userInfo, threadId)).thenThrow(new UnauthorizedException());
		replyManager.createReply(userInfo, createReply);
	}

	@Test (expected = UnauthorizedException.class)
	public void testCreateReplyByAnonymous() throws IOException {
		Mockito.when(mockAuthorizationManager.isAnonymousUser(userInfo)).thenReturn(true);
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
		Mockito.when(mockUploadDao.uploadReplyMessage(message, forumId, threadId, replyId.toString()))
				.thenReturn(messageKey);
		Mockito.when(mockReplyDao.createReply(threadId, replyId.toString(), messageKey, userInfo.getId()))
				.thenReturn(bundle);
		DiscussionReplyBundle reply = replyManager.createReply(userInfo, createReply);
		assertEquals(bundle, reply);
		Mockito.verify(mockSubscriptionDao).create(userInfo.getId().toString(), reply.getThreadId(), SubscriptionObjectType.THREAD);
		Mockito.verify(mockTransactionalMessenger).sendMessageAfterCommit(replyId.toString(), ObjectType.REPLY, bundle.getEtag(), ChangeType.CREATE, userInfo.getId());
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetReplyUnauthorized() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		replyManager.getReply(userInfo, replyId.toString());
	}

	@Test
	public void testGetReplyAuthorized() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		DiscussionReplyBundle reply = replyManager.getReply(userInfo, replyId.toString());
		assertEquals(bundle, reply);
	}

	@Test (expected = NotFoundException.class)
	public void testGetDeletedReply() {
		Mockito.when(mockReplyDao.getReply(Mockito.anyLong(), Mockito.any(DiscussionFilter.class)))
				.thenThrow(new NotFoundException());
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		replyManager.getReply(userInfo, replyId.toString());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateReplyMessageWithNullMessageMarkdown() throws IOException {
		Mockito.when(mockAuthorizationManager.isUserCreatorOrAdmin(userInfo, replyId.toString())).thenReturn(false);
		UpdateReplyMessage newMessage = new UpdateReplyMessage();
		replyManager.updateReplyMessage(userInfo, replyId.toString(), newMessage);
	}

	@Test (expected = UnauthorizedException.class)
	public void testUpdateReplyMessageUnauthorized() throws IOException {
		Mockito.when(mockAuthorizationManager.isUserCreatorOrAdmin(userInfo, bundle.getCreatedBy())).thenReturn(false);
		UpdateReplyMessage newMessage = new UpdateReplyMessage();
		newMessage.setMessageMarkdown("messageMarkdown");
		replyManager.updateReplyMessage(userInfo, replyId.toString(), newMessage);
	}

	@Test
	public void testUpdateReplyMessageAuthorized() throws IOException {
		Mockito.when(mockAuthorizationManager.isUserCreatorOrAdmin(userInfo, bundle.getCreatedBy())).thenReturn(true);
		Mockito.when(mockUploadDao.uploadReplyMessage("messageMarkdown", forumId, threadId, replyId.toString()))
				.thenReturn(messageKey);
		Mockito.when(mockReplyDao.updateMessageKey(replyId, messageKey)).thenReturn(bundle);
		UpdateReplyMessage newMessage = new UpdateReplyMessage();
		newMessage.setMessageMarkdown("messageMarkdown");
		replyManager.updateReplyMessage(userInfo, replyId.toString(), newMessage);
		Mockito.verify(mockReplyDao).updateMessageKey(replyId, messageKey);
	}

	@Test (expected = UnauthorizedException.class)
	public void testMarkReplyAsDeletedUnauthorized() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		replyManager.markReplyAsDeleted(userInfo, replyId.toString());
		Mockito.verifyZeroInteractions(mockReplyDao);
	}

	@Test
	public void testMarkReplyAsDeletedAuthorized() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		replyManager.markReplyAsDeleted(userInfo, replyId.toString());
		Mockito.verify(mockReplyDao).markReplyAsDeleted(replyId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRepliesForThreadWithNullUserInfo() {
		replyManager.getRepliesForThread(null, threadId, 2L, 0L, DiscussionReplyOrder.CREATED_ON, false, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRepliesForThreadWithNullThreadId() {
		replyManager.getRepliesForThread(userInfo, null, 2L, 0L, DiscussionReplyOrder.CREATED_ON, false, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetRepliesForThreadUnauthorized() {
		Mockito.when(mockThreadManager.getThread(userInfo, threadId))
				.thenThrow(new UnauthorizedException());
		replyManager.getRepliesForThread(userInfo, threadId, 2L, 0L, DiscussionReplyOrder.CREATED_ON, true, DiscussionFilter.NO_FILTER);
	}

	@Test
	public void testGetRepliesForThread() {
		PaginatedResults<DiscussionReplyBundle> replies = new PaginatedResults<DiscussionReplyBundle>();
		replies.setResults(Arrays.asList(bundle));
		Mockito.when(mockReplyDao.getRepliesForThread(Mockito.anyLong(), Mockito.anyLong(),
				Mockito.anyLong(), (DiscussionReplyOrder) Mockito.any(), Mockito.anyBoolean(),
				Mockito.any(DiscussionFilter.class))).thenReturn(replies);
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		assertEquals(replies, replyManager.getRepliesForThread(userInfo, threadId, 2L, 0L, DiscussionReplyOrder.CREATED_ON, true, DiscussionFilter.NO_FILTER));
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetReplyUrlUnauthorized() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		replyManager.getMessageUrl(userInfo, messageKey);
	}

	@Test
	public void testGetReplyUrlAuthorized() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		MessageURL url = replyManager.getMessageUrl(userInfo, messageKey);
		assertNotNull(url);
		assertNotNull(url.getMessageUrl());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetReplyCountForThreadWithNullUserInfo() {
		replyManager.getReplyCountForThread(null, threadId, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetReplyCountForThreadWithNullThreadId() {
		replyManager.getReplyCountForThread(userInfo, null, DiscussionFilter.NO_FILTER);
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetReplyCountForThreadUnauthorized() {
		Mockito.when(mockThreadManager.getThread(userInfo, threadId))
				.thenThrow(new UnauthorizedException());
		replyManager.getReplyCountForThread(userInfo, threadId, DiscussionFilter.NO_FILTER);
	}

	@Test
	public void testGetReplyCountForThread() {
		Long count = 3L;
		Mockito.when(mockReplyDao.getReplyCount(Mockito.anyLong(), Mockito.any(DiscussionFilter.class))).thenReturn(count);
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		assertEquals((Long)3L, replyManager.getReplyCountForThread(userInfo, threadId, DiscussionFilter.NO_FILTER).getCount());
	}
}
