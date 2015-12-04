package org.sagebionetworks.repo.web.service.discussion;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.discussion.DiscussionThreadManager;
import org.sagebionetworks.repo.manager.discussion.ForumManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;
import org.springframework.test.util.ReflectionTestUtils;

public class DiscussionServiceImplTest {

	@Mock
	private UserManager mockUserManager;
	@Mock
	private ForumManager mockForumManager;
	@Mock
	private DiscussionThreadManager mockThreadManager;

	private DiscussionServiceImpl discussionServices;
	private Long userId = 123L;
	private UserInfo userInfo = new UserInfo(false /*not admin*/);
	private String projectId = "syn456";
	private CreateDiscussionThread toCreate;
	private String forumId = "789";
	private String messageMarkdown = "messageMarkdown";
	private String title = "title";
	private DiscussionThreadBundle bundle;
	private String threadId = "321";
	private String messageUrl = "messageUrl";
	private String messageKey = "messageKey";

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);

		discussionServices = new DiscussionServiceImpl();
		ReflectionTestUtils.setField(discussionServices, "userManager", mockUserManager);
		ReflectionTestUtils.setField(discussionServices, "forumManager", mockForumManager);
		ReflectionTestUtils.setField(discussionServices, "threadManager", mockThreadManager);

		Mockito.when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);

		toCreate = new CreateDiscussionThread();
		toCreate.setForumId(forumId);
		toCreate.setMessageMarkdown(messageMarkdown);
		toCreate.setTitle(title);

		bundle = new DiscussionThreadBundle();
		bundle.setId(threadId);
		bundle.setForumId(forumId);
		bundle.setTitle(title);
		bundle.setProjectId(projectId);
		bundle.setMessageKey(messageKey);
		bundle.setMessageUrl(messageUrl);
	}

	@Test
	public void testGetForumMetadata() {
		discussionServices.getForumMetadata(userId, projectId);
		Mockito.verify(mockUserManager).getUserInfo(userId);
		Mockito.verify(mockForumManager).getForumMetadata(userInfo, projectId);
	}

	@Test
	public void testCreateThread() throws Exception {
		Mockito.when(mockThreadManager.createThread(userInfo, toCreate)).thenReturn(bundle);
		assertEquals(bundle, discussionServices.createThread(userId, toCreate));
	}

	@Test
	public void testGetThread() {
		Mockito.when(mockThreadManager.getThread(userInfo, threadId)).thenReturn(bundle);
		assertEquals(bundle, discussionServices.getThread(userId, threadId));
	}

	@Test
	public void testUpdateThreadTitle() {
		UpdateThreadTitle newTitle = new UpdateThreadTitle();
		newTitle.setTitle("newTitle");
		bundle.setTitle("newTitle");
		Mockito.when(mockThreadManager.updateTitle(userInfo, threadId, newTitle)).thenReturn(bundle);
		assertEquals(bundle, discussionServices.updateThreadTitle(userId, threadId, newTitle));
	}

	@Test
	public void testUpdateThreadMessage() throws Exception {
		UpdateThreadMessage newMessage = new UpdateThreadMessage();
		newMessage.setMessageMarkdown("newMessage");
		bundle.setMessageKey("newkey");
		bundle.setMessageUrl("newUrl");
		Mockito.when(mockThreadManager.updateMessage(userInfo, threadId, newMessage)).thenReturn(bundle);
		assertEquals(bundle, discussionServices.updateThreadMessage(userId, threadId, newMessage));
	}

	@Test
	public void testMarkThreadAsDeleted() {
		discussionServices.markThreadAsDeleted(userId, threadId);
		Mockito.verify(mockThreadManager).markThreadAsDeleted(userInfo, threadId);
	}

	@Test
	public void testGetThreads() {
		PaginatedResults<DiscussionThreadBundle> threads = new PaginatedResults<DiscussionThreadBundle>();
		threads.setResults(Arrays.asList(bundle));
		Mockito.when(mockThreadManager.getThreadsForForum(userInfo, forumId, 10L, 0L, null, true)).thenReturn(threads);
		assertEquals(threads, discussionServices.getThreads(userId, forumId, 10L, 0L, null, true));
	}

	@Test
	public void testGetThreadCount() {
		Long count = 23L;
		Mockito.when(mockThreadManager.getThreadCount(userInfo, forumId)).thenReturn(count);
		assertEquals(count, discussionServices.getThreadCount(userId, forumId));
	}
}
