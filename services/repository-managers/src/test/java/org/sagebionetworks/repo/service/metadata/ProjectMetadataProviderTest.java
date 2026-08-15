package org.sagebionetworks.repo.service.metadata;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.discussion.ForumManager;
import org.sagebionetworks.repo.manager.limits.ProjectStorageLimitsManager;
import org.sagebionetworks.repo.manager.subscription.SubscriptionManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;

@ExtendWith(MockitoExtension.class)
public class ProjectMetadataProviderTest {

	@Mock
	private ForumManager mockForumManager;
	@Mock
	private SubscriptionManager mockSubscriptionManager;
	@Mock
	private ProjectStorageLimitsManager mockStorageLimitsManager;
	@InjectMocks
	private ProjectMetadataProvider provider;
	
	private Project project;
	private String projectId;
	private UserInfo userInfo;
	private Long userId;
	private Forum forum;
	private String forumId;
	
	@BeforeEach
	public void before(){
		project = new Project();
		projectId = "101";
		project.setId(projectId);
		userId = 123L;
		userInfo = new UserInfo(false, userId, AuthorizationConstants.DEFAULT_REALM_ID);
		forum = new Forum();
		forumId = "456";
		forum.setId(forumId);

	}

	@Test
	public void testEntityCreated() {
		
		when(mockForumManager.createForum(userInfo, projectId)).thenReturn(forum);

		// Call under test
		provider.entityCreated(userInfo, project);
		
		verify(mockForumManager).createForum(userInfo, projectId);
		verify(mockSubscriptionManager).create(userInfo, new Topic().setObjectId(forumId).setObjectType(SubscriptionObjectType.FORUM));
		verify(mockStorageLimitsManager).setDefaultProjectStorageLimit(projectId, StorageLocationDAO.DEFAULT_STORAGE_LOCATION_ID);
	}
}
