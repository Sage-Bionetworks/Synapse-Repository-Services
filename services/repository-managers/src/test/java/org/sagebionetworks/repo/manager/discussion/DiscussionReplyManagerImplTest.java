package org.sagebionetworks.repo.manager.discussion;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
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
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
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
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		replyManager.createReply(userInfo, createReply);
	}

	@Test
	public void testCreateReplyAuthorized() throws IOException {
		String message = "messageMarkdown";
		String messageKey = UUID.randomUUID().toString();
		CreateDiscussionReply createReply = new CreateDiscussionReply();
		createReply.setThreadId(threadId);
		createReply.setMessageMarkdown(message);
		DiscussionReplyBundle bundle = new DiscussionReplyBundle();
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		Mockito.when(mockUploadDao.uploadDiscussionContent(message, forumId, threadId))
				.thenReturn(messageKey);
		Mockito.when(mockReplyDao.createReply(threadId, messageKey, userInfo.getId()))
				.thenReturn(bundle);
		DiscussionReplyBundle reply = replyManager.createReply(userInfo, createReply);
		assertEquals(bundle, reply);
	}
}
