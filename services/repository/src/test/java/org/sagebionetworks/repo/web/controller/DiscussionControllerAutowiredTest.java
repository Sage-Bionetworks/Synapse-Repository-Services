package org.sagebionetworks.repo.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.EntityThreadCounts;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.MessageURL;
import org.sagebionetworks.repo.model.discussion.UpdateReplyMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;
import org.springframework.beans.factory.annotation.Autowired;

public class DiscussionControllerAutowiredTest extends AbstractAutowiredControllerJunit5TestBase {

	@Autowired
	private OIDCTokenHelper oidcTokenHelper;
		
	private String accessToken;
	private Entity project;
	private Long adminUserId;
	private CreateDiscussionThread createThread;
	private CreateDiscussionReply createReply;

	@BeforeEach
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		accessToken = oidcTokenHelper.createTotalAccessToken(adminUserId);

		project = new Project();
		project.setName(UUID.randomUUID().toString());
		project = servletTestHelper.createEntity(dispatchServlet, project, accessToken);

		createThread = new CreateDiscussionThread();
		createThread.setTitle("title");
		createThread.setMessageMarkdown("messageMarkdown");

		createReply = new CreateDiscussionReply();
		createReply.setMessageMarkdown("messageMardown");
	}

	@AfterEach
	public void cleanup() {
		try {
			servletTestHelper.deleteEntity(dispatchServlet, null, project.getId(), accessToken,
					Collections.singletonMap("skipTrashCan", "false"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetForum() throws Exception {
		Forum dto = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		assertNotNull(dto);
		assertNotNull(dto.getId());
		assertEquals(dto.getProjectId(), project.getId());
		assertEquals(dto, servletTestHelper.getForum(dispatchServlet, dto.getId(), accessToken));
	}

	@Test
	public void testCreateThread() throws Exception {
		Forum dto = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle bundle = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		assertNotNull(bundle);
		assertEquals(bundle.getForumId(), createThread.getForumId());
		assertEquals(bundle.getTitle(), createThread.getTitle());
	}

	@Test
	public void testGetThread() throws Exception {
		Forum dto = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle bundle = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		DiscussionThreadBundle bundle2 = servletTestHelper.getThread(dispatchServlet, accessToken, bundle.getId());
		assertEquals(bundle, bundle2);
	}

	@Test
	public void testGetDeletedThread() throws Exception {
		Forum dto = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle bundle = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		servletTestHelper.markThreadAsDeleted(dispatchServlet, accessToken, bundle.getId());
		DiscussionThreadBundle bundle2 = servletTestHelper.getThread(dispatchServlet, accessToken, bundle.getId());
		bundle.setIsDeleted(true);
		bundle.setEtag(bundle2.getEtag());
		assertEquals(bundle, bundle2);
	}

	@Test
	public void testGetThreadsForForum() throws Exception {
		Forum forum = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		createThread.setForumId(forum.getId());
		DiscussionThreadBundle bundle1 = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		DiscussionThreadBundle bundle2 = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		PaginatedResults<DiscussionThreadBundle> results = servletTestHelper.getThreadsForForum(dispatchServlet, accessToken, forum.getId(), 1L, 1L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER);
		assertEquals(bundle2, results.getResults().get(0));
		assertEquals(3L, results.getTotalNumberOfResults());
	}

	@Test
	public void testGetAvailableThreadsAndDeletedThreads() throws Exception {
		Forum forum = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		createThread.setForumId(forum.getId());
		DiscussionThreadBundle bundle1 = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		DiscussionThreadBundle bundle2 = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		servletTestHelper.markThreadAsDeleted(dispatchServlet, accessToken, bundle1.getId());
		PaginatedResults<DiscussionThreadBundle> deleted = servletTestHelper.getThreadsForForum(dispatchServlet, accessToken, forum.getId(), 10L, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.DELETED_ONLY);
		assertEquals(1L, deleted.getTotalNumberOfResults());
		assertEquals(bundle1.getId(), deleted.getResults().get(0).getId());
		PaginatedResults<DiscussionThreadBundle> available = servletTestHelper.getThreadsForForum(dispatchServlet, accessToken, forum.getId(), 10L, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.EXCLUDE_DELETED);
		assertEquals(1L, available.getTotalNumberOfResults());
		assertEquals(bundle2.getId(), available.getResults().get(0).getId());
	}

	@Test
	public void testGetThreadCount() throws Exception {
		Forum forum = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		createThread.setForumId(forum.getId());
		DiscussionThreadBundle bundle1 = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		DiscussionThreadBundle bundle2 = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		servletTestHelper.markThreadAsDeleted(dispatchServlet, accessToken, bundle1.getId());
		assertEquals((Long)1L, servletTestHelper.getThreadCount(dispatchServlet, accessToken, forum.getId(), DiscussionFilter.DELETED_ONLY).getCount());
		assertEquals((Long)1L, servletTestHelper.getThreadCount(dispatchServlet, accessToken, forum.getId(), DiscussionFilter.EXCLUDE_DELETED).getCount());
		assertEquals((Long)2L, servletTestHelper.getThreadCount(dispatchServlet, accessToken, forum.getId(), DiscussionFilter.NO_FILTER).getCount());
	}

	@Test
	public void testUpdateThreadTitle() throws Exception {
		Forum dto = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle bundle = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		UpdateThreadTitle newTitle = new UpdateThreadTitle();
		newTitle.setTitle("newTitle");
		DiscussionThreadBundle bundle2 = servletTestHelper.updateThreadTitle(dispatchServlet, accessToken, bundle.getId(), newTitle );
		assertFalse(bundle.equals(bundle2));
		assertEquals(bundle2.getId(), bundle.getId());
		assertTrue(bundle2.getIsEdited());
	}

	@Test
	public void testUpdateThreadMessage() throws Exception {
		Forum dto = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle bundle = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		UpdateThreadMessage newMessage = new UpdateThreadMessage();
		newMessage.setMessageMarkdown("newMessageMarkdown");
		DiscussionThreadBundle bundle2 = servletTestHelper.updateThreadMessage(dispatchServlet, accessToken, bundle.getId(), newMessage);
		assertFalse(bundle.equals(bundle2));
		assertEquals(bundle2.getId(), bundle.getId());
		assertTrue(bundle2.getIsEdited());
	}

	@Test
	public void testMarkThreadAsDeletedAndRestore() throws Exception {
		Forum dto = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle bundle = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		servletTestHelper.markThreadAsDeleted(dispatchServlet, accessToken, bundle.getId());
		assertTrue(servletTestHelper.getThread(dispatchServlet, accessToken, bundle.getId()).getIsDeleted());
		servletTestHelper.markThreadAsNotDeleted(dispatchServlet, accessToken, bundle.getId());
		assertFalse(servletTestHelper.getThread(dispatchServlet, accessToken, bundle.getId()).getIsDeleted());
	}

	@Test
	public void testPinning() throws Exception {
		Forum dto = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle bundle = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		assertFalse(servletTestHelper.getThread(dispatchServlet, accessToken, bundle.getId()).getIsPinned());
		servletTestHelper.pinThread(dispatchServlet, accessToken, bundle.getId());
		assertTrue(servletTestHelper.getThread(dispatchServlet, accessToken, bundle.getId()).getIsPinned());
		servletTestHelper.unpinThread(dispatchServlet, accessToken, bundle.getId());
		assertFalse(servletTestHelper.getThread(dispatchServlet, accessToken, bundle.getId()).getIsPinned());
	}

	@Test
	public void testGetThreadUrl() throws Exception {
		Forum dto = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle bundle = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		MessageURL url = servletTestHelper.getThreadUrl(dispatchServlet, accessToken, bundle.getMessageKey());
		assertNotNull(url);
		assertNotNull(url.getMessageUrl());
	}

	@Test
	public void testCreateReply() throws Exception {
		Forum dto = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle threadBundle = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		createReply.setThreadId(threadBundle.getId());
		DiscussionReplyBundle replyBundle = servletTestHelper.createReply(dispatchServlet, accessToken, createReply);
		assertNotNull(replyBundle);
		assertEquals(replyBundle.getThreadId(), createReply.getThreadId());
	}

	@Test
	public void testGetReply() throws Exception {
		Forum dto = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle threadBundle = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		createReply.setThreadId(threadBundle.getId());
		DiscussionReplyBundle replyBundle = servletTestHelper.createReply(dispatchServlet, accessToken, createReply);
		assertEquals(replyBundle, servletTestHelper.getReply(dispatchServlet, accessToken, replyBundle.getId()));
	}

	@Test
	public void testGetReplies() throws Exception {
		Forum forum = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		createThread.setForumId(forum.getId());
		DiscussionThreadBundle threadBundle = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		createReply.setThreadId(threadBundle.getId());
		DiscussionReplyBundle replyBundle1 = servletTestHelper.createReply(dispatchServlet, accessToken, createReply);
		DiscussionReplyBundle replyBundle2 = servletTestHelper.createReply(dispatchServlet, accessToken, createReply);
		PaginatedResults<DiscussionReplyBundle> results = servletTestHelper.getReplies(dispatchServlet, accessToken, threadBundle.getId(), 1L, 1L, DiscussionReplyOrder.CREATED_ON, true, DiscussionFilter.NO_FILTER);
		assertEquals(replyBundle2, results.getResults().get(0));
		assertEquals(3L, results.getTotalNumberOfResults());
		servletTestHelper.markReplyAsDeleted(dispatchServlet, accessToken, replyBundle1.getId());
		results = servletTestHelper.getReplies(dispatchServlet, accessToken, threadBundle.getId(), 1L, 0L, DiscussionReplyOrder.CREATED_ON, true, DiscussionFilter.EXCLUDE_DELETED);
		assertEquals(replyBundle2, results.getResults().get(0));
		assertEquals(2L, results.getTotalNumberOfResults());
	}

	@Test
	public void testGetAvailableRepliesAndDeletedReplies() throws Exception {
		Forum forum = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		createThread.setForumId(forum.getId());
		DiscussionThreadBundle threadBundle = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		createReply.setThreadId(threadBundle.getId());
		DiscussionReplyBundle replyBundle1 = servletTestHelper.createReply(dispatchServlet, accessToken, createReply);
		DiscussionReplyBundle replyBundle2 = servletTestHelper.createReply(dispatchServlet, accessToken, createReply);
		servletTestHelper.markReplyAsDeleted(dispatchServlet, accessToken, replyBundle1.getId());
		PaginatedResults<DiscussionReplyBundle> deleted = servletTestHelper.getReplies(dispatchServlet, accessToken, threadBundle.getId(), 10L, 0L, DiscussionReplyOrder.CREATED_ON, true, DiscussionFilter.DELETED_ONLY);
		assertEquals(1L, deleted.getTotalNumberOfResults());
		assertEquals(replyBundle1.getId(), deleted.getResults().get(0).getId());
		PaginatedResults<DiscussionReplyBundle> available = servletTestHelper.getReplies(dispatchServlet, accessToken, threadBundle.getId(), 10L, 0L, DiscussionReplyOrder.CREATED_ON, true, DiscussionFilter.EXCLUDE_DELETED);
		assertEquals(1L, available.getTotalNumberOfResults());
		assertEquals(replyBundle2.getId(), available.getResults().get(0).getId());
	}

	@Test
	public void testGetReplyCount() throws Exception {
		Forum forum = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		createThread.setForumId(forum.getId());
		DiscussionThreadBundle threadBundle = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		createReply.setThreadId(threadBundle.getId());
		DiscussionReplyBundle replyBundle1 = servletTestHelper.createReply(dispatchServlet, accessToken, createReply);
		DiscussionReplyBundle replyBundle2 = servletTestHelper.createReply(dispatchServlet, accessToken, createReply);
		servletTestHelper.markReplyAsDeleted(dispatchServlet, accessToken, replyBundle1.getId());
		assertEquals((Long)1L, servletTestHelper.getReplyCount(dispatchServlet, accessToken, threadBundle.getId(), DiscussionFilter.DELETED_ONLY).getCount());
		assertEquals((Long)1L, servletTestHelper.getReplyCount(dispatchServlet, accessToken, threadBundle.getId(), DiscussionFilter.EXCLUDE_DELETED).getCount());
		assertEquals((Long)2L, servletTestHelper.getReplyCount(dispatchServlet, accessToken, threadBundle.getId(), DiscussionFilter.NO_FILTER).getCount());
	}

	@Test
	public void testUpdateReplyMessage() throws Exception {
		Forum dto = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle threadBundle = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		createReply.setThreadId(threadBundle.getId());
		DiscussionReplyBundle replyBundle = servletTestHelper.createReply(dispatchServlet, accessToken, createReply);
		UpdateReplyMessage newMessage = new UpdateReplyMessage();
		newMessage.setMessageMarkdown("newMessageMarkdown");
		DiscussionReplyBundle bundle2 = servletTestHelper.updateReplyMessage(dispatchServlet, accessToken, replyBundle.getId(), newMessage);
		assertFalse(replyBundle.equals(bundle2));
		assertEquals(bundle2.getId(), replyBundle.getId());
		assertTrue(bundle2.getIsEdited());
	}

	@Test
	public void testMarkReplyAsDeleted() throws Exception {
		Forum dto = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle threadBundle = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		createReply.setThreadId(threadBundle.getId());
		DiscussionReplyBundle replyBundle = servletTestHelper.createReply(dispatchServlet, accessToken, createReply);
		servletTestHelper.markReplyAsDeleted(dispatchServlet, accessToken, replyBundle.getId());
		assertTrue(servletTestHelper.getReply(dispatchServlet, accessToken, replyBundle.getId()).getIsDeleted());
	}

	@Test
	public void testGetReplyUrl() throws Exception {
		Forum dto = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle threadBundle = servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		createReply.setThreadId(threadBundle.getId());
		DiscussionReplyBundle replyBundle = servletTestHelper.createReply(dispatchServlet, accessToken, createReply);
		MessageURL url = servletTestHelper.getReplyUrl(dispatchServlet, accessToken, replyBundle.getMessageKey());
		assertNotNull(url);
		assertNotNull(url.getMessageUrl());
	}

	@Test
	public void testGetThreadsForEntity() throws Exception {
		Forum forum = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		createThread.setForumId(forum.getId());
		servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		PaginatedResults<DiscussionThreadBundle> results = servletTestHelper.getThreadsForEntity(dispatchServlet, accessToken, project.getId(), 1L, 1L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true);
		assertNotNull(results);
		assertEquals(1L, results.getTotalNumberOfResults());
		assertNotNull(results.getResults());
		assertTrue(results.getResults().isEmpty());
	}

	@Test
	public void testGetEntityThreadCounts() throws Exception {
		Forum forum = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		createThread.setForumId(forum.getId());
		servletTestHelper.createThread(dispatchServlet, accessToken, createThread);
		EntityIdList entityIdList = new EntityIdList();
		entityIdList.setIdList(Arrays.asList(project.getId()));
		EntityThreadCounts results = servletTestHelper.getEntityThreadCounts(dispatchServlet, accessToken, entityIdList);
		assertNotNull(results);
		assertNotNull(results.getList());
		assertTrue(results.getList().isEmpty());
	}

	@Test
	public void testGetModeratorsForForum() throws Exception {
		Forum forum = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), accessToken);
		PaginatedIds moderators = servletTestHelper.getModerators(dispatchServlet, adminUserId, forum.getId(), 10L, 0L);
		assertNotNull(moderators);
		assertEquals((Long)1L, moderators.getTotalNumberOfResults());
		assertTrue(moderators.getResults().contains(adminUserId.toString()));
	}
}
