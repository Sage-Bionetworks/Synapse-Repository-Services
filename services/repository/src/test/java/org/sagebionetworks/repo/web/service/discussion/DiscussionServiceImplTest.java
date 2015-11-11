package org.sagebionetworks.repo.web.service.discussion;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.discussion.ForumManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.test.util.ReflectionTestUtils;

public class DiscussionServiceImplTest {
	private UserManager mockUserManager;
	private ForumManager mockForumManager;
	private DiscussionServiceImpl discussionServices;
	private Long userId = 123L;
	private UserInfo userInfo = new UserInfo(false /*not admin*/);
	private String projectId = "syn456";

	@Before
	public void before() {
		mockUserManager = Mockito.mock(UserManager.class);
		mockForumManager = Mockito.mock(ForumManager.class);
		discussionServices = new DiscussionServiceImpl();
		ReflectionTestUtils.setField(discussionServices, "userManager", mockUserManager);
		ReflectionTestUtils.setField(discussionServices, "forumManager", mockForumManager);

		Mockito.when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
	}

	@Test
	public void testCreateForum() {
		discussionServices.createForum(userId, projectId);
		Mockito.verify(mockUserManager).getUserInfo(userId);
		Mockito.verify(mockForumManager).createForum(userInfo, projectId);
	}

	@Test
	public void testGetForumMetadata() {
		discussionServices.getForumMetadata(userId, projectId);
		Mockito.verify(mockUserManager).getUserInfo(userId);
		Mockito.verify(mockForumManager).getForumMetadata(userInfo, projectId);
	}
}
