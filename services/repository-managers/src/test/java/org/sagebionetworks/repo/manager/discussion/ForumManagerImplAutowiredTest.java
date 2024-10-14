package org.sagebionetworks.repo.manager.discussion;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ForumManagerImplAutowiredTest {
	@Autowired
	public NodeManager nodeManager;
	@Autowired
	public UserManager userManager;
	@Autowired
	public ForumManager forumManager;

	private UserInfo adminUserInfo;
	private UserInfo userInfo;
	private String projectId;

	@BeforeEach
	public void before() {
		
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		UserInfo.validateUserInfo(adminUserInfo);

		NewUser nu = new NewUser();
		nu.setEmail(UUID.randomUUID().toString() + "@test.com");
		nu.setUserName(UUID.randomUUID().toString());
		userInfo = userManager.createOrGetTestUser(adminUserInfo, nu);
		userInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		Node newNode = new Node();
		newNode.setName("project");
		newNode.setNodeType(EntityType.project);
		projectId = nodeManager.createNewNode(newNode, userInfo);
	}

	@AfterEach
	public void cleanup() {
		nodeManager.delete(adminUserInfo, projectId);
		userManager.deletePrincipal(adminUserInfo, userInfo.getId());
	}

	@Test
	public void testGetNonExistingForum() {
		Forum dto = forumManager.getForumByProjectId(userInfo, projectId);
		assertNotNull(dto);
		assertNotNull(dto.getId());
		assertEquals(dto.getProjectId(), projectId);
	}

}
