package org.sagebionetworks.repo.service.discussion;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.discussion.DiscussionReplyManager;
import org.sagebionetworks.repo.manager.discussion.DiscussionSearchIndexManager;
import org.sagebionetworks.repo.manager.discussion.DiscussionThreadManager;
import org.sagebionetworks.repo.manager.discussion.ForumManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionSearchRequest;
import org.sagebionetworks.repo.model.discussion.DiscussionSearchResponse;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.MessageURL;
import org.sagebionetworks.repo.model.discussion.UpdateReplyMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;
import org.sagebionetworks.repo.service.discussion.DiscussionServiceImpl;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class DiscussionServiceImplTest {

	@Mock
	private UserManager mockUserManager;
	@Mock
	private ForumManager mockForumManager;
	@Mock
	private DiscussionThreadManager mockThreadManager;
	@Mock
	private DiscussionReplyManager mockReplyManager;
	@Mock
	private DiscussionSearchIndexManager mockSearchManager;
	
	@InjectMocks
	private DiscussionServiceImpl discussionServices;
	
	private Long userId = 123L;
	private UserInfo userInfo = new UserInfo(false /*not admin*/);
	private String projectId = "syn456";
	private CreateDiscussionThread createThread;
	private String forumId = "789";
	private String messageMarkdown = "messageMarkdown";
	private String title = "title";
	private DiscussionThreadBundle threadBundle;
	private String threadId = "321";
	private String messageKey = "messageKey";
	private DiscussionReplyBundle replyBundle;
	private String replyId = "987";
	private CreateDiscussionReply createReply;
	private MessageURL messageUrl = new MessageURL();
	
	@Mock
	private DiscussionSearchRequest mockSearchRequest;
	@Mock
	private DiscussionSearchResponse mockSearchResponse;

	@BeforeEach
	public void before() {

		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);

		createThread = new CreateDiscussionThread();
		createThread.setForumId(forumId);
		createThread.setMessageMarkdown(messageMarkdown);
		createThread.setTitle(title);

		threadBundle = new DiscussionThreadBundle();
		threadBundle.setId(threadId);
		threadBundle.setForumId(forumId);
		threadBundle.setTitle(title);
		threadBundle.setProjectId(projectId);
		threadBundle.setMessageKey(messageKey);

		createReply = new CreateDiscussionReply();
		createReply.setThreadId(threadId);
		createReply.setMessageMarkdown(messageMarkdown);

		replyBundle = new DiscussionReplyBundle();
		replyBundle.setId(replyId);
		replyBundle.setThreadId(threadId);
		replyBundle.setMessageKey(messageKey);

		messageUrl.setMessageUrl("messageUrl");
	}

	@Test
	public void testGetForumByProjectId() {
		discussionServices.getForumByProjectId(userId, projectId);
		Mockito.verify(mockUserManager).getUserInfo(userId);
		Mockito.verify(mockForumManager).getForumByProjectId(userInfo, projectId);
	}

	@Test
	public void testCreateThread() throws Exception {
		when(mockThreadManager.createThread(userInfo, createThread)).thenReturn(threadBundle);
		assertEquals(threadBundle, discussionServices.createThread(userId, createThread));
	}

	@Test
	public void testGetThread() {
		when(mockThreadManager.getThread(userInfo, threadId)).thenReturn(threadBundle);
		assertEquals(threadBundle, discussionServices.getThread(userId, threadId));
	}

	@Test
	public void testUpdateThreadTitle() {
		UpdateThreadTitle newTitle = new UpdateThreadTitle();
		newTitle.setTitle("newTitle");
		threadBundle.setTitle("newTitle");
		when(mockThreadManager.updateTitle(userInfo, threadId, newTitle)).thenReturn(threadBundle);
		assertEquals(threadBundle, discussionServices.updateThreadTitle(userId, threadId, newTitle));
	}

	@Test
	public void testUpdateThreadMessage() throws Exception {
		UpdateThreadMessage newMessage = new UpdateThreadMessage();
		newMessage.setMessageMarkdown("newMessage");
		threadBundle.setMessageKey("newkey");
		when(mockThreadManager.updateMessage(userInfo, threadId, newMessage)).thenReturn(threadBundle);
		assertEquals(threadBundle, discussionServices.updateThreadMessage(userId, threadId, newMessage));
	}

	@Test
	public void testMarkThreadAsDeleted() {
		discussionServices.markThreadAsDeleted(userId, threadId);
		Mockito.verify(mockThreadManager).markThreadAsDeleted(userInfo, threadId);
	}

	@Test
	public void testGetThreads() {
		PaginatedResults<DiscussionThreadBundle> threads = new PaginatedResults<DiscussionThreadBundle>();
		threads.setResults(Arrays.asList(threadBundle));
		when(mockThreadManager.getThreadsForForum(userInfo, forumId, 10L, 0L, null, true, DiscussionFilter.NO_FILTER)).thenReturn(threads);
		assertEquals(threads, discussionServices.getThreadsForForum(userId, forumId, 10L, 0L, null, true, DiscussionFilter.NO_FILTER));
	}

	@Test
	public void testCreateReply() throws Exception {
		when(mockReplyManager.createReply(userInfo, createReply)).thenReturn(replyBundle);
		assertEquals(replyBundle, discussionServices.createReply(userId, createReply));
	}

	@Test
	public void testGetReply() {
		when(mockReplyManager.getReply(userInfo, replyId)).thenReturn(replyBundle);
		assertEquals(replyBundle, discussionServices.getReply(userId, replyId));
	}

	@Test
	public void testUpdateReplyMessage() throws Exception {
		UpdateReplyMessage newMessage = new UpdateReplyMessage();
		newMessage.setMessageMarkdown("newMessage");
		replyBundle.setMessageKey("newkey");
		when(mockReplyManager.updateReplyMessage(userInfo, replyId, newMessage)).thenReturn(replyBundle);
		assertEquals(replyBundle, discussionServices.updateReplyMessage(userId, replyId, newMessage));
	}

	@Test
	public void testMarkReplyAsDeleted() {
		discussionServices.markReplyAsDeleted(userId, replyId);
		Mockito.verify(mockReplyManager).markReplyAsDeleted(userInfo, replyId);
	}

	@Test
	public void testGetReplies() {
		PaginatedResults<DiscussionReplyBundle> replies = new PaginatedResults<DiscussionReplyBundle>();
		replies.setResults(Arrays.asList(replyBundle));
		when(mockReplyManager.getRepliesForThread(userInfo, threadId, 10L, 0L, null, true, DiscussionFilter.NO_FILTER)).thenReturn(replies);
		assertEquals(replies, discussionServices.getReplies(userId, threadId, 10L, 0L, null, true, DiscussionFilter.NO_FILTER));
	}

	@Test
	public void testGetThreadUrl() {
		when(mockThreadManager.getMessageUrl(userInfo, messageKey)).thenReturn(messageUrl);
		assertEquals(messageUrl, discussionServices.getThreadUrl(userId, messageKey));
	}

	@Test
	public void testGetReplyUrl() {
		when(mockReplyManager.getMessageUrl(userInfo, messageKey)).thenReturn(messageUrl);
		assertEquals(messageUrl, discussionServices.getReplyUrl(userId, messageKey));
	}
	
	@Test
	public void testSearch() {
		when(mockSearchManager.search(any(), any(), any())).thenReturn(mockSearchResponse);
		assertEquals(mockSearchResponse, discussionServices.search(userId, forumId, mockSearchRequest));		
		verify(mockSearchManager).search(userInfo, Long.valueOf(forumId), mockSearchRequest);
	}
}
