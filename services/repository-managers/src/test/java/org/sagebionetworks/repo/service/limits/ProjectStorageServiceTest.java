package org.sagebionetworks.repo.service.limits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.limits.ProjectStorageLimitsManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.limits.ProjectStorageLocationLimit;
import org.sagebionetworks.repo.model.limits.ProjectStorageUsage;

@ExtendWith(MockitoExtension.class)
public class ProjectStorageServiceTest {
	
	@Mock
	private UserManager mockUserManager;
	
	@Mock
	private ProjectStorageLimitsManager mockLimitsManager;
	
	@InjectMocks
	private ProjectStorageService service;

	private Long userId;
	
	private UserInfo user;
	
	private String projectId;
	
	@BeforeEach
	public void before() {
		userId = 123L;
		user = new UserInfo(false, userId, AuthorizationConstants.DEFAULT_REALM_ID);
		projectId = "syn123";
		
		when(mockUserManager.getUserInfo(userId)).thenReturn(user);
	}
	
	@Test
	public void testGetProjectStorageUsage() {
		ProjectStorageUsage usage = new ProjectStorageUsage();
		
		when(mockLimitsManager.getProjectStorageUsage(user, projectId)).thenReturn(usage);
		
		// Call under test
		assertEquals(usage, service.getProjectStorageUsage(userId, projectId));
	}

	@Test
	public void testSetProjectStorageLimit() {
		ProjectStorageLocationLimit limit = new ProjectStorageLocationLimit();
		
		when(mockLimitsManager.setProjectStorageLimit(user, limit)).thenReturn(limit);
		
		// Call under test
		assertEquals(limit, service.setProjectStorageLocationLimit(userId, limit));
	}
}
