package org.sagebionetworks.repo.manager.discussion;

import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DiscussionThreadManagerImplAutowiredTest {

	@Autowired
	public NodeManager nodeManager;
	@Autowired
	public UserManager userManager;
	@Autowired
	public ForumManager forumManager;
	@Autowired
	public DiscussionThreadManager threadManager;

	private UserInfo adminUserInfo;
	private UserInfo userInfo;
	private String projectId;
	private Forum forum;
	private CreateDiscussionThread createThread;

	@Before
	public void before() {
		DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
		tou.setDomain(DomainType.SYNAPSE);
		tou.setAgreesToTermsOfUse(Boolean.TRUE);

		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		UserInfo.validateUserInfo(adminUserInfo);

		DBOCredential cred = new DBOCredential();
		cred.setSecretKey("");
		NewUser nu = new NewUser();
		nu.setEmail(UUID.randomUUID().toString() + "@test.com");
		nu.setUserName(UUID.randomUUID().toString());
		userInfo = userManager.createUser(adminUserInfo, nu, cred, tou);
		userInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		Node newNode = new Node();
		newNode.setName("project");
		newNode.setNodeType(EntityType.project);
		projectId = nodeManager.createNewNode(newNode, userInfo);
		forum = forumManager.getForumMetadata(userInfo, projectId);

		createThread = new CreateDiscussionThread();
		createThread.setForumId(forum.getId());
		createThread.setTitle("Title");
		createThread.setMessageMarkdown("<header>This is a message.</header>");
	}

	@After
	public void cleanup() {
		nodeManager.delete(adminUserInfo, projectId);
		userManager.deletePrincipal(adminUserInfo, userInfo.getId());
	}

	@Test
	public void test() throws Exception {
		assertNotNull(threadManager.createThread(userInfo, createThread));
		assertNotNull(threadManager.createThread(userInfo, createThread).getMessageUrl());
	}
}
