package org.sagebionetworks.repo.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.FileHandleUrlRequest;
import org.sagebionetworks.repo.manager.sts.StsManager;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.sts.StsCredentials;
import org.sagebionetworks.repo.model.sts.StsPermission;
import org.sagebionetworks.repo.web.service.metadata.AllTypesValidator;
import org.sagebionetworks.repo.web.service.metadata.EntityProvider;
import org.sagebionetworks.repo.web.service.metadata.MetadataProviderFactory;
import org.sagebionetworks.repo.web.service.metadata.TypeSpecificCreateProvider;
import org.sagebionetworks.repo.web.service.metadata.TypeSpecificUpdateProvider;

@ExtendWith(MockitoExtension.class)
public class EntityServiceImplUnitTest {
	private static final String ENTITY_ID = "syn123";

	@InjectMocks
	EntityServiceImpl entityService;
	@Mock
	EntityManager mockEntityManager;
	@Mock
	HttpServletRequest mockRequest;
	@Mock
	StsManager mockStsManager;
	@Mock
	UserManager mockUserManager;
	@Mock
	FileHandleManager mockFileHandleManager;
	@Mock
	AllTypesValidator mockAllTypesValidator;
	@Mock
	MetadataProviderFactory mockMetadataProviderFactory;
	@Mock
	TypeSpecificUpdateProvider<Project> mockProjectUpdateProvider;
	@Mock
	TypeSpecificCreateProvider<Project> mockProjectCreateProvider;

	List<EntityProvider<? extends Entity>> projectProviders;

	static final Long PRINCIPAL_ID = 101L;
	UserInfo userInfo = null;

	Project project;

	@BeforeEach
	public void before() {

		projectProviders = new ArrayList<>();
		projectProviders.add(mockProjectUpdateProvider);
		projectProviders.add(mockProjectCreateProvider);

		userInfo = new UserInfo(false);
		userInfo.setId(PRINCIPAL_ID);

		project = new Project();
		project.setId("syn123");
	}

	@Test
	public void testGetFileRedirectURLForCurrentVersion() {
		String entityId = "999";
		String fileHandleId = "111";
		when(mockUserManager.getUserInfo(PRINCIPAL_ID)).thenReturn(userInfo);
		when(mockEntityManager.getFileHandleIdForVersion(userInfo, entityId, null))
				.thenReturn(fileHandleId);
		String url = "http://foo.bar";
		when(mockFileHandleManager.getRedirectURLForFileHandle(any(FileHandleUrlRequest.class))).thenReturn(url);
		assertEquals(url, entityService.getFileRedirectURLForCurrentVersion(PRINCIPAL_ID, entityId));
	}

	@Test
	public void testGetFileRedirectURLForVersion() {
		String entityId = "999";
		String fileHandleId = "111";
		Long version = 1L;
		when(mockUserManager.getUserInfo(PRINCIPAL_ID)).thenReturn(userInfo);
		when(mockEntityManager.getFileHandleIdForVersion(userInfo, entityId, version)).thenReturn(fileHandleId);
		String url = "http://foo.bar";
		when(mockFileHandleManager.getRedirectURLForFileHandle(any(FileHandleUrlRequest.class))).thenReturn(url);
		assertEquals(url, entityService.getFileRedirectURLForVersion(PRINCIPAL_ID, entityId, version));
	}

	@Test
	public void testFireCreate() {
		when(mockUserManager.getUserInfo(PRINCIPAL_ID)).thenReturn(userInfo);
		when(mockMetadataProviderFactory.getMetadataProvider(EntityType.project)).thenReturn(projectProviders);
		// Call under test.
		entityService.createEntity(userInfo.getId(), project, null);
		verify(mockProjectCreateProvider).entityCreated(userInfo, project);
		verify(mockProjectUpdateProvider, never()).entityUpdated(any(UserInfo.class), any(Project.class), anyBoolean());
	}

	@Test
	public void testUpdateEntity_NullId() {
		project.setId(null);

		// Method under test.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> entityService.updateEntity(userInfo.getId(), project,
				false, null));
		assertEquals("Updated Entity cannot have a null id", ex.getMessage());
		verifyZeroInteractions(mockEntityManager);
	}

	@Test
	public void testFireUpdate() {
		boolean newVersion = true;
		when(mockUserManager.getUserInfo(PRINCIPAL_ID)).thenReturn(userInfo);
		when(mockMetadataProviderFactory.getMetadataProvider(EntityType.project)).thenReturn(projectProviders);
		when(mockEntityManager.updateEntity(userInfo, project, newVersion, null)).thenReturn(newVersion);
		// Call under test.
		entityService.updateEntity(userInfo.getId(), project, newVersion, null);
		verify(mockProjectCreateProvider, never()).entityCreated(any(UserInfo.class), any(Project.class));
		verify(mockProjectUpdateProvider).entityUpdated(userInfo, project, newVersion);
	}

	@Test
	public void testFireUpdateNoNewVersion() {
		boolean newVersion = false;
		when(mockUserManager.getUserInfo(PRINCIPAL_ID)).thenReturn(userInfo);
		when(mockMetadataProviderFactory.getMetadataProvider(EntityType.project)).thenReturn(projectProviders);
		when(mockEntityManager.updateEntity(userInfo, project, newVersion, null)).thenReturn(newVersion);
		// Call under test.
		entityService.updateEntity(userInfo.getId(), project, newVersion, null);
		verify(mockProjectCreateProvider, never()).entityCreated(any(UserInfo.class), any(Project.class));
		verify(mockProjectUpdateProvider).entityUpdated(userInfo, project, newVersion);
	}

	/**
	 * The new version parameter might be false, but we will still create a new
	 * version under some conditions such as changing a file's handle ID. When a new
	 * new version is created for such cases, the value must be forwarded to the
	 * entity update.
	 */
	@Test
	public void testFireUpdateTriggersNewVersion() {
		boolean newVersionParameter = false;
		final boolean wasNewVersionCreated = true;
		when(mockUserManager.getUserInfo(PRINCIPAL_ID)).thenReturn(userInfo);
		when(mockMetadataProviderFactory.getMetadataProvider(EntityType.project)).thenReturn(projectProviders);
		when(mockEntityManager.updateEntity(userInfo, project, newVersionParameter, null))
				.thenReturn(wasNewVersionCreated);
		// Call under test.
		entityService.updateEntity(userInfo.getId(), project, newVersionParameter, null);
		verify(mockProjectCreateProvider, never()).entityCreated(any(UserInfo.class), any(Project.class));
		verify(mockProjectUpdateProvider).entityUpdated(userInfo, project, wasNewVersionCreated);
	}

	@Test
	public void getTemporaryCredentialsForEntity() {
		// Mock dependencies.
		when(mockUserManager.getUserInfo(PRINCIPAL_ID)).thenReturn(userInfo);

		StsCredentials managerResult = new StsCredentials();
		when(mockStsManager.getTemporaryCredentials(userInfo, ENTITY_ID, StsPermission.read_only)).thenReturn(
				managerResult);

		// Method under test.
		StsCredentials serviceResult = entityService.getTemporaryCredentialsForEntity(PRINCIPAL_ID, ENTITY_ID,
				StsPermission.read_only);
		assertSame(managerResult, serviceResult);
		verify(mockStsManager).getTemporaryCredentials(userInfo, ENTITY_ID, StsPermission.read_only);
	}
}
