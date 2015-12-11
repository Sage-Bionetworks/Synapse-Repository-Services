package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.UpdateReplyMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;

public class DiscussionControllerAutowiredTest extends AbstractAutowiredControllerTestBase{

	private Entity project;
	private Long adminUserId;
	private CreateDiscussionThread createThread;
	private CreateDiscussionReply createReply;

	@Before
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		project = new Project();
		project.setName(UUID.randomUUID().toString());
		project = servletTestHelper.createEntity(dispatchServlet, project, adminUserId);

		createThread = new CreateDiscussionThread();
		createThread.setTitle("title");
		createThread.setMessageMarkdown("messageMarkdown");

		createReply = new CreateDiscussionReply();
		createReply.setMessageMarkdown("messageMardown");
	}

	@After
	public void cleanup() {
		try {
			servletTestHelper.deleteEntity(dispatchServlet, null, project.getId(), adminUserId,
					Collections.singletonMap("skipTrashCan", "false"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetForumMetadata() throws Exception {
		Forum dto = servletTestHelper.getForumMetadata(dispatchServlet, project.getId(), adminUserId);
		assertNotNull(dto);
		assertNotNull(dto.getId());
		assertEquals(dto.getProjectId(), project.getId());
	}

	@Test
	public void testCreateThread() throws Exception {
		Forum dto = servletTestHelper.getForumMetadata(dispatchServlet, project.getId(), adminUserId);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle bundle = servletTestHelper.createThread(dispatchServlet, adminUserId, createThread);
		assertNotNull(bundle);
		assertEquals(bundle.getForumId(), createThread.getForumId());
		assertEquals(bundle.getTitle(), createThread.getTitle());
	}

	@Test
	public void testGetThread() throws Exception {
		Forum dto = servletTestHelper.getForumMetadata(dispatchServlet, project.getId(), adminUserId);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle bundle = servletTestHelper.createThread(dispatchServlet, adminUserId, createThread);
		DiscussionThreadBundle bundle2 = servletTestHelper.getThread(dispatchServlet, adminUserId, bundle.getId());
		assertEquals(bundle, bundle2);
	}

	@Test
	public void testGetThreads() throws Exception {
		Forum forum = servletTestHelper.getForumMetadata(dispatchServlet, project.getId(), adminUserId);
		createThread.setForumId(forum.getId());
		DiscussionThreadBundle bundle = servletTestHelper.createThread(dispatchServlet, adminUserId, createThread);
		PaginatedResults<DiscussionThreadBundle> results = servletTestHelper.getThreads(dispatchServlet, adminUserId, forum.getId(), 100L, 0L, null, null);
		assertEquals(bundle, results.getResults().get(0));
		assertEquals(1L, results.getTotalNumberOfResults());
	}

	@Test
	public void testUpdateThreadTitle() throws Exception {
		Forum dto = servletTestHelper.getForumMetadata(dispatchServlet, project.getId(), adminUserId);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle bundle = servletTestHelper.createThread(dispatchServlet, adminUserId, createThread);
		UpdateThreadTitle newTitle = new UpdateThreadTitle();
		newTitle.setTitle("newTitle");
		DiscussionThreadBundle bundle2 = servletTestHelper.updateThreadTitle(dispatchServlet, adminUserId, bundle.getId(), newTitle );
		assertFalse(bundle.equals(bundle2));
		assertEquals(bundle2.getId(), bundle.getId());
		assertTrue(bundle2.getIsEdited());
	}

	@Test
	public void testUpdateThreadMessage() throws Exception {
		Forum dto = servletTestHelper.getForumMetadata(dispatchServlet, project.getId(), adminUserId);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle bundle = servletTestHelper.createThread(dispatchServlet, adminUserId, createThread);
		UpdateThreadMessage newMessage = new UpdateThreadMessage();
		newMessage.setMessageMarkdown("newMessageMarkdown");
		DiscussionThreadBundle bundle2 = servletTestHelper.updateThreadMessage(dispatchServlet, adminUserId, bundle.getId(), newMessage);
		assertFalse(bundle.equals(bundle2));
		assertEquals(bundle2.getId(), bundle.getId());
		assertTrue(bundle2.getIsEdited());
	}

	@Test
	public void testMarkThreadAsDeleted() throws Exception {
		Forum dto = servletTestHelper.getForumMetadata(dispatchServlet, project.getId(), adminUserId);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle bundle = servletTestHelper.createThread(dispatchServlet, adminUserId, createThread);
		servletTestHelper.markThreadAsDeleted(dispatchServlet, adminUserId, bundle.getId());
		DiscussionThreadBundle bundle2 = servletTestHelper.getThread(dispatchServlet, adminUserId, bundle.getId());
		assertFalse(bundle.equals(bundle2));
		assertEquals(bundle2.getId(), bundle.getId());
		assertTrue(bundle2.getIsDeleted());
	}

	@Test
	public void testCreateReply() throws Exception {
		Forum dto = servletTestHelper.getForumMetadata(dispatchServlet, project.getId(), adminUserId);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle threadBundle = servletTestHelper.createThread(dispatchServlet, adminUserId, createThread);
		createReply.setThreadId(threadBundle.getId());
		DiscussionReplyBundle replyBundle = servletTestHelper.createReply(dispatchServlet, adminUserId, createReply);
		assertNotNull(replyBundle);
		assertEquals(replyBundle.getThreadId(), createReply.getThreadId());
	}

	@Test
	public void testGetReply() throws Exception {
		Forum dto = servletTestHelper.getForumMetadata(dispatchServlet, project.getId(), adminUserId);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle threadBundle = servletTestHelper.createThread(dispatchServlet, adminUserId, createThread);
		createReply.setThreadId(threadBundle.getId());
		DiscussionReplyBundle replyBundle = servletTestHelper.createReply(dispatchServlet, adminUserId, createReply);
		assertEquals(replyBundle, servletTestHelper.getReply(dispatchServlet, adminUserId, replyBundle.getId()));
	}

	@Test
	public void testGetReplies() throws Exception {
		Forum forum = servletTestHelper.getForumMetadata(dispatchServlet, project.getId(), adminUserId);
		createThread.setForumId(forum.getId());
		DiscussionThreadBundle threadBundle = servletTestHelper.createThread(dispatchServlet, adminUserId, createThread);
		createReply.setThreadId(threadBundle.getId());
		DiscussionReplyBundle replyBundle = servletTestHelper.createReply(dispatchServlet, adminUserId, createReply);
		PaginatedResults<DiscussionReplyBundle> results = servletTestHelper.getReplies(dispatchServlet, adminUserId, threadBundle.getId(), 100L, 0L, null, null);
		assertEquals(replyBundle, results.getResults().get(0));
		assertEquals(1L, results.getTotalNumberOfResults());
	}

	@Test
	public void testUpdateReplyMessage() throws Exception {
		Forum dto = servletTestHelper.getForumMetadata(dispatchServlet, project.getId(), adminUserId);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle threadBundle = servletTestHelper.createThread(dispatchServlet, adminUserId, createThread);
		createReply.setThreadId(threadBundle.getId());
		DiscussionReplyBundle replyBundle = servletTestHelper.createReply(dispatchServlet, adminUserId, createReply);
		UpdateReplyMessage newMessage = new UpdateReplyMessage();
		newMessage.setMessageMarkdown("newMessageMarkdown");
		DiscussionReplyBundle bundle2 = servletTestHelper.updateReplyMessage(dispatchServlet, adminUserId, replyBundle.getId(), newMessage);
		assertFalse(replyBundle.equals(bundle2));
		assertEquals(bundle2.getId(), replyBundle.getId());
		assertTrue(bundle2.getIsEdited());
	}

	@Test
	public void testMarkReplyAsDeleted() throws Exception {
		Forum dto = servletTestHelper.getForumMetadata(dispatchServlet, project.getId(), adminUserId);
		createThread.setForumId(dto.getId());
		DiscussionThreadBundle threadBundle = servletTestHelper.createThread(dispatchServlet, adminUserId, createThread);
		createReply.setThreadId(threadBundle.getId());
		DiscussionReplyBundle replyBundle = servletTestHelper.createReply(dispatchServlet, adminUserId, createReply);
		servletTestHelper.markReplyAsDeleted(dispatchServlet, adminUserId, replyBundle.getId());
		DiscussionReplyBundle bundle2 = servletTestHelper.getReply(dispatchServlet, adminUserId, replyBundle.getId());
		assertFalse(replyBundle.equals(bundle2));
		assertEquals(bundle2.getId(), replyBundle.getId());
		assertTrue(bundle2.getIsDeleted());
	}
}
