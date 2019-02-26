package org.sagebionetworks.discussion.workers;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.discussion.DiscussionReplyManager;
import org.sagebionetworks.repo.manager.discussion.DiscussionThreadManager;
import org.sagebionetworks.repo.manager.discussion.ForumManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DiscussionThreadStatsWorkerIntegrationTest {
	private static final long TIME_OUT = 30*1000;

	@Autowired
	private DiscussionThreadManager threadManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private ForumManager forumManager;
	@Autowired
	private DiscussionReplyManager replyManager;

	private UserInfo adminUserInfo;
	private String entityToDelete;
	private String threadId;

	@Before
	public void before() throws IOException {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		Project project = new Project();
		project.setName(UUID.randomUUID().toString());
		String id = entityManager.createEntity(adminUserInfo, project, null);
		entityToDelete = id;
		Forum forum = forumManager.createForum(adminUserInfo, id);
		CreateDiscussionThread createThread = new CreateDiscussionThread();
		createThread.setForumId(forum.getId());
		createThread.setTitle("Test Thread");
		createThread.setMessageMarkdown("some text");
		threadId = threadManager.createThread(adminUserInfo, createThread).getId();
	}

	@After
	public void after(){
		try {
			entityManager.deleteEntity(adminUserInfo, entityToDelete);
		} catch (Exception e) {
			// the entity is deleted, do nothing
		}
	}

	@Test
	public void test() throws InterruptedException, IOException {
		CreateDiscussionReply createReply = new CreateDiscussionReply();
		createReply.setThreadId(threadId);
		createReply.setMessageMarkdown("a reply");
		replyManager.createReply(adminUserInfo, createReply );
		long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() - startTime < TIME_OUT) {
			// wait for the worker to update stat
			Thread.sleep(1000);
			DiscussionThreadBundle bundle = threadManager.getThread(adminUserInfo, threadId);
			if (bundle.getNumberOfReplies() == 1) {
				return;
			}
		}
		fail();
	}

}
