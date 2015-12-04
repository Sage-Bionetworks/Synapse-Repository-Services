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
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;

public class DiscussionControllerAutowiredTest extends AbstractAutowiredControllerTestBase{

	private Entity project;
	private Long adminUserId;
	private CreateDiscussionThread toCreate;

	@Before
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		project = new Project();
		project.setName(UUID.randomUUID().toString());
		project = servletTestHelper.createEntity(dispatchServlet, project, adminUserId);

		toCreate = new CreateDiscussionThread();
		toCreate.setTitle("title");
		toCreate.setMessageMarkdown("messageMarkdown");
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
		toCreate.setForumId(dto.getId());
		DiscussionThreadBundle bundle = servletTestHelper.createThread(dispatchServlet, adminUserId, toCreate);
		assertNotNull(bundle);
		assertEquals(bundle.getForumId(), toCreate.getForumId());
		assertEquals(bundle.getTitle(), toCreate.getTitle());
	}

	@Test
	public void testGetThread() throws Exception {
		Forum dto = servletTestHelper.getForumMetadata(dispatchServlet, project.getId(), adminUserId);
		toCreate.setForumId(dto.getId());
		DiscussionThreadBundle bundle = servletTestHelper.createThread(dispatchServlet, adminUserId, toCreate);
		DiscussionThreadBundle bundle2 = servletTestHelper.getThread(dispatchServlet, adminUserId, bundle.getId());
		assertEquals(bundle, bundle2);
	}

	@Test
	public void testGetThreadCount() throws Exception {
		Forum forum = servletTestHelper.getForumMetadata(dispatchServlet, project.getId(), adminUserId);
		toCreate.setForumId(forum.getId());
		servletTestHelper.createThread(dispatchServlet, adminUserId, toCreate);
		assertEquals((Long) 1L, servletTestHelper.getThreadCount(dispatchServlet, adminUserId, forum.getId()));
	}

	@Test
	public void testGetThreads() throws Exception {
		Forum forum = servletTestHelper.getForumMetadata(dispatchServlet, project.getId(), adminUserId);
		toCreate.setForumId(forum.getId());
		DiscussionThreadBundle bundle = servletTestHelper.createThread(dispatchServlet, adminUserId, toCreate);
		PaginatedResults<DiscussionThreadBundle> results = servletTestHelper.getThreads(dispatchServlet, adminUserId, forum.getId(), 100L, 0L, null, true);
		assertEquals(bundle, results.getResults().get(0));
	}

	@Test
	public void testUpdateThreadTitle() throws Exception {
		Forum dto = servletTestHelper.getForumMetadata(dispatchServlet, project.getId(), adminUserId);
		toCreate.setForumId(dto.getId());
		DiscussionThreadBundle bundle = servletTestHelper.createThread(dispatchServlet, adminUserId, toCreate);
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
		toCreate.setForumId(dto.getId());
		DiscussionThreadBundle bundle = servletTestHelper.createThread(dispatchServlet, adminUserId, toCreate);
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
		toCreate.setForumId(dto.getId());
		DiscussionThreadBundle bundle = servletTestHelper.createThread(dispatchServlet, adminUserId, toCreate);
		servletTestHelper.markThreadAsDeleted(dispatchServlet, adminUserId, bundle.getId());
		DiscussionThreadBundle bundle2 = servletTestHelper.getThread(dispatchServlet, adminUserId, bundle.getId());
		assertFalse(bundle.equals(bundle2));
		assertEquals(bundle2.getId(), bundle.getId());
		assertTrue(bundle2.getIsDeleted());
	}
}
