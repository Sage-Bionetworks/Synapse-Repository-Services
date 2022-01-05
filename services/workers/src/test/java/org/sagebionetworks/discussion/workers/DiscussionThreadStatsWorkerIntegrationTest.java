package org.sagebionetworks.discussion.workers;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
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

	@BeforeEach
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

	@AfterEach
	public void after(){
		try {
			entityManager.deleteEntity(adminUserInfo, entityToDelete);
		} catch (Exception e) {
			// the entity is deleted, do nothing
		}
	}

	@Test
	public void test() throws Exception {
		CreateDiscussionReply createReply = new CreateDiscussionReply();
		createReply.setThreadId(threadId);
		createReply.setMessageMarkdown("a reply");
		replyManager.createReply(adminUserInfo, createReply );
		TimeUtils.waitFor(TIME_OUT, 1000, () -> {
			DiscussionThreadBundle bundle = threadManager.getThread(adminUserInfo, threadId);
			
			return Pair.create(bundle.getNumberOfReplies() == 1, null);
		});
	}

}
