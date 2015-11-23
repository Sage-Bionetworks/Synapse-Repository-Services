package org.sagebionetworks.repo.manager.discussion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.ForumDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.springframework.test.util.ReflectionTestUtils;

public class DiscussionThreadManagerImplTest {
	private DiscussionThreadDAO mockThreadDao;
	private ForumDAO mockForumDao;
	private UploadContentToS3DAO mockUploadDao;
	private AuthorizationManager mockAuthorizationManager;
	private DiscussionThreadManager threadManager;
	private UserInfo userInfo = new UserInfo(false /*not admin*/);
	private CreateDiscussionThread createDto;
	private DiscussionThreadBundle dto;
	private Long forumId = 1L;
	private String projectId = "syn123";
	private Long userId = 2L;
	private Long threadId = 3L;
	private Forum forum;
	private String messageUrl = "messageUrl";

	@Before
	public void before() {
		mockThreadDao = Mockito.mock(DiscussionThreadDAO.class);
		mockForumDao = Mockito.mock(ForumDAO.class);
		mockUploadDao = Mockito.mock(UploadContentToS3DAO.class);
		mockAuthorizationManager = Mockito.mock(AuthorizationManager.class);
		threadManager = new DiscussionThreadManagerImpl();
		ReflectionTestUtils.setField(threadManager, "threadDao", mockThreadDao);
		ReflectionTestUtils.setField(threadManager, "forumDao", mockForumDao);
		ReflectionTestUtils.setField(threadManager, "uploadDao", mockUploadDao);
		ReflectionTestUtils.setField(threadManager, "authorizationManager", mockAuthorizationManager);

		createDto = new CreateDiscussionThread();
		createDto.setForumId(forumId.toString());
		createDto.setTitle("title");
		createDto.setMessageMarkdown("messageMarkdown");
		forum = new Forum();
		forum.setId(forumId.toString());
		forum.setProjectId(projectId);
		dto = new DiscussionThreadBundle();
		dto.setProjectId(projectId);
		userInfo.setId(userId);

		Mockito.when(mockForumDao.getForum(Long.parseLong(createDto.getForumId()))).thenReturn(forum);
		Mockito.when(mockThreadDao.getThread(threadId)).thenReturn(dto);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullDTO(){
		threadManager.createThread(userInfo, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullForumId(){
		createDto.setForumId(null);
		threadManager.createThread(userInfo, createDto);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullTitle(){
		createDto.setTitle(null);
		threadManager.createThread(userInfo, createDto);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullMessage(){
		createDto.setMessageMarkdown(null);
		threadManager.createThread(userInfo, createDto);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNulluserInfo(){
		threadManager.createThread(null, new CreateDiscussionThread());
	}

	@Test (expected = UnauthorizedException.class)
	public void testCreateAccessDenied() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		threadManager.createThread(userInfo, createDto);
	}

	@Test
	public void testCreateAuthorized() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		Mockito.when(mockUploadDao.upload(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(messageUrl);
		Mockito.when(mockThreadDao.createThread(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong()))
				.thenReturn(dto);
		DiscussionThreadBundle createdThread = threadManager.createThread(userInfo, createDto);
		assertNotNull(createdThread);
		assertEquals(createdThread, dto);
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
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateTitleWithNullTitle() {
		threadManager.updateTitle(userInfo, threadId.toString(), null);
	}

	@Test (expected = UnauthorizedException.class)
	public void testUpdateTitleUnauthorized() {
		Mockito.when(mockAuthorizationManager.isUserCreatorOrAdmin(Mockito.eq(userInfo), Mockito.anyString()))
				.thenReturn(false);
		threadManager.updateTitle(userInfo, threadId.toString(), "newTitle");
	}

	@Test
	public void testUpdateTitleAuthorized() {
		Mockito.when(mockAuthorizationManager.isUserCreatorOrAdmin(Mockito.eq(userInfo), Mockito.anyString()))
				.thenReturn(true);
		Mockito.when(mockThreadDao.updateTitle(Mockito.anyLong(), Mockito.anyString())).thenReturn(dto);
		assertEquals(dto, threadManager.updateTitle(userInfo, threadId.toString(), "newTitle"));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateMessageWithNullMessage() {
		threadManager.updateMessage(userInfo, threadId.toString(), null);
	}

	@Test (expected = UnauthorizedException.class)
	public void testUpdateMessageUnauthorized() {
		Mockito.when(mockAuthorizationManager.isUserCreatorOrAdmin(Mockito.eq(userInfo), Mockito.anyString()))
				.thenReturn(false);
		threadManager.updateMessage(userInfo, threadId.toString(), "newMessage");
	}

	@Test
	public void testUpdateMessageAuthorized() {
		Mockito.when(mockAuthorizationManager.isUserCreatorOrAdmin(Mockito.eq(userInfo), Mockito.anyString()))
				.thenReturn(true);
		Mockito.when(mockUploadDao.upload(Mockito.anyString(), Mockito.anyString()))
				.thenReturn("newMessage");
		Mockito.when(mockThreadDao.updateMessageUrl(Mockito.anyLong(), Mockito.anyString())).thenReturn(dto);
		assertEquals(dto, threadManager.updateMessage(userInfo, threadId.toString(), "newMessage"));
	}

	@Test (expected = UnauthorizedException.class)
	public void testDeleteUnauthorized() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.DELETE))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		threadManager.deleteThread(userInfo, threadId.toString());
	}

	@Test
	public void testDeleteAuthorized() {
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.DELETE))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		threadManager.deleteThread(userInfo, threadId.toString());
		Mockito.verify(mockThreadDao).deleteThread(threadId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetThreadsForForumWithNullForumId() {
		threadManager.getThreadsForForum(userInfo, null, null, 2, 0);
	}

	@Test
	public void testGetThreadsForForum() {
		PaginatedResults<DiscussionThreadBundle> threads = new PaginatedResults<DiscussionThreadBundle>();
		Mockito.when(mockThreadDao.getThreads(Mockito.anyLong(), (DiscussionOrder) Mockito.any(), Mockito.anyInt(), Mockito.anyInt()))
				.thenReturn(threads);
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		assertEquals(threads, threadManager.getThreadsForForum(userInfo, forumId.toString(), DiscussionOrder.LAST_ACTIVITY, 2, 0));
	}
}
