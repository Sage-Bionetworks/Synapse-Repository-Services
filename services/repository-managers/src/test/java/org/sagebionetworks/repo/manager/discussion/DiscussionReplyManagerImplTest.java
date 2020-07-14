package org.sagebionetworks.repo.manager.discussion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
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
import org.sagebionetworks.repo.model.message.MessageToSend;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
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

	@InjectMocks
	private DiscussionReplyManagerImpl replyManager;
	
	private UserInfo userInfo = new UserInfo(false /*not admin*/);
	private Long userId = 765L;
	private String threadId = "123";
	private String projectId = "syn456";
	private String forumId = "789";
	private Long replyId = 222L;
	private DiscussionReplyBundle bundle;
	private String messageKey;
	private MessageURL messageUrl = new MessageURL();

	@BeforeEach
	public void before() {		
		messageUrl.setMessageUrl("messageUrl");
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
	}

	@Test
	public void testCreateReplyWithInvalidUserInfo() throws IOException {
		assertThrows(IllegalArgumentException.class, () -> {			
			replyManager.createReply(null, new CreateDiscussionReply());
		});
	}

	@Test
	public void testCreateReplyWithNullDTO() throws IOException {
		assertThrows(IllegalArgumentException.class, () -> {	
			replyManager.createReply(userInfo, null);
		});
	}

	@Test
	public void testCreateReplyWithNullThreadId() throws IOException {
		CreateDiscussionReply createReply = new CreateDiscussionReply();
		createReply.setThreadId(null);
		createReply.setMessageMarkdown("messageMarkdown");
		
		assertThrows(IllegalArgumentException.class, () -> {	
			replyManager.createReply(userInfo, createReply);
		});
	}

	@Test
	public void testCreateReplyWithNullMessage() throws IOException {
		CreateDiscussionReply createReply = new CreateDiscussionReply();
		createReply.setThreadId(threadId);
		createReply.setMessageMarkdown(null);
		
		assertThrows(IllegalArgumentException.class, () -> {	
			replyManager.createReply(userInfo, createReply);
		});
	}

	@Test
	public void testCreateReplyUnauthorized() throws IOException {
		CreateDiscussionReply createReply = new CreateDiscussionReply();
		createReply.setThreadId(threadId);
		createReply.setMessageMarkdown("messageMarkdown");
		when(mockThreadManager.getThread(userInfo, threadId)).thenThrow(new UnauthorizedException());
		
		assertThrows(UnauthorizedException.class, () -> {	
			replyManager.createReply(userInfo, createReply);
		});
	}

	@Test
	public void testCreateReplyByAnonymous() throws IOException {
		when(mockAuthorizationManager.isAnonymousUser(userInfo)).thenReturn(true);
		CreateDiscussionReply createReply = new CreateDiscussionReply();
		createReply.setThreadId(threadId);
		createReply.setMessageMarkdown("messageMarkdown");
		
		assertThrows(UnauthorizedException.class, () -> {	
			replyManager.createReply(userInfo, createReply);
		});
	}

	@Test
	public void testCreateReplyAuthorized() throws IOException {
		
		when(mockThreadManager.getThread(userInfo, threadId)).thenReturn(mockThread);
		when(mockThread.getForumId()).thenReturn(forumId);
		when(mockIdGenerator.generateNewId(IdType.DISCUSSION_REPLY_ID)).thenReturn(replyId);
		when(mockAuthorizationManager.isAnonymousUser(userInfo)).thenReturn(false);
		
		String message = "messageMarkdown";
		CreateDiscussionReply createReply = new CreateDiscussionReply();
		createReply.setThreadId(threadId);
		createReply.setMessageMarkdown(message);
		when(mockUploadDao.uploadReplyMessage(message, forumId, threadId, replyId.toString()))
				.thenReturn(messageKey);
		when(mockReplyDao.createReply(threadId, replyId.toString(), messageKey, userInfo.getId()))
				.thenReturn(bundle);
		
		// Call under test
		DiscussionReplyBundle reply = replyManager.createReply(userInfo, createReply);
		
		assertEquals(bundle, reply);
		verify(mockSubscriptionDao).create(eq(userId.toString()), eq(reply.getThreadId()), eq(SubscriptionObjectType.THREAD));
		
		MessageToSend expectedReplyMessage = new MessageToSend()
				.withUserId(userId)
				.withObjectType(ObjectType.REPLY)
				.withObjectId(replyId.toString())
				.withChangeType(ChangeType.CREATE);
		
		verify(mockTransactionalMessenger).sendMessageAfterCommit(expectedReplyMessage);
		verify(mockThreadDao).insertEntityReference(anyList());
		
		MessageToSend expectedThreadMessage = new MessageToSend()
				.withUserId(userId)
				.withObjectType(ObjectType.THREAD)
				.withObjectId(threadId)
				.withChangeType(ChangeType.UPDATE);
		
		verify(mockTransactionalMessenger).sendMessageAfterCommit(expectedThreadMessage);
	}

	@Test
	public void testGetReplyUnauthorized() {
		when(mockReplyDao.getReply(Mockito.anyLong(), Mockito.any(DiscussionFilter.class))).thenReturn(bundle);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		
		assertThrows(UnauthorizedException.class, () -> {			
			replyManager.getReply(userInfo, replyId.toString());
		});
	}

	@Test
	public void testGetReplyAuthorized() {
		when(mockReplyDao.getReply(Mockito.anyLong(), Mockito.any(DiscussionFilter.class))).thenReturn(bundle);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		DiscussionReplyBundle reply = replyManager.getReply(userInfo, replyId.toString());
		assertEquals(bundle, reply);
	}

	@Test
	public void testGetDeletedReplyUnauthorized() {
		bundle.setIsDeleted(true);
		when(mockReplyDao.getReply(Mockito.anyLong(), Mockito.any(DiscussionFilter.class))).thenReturn(bundle);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		
		assertThrows(NotFoundException.class, () -> {			
			replyManager.getReply(userInfo, replyId.toString());
		});
	}

	@Test
	public void testGetDeletedReplyAuthorized() {
		bundle.setIsDeleted(true);
		when(mockReplyDao.getReply(Mockito.anyLong(), Mockito.any(DiscussionFilter.class))).thenReturn(bundle);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationStatus.authorized());
		assertEquals(bundle, replyManager.getReply(userInfo, replyId.toString()));
	}

	@Test
	public void testUpdateReplyMessageWithNullMessageMarkdown() throws IOException {
		UpdateReplyMessage newMessage = new UpdateReplyMessage();
		
		assertThrows(IllegalArgumentException.class, () -> {			
			replyManager.updateReplyMessage(userInfo, replyId.toString(), newMessage);
		});
	}

	@Test
	public void testUpdateReplyMessageUnauthorized() throws IOException {
		when(mockReplyDao.getReply(Mockito.anyLong(), Mockito.any(DiscussionFilter.class))).thenReturn(bundle);
		when(mockAuthorizationManager.isUserCreatorOrAdmin(userInfo, bundle.getCreatedBy())).thenReturn(false);
		UpdateReplyMessage newMessage = new UpdateReplyMessage();
		newMessage.setMessageMarkdown("messageMarkdown");
		
		assertThrows(UnauthorizedException.class, () -> {
			replyManager.updateReplyMessage(userInfo, replyId.toString(), newMessage);
		});
	}

	@Test
	public void testUpdateReplyMessageForDeletedReply() throws IOException {
		when(mockReplyDao.getReply(replyId, DiscussionFilter.EXCLUDE_DELETED)).thenThrow(new NotFoundException());
		UpdateReplyMessage newMessage = new UpdateReplyMessage();
		newMessage.setMessageMarkdown("messageMarkdown");
		
		assertThrows(NotFoundException.class, () -> {
			replyManager.updateReplyMessage(userInfo, replyId.toString(), newMessage);
		});
	}

	@Test
	public void testUpdateReplyMessageAuthorized() throws IOException {
		when(mockReplyDao.getReply(Mockito.anyLong(), Mockito.any(DiscussionFilter.class))).thenReturn(bundle);
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

	@Test
	public void testMarkReplyAsDeletedUnauthorized() {
		when(mockReplyDao.getProjectId(replyId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, () -> {
			replyManager.markReplyAsDeleted(userInfo, replyId.toString());
		});
		verifyZeroInteractions(mockReplyDao);
	}

	@Test
	public void testMarkReplyAsDeletedAuthorized() {
		when(mockReplyDao.getProjectId(replyId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationStatus.authorized());
		replyManager.markReplyAsDeleted(userInfo, replyId.toString());
		verify(mockReplyDao).markReplyAsDeleted(replyId);
	}

	@Test
	public void testGetRepliesForThreadWithNullUserInfo() {
		Mockito.doThrow(new IllegalArgumentException()).when(mockThreadManager).checkPermission(null, threadId, ACCESS_TYPE.READ);
		
		assertThrows(IllegalArgumentException.class, () -> {
			replyManager.getRepliesForThread(null, threadId, 2L, 0L, DiscussionReplyOrder.CREATED_ON, false, DiscussionFilter.NO_FILTER);
		});
	}

	@Test
	public void testGetRepliesForThreadWithNullThreadId() {
		assertThrows(IllegalArgumentException.class, () -> {
			replyManager.getRepliesForThread(userInfo, null, 2L, 0L, DiscussionReplyOrder.CREATED_ON, false, DiscussionFilter.NO_FILTER);
		});
	}

	@Test
	public void testGetRepliesForThreadUnauthorized() {
		Mockito.doThrow(new UnauthorizedException()).when(mockThreadManager).checkPermission(userInfo, threadId, ACCESS_TYPE.READ);
		assertThrows(UnauthorizedException.class, () -> {
			replyManager.getRepliesForThread(userInfo, threadId, 2L, 0L, DiscussionReplyOrder.CREATED_ON, true, DiscussionFilter.NO_FILTER);
		});
	}

	@Test
	public void testGetRepliesForThread() {
		doNothing().when(mockThreadManager).checkPermission(any(), any(), any());
		PaginatedResults<DiscussionReplyBundle> replies = PaginatedResults.createWithLimitAndOffset(Arrays.asList(bundle), 2L, 0L);
		when(mockReplyDao.getRepliesForThread(Mockito.anyLong(), Mockito.anyLong(),
				Mockito.anyLong(), (DiscussionReplyOrder) Mockito.any(), Mockito.anyBoolean(),
				Mockito.any(DiscussionFilter.class))).thenReturn(Arrays.asList(bundle));
		assertEquals(replies, replyManager.getRepliesForThread(userInfo, threadId, 2L, 0L, DiscussionReplyOrder.CREATED_ON, true, DiscussionFilter.NO_FILTER));
	}

	@Test
	public void testGetReplyUrlUnauthorized() {
		when(mockReplyDao.getProjectId(replyId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, () -> {
			replyManager.getMessageUrl(userInfo, messageKey);
		});
	}

	@Test
	public void testGetReplyUrlAuthorized() {
		when(mockUploadDao.getReplyUrl(Mockito.anyString())).thenReturn(messageUrl);
		when(mockReplyDao.getProjectId(replyId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		MessageURL url = replyManager.getMessageUrl(userInfo, messageKey);
		assertNotNull(url);
		assertNotNull(url.getMessageUrl());
	}

	@Test
	public void testGetReplyCountForThreadWithNullUserInfo() {
		Mockito.doThrow(new IllegalArgumentException()).when(mockThreadManager).checkPermission(null, threadId, ACCESS_TYPE.READ);
		assertThrows(IllegalArgumentException.class, () -> {
			replyManager.getReplyCountForThread(null, threadId, DiscussionFilter.NO_FILTER);
		});
	}

	@Test
	public void testGetReplyCountForThreadWithNullThreadId() {
		assertThrows(IllegalArgumentException.class, () -> {
			replyManager.getReplyCountForThread(userInfo, null, DiscussionFilter.NO_FILTER);
		});
	}

	@Test
	public void testGetReplyCountForThreadUnauthorized() {
		Mockito.doThrow(new UnauthorizedException()).when(mockThreadManager).checkPermission(userInfo, threadId, ACCESS_TYPE.READ);
		assertThrows(UnauthorizedException.class, () -> {
			replyManager.getReplyCountForThread(userInfo, threadId, DiscussionFilter.NO_FILTER);
		});
	}

	@Test
	public void testGetReplyCountForThread() {
		doNothing().when(mockThreadManager).checkPermission(any(), any(), any());
		Long count = 3L;
		when(mockReplyDao.getReplyCount(Mockito.anyLong(), Mockito.any(DiscussionFilter.class))).thenReturn(count);
		assertEquals((Long)3L, replyManager.getReplyCountForThread(userInfo, threadId, DiscussionFilter.NO_FILTER).getCount());
	}

}
