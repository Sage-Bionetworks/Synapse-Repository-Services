package org.sagebionetworks.repo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.manager.DockerManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;

@ExtendWith(MockitoExtension.class)
public class DockerServiceImplTest {
	
	@Mock
	private UserManager mockUserManager;
	
	@Mock
	private DockerManager dockerManager;
	
	@Mock
	private Consumer consumer;
	
	@InjectMocks
	private DockerServiceImpl dockerService;
	
	@Mock
	private UserInfo mockUser;
	
	@Test
	public void testDockerRegistryNotification() {
		DockerRegistryEventList events = new DockerRegistryEventList();
		
		// method under test
		dockerService.dockerRegistryNotification(events);
		
		verify(dockerManager).dockerRegistryNotification(events);
		verify(consumer, never()).addProfileData((ProfileData)anyObject());
	}

	@Test
	public void testDockerRegistryNotificationError() {
		DockerRegistryEventList events = new DockerRegistryEventList();
		String message = "<message>";
		doThrow(new RuntimeException(message)).when(dockerManager).dockerRegistryNotification(events);
		// method under test
		dockerService.dockerRegistryNotification(events);

		verify(dockerManager).dockerRegistryNotification(events);
		ArgumentCaptor<ProfileData> profileDataCaptor = ArgumentCaptor.forClass(ProfileData.class);
		verify(consumer).addProfileData(profileDataCaptor.capture());
		ProfileData profileData = profileDataCaptor.getValue();

		assertEquals("java.lang.RuntimeException "+message, profileData.getName());
		assertEquals("org.sagebionetworks.repo.service.DockerService", profileData.getNamespace());
		assertEquals(1.0, profileData.getValue(), 1e-6);
		assertEquals("Count", profileData.getUnit());
		assertNotNull(profileData.getTimestamp());

	}
	
	@Test
	public void testGetEntityIdForRepositoryName() {
		Long userId = 123L;
		String repoName = "repository";
		
		when(mockUserManager.getUserInfo(anyLong())).thenReturn(mockUser);
		
		// Call under test
		dockerService.getEntityIdForRepositoryName(userId, repoName);
		
		verify(dockerManager).getEntityIdForRepositoryName(mockUser, repoName);
	}

}
