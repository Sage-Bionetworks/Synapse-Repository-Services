package org.sagebionetworks.repo.web.service.metadata;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.discussion.ForumManager;
import org.sagebionetworks.repo.manager.subscription.SubscriptionManager;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.springframework.test.util.ReflectionTestUtils;

public class ProjectMetadataProviderTest {

	@Mock
	Project mockProject;
	@Mock
	HttpServletRequest mockRequest;
	@Mock
	ForumManager mockForumManager;
	@Mock
	SubscriptionManager mockSubscriptionManager;

	ProjectMetadataProvider provider;
	Project project;
	String projectId;
	UserInfo userInfo;
	Long userId;
	Forum forum;
	String forumId;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		when(mockProject.getId()).thenReturn("101");
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		when(mockRequest.getRequestURI()).thenReturn("/project");

		provider = new ProjectMetadataProvider();
		ReflectionTestUtils.setField(provider, "forumManager", mockForumManager);
		ReflectionTestUtils.setField(provider, "subscriptionManager", mockSubscriptionManager);

		project = new Project();
		projectId = "101";
		project.setId(projectId);
		userInfo = new UserInfo(false);
		userId = 123L;
		userInfo.setId(userId);
		forum = new Forum();
		forumId = "456";
		forum.setId(forumId);

		when(mockForumManager.createForum(userInfo, projectId)).thenReturn(forum);

	}

	@Test
	public void testEntityCreated() {
		provider.entityCreated(userInfo, project);
		verify(mockForumManager).createForum(userInfo, projectId);
		Topic topic = new Topic();
		topic.setObjectId(forumId);
		topic.setObjectType(SubscriptionObjectType.FORUM);
		verify(mockSubscriptionManager).create(userInfo, topic);
	}
}
