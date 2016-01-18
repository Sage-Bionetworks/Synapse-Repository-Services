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
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.MessageURL;
import org.sagebionetworks.repo.model.discussion.UpdateReplyMessage;
import org.springframework.test.util.ReflectionTestUtils;

public class DiscussionReplyManagerImplTest {
	@Mock
	private DiscussionThreadManager mockThreadManager;
	@Mock
	private DiscussionReplyDAO mockReplyDao;
	@Mock
	private UploadContentToS3DAO mockUploadDao;
	@Mock
	private AuthorizationManager mockAuthorizationManager;
	@Mock
	private DiscussionThreadBundle mockThread;

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

		Mockito.when(mockThreadManager.getThread(userInfo, threadId)).thenReturn(mockThread);
		Mockito.when(mockThread.getProjectId()).thenReturn(projectId);
		Mockito.when(mockThread.getForumId()).thenReturn(forumId);
		messageUrl.setMessageUrl("messageUrl");
		Mockito.when(mockUploadDao.getUrl(Mockito.anyString())).thenReturn(messageUrl);

		bundle = new DiscussionReplyBundle();
		bundle.setThreadId(threadId);
		bundle.setForumId(forumId);
		bundle.setProjectId(projectId);
		messageKey = UUID.randomUUID().toString();
		bundle.setMessageKey(messageKey);
		userInfo.setId(765L);
		bundle.setCreatedBy(userInfo.getId().toString());
		Mockito.when(mockReplyDao.getReply(Mockito.anyLong())).thenReturn(bundle);
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

	@Test
	public void testCreateReplyAuthorized() throws IOException {
		String message = "messageMarkdown";
		CreateDiscussionReply createReply = new CreateDiscussionReply();
		createReply.setThreadId(threadId);
		createReply.setMessageMarkdown(message);
		Mockito.when(mockUploadDao.uploadDiscussionContent(message, forumId, threadId))
				.thenReturn(messageKey);
		Mockito.when(mockReplyDao.createReply(threadId, messageKey, userInfo.getId()))
				.thenReturn(bundle);
		DiscussionReplyBundle reply = replyManager.createReply(userInfo, createReply);
		assertEquals(bundle, reply);
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
		Mockito.when(mockUploadDao.uploadDiscussionContent("messageMarkdown", forumId, threadId))
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
	public void testGetRepliesForThreadWithNullThreadId() {
		replyManager.getRepliesForThread(userInfo, null, 2L, 0L, DiscussionReplyOrder.CREATED_ON, false);
	}

	@Test
	public void testGetThreadsForForum() {
		PaginatedResults<DiscussionReplyBundle> replies = new PaginatedResults<DiscussionReplyBundle>();
		replies.setResults(Arrays.asList(bundle));
		Mockito.when(mockReplyDao.getRepliesForThread(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), (DiscussionReplyOrder) Mockito.any(), Mockito.anyBoolean()))
				.thenReturn(replies);
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		assertEquals(replies, replyManager.getRepliesForThread(userInfo, threadId, 2L, 0L, DiscussionReplyOrder.CREATED_ON, true));
	}


	@Test (expected = UnauthorizedException.class)
	public void testGetReplyUrlUnauthorized() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		replyManager.getMessageUrl(userInfo, replyId.toString());
	}

	@Test
	public void testGetReplyUrlAuthorized() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		MessageURL url = replyManager.getMessageUrl(userInfo, replyId.toString());
		assertNotNull(url);
		assertNotNull(url.getMessageUrl());
	}
}
