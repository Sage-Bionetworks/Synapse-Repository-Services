package org.sagebionetworks.repo.manager.discussion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.subscription.SubscriptionManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.MessageURL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DiscussionThreadManagerImplAutowiredTest {

	@Autowired
	public NodeManager nodeManager;
	@Autowired
	public EntityManager entityManager;
	@Autowired
	public UserManager userManager;
	@Autowired
	public ForumManager forumManager;
	@Autowired
	public DiscussionThreadManager threadManager;
	@Autowired
	public SubscriptionManager subscriptionManager;

	private UserInfo adminUserInfo;
	private UserInfo userInfo;
	private String projectId;
	private Forum forum;
	private CreateDiscussionThread createThread;
	private String messageMarkdown = "<header>This is a message.</header>";

	@BeforeEach
	public void before() {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		UserInfo.validateUserInfo(adminUserInfo);

		NewUser nu = new NewUser();
		nu.setEmail(UUID.randomUUID().toString() + "@test.com");
		nu.setUserName(UUID.randomUUID().toString());
		userInfo = userManager.createOrGetTestUser(adminUserInfo, nu);
		userInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		Project project = new Project();
		project.setName("project");
		projectId = entityManager.createEntity(userInfo, project, null);
		forum = forumManager.createForum(userInfo, projectId);

		createThread = new CreateDiscussionThread();
		createThread.setForumId(forum.getId());
		createThread.setTitle("Title");
		createThread.setMessageMarkdown(messageMarkdown);
	}

	@AfterEach
	public void cleanup() {
		nodeManager.delete(adminUserInfo, projectId);
		subscriptionManager.deleteAll(userInfo);
		userManager.deletePrincipal(adminUserInfo, userInfo.getId());
	}

	@Test
	public void test() throws Exception {
		DiscussionThreadBundle bundle = threadManager.createThread(userInfo, createThread);
		assertNotNull(bundle);
		MessageURL messageUrl = threadManager.getMessageUrl(userInfo, bundle.getMessageKey());
		assertNotNull(messageUrl);
		URL url = new URL(messageUrl.getMessageUrl());
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
		assertEquals(con.getResponseCode(), 200);
		BufferedReader in = new BufferedReader(new InputStreamReader(
				new GZIPInputStream(con.getInputStream())));
		String inputLine;
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		assertNotNull(response.toString());
		assertEquals(messageMarkdown, response.toString());
	}
}
