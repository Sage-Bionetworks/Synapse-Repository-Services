package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.manager.DockerManager;
import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;
import org.springframework.test.util.ReflectionTestUtils;

public class DockerServiceImplTest {
	
	private DockerServiceImpl dockerService;
	
	@Mock
	private DockerManager dockerManager;
	
	@Mock
	private Consumer consumer;
	
	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		dockerService = new DockerServiceImpl();
		ReflectionTestUtils.setField(dockerService, "dockerManager", dockerManager);
		ReflectionTestUtils.setField(dockerService, "consumer", consumer);
		
	}

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
		assertEquals("org.sagebionetworks.repo.web.service.DockerService", profileData.getNamespace());
		assertEquals(1.0, profileData.getValue(), 1e-6);
		assertEquals("Count", profileData.getUnit());
		assertNotNull(profileData.getTimestamp());

	}

}
