package org.sagebionetworks.repo.web;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.NodeManager.FileHandleReason;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.service.EntityServiceImpl;
import org.sagebionetworks.repo.web.service.metadata.AllTypesValidator;
import org.sagebionetworks.repo.web.service.metadata.EntityProvider;
import org.sagebionetworks.repo.web.service.metadata.MetadataProviderFactory;
import org.sagebionetworks.repo.web.service.metadata.TypeSpecificCreateProvider;
import org.sagebionetworks.repo.web.service.metadata.TypeSpecificUpdateProvider;

@RunWith(MockitoJUnitRunner.class)
public class EntityServiceImplUnitTest {
	
	@InjectMocks
	EntityServiceImpl entityService;
	@Mock
	EntityManager mockEntityManager;
	@Mock
	HttpServletRequest mockRequest;
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
	
	@Before
	public void before(){
		
		projectProviders = new ArrayList<EntityProvider<?>>();
		projectProviders.add(mockProjectUpdateProvider);
		projectProviders.add(mockProjectCreateProvider);
		
		userInfo = new UserInfo(false);
		userInfo.setId(PRINCIPAL_ID);
		when(mockUserManager.getUserInfo(PRINCIPAL_ID)).thenReturn(userInfo);
		
		when(mockMetadataProviderFactory.getMetadataProvider(EntityType.project)).thenReturn(projectProviders);
		
		project = new Project();
		project.setId("syn123");
		when(mockEntityManager.getEntity(any(UserInfo.class), anyString(), any(Class.class))).thenReturn(project);
		when(mockEntityManager.createEntity(any(UserInfo.class), any(Entity.class), any(String.class))).thenReturn(project.getId());
		
		when(mockRequest.getServletPath()).thenReturn("path");

	}
	
	@Test
	public void testGetFileRedirectURLForCurrentVersion() throws Exception {
		String entityId = "999";
		String fileHandleId = "111";
		when(mockEntityManager.
				getFileHandleIdForVersion(userInfo, entityId, null, FileHandleReason.FOR_FILE_DOWNLOAD)).
				thenReturn(fileHandleId);
		String url = "http://foo.bar";
		when(mockFileHandleManager.getRedirectURLForFileHandle(fileHandleId)).thenReturn(url);
		assertEquals(url, entityService.getFileRedirectURLForCurrentVersion(PRINCIPAL_ID, entityId));
	}
	
	
	@Test
	public void testGetFileRedirectURLForVersion() throws Exception {
		String entityId = "999";
		String fileHandleId = "111";
		Long version = 1L;
		when(mockEntityManager.
				getFileHandleIdForVersion(userInfo, entityId, version, FileHandleReason.FOR_FILE_DOWNLOAD)).
				thenReturn(fileHandleId);
		String url = "http://foo.bar";
		when(mockFileHandleManager.getRedirectURLForFileHandle(fileHandleId)).thenReturn(url);
		assertEquals(url, entityService.getFileRedirectURLForVersion(PRINCIPAL_ID, entityId, version));
	}
	
	@Test
	public void testFireCreate(){
		// Call under test.
		entityService.createEntity(userInfo.getId(), project, null, mockRequest);
		verify(mockProjectCreateProvider).entityCreated(userInfo, project);
		verify(mockProjectUpdateProvider, never()).entityUpdated(userInfo, project);
	}
	
	@Test
	public void testFireUpdate(){
		boolean newVersion = true;
		// Call under test.
		entityService.updateEntity(userInfo.getId(), project, newVersion, null, mockRequest);
		verify(mockProjectCreateProvider, never()).entityCreated(userInfo, project);
		verify(mockProjectUpdateProvider).entityUpdated(userInfo, project);
	}
	
}
